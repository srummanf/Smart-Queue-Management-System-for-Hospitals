-- Demo login accounts. Passwords are BCrypt hashes of "<username>123"
-- (e.g. staff/staff123, simi/simi123) — fine for a local portfolio demo,
-- not meant to be production credentials.

INSERT INTO staff_user (username, password_hash, role, doctor_id) VALUES
    ('staff',  '$2a$10$M6u17MPGRf3T/yqnLsv2YenBZjaJ2ctBUwdIGxPWlne5sKHeq9PgG', 'STAFF', NULL),
    ('simi',   '$2a$10$kVIxVPsjRo0YmWIPp6S5renG7tIyp6L9NmMl9opS2DEtnKi/NuyAe', 'DOCTOR', (SELECT id FROM doctor WHERE name = 'Dr. Simi')),
    ('noor',   '$2a$10$5fBkhQr57g5JI/S/XNqLN.OkoAyG1zkKydpV/mRwx6p673cQK9Zi6', 'DOCTOR', (SELECT id FROM doctor WHERE name = 'Dr. Noor')),
    ('shyam',  '$2a$10$EE/NgmNDS9gPi3MXqu8theLpeXN8Zz4//y4gCsYBiv3eV2FwZ32gu', 'DOCTOR', (SELECT id FROM doctor WHERE name = 'Dr. Shyam')),
    ('george', '$2a$10$B8pZTEbSRm6mCfdMZplHoOUBlNST5MzT0LGl.pc3XO9y7BU4c5M/6', 'DOCTOR', (SELECT id FROM doctor WHERE name = 'Dr. George'));
