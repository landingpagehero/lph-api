To do
=====

Near-term
---------

* Allow landing pages to have prod hosts other than lph-sites. (CNAME for each landing page pointing to *-prod.lph-sites?).
* Authentication.
* Extend audit log to include user who performs action
* "Thank you" email to user who submitted form (e.g. via adding a email.html/email.txt file to the template repository). The email should be sent through customizable SMTP details.
* Improve access to GitHub/GitLab private repos from server.
* Multi-page test (more than 1 page) - add multiple pages to the example template. Also multiple success pages.
* HTTPS support

Not near-term
-------------

* Deployment and viewing form responses need to be permission-based.
* Gitflow workflow with Github style hooks - ie, PR's are automatically created as a unique "dev" environment, allowing account services to test and see changes before they're merged.  Once merged, staging is automatically updated, and can be promoted to production (if you're unsure - I can demo Heroku's GH integration here)
* Auto-deploy to dev enviroment when Git repo changes.
* Tracking JS: ability to track link clicks, modal window clicks.
* AJAX form submissions.
* IDs on forms to recognise which form on the page was submitted.
* Automatic inclusion of Google Analytics (if possible), and automated tests to ensure its included.
* 404 link checker - maybe separate service (Overwatch?). Maybe don't let promotion to production if a link is 404.
* Monitoring of sites - integrate with an Overwatch instance.
* Synchronisation with SFDC (long term)
* SSO with SAML2 (so anyone in twogether can login and view pages)
* Test external assets in pages - ie, videos etc
* Mobile screenshots as well as desktop screenshots (integration with an automated service for screenshoting in multiple browsers?)
* Customizable deployment backend, e.g. deploy to an S3 bucket.
* Penetration testing (long term).
* Copyright notice.
* License (MIT?).
