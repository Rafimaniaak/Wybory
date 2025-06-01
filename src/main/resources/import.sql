INSERT INTO USERS (username, password, role, has_voted)
VALUES ('admin', '$2a$10$dj9eHfuT2oAOd4H0LabcZexzBX7MS7/NgfhabawxQpXd57ZAr3nEq', 'ADMIN', FALSE);

INSERT INTO USERS (username, password, role, has_voted)
VALUES ('user', '$2a$10$SvcT2pdsn5ndUfkfSvtFZ.dlYk/jnKfOz/gyq0yQdl58miFuAi23e', 'USER', FALSE);

INSERT INTO CANDIDATE (id, name, party, votes) VALUES (1, 'Jan Kowalski', 'Partia A', 0);
INSERT INTO CANDIDATE (id, name, party, votes) VALUES (2, 'Anna Nowak', 'Partia B', 0);
INSERT INTO CANDIDATE (id, name, party, votes) VALUES (3, 'Jan Tarczynski', 'Partia C', 0);