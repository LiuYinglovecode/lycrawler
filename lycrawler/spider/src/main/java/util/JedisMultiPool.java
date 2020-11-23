package util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;

public class JedisMultiPool {
    private static HashMap<String, JedisPool> _jedisPool = new HashMap<>();

    private static JedisPool getPool() {
        return getPool(null);
    }
    /**
     * 根据连接池名称获取Redis连接池
     */
    private static JedisPool getPool(final String poolName) {
        JedisPool pool = null;
        String newPoolName = poolName;
        if(poolName == null || poolName.length() == 0) {
            newPoolName = "redis";
        }

        if(!_jedisPool.containsKey(newPoolName)) {
            synchronized (_jedisPool) {
                if(!_jedisPool.containsKey(newPoolName)) {
                    pool = createPool(newPoolName);
                    _jedisPool.put(newPoolName, pool);
                }
            }
        }
        else {
            pool = _jedisPool.get(newPoolName);
        }

        return pool;
    }

    private static redis.clients.jedis.JedisPool createPool(final String poolName) {
        redis.clients.jedis.JedisPool jedisPool = null;
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            //最大连接数, 默认8个
            config.setMaxTotal(8);

            //获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
            config.setMaxWaitMillis(-1);

            //逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
            config.setMinEvictableIdleTimeMillis(1800000);

            //最小空闲连接数, 默认0
            config.setMinIdle(0);

            //每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
            config.setNumTestsPerEvictionRun(3);

            //对象空闲多久后逐出, 当空闲时间>该值 且 空闲连接>最大空闲数 时直接逐出,不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)
            config.setSoftMinEvictableIdleTimeMillis(1800000);

            //在获取连接的时候检查有效性, 默认false
            config.setTestOnBorrow(false);

            //在空闲时检查有效性, 默认false
            config.setTestWhileIdle(false);

            //逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
            config.setTimeBetweenEvictionRunsMillis(-1);

//            String redisAddress = ConfigClient.instance().get(poolName, "address");
//            String port = ConfigClient.instance().get(poolName, "port", "6379");
//            String timeout = ConfigClient.instance().get(poolName, "timeout", "5000");
//            String auth = ConfigClient.instance().get(poolName, "auth", null);
            String redisAddress = "192.168.56.10";
            String port = "6379";
            String timeout = "5000";
            String auth = null;

            jedisPool = new redis.clients.jedis.JedisPool(config, redisAddress, Integer.parseInt(port), Integer.parseInt(timeout), auth);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jedisPool;
    }

    /**
     * 从默认连接池获取jedis实例
     * @return
     */
    public static Jedis getJedis() {
        return getJedis(null);
    }

    /**
     * 从指定连接池名称中获取jedis实例
     * @param poolName
     * @return
     */
    public synchronized static Jedis getJedis(final String poolName) {
        try {
            redis.clients.jedis.JedisPool pool = getPool(poolName);
            if (pool != null) {
                Jedis resource = pool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
