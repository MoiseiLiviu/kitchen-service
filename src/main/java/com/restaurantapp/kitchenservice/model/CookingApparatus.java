package com.restaurantapp.kitchenservice.model;

import com.restaurantapp.kitchenservice.service.KitchenServiceImpl;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.restaurantapp.kitchenservice.KitchenServiceApplication.TIME_UNIT;

public abstract class CookingApparatus {

    private final BlockingQueue<Pair<Cook, OrderItem>> items = new LinkedBlockingQueue<>();

    public CookingApparatus(int nrOfAppliances){

        for(int i = 0;i<nrOfAppliances;i++){
            new Thread(()->{
                while(true){
                    try {
                        Pair<Cook,OrderItem> orderItemPair = items.take();
                        OrderItem orderItem = orderItemPair.getRight();
                        Cook cook = orderItemPair.getLeft();
                        Thread.sleep(orderItem.getCookingTime() * TIME_UNIT);
                        KitchenServiceImpl.checkIfOrderIsReady(orderItem, cook.getId());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public void addOrderItemToQueue(Cook cook, OrderItem orderItem){
        items.add(Pair.of(cook, orderItem));
    }
}
