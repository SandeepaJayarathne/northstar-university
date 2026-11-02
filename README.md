# Northstar University website and PostgreSQL system

This starter site uses HTML/CSS/JavaScript in the browser and a Java backend. Admissions enquiries are stored in a PostgreSQL `admissions` table. The Java server creates the table at startup. The student portal remains a visual demo and does not authenticate users or store course records.

## Run locally

Requires JDK 21 or newer, `curl`, and a PostgreSQL database. Set `DATABASE_URL` to the database connection URL before starting the server. Keep credentials private and never commit them to GitHub.

For Docker deployment, the included Dockerfile downloads the PostgreSQL JDBC driver and builds the Java app. On Render, create a PostgreSQL database and set the web service environment variable `DATABASE_URL` to the database's internal connection URL. Keep the web service and database in the same Render region.

## Files

- `src/main/resources/public/index.html` — page structure and content
- `src/main/resources/public/styles.css` — layout and responsive styling
- `src/main/resources/public/app.js` — menu, modal and form behavior
- `src/main/java/edu/northstar/App.java` — Java HTTP server and PostgreSQL database code
- `Dockerfile` — container build and run configuration

## Before real public use

This is a learning starter, not a production student information system. The portal sign-in is intentionally a demo. Add real authentication, authorization, privacy and retention policies, and operational safeguards before collecting real student data.
