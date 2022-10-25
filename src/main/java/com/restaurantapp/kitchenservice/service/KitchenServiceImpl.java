package com.restaurantapp.kitchenservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.kitchenservice.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


import static com.restaurantapp.kitchenservice.model.Oven.NUMBER_OF_OVENS;
import static com.restaurantapp.kitchenservice.model.Stove.NUMBER_OF_STOVES;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {

    private static final List<Cook> cooks = new ArrayList<>();

    private static final Map<Integer, List<Cook>> rankMap = new HashMap<>();

    protected static final Map<Long, List<FoodDetails>> orderToFoodListMap = new ConcurrentHashMap<>();

    protected static final Map<Long, MenuItem> menuItems = new HashMap<>();
    protected static final Map<Long, Order> orders = new HashMap<>();


    public static BlockingQueue<OrderItem> items = new LinkedBlockingQueue<>();

    private static final RestTemplate restTemplate = new RestTemplate();

    private static String DINNNING_HALL_URL;

    @Value("${dinning-hall-service.url}")
    public void setDinningHallServiceUrl(String url){
        DINNNING_HALL_URL = url;
    }

    @Value("${restaurant.menu}")
    public String restaurantMenu;


    @PostConstruct
    public void init() throws IOException {
        initCooks();
        initMenuItems();
    }

    private void initCooks() {
        cooks.add(new Cook(3, 4));
        cooks.add(new Cook(2, 3));
        cooks.add(new Cook(1, 2));


        for(int i = 1;i<=3;i++){
            rankMap.put(i, new ArrayList<>());
            for (Cook cook : cooks){
                if(cook.getRank() >= i){
                    rankMap.get(i).add(cook);
                }
            }
        }
    }

    @Override
    public Double takeOrder(Order order) {

        order.setOrderReceivedAt(Instant.now());
        orderToFoodListMap.put(order.getOrderId(), new ArrayList<>());
        orders.put(order.getOrderId(), order);
        order.getItems()
                .stream()
                .map(this::getMenuItemById)
                .map(mi -> new OrderItem(mi.getId(), order.getOrderId(), order.getPriority(), mi.getCookingApparatusType(),
                        mi.getComplexity(), mi.getPreparationTime(), mi.getComplexity(), order.getMaximumWaitTime().longValue(), order.getPickUpTime()))
                .forEach(i-> items.add(i));

        if(order.getWaiterId() == null){
            log.info("Received external order : "+order);
            return getEstimatedPrepTimeForOrderById(order.getOrderId());
        }
        return null;
    }

    @Override
    public Double getEstimatedPrepTimeForOrderById(Long orderId) {
        List<FoodDetails> foodDetails = orderToFoodListMap.get(orderId);
        Order order = getOrderById(orderId);
        if(foodDetails != null && order != null) {
            int B = cooks.stream().mapToInt(Cook::getProficiency).sum();
            List<Long> cookedItemsIds = orderToFoodListMap.get(orderId).stream().map(FoodDetails::getItemId).collect(Collectors.toList());
            List<MenuItem> itemsNotReady = order.getItems().stream().filter(i -> !cookedItemsIds.contains(i)).map(this::getMenuItemById).collect(Collectors.toList());

            double A = 0;
            double C = 0;
            if(itemsNotReady.isEmpty())return 0D;
            for (MenuItem item : itemsNotReady) {
                if (item.getCookingApparatusType() == null) {
                    A += item.getPreparationTime();
                } else {
                    C += item.getPreparationTime();
                }
            }
            int D = NUMBER_OF_OVENS + NUMBER_OF_STOVES;

            double E = items.size();
            int F = itemsNotReady.size();
            return ( A / B +  C / D) * (E + F) / F;
        } else return 0D;
    }

    private void initMenuItems() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = KitchenServiceImpl.class.getResourceAsStream("/"+restaurantMenu);
        try {
            for (MenuItem menuItem : mapper.readValue(is, new TypeReference<List<MenuItem>>() {})){
                menuItems.put(menuItem.getId(), menuItem);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private MenuItem getMenuItemById(Long id) {
        return menuItems.get(id);
    }

    public synchronized static void checkIfOrderIsReady(OrderItem orderItem, Long cookId) {
//        log.info("Order item ready : "+orderItem);
        Order order = getOrderById(orderItem.getOrderId());

        List<FoodDetails> foodDetails = orderToFoodListMap.get(orderItem.getOrderId());
        foodDetails.add(new FoodDetails(orderItem.getMenuId(), cookId));
        if (foodDetails.size() == order.getItems().size()) {
            sendFinishedOrderBackToDinningHall(order);
        }
    }

    private static void sendFinishedOrderBackToDinningHall(Order order) {

        orders.remove(order.getOrderId());

        FinishedOrder finishedOrder = new FinishedOrder();
        finishedOrder.setOrderId(order.getOrderId());
        finishedOrder.setCookingDetails(orderToFoodListMap.get(order.getOrderId()));
        finishedOrder.setItems(order.getItems());
        finishedOrder.setPriority(order.getPriority());
        finishedOrder.setWaiterId(order.getWaiterId());
        finishedOrder.setCookingTime(Instant.now().getEpochSecond() - order.getOrderReceivedAt().getEpochSecond());
        finishedOrder.setPickUpTime(order.getPickUpTime());
        finishedOrder.setMaximumWaitTime(order.getMaximumWaitTime());
        finishedOrder.setTableId(order.getTableId());

        orderToFoodListMap.remove(order.getOrderId());

        ResponseEntity<Void> response = restTemplate.postForEntity(DINNNING_HALL_URL, finishedOrder, Void.class);
        if (response.getStatusCode() != HttpStatus.ACCEPTED) {
            log.error("Order couldn't be sent back to dinning hall service!");
        } else {
            log.info("Order " + finishedOrder + " was sent back to dinning hall successfully.");

        }
    }

    public static Order getOrderById(Long id) {
        return orders.get(id);
    }
}
