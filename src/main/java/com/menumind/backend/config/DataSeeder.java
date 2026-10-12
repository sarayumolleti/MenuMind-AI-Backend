package com.menumind.backend.config;

import com.menumind.backend.entity.*;
import com.menumind.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking and safely seeding demo data...");

        // 1. Users (Only add if missing)
        User admin = createUserIfMissing("Admin", "admin@spicegarden.com", "Admin@123", Role.ROLE_ADMIN);
        User manager = createUserIfMissing("Manager", "manager@spicegarden.com", "Manager@123", Role.ROLE_MANAGER);
        User staff = createUserIfMissing("Staff", "staff@spicegarden.com", "Staff@123", Role.ROLE_STAFF);
        User customer1 = createUserIfMissing("Alice", "alice@customer.com", "Customer@123", Role.ROLE_CUSTOMER);
        User customer2 = createUserIfMissing("Bob", "bob@customer.com", "Customer@123", Role.ROLE_CUSTOMER);

        // 2. Categories
        Category starters = createCategoryIfMissing("Starters", "Appetizers to start your meal");
        Category mainCourse = createCategoryIfMissing("Main Course", "Delicious main dishes");
        Category biryani = createCategoryIfMissing("Biryani", "Authentic aromatic biryani");
        Category breads = createCategoryIfMissing("Breads", "Freshly baked Indian breads");
        Category beverages = createCategoryIfMissing("Beverages", "Refreshing drinks");

        // 3. Menu Items
        MenuItem paneerTikka = createMenuItemIfMissing("Paneer Tikka", "Spicy grilled cottage cheese", 250, 100, true, starters);
        MenuItem chickenTikka = createMenuItemIfMissing("Chicken Tikka", "Tandoori chicken chunks", 300, 120, false, starters);
        MenuItem butterChicken = createMenuItemIfMissing("Butter Chicken", "Rich tomato gravy chicken", 350, 150, false, mainCourse);
        MenuItem palakPaneer = createMenuItemIfMissing("Palak Paneer", "Spinach and cottage cheese", 280, 110, true, mainCourse);
        MenuItem chickenBiryani = createMenuItemIfMissing("Chicken Biryani", "Aromatic basmati rice with chicken", 320, 140, false, biryani);
        MenuItem vegBiryani = createMenuItemIfMissing("Veg Biryani", "Mixed vegetables with basmati rice", 260, 100, true, biryani);
        MenuItem garlicNaan = createMenuItemIfMissing("Garlic Naan", "Soft bread with garlic butter", 60, 20, true, breads);
        MenuItem butterRoti = createMenuItemIfMissing("Butter Roti", "Whole wheat bread with butter", 30, 10, true, breads);
        MenuItem lassi = createMenuItemIfMissing("Sweet Lassi", "Traditional yogurt drink", 80, 30, true, beverages);
        MenuItem mojito = createMenuItemIfMissing("Virgin Mojito", "Mint and lime cooler", 120, 40, true, beverages);

        List<MenuItem> allDemoItems = List.of(paneerTikka, chickenTikka, butterChicken, palakPaneer, chickenBiryani, vegBiryani, garlicNaan, butterRoti, lassi, mojito);

        // 4. Orders (Only generate if we have fewer than 45 orders to avoid duplicating huge data sets)
        if (orderRepository.count() < 45) {
            log.info("Generating 45 historical orders for analytics and ML...");
            Random random = new Random(42);
            LocalDateTime now = LocalDateTime.now();

            for (int i = 1; i <= 45; i++) {
                boolean isOnline = random.nextBoolean();
                User cust = isOnline ? (random.nextBoolean() ? customer1 : customer2) : null;
                User stf = isOnline ? null : staff;
                
                LocalDateTime orderDate = now.minusDays(random.nextInt(30)).minusHours(random.nextInt(12));

                Order order = Order.builder()
                        .orderNumber("ORD-" + (isOnline ? "ONL-" : "POS-") + orderDate.getYear() + orderDate.getMonthValue() + "-" + (1000 + i))
                        .orderType(isOnline ? OrderType.ONLINE : OrderType.OFFLINE_COUNTER)
                        .customer(cust)
                        .staff(stf)
                        .orderStatus(OrderStatus.COMPLETED)
                        .paymentStatus(PaymentStatus.PAID)
                        .paymentMethod(isOnline ? PaymentMethod.ONLINE : PaymentMethod.CASH)
                        .tableNumber(isOnline ? null : "Table " + (random.nextInt(10) + 1))
                        .build();

                order.setCreatedAt(orderDate);
                order.setUpdatedAt(orderDate);

                int numItems = random.nextInt(4) + 1;
                BigDecimal subtotal = BigDecimal.ZERO;
                
                for (int j = 0; j < numItems; j++) {
                    MenuItem item = allDemoItems.get(random.nextInt(allDemoItems.size()));
                    int qty = random.nextInt(3) + 1;
                    BigDecimal unitPrice = item.getSellingPrice();
                    BigDecimal itemSub = unitPrice.multiply(BigDecimal.valueOf(qty));
                    
                    OrderItem oi = OrderItem.builder()
                            .menuItem(item)
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .unitCost(item.getEstimatedCost())
                            .subtotal(itemSub)
                            .build();
                    order.addItem(oi);
                    subtotal = subtotal.add(itemSub);
                }

                BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));
                order.setSubtotal(subtotal);
                order.setTaxAmount(tax);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subtotal.add(tax));

                orderRepository.save(order);
            }
            log.info("Successfully seeded 45 orders.");
        } else {
            log.info("Historical orders already exist. Skipping order generation to prevent duplicates.");
        }

        log.info("Demo data seeding check completed.");
    }

    private User createUserIfMissing(String name, String email, String password, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> 
            userRepository.save(User.builder()
                    .fullName(name)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .phone("9876543210")
                    .isActive(true)
                    .build())
        );
    }

    private Category createCategoryIfMissing(String name, String desc) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> 
            categoryRepository.save(Category.builder().name(name).description(desc).isActive(true).build())
        );
    }

    private MenuItem createMenuItemIfMissing(String name, String desc, double price, double cost, boolean isVeg, Category cat) {
        List<MenuItem> existing = menuItemRepository.searchMenuItems(null, null, false, name);
        for(MenuItem item : existing) {
            if(item.getName().equalsIgnoreCase(name)) return item;
        }
        return menuItemRepository.save(MenuItem.builder()
                .name(name)
                .description(desc)
                .sellingPrice(BigDecimal.valueOf(price))
                .estimatedCost(BigDecimal.valueOf(cost))
                .isVeg(isVeg)
                .isActive(true)
                .isAvailable(true)
                .category(cat)
                .build());
    }
}
