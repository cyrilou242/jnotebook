:;if ! command -v java &>/dev/null; then
:;  echo "java is not found in the path. Java is not installed?"
:;  exit 1
:;fi
:;java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
:;major_version=$(echo "$java_version" | awk -F '.' '{print $1}')
:;if [ "$major_version" -ge 17 ]; then
:;  # do nothing
:;else
:;  echo "java version is $major_version. jnotebook requires java >= 17."
:;  exit 1
:;fi
:;JNOTEBOOK_JAVA_OPTS=${JNOTEBOOK_JAVA_OPTS:-""}
:;exec java $JNOTEBOOK_JAVA_OPTS -jar "$0" "$@"

:; # support for windows is limited - no checks are performed
@echo off
java %JNOTEBOOK_JAVA_OPTS% -jar "%~f0" %*
goto :eof
