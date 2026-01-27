INSERT INTO users (user_id, email, password) VALUES
                                                    (1, 'admin', 'admin_pass'),
                                                    (2, 'john_doe', 'user_pass'),
                                                    (3, 'jane_smith', 'secure_pass');

INSERT INTO user_authorities (user_id, authority) VALUES
                                                      (1, 'ROLE_ADMIN'),
                                                      (1, 'ROLE_USER'),
                                                      (2, 'ROLE_USER'),
                                                      (3, 'ROLE_USER');

INSERT INTO inquiries (email, title, message, created_at) VALUES
                                                              ('support@example.com', 'Login Issue', 'I cannot log in to my account since yesterday.', CURRENT_TIMESTAMP - INTERVAL '2 days'),
                                                              ('info@company.com', 'Partnership Query', 'We are interested in a business partnership.', CURRENT_TIMESTAMP - INTERVAL '1 day'),
                                                              ('customer@gmail.com', 'Refund Request', 'I would like to request a refund for order #12345.', CURRENT_TIMESTAMP);

SELECT setval(pg_get_serial_sequence('users', 'user_id'), (SELECT MAX(user_id) FROM users));
SELECT setval(pg_get_serial_sequence('inquiries', 'inquiry_id'), (SELECT MAX(inquiry_id) FROM inquiries));