echo off
title Prsi Server
reg add HKEY_CURRENT_USER\Console /v VirtualTerminalLevel /t REG_DWORD /d 0x00000001 /f
cls
echo Run this server with -noColor argument to disable colored output on unsupported terminals.
java -jar .\Prsi-1.0-SNAPSHOT.jar
pause