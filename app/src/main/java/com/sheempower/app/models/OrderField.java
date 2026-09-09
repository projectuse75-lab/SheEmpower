package com.sheempower.app.models;

public class OrderField {
    public String labelUrdu;
    public String labelEnglish;
    public String hintUrdu;
    public String type; // "text", "number", "spinner"
    public String[] options;

    public OrderField(String labelUrdu, String labelEnglish, String hintUrdu, String type) {
        this.labelUrdu = labelUrdu;
        this.labelEnglish = labelEnglish;
        this.hintUrdu = hintUrdu;
        this.type = type;
    }

    public OrderField(String labelUrdu, String labelEnglish, String type, String[] options) {
        this.labelUrdu = labelUrdu;
        this.labelEnglish = labelEnglish;
        this.type = type;
        this.options = options;
    }
}