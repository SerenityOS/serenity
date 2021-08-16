/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import sun.security.util.HexDumpEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.MatchResult;

/*
 * A dummy DNS server.
 *
 * Loads a sequence of DNS messages from a capture file into its cache.
 * It listens for DNS UDP requests, finds match request in cache and sends the
 * corresponding DNS responses.
 *
 * The capture file contains an DNS protocol exchange in the hexadecimal
 * dump format emitted by HexDumpEncoder:
 *
 * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff  ................
 *
 * Typically, DNS protocol exchange is generated by DNSTracer who captures
 * communication messages between DNS application program and real DNS server
 */
public class DNSServer extends Thread implements Server {

    public class Pair<F, S> {
        private F first;
        private S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public void setFirst(F first) {
            this.first = first;
        }

        public void setSecond(S second) {
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }

    public static final int DNS_HEADER_SIZE = 12;
    public static final int DNS_PACKET_SIZE = 512;

    static HexDumpEncoder encoder = new HexDumpEncoder();

    private DatagramSocket socket;
    private String filename;
    private boolean loop;
    private final List<Pair<byte[], byte[]>> cache = new ArrayList<>();
    private ByteBuffer reqBuffer = ByteBuffer.allocate(DNS_PACKET_SIZE);
    private volatile boolean isRunning;

    public DNSServer(String filename) throws SocketException {
        this(filename, false);
    }

    public DNSServer(String filename, boolean loop) throws SocketException {
        this.socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        this.filename = filename;
        this.loop = loop;
    }

    public void run() {
        try {
            isRunning = true;
            System.out.println(
                    "DNSServer: Loading DNS cache data from : " + filename);
            loadCaptureFile(filename);

            System.out.println(
                    "DNSServer: listening on port " + socket.getLocalPort());

            System.out.println("DNSServer: loop playback: " + loop);

            int playbackIndex = 0;

            while (playbackIndex < cache.size()) {
                DatagramPacket reqPacket = receiveQuery();

                if (!verifyRequestMsg(reqPacket, playbackIndex)) {
                    if (playbackIndex > 0 && verifyRequestMsg(reqPacket,
                            playbackIndex - 1)) {
                        System.out.println(
                                "DNSServer: received retry query, resend");
                        playbackIndex--;
                    } else {
                        throw new RuntimeException(
                                "DNSServer: Error: Failed to verify DNS request. "
                                        + "Not identical request message : \n"
                                        + encoder.encodeBuffer(
                                        Arrays.copyOf(reqPacket.getData(),
                                                reqPacket.getLength())));
                    }
                }

                sendResponse(reqPacket, playbackIndex);

                playbackIndex++;
                if (loop && playbackIndex >= cache.size()) {
                    playbackIndex = 0;
                }
            }

            System.out.println(
                    "DNSServer: Done for all cached messages playback");

            System.out.println(
                    "DNSServer: Still listening for possible retry query");
            while (true) {
                DatagramPacket reqPacket = receiveQuery();

                // here we only handle the retry query for last one
                if (!verifyRequestMsg(reqPacket, playbackIndex - 1)) {
                    throw new RuntimeException(
                            "DNSServer: Error: Failed to verify DNS request. "
                                    + "Not identical request message : \n"
                                    + encoder.encodeBuffer(
                                    Arrays.copyOf(reqPacket.getData(),
                                            reqPacket.getLength())));
                }

                sendResponse(reqPacket, playbackIndex - 1);
            }
        } catch (Exception e) {
            if (isRunning) {
                System.err.println("DNSServer: Error: " + e);
                e.printStackTrace();
            } else {
                System.out.println("DNSServer: Exit");
            }
        }
    }

    private DatagramPacket receiveQuery() throws IOException {
        DatagramPacket reqPacket = new DatagramPacket(reqBuffer.array(),
                reqBuffer.array().length);
        socket.receive(reqPacket);

        System.out.println("DNSServer: received query message from " + reqPacket
                .getSocketAddress());

        return reqPacket;
    }

    private void sendResponse(DatagramPacket reqPacket, int playbackIndex)
            throws IOException {
        byte[] payload = generateResponsePayload(reqPacket, playbackIndex);
        socket.send(new DatagramPacket(payload, payload.length,
                reqPacket.getSocketAddress()));
        System.out.println("DNSServer: send response message to " + reqPacket
                .getSocketAddress());
    }

