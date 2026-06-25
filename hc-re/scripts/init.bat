@echo off
set DB_NAME=hc
set DB_USER=postgres
set PGPASSWORD=123321

psql -U %DB_USER% -d %DB_NAME% -f clean.sql
psql -U %DB_USER% -d %DB_NAME% -f table.sql
psql -U %DB_USER% -d %DB_NAME% -f insert.sql