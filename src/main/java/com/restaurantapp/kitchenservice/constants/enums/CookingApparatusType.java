package com.restaurantapp.kitchenservice.constants.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CookingApparatusType {

    STOVE, OVEN;

    @JsonValue
    public String toLowerCase() {
        return toString().toLowerCase();
    }
}
