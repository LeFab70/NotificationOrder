package org.fab.notificationorderlive.mapper;

import org.fab.notificationorderlive.dto.OrderDto;
import org.fab.notificationorderlive.dto.OrderStatusHistoryDto;
import org.fab.notificationorderlive.entities.Order;
import org.fab.notificationorderlive.entities.OrderStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        return new OrderDto(order.getId(), order.getStatus(), order.getCustomerId(), order.getCreatedAt());
    }

    public OrderStatusHistoryDto toDto(OrderStatusHistory history) {
        return OrderStatusHistoryDto.builder()
                .id(history.getId())
                .orderId(history.getOrder().getId())
                .status(history.getStatus())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
