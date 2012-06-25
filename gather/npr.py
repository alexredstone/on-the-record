from urllib2 import urlopen
from dateutil import parser
from datetime import timedelta
import json
import pika

def index_story(story):

	# Skip unlinked articles
	if not story.has_key('link'):
		return

	# Skip untitled articles
	if not story.has_key('title'):
		return

	# Skip non-text articles
	if not story.has_key('text'):
		return

	# Skip articles without dates
	if not story.has_key('storyDate'):
		return

	if not story['text'].has_key('paragraph'):
		return

	print '* Parsing article fields ...',

	url = story['link'][0]['$text']
	title = story['title']['$text']
	content = '\n'.join([paragraph['$text'] for paragraph in story['text']['paragraph'] if paragraph.has_key('$text')])
	date = parser.parse(story['storyDate']['$text'])

	print 'done'

	message = json.dumps({
		'url': url,
		'title': title,
		'date': date.isoformat(),
		'content': content
	}).encode('utf-8')

	print '* Sending article to RabbitMQ ...',

	connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
	channel = connection.channel()
	channel.queue_declare(queue='articles')
	channel.basic_publish(exchange='', routing_key='articles', body=message)
	connection.close()

	print 'done'

#from_date = parser.parse('2012-01-01')
#to_date = parser.parse('2012-06-01')

startIndex = 0
numResults = 20
#start_date = from_date
#while start_date <= to_date:
while True:
	startIndex += numResults
	#startDate = start_date.strftime('%Y-%m-%d')
	#endDate = (start_date + timedelta(1)).strftime('%Y-%m-%d')

	url = 'http://api.npr.org/query?apiKey=MDA5NjUwNDIzMDEzNDA0NzAwMzdhYzY2Ng001&startDate=2012-01-01&endDate=2012-06-01&output=JSON&startNum=%d&numResults=%d' % (startIndex,numResults)
	print url
	data = urlopen(url).read()

	js = json.loads(data)
	if not js['list'].has_key('story'):
		break
	for story in js['list']['story']:

		index_story(story)
	#start_date = start_date + timedelta(1)
