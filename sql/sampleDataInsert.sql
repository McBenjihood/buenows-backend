INSERT INTO users (user_id, email, password) VALUES
    ('019c2433-0ff7-7658-b903-03dcdf59db39','admin', 'admin_pass'),
    ('019c2433-0ff7-7854-bde8-42c977c4fa50','john_doe', 'user_pass'),
    ('019c2433-0ff7-7870-a1ed-abd4d943a15c','jane_smith', 'secure_pass');

INSERT INTO inquiries (email, title, message) VALUES
    ('support@example.com', 'Login Issue', 'I cannot log in to my account since yesterday.'),
    ('info@company.com', 'Partnership Query', 'We are interested in a business partnership.'),
    ('customer@gmail.com', 'Refund Request', 'I would like to request a refund for order #12345.');

INSERT INTO refresh_tokens(user_id, token) VALUES
    ('019c2433-0ff7-7658-b903-03dcdf59db39', '9Y3vP7MIPH9Y3vP7MIPH9Y3vP7MIPH9Y3vP7MIPHgkMmtGFe6X'),
    ('019c2433-0ff7-7854-bde8-42c977c4fa50','M7y2EbnqbYM7y2EbnqbYM7y2EbnqbYM7y2EbnqbYM7y2EbnqbY'),
    ('019c2433-0ff7-7870-a1ed-abd4d943a15c','gkMmtGFe6XgkMmtGFe6XgkMmtGFe6XgkMmtGFe6XgkMmtGFe6X');

SELECT setval(pg_get_serial_sequence('users', 'user_id'), (SELECT MAX(user_id) FROM users));
SELECT setval(pg_get_serial_sequence('inquiries', 'inquiry_id'), (SELECT MAX(inquiry_id) FROM inquiries));