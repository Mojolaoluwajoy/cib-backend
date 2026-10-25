-- Postgres doesn't index foreign keys automatically. These back the organization-scoped lookups, the ledger's
-- "latest entry for an account" query, the approval/expiry jobs (status + age) and ending a user's sessions.
create index if not exists idx_account_organization on account (organization_id);
create index if not exists idx_users_organization on users (organization_id);
create index if not exists idx_transaction_source_account on transaction (source_account);
create index if not exists idx_transaction_destination_account on transaction (destination_account);
create index if not exists idx_transaction_status_created_at on transaction (status, created_at);
create index if not exists idx_ledger_entry_account on ledger_entry (account_id, id);
create index if not exists idx_ledger_entry_transaction on ledger_entry (transaction_id);
create index if not exists idx_auth_session_user on auth_session (user_id);
