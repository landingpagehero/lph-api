To do
=====

* Allow landing pages to have prod hosts other than lph-sites. (CNAME for each landing page pointing to *-prod.lph-sites?)
* Inject tracking Javascript into landing pages.
* Set up developer access via SSH keys.
* "Thank you" email to user who submitted form (e.g. via adding a email.html/email.txt file to the template repository).
* Tracking JS: ability to track link clicks, modal window clicks.
* AJAX form submissions.
* IDs on forms to recognise which form on the page was submitted.
* Automatic inclusion of Google Analytics (if possible), and automated tests to ensure its included
* Multipage test (more than 1 page)
* 404 link checker
* Monitoring of sites 
* Promote to production workflow
* Synchronisation with SFDC (long term)
* SSO with SAML2 (so anyone in twogether can login and view pages)
* Testing external assets in pages - ie, videos etc
* Mobile screenshots as well as desktop screenshots (integration with an automated service for screenshoting in multiple browsers?)
* Gitflow workflow with Github style hooks - ie, PR's are automatically created as a unique "dev" environment, allowing account services to test and see changes before they're merged.  Once merged, staging is automatically updated, and can be promoted to production (if you're unsure - I can demo Heroku's GH integration here)
