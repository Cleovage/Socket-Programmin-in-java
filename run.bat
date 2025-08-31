@echo off
echo Advanced Socket Programming Project
echo ==================================
echo.

if "%1"=="compile" (
    echo Compiling Java files...
    javac *.java
    if %errorlevel%==0 (
        echo Compilation successful!
    ) else (
        echo Compilation failed!
    )
    goto end
)

if "%1"=="server" (
    echo Starting Advanced Server...
    java Server
    goto end
)

if "%1"=="client" (
    echo Starting Advanced Client...
    java AdvancedClient
    goto end
)

if "%1"=="console" (
    echo Starting Console Client...
    java Client
    goto end
)

if "%1"=="clean" (
    echo Cleaning compiled files...
    del *.class 2>nul
    echo Clean complete!
    goto end
)

echo Usage:
echo   run.bat compile    - Compile all Java files
echo   run.bat server     - Run the advanced GUI server
echo   run.bat client     - Run the advanced GUI client
echo   run.bat console    - Run the console client
echo   run.bat clean      - Remove compiled class files
echo.
echo Example workflow:
echo   1. run.bat compile
echo   2. run.bat server    (in one terminal)
echo   3. run.bat client    (in another terminal)

:end