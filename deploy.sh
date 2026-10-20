#!/bin/bash
# ==========================================
# AWS 및 DB 환경변수 설정
# ==========================================
ENV_FILE=/home/ec2-user/config/app.env

# 환경변수 파일 존재 및 읽기 권한 확인
if [ ! -r "$ENV_FILE" ]; then
    echo "ERROR: 환경변수 파일을 읽을 수 없습니다."
    exit 1
fi

# 환경변수 로드
set -a
source "$ENV_FILE" || exit 1
set +a

# 필수 환경변수 확인
for VAR in DB_URL DB_USERNAME DB_PASSWORD AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
do
    if [ -z "${!VAR}" ]; then
        echo "ERROR: $VAR 환경변수가 설정되지 않았습니다."
        exit 1
    fi
done

echo "> AWS 및 DB 환경변수 로드 완료"

BUILD_PATH=$(ls /home/ec2-user/build/*.jar)
JAR_NAME=$(basename $BUILD_PATH)
echo "> build 파일명: $JAR_NAME"

echo "> build 파일 복사"
DEPLOY_PATH=/home/ec2-user/
cp $BUILD_PATH $DEPLOY_PATH

echo "> springboot-deploy.jar 교체"
CP_JAR_PATH=$DEPLOY_PATH$JAR_NAME
APPLICATION_JAR_NAME=springboot-deploy.jar
APPLICATION_JAR=$DEPLOY_PATH$APPLICATION_JAR_NAME

ln -Tfs $CP_JAR_PATH $APPLICATION_JAR

echo "> 현재 실행중인 애플리케이션 pid 확인"
CURRENT_PID=$(pgrep -f $APPLICATION_JAR_NAME)

if [ -z $CURRENT_PID ]
then
  echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다."
else
  echo "> kill -15 $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi

echo "> $APPLICATION_JAR 배포"
nohup java -Xms2048m -Xmx8192m -jar $APPLICATION_JAR > /dev/null 2> /dev/null < /dev/null &