package com.restaurantapp.kitchenservice.service;

import com.restaurantapp.kitchenservice.model.Order;

public interface KitchenService {
    Double takeOrder(Order order);

    Double getEstimatedPrepTimeForOrderById(Long orderId);
}
