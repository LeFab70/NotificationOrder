package org.fab.notificationorderlive.repositories;

import org.fab.notificationorderlive.entities.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {

    List<OrderStatusHistory> findByOrder_IdOrderByCreatedAtAsc(String orderId);
}
