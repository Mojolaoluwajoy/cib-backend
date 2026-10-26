-- V2 seeded placeholder symbols ('#' for the naira, 'E' for the euro); only those placeholders are replaced.
UPDATE currency SET symbol = '₦' WHERE code = 'NGN' AND symbol = '#';
UPDATE currency SET symbol = '€' WHERE code = 'EUR' AND symbol = 'E';
