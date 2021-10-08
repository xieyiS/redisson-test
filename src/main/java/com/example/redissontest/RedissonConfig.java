package com.example.redissontest;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: xieyi
 * @create: 2021/10/8 14:19
 * @conent:
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //配置redis的服务地址,地址要以redis://ip:port  如果是安全连接ssl，就要rediss://ip:port
        config.useSingleServer().setAddress("redis://localhost:6379");
        //创建一个RedissonClient，redisson的主要操作都是通过这个客户端完成。
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}