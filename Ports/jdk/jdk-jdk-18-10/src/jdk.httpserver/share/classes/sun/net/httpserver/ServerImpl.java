/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.net.httpserver;

import java.net.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.net.httpserver.HttpConnection.State;

/**
 * Provides implementation for both HTTP and HTTPS
 */
class ServerImpl implements TimeSource {

    private String protocol;
    private boolean https;
    private Executor executor;
    private HttpsConfigurator httpsConfig;
    private SSLContext sslContext;
    private ContextList contexts;
    private InetSocketAddress address;
    private ServerSocketChannel schan;
    private Selector selector;
    private SelectionKey listenerKey;
    private Set<HttpConnection> idleConnections;
    private Set<HttpConnection> allConnections;
    /* following two are used to keep track of the times
     * when a connection/request is first received
     * and when we start to send the response
     */
    private Set<HttpConnection> reqConnections;
    private Set<HttpConnection> rspConnections;
    private List<Event> events;
    private Object lolock = new Object();
    private volatile boolean finished = false;
    private volatile boolean terminating = false;
    private boolean bound = false;
    private boolean started = false;
    private volatile long time;  /* current time */
    private volatile long subticks = 0;
    private volatile long ticks; /* number of clock ticks since server started */
    private HttpServer wrapper;

    final static int CLOCK_TICK = ServerConfig.getClockTick();
    final static long IDLE_INTERVAL = ServerConfig.getIdleInterval();
    final static int MAX_IDLE_CONNECTIONS = ServerConfig.getMaxIdleConnections();
    final static long TIMER_MILLIS = ServerConfig.getTimerMillis ();
    final static long MAX_REQ_TIME=getTimeMillis(ServerConfig.getMaxReqTime());
    final static long MAX_RSP_TIME=getTimeMillis(ServerConfig.getMaxRspTime());
    final static boolean timer1Enabled = MAX_REQ_TIME != -1 || MAX_RSP_TIME != -1;

    private Timer timer, timer1;
    private final Logger logger;
    private Thread dispatcherThread;

    ServerImpl (
        HttpServer wrapper, String protocol, InetSocketAddress addr, int backlog
    ) throws IOException {

        this.protocol = protocol;
        this.wrapper = wrapper;
        this.logger = System.getLogger ("com.sun.net.httpserver");
        ServerConfig.checkLegacyProperties (logger);
        https = protocol.equalsIgnoreCase ("https");
        this.address = addr;
        contexts = new ContextList();
        schan = ServerSocketChannel.open();
        if (addr != null) {
            ServerSocket socket = schan.socket();
            socket.bind (addr, backlog);
            bound = true;
        }
        selector = Selector.open ();
        schan.configureBlocking (false);
        listenerKey = schan.register (selector, SelectionKey.OP_ACCEPT);
        dispatcher = new Dispatcher();
        idleConnections = Collections.synchronizedSet (new HashSet<HttpConnection>());
        allConnections = Collections.synchronizedSet (new HashSet<HttpConnection>());
        reqConnections = Collections.synchronizedSet (new HashSet<HttpConnection>());
        rspConnections = Collections.synchronizedSet (new HashSet<HttpConnection>());
        time = System.currentTimeMillis();
        timer = new Timer ("server-timer", true);
        timer.schedule (new ServerTimerTask(), CLOCK_TICK, CLOCK_TICK);
        if (timer1Enabled) {
            timer1 = new Timer ("server-timer1", true);
            timer1.schedule (new ServerTimerTask1(),TIMER_MILLIS,TIMER_MILLIS);
            logger.log (Level.DEBUG, "HttpServer timer1 enabled period in ms: ", TIMER_MILLIS);
            logger.log (Level.DEBUG, "MAX_REQ_TIME:  "+MAX_REQ_TIME);
            logger.log (Level.DEBUG, "MAX_RSP_TIME:  "+MAX_RSP_TIME);
        }
        events = new LinkedList<Event>();
        logger.log (Level.DEBUG, "HttpServer created "+protocol+" "+ addr);
    }

