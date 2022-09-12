package com.restaurantapp.kitchenservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FoodDetails {

    @JsonAlias("food_id")
    private Long itemId;

    @JsonAlias("cook_id")
    private Long cookId;
}
