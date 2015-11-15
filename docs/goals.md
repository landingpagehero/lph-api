LPH
===

Goals
-----

* Hosts simple static landing pages.
* Allows landing page forms to be submitted and stored in a MongoDB database.
* Allows each site to be deployed to it's own Digital Ocean droplet easily.
* Allows deployment through a Jenkins job.
* Tracks everything to a MongoDB database - including downloads (e.g. PDFs in the landing page) and page views.
* Injects tracking Javascript into the pages automatically.
* Injects configured dev SSH keys into the server and creates individual user accounts for them.
* Multiple environments per landing page - e.g. stage, prod.
* Deployments are logged for auditing for ISO-27001.
* Super-easy to code landing pages, without having to worry about build tools (minification, etc.) - that's all handled automatically.

Business case
-------------

* Easy deployments via a web page - non-devs can deploy.
* Easy server set up via Puppet - no need for an infrastructure engineer.
* No need to write a backend to store form data - a backend already exists.
* Save time coding landing pages - build tools all handled automatically.
