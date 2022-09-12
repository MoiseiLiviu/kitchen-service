package com.restaurantapp.kitchenservice.model;

import com.restaurantapp.kitchenservice.constants.enums.CookingApparatus;
import com.restaurantapp.kitchenservice.service.KitchenServiceImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Setter
@Getter
@Slf4j
public class Cook {

    private Long id;
    private int rank;
    private int proeficiency;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Queue<OrderItem> itemsQueue = new PriorityQueue<>(11, Comparator.comparing(OrderItem::getPriority));

    private AtomicLong idCounter = new AtomicLong();
    private int concurrentDishesCounter = 0;

    public Cook(Integer rank, Integer proeficiency) {
        this.id = idCounter.incrementAndGet();
        this.rank = rank;
        this.proeficiency = proeficiency;
    }

    public void takeOrderItem(OrderItem orderItem) {

        if (concurrentDishesCounter == proeficiency) {
            itemsQueue.add(orderItem);
        } else {
            this.concurrentDishesCounter++;
            executorService.schedule(() -> cookOrderItem(orderItem), orderItem.getCookingTime(), TimeUnit.MICROSECONDS);
        }
    }

    public void cookOrderItem(OrderItem orderItem) {
        try {
            if (orderItem != null) {
                if (orderItem.getCookingApparatus().equals(CookingApparatus.OVEN)) {
                    KitchenServiceImpl.ovenSemaphore.release();
                } else if (orderItem.getCookingApparatus().equals(CookingApparatus.STOVE)) {
                    KitchenServiceImpl.stoveSemaphore.release();
                }
                this.concurrentDishesCounter--;
                log.info(String.format("Order item %s is ready.", orderItem));
                KitchenServiceImpl.checkIfOrderIsReady(orderItem, this.id);
                OrderItem nextItem = itemsQueue.poll();
                if (nextItem != null) {
                    this.concurrentDishesCounter++;
                    if (nextItem.getCookingApparatus().equals(CookingApparatus.OVEN)) {
                        KitchenServiceImpl.useOven();
                    } else if (nextItem.getCookingApparatus().equals(CookingApparatus.STOVE)) {
                        KitchenServiceImpl.useStove();
                    }
                    executorService.schedule(() -> cookOrderItem(itemsQueue.poll()), orderItem.getCookingTime(), TimeUnit.MICROSECONDS);
                }
            }
        } catch (InterruptedException ex){
            log.error(ex.getMessage());
        }
    }

    public double getOrderItemsQueueSizeProeficiencyRatio() {
        return (itemsQueue.size() / (double) proeficiency);
    }
}
