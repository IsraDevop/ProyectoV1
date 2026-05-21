-- Seed data for Yala marketplace
-- Password for all seed users: Test1234 (BCrypt hash)

INSERT INTO categories (name, description) VALUES
    ('Pokémon TCG', 'Pokémon Trading Card Game cards and accessories'),
    ('Funko Pop', 'Funko Pop vinyl figures'),
    ('Comics', 'Comic books and graphic novels')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (name, email, password_hash, dni_verified, reputation, is_verified_seller, role, failed_payments, created_at) VALUES
    ('John Buyer', 'buyer@yala.pe', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewNMnqFV5HxGvV.6', true, 0.0, false, 'USER', 0, NOW()),
    ('Maria Seller', 'seller@yala.pe', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewNMnqFV5HxGvV.6', true, 4.5, true, 'SELLER', 0, NOW()),
    ('Admin Yala', 'admin@yala.pe', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewNMnqFV5HxGvV.6', true, 0.0, true, 'ADMIN', 0, NOW())
ON CONFLICT (email) DO NOTHING;
