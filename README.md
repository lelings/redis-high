# redis-high
基于Springboot、MySQL、Redis实现的网上点评服务

#### 使用双重拦截器代替session实现登陆功能
将大部分刷新逻辑抽离到刷新token拦截器中
<img width="870" alt="image" src="https://github.com/lelings/redis-high/assets/104212137/68e25373-9f52-42df-bad8-f0a0809fe7b2">

#### 解决缓存穿透问题
在使用布隆过滤器和存储空数据之间选择存储空数据，并没有选择延迟双删，选择了先操作数据库再删除缓存。

#### 解决缓存击穿问题
使用互斥锁机制重建缓存

<img width="613" alt="image" src="https://github.com/lelings/redis-high/assets/104212137/490e9280-147e-4528-9875-774c723d106e">

#### 解决秒杀问题
通过lua脚本实现原子性操作，并通过缓存到redis中和使用异步进程处理订单信息，避免多次操作数据库，提高效率
<img width="676" alt="image" src="https://github.com/lelings/redis-high/assets/104212137/a294ffa3-8c2e-4971-8c08-d52c00137c7d">


