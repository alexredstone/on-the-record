import cherrypy
from resources.person import PersonResource
from resources.quote import QuoteResource

import os

# Root resource linking to available services
class RootResource:

    @cherrypy.expose
    def index(self):
        return '''
<html>
  <body>
    <ul>
      <li><a href="person">person</a></li>
      <li><a href="quote">quote</a></li>
    </ul>
  </body>
</html>'''

# Construct the RESTful Resource Hierarchy
rootResource = lambda:0
rootResource.api = RootResource()
rootResource.api.person = PersonResource()
rootResource.api.quote = QuoteResource()

# Configure a development server
basedir = os.path.dirname(os.path.abspath(__file__))
conf = {
    'global': {
        'server.socket_host': '0.0.0.0',
        'server.socket_port': 8000,
    },
    '/': {
        'tools.staticdir.on': True,
        'tools.staticdir.dir': os.path.join(basedir, 'ui'),
        'tools.staticdir.index': 'index.html',
    }
}

# Run the server
if __name__ == '__main__':
	cherrypy.quickstart(rootResource, '/', conf)
else:
	application = cherrypy.Application(rootResource, script_name=None, config=conf)