    public void bind (InetSocketAddress addr, int backlog) throws IOException {
        if (bound) {
            throw new BindException ("HttpServer already bound");
        }
        if (addr == null) {
            throw new NullPointerException ("null address");
        }
        ServerSocket socket = schan.socket();
        socket.bind (addr, backlog);
        bound = true;
    }

    public void start () {
        if (!bound || started || finished) {
            throw new IllegalStateException ("server in wrong state");
        }
        if (executor == null) {
            executor = new DefaultExecutor();
        }
        dispatcherThread = new Thread(null, dispatcher, "HTTP-Dispatcher", 0, false);
        started = true;
        dispatcherThread.start();
    }

    public void setExecutor (Executor executor) {
        if (started) {
            throw new IllegalStateException ("server already started");
        }
        this.executor = executor;
    }

    private static class DefaultExecutor implements Executor {
        public void execute (Runnable task) {
            task.run();
        }
    }

    public Executor getExecutor () {
        return executor;
    }

    public void setHttpsConfigurator (HttpsConfigurator config) {
        if (config == null) {
            throw new NullPointerException ("null HttpsConfigurator");
        }
        if (started) {
            throw new IllegalStateException ("server already started");
        }
        this.httpsConfig = config;
        sslContext = config.getSSLContext();
    }

    public HttpsConfigurator getHttpsConfigurator () {
        return httpsConfig;
    }

    public final boolean isFinishing() {
        return finished;
    }

    public void stop (int delay) {
        if (delay < 0) {
            throw new IllegalArgumentException ("negative delay parameter");
        }
        terminating = true;
        try { schan.close(); } catch (IOException e) {}
        selector.wakeup();
        long latest = System.currentTimeMillis() + delay * 1000;
        while (System.currentTimeMillis() < latest) {
            delay();
            if (finished) {
                break;
            }
        }
        finished = true;
        selector.wakeup();
        synchronized (allConnections) {
            for (HttpConnection c : allConnections) {
                c.close();
            }
        }
        allConnections.clear();
        idleConnections.clear();
        timer.cancel();
        if (timer1Enabled) {
            timer1.cancel();
        }
        if (dispatcherThread != null && dispatcherThread != Thread.currentThread()) {
            try {
                dispatcherThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log (Level.TRACE, "ServerImpl.stop: ", e);
            }
        }
    }

    Dispatcher dispatcher;

    public synchronized HttpContextImpl createContext (String path, HttpHandler handler) {
        if (handler == null || path == null) {
            throw new NullPointerException ("null handler, or path parameter");
        }
        HttpContextImpl context = new HttpContextImpl (protocol, path, handler, this);
        contexts.add (context);
        logger.log (Level.DEBUG, "context created: " + path);
        return context;
    }

    public synchronized HttpContextImpl createContext (String path) {
        if (path == null) {
            throw new NullPointerException ("null path parameter");
        }
        HttpContextImpl context = new HttpContextImpl (protocol, path, null, this);
        contexts.add (context);
        logger.log (Level.DEBUG, "context created: " + path);
        return context;
    }

    public synchronized void removeContext (String path) throws IllegalArgumentException {
        if (path == null) {
            throw new NullPointerException ("null path parameter");
        }
        contexts.remove (protocol, path);
        logger.log (Level.DEBUG, "context removed: " + path);
    }

    public synchronized void removeContext (HttpContext context) throws IllegalArgumentException {
        if (!(context instanceof HttpContextImpl)) {
            throw new IllegalArgumentException ("wrong HttpContext type");
        }
        contexts.remove ((HttpContextImpl)context);
        logger.log (Level.DEBUG, "context removed: " + context.getPath());
    }

    @SuppressWarnings("removal")
    public InetSocketAddress getAddress() {
        return AccessController.doPrivileged(
                new PrivilegedAction<InetSocketAddress>() {
                    public InetSocketAddress run() {
                        return
                            (InetSocketAddress)schan.socket()
                                .getLocalSocketAddress();
                    }
                });
    }

    Selector getSelector () {
        return selector;
    }

