package com.example.springbatchinaction.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;   // 주문 번호
    private String storeName;   // 가맹점명
    private Integer settlementAmount; // 정산금액(수수료 제외)
    private LocalDate settlementDate; // 정산 처리일

    public Settlement(Long orderId, String storeName, Integer settlementAmount, LocalDate settlementDate) {
        this.orderId = orderId;
        this.storeName = storeName;
        this.settlementAmount = settlementAmount;
        this.settlementDate = settlementDate;
    }
}
