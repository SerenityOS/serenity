/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Paths;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @test
 * @bug 8172886
 * @summary Verifies that a custom LogManager or custom Handler can be
 *          instantiated by the logging system if they are in a package
 *          that is exported to java.logging by a module.
 * @build test.logmanager/test.logmanager.TestLogManager
 *        test.handlers/test.handlers.TestHandler
 *        test.config/test.config.LogConfig
 *        LogManagerInModuleTest
 * @run main/othervm --add-modules test.logmanager,test.handlers
 *          -Djava.util.logging.manager=test.logmanager.TestLogManager
 *          LogManagerInModuleTest
 * @run main/othervm --add-modules test.logmanager,test.handlers,test.config
 *          -Djava.util.logging.manager=test.logmanager.TestLogManager
 *          -Djava.util.logging.config.class=test.config.LogConfig
 *          LogManagerInModuleTest
 *
 * @author danielfuchs
 */
public class LogManagerInModuleTest {

    public static void main(String[] args) throws Exception {
        if (System.getProperty("java.util.logging.config.class", null) == null) {
            System.setProperty("java.util.logging.config.file",
                Paths.get(System.getProperty("test.src", "src"),
                          "logging.properties").toString());
        }
        // sanity check
        if (LogManagerInModuleTest.class.getModule().isNamed()) {
            throw new RuntimeException("Unexpected named module for "
                  + LogManagerInModuleTest.class + ": "
                  + LogManagerInModuleTest.class.getModule().getName());
        }

        // now check that the LogManager was correctly instantiated.
        LogManager manager = LogManager.getLogManager();
        System.out.println("LogManager: " + manager);
        Class<?> logManagerClass = manager.getClass();
        if (!"test.logmanager".equals(logManagerClass.getModule().getName())) {
            throw new RuntimeException("Bad module for log manager: "
                    + logManagerClass.getModule() + "; class is: "
                    + logManagerClass.getName());
        }

        Logger logger = Logger.getLogger("com.xyz.foo");
        Handler[] handlers = logger.getHandlers();
        if (handlers.length != 1) {
            throw new RuntimeException("Expected 1 handler, found " + handlers.length);
        }
        Class<?> handlerClass = handlers[0].getClass();
        if (!"test.handlers".equals(handlerClass.getModule().getName())) {
            throw new RuntimeException("Bad module for handler: "
                    + handlerClass.getModule() + "; class is: "
                    + handlerClass.getName());
        }
    }

}
