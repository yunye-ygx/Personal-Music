# Database Migrations

This directory contains database migration scripts for MoodTune.

## Migration Files

- `V2__add_mood_tags.sql` - Adds mood_tags field and GIN index to songs table

## Execution Instructions

### Prerequisites

Database connection details:
- Host: 192.168.100.128
- Port: 5433
- Database: moodtune
- User: postgres
- Password: postgres

### Method 1: Using psql (if installed)

```bash
cd backend/src/main/resources/db/migration
PGPASSWORD=postgres psql -h 192.168.100.128 -p 5433 -U postgres -d moodtune -f V2__add_mood_tags.sql
```

### Method 2: Using Docker

```bash
cd backend/src/main/resources/db/migration
docker run --rm -i -v "$(pwd):/migration" postgres:15 \
  psql -h 192.168.100.128 -p 5433 -U postgres -d moodtune -f /migration/V2__add_mood_tags.sql
```

### Method 3: Using run_migration.sh

```bash
cd backend/src/main/resources/db/migration
chmod +x run_migration.sh
./run_migration.sh
```

## Verification

After running the migration, verify the changes:

```sql
\d songs
```

Expected output should include:
- Column: `mood_tags | text[] |`
- Index: `idx_songs_mood_tags gin (mood_tags)`

Query the index list:

```sql
\di idx_songs_mood_tags
```

## Rollback (if needed)

```sql
DROP INDEX IF EXISTS idx_songs_mood_tags;
ALTER TABLE songs DROP COLUMN IF EXISTS mood_tags;
```
