package com.coldchain.web;

public enum Role {
    ADMIN("Admin"), WAREHOUSE_MANAGER("Warehouse Manager"), TRANSPORT_MANAGER("Transport Manager"), DRIVER("Driver"),
    HOSPITAL_STAFF("Hospital Staff"), SUPPLIER("Supplier"), QUALITY_INSPECTOR("Quality Inspector");
    private final String label;
    Role(String label) { this.label = label; }
    public String label() { return label; }
}
