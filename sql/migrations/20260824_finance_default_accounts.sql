USE medicine_system;

-- 修复历史账本没有付款账户、导致无法登记第一笔流水的问题。
-- 新建账本由后端自动创建默认账户；本脚本只补齐升级前已有的空账本。
INSERT INTO personal_account (
  ledger_id, owner_user_id, account_name, account_type,
  initial_balance, status, create_time, update_time, deleted
)
SELECT l.ledger_id, l.owner_user_id, '日常账户', 'WECHAT',
       0.00, 1, NOW(), NOW(), 0
FROM personal_ledger l
WHERE l.status = 1
  AND l.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM personal_account a
    WHERE a.ledger_id = l.ledger_id
      AND a.owner_user_id = l.owner_user_id
      AND a.status = 1
      AND a.deleted = 0
  );

SELECT ROW_COUNT() AS repaired_ledger_count;
