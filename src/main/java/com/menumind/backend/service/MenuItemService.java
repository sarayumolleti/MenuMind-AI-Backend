package com.menumind.backend.service;

import com.menumind.backend.dto.menu.MenuItemRequest;
import com.menumind.backend.dto.menu.MenuItemResponse;

import java.util.List;

/**
 * Menu Item Service Contract
 */
public interface MenuItemService {

    List<MenuItemResponse> getAllMenuItems();

    List<MenuItemResponse> searchMenuItems(Long categoryId, Boolean isVeg, Boolean availableOnly, String keyword);

    MenuItemResponse getMenuItemById(Long id);

    MenuItemResponse createMenuItem(MenuItemRequest request);

    MenuItemResponse updateMenuItem(Long id, MenuItemRequest request);

    MenuItemResponse toggleAvailability(Long id, Boolean isAvailable);

    void deleteMenuItem(Long id);
}
