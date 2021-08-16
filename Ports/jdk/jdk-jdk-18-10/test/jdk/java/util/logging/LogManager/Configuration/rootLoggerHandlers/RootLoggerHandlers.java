/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8191033
 * @build custom.DotHandler custom.Handler
 * @run main/othervm RootLoggerHandlers
 * @run main/othervm/java.security.policy==test.policy RootLoggerHandlers
 * @author danielfuchs
 */
public class RootLoggerHandlers {

    public static final Path SRC_DIR =
            Paths.get(System.getProperty("test.src", "src"));
    public static final Path USER_DIR =
            Paths.get(System.getProperty("user.dir", "."));
    public static final Path CONFIG_FILE = Paths.get("logging.properties");

    // Uncomment this to run the test on Java 8. Java 8 does not have
    // List.of(...)
    //    static final class List {
    //        static <T> java.util.List<T> of(T... items) {
    //            return Collections.unmodifiableList(Arrays.asList(items));
    //        }
    //    }

    public static void main(String[] args) throws IOException {
        Path initialProps = SRC_DIR.resolve(CONFIG_FILE);
        Path loggingProps = USER_DIR.resolve(CONFIG_FILE);
        System.setProperty("java.util.logging.config.file", loggingProps.toString());
        Files.copy(initialProps, loggingProps, StandardCopyOption.REPLACE_EXISTING);
        loggingProps.toFile().setWritable(true);
        System.out.println("Root level is: " + Logger.getLogger("").getLevel());
        if (Logger.getLogger("").getLevel() != Level.INFO) {
            throw new RuntimeException("Expected root level INFO, got: "
                                        + Logger.getLogger("").getLevel());
        }
        // Verify that we have two handlers. One was configured with
        // handlers=custom.Handler, the other with
        // .handlers=custom.DotHandler
        // Verify that exactly one of the two handlers is a custom.Handler
        // Verify that exactly one of the two handlers is a custom.DotHandler
        // Verify that the two handlers has an id of '1'
        checkHandlers(Logger.getLogger(""),
                Logger.getLogger("").getHandlers(),
                1L,
                custom.Handler.class,
                custom.DotHandler.class);
        checkHandlers(Logger.getLogger("global"),
                Logger.getGlobal().getHandlers(),
                1L,
                custom.GlobalHandler.class);

        // The log message "hi" should appear twice on the console.
        // We don't check that. This is just for log analysis in case
        // of test failure.
        Logger.getAnonymousLogger().info("hi");

        // Change the root logger level to FINE in the properties file
        // and reload the configuration.
        Files.write(loggingProps,
                Files.lines(initialProps)
                        .map((s) -> s.replace("INFO", "FINE"))
                        .collect(Collectors.toList()));
        LogManager.getLogManager().readConfiguration();

        System.out.println("Root level is: " + Logger.getLogger("").getLevel());
        if (Logger.getLogger("").getLevel() != Level.FINE) {
            throw new RuntimeException("Expected root level FINE, got: "
                    + Logger.getLogger("").getLevel());
        }

        // Verify that we have now only one handler, configured with
        // handlers=custom.Handler, and that the other configured with
        // .handlers=custom.DotHandler was ignored.
        // Verify that the handler is a custom.Handler
        // Verify that the handler has an id of '2'
        checkHandlers(Logger.getLogger(""),
                Logger.getLogger("").getHandlers(),
                2L,
                custom.Handler.class);
        checkHandlers(Logger.getGlobal(),
                Logger.getGlobal().getHandlers(),
                1L);

        // The log message "there" should appear only once on the console.
        // We don't check that. This is just for log analysis in case
        // of test failure.
        Logger.getAnonymousLogger().info("there!");

        // Change the root logger level to FINER in the properties file
        // and reload the configuration.
        Files.write(loggingProps,
                Files.lines(initialProps)
                        .map((s) -> s.replace("INFO", "FINER"))
                        .collect(Collectors.toList()));
        LogManager.getLogManager().readConfiguration();

        System.out.println("Root level is: " + Logger.getLogger("").getLevel());
        if (Logger.getLogger("").getLevel() != Level.FINER) {
            throw new RuntimeException("Expected root level FINER, got: "
                    + Logger.getLogger("").getLevel());
        }

        // Verify that we have only one handler, configured with
        // handlers=custom.Handler, and that the other configured with
        // .handlers=custom.DotHandler was ignored.
        // Verify that the handler is a custom.Handler
        // Verify that the handler has an id of '3'
        checkHandlers(Logger.getLogger(""),
                Logger.getLogger("").getHandlers(),
                3L,
                custom.Handler.class);
        checkHandlers(Logger.getGlobal(),
                Logger.getGlobal().getHandlers(),
                1L);

        LogManager.getLogManager().reset();
        LogManager.getLogManager().updateConfiguration((s) -> (o,n) -> n);
        // Verify that we have only one handler, configured with
        // handlers=custom.Handler, and that the other configured with
        // .handlers=custom.DotHandler was ignored.
        // Verify that the handler is a custom.Handler
        // Verify that the handler has an id of '4'
        checkHandlers(Logger.getLogger(""),
                Logger.getLogger("").getHandlers(),
                4L,
                custom.Handler.class);
        checkHandlers(Logger.getGlobal(),
                Logger.getGlobal().getHandlers(),
                2L,
                custom.GlobalHandler.class);

        LogManager.getLogManager().updateConfiguration((s) -> (o,n) -> n);
        // Verify that we have only one handler, configured with
        // handlers=custom.Handler, and that the other configured with
        // .handlers=custom.DotHandler was ignored.
        // Verify that the handler is a custom.Handler
        // Verify that the handler has an id of '4'
        checkHandlers(Logger.getLogger(""),
                Logger.getLogger("").getHandlers(),
                4L,
                custom.Handler.class);
        checkHandlers(Logger.getGlobal(),
                Logger.getGlobal().getHandlers(),
                2L,
                custom.GlobalHandler.class);


        // The log message "done" should appear only once on the console.
        // We don't check that. This is just for log analysis in case
        // of test failure.
        Logger.getAnonymousLogger().info("done!");
    }

    static void checkHandlers(Logger logger, Handler[] handlers, Long expectedID, Class<?>... clz) {
        // Verify that we have the expected number of handlers.
        if (Stream.of(handlers).count() != clz.length) {
            throw new RuntimeException("Expected " + clz.length + " handlers, got: "
                    + List.of(logger.getHandlers()));
        }
        for (Class<?> cl : clz) {
            // Verify that the handlers are of the expected class.
            // For each class, we should have exactly one handler
            // of that class.
            if (Stream.of(handlers)
                    .map(Object::getClass)
                    .filter(cl::equals)
                    .count() != 1) {
                throw new RuntimeException("Expected one " + cl +", got: "
                        + List.of(logger.getHandlers()));
            }
        }
        // Verify that all handlers have the expected ID
        if (Stream.of(logger.getHandlers())
                .map(RootLoggerHandlers::getId)
                .filter(expectedID::equals)
                .count() != clz.length) {
            throw new RuntimeException("Expected ids to be " + expectedID + ", got: "
                    + List.of(logger.getHandlers()));
        }
    }

    static long getId(Handler h) {
        if (h instanceof custom.Handler) {
            return ((custom.Handler)h).id;
        }
        if (h instanceof custom.DotHandler) {
            return ((custom.DotHandler)h).id;
        }
        if (h instanceof custom.GlobalHandler) {
            return ((custom.GlobalHandler)h).id;
        }
        return -1;
    }
}
