package com.restaurantapp.kitchenservice.model;

import com.restaurantapp.kitchenservice.constants.enums.CookingApparatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class OrderItem {

    private Long menuId;
    private Long orderId;
    private Integer priority;
    private CookingApparatus cookingApparatus;
    private Integer proeficiency;
    private Integer cookingTime;
    private Integer complexity;
}
