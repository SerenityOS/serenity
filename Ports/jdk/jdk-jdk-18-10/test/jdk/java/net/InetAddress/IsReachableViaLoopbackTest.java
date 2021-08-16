import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @test
 * @bug 8135305
 * @key intermittent
 * @summary ensure we can't ping external hosts via loopback if
 */

public class IsReachableViaLoopbackTest {
    public static void main(String[] args) {
        try {
            InetAddress addr = InetAddress.getByName("localhost");
            InetAddress remoteAddr = InetAddress.getByName("bugs.openjdk.java.net");
            if (!addr.isReachable(10000))
                throw new RuntimeException("Localhost should always be reachable");
            NetworkInterface inf = NetworkInterface.getByInetAddress(addr);
            if (inf != null) {
                if (!addr.isReachable(inf, 20, 10000)) {
                    throw new RuntimeException("Localhost should always be reachable");
                } else {
                    System.out.println(addr + "  is reachable");
                }
                if (remoteAddr.isReachable(inf, 20, 10000)) {
                    throw new RuntimeException(remoteAddr + " is reachable");
                } else {
                    System.out.println(remoteAddr + "  is NOT reachable");
                }
            } else {
                System.out.println("inf == null");
            }

        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception:" + e);
        }
        System.out.println("IsReachableViaLoopbackTest EXIT");
    }
}

