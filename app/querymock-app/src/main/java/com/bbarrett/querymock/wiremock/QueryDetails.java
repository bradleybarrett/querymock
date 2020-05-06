package com.bbarrett.querymock.wiremock;

public class QueryDetails
{
    private String query;
    private String data;
    private boolean findOne = false;
    private Object bodyTemplate;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean getFindOne() {
        return findOne;
    }

    public void setFindOne(boolean findOne) {
        this.findOne = findOne;
    }

    public Object getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(Object bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }
}
