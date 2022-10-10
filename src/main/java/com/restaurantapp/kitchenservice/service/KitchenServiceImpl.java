package com.restaurantapp.kitchenservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.kitchenservice.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {

    private static final String dinningServiceHallUrl = "http://localhost:8082/dinning-hall/distribution";

    private final List<Cook> cooks = new ArrayList<>();

    private static final Integer NUMBER_OF_STOVES = 1;
    private static final Integer NUMBER_OF_OVENS = 2;

    public static final Semaphore stoveSemaphore = new Semaphore(NUMBER_OF_STOVES);
    public static final Semaphore ovenSemaphore = new Semaphore(NUMBER_OF_OVENS);

    protected static final Map<Long, List<FoodDetails>> orderToFoodListMap = new ConcurrentHashMap<>();

    protected final List<MenuItem> menuItems;
    protected static final List<Order> orders = new CopyOnWriteArrayList<>();

    public static BlockingQueue<OrderItem> items = new LinkedBlockingQueue<>();


    KitchenServiceImpl() throws IOException {
        initCooks();
        menuItems = initMenuItems();
    }

    private void initCooks() {
        cooks.add(new Cook(3, 4));
        cooks.add(new Cook(2, 3));
        cooks.add(new Cook(2, 2));
        cooks.add(new Cook(1, 2));
    }

    @Override
    public Double takeOrder(Order order) {

        order.setOrderReceivedAt(Instant.now());
        orderToFoodListMap.put(order.getOrderId(), new ArrayList<>());
        orders.add(order);
        order.getItems()
                .stream()
                .map(this::getMenuItemById)
                .map(mi -> new OrderItem(mi.getId(), order.getOrderId(), order.getPriority(), mi.getCookingApparatus(),
                        mi.getComplexity(), mi.getPreparationTime(), mi.getComplexity()))
                .forEach(i -> items.add(i));
        if(order.getWaiterId() == null){
            return getEstimatedPrepTimeForOrderById(order.getOrderId());
        }
        return null;
    }

    @Override
    public Double getEstimatedPrepTimeForOrderById(Long orderId) {
        if(orderToFoodListMap.containsKey(orderId)) {
            int B = cooks.stream().mapToInt(Cook::getProficiency).sum();
            List<Long> cookedItemsIds = orderToFoodListMap.get(orderId).stream().map(FoodDetails::getItemId).collect(Collectors.toList());
            Order order = getOrderById(orderId);
            List<MenuItem> itemsNotReady = order.getItems().stream().filter(i -> !cookedItemsIds.contains(i)).map(this::getMenuItemById).collect(Collectors.toList());
            double A = 0;
            double C = 0;
            for (MenuItem item : itemsNotReady) {
                if (item.getCookingApparatus() == null) {
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

    private static List<MenuItem> initMenuItems() throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = KitchenServiceImpl.class.getResourceAsStream("/menu-items.json");
        try {
            return mapper.readValue(is, new TypeReference<List<MenuItem>>() {
            });
        } catch (IOException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    private MenuItem getMenuItemById(Long id) {
        return menuItems.stream().filter(menuItem -> menuItem.getId().equals(id)).findFirst().orElseThrow();
    }

    public static void useStove() throws InterruptedException {
        stoveSemaphore.acquire();
    }

    public static void useOven() throws InterruptedException {
        ovenSemaphore.acquire();
    }

    public static void checkIfOrderIsReady(OrderItem orderItem, Long cookId) {

        Order order = getOrderById(orderItem.getOrderId());
        System.out.println("order item : "+orderItem);
        List<FoodDetails> foodDetails = orderToFoodListMap.get(orderItem.getOrderId());
        foodDetails.add(new FoodDetails(orderItem.getMenuId(), cookId));
        if (foodDetails.size() == order.getItems().size()) {
            sendFinishedOrderBackToKitchen(order);
        }
    }

    private static void sendFinishedOrderBackToKitchen(Order order) {

        orders.remove(order);
        orderToFoodListMap.remove(order.getOrderId());

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

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> response = restTemplate.postForEntity(dinningServiceHallUrl, finishedOrder, Void.class);
        if (response.getStatusCode() != HttpStatus.ACCEPTED) {
            log.error("Order couldn't be sent back to dinning hall service!");
        } else {
            log.info("Order " + finishedOrder + " was sent back to kitchen successfully.");
        }
    }

    public static Order getOrderById(Long id) {
        return orders.stream().filter(o -> o.getOrderId().equals(id)).findFirst().orElseThrow();
    }
}
