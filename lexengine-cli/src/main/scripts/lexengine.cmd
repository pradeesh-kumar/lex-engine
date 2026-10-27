@echo off
setlocal
set "LEX_ENGINE_JAVA=java"
if defined JAVA_HOME set "LEX_ENGINE_JAVA=%JAVA_HOME%\bin\java.exe"
"%LEX_ENGINE_JAVA%" -jar "%~dp0lex-engine.jar" %*
exit /b %ERRORLEVEL%
