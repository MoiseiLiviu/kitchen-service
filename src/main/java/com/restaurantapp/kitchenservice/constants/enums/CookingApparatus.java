package com.restaurantapp.kitchenservice.constants.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CookingApparatus {

    STOVE, OVEN;

    @JsonValue
    public String toLowerCase() {
        return toString().toLowerCase();
    }
}
