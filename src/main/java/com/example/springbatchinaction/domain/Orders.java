package com.example.springbatchinaction.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName; // 주문자명
    private String storeName; // 가맹점명
    private int amount; // 주문금액
    private LocalDate orderDate; // 주문일자

    public Orders(Long id, String customerName, String storeName, int amount, LocalDate orderDate) {
        this.id = id;
        this.customerName = customerName;
        this.storeName = storeName;
        this.amount = amount;
        this.orderDate = orderDate;
    }
}
