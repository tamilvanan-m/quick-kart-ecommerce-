package com.quickkart.service;

import com.quickkart.dto.AdminDtos;
import com.quickkart.dto.OrderPaymentDtos;
import com.quickkart.entity.Order;
import com.quickkart.entity.enums.OrderStatus;
import com.quickkart.entity.enums.ProductStatus;
import com.quickkart.entity.enums.RoleName;
import com.quickkart.mapper.EntityMapper;
import com.quickkart.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final EntityMapper mapper;

    public AdminDashboardService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            EntityMapper mapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AdminDtos.DashboardStatsDto getDashboardStats(int days) {
        BigDecimal totalSales = orderRepository.sumTotalRevenue();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        BigDecimal todaySales = orderRepository.sumRevenueBetween(startOfToday, LocalDateTime.now());
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal monthlySales = orderRepository.sumRevenueBetween(startOfMonth, LocalDateTime.now());

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PLACED)
                + orderRepository.countByOrderStatus(OrderStatus.CONFIRMED)
                + orderRepository.countByOrderStatus(OrderStatus.PACKED);
        long deliveredOrders = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        long totalProducts = productRepository.countByDeletedFalse();
        long totalCustomers = userRepository.findByRoleName(RoleName.CUSTOMER, PageRequest.of(0, 1)).getTotalElements();
        long lowStockProducts = inventoryRepository.findLowStockInventory().size();
        long outOfStockProducts = productRepository.countByDeletedFalseAndStatus(ProductStatus.OUT_OF_STOCK);

        List<AdminDtos.RevenuePointDto> revenueChart = getRevenueSeries(days);

        List<OrderPaymentDtos.OrderDto> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(o -> mapper.toOrderDto(o, null, null))
                .toList();

        List<AdminDtos.TopProductDto> topSellingProducts = orderItemRepository.findTopSellingProducts()
                .stream()
                .limit(6)
                .map(row -> new AdminDtos.TopProductDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        (BigDecimal) row[4]
                ))
                .toList();

        List<AdminDtos.CategorySalesDto> categorySales = orderItemRepository.findCategorySalesBreakdown()
                .stream()
                .map(row -> new AdminDtos.CategorySalesDto(
                        (String) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();

        return new AdminDtos.DashboardStatsDto(
                totalSales,
                todaySales,
                monthlySales,
                totalOrders,
                pendingOrders,
                deliveredOrders,
                totalProducts,
                totalCustomers,
                lowStockProducts,
                outOfStockProducts,
                revenueChart,
                recentOrders,
                topSellingProducts,
                categorySales
        );
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.RevenuePointDto> getRevenueSeries(int days) {
        int safeDays = Math.min(90, Math.max(7, days));
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        List<Order> orders = orderRepository.findValidOrdersSince(startDate.atStartOfDay());

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        Map<LocalDate, Long> countByDate = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate d = startDate.plusDays(i);
            revenueByDate.put(d, BigDecimal.ZERO);
            countByDate.put(d, 0L);
        }

        for (Order o : orders) {
            LocalDate d = o.getCreatedAt().toLocalDate();
            if (revenueByDate.containsKey(d)) {
                revenueByDate.put(d, revenueByDate.get(d).add(o.getTotalAmount()));
                countByDate.put(d, countByDate.get(d) + 1);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        return revenueByDate.entrySet().stream()
                .map(e -> new AdminDtos.RevenuePointDto(
                        e.getKey().format(fmt),
                        e.getValue(),
                        countByDate.getOrDefault(e.getKey(), 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportOrdersCsv() {
        List<Order> orders = orderRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Order Number,Customer,Email,Order Status,Payment Method,Payment Status,Subtotal,Discount,Delivery,Total,Created At\n");
        for (Order o : orders) {
            sb.append(csvEscape(o.getOrderNumber())).append(',')
                    .append(csvEscape(o.getShippingFullName())).append(',')
                    .append(csvEscape(o.getUser().getEmail())).append(',')
                    .append(csvEscape(o.getOrderStatus().name())).append(',')
                    .append(csvEscape(o.getPaymentMethod().name())).append(',')
                    .append(csvEscape(o.getPaymentStatus().name())).append(',')
                    .append(o.getSubtotal()).append(',')
                    .append(o.getDiscountAmount()).append(',')
                    .append(o.getDeliveryCharge()).append(',')
                    .append(o.getTotalAmount()).append(',')
                    .append(o.getCreatedAt()).append('\n');
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
