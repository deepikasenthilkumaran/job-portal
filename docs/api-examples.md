# API examples

Base URL: `http://localhost:8080`

For protected endpoints, send the header `Authorization: Bearer <token>`.
The token comes from the register or login response.

## 1. Register a recruiter
`POST /api/auth/register`
```json
{ "fullName": "Ravi HR", "email": "ravi@acme.com", "password": "secret123", "role": "RECRUITER" }
```

## 2. Create the company profile (recruiter token)
`PUT /api/recruiter/company`
```json
{ "name": "Acme Tech", "description": "Product company", "website": "https://acme.com", "location": "Coimbatore" }
```

## 3. Post a job (recruiter token)
`POST /api/recruiter/jobs`
```json
{
  "title": "Java Backend Developer",
  "description": "Build REST APIs with Spring Boot",
  "location": "Coimbatore",
  "minSalary": 600000,
  "maxSalary": 1200000,
  "employmentType": "FULL_TIME",
  "requiredSkills": "java, spring boot, postgresql, git",
  "expiresAt": "2027-12-31"
}
```

## 4. Register a candidate
`POST /api/auth/register`
```json
{ "fullName": "Deepika", "email": "deepika@gmail.com", "password": "secret123", "role": "CANDIDATE" }
```

## 5. Save skills (candidate token)
`PUT /api/candidate/profile`
```json
{ "skills": "Java, Spring Boot, Git, Docker", "location": "Coimbatore" }
```

## 6. Upload a resume (candidate token)
`POST /api/candidate/resume`
Body type: form-data, key `file` (type File), a PDF, DOC or DOCX under 5 MB.

## 7. Search jobs (no token)
`GET /api/jobs?skill=java&location=coimbatore&minSalary=500000&type=FULL_TIME`

## 8. Recommended jobs with match score (candidate token)
`GET /api/candidate/jobs/recommended`

## 9. Apply (candidate token)
`POST /api/candidate/jobs/1/apply`
```json
{ "coverNote": "I enjoy backend development and want to grow with Acme." }
```

## 10. View applicants, best match first (recruiter token)
`GET /api/recruiter/jobs/1/applications`

## 11. Change the status (recruiter token)
`PUT /api/recruiter/applications/1/status`
```json
{ "status": "SHORTLISTED", "note": "Strong backend skills" }
```
Statuses: APPLIED, UNDER_REVIEW, SHORTLISTED, INTERVIEW, OFFERED, REJECTED.
OFFERED and REJECTED are final.

## 12. Track the status (candidate token)
`GET /api/candidate/applications`
`GET /api/candidate/applications/1/timeline`

## 13. Recruiter dashboard (recruiter token)
`GET /api/recruiter/dashboard`

## Expected error cases
| Request | Result |
|---|---|
| Candidate token on a `/api/recruiter/...` URL | 403 |
| Applying twice to the same job | 409 "You already applied to this job" |
| Applying without a resume | 400 "Upload your resume before applying" |
| Changing the status of an OFFERED or REJECTED application | 400 "Application is already finalized" |