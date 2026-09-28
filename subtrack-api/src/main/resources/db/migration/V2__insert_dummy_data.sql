-- ===============================================
-- Users
-- Demo login password for all seeded users: "Password123!" (bcrypt hash below)
-- ===============================================
INSERT INTO users (email, name, password_hash, created_at)
VALUES
  ('alice@example.com', 'Alice', '$2a$10$kWSgg7yHGbWXckaN83z7zuTjPyzgAVmdme3.y2aS.6oimGp22X8yy', NOW()),
  ('bob@example.com', 'Bob', '$2a$10$kWSgg7yHGbWXckaN83z7zuTjPyzgAVmdme3.y2aS.6oimGp22X8yy', NOW()),
  ('carol@example.com', 'Carol', '$2a$10$kWSgg7yHGbWXckaN83z7zuTjPyzgAVmdme3.y2aS.6oimGp22X8yy', NOW()),
  ('dave@example.com', 'Dave', '$2a$10$kWSgg7yHGbWXckaN83z7zuTjPyzgAVmdme3.y2aS.6oimGp22X8yy', NOW()),
  ('eve@example.com', 'Eve', '$2a$10$kWSgg7yHGbWXckaN83z7zuTjPyzgAVmdme3.y2aS.6oimGp22X8yy', NOW());

-- ===============================================
-- Subscriptions
-- ===============================================
INSERT INTO subscriptions (user_id, name, price, billing_cycle, next_payment_date, category, created_at)
VALUES
  -- Alice
  ((SELECT id FROM users WHERE email='alice@example.com'), 'Netflix', 12.99, 'MONTHLY', '2025-11-01', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='alice@example.com'), 'Spotify', 9.99, 'MONTHLY', '2025-11-05', 'Music', NOW()),
  ((SELECT id FROM users WHERE email='alice@example.com'), 'Gym', 45.00, 'MONTHLY', '2025-11-03', 'Health', NOW()),
  ((SELECT id FROM users WHERE email='alice@example.com'), 'Adobe Creative Cloud', 52.99, 'MONTHLY', '2025-11-10', 'Software', NOW()),
  ((SELECT id FROM users WHERE email='alice@example.com'), 'Dropbox', 19.99, 'MONTHLY', '2025-11-15', 'Cloud', NOW()),
  ((SELECT id FROM users WHERE email='alice@example.com'), 'NYTimes', 7.99, 'MONTHLY', '2025-11-12', 'News', NOW()),

  -- Bob
  ((SELECT id FROM users WHERE email='bob@example.com'), 'Netflix', 12.99, 'MONTHLY', '2025-11-02', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='bob@example.com'), 'Hulu', 11.99, 'MONTHLY', '2025-11-08', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='bob@example.com'), 'Gym', 50.00, 'MONTHLY', '2025-11-04', 'Health', NOW()),
  ((SELECT id FROM users WHERE email='bob@example.com'), 'Amazon Prime', 14.99, 'MONTHLY', '2025-11-07', 'Shopping', NOW()),
  ((SELECT id FROM users WHERE email='bob@example.com'), 'Slack', 6.67, 'MONTHLY', '2025-11-09', 'Software', NOW()),
  ((SELECT id FROM users WHERE email='bob@example.com'), 'NYTimes', 7.99, 'MONTHLY', '2025-11-13', 'News', NOW()),

  -- Carol
  ((SELECT id FROM users WHERE email='carol@example.com'), 'Spotify', 9.99, 'MONTHLY', '2025-11-06', 'Music', NOW()),
  ((SELECT id FROM users WHERE email='carol@example.com'), 'HBO Max', 14.99, 'MONTHLY', '2025-11-10', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='carol@example.com'), 'Gym', 45.00, 'MONTHLY', '2025-11-05', 'Health', NOW()),
  ((SELECT id FROM users WHERE email='carol@example.com'), 'Adobe Creative Cloud', 52.99, 'MONTHLY', '2025-11-11', 'Software', NOW()),
  ((SELECT id FROM users WHERE email='carol@example.com'), 'Dropbox', 19.99, 'MONTHLY', '2025-11-16', 'Cloud', NOW()),
  ((SELECT id FROM users WHERE email='carol@example.com'), 'Wall Street Journal', 8.99, 'MONTHLY', '2025-11-14', 'News', NOW()),

  -- Dave
  ((SELECT id FROM users WHERE email='dave@example.com'), 'Netflix', 12.99, 'MONTHLY', '2025-11-03', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='dave@example.com'), 'Spotify', 9.99, 'MONTHLY', '2025-11-07', 'Music', NOW()),
  ((SELECT id FROM users WHERE email='dave@example.com'), 'Gym', 45.00, 'MONTHLY', '2025-11-06', 'Health', NOW()),
  ((SELECT id FROM users WHERE email='dave@example.com'), 'Adobe Creative Cloud', 52.99, 'MONTHLY', '2025-11-12', 'Software', NOW()),
  ((SELECT id FROM users WHERE email='dave@example.com'), 'Dropbox', 19.99, 'MONTHLY', '2025-11-17', 'Cloud', NOW()),
  ((SELECT id FROM users WHERE email='dave@example.com'), 'NYTimes', 7.99, 'MONTHLY', '2025-11-15', 'News', NOW()),

  -- Eve
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Netflix', 12.99, 'MONTHLY', '2025-11-04', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Hulu', 11.99, 'MONTHLY', '2025-11-09', 'Entertainment', NOW()),
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Gym', 50.00, 'MONTHLY', '2025-11-08', 'Health', NOW()),
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Amazon Prime', 14.99, 'MONTHLY', '2025-11-10', 'Shopping', NOW()),
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Slack', 6.67, 'MONTHLY', '2025-11-12', 'Software', NOW()),
  ((SELECT id FROM users WHERE email='eve@example.com'), 'Wall Street Journal', 8.99, 'MONTHLY', '2025-11-16', 'News', NOW());

-- ===============================================
-- Subscription History
-- ===============================================
INSERT INTO subscription_history (subscription_id, user_id, name, price, billing_cycle, category, action_type, previous_value, new_value, changed_at)
SELECT
  id AS subscription_id,
  user_id,
  name,
  price,
  billing_cycle,
  category,
  'INSERT' AS action_type,
  NULL AS previous_value,
  json_build_object(
      'id', id,
      'user_id', user_id,
      'name', name,
      'price', price,
      'billing_cycle', billing_cycle,
      'category', category
  ) AS new_value,
  NOW() AS changed_at
FROM subscriptions;