-- Test data for integration tests
-- This file can be used with @Sql annotation to populate test database

-- Insert test reason dictionary entries
INSERT INTO reason_dictionary (id, description) VALUES (1, 'Парковка заблокирована');
INSERT INTO reason_dictionary (id, description) VALUES (2, 'ДТП');
INSERT INTO reason_dictionary (id, description) VALUES (3, 'Фары включены');
INSERT INTO reason_dictionary (id, description) VALUES (4, 'Другое');

-- Insert test users (if user table exists)
-- INSERT INTO users (id, telephone, role) VALUES (1, '79001234567', 'ROLE_USER');
-- INSERT INTO users (id, telephone, role) VALUES (2, '79009876543', 'ROLE_ADMIN');

-- Insert test QR codes
INSERT INTO qrs (id, batch_id, name, printed, status, created_date, user_id)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 1, 'Test QR 1', false, 'NEW', CURRENT_TIMESTAMP, NULL);

INSERT INTO qrs (id, batch_id, name, printed, status, created_date, user_id)
VALUES ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 1, 'Active QR', false, 'ACTIVE', CURRENT_TIMESTAMP, 1);

INSERT INTO qrs (id, batch_id, name, printed, status, created_date, user_id)
VALUES ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 1, 'Temporary QR', false, 'TEMPORARY', CURRENT_TIMESTAMP, 2);

-- Insert test notifications
INSERT INTO notifications (id, qr_id, reason_id, text, created_date, status, sender_id, visitor_id)
VALUES ('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 1, NULL, CURRENT_TIMESTAMP, 'UNREAD', NULL, 'visitor123');

INSERT INTO notifications (id, qr_id, reason_id, text, created_date, status, sender_id, visitor_id)
VALUES ('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 2, 'Custom message', CURRENT_TIMESTAMP, 'READ', 2, NULL);
