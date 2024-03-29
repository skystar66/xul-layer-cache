## 缓存的选择

一级缓存：Caffeine是一个一个高性能的 Java 缓存库；使用 Window TinyLfu 回收策略，提供了一个近乎最佳的命中率（Caffeine 缓存详解）。
        优点数据就在应用内存所以速度快。缺点受应用内存的限制，所以容量有限；没有持久化，重启服务后缓存数据会丢失；在分布式环境下缓存数据数据无法同步；
二级缓存：redis是一高性能、高可用的key-value数据库，支持多种数据类型，支持集群，和应用服务器分开部署易于横向扩展。
        优点支持多种数据类型，扩容方便；有持久化，重启应用服务器缓存数据不会丢失；他是一个集中式缓存，不存在在应用服务器之间同步数据的问题。缺点每次都需要访问redis存在IO浪费的情况。
        
        
可以发现Caffeine和Redis的优缺点正好相反，所以他们可以有效的互补。

## 整体架构图
![Image text](https://github.com/skystar66/xul-layer-cache/blob/master/%E5%88%86%E5%B8%83%E5%BC%8F%E7%BC%93%E5%AD%98-%E8%AF%BB%E5%8F%96:%E6%9B%B4%E6%96%B0:%E5%88%A0%E9%99%A4:%E5%8F%AF%E7%94%A8%E6%80%A7%E6%9E%B6%E6%9E%84%E5%9B%BE.jpg)

## 服务健壮性架构图
![Image text](https://github.com/skystar66/xul-layer-cache/blob/master/%E5%88%86%E5%B8%83%E5%BC%8F%E7%BC%93%E5%AD%98-%E5%BF%83%E8%B7%B3%E6%96%AD%E7%BA%BF:%E6%95%B0%E6%8D%AE%E5%90%8C%E6%AD%A5:%E6%9C%8D%E5%8A%A1%E5%90%AF%E5%8A%A8%E6%9E%B6%E6%9E%84%E5%9B%BE.jpg)

## 功能

1，数据一致性：通过mq机制 ，每场次变更时 将变更的数据 广播到所有服务器,包括:更新/删除

2，预刷新机制：缓存快到期时，提供两种刷新机制，强/软刷新，强刷新：提前刷新mysql到redis中，软刷新：重设redis过期时间

3，缓存不存在时：使用互斥锁，其它线程集中等待，集中并发，效率会更高，cpu切换更少，然后获取mysql、redis数据 load到cache中

4，分布式锁： 使用 redission，看门狗机制 可解决分布式缓存的续期/集群同步失败问题

5，支持缓存的自动刷新：当发现二级缓存快要过期时，会开启一个异步线程进行刷新

6，通过设置缓存空值解决缓存穿透，通过异步加载的机制解决缓存雪崩、缓存击穿

## json序列化性能对比
 
||size|serialize(set 10W次)|deserialize(get 10W次)|
---|---|---|---
Kryo|269 b|8956 ms|2530 ms
FastJson|365 b|2409 ms|10286 ms
Jackson|502 b|1726 ms|3811 ms
Jdk|1015 b|7695 ms|24536 ms
Protostuff|268 b|1198 ms|1508 ms
gson|360 b|6319 ms|16219 ms

