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
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @test
 * @bug 8026863
 * @summary checks that anonymous logger setters work as expected when a
 *          security manager is set, and checks that getters return expected
 *          values.
 * @run main/othervm -Djava.security.manager=allow TestAnonymousLogger
 */
public class TestAnonymousLogger {

    public static final class TestHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            System.out.println(new SimpleFormatter().format(record));
        }
        @Override
        public void flush() {
            System.out.flush();
        }
        @Override
        public void close() {
            flush();
        }
    }

    public static final class TestFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            return true;
        }
    }

    public static final class TestBundle extends ResourceBundle {
        Set<String> keys = Collections.synchronizedSet(new LinkedHashSet<>());
        @Override
        protected Object handleGetObject(String key) {
            keys.add(key);
            return "[LOCALIZED] "+key;
        }
        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(keys);
        }
    }

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());
        Logger anonymous = Logger.getAnonymousLogger();

        final TestHandler handler = new TestHandler();
        final TestFilter filter = new TestFilter();
        final ResourceBundle bundle = ResourceBundle.getBundle(TestBundle.class.getName());
        anonymous.setLevel(Level.FINEST);
        anonymous.addHandler(handler);
        anonymous.setFilter(filter);
        anonymous.setUseParentHandlers(true);
        anonymous.setResourceBundle(bundle);

        if (anonymous.getLevel() != Level.FINEST) {
            throw new RuntimeException("Unexpected level: " + anonymous.getLevel());
        } else {
            System.out.println("Got expected level: " + anonymous.getLevel());
        }
        if (!Arrays.asList(anonymous.getHandlers()).contains(handler)) {
            throw new RuntimeException("Expected handler not found in: "
                    + Arrays.asList(anonymous.getHandlers()));
        } else {
            System.out.println("Got expected handler in: " + Arrays.asList(anonymous.getHandlers()));
        }
        if (anonymous.getFilter() != filter) {
            throw new RuntimeException("Unexpected filter: " + anonymous.getFilter());
        } else {
            System.out.println("Got expected filter: " + anonymous.getFilter());
        }
        if (!anonymous.getUseParentHandlers()) {
            throw new RuntimeException("Unexpected flag: " + anonymous.getUseParentHandlers());
        } else {
            System.out.println("Got expected flag: " + anonymous.getUseParentHandlers());
        }
        if (anonymous.getResourceBundle() != bundle) {
            throw new RuntimeException("Unexpected bundle: " + anonymous.getResourceBundle());
        } else {
            System.out.println("Got expected bundle: " + anonymous.getResourceBundle());
        }
        try {
            anonymous.setParent(Logger.getLogger("foo.bar"));
            throw new RuntimeException("Expected SecurityException not raised!");
        } catch (SecurityException x) {
            System.out.println("Got expected exception: " + x);
        }
        if (anonymous.getParent() != Logger.getLogger("")) {
            throw new RuntimeException("Unexpected parent: " + anonymous.getParent());
        } else {
            System.out.println("Got expected parent: " + anonymous.getParent());
        }
    }

}
