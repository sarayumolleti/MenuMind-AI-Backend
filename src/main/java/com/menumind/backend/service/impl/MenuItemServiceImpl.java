package com.menumind.backend.service.impl;

import com.menumind.backend.dto.menu.MenuItemRequest;
import com.menumind.backend.dto.menu.MenuItemResponse;
import com.menumind.backend.entity.Category;
import com.menumind.backend.entity.MenuItem;
import com.menumind.backend.exception.BadRequestException;
import com.menumind.backend.exception.ResourceNotFoundException;
import com.menumind.backend.repository.CategoryRepository;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(MenuItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> searchMenuItems(Long categoryId, Boolean isVeg, Boolean availableOnly, String keyword) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        boolean filterAvailable = availableOnly != null && availableOnly;

        return menuItemRepository.searchMenuItems(categoryId, isVeg, filterAvailable, trimmedKeyword).stream()
                .map(MenuItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
        return MenuItemResponse.fromEntity(item);
    }

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        String trimmedName = request.getName().trim();
        if (menuItemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("A menu item with name '" + trimmedName + "' already exists.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        MenuItem item = MenuItem.builder()
                .category(category)
                .name(trimmedName)
                .description(request.getDescription())
                .sellingPrice(request.getSellingPrice())
                .estimatedCost(request.getEstimatedCost())
                .imageUrl(request.getImageUrl())
                .isVeg(request.getIsVeg() != null ? request.getIsVeg() : true)
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        MenuItem savedItem = menuItemRepository.save(item);
        log.info("Created menu item: {} in category: {}", savedItem.getName(), category.getName());
        return MenuItemResponse.fromEntity(savedItem);
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        String trimmedName = request.getName().trim();
        if (!item.getName().equalsIgnoreCase(trimmedName) &&
                menuItemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Another menu item with name '" + trimmedName + "' already exists.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        item.setCategory(category);
        item.setName(trimmedName);
        item.setDescription(request.getDescription());
        item.setSellingPrice(request.getSellingPrice());
        item.setEstimatedCost(request.getEstimatedCost());
        item.setImageUrl(request.getImageUrl());
        if (request.getIsVeg() != null) item.setIsVeg(request.getIsVeg());
        if (request.getIsAvailable() != null) item.setIsAvailable(request.getIsAvailable());
        if (request.getIsActive() != null) item.setIsActive(request.getIsActive());

        MenuItem updatedItem = menuItemRepository.save(item);
        return MenuItemResponse.fromEntity(updatedItem);
    }

    @Override
    @Transactional
    public MenuItemResponse toggleAvailability(Long id, Boolean isAvailable) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        item.setIsAvailable(isAvailable);
        MenuItem updatedItem = menuItemRepository.save(item);
        log.info("Toggled availability for item {}: {}", item.getName(), isAvailable);
        return MenuItemResponse.fromEntity(updatedItem);
    }

    @Override
    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));

        // Soft delete dish by setting isActive to false
        item.setIsActive(false);
        menuItemRepository.save(item);
        log.info("Soft-deleted menu item id: {}", id);
    }
}
