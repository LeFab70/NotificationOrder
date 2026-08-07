package org.fab.notificationorderlive.service;

import org.fab.notificationorderlive.dto.OrderCreateRequest;
import org.fab.notificationorderlive.dto.OrderDto;
import org.fab.notificationorderlive.dto.OrderStatusHistoryDto;
import org.fab.notificationorderlive.enums.Status;

import java.util.List;

public interface IOrderService {

    OrderDto createOrder(OrderCreateRequest request);

    List<OrderDto> listOrders();

    OrderDto getOrder(String orderId);

    List<OrderStatusHistoryDto> getHistory(String orderId);

    OrderStatusHistoryDto updateStatus(String orderId, Status newStatus);
}
