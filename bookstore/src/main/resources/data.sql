DELETE
FROM books;

DELETE
FROM authors;

INSERT INTO books (title, description)
VALUES ('R2dbc Revealed ', 'Asynchronous API to connect RDBMS');
