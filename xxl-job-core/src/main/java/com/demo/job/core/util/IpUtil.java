package com.demo.job.core.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class IpUtil {
    private static final Logger logger = LoggerFactory.getLogger(IpUtil.class);

    private static final String ANYHOST_VALUE = "0.0.0.0";
    private static final String LOCALHOST_VALUE = "127.0.0.1";
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");



    private static volatile InetAddress LOCAL_ADDRESS = null;

    // ---------------------- valid ----------------------


    private static InetAddress toValidAddress(InetAddress address){
        if(address instanceof  Inet6Address) {
            Inet6Address v6address = (Inet6Address) address;
            if(isPreferIPV6Address()){
                return normalizeV6Address(v6address);
            }
        }
        if(isValidV4Address(address)) {
            return address;
        }
        return null;
    }

    private static boolean isPreferIPV6Address() {
        return Boolean.getBoolean("java.net.preferIPv6Addresses");
    }

    private static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if(i>0){
            try{
                return InetAddress.getByName(addr.substring(0,i)+'%'+address.getScopeId());
            } catch (UnknownHostException e) {
                logger.debug("unknown ipv6 address");
                e.printStackTrace();
            }
        }
        return address;
    }

    private static boolean isValidV4Address(InetAddress address) {
        if(address==null||address.isLoopbackAddress()) {
            return false;
        }
        String hostAddress = address.getHostAddress();
        boolean result = (hostAddress!=null&&IP_PATTERN.matcher(hostAddress).matches()&&!hostAddress.equals(LOCAL_ADDRESS)&&!ANYHOST_VALUE.equals(hostAddress));
        return result;
    }

    // ---------------------- find ip ----------------------


    private static InetAddress getLocalAddress0() {
        InetAddress address = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            address = toValidAddress(localHost);
            if(address!=null){
                return address;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if(networkInterfaces==null){
                return address;
            }
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if(networkInterface.isLoopback()||networkInterface.isVirtual()||!networkInterface.isUp()){
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while(inetAddresses.hasMoreElements()) {
                    InetAddress address1 = toValidAddress(inetAddresses.nextElement());
                    if(address1!=null){
                        if(address1.isReachable(100)) {
                            return address1;
                        }
                    }
                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage(),e);
        }

        return address;
    }

    private static InetAddress getLocalAddress(){
        if(LOCAL_ADDRESS==null){
            LOCAL_ADDRESS = getLocalAddress0();
            return LOCAL_ADDRESS;
        } else {
            return LOCAL_ADDRESS;
        }
    }

    private static String  getIp() {
        return getLocalAddress().getHostAddress();
    }

    public static String getIpPort(int port){
        String ip = getIp();
        return getIpPort(ip, port);
    }

    public static String getIpPort(String ip, int port){
        if (ip==null) {
            return null;
        }
        return ip.concat(":").concat(String.valueOf(port));
    }

    public static Object[] parseIpPort(String address){
        String[] array = address.split(":");

        String host = array[0];
        int port = Integer.parseInt(array[1]);

        return new Object[]{host, port};
    }


}
