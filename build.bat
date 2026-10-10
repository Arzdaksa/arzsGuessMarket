@echo off
rem Builds dist\guess-market.war (server, deploy to Tomcat 9 webapps) and dist\client\ (JavaFX client + run-client.bat).
rem Needs a JDK on PATH and the JavaFX SDK; override its location with:  set JAVAFX_LIB=C:\path\to\javafx-sdk\lib
setlocal
cd /d "%~dp0"

if not defined JAVAFX_LIB set "JAVAFX_LIB=%USERPROFILE%\Documents\javafx-sdk-26.0.2\lib"
if not exist "%JAVAFX_LIB%" (
    echo JavaFX SDK lib folder not found: "%JAVAFX_LIB%"
    echo Set JAVAFX_LIB to the lib folder of your JavaFX SDK and run again.
    exit /b 1
)

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\war\WEB-INF\classes\engine build\war\WEB-INF\lib build\client\ui\fx dist\client\lib\javafx || goto :fail

echo [1/4] Compiling server (engine + shared DTOs + servlets)...
rem --release 17 so the war runs on any Tomcat JVM from Java 17 up; lib\provided is Tomcat's own servlet API
javac --release 17 -encoding UTF-8 -cp "lib\*;lib\provided\*" -sourcepath "engine\src;shared\src;server\src" ^
    -d build\war\WEB-INF\classes server\src\server\*.java || goto :fail

echo [2/4] Packaging guess-market.war...
copy /y engine\src\engine\GM-EX3-Schema.xsd build\war\WEB-INF\classes\engine\ >nul || goto :fail
copy /y server\web\WEB-INF\web.xml build\war\WEB-INF\ >nul || goto :fail
copy /y lib\*.jar build\war\WEB-INF\lib\ >nul || goto :fail
jar --create --file dist\guess-market.war -C build\war . || goto :fail

echo [3/4] Compiling client (shared DTOs + JavaFX UI, no engine)...
javac -encoding UTF-8 -cp "lib\gson-2.11.0.jar;%JAVAFX_LIB%\*" -sourcepath "shared\src;client\src" ^
    -d build\client client\src\ui\*.java client\src\ui\fx\*.java client\src\ui\net\*.java || goto :fail

echo [4/4] Packaging client...
copy /y client\src\ui\fx\theme.css build\client\ui\fx\ >nul || goto :fail
jar --create --file dist\client\guess-market-client.jar --main-class ui.Main -C build\client . || goto :fail
copy /y lib\gson-2.11.0.jar dist\client\lib\ >nul || goto :fail
copy /y "%JAVAFX_LIB%\javafx-base-*.jar" dist\client\lib\javafx\ >nul || goto :fail
copy /y "%JAVAFX_LIB%\javafx-graphics-*.jar" dist\client\lib\javafx\ >nul || goto :fail
copy /y "%JAVAFX_LIB%\javafx-controls-*.jar" dist\client\lib\javafx\ >nul || goto :fail
copy /y client\run-client.bat dist\client\ >nul || goto :fail

echo.
echo Done.
echo   Server: dist\guess-market.war   -^> copy into ^<tomcat^>\webapps
echo   Client: dist\client\run-client.bat
exit /b 0

:fail
echo.
echo BUILD FAILED
exit /b 1
