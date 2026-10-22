#!/bin/bash
# Safe Spring Boot deployment for AWS CodeDeploy
set -uo pipefail
umask 077

BASE=/home/ec2-user
BUILD_DIR="$BASE/build"
BACKUP_DIR="$BASE/deploy-backups"
RELEASE_DIR="$BASE/deploy-releases"
LOG_DIR="$BASE/deploy-logs"
ENV_FILE="$BASE/config/app.env"
APP_LINK="$BASE/springboot-deploy.jar"
PORT=13270
HEALTH_URL="http://127.0.0.1:${PORT}/"
JAVA=/usr/bin/java

OLD_PID=""
OLD_JAR=""
NEW_PID=""
NEW_JAR=""
BACKUP_FILE=""
NEW_LOG=""
SERVICE_STOPPED=0
COMMITTED=0
ROLLBACK_DONE=0

log() { printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "ERROR: $*"; exit 1; }
alive() {
  [[ "${1:-}" =~ ^[0-9]+$ ]] || return 1
  kill -0 "$1" 2>/dev/null || return 1
  local state
  state=$(ps -o stat= -p "$1" 2>/dev/null) || return 1
  [[ -n "$state" && "$state" != Z* ]]
}
port_pids() {
  sudo -n ss -H -lntp "( sport = :$PORT )" 2>/dev/null |
    grep -oE 'pid=[0-9]+' | cut -d= -f2 | sort -un || true
}
health() {
  local expected="$1" loops="$2" i actual code
  for ((i=0; i<loops; i++)); do
    alive "$expected" || return 1
    actual=$(port_pids)
    if [[ "$actual" == "$expected" ]]; then
      code=$(curl --connect-timeout 1 --max-time 2 -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" 2>/dev/null || true)
      if [[ "$code" == 200 ]]; then return 0; fi
    fi
    sleep 2
  done
  return 1
}
stop_app() {
  local pid="$1" i
  alive "$pid" || return 0
  kill -TERM "$pid" || return 1
  for ((i=0; i<20; i++)); do
    if ! alive "$pid"; then return 0; fi
    sleep 1
  done
  return 1
}
link_to() {
  local target="$1" tmp
  tmp=$(mktemp -u "$BASE/.springboot-link-XXXXXXXX") || return 1
  ln -s "$target" "$tmp" || return 1
  mv -Tf "$tmp" "$APP_LINK" || { rm -f "$tmp"; return 1; }
}
start_app() {
  local logfile="$1"
  nohup "$JAVA" -Xms2048m -Xmx8192m -jar "$APP_LINK" > "$logfile" 2>&1 < /dev/null &
  STARTED_PID=$!
}
rollback() {
  local rp
  (( ROLLBACK_DONE == 0 )) || return 1
  ROLLBACK_DONE=1
  log '이전 버전 복구 시작'
  if [[ -n "$NEW_PID" ]] && alive "$NEW_PID"; then
    stop_app "$NEW_PID" || { log 'ERROR: 신규 프로세스 종료 실패'; return 1; }
  fi
  if [[ -n "$OLD_PID" ]] && alive "$OLD_PID"; then
    log '기존 프로세스가 아직 실행 중입니다. 중복 실행하지 않습니다.'
    return 0
  fi
  [[ -s "$BACKUP_FILE" ]] || { log 'ERROR: 복구 백업 파일 없음'; return 1; }
  link_to "$BACKUP_FILE" || return 1
  start_app "$LOG_DIR/rollback-$(date +%Y%m%d%H%M%S)-$$.log"
  rp="$STARTED_PID"
  if health "$rp" 20; then
    log "롤백 성공: PID $rp"
    return 0
  fi
  log 'ERROR: 롤백 서비스 HTTP 200 검증 실패'
  return 1
}
on_exit() {
  local status="$1"
  trap - EXIT INT TERM
  if (( COMMITTED == 0 && SERVICE_STOPPED == 1 )); then
    rollback || log 'ERROR: 자동 롤백 실패 - 수동 확인 필요'
  fi
  if (( COMMITTED == 0 )) && [[ -n "$NEW_JAR" && -f "$NEW_JAR" ]]; then
    if [[ "$(readlink -f "$APP_LINK" 2>/dev/null || true)" != "$NEW_JAR" ]]; then
      rm -f -- "$NEW_JAR" || true
    fi
  fi
  exit "$status"
}
trap 'on_exit $?' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

mkdir -p "$BACKUP_DIR" "$RELEASE_DIR" "$LOG_DIR" || fail '작업 폴더 생성 실패'
exec 9>"$BASE/deploy.lock" || fail '잠금 파일 열기 실패'
flock -n 9 || fail '다른 배포가 진행 중입니다.'
log '안전 배포 시작'
for cmd in sudo ss curl ps readlink find cp sha256sum mktemp flock grep cut sort mv ln java; do
  command -v "$cmd" >/dev/null 2>&1 || fail "필수 명령어 없음: $cmd"
done
sudo -n ss -H -lntp "( sport = :$PORT )" >/dev/null 2>&1 || fail '포트 조회 권한 없음'
[[ -r "$ENV_FILE" ]] || fail '환경변수 파일 접근 불가'
set -a
# shellcheck disable=SC1090
source "$ENV_FILE" || fail '환경변수 로드 실패'
set +a
for key in DB_URL DB_USERNAME DB_PASSWORD AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY; do
  [[ -n "${!key:-}" ]] || fail "환경변수 누락: $key"