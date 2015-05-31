@Echo off
REM This file will start the TrekBuddy Atlas Creator with custom memory settings for
REM the JVM. With the below settings the heap size (Available memory for the application)
REM will range up to 512 megabyte.

start javaw.exe -Xms64M -Xmx512M -jar Mobile_Atlas_Creator.jar