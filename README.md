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
- no site security needed, rely on github controls
- more complex publishing pipeline (CI/CD style)
- overall less code, more glue scripts
- lower control but faster results

Dynamic content debate:
- content created per POST on the site
- site security needed (eg integration to github controls)
- database needed to store the content
- overall more code and with fewer dependencies
- high control but more effort to create and manage

I would prefer dynamic content. Why? It means that more features are built with Clojure. That feels more appropriate for Apropos as a vehicle for discussing how to make / do things in Clojure.




