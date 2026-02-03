INSERT INTO users (email, password) VALUES
                                                    ( 'admin', 'admin_pass'),
                                                    ( 'john_doe', 'user_pass'),
                                                    ( 'jane_smith', 'secure_pass');

INSERT INTO inquiries (email, title, message, created_at) VALUES
                                                              ('support@example.com', 'Login Issue', 'I cannot log in to my account since yesterday.', CURRENT_TIMESTAMP - INTERVAL '2 days'),
                                                              ('info@company.com', 'Partnership Query', 'We are interested in a business partnership.', CURRENT_TIMESTAMP - INTERVAL '1 day'),
                                                              ('customer@gmail.com', 'Refund Request', 'I would like to request a refund for order #12345.', CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('users', 'user_id'), (SELECT MAX(user_id) FROM users));
SELECT setval(pg_get_serial_sequence('inquiries', 'inquiry_id'), (SELECT MAX(inquiry_id) FROM inquiries));