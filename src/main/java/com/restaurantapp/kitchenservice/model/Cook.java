package com.restaurantapp.kitchenservice.model;

import com.restaurantapp.kitchenservice.constants.enums.CookingApparatusType;
import com.restaurantapp.kitchenservice.service.KitchenServiceImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static com.restaurantapp.kitchenservice.KitchenServiceApplication.TIME_UNIT;

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

    private final BlockingQueue<OrderItem> itemsQueue = new PriorityBlockingQueue<>(999, Comparator.comparing(OrderItem::getPickupTime).thenComparing(i->i.getCookingApparatusType() == null? 0 : 3));

    private void init() {

        for (int i = 0; i < proficiency; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        Optional<OrderItem> orderItemOptional = KitchenServiceImpl.items.stream().filter(item->item.getComplexity() <= this.rank).findAny();
                        if(orderItemOptional.isPresent()) {
                            OrderItem orderItem = orderItemOptional.get();
//                            log.info("Cooking order item : "+orderItem);
                            if (KitchenServiceImpl.items.remove(orderItem)) {
                                if (orderItem.getComplexity() <= this.rank) {
                                    if (orderItem.getCookingApparatusType() == null) {
                                        Thread.sleep(orderItem.getCookingTime() * TIME_UNIT);
                                        KitchenServiceImpl.checkIfOrderIsReady(orderItem, this.id);
                                    } else if (orderItem.getCookingApparatusType() == CookingApparatusType.OVEN)
                                        Oven.getInstance().addOrderItemToQueue(this, orderItem);
                                    else if (orderItem.getCookingApparatusType() == CookingApparatusType.STOVE)
                                        Stove.getInstance().addOrderItemToQueue(this, orderItem);
                                } else {
                                    KitchenServiceImpl.items.add(orderItem);
                                }
                            }
                        } else {
//                            Thread.sleep(10);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                }
            }).start();
        }
    }

    public int getProficiency() {
        return proficiency;
    }

    public BlockingQueue<OrderItem> getItemsQueue() {
        return itemsQueue;
    }

    public Double getQueueSizeOnProeficiencyRatio() {
        return (double) itemsQueue.size() / proficiency;
    }
}
