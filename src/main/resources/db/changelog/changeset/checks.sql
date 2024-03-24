-- Modify the transactions table to add the CHECK constraint
ALTER TABLE transactions
    ADD CONSTRAINT check_sender_not_equal_recipient
        CHECK (sender_id != recipient_id);

-- Modify the debts table to add the CHECK constraint
ALTER TABLE debts
    ADD CONSTRAINT check_sender_not_equal_recipient
        CHECK (sender_id != recipient_id);