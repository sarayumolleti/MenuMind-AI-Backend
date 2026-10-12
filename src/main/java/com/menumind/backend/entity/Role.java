package com.menumind.backend.entity;

/**
 * User Roles within MenuMind AI
 * 
 * ROLE_CUSTOMER: End-user placing online food orders
 * ROLE_STAFF: Counter/POS operator creating bills and fulfilling orders
 * ROLE_MANAGER: Access to analytics, profit metrics, ML forecasts, and AI insights
 * ROLE_ADMIN: System administrator managing users, roles, and restaurant settings
 */
public enum Role {
    ROLE_CUSTOMER,
    ROLE_STAFF,
    ROLE_MANAGER,
    ROLE_ADMIN
}
