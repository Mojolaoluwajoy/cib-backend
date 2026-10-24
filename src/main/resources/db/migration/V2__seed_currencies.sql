-- Supported currencies, all inactive until a super admin enables them (formerly data.sql, which ran on every start).
INSERT INTO currency(name,code,symbol,status) VALUES
('Nigerian Naira', 'NGN', '#','INACTIVE'),
('US Dollar','USD','$','INACTIVE'),
('Euro','EUR','E','INACTIVE'),
('Canadian Dollar','CAD','C$','INACTIVE'),
('Australian Dollar','AUD','A$','INACTIVE')
ON CONFLICT (code) DO NOTHING;
