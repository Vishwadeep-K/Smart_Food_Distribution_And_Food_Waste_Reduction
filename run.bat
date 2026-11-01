@echo off
setlocal
echo ==========================================================================
echo    STARTING ANNAM SMART FOOD DISTRIBUTION & FOOD WASTE REDUCTION SYSTEM
echo ==========================================================================
echo [1/3] Ensuring MySQL database is reachable...
call "%~dp0mvnw.cmd" spring-boot:run
endlocal
