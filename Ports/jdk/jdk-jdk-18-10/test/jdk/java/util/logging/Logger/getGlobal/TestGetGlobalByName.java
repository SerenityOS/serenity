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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @test
 * @bug 7184195
 * @summary checks that java.util.logging.Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info() logs without configuration
 * @build TestGetGlobalByName testgetglobal.HandlerImpl testgetglobal.LogManagerImpl1 testgetglobal.LogManagerImpl2 testgetglobal.LogManagerImpl3 testgetglobal.BadLogManagerImpl testgetglobal.DummyLogManagerImpl
 * @run main/othervm TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager TestGetGlobalByName
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl1 TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl TestGetGlobalByName
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl2 TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl2 TestGetGlobalByName
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.LogManagerImpl3 TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.LogManagerImpl3 TestGetGlobalByName
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.BadLogManagerImpl TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.BadLogManagerImpl TestGetGlobalByName
 * @run main/othervm -Djava.util.logging.manager=testgetglobal.DummyLogManagerImpl TestGetGlobalByName
 * @run main/othervm/policy=policy -Djava.security.manager -Djava.util.logging.manager=testgetglobal.DummyLogManagerImpl TestGetGlobalByName
 * @author danielfuchs
 */
public class TestGetGlobalByName {

    static final String[] messages = {
        "1. This message should not appear on the console.",
        "2. This message should appear on the console.",
        "3. This message should now appear on the console too."
    };

    static {
        System.setProperty("java.util.logging.config.file",
            System.getProperty("test.src", ".") + java.io.File.separator + "logging.properties");
    }

    public static void main(String... args) {

        Logger.global.info(messages[0]); // at this point LogManager is not
             // initialized yet, so this message should not appear.
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(messages[1]); // calling getLogger() will
             // initialize the LogManager - and thus this message should appear.
        Logger.global.info(messages[2]); // Now that the LogManager is
             // initialized, this message should appear too.

        final List<String> expected = Arrays.asList(Arrays.copyOfRange(messages, 1, messages.length));
        if (!testgetglobal.HandlerImpl.received.equals(expected)) {
            throw new Error("Unexpected message list: "+testgetglobal.HandlerImpl.received+" vs "+ expected);
        }
    }
}
