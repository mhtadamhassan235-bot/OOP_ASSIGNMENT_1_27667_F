@echo off
cd /d "%~dp0"
if not exist out call compile.bat
java -cp out com.auca.main.Main
