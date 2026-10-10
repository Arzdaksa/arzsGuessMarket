@echo off
rem Runs Guess Market: starts Tomcat with the server if it is not running yet, then opens one client.
rem Tomcat 9 is taken from the first argument or from CATALINA_HOME:
rem     run.bat C:\path\to\apache-tomcat-9.x
rem Run it again (or client\run-client.bat) to open a client for another user.
setlocal
set "HERE=%~dp0"
if not defined GM_URL set "GM_URL=http://localhost:8080/guess-market"

rem works from the submission folder and from the project root after build.bat
set "WAR=%HERE%guess-market.war"
set "CLIENT=%HERE%client"
if not exist "%WAR%" set "WAR=%HERE%dist\guess-market.war"
if not exist "%CLIENT%\guess-market-client.jar" set "CLIENT=%HERE%dist\client"
if not exist "%CLIENT%\guess-market-client.jar" goto :no_client

call :server_up
if "%UP%"=="1" goto :client

if not "%~1"=="" set "CATALINA_HOME=%~1"
if not defined CATALINA_HOME goto :no_tomcat
if not exist "%CATALINA_HOME%\bin\catalina.bat" goto :bad_tomcat
if not exist "%WAR%" goto :no_war

rem Tomcat needs JAVA_HOME or JRE_HOME; if neither is set, take the java that is on the PATH
if defined JAVA_HOME goto :deploy
if defined JRE_HOME goto :deploy
for /f "delims=" %%j in ('where java 2^>nul') do if not defined JRE_HOME if exist "%%~dpj..\bin\java.exe" set "JRE_HOME=%%~dpj.."

:deploy
echo Deploying guess-market.war to %CATALINA_HOME%\webapps
copy /y "%WAR%" "%CATALINA_HOME%\webapps\" >nul || goto :fail
echo Starting Tomcat in a new window...
start "Guess Market server (Tomcat)" cmd /c ""%CATALINA_HOME%\bin\catalina.bat" run"

set TRIES=0
:wait
call :server_up
if "%UP%"=="1" goto :client
set /a TRIES+=1
if %TRIES% geq 60 goto :timeout
ping -n 2 127.0.0.1 >nul
goto :wait

:client
echo Server is up at %GM_URL%
echo Opening a client...
start "Guess Market client" cmd /c ""%CLIENT%\run-client.bat" %GM_URL%"
exit /b 0

rem the server answers 401 on /poll to a client that is not logged in yet
:server_up
set "UP=0"
for /f %%c in ('curl -s -o nul -m 2 -w "%%{http_code}" "%GM_URL%/poll" 2^>nul') do if "%%c"=="401" set "UP=1"
exit /b 0

:no_client
echo The client folder was not found next to this file.
goto :fail

:no_war
echo guess-market.war was not found next to this file.
goto :fail

:no_tomcat
echo The server is not running and the Tomcat folder is not known.
echo Give the Tomcat 9 folder as an argument:   run.bat C:\path\to\apache-tomcat-9.x
echo or set CATALINA_HOME, or copy guess-market.war into Tomcat's webapps folder and start Tomcat yourself.
goto :fail

:bad_tomcat
echo "%CATALINA_HOME%" does not look like a Tomcat folder, bin\catalina.bat is missing.
goto :fail

:timeout
echo The server did not answer at %GM_URL% after a minute. Check the Tomcat window for errors.
goto :fail

:fail
echo.
pause
exit /b 1
