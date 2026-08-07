package org.fab.notificationorderlive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fab.notificationorderlive.dto.OrderCreateRequest;
import org.fab.notificationorderlive.dto.OrderDto;
import org.fab.notificationorderlive.dto.OrderStatusHistoryDto;
import org.fab.notificationorderlive.entities.Order;
import org.fab.notificationorderlive.entities.OrderStatusHistory;
import org.fab.notificationorderlive.enums.Status;
import org.fab.notificationorderlive.exception.InvalidOrderStatusTransitionException;
import org.fab.notificationorderlive.exception.OrderNotFoundException;
import org.fab.notificationorderlive.exception.OrderNotificationException;
import org.fab.notificationorderlive.mapper.OrderMapper;
import org.fab.notificationorderlive.repositories.OrderRepository;
import org.fab.notificationorderlive.repositories.OrderStatusHistoryRepository;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderService implements IOrderService {

    private static final Set<Status> TERMINAL_STATUSES = Set.of(Status.DELIVERED, Status.FAILED);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderMapper orderMapper;

    @Override
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

        return orderMapper.toDto(order);
    }

    @Override
    public List<OrderDto> listOrders() {
        return orderRepository.findAll().stream().map(orderMapper::toDto).toList();
    }

    @Override
    public OrderDto getOrder(String orderId) {
        return orderMapper.toDto(findOrderOrThrow(orderId));
    }

    @Override
    public List<OrderStatusHistoryDto> getHistory(String orderId) {
        findOrderOrThrow(orderId);
        return historyRepository.findByOrder_IdOrderByCreatedAtAsc(orderId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public OrderStatusHistoryDto updateStatus(String orderId, Status newStatus) {
        Order order = findOrderOrThrow(orderId);

        if (TERMINAL_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStatusTransitionException(orderId, order.getStatus());
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        OrderStatusHistory history = historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .createdAt(LocalDateTime.now())
                .build());

        OrderStatusHistoryDto dto = orderMapper.toDto(history);
        broadcast(orderId, dto);
        return dto;
    }

    private void broadcast(String orderId, OrderStatusHistoryDto dto) {
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + orderId, dto);
        } catch (MessagingException ex) {
            OrderNotificationException wrapped = new OrderNotificationException(orderId, ex);
            log.error(wrapped.getMessage(), wrapped);
        }
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
