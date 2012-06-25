package org.newshack.quotes;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Quote {

	private int start;
	private int end;
	private String quote;
	private String person;
	
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getQuote() {
		return quote;
	}
	public void setQuote(String quote) {
		this.quote = quote;
	}
	public String getPerson() {
		return person;
	}
	public void setPerson(String person) {
		this.person = person;
	}
	
	public JSONObject toJson() {
		StringBuilder stringBuilder = new StringBuilder();
		JSONObject obj = new JSONObject();
		obj.put("quote", quote);
		obj.put("person", person);
		return obj;
	}
}
