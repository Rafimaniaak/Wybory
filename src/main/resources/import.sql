MERGE INTO users (username, password, role) KEY(username) VALUES
('admin', '$2a$10$7YcKdX.DM7iRmVrbV7nCC.B9MExsLsoem0e7Rs7H/KI1UKLObWSEm', 'ADMIN'),
('user', '$2a$10$7iApbN9Nypj/q1zlWQnsCOcQCExUKK/CBX356ZEjAx1dbEIR0ADlq', 'USER');

INSERT INTO candidates (name, party, votes) VALUES
('Jan Kowalski', 'Partia X', 0),
('Anna Nowak', 'Partia Y', 0);