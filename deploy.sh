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
  nohup "$JAVA" -Xms2048m -Xmx8192m -jar "$APP_LINK" \
    > "$logfile" 2>&1 < /dev/null 9>&- &
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
    if health "$OLD_PID" 3; then
      log '기존 프로세스 정상 응답 확인. 중복 실행하지 않습니다.'
      return 0
    fi
    log 'ERROR: 기존 프로세스가 살아 있지만 정상 응답하지 않습니다. 포트 충돌 방지를 위해 재실행하지 않습니다.'
    return 1
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
  if (( COMMITTED == 0 && status == 0 )); then status=1; fi
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
exec 9>"$BASE/deploy-v2.lock" || fail '잠금 파일 열기 실패'
flock -n 9 || fail '다른 배포가 진행 중입니다.'
log '안전 배포 시작'
for cmd in sudo ss curl ps readlink find cp sha256sum mktemp flock grep cut sort mv ln tr rm chmod; do
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
done

mapfile -t jars < <(find "$BUILD_DIR" -maxdepth 1 -type f -name '*.jar' -print)
[[ ${#jars[@]} -eq 1 ]] || fail '빌드 JAR이 정확히 1개여야 합니다.'
BUILD_JAR="${jars[0]}"
[[ -s "$BUILD_JAR" ]] || fail '빌드 JAR이 비어 있습니다.'

# 포트를 실제로 점유한 Java 프로세스가 기존 서비스의 기준입니다.
OLD_PID=$(port_pids)
[[ "$OLD_PID" =~ ^[0-9]+$ ]] || fail '13270 포트 점유 PID가 정확히 1개가 아닙니다.'
alive "$OLD_PID" || fail '기존 서비스 프로세스가 실행 중이지 않습니다.'
cmdline=$(tr '\0' '\n' < "/proc/$OLD_PID/cmdline") || fail '기존 실행 명령 확인 실패'
mapfile -t args < <(printf '%s\n' "$cmdline")
OLD_JAR=""
for ((i=0; i<${#args[@]}-1; i++)); do
  if [[ "${args[i]}" == '-jar' ]]; then OLD_JAR="${args[i+1]}"; break; fi
done
[[ "$OLD_JAR" == "$BASE/"* ]] || fail '기존 프로세스 JAR 경로가 허용 범위 밖입니다.'
OLD_JAR=$(readlink -f "$OLD_JAR") || fail '기존 JAR 실경로 확인 실패'
[[ -s "$OLD_JAR" ]] || fail '기존 운영 JAR이 없습니다.'
[[ "$cmdline" == *java* && "$cmdline" == *-jar* ]] || fail '포트 점유 프로세스가 Java JAR 실행이 아닙니다.'
health "$OLD_PID" 1 || fail '기존 서비스 HTTP 200 검증 실패'
log "현재 운영 확인: PID $OLD_PID / $OLD_JAR"

BACKUP_FILE="$BACKUP_DIR/backup-$(date +%Y%m%d%H%M%S)-$$.jar"
cp -- "$OLD_JAR" "$BACKUP_FILE" || fail '정상 버전 백업 실패'
[[ "$(sha256sum "$OLD_JAR" | cut -d' ' -f1)" == "$(sha256sum "$BACKUP_FILE" | cut -d' ' -f1)" ]] || fail '백업 체크섬 불일치'
log "정상 버전 백업: $BACKUP_FILE"

NEW_JAR=$(mktemp "$RELEASE_DIR/release-$(date +%Y%m%d%H%M%S)-XXXXXXXX.jar") || fail '신규 JAR 생성 실패'
cp -- "$BUILD_JAR" "$NEW_JAR" || fail '신규 JAR 복사 실패'
[[ "$(sha256sum "$BUILD_JAR" | cut -d' ' -f1)" == "$(sha256sum "$NEW_JAR" | cut -d' ' -f1)" ]] || fail '신규 JAR 체크섬 불일치'
NEW_LOG="$LOG_DIR/start-$(date +%Y%m%d%H%M%S)-$$.log"

# 종료 신호를 보내기 전에 롤백 보호를 활성화합니다.
# 종료 대기 시간이 초과된 뒤 프로세스가 늦게 종료되는 경우도 복구 대상입니다.
SERVICE_STOPPED=1
if ! stop_app "$OLD_PID"; then
  fail '기존 서비스 정상 종료 실패 (강제 종료하지 않음)'
fi
link_to "$NEW_JAR" || fail '신규 JAR 링크 변경 실패'
start_app "$NEW_LOG"
NEW_PID="$STARTED_PID"
log "신규 프로세스 시작: PID $NEW_PID"
health "$NEW_PID" 20 || fail "신규 서비스 검증 실패 (로그: $NEW_LOG)"
COMMITTED=1
log "배포 성공: PID $NEW_PID"

# 최근 자동 백업 2개만 유지합니다.
# 구버전 Bash에서도 동작하도록 mapfile -d를 사용하지 않습니다.
BACKUP_COUNT=0

while IFS= read -r name; do
  BACKUP_COUNT=$((BACKUP_COUNT + 1))

  if (( BACKUP_COUNT > 2 )); then
    if [[ "$(readlink -f "$APP_LINK")" != "$BACKUP_DIR/$name" ]]; then
      rm -f -- "$BACKUP_DIR/$name" || log "WARNING: 백업 정리 실패: $name"
    fi
  fi
done < <(
  find "$BACKUP_DIR" -maxdepth 1 -type f -name 'backup-*.jar' -printf '%f\n' |
  sort -r
)

log '배포 종료'
exit 0
