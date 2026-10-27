@echo off
setlocal
call mvn -f "%~dp0..\pom.xml" -pl lexengine-cli -am %* package
exit /b %ERRORLEVEL%
