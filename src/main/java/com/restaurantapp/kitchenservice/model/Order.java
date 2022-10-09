package com.restaurantapp.kitchenservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Order {

    @JsonAlias("order_id")
    private Long orderId;

    private List<Long> items = new ArrayList<>();

    private Integer priority;

    @JsonAlias("max_wait")
    private Double maximumWaitTime;

    @JsonAlias("pick_up_time")
    private Long pickUpTime;

    @JsonAlias("waiter_id")
    private Long waiterId;

    @JsonAlias("table_id")
    private Long tableId;

    @JsonIgnore
    private Instant orderReceivedAt;
}
