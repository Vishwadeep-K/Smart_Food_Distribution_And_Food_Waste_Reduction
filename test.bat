@echo off
setlocal
echo ==========================================================================
echo       RUNNING ANNAM SYSTEM AUTOMATED JUNIT TEST SUITE
echo ==========================================================================
call "%~dp0mvnw.cmd" test
endlocal
