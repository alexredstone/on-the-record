import cherrypy

import json
import urllib2

from solr import SolrConnection

class QuoteResource:

	def __init__(self):
		self.solr = SolrConnection('http://localhost:8983/solr')

	@cherrypy.expose
	def index(self, callback=None, person=None, topic=None):

		filters = []
		filters.append('type:quote')

		if person:
			filters.append('person_t:%s' % person)
		if topic:
			filters.append('quote_t:%s' % topic)

		results = self.solr.query(q = ' AND '.join(filters), rows=100)
		docs = []

		timeline = { 
 	   		  'timeline':
    				{
				        'headline':'OnTheRecord',
				        'type':'default',
					'startDate':'2012,1,1',
					'text':'We help you track quotations from politicians over time',
				}
			}
		for result in results:
			doc = { 
                			"startDate":result['date'].strftime('%Y,%m,%d'),
                			"headline":result['person'],
                			"text":'<a href="' + result['url'] + '">'+result['title'] +'</a>',
                			"asset":
                			{
                    				"media":"<blockquote>\""+result['quote'] + "\"</blockquote>",
                    				"credit":"",
                    				"caption":""
                			}
            			}
			docs.append(doc)
		timeline['timeline']['date'] = docs;
		
		cherrypy.response.headers['Content-Type'] = 'application/json; charset=utf-8'
		return json.dumps(timeline, ensure_ascii=False, indent=4).encode('utf-8')
