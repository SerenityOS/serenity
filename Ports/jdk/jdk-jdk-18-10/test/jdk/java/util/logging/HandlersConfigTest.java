/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8029781 8030801
 * @summary Test which verifies that various JDK logging Handlers are
 *          configured correctly from defaults and/or LogManager properties
 *          as specified in javadoc and that no special
 *          logging permission is required for instantiating them.
 * @modules java.logging/java.util.logging:open
 * @run main/othervm -Djava.security.manager=allow HandlersConfigTest default
 * @run main/othervm -Djava.security.manager=allow HandlersConfigTest configured
 */

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.SocketHandler;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

public abstract class HandlersConfigTest implements Runnable {

    public static void main(String[] args) {
        switch (args.length == 1 ? args[0] : "usage") {
            case "default":
                new Default().run();
                break;
            case "configured":
                new Configured().run();
                break;
            default:
                System.err.println("Usage: HandlersConfigTest [default|configured]");
                break;
        }
    }

    static final String CONFIG_FILE_PROPERTY = "java.util.logging.config.file";
    final Field memoryHandlerTarget, memoryHandlerSize, streamHandlerOutput;
    final ServerSocket serverSocket;

    HandlersConfigTest() {
        // establish access to private fields
        try {
            memoryHandlerTarget = MemoryHandler.class.getDeclaredField("target");
            memoryHandlerTarget.setAccessible(true);
            memoryHandlerSize = MemoryHandler.class.getDeclaredField("size");
            memoryHandlerSize.setAccessible(true);
            streamHandlerOutput = StreamHandler.class.getDeclaredField("output");
            streamHandlerOutput.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }

        // load logging.propertes for the test
        String rname = getClass().getName().replace('.', '/') + ".props";
        URL url = getClass().getClassLoader().getResource(rname);
        if (url == null || !"file".equals(url.getProtocol())) {
            throw new IllegalStateException("Resource: " + rname + " not found or not on file: " + url);
        }
        System.setProperty(CONFIG_FILE_PROPERTY, url.getFile());

        // create ServerSocket as a target for SocketHandler
        try {
            serverSocket = new ServerSocket(0); // auto allocated port
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        // activate security
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkConnect(String host, int port) {
                // allow socket connections
            }
        });

