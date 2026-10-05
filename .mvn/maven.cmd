REM ============================================================================
REM Organization : Portage Cybertech
REM Project      : Mini OAuth 2.0 platform
REM Purpose      : Project support utility maven: maven.
REM Author       : Sylvose Allogo ^<sylvose.allogo@yahoo.com^>
REM Version      : 1.0.0
REM Date         : 2026-10-04
REM ============================================================================
@echo off
setlocal
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

set "MAVEN_COMMAND="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_COMMAND=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MAVEN_COMMAND if defined M2_HOME if exist "%M2_HOME%\bin\mvn.cmd" set "MAVEN_COMMAND=%M2_HOME%\bin\mvn.cmd"
if not defined MAVEN_COMMAND for %%M in (mvn.cmd) do if not "%%~$PATH:M"=="" set "MAVEN_COMMAND=%%~$PATH:M"
if not defined MAVEN_COMMAND if exist "C:\apache-maven-3.10.0\bin\mvn.cmd" set "MAVEN_COMMAND=C:\apache-maven-3.10.0\bin\mvn.cmd"

if not defined MAVEN_COMMAND (
    echo Maven was not found on PATH. Install Maven or add its bin directory to PATH.
    exit /b 9009
)

if "%~1"=="" (
    call "%MAVEN_COMMAND%" -f "%PROJECT_ROOT%\pom.xml" verify
) else (
    call "%MAVEN_COMMAND%" -f "%PROJECT_ROOT%\pom.xml" %*
)
set "MAVEN_EXIT_CODE=%ERRORLEVEL%"
exit /b %MAVEN_EXIT_CODE%
