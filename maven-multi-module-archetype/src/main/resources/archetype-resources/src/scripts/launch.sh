#!/usr/bin/env bash

# DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

SOURCE="${BASH_SOURCE[0]}"
while [[ -h "$SOURCE" ]]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ ${SOURCE} != /* ]] && SOURCE="$DIR/$SOURCE"
done

DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

APP_HOME=$(cd ${DIR} && cd .. && pwd)
APP_DIST_DIR=$(cd ${APP_HOME} && cd ../.. && pwd)

APP_NAME="${APPLICATION_NAME}"
APP_EXEC="${BUILD_FINAL_NAME}"
SERVER_URL="http://localhost:${SERVER_PORT}"

LOG_DIR="${prop.log.dir}"

ENABLE_SKYWALKING="${ENABLE_APM}"
SKYWALKING_COLLECTOR_BACKEND_SERVICE=${SKYWALKING_COLLECTOR_SERVICE}
SKYWALKING_AGENT_SERVICE_NAME=${APPLICATION_NAME}
SKYWALKING_LOGGING_LEVEL=WARN

[[ -z "${APP_NAME}" ]] && APP_NAME="app"
[[ -z "${APP_EXEC}" ]] && APP_EXEC="app"

APP_EXEC_DIR=${APP_HOME}/bin
APP_EXEC_PATH=${APP_EXEC_DIR}/${APP_EXEC}
APP_PID_DIR=${APP_DIST_DIR}/run
APP_PID_FILE=${APP_PID_DIR}/${APP_EXEC}.pid

SKYWALKING_AGENT_DIR=${APP_DIST_DIR}/skywalking-agent

[[ ! -d ${APP_PID_DIR} ]] && mkdir -p ${APP_PID_DIR}

START_WAIT_TIME=180 # 180*1=180s
STOP_WAIT_TIME=30

ARGS="$2"

CMD_USAGE="Usage: $0 {\033[1;31mstart\033[0m\033[1m|\033[1;31mstop\033[0m\033[1m|\033[1;31mkill\033[0m\033[1m|\033[1;31mstatus\033[0m\033[1m|\033[1;31mrestart\033[0m\033[1m|\033[1;31mps\033[0m\033[1m}"

# ANSI Colors
echoRed() { echo $'\e[0;31m'"$1"$'\e[0m'; }
echoGreen() { echo $'\e[0;32m'"$1"$'\e[0m'; }
echoYellow() { echo $'\e[0;33m'"$1"$'\e[0m'; }

## Adjust memory settings if necessary
JVM_OPTS="${prop.app.java-opts}"
[[ -z "${JVM_OPTS}" ]] && JVM_OPTS="-Xms1024m -Xmx1024m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:NewSize=384m -XX:MaxNewSize=384m -XX:SurvivorRatio=8"

export JAVA_OPTS="-Dsun.misc.URLClassPath.disableJarChecking=true"
export JAVA_OPTS="$JAVA_OPTS $JVM_OPTS"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+ScavengeBeforeFullGC -XX:+UseCMSCompactAtFullCollection"
export JAVA_OPTS="$JAVA_OPTS -XX:+CMSParallelRemarkEnabled -XX:CMSFullGCsBeforeCompaction=9 -XX:CMSInitiatingOccupancyFraction=60"
export JAVA_OPTS="$JAVA_OPTS -XX:+CMSClassUnloadingEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSPermGenSweepingEnabled"
export JAVA_OPTS="$JAVA_OPTS -XX:+ExplicitGCInvokesConcurrent -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationConcurrentTime"
export JAVA_OPTS="$JAVA_OPTS -XX:+PrintHeapAtGC -XX:+HeapDumpOnOutOfMemoryError -Duser.timezone=Asia/Shanghai"
export JAVA_OPTS="$JAVA_OPTS -Dclient.encoding.override=UTF-8 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
export JAVA_OPTS="$JAVA_OPTS -Xloggc:$LOG_DIR/heap/gc.log -XX:HeapDumpPath=$LOG_DIR/heap/HeapDumpOnOutOfMemoryError/"

if [[ "$ENABLE_SKYWALKING" == "true" ]] && [[ -d "$SKYWALKING_AGENT_DIR" ]]; then
    echoGreen "SkyWalking agent installed and enabled."

    export JAVA_OPTS="$JAVA_OPTS -javaagent:$SKYWALKING_AGENT_DIR/skywalking-agent.jar"
    export JAVA_OPTS="$JAVA_OPTS -Dskywalking.agent.service_name=$SKYWALKING_AGENT_SERVICE_NAME"
    export JAVA_OPTS="$JAVA_OPTS -Dskywalking.collector.backend_service=$SKYWALKING_COLLECTOR_BACKEND_SERVICE"
    export JAVA_OPTS="$JAVA_OPTS -Dskywalking.agent.service_name=$SKYWALKING_AGENT_SERVICE_NAME"
    export JAVA_OPTS="$JAVA_OPTS -Dskywalking.logging.level=$SKYWALKING_LOGGING_LEVEL"
fi

if [[ ! -f "${APP_EXEC_PATH}.jar" ]]; then
    APP_EXEC_DIR=${APP_HOME}/lib
    APP_EXEC_PATH=${APP_EXEC_DIR}/${APP_EXEC}
fi

isRunning() { ps -p "$1" &> /dev/null; }

# echo "[INFO] ---- APP_EXEC_PATH: ${APP_EXEC_PATH}.jar"

[[ -n "${ARGS}" ]] && echoGreen "[INFO] ---- args: ${ARGS}"

getPid() {
    declare -i appPid=0

    if [[ ! -f "${APP_PID_FILE}" ]]; then
        PID="$(ps -ef | grep "${APP_EXEC_PATH}" | grep -v grep | grep -v kill | awk '{print $2}' | xargs)";
        if [[ -z "${PID}" ]]; then
            appPid=0
        else
            appPid=${PID}
        fi
    else
        read -r pid < ${APP_PID_FILE}
        appPid=${pid}
    fi

    # appPid=$(echo "${appPid}" | xargs)

    if [[ -n "${appPid}" ]] && [[ ${appPid} -gt 0 ]]; then
        isRunning ${appPid} || { echo 0; return 0; }
        echo ${appPid}
	else
	    echo 0
	fi
}

checkPidAlive() {
    if [[ ! -f "${APP_PID_FILE}" ]]; then
        printf "\n"
        echoYellow "No pid file found, startup may failed. Please check logs for more information!"
        exit 1;
    else
        read -r pid < ${APP_PID_FILE}
        if [[ "$pid" -eq 0 ]]; then
            printf "\n"
            echoYellow "pid - $pid just quit unexpectedly. Please check logs for more information!"
            exit 1;
        else
            return 0
        fi
    fi
}

check_status() {
	PID=$(getPid)

	if [[ -n "${PID}" ]] && [[ ${PID} -gt 0 ]]; then
	    return 1;
	else
	    return 0;
	fi
}

start() {
    check_status

    if [[ $? -eq 1 ]]; then
        echoGreen "[INFO] ---- ${APP_NAME} already started."
        return 0;
    fi

    echo "[INFO] ---- Starting ${APP_NAME}..."

    cd ${APP_EXEC_DIR}
    nohup java ${JAVA_OPTS} -jar ${APP_EXEC_PATH}.jar ${ARGS} >/dev/null 2>&1 & pid=$!
    # $(nohup java -jar ${APP_EXEC_PATH}.jar >catalina.out 2>&1 &)
    echo ${pid} > ${APP_PID_FILE}

    declare -i counter=0
    declare -i max_counter=${START_WAIT_TIME}
    declare -i total_time=0

    until [[ (( counter -ge max_counter )) || "$(curl -X GET --silent --connect-timeout 1 --max-time 2 --head ${SERVER_URL} | grep "HTTP")" != "" ]];
    do
        printf "."
        counter+=1
        sleep 1

        checkPidAlive
    done

    total_time=counter*1
    if [[ (( counter -ge max_counter )) ]]; then
        printf "\n"
        echoRed "[ERROR] ---- $(date) Server failed to start in $total_time seconds!"
        rm -rf ${APP_PID_FILE}
        return 1;
    fi

    printf "\n"
    echoGreen "[INFO] ---- $(date) Server started in $total_time seconds!"
    return 0;
}

stop() {
    PID=$(getPid)

    if [[ -z "${PID}" ]] || [[ ${PID} -le 0 ]]; then
        echoYellow "[INFO] ---- ${APP_NAME} is not running.";
        return 0;
    fi

    echo "[INFO] ---- Stopping ${APP_NAME}..."
    kill ${PID} &> /dev/null || { printf "\n"; echoRed "[ERROR] ---- Unable to kill process ${PID}"; return 1; }

    for i in $(seq 1 ${STOP_WAIT_TIME}); do
        printf "."

        isRunning "${PID}" || {
            printf "\n"
            echoGreen "[INFO] ---- [${PID}] - ${APP_NAME} stopped.";
            rm -rf ${APP_PID_FILE};
            return 0;
        }

        [[ ${i} -eq STOP_WAIT_TIME/2 ]] && kill "${PID}" &> /dev/null
        sleep 1
    done

    force_stop "${PID}"
    return $?;
}

force_stop() {
  kill -9 "$1" &> /dev/null || { printf "\n"; echoRed "Unable to kill process $1"; return 1; }

  for i in $(seq 1 ${STOP_WAIT_TIME}); do
    printf "."

    isRunning "$1" || {
        printf "\n"
        echoGreen "[INFO] ---- [$1] - ${APP_NAME} stopped.";
        rm -rf ${APP_PID_FILE};
        return 0;
    }

    [[ ${i} -eq STOP_WAIT_TIME/2 ]] && kill -9 "$1" &> /dev/null
    sleep 1
  done

  printf "\n"
  echoRed "Unable to kill process $1";
  return 1;
}

status() {
    PID=$(getPid)

    if [[ -n "${PID}" ]] && [[ ${PID} -gt 0 ]]; then
	    echoGreen "[INFO] ---- [${PID}] ${APP_NAME} is running."
	else
	    echoYellow "[INFO] ---- ${APP_NAME} is not running."
	fi

    return 0;
}

terminate() {
    PID=$(getPid)

    if [[ -z "${PID}" ]] || [[ ${PID} -le 0 ]]; then
        echoYellow "[INFO] ---- ${APP_NAME} is not running.";
        return 0
    fi

    force_stop "${PID}"
    return $?
}

show() {
    export GREP_OPTIONS='--color=always' GREP_COLOR='1;31'
    ps -ef | grep "${APP_EXEC_PATH}" | grep -v grep | grep -v kill
    return 0
}

case $1 in
    start)
        start; exit $?;;
    stop)
        stop; exit $?;;
    restart)
        stop && start; exit $?;;
    status)
        status; exit $?;;
    kill)
        terminate; exit $?;;
    ps)
        show; exit $?;;
    *)
        echo -e >&2 ${CMD_USAGE}; exit $?;;
esac

exit 0
