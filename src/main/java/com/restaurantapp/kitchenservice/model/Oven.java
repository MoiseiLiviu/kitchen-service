package com.restaurantapp.kitchenservice.model;


public class Oven extends CookingApparatus{

    public static final Integer NUMBER_OF_OVENS = 2;

    private static final Oven instance = new Oven(NUMBER_OF_OVENS);

    public static Oven getInstance(){
        return instance;
    }

    private Oven(int nrOfOvens){
        super(nrOfOvens);
    }
}