    void addEvent (Event r) {
        synchronized (lolock) {
            events.add (r);
            selector.wakeup();
        }
    }

    /* main server listener task */

    class Dispatcher implements Runnable {

        private void handleEvent (Event r) {
            ExchangeImpl t = r.exchange;
            HttpConnection c = t.getConnection();
            try {
                if (r instanceof WriteFinishedEvent) {

                    logger.log(Level.TRACE, "Write Finished");
                    int exchanges = endExchange();
                    if (terminating && exchanges == 0) {
                        finished = true;
                    }
                    LeftOverInputStream is = t.getOriginalInputStream();
                    if (!is.isEOF()) {
                        t.close = true;
                        if (c.getState() == State.REQUEST) {
                            requestCompleted(c);
                        }
                    }
                    responseCompleted (c);
                    if (t.close || idleConnections.size() >= MAX_IDLE_CONNECTIONS) {
                        c.close();
                        allConnections.remove (c);
                    } else {
                        if (is.isDataBuffered()) {
                            /* don't re-enable the interestops, just handle it */
                            requestStarted (c);
                            handle (c.getChannel(), c);
                        } else {
                            connsToRegister.add (c);
                        }
                    }
                }
            } catch (IOException e) {
                logger.log (
                    Level.TRACE, "Dispatcher (1)", e
                );
                c.close();
            }
        }

        final LinkedList<HttpConnection> connsToRegister =
                new LinkedList<HttpConnection>();

        void reRegister (HttpConnection c) {
            /* re-register with selector */
            try {
                SocketChannel chan = c.getChannel();
                chan.configureBlocking (false);
                SelectionKey key = chan.register (selector, SelectionKey.OP_READ);
                key.attach (c);
                c.selectionKey = key;
                c.time = getTime() + IDLE_INTERVAL;
                idleConnections.add (c);
            } catch (IOException e) {
                dprint(e);
                logger.log (Level.TRACE, "Dispatcher(8)", e);
                c.close();
            }
        }

        public void run() {
            while (!finished) {
                try {
                    List<Event> list = null;
                    synchronized (lolock) {
                        if (events.size() > 0) {
                            list = events;
                            events = new LinkedList<Event>();
                        }
                    }

                    if (list != null) {
                        for (Event r: list) {
                            handleEvent (r);
                        }
                    }

                    for (HttpConnection c : connsToRegister) {
                        reRegister(c);
                    }
                    connsToRegister.clear();

                    selector.select(1000);

                    /* process the selected list now  */
                    Set<SelectionKey> selected = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selected.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove ();
                        if (key.equals (listenerKey)) {
                            if (terminating) {
                                continue;
                            }
                            SocketChannel chan = schan.accept();

                            // optimist there's a channel
                            if (chan != null) {
                                // Set TCP_NODELAY, if appropriate
                                if (ServerConfig.noDelay()) {
                                    chan.socket().setTcpNoDelay(true);
                                }
                                chan.configureBlocking (false);
                                SelectionKey newkey =
                                    chan.register (selector, SelectionKey.OP_READ);
                                HttpConnection c = new HttpConnection ();
                                c.selectionKey = newkey;
                                c.setChannel (chan);
                                newkey.attach (c);
                                requestStarted (c);
                                allConnections.add (c);
                            }
                        } else {
                            try {
                                if (key.isReadable()) {
                                    SocketChannel chan = (SocketChannel)key.channel();
                                    HttpConnection conn = (HttpConnection)key.attachment();

                                    key.cancel();
                                    chan.configureBlocking (true);
                                    if (idleConnections.remove(conn)) {
                                        // was an idle connection so add it
                                        // to reqConnections set.
                                        requestStarted (conn);
                                    }
                                    handle (chan, conn);
                                } else {
                                    assert false : "Unexpected non-readable key:" + key;
                                }
                            } catch (CancelledKeyException e) {
                                handleException(key, null);
                            } catch (IOException e) {
                                handleException(key, e);
                            }
                        }
                    }
                    // call the selector just to process the cancelled keys
                    selector.selectNow();
                } catch (IOException e) {
                    logger.log (Level.TRACE, "Dispatcher (4)", e);
                } catch (Exception e) {
                    logger.log (Level.TRACE, "Dispatcher (7)", e);
                }
            }
            try {selector.close(); } catch (Exception e) {}
        }

