package com.restaurantapp.kitchenservice.controller;

import com.restaurantapp.kitchenservice.model.Order;
import com.restaurantapp.kitchenservice.service.KitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class KitchenController {

    private final KitchenService kitchenService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void takeOrder(@RequestBody Order order){
        kitchenService.takeOrder(order);
    }
}
