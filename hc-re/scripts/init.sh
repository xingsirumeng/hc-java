DB_NAME=hc
DB_USER=postgres
PGPASSWORD=88888888

psql -U $DB_USER -d $DB_NAME -f clean.sql
psql -U $DB_USER -d $DB_NAME -f table.sql
psql -U $DB_USER -d $DB_NAME -f insert.sql
