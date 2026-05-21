package com.yala.order.repository;

import com.yala.order.model.Order;
import com.yala.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.buyer.id = :userId OR o.seller.id = :userId")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

    List<Order> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime deadline);

    @Query("SELECT o FROM Order o WHERE o.status = 'IN_TRANSIT' AND o.createdAt <= :cutoff")
    List<Order> findInTransitOrdersOlderThan(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByListingIdAndStatusIn(Long listingId, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.listing.id = :listingId AND o.status IN :statuses")
    List<Order> findByListingIdAndStatusIn(
            @Param("listingId") Long listingId,
            @Param("statuses") List<OrderStatus> statuses);
}
