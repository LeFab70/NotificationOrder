package org.fab.notificationorderlive.service;

import lombok.RequiredArgsConstructor;
import org.fab.notificationorderlive.dto.OrderCreateRequest;
import org.fab.notificationorderlive.dto.OrderDto;
import org.fab.notificationorderlive.dto.OrderStatusHistoryDto;
import org.fab.notificationorderlive.entities.Order;
import org.fab.notificationorderlive.entities.OrderStatusHistory;
import org.fab.notificationorderlive.enums.Status;
import org.fab.notificationorderlive.repositories.OrderRepository;
import org.fab.notificationorderlive.repositories.OrderStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final Set<Status> TERMINAL_STATUSES = Set.of(Status.DELIVERED, Status.FAILED);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public OrderDto createOrder(OrderCreateRequest request) {
        Order order = Order.builder()
                .status(Status.PAYMENT_PENDING)
                .customerId(request.customerId())
                .createdAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build());

        return toDto(order);
    }

    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream().map(this::toDto).toList();
    }

    public OrderDto getOrder(String orderId) {
        return toDto(findOrderOrThrow(orderId));
    }

    public List<OrderStatusHistoryDto> getHistory(String orderId) {
        findOrderOrThrow(orderId);
        return historyRepository.findByOrder_IdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public OrderStatusHistoryDto updateStatus(String orderId, Status newStatus) {
        Order order = findOrderOrThrow(orderId);

        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La commande %s est dans un statut final (%s) et ne peut plus evoluer."
                            .formatted(orderId, order.getStatus()));
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        OrderStatusHistory history = historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .createdAt(LocalDateTime.now())
                .build());

        OrderStatusHistoryDto dto = toDto(history);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, dto);
        return dto;
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable : " + orderId));
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(order.getId(), order.getStatus(), order.getCustomerId(), order.getCreatedAt());
    }

    private OrderStatusHistoryDto toDto(OrderStatusHistory history) {
        return OrderStatusHistoryDto.builder()
                .id(history.getId())
                .orderId(history.getOrder().getId())
                .status(history.getStatus())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
