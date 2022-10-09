package com.restaurantapp.kitchenservice.model;

import com.restaurantapp.kitchenservice.constants.enums.CookingApparatus;
import com.restaurantapp.kitchenservice.service.KitchenServiceImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

@Setter
@Getter
@Slf4j
public class Cook {

    private Long id;
    private int rank;
    private int proficiency;
    private AtomicLong idCounter = new AtomicLong();

    public Cook(Integer rank, Integer proficiency) {
        this.id = idCounter.incrementAndGet();
        this.rank = rank;
        this.proficiency = proficiency;
        init();
    }

    private void init() {

        for (int i = 0; i < proficiency; i++) {
            new Thread(() -> {
                try {
                    while (true) {
                        OrderItem orderItem = KitchenServiceImpl.items.take();
                        if (orderItem.getComplexity() > rank) {
                            OrderItem oldItem = orderItem;
                            orderItem = KitchenServiceImpl.items.take();
                            KitchenServiceImpl.items.add(oldItem);
                        }
                        if (orderItem.getCookingApparatus() != null) {
                            Semaphore semaphore = getSemaphoreByType(orderItem.getCookingApparatus());
                            if (!semaphore.tryAcquire()) {
                                OrderItem oldItem = orderItem;
                                orderItem = KitchenServiceImpl.items.take();
                                KitchenServiceImpl.items.add(oldItem);
                            } else {
                                Thread.sleep(orderItem.getCookingTime() * 50L);
                                semaphore.release();
                            }
                        } else {
                            Thread.sleep(orderItem.getCookingTime() * 50L);
                        }
                        KitchenServiceImpl.checkIfOrderIsReady(orderItem, id);
                    }
                } catch (
                        InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private Semaphore getSemaphoreByType(CookingApparatus cookingApparatus) {

        if (cookingApparatus.equals(CookingApparatus.OVEN)) {
            return KitchenServiceImpl.ovenSemaphore;
        } else return KitchenServiceImpl.stoveSemaphore;
    }
}
