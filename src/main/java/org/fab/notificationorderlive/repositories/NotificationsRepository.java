package org.fab.notificationorderlive.repositories;

import org.fab.notificationorderlive.entities.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationsRepository extends JpaRepository<OrderStatusHistory, String> {

}
