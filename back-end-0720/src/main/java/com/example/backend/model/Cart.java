package com.example.backend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Cart {
    private Long id;
    private Long userId;
    private Long productId;
    private Long quantity;
    private Long shoseSize;
    private Long finalPrice;

    private String name;
    private byte[] data;
    private byte[] data2;
    private String type;
    private String title;
    private String model;
}