        private void handleException (SelectionKey key, Exception e) {
            HttpConnection conn = (HttpConnection)key.attachment();
            if (e != null) {
                logger.log (Level.TRACE, "Dispatcher (2)", e);
            }
            closeConnection(conn);
        }

        public void handle (SocketChannel chan, HttpConnection conn)
        {
            try {
                Exchange t = new Exchange (chan, protocol, conn);
                executor.execute (t);
            } catch (HttpError e1) {
                logger.log (Level.TRACE, "Dispatcher (4)", e1);
                closeConnection(conn);
            } catch (IOException e) {
                logger.log (Level.TRACE, "Dispatcher (5)", e);
                closeConnection(conn);
            } catch (Throwable e) {
                logger.log (Level.TRACE, "Dispatcher (6)", e);
                closeConnection(conn);
            }
        }
    }

    static boolean debug = ServerConfig.debugEnabled ();

    static synchronized void dprint (String s) {
        if (debug) {
            System.out.println (s);
        }
    }

    static synchronized void dprint (Exception e) {
        if (debug) {
            System.out.println (e);
            e.printStackTrace();
        }
    }

    Logger getLogger () {
        return logger;
    }

    private void closeConnection(HttpConnection conn) {
        conn.close();
        allConnections.remove(conn);
        switch (conn.getState()) {
        case REQUEST:
            reqConnections.remove(conn);
            break;
        case RESPONSE:
            rspConnections.remove(conn);
            break;
        case IDLE:
            idleConnections.remove(conn);
            break;
        }
        assert !reqConnections.remove(conn);
        assert !rspConnections.remove(conn);
        assert !idleConnections.remove(conn);
    }

        /* per exchange task */

    class Exchange implements Runnable {
        SocketChannel chan;
        HttpConnection connection;
        HttpContextImpl context;
        InputStream rawin;
        OutputStream rawout;
        String protocol;
        ExchangeImpl tx;
        HttpContextImpl ctx;
        boolean rejected = false;

        Exchange (SocketChannel chan, String protocol, HttpConnection conn) throws IOException {
            this.chan = chan;
            this.connection = conn;
            this.protocol = protocol;
        }

        public void run () {
            /* context will be null for new connections */
            logger.log(Level.TRACE, "exchange started");
            context = connection.getHttpContext();
            boolean newconnection;
            SSLEngine engine = null;
            String requestLine = null;
            SSLStreams sslStreams = null;
            try {
                if (context != null ) {
                    this.rawin = connection.getInputStream();
                    this.rawout = connection.getRawOutputStream();
                    newconnection = false;
                } else {
                    /* figure out what kind of connection this is */
                    newconnection = true;
                    if (https) {
                        if (sslContext == null) {
                            logger.log (Level.WARNING,
                                "SSL connection received. No https context created");
                            throw new HttpError ("No SSL context established");
                        }
                        sslStreams = new SSLStreams (ServerImpl.this, sslContext, chan);
                        rawin = sslStreams.getInputStream();
                        rawout = sslStreams.getOutputStream();
                        engine = sslStreams.getSSLEngine();
                        connection.sslStreams = sslStreams;
                    } else {
                        rawin = new BufferedInputStream(
                            new Request.ReadStream (
                                ServerImpl.this, chan
                        ));
                        rawout = new Request.WriteStream (
                            ServerImpl.this, chan
                        );
                    }
                    connection.raw = rawin;
                    connection.rawout = rawout;
                }
                Request req = new Request (rawin, rawout);
                requestLine = req.requestLine();
                if (requestLine == null) {
                    /* connection closed */
                    logger.log(Level.DEBUG, "no request line: closing");
                    closeConnection(connection);
                    return;
                }
                logger.log(Level.DEBUG, "Exchange request line: {0}", requestLine);
                int space = requestLine.indexOf (' ');
                if (space == -1) {
                    reject (Code.HTTP_BAD_REQUEST,
                            requestLine, "Bad request line");
                    return;
                }
                String method = requestLine.substring (0, space);
                int start = space+1;
                space = requestLine.indexOf(' ', start);
                if (space == -1) {
                    reject (Code.HTTP_BAD_REQUEST,
                            requestLine, "Bad request line");
                    return;
                }
                String uriStr = requestLine.substring (start, space);
                URI uri = new URI (uriStr);
                start = space+1;
                String version = requestLine.substring (start);
                Headers headers = req.headers();
                /* check key for illegal characters */
                for (var k : headers.keySet()) {
                    if (!isValidHeaderKey(k)) {
                        reject(Code.HTTP_BAD_REQUEST, requestLine,
                                "Header key contains illegal characters");
                        return;
                    }
                }
                /* checks for unsupported combinations of lengths and encodings */
                if (headers.containsKey("Content-Length") &&
                        (headers.containsKey("Transfer-encoding") || headers.get("Content-Length").size() > 1)) {
                    reject(Code.HTTP_BAD_REQUEST, requestLine,
                            "Conflicting or malformed headers detected");
                    return;
                }
                long clen = 0L;
                String headerValue = null;
                List<String> teValueList = headers.get("Transfer-encoding");
                if (teValueList != null && !teValueList.isEmpty()) {
                    headerValue = teValueList.get(0);
                }
                if (headerValue != null) {
                    if (headerValue.equalsIgnoreCase("chunked") && teValueList.size() == 1) {
                        clen = -1L;
                    } else {
                        reject(Code.HTTP_NOT_IMPLEMENTED,
                                requestLine, "Unsupported Transfer-Encoding value");
                        return;
                    }
                } else {
                    headerValue = headers.getFirst("Content-Length");
                    if (headerValue != null) {
                        clen = Long.parseLong(headerValue);
                    }
                    if (clen == 0) {
                        requestCompleted(connection);
                    }
                }
                ctx = contexts.findContext (protocol, uri.getPath());
                if (ctx == null) {
                    reject (Code.HTTP_NOT_FOUND,
                            requestLine, "No context found for request");
                    return;
                }
                connection.setContext (ctx);
                if (ctx.getHandler() == null) {
                    reject (Code.HTTP_INTERNAL_ERROR,
                            requestLine, "No handler for context");
                    return;
                }
                tx = new ExchangeImpl (
                    method, uri, req, clen, connection
                );
                String chdr = headers.getFirst("Connection");
                Headers rheaders = tx.getResponseHeaders();

                if (chdr != null && chdr.equalsIgnoreCase ("close")) {
                    tx.close = true;
                }
                if (version.equalsIgnoreCase ("http/1.0")) {
                    tx.http10 = true;
                    if (chdr == null) {
                        tx.close = true;
                        rheaders.set ("Connection", "close");
                    } else if (chdr.equalsIgnoreCase ("keep-alive")) {
                        rheaders.set ("Connection", "keep-alive");
                        int idle=(int)(ServerConfig.getIdleInterval()/1000);
                        int max=ServerConfig.getMaxIdleConnections();
                        String val = "timeout="+idle+", max="+max;
                        rheaders.set ("Keep-Alive", val);
                    }
                }

                if (newconnection) {
                    connection.setParameters (
                        rawin, rawout, chan, engine, sslStreams,
                        sslContext, protocol, ctx, rawin
                    );
                }
                /* check if client sent an Expect 100 Continue.
                 * In that case, need to send an interim response.
                 * In future API may be modified to allow app to
                 * be involved in this process.
                 */
                String exp = headers.getFirst("Expect");
                if (exp != null && exp.equalsIgnoreCase ("100-continue")) {
                    logReply (100, requestLine, null);
                    sendReply (
                        Code.HTTP_CONTINUE, false, null
                    );
                }
                /* uf is the list of filters seen/set by the user.
                 * sf is the list of filters established internally
                 * and which are not visible to the user. uc and sc
                 * are the corresponding Filter.Chains.
                 * They are linked together by a LinkHandler
                 * so that they can both be invoked in one call.
                 */
                final List<Filter> sf = ctx.getSystemFilters();
                final List<Filter> uf = ctx.getFilters();

                final Filter.Chain sc = new Filter.Chain(sf, ctx.getHandler());
                final Filter.Chain uc = new Filter.Chain(uf, new LinkHandler (sc));

                /* set up the two stream references */
                tx.getRequestBody();
                tx.getResponseBody();
                if (https) {
                    uc.doFilter (new HttpsExchangeImpl (tx));
                } else {
                    uc.doFilter (new HttpExchangeImpl (tx));
                }

            } catch (IOException e1) {
                logger.log (Level.TRACE, "ServerImpl.Exchange (1)", e1);
                closeConnection(connection);
            } catch (NumberFormatException e2) {
                logger.log (Level.TRACE, "ServerImpl.Exchange (2)", e2);
                reject (Code.HTTP_BAD_REQUEST,
                        requestLine, "NumberFormatException thrown");
            } catch (URISyntaxException e3) {
                logger.log (Level.TRACE, "ServerImpl.Exchange (3)", e3);
                reject (Code.HTTP_BAD_REQUEST,
                        requestLine, "URISyntaxException thrown");
            } catch (Exception e4) {
                logger.log (Level.TRACE, "ServerImpl.Exchange (4)", e4);
                closeConnection(connection);
            } catch (Throwable t) {
                logger.log(Level.TRACE, "ServerImpl.Exchange (5)", t);
                throw t;
            }
        }

        /* used to link to 2 or more Filter.Chains together */

        class LinkHandler implements HttpHandler {
            Filter.Chain nextChain;

            LinkHandler (Filter.Chain nextChain) {
                this.nextChain = nextChain;
            }

            public void handle (HttpExchange exchange) throws IOException {
                nextChain.doFilter (exchange);
            }
        }

        void reject (int code, String requestStr, String message) {
            rejected = true;
            logReply (code, requestStr, message);
            sendReply (
                code, false, "<h1>"+code+Code.msg(code)+"</h1>"+message
            );
            closeConnection(connection);
        }

        void sendReply (
            int code, boolean closeNow, String text)
        {
            try {
                StringBuilder builder = new StringBuilder (512);
                builder.append ("HTTP/1.1 ")
                    .append (code).append (Code.msg(code)).append ("\r\n");

                if (text != null && text.length() != 0) {
                    builder.append ("Content-Length: ")
                        .append (text.length()).append ("\r\n")
                        .append ("Content-Type: text/html\r\n");
                } else {
                    builder.append ("Content-Length: 0\r\n");
                    text = "";
                }
                if (closeNow) {
                    builder.append ("Connection: close\r\n");
                }
                builder.append ("\r\n").append (text);
                String s = builder.toString();
                byte[] b = s.getBytes("ISO8859_1");
                rawout.write (b);
                rawout.flush();
                if (closeNow) {
                    closeConnection(connection);
                }
            } catch (IOException e) {
                logger.log (Level.TRACE, "ServerImpl.sendReply", e);
                closeConnection(connection);
            }
        }

    }

    void logReply (int code, String requestStr, String text) {
        if (!logger.isLoggable(Level.DEBUG)) {
            return;
        }
        if (text == null) {
            text = "";
        }
        String r;
        if (requestStr.length() > 80) {
           r = requestStr.substring (0, 80) + "<TRUNCATED>";
        } else {
           r = requestStr;
        }
        String message = r + " [" + code + " " +
                    Code.msg(code) + "] ("+text+")";
        logger.log (Level.DEBUG, message);
    }

    long getTicks() {
        return ticks;
    }

    public long getTime() {
        return time;
    }

    void delay () {
        Thread.yield();
        try {
            Thread.sleep (200);
        } catch (InterruptedException e) {}
    }

    private int exchangeCount = 0;

    synchronized void startExchange () {
        exchangeCount ++;
    }

    synchronized int endExchange () {
        exchangeCount --;
        assert exchangeCount >= 0;
        return exchangeCount;
    }

    HttpServer getWrapper () {
        return wrapper;
    }

    void requestStarted (HttpConnection c) {
        c.creationTime = getTime();
        c.setState (State.REQUEST);
        reqConnections.add (c);
    }

    // called after a request has been completely read
    // by the server. This stops the timer which would
    // close the connection if the request doesn't arrive
    // quickly enough. It then starts the timer
    // that ensures the client reads the response in a timely
    // fashion.

    void requestCompleted (HttpConnection c) {
        State s = c.getState();
        assert s == State.REQUEST : "State is not REQUEST ("+s+")";
        reqConnections.remove (c);
        c.rspStartedTime = getTime();
        rspConnections.add (c);
        c.setState (State.RESPONSE);
    }

    // called after response has been sent
    void responseCompleted (HttpConnection c) {
        State s = c.getState();
        assert s == State.RESPONSE : "State is not RESPONSE ("+s+")";
        rspConnections.remove (c);
        c.setState (State.IDLE);
    }

    /**
     * TimerTask run every CLOCK_TICK ms
     */
    class ServerTimerTask extends TimerTask {
        public void run () {
            LinkedList<HttpConnection> toClose = new LinkedList<HttpConnection>();
            time = System.currentTimeMillis();
            ticks ++;
            synchronized (idleConnections) {
                for (HttpConnection c : idleConnections) {
                    if (c.time <= time) {
                        toClose.add (c);
                    }
                }
                for (HttpConnection c : toClose) {
                    idleConnections.remove (c);
                    allConnections.remove (c);
                    c.close();
                }
            }
        }
    }

    class ServerTimerTask1 extends TimerTask {

        // runs every TIMER_MILLIS
        public void run () {
            LinkedList<HttpConnection> toClose = new LinkedList<HttpConnection>();
            time = System.currentTimeMillis();
            synchronized (reqConnections) {
                if (MAX_REQ_TIME != -1) {
                    for (HttpConnection c : reqConnections) {
                        if (c.creationTime + TIMER_MILLIS + MAX_REQ_TIME <= time) {
                            toClose.add (c);
                        }
                    }
                    for (HttpConnection c : toClose) {
                        logger.log (Level.DEBUG, "closing: no request: " + c);
                        reqConnections.remove (c);
                        allConnections.remove (c);
                        c.close();
                    }
                }
            }
            toClose = new LinkedList<HttpConnection>();
            synchronized (rspConnections) {
                if (MAX_RSP_TIME != -1) {
                    for (HttpConnection c : rspConnections) {
                        if (c.rspStartedTime + TIMER_MILLIS +MAX_RSP_TIME <= time) {
                            toClose.add (c);
                        }
                    }
                    for (HttpConnection c : toClose) {
                        logger.log (Level.DEBUG, "closing: no response: " + c);
                        rspConnections.remove (c);
                        allConnections.remove (c);
                        c.close();
                    }
                }
            }
        }
    }

    void logStackTrace (String s) {
        logger.log (Level.TRACE, s);
        StringBuilder b = new StringBuilder ();
        StackTraceElement[] e = Thread.currentThread().getStackTrace();
        for (int i=0; i<e.length; i++) {
            b.append (e[i].toString()).append("\n");
        }
        logger.log (Level.TRACE, b.toString());
    }

    static long getTimeMillis(long secs) {
        if (secs == -1) {
            return -1;
        } else {
            return secs * 1000;
        }
    }

    /*
     * Validates a RFC 7230 header-key.
     */
    static boolean isValidHeaderKey(String token) {
        if (token == null) return false;

        boolean isValidChar;
        char[] chars = token.toCharArray();
        String validSpecialChars = "!#$%&'*+-.^_`|~";
        for (char c : chars) {
            isValidChar = ((c >= 'a') && (c <= 'z')) ||
                          ((c >= 'A') && (c <= 'Z')) ||
                          ((c >= '0') && (c <= '9'));
            if (!isValidChar && validSpecialChars.indexOf(c) == -1) {
                return false;
            }
        }
        return !token.isEmpty();
    }
}
