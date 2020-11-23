package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IpProxyUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(IpProxyUtil.class);
    private static List<String> proxyList = new ArrayList<>();
    private int base = 0;
    private int max = 0;
    private int size = 0;

    public IpProxyUtil() {

    }

    public boolean isEmpty() {
        return max > 0 ? false : true;
    }

    public String getProxyIp() {
        String res = null;
        try {
            if (base < max) {
                res = proxyList.get(base++);
            }
            if (base >= (max - 1)) {
                base = 0;
            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error(e.getMessage());
        }
        return res;
    }

    public boolean addProxyIp(Set<String> set) {

        try {
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                proxyList.add(it.next());
            }
            max = proxyList.size();
            return true;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    public boolean removeProxyIpByOne(String ipProxy) {
        try {
            if (proxyList.contains(ipProxy)) {
                proxyList.remove(ipProxy);
                if (max > 0) {
                    max--;
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    public boolean removeProxyIpBySet(Set<String> proxy) {
        try {
            size = proxy.size();
            proxyList.removeAll(proxy);
            if (max >= size) {
                max = max - size;
                return true;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }
}
