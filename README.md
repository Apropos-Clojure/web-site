[![CI-Test](https://github.com/Apropos-Clojure/web-site/actions/workflows/ci.yml/badge.svg)](https://github.com/Apropos-Clojure/web-site/actions/workflows/ci.yml)

[![CI-Lint](https://github.com/Apropos-Clojure/web-site/actions/workflows/lint.yml/badge.svg)](https://github.com/Apropos-Clojure/web-site/actions/workflows/lint.yml)

# Apropos Web Site

## Basic Requirements:

**Open access for all**
- Serve general info about the show (purpose, people, places)
- Serve show notes before and after each episode, most recent first

**Restricted access for hosts**
- create / edit show notes per episode

**Content options**
- Static content: whole site generated on each update
- Dynamic content: pages content generated per request

Static content debate:
- content created per update on master branch
- no site security needed, rely on GitHub organization controls
- more complex publishing pipeline (CI/CD style)
- overall less code, more glue scripts
- lower control but faster results

Dynamic content debate:
- content created per POST on the site
- site security needed: integration to GitHub organization controls
- database needed to store the content
- overall more code but with fewer external tools and dependencies
- high control but more effort to create and manage

We prefer dynamic content in this case.

Why? It means that we build more features with Clojure.

This option feels appropriate for Apropos as a vehicle for discussing how to make / do things in Clojure.




