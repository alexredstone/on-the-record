package info.ontherecord;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Article {
	private String title;
	private String url;
	private String date;
	private String content;
	private List<Quote> quotes;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		StringBuilder stringBuilder = new StringBuilder();
		JSONObject obj = new JSONObject();
		obj.put("title", title);
		obj.put("url", url);
		obj.put("date", date);
		obj.put("content", content);
		
		JSONArray array = new JSONArray();
		for (Quote quote : quotes) {
			array.add(quote.toJson());
		}
			
		obj.put("quotes", array);
		return obj;
	}
	public List<Quote> getQuotes() {
		return quotes;
	}
	public void setQuotes(List<Quote> quotes) {
		this.quotes = quotes;
	}
	
}
