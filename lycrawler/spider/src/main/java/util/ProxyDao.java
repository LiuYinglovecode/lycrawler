package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ProxyDao {

    //使用记录器代理获取proxydao的详细信息
    private final static Logger LOGGER = LoggerFactory.getLogger(ProxyDao.class);
    //IP地址
    private final static String KEY_PREFIX = "ipregion.taobao_";
    //    private final static int EXCEED_TIME = 86400;
    //超时时间
    private final static int EXCEED_TIME = 604800;
    //用代理将信息插入到redis中
    public static boolean insetProxyToRedis(Set<String> proxy) {
        Jedis jedis = null;
        try {
            jedis = JedisMultiPool.getJedis();
            Iterator<String> iterator = proxy.iterator();
            while (iterator.hasNext()) {
                String proxyStr = iterator.next();
                String key = KEY_PREFIX + proxyStr;
                jedis.setex(key, EXCEED_TIME, proxyStr);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return false;
    }

    public static int getCountFromKey() {
        Jedis jedis = null;
        int count = 0;
        Set<String> proxy = new HashSet<>();
        try {
            jedis = JedisMultiPool.getJedis();
            String key = KEY_PREFIX + "*";
            proxy = jedis.keys(key);
            count = proxy.size();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return count;
    }

    public static Set<String> getProxyFromRedis() {
        Jedis jedis = null;
        Set<String> proxyKey;
        Set<String> proxyStr = new HashSet<>();
        try {
            jedis = JedisMultiPool.getJedis();
            String pattern = KEY_PREFIX + "*";
            proxyKey = jedis.keys(pattern);
            Iterator<String> iterator = proxyKey.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String proxy = jedis.get(key);
                proxyStr.add(proxy);
            }
            return proxyStr;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }

    public static boolean delectProxyBySet(Set<String> proxy) {
        Jedis jedis = null;
        try {
            jedis = JedisMultiPool.getJedis();
            Iterator<String> iterator = proxy.iterator();
            while (iterator.hasNext()) {
                String proxyStr = iterator.next();
                String key = KEY_PREFIX + proxyStr;
                jedis.del(key);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return false;
    }

    public static boolean delectProxyByOne(String proxy) {
        Jedis jedis = null;
        try {
            jedis = JedisMultiPool.getJedis();
            String key = KEY_PREFIX + proxy;
            if (jedis.exists(key)) {
                jedis.del(key);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return false;
    }

}
