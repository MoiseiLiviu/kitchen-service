package com.restaurantapp.kitchenservice.controller;

import com.restaurantapp.kitchenservice.model.Order;
import com.restaurantapp.kitchenservice.service.KitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class KitchenController {

    private final KitchenService kitchenService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Double takeOrder(@RequestBody Order order){
//        log.info("Received order : "+order.toString());
        return kitchenService.takeOrder(order);
    }

    @GetMapping("/{id}")
    public Double getEstimatedPrepTimeForOrderById(@PathVariable Long id){
        return kitchenService.getEstimatedPrepTimeForOrderById(id);
    }
}
