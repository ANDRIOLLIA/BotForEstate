package org.example.model;

public class EndCustomer {
    String name;
    Long phoneNumber;
    String cityForBuyEstate;
    String typeOfEstate;

    public EndCustomer(String name, Long phoneNumber, String cityForBuyEstate, String typeOfEstate) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.cityForBuyEstate = cityForBuyEstate;
        this.typeOfEstate = typeOfEstate;
    }

    @Override
    public String toString() {
        String ret;
        ret = "Имя: " + name +
                "\nНомер телефона: " + phoneNumber +
                "\nГород: " + cityForBuyEstate +
                "\nТип недвижимости: ";
        if (typeOfEstate.equals("ATELIER")) ret += "Студия";
        if (typeOfEstate.equals("ONE_ROOM_APARTMENT")) ret += "Однокомнатная квартира";
        if (typeOfEstate.equals("TWO_ROOM_APARTMENT")) ret += "Двухкомнатная квартира";
        if (typeOfEstate.equals("THREE_ROOM_APARTMENT")) ret += "Трехкомнатная квартира";
        if (typeOfEstate.equals("HOUSE")) ret += "Дом";
        return ret;
    }
}