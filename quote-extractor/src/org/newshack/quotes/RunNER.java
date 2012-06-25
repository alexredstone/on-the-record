package org.newshack.quotes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.SAXException;

import com.aliasi.demo.demos.NamedEntityDemo;
import com.aliasi.xml.SAXWriter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;


public class RunNER {
	
	public final static String QUOTE = "&quot;";
	public final static int CLOSE_CHAR_DIST = 30;
	
	public NamedEntityDemo extractor;
	
	public static void main(String[] args) throws SAXException, XPathExpressionException, ParserConfigurationException, IOException {
		RunNER runNER = new RunNER();
		consume("articles");
	}
	
	public void init() {
		extractor = new NamedEntityDemo(
				"com.aliasi.tokenizer.IndoEuropeanTokenizerFactory",
				"com.aliasi.sentences.IndoEuropeanSentenceModel",
				"/models/ne-en-news-muc6.AbstractCharLmRescoringChunker",
				"News English trained on the MUC 6 Corpus");
	}
	
	private static Article extractArticle(String s) {
		Article article = new Article();
		JSONObject obj = (JSONObject)JSONValue.parse(s);
		article.setContent((String)obj.get("content"));
		article.setTitle((String)obj.get("title"));
		article.setUrl((String)obj.get("url"));
		article.setDate((String)obj.get("date"));
		return article;
	}
	
	private static void consume(String queueName) {
		RunNER runNER = new RunNER();
		runNER.init();
		
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try {
        	Connection connection = factory.newConnection();
        	Channel channel = connection.createChannel();

        	//channel.exchangeDeclare("", "fanout");
        	//String queueName = channel.queueDeclare().getQueue();
        	channel.queueDeclare(queueName, false, false, false, null);
        	//channel.queueBind(queueName, "", "");
        	
        	System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        	QueueingConsumer consumer = new QueueingConsumer(channel);
        	channel.basicConsume(queueName, true, consumer);

        	while (true) {
        		QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        		String message = new String(delivery.getBody());
        		//System.out.println(" [x] Received '" + message + "'");  
        		
        		Article article = extractArticle(message);
        		runNER.enrichArticleWithNamedEntitiesAndQuotes(article);
        		for (Quote quote : article.getQuotes()) {
        			System.out.println(quote.getPerson()+"->"+quote.getQuote());
        		}
        		String utf8String = new String(article.toJson().toJSONString().getBytes(), "UTF-8");
        		publish(utf8String, "quotes");
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	private static void publish(String message, String queueName) {
		ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection;
        try {
        	connection = factory.newConnection();
        	Channel channel = connection.createChannel();
        	//channel.exchangeDeclare(exchangeName, "fanout");
        	
        	///String queueName = channel.queueDeclare().getQueue();
        	channel.queueDeclare(queueName, false, false, false, null);
        	String routingKey = queueName;
        	channel.basicPublish("", routingKey, null, message.getBytes());
        	//System.out.println(" [x] Sent '" + message + "'");

        	channel.close();
        	connection.close();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "boxing" })
	private void enrichArticleWithNamedEntitiesAndQuotes(Article article) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException {
		String entityString = getNER(article.getContent(), extractor);
		StringBuilder text = new StringBuilder();
		int tagCharCount = 0;
		Map<Integer,Integer> absCountToTextCount = new HashMap<Integer,Integer>();
		boolean inTag = false;
		for (int i = 0; i < entityString.length(); i++) {
			char c = entityString.toCharArray()[i];
			if (c == '<') {
				inTag = true;
			} else if (c == '>') {
				inTag = false;
			}
			if (inTag || c == '>') {
				tagCharCount++;
			} else {
				text.append(c); // get text content
			}
			absCountToTextCount.put(i, i + 1 - tagCharCount);

		}
		
		// quotes are added in order they appear in string
		List<Quote> quotes = getQuotes(text.toString());
		//System.out.println(entityString);
		//System.out.println(text);
		int match = 0;
		while (match >= 0) {
			match = entityString.indexOf("<ENAMEX TYPE=\"PERSON", match+1); // +1 to move forwards
			if (-1 == match) break; 
			int startIndex = entityString.indexOf(">", match+1) + 1;
			int endIndex = entityString.indexOf("<", startIndex);
			int realStart = absCountToTextCount.get(startIndex-1);
			int realEnd = absCountToTextCount.get(endIndex);
			String entity = text.substring(realStart, realEnd);
			//System.out.println(entity);
			for (Quote q : quotes) {
				if (q.getStart() > realEnd && q.getStart() < realEnd + CLOSE_CHAR_DIST) {
					q.setPerson(entity);
					break;
				}
			}
			for (int i = quotes.size()-1; i >= 0; i--) {
				Quote q = quotes.get(i);
				if (q.getEnd() < realStart && q.getEnd() > realStart - CLOSE_CHAR_DIST) {
					q.setPerson(entity);
					break;
				}
			}
		}
		List<Quote> attributedQuotes = new LinkedList<Quote>();
		for (Quote quote : quotes) {
			if (null != quote.getPerson()) {
				attributedQuotes.add(quote);
			}
		}
		article.setQuotes(attributedQuotes);
	}
	
	private List<Quote> getQuotes(String input) {
		List<Quote> quotes = new LinkedList<Quote>();
		int match = 0;
		Quote quote = null;
		while (match >= 0) {
			match = input.indexOf(QUOTE, match);
			if (match < 0) return quotes;
			int endIndex = input.indexOf(QUOTE, match+1);
			if (endIndex < 0) return quotes;
			quote = new Quote();
			quote.setStart(match + QUOTE.length());
			quote.setEnd(endIndex);
			quote.setQuote(input.substring(quote.getStart(), quote.getEnd()));
			quotes.add(quote);
			match = endIndex+1; // avoid parsing the same quote
		}
		return quotes;
	}
	
	private String getNER(String input, NamedEntityDemo extractor) throws UnsupportedEncodingException, SAXException {
		OutputStream os = new ByteArrayOutputStream();
        SAXWriter saxWriter = new SAXWriter(os, "UTF-8"); // don't need XML

        Properties properties = new Properties();
        properties.setProperty("resultType", "firstBest");
        char[] cs = input.toCharArray();
        saxWriter.startDocument();
        saxWriter.startSimpleElement("output");
        extractor.process(cs, 0, cs.length, saxWriter, properties);
        saxWriter.endSimpleElement("output");
        saxWriter.endDocument();
		return os.toString();
	}
}
