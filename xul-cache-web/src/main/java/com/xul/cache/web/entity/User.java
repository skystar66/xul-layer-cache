package com.xul.cache.web.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Data
public class User implements Serializable {

    public User() {
        this.userId = 11L;
        this.name = "name";
        address = new Address();
        this.lastName = new String[]{"w", "四川", "~！@#%……&*（）——+{}：“？》:''\">?《~!@#$%^&*()_+{}\\"};
        List<String> lastNameList = new ArrayList<>();
        lastNameList.add("W");
        lastNameList.add("成都");
        this.lastNameList = lastNameList;
        this.lastNameSet = new HashSet<>(lastNameList);
        this.lastName = new String[]{"w", "四川", "~！@#%……&*（）——+{}：“？》:''\">?《~!@#$%^&*()_+{}\\"};
        this.age = 122;
        this.height = 18.2;
        this.income = new BigDecimal(22.22);
        this.birthday = new Date();
    }

    private long userId;

    private String name;

    private Address address;

    private String[] lastName;

    private List<String> lastNameList;

    private Set<String> lastNameSet;

    private int age;

    private double height;

    private BigDecimal income;

    private Date birthday;


    @Data
    public static class Address implements Serializable {
        private String addredd;

        public Address() {
            this.addredd = "成都";
        }

    }
}
