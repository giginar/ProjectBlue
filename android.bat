@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\android.ps1" %*
exit /b %ERRORLEVEL%

