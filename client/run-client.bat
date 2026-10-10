@echo off
rem Starts the Guess Market client. Optional argument: the server URL
rem (default http://localhost:8080/guess-market), e.g.  run-client.bat http://192.168.1.20:8080/guess-market
cd /d "%~dp0"
java --module-path lib\javafx --add-modules javafx.controls --enable-native-access=javafx.graphics ^
    -cp "guess-market-client.jar;lib\*" ui.Main %*
if errorlevel 1 pause
