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

    private static final String dinningServiceHallUrl = "http://dinning-hall-service:8080/dinning-hall/distribution";

    private final List<Cook> cooks = new ArrayList<>();

    private static final Integer NUMBER_OF_STOVES = 1;
    private static final Integer NUMBER_OF_OVENS = 2;

    public static final Semaphore stoveSemaphore = new Semaphore(NUMBER_OF_STOVES);
    public static final Semaphore ovenSemaphore = new Semaphore(NUMBER_OF_OVENS);

    protected static final Map<Long, List<FoodDetails>> orderToFoodListMap = new ConcurrentHashMap<>();

    private final ExecutorService orderItemDispatcher = Executors.newSingleThreadExecutor();

    protected final List<MenuItem> menuItems;
    protected static final List<Order> orders = new CopyOnWriteArrayList<>();


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
    public void takeOrder(Order order) {

        order.setOrderReceivedAt(Instant.now());
        orders.add(order);
        List<OrderItem> orderItems = order.getItems()
                .stream()
                .map(this::getMenuItemById)
                .map(mi -> new OrderItem(mi.getId(), order.getOrderId(), order.getPriority(), mi.getCookingApparatus(),
                        mi.getComplexity(), mi.getPreparationTime(), mi.getComplexity()))
                .collect(Collectors.toList());
        orderItems.forEach(orderItem -> orderItemDispatcher.submit(() -> dispatchOrderItemToTheRightCook(orderItem)));
    }

    private void dispatchOrderItemToTheRightCook(OrderItem orderItem) {

        Cook selectedCook = cooks.stream()
                .filter(c -> c.getRank() >= orderItem.getComplexity())
                .min(Comparator.comparing(Cook::getOrderItemsQueueSizeProeficiencyRatio)
                        .thenComparing(Cook::getRank))
                .orElseThrow();
        selectedCook.takeOrderItem(orderItem);
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

    public static synchronized void checkIfOrderIsReady(OrderItem orderItem, Long cookId){

        Order order = getOrderById(orderItem.getOrderId());
        orderToFoodListMap.putIfAbsent(orderItem.getOrderId(), new ArrayList<>());
        List<FoodDetails> foodDetails = orderToFoodListMap.get(orderItem.getOrderId());
        foodDetails.add(new FoodDetails(orderItem.getMenuId(), cookId));
        if(foodDetails.size() == order.getItems().size()){
            sendFinishedOrderBackToKitchen(order);
        }
    }

    private static void sendFinishedOrderBackToKitchen(Order order){

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
        if(response.getStatusCode() != HttpStatus.ACCEPTED){
            log.error("Order couldn't be sent back to dinning hall service!");
        } else {
            log.info("Order "+finishedOrder+" was sent back to kitchen successfully.");
        }
    }

    public static Order getOrderById(Long id){
        return orders.stream().filter(o->o.getOrderId().equals(id)).findFirst().orElseThrow();
    }
}
