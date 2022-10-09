package com.restaurantapp.kitchenservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FoodDetails {

    @JsonAlias("food_id")
    private Long itemId;

    @JsonAlias("cook_id")
    private Long cookId;
}
