import cherrypy

import json
import urllib2

from solr import SolrConnection

class PersonResource:

	def __init__(self):
		self.solr = SolrConnection('http://localhost:8983/solr')

	@cherrypy.expose
	def index(self):

		results = self.solr.query('*:*', facet='true', facet_field='person')

		for person in results.facet_counts[u'facet_fields'][u'person']:

			print person

		cherrypy.response.headers['Content-Type'] = 'application/json; charset=utf-8'
		return json.dumps({
			'test': 'test',
			'data': 'data'
		}, ensure_ascii=False, indent=4).encode('utf-8')
