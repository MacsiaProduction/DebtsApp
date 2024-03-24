-- Add foreign key constraint for sender_id in transactions table
ALTER TABLE transactions
    ADD CONSTRAINT fk_sender_id
        FOREIGN KEY (sender_id)
            REFERENCES users(user_id);

-- Add foreign key constraint for recipient_id in transactions table
ALTER TABLE transactions
    ADD CONSTRAINT fk_recipient_id
        FOREIGN KEY (recipient_id)
            REFERENCES users(user_id);

-- Add foreign key constraint for sender_id in debts table
ALTER TABLE debts
    ADD CONSTRAINT fk_debts_sender_id
        FOREIGN KEY (sender_id)
            REFERENCES users(user_id);

-- Add foreign key constraint for recipient_id in debts table
ALTER TABLE debts
    ADD CONSTRAINT fk_debts_recipient_id
        FOREIGN KEY (recipient_id)
            REFERENCES users(user_id);