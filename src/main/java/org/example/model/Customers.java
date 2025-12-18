package org.example.model;

import java.time.LocalDateTime;

public class Customers {
    String name;
    Long phoneNumber;
    String cityForBuyEstate;
    String typeOfEstate;
    LocalDateTime timeOfCreateQuery;
    LocalDateTime timeToContact;


    public Customers(String name, Long phoneNumber, String cityForBuyEstate, String typeOfEstate, LocalDateTime currentTime, LocalDateTime timeToContact) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.cityForBuyEstate = cityForBuyEstate;
        this.typeOfEstate = typeOfEstate;
        this.timeOfCreateQuery = currentTime;
        this.timeToContact = timeToContact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Long phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCityForBuyEstate() {
        return cityForBuyEstate;
    }

    public void setCityForBuyEstate(String cityForBuyEstate) {
        this.cityForBuyEstate = cityForBuyEstate;
    }

    public String getTypeOfEstate() {
        return typeOfEstate;
    }

    public void setTypeOfEstate(String typeOfEstate) {
        this.typeOfEstate = typeOfEstate;
    }

    public LocalDateTime getTimeOfCreateQuery() {
        return timeOfCreateQuery;
    }

    public void setTimeOfCreateQuery(LocalDateTime timeOfCreateQuery) {
        this.timeOfCreateQuery = timeOfCreateQuery;
    }

    public LocalDateTime getTimeToContact() {
        return timeToContact;
    }

    public void setTimeToContact(LocalDateTime timeToContact) {
        this.timeToContact = timeToContact;
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
        if (timeToContact != null && timeOfCreateQuery != null) {
            ret += "\nВремя создания запроса: " + timeOfCreateQuery +
                    "\nВремя для обращения: " + timeToContact;

        }
        return ret;
    }
}