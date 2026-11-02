#!/usr/bin/env python3
"""Verify KalabanDatabase.MIGRATION_2_3 against a real v2 database without an emulator.

Usage: tools/verify_room_migration.py path/to/kalaban.db

The database (plus -wal/-shm if present) is copied, never modified. The script runs the exact
statements from MIGRATION_2_3_STATEMENTS in KalabanDatabase.kt, then compares the result with a
database built from Room's exported 3.json the way Room validates schemas at startup: columns
(type affinity, NOT NULL, primary key, defaults declared by the entity), foreign keys and
CREATE INDEX indices. It also checks that pre-existing data is unchanged.
"""
import json
import os
import re
import shutil
import sqlite3
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KOTLIN = os.path.join(ROOT, "app/src/main/java/com/kalaban/farshidb2011/data/local/KalabanDatabase.kt")
SCHEMA = os.path.join(ROOT, "app/schemas/com.kalaban.farshidb2011.data.local.KalabanDatabase/3.json")


def migration_statements():
    source = open(KOTLIN, encoding="utf-8").read()
    block = re.search(r"MIGRATION_2_3_STATEMENTS = listOf\((.*?)\n\s*\)\n", source, re.S).group(1)
    return [s.encode().decode("unicode_escape") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', block)]


def affinity(declared):
    t = (declared or "").upper()
    if "INT" in t:
        return "INTEGER"
    if "CHAR" in t or "CLOB" in t or "TEXT" in t:
        return "TEXT"
    if "BLOB" in t or not t:
        return "BLOB"
    if "REAL" in t or "FLOA" in t or "DOUB" in t:
        return "REAL"
    return "NUMERIC"


def describe(db, table):
    cols = {r[1]: (affinity(r[2]), bool(r[3]), r[4], r[5]) for r in db.execute(f"PRAGMA table_info(`{table}`)")}
    fks = sorted((r[2], r[3], r[4], r[5], r[6]) for r in db.execute(f"PRAGMA foreign_key_list(`{table}`)"))
    idx = {}
    for r in db.execute(f"PRAGMA index_list(`{table}`)"):
        if r[3] != "c":
            continue
        idx[r[1]] = (bool(r[2]), [c[2] for c in db.execute(f"PRAGMA index_info(`{r[1]}`)")])
    return cols, fks, idx


def main(path):
    work = tempfile.mkdtemp(prefix="kalaban-migration-")
    db_path = os.path.join(work, "v2.db")
    shutil.copy(path, db_path)
    for ext in ("-wal", "-shm"):
        if os.path.exists(path + ext):
            shutil.copy(path + ext, db_path + ext)

    db = sqlite3.connect(db_path, isolation_level=None)
    db.execute("PRAGMA wal_checkpoint(FULL)")
    assert db.execute("PRAGMA user_version").fetchone()[0] == 2, "expected a version 2 database"
    tables = [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'")]
    before = {t: db.execute(f"SELECT * FROM `{t}` ORDER BY rowid").fetchall()
              for t in tables if t not in ("sqlite_sequence", "room_master_table", "android_metadata")}
    old_cols = {t: [r[1] for r in db.execute(f"PRAGMA table_info(`{t}`)")] for t in before}

    statements = migration_statements()
    db.execute("BEGIN")
    for sql in statements:
        db.execute(sql)
    db.execute("COMMIT")
    db.execute("PRAGMA foreign_keys = ON")
    violations = db.execute("PRAGMA foreign_key_check").fetchall()

    schema = json.load(open(SCHEMA, encoding="utf-8"))["database"]
    fresh = sqlite3.connect(":memory:")
    for entity in schema["entities"]:
        name = entity["tableName"]
        fresh.execute(entity["createSql"].replace("${TABLE_NAME}", name))
        for index in entity.get("indices", []):
            fresh.execute(index["createSql"].replace("${TABLE_NAME}", name))

    problems = []
    for entity in schema["entities"]:
        t = entity["tableName"]
        exp_cols, exp_fks, exp_idx = describe(fresh, t)
        got_cols, got_fks, got_idx = describe(db, t)
        declared_defaults = {f["columnName"]: f.get("defaultValue") for f in entity["fields"]}
        for col, (aff, notnull, default, pk) in exp_cols.items():
            got = got_cols.get(col)
            if got is None:
                problems.append(f"{t}.{col}: missing")
                continue
            if (aff, notnull, pk) != (got[0], got[1], got[3]):
                problems.append(f"{t}.{col}: expected {(aff, notnull, pk)} got {(got[0], got[1], got[3])}")
            if declared_defaults.get(col) is not None and declared_defaults[col] != got[2]:
                problems.append(f"{t}.{col}: default {declared_defaults[col]!r} != {got[2]!r}")
        for extra in set(got_cols) - set(exp_cols):
            problems.append(f"{t}.{extra}: not in entity")
        if exp_fks != got_fks:
            problems.append(f"{t}: foreign keys {got_fks} != {exp_fks}")
        if exp_idx != got_idx:
            problems.append(f"{t}: indices {got_idx} != {exp_idx}")

    for t, rows in before.items():
        cols = ", ".join(f"`{c}`" for c in old_cols[t])
        after = db.execute(f"SELECT {cols} FROM `{t}` ORDER BY rowid").fetchall()
        if after != rows:
            problems.append(f"{t}: existing data changed")

    item_defaults = db.execute(
        "SELECT COUNT(*) FROM items WHERE status != 'ACTIVE' OR pricingMode != 'AUTO' OR dollarLinked != 1 "
        "OR transportCost != 0 OR createdAtEpochMs != updatedAtEpochMs"
    ).fetchone()[0]
    if item_defaults:
        problems.append(f"items: {item_defaults} rows without expected defaults")

    print(f"statements executed: {len(statements)}")
    print(f"tables compared: {len(schema['entities'])}, rows checked: {sum(len(r) for r in before.values())}")
    print(f"foreign key violations: {violations}")
    for p in problems:
        print("PROBLEM:", p)
    ok = not problems and not violations
    print("RESULT:", "OK" if ok else "FAILED")
    shutil.rmtree(work)
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
