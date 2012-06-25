from dateutil import parser
import json
import pika
import solr
import md5

s = solr.SolrConnection('http://localhost:8983/solr')

def callback(channel, method, properties, body):

        try:
		js = json.loads(body)
        except:
                print '- Error in json decoding'
                return

	print '* Writing document to Solr ...',

	docs = []
	try:
		doc = {
			'id': js['url'],
			'type': 'article',

			'url': js['url'],
			'title': js['title'],
			'date': parser.parse(js['date']),
			'content': js['content']
		}
	except:
		print '- Error reading article contents, skipping'
		return

	if js.has_key('quotes'):
		try:
			for quote in js['quotes']:
				doc = {
					'id': md5.md5(quote['person'] + quote['quote']).hexdigest() + '_' + js['url'],
					'type': 'quote',

					'quote': quote['quote'],
					'person': quote['person'],
                        		'url': js['url'],
                        		'title': js['title'],
                        		'date': parser.parse(js['date']),
                        		'content': js['content']
				}
				docs.append(doc)
		except:
			print '- Error reading quote pairs, skipping article'
			return

	s.add_many(docs)
	s.commit()

	print 'done'

print '* Receiving articles from RabbitMQ'

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()
channel.queue_declare(queue='quotes')

while True:
	channel.basic_consume(callback, queue='quotes', no_ack=True)
