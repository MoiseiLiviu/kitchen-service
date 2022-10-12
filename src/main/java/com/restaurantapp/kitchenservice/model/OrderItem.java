package com.restaurantapp.kitchenservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.restaurantapp.kitchenservice.constants.enums.CookingApparatusType;
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
    private CookingApparatusType cookingApparatusType;
    private Integer proeficiency;
    private Integer cookingTime;
    private Integer complexity;

    @JsonIgnore
    private Long orderMaxWaitTime;

    @JsonIgnore
    private Long pickupTime;
}
