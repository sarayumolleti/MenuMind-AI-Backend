package com.menumind.backend.entity;

/**
 * Lifecycle status of an Order / Bill
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED
}
