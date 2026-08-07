package org.fab.notificationorderlive.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fab.notificationorderlive.dto.OrderCreateRequest;
import org.fab.notificationorderlive.dto.OrderDto;
import org.fab.notificationorderlive.dto.OrderStatusHistoryDto;
import org.fab.notificationorderlive.dto.UpdateOrderStatusRequest;
import org.fab.notificationorderlive.service.IOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderDto> listOrders() {
        return orderService.listOrders();
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/{orderId}/history")
    public List<OrderStatusHistoryDto> getHistory(@PathVariable String orderId) {
        return orderService.getHistory(orderId);
    }

    @PostMapping("/{orderId}/status")
    public OrderStatusHistoryDto updateStatus(@PathVariable String orderId,
                                               @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(orderId, request.status());
    }
}
