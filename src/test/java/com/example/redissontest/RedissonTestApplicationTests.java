package com.example.redissontest;

import com.example.redissontest.entity.SubObj;
import com.example.redissontest.entity.TestObj;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class RedissonTestApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    RedissonClient redissonClient;

    @Test
    void testRedissonReentrantLock(){
        //获取RLock对象，输入锁的名字，只有相同名字的锁才是同一把锁。
        RLock rLock = redissonClient.getLock("redissonReentrantLock");
        //上锁，该方法会阻塞当前线程，直到获取锁才往下走。
        rLock.lock();

        //下面方法执行时设置锁的超时时间为十秒，是另一个上锁重载方法，该方法会跳过看门狗
        //机制，即使锁超时后业务代码没有执行完成，依旧会释放锁，而不会对锁进行续期。
        rLock.lock(10,TimeUnit.SECONDS);
        try {
            System.out.println("获取分布式可重入锁成功");
            //执行业务代码
            try {
                TimeUnit.SECONDS.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally {
            //释放锁
            rLock.unlock();
        }


    }

    @Test
    void testRedissonReentrantTryLock(){
        //获取RLock对象，输入锁的名字，只有相同名字的锁才是同一把锁。
        RLock rLock = redissonClient.getLock("redissonReentrantLock");
        boolean succeed = false;
        try {
            //尝试获取锁，第一个参数是等待时间，第二个参数是锁的超时时间，
            //该方法尝试获取锁，不一定要获取成功，设置一个等待时间，线程会最多阻塞住该时间
            //假如在等待时间内还是没有获取锁的话，线程就会不阻塞了，继续往下走，只是没有获取到
            //锁succeed 会为false。如果获取到了锁，succeed会为true。
            //如果在等待时间内获取到了锁，线程就会往下执行。
            succeed = rLock.tryLock(100, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (succeed){
            try {
                System.out.println("获取分布式可重入锁成功:"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                //执行业务代码
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }finally {
                //释放锁
                rLock.unlock();
            }
        }
    }

    /**
     * 读写锁
     */
    @Test
    //读锁
    public void testReadLock(){
        //获取读写锁RReadWriteLock ，读锁跟写锁是成对出现的，锁定的资源也是一样的，所以锁的名称要一样
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("r-w-lock");
        //获取一把读锁
        RLock readLock = readWriteLock.readLock();
        //其余操作就跟可重入锁一样了
        readLock.lock();
        try {
            System.out.println("获取了读锁:"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally {
            //释放锁
            readLock.unlock();
        }
    }

    @Test
    //写锁
    public void testWriteLock(){
        //锁名要与读锁一致，因为是成对出现
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("r-w-lock");
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            System.out.println("获取了写锁:"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally {
            writeLock.unlock();
        }
    }

    /**
     * CountDownLatch
     */
    @Test
    //上锁
    public void testCountDownLatchLock(){
        RCountDownLatch rCountDownLatch = redissonClient.getCountDownLatch("countDownLatch");
        //设置计数器为10，当减到0时取消阻塞，执行下面代码。
        rCountDownLatch.trySetCount(3);
        try {
            System.out.println("开始等待:"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            rCountDownLatch.await();
            //执行业务流程
            System.out.println("继续执行:"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCountDownLatchLockRelease(){
        RCountDownLatch rCountDownLatch = redissonClient.getCountDownLatch("countDownLatch");
        //减一
        rCountDownLatch.countDown();
    }


    /**
     * 信号量
     */
    @Test
    public void testLimitFlow() throws InterruptedException {
        //获取信号量，可以将信号量设置为1000，代表当前系统的当前业务限流为1000，同时只能有1000并发，其他进行服务的降级
        RSemaphore semaphore = redissonClient.getSemaphore("limitFlow");
        semaphore.trySetPermits(2);
        //尝试获取信号，信号量减一，会阻塞当前线程，直到获取到信号或者超时5秒返回，超时就返回false，获取到信号就返回true
        boolean succeed = false;
        try {
            succeed = semaphore.tryAcquire(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }
        if (succeed){
            try {
                System.out.println("获取到了信号，执行业务逻辑");
            }finally {
                //释放信号
                //semaphore.release();
            }
        }else {
            //没有获取到信号量，直接返回，类似服务降级
        }
        TimeUnit.SECONDS.sleep(10);
    }


    /**
     * bucket
     * @throws Exception
     */
    @Test
    public void bucket() throws Exception {
        //同步
        RBucket<String> bucket = redissonClient.getBucket("name");
        bucket.set("zhaoyun");
        System.out.println(bucket.get());

        //异步
        RBucket<String> bucket2 = redissonClient.getBucket("name2");
        bucket2.setAsync("赵云2").get();
        bucket2.getAsync().thenAccept(System.out::println);

        //对象
        RBucket<TestObj> bucket3= redissonClient.getBucket("obj");
        TestObj testObj = new TestObj();
        testObj.setId(111);
        testObj.setName("name");
        SubObj subObj = new SubObj();
        subObj.setSubId(222);
        subObj.setSubName("subName");
        testObj.setSubObj(subObj);
        bucket3.set(testObj);
        System.out.println(bucket3.get());

        System.out.println(redissonClient.getBucket("obj").get());

        RBuckets buckets = redissonClient.getBuckets();
        //List<RBucket<Object>> foundBuckets = buckets.find("myBucket*");
        Map<String, Object> loadedBuckets = buckets.get("name", "name2", "obj");


        //Reactive
        /*RBucketReactive<String> bucket3 = reactiveClient.getBucket("name3");
        bucket3.set("赵云3").block();
        bucket3.get().subscribe(System.out::println);*/

        //RxJava2
        /*RBucketRx<String> bucket4 = rxClient.getBucket("name4");
        bucket4.set("赵云4").blockingGet();
        bucket4.get().subscribe(System.out::println);*/

        //Thread.sleep(1000 * 5);
    }



}
