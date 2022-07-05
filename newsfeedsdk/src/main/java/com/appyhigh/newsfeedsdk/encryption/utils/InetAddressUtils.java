package com.appyhigh.newsfeedsdk.encryption.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class InetAddressUtils {
    private static final String IPV4_BASIC_PATTERN_STRING =
            "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" + // initial 3 fields, 0-255 followed by .
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])"; // final field, 0-255
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^" + IPV4_BASIC_PATTERN_STRING + "$");
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        //TODO 3.0.0
                        boolean isIPv4 = isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port
                                // suffix
                                return delim < 0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }
}
