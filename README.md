# Job Portal

A job portal with separate workflows for **job seekers** and **recruiters**.
Built with Java 17, Spring Boot 3, PostgreSQL, JWT security and a plain HTML/CSS/JavaScript front end.

## Features

**Job seeker**
- Register and log in, save skills and location
- Upload a resume (PDF, DOC or DOCX, max 5 MB)
- Search jobs by skill, location, minimum salary and employment type (paginated)
- Recommended jobs ranked by a **skill-match score**, with a "You lack: ..." hint
- Apply for jobs (duplicate applications are blocked)
- Track every application with a **status timeline**

**Recruiter**
- Register and log in, create a company profile
- Post, edit and close jobs
- View applicants for each job, **sorted by match score**
- Download resumes and move applications through the hiring stages with notes
- **Dashboard** with a hiring funnel and the average match score per job

**Extras**
- A job with a past expiry date is closed automatically (on save, and by a daily scheduled task)
- Duplicate protection, ownership checks and locked final decisions

## Tech stack
Java 17, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA / Hibernate, PostgreSQL, Maven, HTML / CSS / JavaScript

## Run locally
1. Install JDK 17, PostgreSQL and IntelliJ IDEA.
2. Create a PostgreSQL database named `jobportal`.
3. Set the environment variable `DB_PASSWORD` to your PostgreSQL password. In IntelliJ: **Run → Edit Configurations → Environment variables**.
   Optionally set `JWT_SECRET` to a random string of at least 32 characters.
4. Run `JobportalApplication`.
5. Open http://localhost:8080

The tables are created automatically on first start.

## API overview

| Method | Endpoint | Who | Purpose |
|---|---|---|---|
| POST | /api/auth/register, /api/auth/login | public | Register or log in, returns a JWT |
| GET | /api/jobs | public | Search jobs (skill, location, minSalary, type, page, size) |
| GET | /api/jobs/{id} | public | Job details |
| PUT, GET | /api/recruiter/company | recruiter | Company profile |
| POST, GET | /api/recruiter/jobs | recruiter | Create or list own jobs |
| PUT | /api/recruiter/jobs/{id} | recruiter | Edit a job |
| PUT | /api/recruiter/jobs/{id}/close | recruiter | Close a job |
| GET | /api/recruiter/jobs/{id}/applications | recruiter | Applicants, best match first |
| PUT | /api/recruiter/applications/{id}/status | recruiter | Change status with a note |
| GET | /api/recruiter/applications/{id}/resume | recruiter | Download a resume |
| GET | /api/recruiter/dashboard | recruiter | Funnel and per-job statistics |
| PUT | /api/candidate/profile | candidate | Save skills and location |
| POST | /api/candidate/resume | candidate | Upload a resume |
| GET | /api/candidate/jobs/recommended | candidate | Jobs ranked by match score |
| POST | /api/candidate/jobs/{id}/apply | candidate | Apply for a job |
| GET | /api/candidate/applications | candidate | My applications |
| GET | /api/candidate/applications/{id}/timeline | candidate | Status history |

Example requests are in [docs/api-examples.md](docs/api-examples.md).

## More
See [ARCHITECTURE.md](ARCHITECTURE.md) for the design decisions and the ER diagram.