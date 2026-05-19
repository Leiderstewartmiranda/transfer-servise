-- Cuentas de prueba para desarrollo local
INSERT INTO accounts (id, owner_name, balance, currency, status, created_at)
VALUES
    ('a1b2c3d4-0000-0000-0000-000000000001', 'Juan Pérez',   10000.00, 'COP', 'ACTIVE', CURRENT_TIMESTAMP),
    ('a1b2c3d4-0000-0000-0000-000000000002', 'María García',  5000.00, 'COP', 'ACTIVE', CURRENT_TIMESTAMP),
    ('a1b2c3d4-0000-0000-0000-000000000003', 'Carlos López',  2500.00, 'COP', 'ACTIVE', CURRENT_TIMESTAMP),
    ('a1b2c3d4-0000-0000-0000-000000000004', 'Ana Martínez',     0.00, 'COP', 'ACTIVE', CURRENT_TIMESTAMP);
