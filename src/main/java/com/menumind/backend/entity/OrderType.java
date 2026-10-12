package com.menumind.backend.entity;

/**
 * Order Type distinguishes channels:
 * ONLINE: Placed by customers via web/mobile app
 * OFFLINE_COUNTER: Placed directly at the restaurant billing counter / POS
 */
public enum OrderType {
    ONLINE,
    OFFLINE_COUNTER
}
