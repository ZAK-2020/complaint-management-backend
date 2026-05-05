@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.11_windows-x64_bin\jdk-17.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using JAVA_HOME=%JAVA_HOME%
java -version

call "%~dp0mvnw.cmd" spring-boot:run
