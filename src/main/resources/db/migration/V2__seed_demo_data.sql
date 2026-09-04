-- Demo data: one department per doctor, plus a handful of patients,
-- so the app has real data to register tokens against out of the box.

INSERT INTO department (name) VALUES
    ('General Medicine'),
    ('Pediatrics'),
    ('Orthopedics'),
    ('Cardiology');

INSERT INTO doctor (name, department_id) VALUES
    ('Dr. Simi',  (SELECT id FROM department WHERE name = 'General Medicine')),
    ('Dr. Noor',  (SELECT id FROM department WHERE name = 'Pediatrics')),
    ('Dr. Shyam', (SELECT id FROM department WHERE name = 'Orthopedics')),
    ('Dr. George', (SELECT id FROM department WHERE name = 'Cardiology'));

INSERT INTO patient (full_name, phone) VALUES
    ('Raj Malhotra',   '9876543210'),
    ('Aryan Kapoor',   '9876543211'),
    ('Jim Carter',     '9876543212'),
    ('Bob Wilson',     '9876543213'),
    ('Salma Sheikh',   '9876543214');
