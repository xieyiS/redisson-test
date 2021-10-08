package com.example.redissontest.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: xieyi
 * @create: 2021/10/8 15:34
 * @conent:
 */
@Data
public class TestObj implements Serializable {

    private Integer id;
    private String name;
    private SubObj subObj;
}
