# redis-high
基于Springboot、MySQL、Redis实现的网上点评服务

## 使用双重拦截器代替session实现登陆功能
将大部分刷新逻辑抽离到刷新token拦截器中
<img width="870" alt="image" src="https://github.com/lelings/redis-high/assets/104212137/68e25373-9f52-42df-bad8-f0a0809fe7b2">

## 解决缓存穿透问题
在使用布隆过滤器和存储空数据之间选择存储空数据，并没有选择延迟双删，选择了先操作数据库再删除缓存。
