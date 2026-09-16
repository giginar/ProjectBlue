@echo off
setlocal
rem No arguments: double-click friendly; build everything and open the output folder.
if not "%~1"=="" goto arguments
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\package.ps1" -OpenOutput
set "result=%ERRORLEVEL%"
pause
exit /b %result%
:arguments
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\package.ps1" %*
exit /b %ERRORLEVEL%
