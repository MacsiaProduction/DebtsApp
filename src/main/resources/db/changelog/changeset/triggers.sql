-- Create the trigger function to delete expired tokens
CREATE OR REPLACE FUNCTION delete_expired_tokens()
    RETURNS TRIGGER AS
$$
BEGIN
    DELETE FROM active_session_tokens
    WHERE expiration_time <= NOW();
    RETURN NULL;
END;
$$
    LANGUAGE plpgsql;

-- Create the trigger to execute the function before each delete operation on the table
CREATE TRIGGER expire_tokens_trigger
    BEFORE DELETE ON active_session_tokens
    FOR EACH ROW
EXECUTE FUNCTION delete_expired_tokens();

-- Create the trigger function to delete rows with zero sum
CREATE OR REPLACE FUNCTION delete_zero_sum_debts()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.sum = 0 THEN
        DELETE FROM debts
        WHERE sender_id = NEW.sender_id
          AND recipient_id = NEW.recipient_id
          AND chat_id = NEW.chat_id;
    END IF;
    RETURN NEW;
END;
$$
    LANGUAGE plpgsql;

-- Create the trigger to execute the function after each update operation on the table
CREATE TRIGGER check_zero_sum_debts_trigger
    AFTER UPDATE ON debts
    FOR EACH ROW
EXECUTE FUNCTION delete_zero_sum_debts();