        // initialize logging system
        LogManager.getLogManager();
    }

    // check that defaults are used as specified by javadoc

    public static class Default extends HandlersConfigTest {
        public static void main(String[] args) {
            new Default().run();
        }

        @Override
        public void run() {
            // MemoryHandler

            check(new MemoryHandler(),
                Level.ALL, null, null, SimpleFormatter.class,
                ConfiguredHandler.class, 1000, Level.SEVERE);

            check(new MemoryHandler(new SpecifiedHandler(), 100, Level.WARNING),
                Level.ALL, null, null, SimpleFormatter.class,
                SpecifiedHandler.class, 100, Level.WARNING);

            // StreamHandler

            check(new StreamHandler(),
                Level.INFO, null, null, SimpleFormatter.class,
                null);

            check(new StreamHandler(System.out, new SpecifiedFormatter()),
                Level.INFO, null, null, SpecifiedFormatter.class,
                System.out);

            // ConsoleHandler

            check(new ConsoleHandler(),
                Level.INFO, null, null, SimpleFormatter.class,
                System.err);

            // SocketHandler (use the ServerSocket's port)

            try {
                check(new SocketHandler("localhost", serverSocket.getLocalPort()),
                    Level.ALL, null, null, XMLFormatter.class);
            } catch (IOException e) {
                throw new RuntimeException("Can't connect to localhost:" + serverSocket.getLocalPort(), e);
            }
        }
    }

    // check that LogManager properties configuration is respected

    public static class Configured extends HandlersConfigTest {
        public static void main(String[] args) {
            new Configured().run();
        }

        @Override
        public void run() {
            // MemoryHandler

            check(new MemoryHandler(),
                Level.FINE, null, ConfiguredFilter.class, ConfiguredFormatter.class,
                ConfiguredHandler.class, 123, Level.FINE);

            check(new MemoryHandler(new SpecifiedHandler(), 100, Level.WARNING),
                Level.FINE, null, ConfiguredFilter.class, ConfiguredFormatter.class,
                SpecifiedHandler.class, 100, Level.WARNING);

            // StreamHandler

            check(new StreamHandler(),
                Level.FINE, "ASCII", ConfiguredFilter.class, ConfiguredFormatter.class,
                null);

            check(new StreamHandler(System.out, new SpecifiedFormatter()),
                Level.FINE, "ASCII", ConfiguredFilter.class, SpecifiedFormatter.class,
                System.out);

            // ConsoleHandler

            check(new ConsoleHandler(),
                Level.FINE, "ASCII", ConfiguredFilter.class, ConfiguredFormatter.class,
                System.err);

            // SocketHandler (use the ServerSocket's port)

            try {
                check(new SocketHandler("localhost", serverSocket.getLocalPort()),
                    Level.FINE, "ASCII", ConfiguredFilter.class, ConfiguredFormatter.class);
            } catch (Exception e) {
                throw new RuntimeException("Can't connect to localhost:" + serverSocket.getLocalPort(), e);
            }
        }
    }

    // test infrastructure

    void check(Handler handler,
               Level expectedLevel,
               String expectedEncoding,
               Class<? extends Filter> expectedFilterType,
               Class<? extends Formatter> expectedFormatterType) {
        checkEquals(handler, "level", handler.getLevel(), expectedLevel);
        checkEquals(handler, "encoding", handler.getEncoding(), expectedEncoding);
        checkType(handler, "filter", handler.getFilter(), expectedFilterType);
        checkType(handler, "formatter", handler.getFormatter(), expectedFormatterType);
    }

    void check(MemoryHandler handler,
               Level expectedLevel,
               String expectedEncoding,
               Class<? extends Filter> expectedFilterType,
               Class<? extends Formatter> expectedFormatterType,
               Class<? extends Handler> expextedTargetType,
               int expextedSize,
               Level expectedPushLevel) {
        checkType(handler, "target", getTarget(handler), expextedTargetType);
        checkEquals(handler, "size", getSize(handler), expextedSize);
        checkEquals(handler, "pushLevel", handler.getPushLevel(), expectedPushLevel);
        check(handler, expectedLevel, expectedEncoding, expectedFilterType, expectedFormatterType);
    }

    void check(StreamHandler handler,
               Level expectedLevel,
               String expectedEncoding,
               Class<? extends Filter> expectedFilterType,
               Class<? extends Formatter> expectedFormatterType,
               OutputStream expectedOutputStream) {
        checkEquals(handler, "outputStream", getOutput(handler), expectedOutputStream);
        check(handler, expectedLevel, expectedEncoding, expectedFilterType, expectedFormatterType);
    }

    <T> void checkEquals(Handler handler, String property, T value, T expectedValue) {
        if (!Objects.equals(value, expectedValue)) {
            fail(handler, property + ": " + value + ", expected " + property + ": " + expectedValue);
        }
    }

    <T> void checkType(Handler handler, String property, T value, Class<? extends T> expectedType) {
        if (!(expectedType == null && value == null || expectedType != null && expectedType.isInstance(value))) {
            Class<?> type = value == null ? null : value.getClass();
            fail(handler, property + " type: " + type + ", expected " + property + " type: " + expectedType);
        }
    }

    void fail(Handler handler, String message) {
        throw new AssertionError("Handler: " + handler.getClass().getName() +
                                 ", configured with: " + getClass().getName() +
                                 ", " + message);
    }

    Handler getTarget(MemoryHandler memoryHandler) {
        try {
            return (Handler) memoryHandlerTarget.get(memoryHandler);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    int getSize(MemoryHandler memoryHandler) {
        try {
            return (int) memoryHandlerSize.get(memoryHandler);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    OutputStream getOutput(StreamHandler streamHandler) {
        try {
            return (OutputStream) streamHandlerOutput.get(streamHandler);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    // various independent types of Formatters, Filters, Handlers...

    public static class SpecifiedFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.valueOf(record);
        }
    }

    public static class SpecifiedHandler extends Handler {
        @Override
        public void publish(LogRecord record) { }

        @Override
        public void flush() { }

        @Override
        public void close() throws SecurityException { }
    }

    public static class ConfiguredFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return String.valueOf(record);
        }
    }

    public static class ConfiguredFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            return true;
        }
    }

    public static class ConfiguredHandler extends Handler {
        @Override
        public void publish(LogRecord record) { }

        @Override
        public void flush() { }

        @Override
        public void close() throws SecurityException { }
    }
}
