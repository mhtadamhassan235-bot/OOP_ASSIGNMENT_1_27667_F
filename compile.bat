@echo off
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if %errorlevel%==0 echo Compilation OK - classes are in the out folder
del sources.txt
