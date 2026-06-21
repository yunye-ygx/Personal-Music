#!/bin/bash
# Migration execution script for V2__add_mood_tags.sql
# This script connects to PostgreSQL and executes the migration

DB_HOST="192.168.100.128"
DB_PORT="5433"
DB_USER="postgres"
DB_NAME="moodtune"

echo "=== MoodTune Migration: Add mood_tags field ==="
echo "Target: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "ERROR: psql command not found."
    echo "Please install PostgreSQL client or use Docker:"
    echo ""
    echo "Using Docker:"
    echo "  docker run --rm -i -v \"\$(pwd):/migration\" postgres:15 \\"
    echo "    psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} \\"
    echo "    -f /migration/V2__add_mood_tags.sql"
    exit 1
fi

# Execute migration
echo "Executing migration..."
PGPASSWORD=postgres psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -f V2__add_mood_tags.sql

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Migration executed successfully"
    echo ""
    echo "Verifying changes..."
    echo ""

    # Verify the table structure
    PGPASSWORD=postgres psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "\d songs"

    echo ""
    echo "Expected output should include:"
    echo "  - Column: mood_tags | text[] |"
    echo "  - Index: idx_songs_mood_tags gin (mood_tags)"
else
    echo ""
    echo "✗ Migration failed"
    exit 1
fi