    /*
     * Load a capture file containing an DNS protocol exchange in the
     * hexadecimal dump format emitted by sun.misc.HexDumpEncoder:
     *
     * xxxx: 00 11 22 33 44 55 66 77   88 99 aa bb cc dd ee ff  ................
     */
    private void loadCaptureFile(String filename) throws IOException {
        StringBuilder hexString = new StringBuilder();
        String pattern = "(....): (..) (..) (..) (..) (..) (..) (..) (..)   "
                + "(..) (..) (..) (..) (..) (..) (..) (..).*";

        try (Scanner fileScanner = new Scanner(Paths.get(filename))) {
            while (fileScanner.hasNextLine()) {

                try (Scanner lineScanner = new Scanner(
                        fileScanner.nextLine())) {
                    if (lineScanner.findInLine(pattern) == null) {
                        continue;
                    }
                    MatchResult result = lineScanner.match();
                    for (int i = 1; i <= result.groupCount(); i++) {
                        String digits = result.group(i);
                        if (digits.length() == 4) {
                            if (digits.equals("0000")) { // start-of-message
                                if (hexString.length() > 0) {
                                    addToCache(hexString.toString());
                                    hexString.delete(0, hexString.length());
                                }
                            }
                            continue;
                        } else if (digits.equals("  ")) { // short message
                            continue;
                        }
                        hexString.append(digits);
                    }
                }
            }
        }
        addToCache(hexString.toString());
    }

    /*
     * Add an DNS encoding to the cache (by request message key).
     */
    private void addToCache(String hexString) {
        byte[] encoding = parseHexBinary(hexString);
        if (encoding.length < DNS_HEADER_SIZE) {
            throw new RuntimeException("Invalid DNS message : " + hexString);
        }

        if (getQR(encoding) == 0) {
            // a query message, create entry in cache
            cache.add(new Pair<>(encoding, null));
            System.out.println(
                    "    adding DNS query message with ID " + getID(encoding)
                            + " to the cache");
        } else {
            // a response message, attach it to the query entry
            if (!cache.isEmpty() && (getID(getLatestCacheEntry().getFirst())
                    == getID(encoding))) {
                getLatestCacheEntry().setSecond(encoding);
                System.out.println(
                        "    adding DNS response message associated to ID "
                                + getID(encoding) + " in the cache");
            } else {
                throw new RuntimeException(
                        "Invalid DNS message : " + hexString);
            }
        }
    }

    /*
     * ID: A 16 bit identifier assigned by the program that generates any
     * kind of query. This identifier is copied the corresponding reply and
     * can be used by the requester to match up replies to outstanding queries.
     */
    private static int getID(byte[] encoding) {
        return ByteBuffer.wrap(encoding, 0, 2).getShort();
    }

    /*
     * QR: A one bit field that specifies whether this message is
     * a query (0), or a response (1) after ID
     */
    private static int getQR(byte[] encoding) {
        return encoding[2] & (0x01 << 7);
    }

    private Pair<byte[], byte[]> getLatestCacheEntry() {
        return cache.get(cache.size() - 1);
    }

    private boolean verifyRequestMsg(DatagramPacket packet, int playbackIndex) {
        byte[] cachedRequest = cache.get(playbackIndex).getFirst();
        return Arrays.equals(Arrays
                        .copyOfRange(packet.getData(), 2, packet.getLength()),
                Arrays.copyOfRange(cachedRequest, 2, cachedRequest.length));
    }

    private byte[] generateResponsePayload(DatagramPacket packet,
            int playbackIndex) {
        byte[] resMsg = cache.get(playbackIndex).getSecond();
        byte[] payload = Arrays.copyOf(resMsg, resMsg.length);

        // replace the ID with same with real request
        payload[0] = packet.getData()[0];
        payload[1] = packet.getData()[1];

        return payload;
    }

    public static byte[] parseHexBinary(String s) {

        final int len = s.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException(
                    "hexBinary needs to be even-length: " + s);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = hexToBin(s.charAt(i));
            int l = hexToBin(s.charAt(i + 1));
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException(
                        "contains illegal character for hexBinary: " + s);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    private static int hexToBin(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

    @Override public void stopServer() {
        isRunning = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override public int getPort() {
        if (socket != null) {
            return socket.getLocalPort();
        } else {
            return -1;
        }
    }
}
