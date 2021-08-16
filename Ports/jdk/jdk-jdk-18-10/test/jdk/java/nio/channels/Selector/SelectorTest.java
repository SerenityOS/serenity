/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Test selectors and socketchannels
 * @library ..
 * @key randomness
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;


public class SelectorTest {
    private static List clientList = new LinkedList();
    private static Random rnd = new Random();
    public static int NUM_CLIENTS = 30;
    public static int TEST_PORT = 31452;
    static PrintStream log = System.err;
    private static int FINISH_TIME = 30000;

    /*
     * Usage note
     *
     * java SelectorTest [server] [client <host>] [<port>]
     *
     * No arguments runs both client and server in separate threads
     * using the default port of 31452.
     *
     * client runs the client on this machine and connects to server
     * at the given IP address.
     *
     * server runs the server on localhost.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Server server = new Server(0);
            server.start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
            InetSocketAddress isa
                = new InetSocketAddress(InetAddress.getLocalHost(), server.port());
            Client client = new Client(isa);
            client.start();
            if ((server.finish(FINISH_TIME) & client.finish(FINISH_TIME)) == 0)
                throw new Exception("Failure");
            log.println();

        } else if (args[0].equals("server")) {

            if (args.length > 1)
                TEST_PORT = Integer.parseInt(args[1]);
            Server server = new Server(TEST_PORT);
            server.start();
            if (server.finish(FINISH_TIME) == 0)
                throw new Exception("Failure");
            log.println();

        } else if (args[0].equals("client")) {

            if (args.length < 2) {
                log.println("No host specified: terminating.");
                return;
            }
            String ip = args[1];
            if (args.length > 2)
                TEST_PORT = Integer.parseInt(args[2]);
            InetAddress ia = InetAddress.getByName(ip);
            InetSocketAddress isa = new InetSocketAddress(ia, TEST_PORT);
            Client client = new Client(isa);
            client.start();
            if (client.finish(FINISH_TIME) == 0)
                throw new Exception("Failure");
            log.println();

        } else {
            System.out.println("Usage note:");
            System.out.println("java SelectorTest [server] [client <host>] [<port>]");
            System.out.println("No arguments runs both client and server in separate threads using the default port of 31452.");
            System.out.println("client runs the client on this machine and connects to the server specified.");
            System.out.println("server runs the server on localhost.");
        }
    }

    static class Client extends TestThread {
        InetSocketAddress isa;
        Client(InetSocketAddress isa) {
            super("Client", SelectorTest.log);
            this.isa = isa;
        }

        public void go() throws Exception {
            log.println("starting client...");
            for (int i=0; i<NUM_CLIENTS; i++)
                clientList.add(new RemoteEntity(i, isa, log));

            Collections.shuffle(clientList);

            log.println("created "+NUM_CLIENTS+" clients");
            do {
                for (Iterator i = clientList.iterator(); i.hasNext(); ) {
                    RemoteEntity re = (RemoteEntity) i.next();
                    if (re.cycle()) {
                        i.remove();
                    }
                }
                Collections.shuffle(clientList);
            } while (clientList.size() > 0);
        }
    }

    static class Server extends TestThread {
        private final ServerSocketChannel ssc;
        private List socketList = new ArrayList();
        private ServerSocket ss;
        private int connectionsAccepted = 0;
        private Selector pollSelector;
        private Selector acceptSelector;
        private Set pkeys;
        private Set pskeys;

        Server(int port) throws IOException {
            super("Server", SelectorTest.log);
            this.ssc = ServerSocketChannel.open().bind(new InetSocketAddress(port));
        }

        int port() {
            return ssc.socket().getLocalPort();
        }

        public void go() throws Exception {
            log.println("starting server...");
            acceptSelector = SelectorProvider.provider().openSelector();
            pollSelector = SelectorProvider.provider().openSelector();
            pkeys = pollSelector.keys();
            pskeys = pollSelector.selectedKeys();
            Set readyKeys = acceptSelector.selectedKeys();
            RequestHandler rh = new RequestHandler(pollSelector, log);
            Thread requestThread = new Thread(rh);

            requestThread.start();

            ssc.configureBlocking(false);
            SelectionKey acceptKey = ssc.register(acceptSelector,
                                                  SelectionKey.OP_ACCEPT);
            while(connectionsAccepted < SelectorTest.NUM_CLIENTS) {
                int keysAdded = acceptSelector.select(100);
                if (keysAdded > 0) {
                    Iterator i = readyKeys.iterator();
                    while(i.hasNext()) {
                        SelectionKey sk = (SelectionKey)i.next();
                        i.remove();
                        ServerSocketChannel nextReady =
                            (ServerSocketChannel)sk.channel();
                        SocketChannel sc = nextReady.accept();
                        connectionsAccepted++;
                        if (sc != null) {
                            sc.configureBlocking(false);
                            synchronized (pkeys) {
                               sc.register(pollSelector, SelectionKey.OP_READ);
                            }
                        } else {
                            throw new RuntimeException(
                                "Socket does not support Channels");
                        }
                    }
                }
            }
            acceptKey.cancel();
            requestThread.join();
            acceptSelector.close();
            pollSelector.close();
        }
    }
}

class RemoteEntity {
    private static Random rnd = new Random();
    int id;
    ByteBuffer data;
    int dataWrittenIndex;
    int totalDataLength;
    boolean initiated = false;
    boolean connected = false;
    boolean written = false;
    boolean acked = false;
    boolean closed = false;
    private SocketChannel sc;
    ByteBuffer ackBuffer;
    PrintStream log;
    InetSocketAddress server;

    RemoteEntity(int id, InetSocketAddress server, PrintStream log)
        throws Exception
    {
        int connectFailures = 0;
        this.id = id;
        this.log = log;
        this.server = server;

        sc = SocketChannel.open();
        sc.configureBlocking(false);

        // Prepare the data buffer to write out from this entity
        // Let's use both slow and fast buffers
        if (rnd.nextBoolean())
            data = ByteBuffer.allocateDirect(100);
        else
            data = ByteBuffer.allocate(100);
        String number = Integer.toString(id);
        if (number.length() == 1)
            number = "0"+number;
        String source = "Testing from " + number;
        data.put(source.getBytes("8859_1"));
        data.flip();
        totalDataLength = source.length();

        // Allocate an ack buffer
        ackBuffer = ByteBuffer.allocateDirect(10);
    }

    private void reset() throws Exception {
        sc.close();
        sc = SocketChannel.open();
        sc.configureBlocking(false);
    }

    private void connect() throws Exception {
        try {
            connected = sc.connect(server);
            initiated = true;
        }  catch (ConnectException e) {
            initiated = false;
            reset();
        }
    }

    private void finishConnect() throws Exception {
        try {
            connected = sc.finishConnect();
        }  catch (IOException e) {
            initiated = false;
            reset();
        }
    }

    int id() {
        return id;
    }

    boolean cycle() throws Exception {
        if (!initiated)
            connect();
        else if (!connected)
            finishConnect();
        else if (!written)
            writeCycle();
        else if (!acked)
            ackCycle();
        else if (!closed)
            close();
        return closed;
    }

    private void ackCycle() throws Exception {
        //log.println("acking from "+id);
        int bytesRead = sc.read(ackBuffer);
        if (bytesRead > 0) {
            acked = true;
        }
    }

    private void close() throws Exception {
        sc.close();
        closed = true;
    }

    private void writeCycle() throws Exception {
        log.println("writing from "+id);
        int numBytesToWrite = rnd.nextInt(10)+1;
        int newWriteTarget = dataWrittenIndex + numBytesToWrite;
        if (newWriteTarget > totalDataLength)
            newWriteTarget = totalDataLength;
        data.limit(newWriteTarget);
        int bytesWritten = sc.write(data);
        if (bytesWritten > 0)
            dataWrittenIndex += bytesWritten;
        if (dataWrittenIndex == totalDataLength) {
            written = true;
            sc.socket().shutdownOutput();
        }
    }

}


class RequestHandler implements Runnable {
    private static Random rnd = new Random();
    private Selector selector;
    private int connectionsHandled = 0;
    private HashMap dataBin = new HashMap();
    PrintStream log;

    public RequestHandler(Selector selector, PrintStream log) {
        this.selector = selector;
        this.log = log;
    }

    public void run() {
        log.println("starting request handler...");
        int connectionsAccepted = 0;

        Set nKeys = selector.keys();
        Set readyKeys = selector.selectedKeys();

        try {
            while(connectionsHandled < SelectorTest.NUM_CLIENTS) {
                int numKeys = selector.select(100);

                // Process channels with data
                synchronized (nKeys) {
                    if (readyKeys.size() > 0) {
                        Iterator i = readyKeys.iterator();
                        while(i.hasNext()) {
                            SelectionKey sk = (SelectionKey)i.next();
                            i.remove();
                            SocketChannel sc = (SocketChannel)sk.channel();
                            if (sc.isOpen())
                                read(sk, sc);
                        }
                    }
                }

                // Give other threads a chance to run
                if (numKeys == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (Exception x) {}
                }
            }
        } catch (Exception e) {
            log.println("Unexpected error 1: "+e);
            e.printStackTrace();
        }
    }

    private void read(SelectionKey sk, SocketChannel sc) throws Exception {
        ByteBuffer bin = (ByteBuffer)dataBin.get(sc);
        if (bin == null) {
            if (rnd.nextBoolean())
                bin = ByteBuffer.allocateDirect(100);
            else
                bin = ByteBuffer.allocate(100);
            dataBin.put(sc, bin);
        }

        int bytesRead = 0;
        do {
            bytesRead = sc.read(bin);
        } while(bytesRead > 0);

        if (bytesRead == -1) {
            sk.interestOps(0);
            bin.flip();
            int size = bin.limit();
            byte[] data = new byte[size];
            for(int j=0; j<size; j++)
                data[j] = bin.get();
            String message = new String(data, "8859_1");
            connectionsHandled++;
            acknowledge(sc);
            log.println("Received >>>"+message + "<<<");
            log.println("Handled: "+connectionsHandled);
        }
    }

    private void acknowledge(SocketChannel sc) throws Exception {
            ByteBuffer ackBuffer = ByteBuffer.allocateDirect(10);
            String s = "ack";
            ackBuffer.put(s.getBytes("8859_1"));
            ackBuffer.flip();
            int bytesWritten = 0;
            while(bytesWritten == 0) {
                bytesWritten += sc.write(ackBuffer);
            }
            sc.close();
    }
}
