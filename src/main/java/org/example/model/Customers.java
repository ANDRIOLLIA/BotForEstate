package org.example.model;

public class Customers {
    String name;
    Long phoneNumber;
    String cityForBuyEstate;
    String typeOfEstate;

    enum TypesOfEstate {
        ATELIER, // Студия
        ONE_ROOM_APARTMENT, // Однокомнатная
        TWO_ROOM_APARTMENT, // Двухкомнатная
        THREE_ROOM_APARTMENT, // Трехкомнатная
        HOUSE // Дом
    }

    public Customers(String name, Long phoneNumber, String cityForBuyEstate, String typeOfEstate) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.cityForBuyEstate = cityForBuyEstate;
        this.typeOfEstate = typeOfEstate;
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

    @Override
    public String toString() {
        return "Customers{" +
                "name='" + name + '\'' +
                ", phoneNumber=" + phoneNumber +
                ", cityForBuyEstate='" + cityForBuyEstate + '\'' +
                ", typeOfEstate='" + typeOfEstate + '\'' +
                '}';
    }
}
