@echo off
setlocal
if exist "C:\Program Files\Java\jdk-22" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-22"
)
set "MAVEN_HOME=%~dp0maven\apache-maven-3.9.9"
"%MAVEN_HOME%\bin\mvn.cmd" %*
