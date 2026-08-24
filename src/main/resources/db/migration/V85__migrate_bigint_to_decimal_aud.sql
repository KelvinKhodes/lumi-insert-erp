
ALTER TABLE products_aud ALTER COLUMN base_price TYPE NUMERIC(19, 4) USING base_price::numeric;
ALTER TABLE products_aud ALTER COLUMN sell_price TYPE NUMERIC(19, 4) USING sell_price::numeric;
ALTER TABLE products_aud ALTER COLUMN stock_quantity TYPE NUMERIC(19, 4) USING stock_quantity::numeric;
ALTER TABLE products_aud ALTER COLUMN stock_minimum TYPE NUMERIC(19, 4) USING stock_minimum::numeric;

ALTER TABLE supply_items_aud ALTER COLUMN quantity TYPE NUMERIC(19, 4) USING quantity::numeric;
ALTER TABLE supply_items_aud ALTER COLUMN price TYPE NUMERIC(19, 4) USING price::numeric;

ALTER TABLE supply_payments_aud ALTER COLUMN total_payment TYPE NUMERIC(19, 4) USING total_payment::numeric;

ALTER TABLE transaction_items_aud ALTER COLUMN quantity TYPE NUMERIC(19, 4) USING quantity::numeric;
ALTER TABLE transaction_items_aud ALTER COLUMN price TYPE NUMERIC(19, 4) USING price::numeric;

ALTER TABLE transaction_payments_aud ALTER COLUMN total_payment TYPE NUMERIC(19, 4) USING total_payment::numeric;