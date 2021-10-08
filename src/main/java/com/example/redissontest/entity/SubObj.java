package com.example.redissontest.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: xieyi
 * @create: 2021/10/8 15:35
 * @conent:
 */
@Data
public class SubObj implements Serializable {
    private Integer subId;
    private String subName;
}
