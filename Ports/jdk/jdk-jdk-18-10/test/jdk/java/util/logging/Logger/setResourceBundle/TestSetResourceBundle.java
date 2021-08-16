/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessControlException;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyPermission;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import resources.ListBundle;

/**
 * @test
 * @bug 8013839 8189291
 * @summary tests Logger.setResourceBundle;
 * @build TestSetResourceBundle resources.ListBundle resources.ListBundle_fr
 * @run main/othervm TestSetResourceBundle UNSECURE
 * @run main/othervm -Djava.security.manager=allow TestSetResourceBundle PERMISSION
 * @run main/othervm -Djava.security.manager=allow TestSetResourceBundle SECURE
 * @author danielfuchs
 */
public class TestSetResourceBundle {

    static final Policy DEFAULT_POLICY = Policy.getPolicy();
    static final String LIST_BUNDLE_NAME = "resources.ListBundle";
    static final String PROPERTY_BUNDLE_NAME = "resources.PropertyBundle";

    /**
     * A dummy handler class that we can use to check the bundle/bundle name
     * that was present in the last LogRecord instance published.
     */
    static final class TestHandler extends Handler {
        volatile ResourceBundle lastBundle = null;
        volatile String lastBundleName = null;
        @Override
        public void publish(LogRecord record) {
            lastBundle = record.getResourceBundle();
            lastBundleName = record.getResourceBundleName();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * We will test setResourceBundle() in 3 configurations.
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         LoggingPermission("control") granted.
     * PERMISSION: With the security manager present - and the required
     *         LoggingPermission("control") *not* granted. Here we will
     *         test that the expected security permission is thrown.
     */
    public static enum TestCase {
        UNSECURE, SECURE, PERMISSION;
        public void run(String name) throws Exception {
            System.out.println("Running test case: " + name());
            switch (this) {
                case UNSECURE:
                    testUnsecure(name);
                    break;
                case SECURE:
                    testSecure(name);
                    break;
                case PERMISSION:
                    testPermission(name);
                    break;
                default:
                    throw new Error("Unknown test case: "+this);
            }
        }
        public String loggerName(String name) {
            return name().toLowerCase(Locale.ROOT) + "." + name;
        }
    }

    public static void main(String... args) throws Exception {

        Locale defaultLocale = Locale.getDefault();

        if (args == null || args.length == 0) {
            args = new String[] {
                TestCase.UNSECURE.name(),
                TestCase.SECURE.name()
            };
        }

        for (String testName : args) {
            TestCase test = TestCase.valueOf(testName);
            try {
                test.run(test.loggerName("foo.bar"));
            } finally {
                Locale.setDefault(defaultLocale);
            }
        }
    }

    /**
     * Test without security manager.
     * @param loggerName The logger to use.
     * @throws Exception if the test fails.
     */
    public static void testUnsecure(String loggerName) throws Exception {
        if (System.getSecurityManager() != null) {
            throw new Error("Security manager is set");
        }
        test(loggerName);
    }

    /**
     * Test with security manager.
     * @param loggerName The logger to use.
     * @throws Exception if the test fails.
     */
    public static void testSecure(String loggerName) throws Exception {
        if (System.getSecurityManager() != null) {
            throw new Error("Security manager is already set");
        }
        Policy.setPolicy(new SimplePolicy(TestCase.SECURE));
        System.setSecurityManager(new SecurityManager());
        test(loggerName);
    }

    /**
     * Test the LoggingPermission("control") is required.
     * @param loggerName The logger to use.
     */
    public static void testPermission(String loggerName) {
        if (System.getSecurityManager() != null) {
            throw new Error("Security manager is already set");
        }
        Policy.setPolicy(new SimplePolicy(TestCase.PERMISSION));
        System.setSecurityManager(new SecurityManager());
        final ResourceBundle bundle = ResourceBundle.getBundle(LIST_BUNDLE_NAME);
        Logger foobar = Logger.getLogger(loggerName);
        try {
            foobar.setResourceBundle(bundle);
            throw new RuntimeException("Permission not checked!");
        } catch (AccessControlException x) {
            if (x.getPermission() instanceof LoggingPermission) {
                if ("control".equals(x.getPermission().getName())) {
                    System.out.println("Got expected exception: " + x);
                    return;
                }
            }
            throw new RuntimeException("Unexpected exception: "+x, x);
        }

    }

    static String getBaseName(ResourceBundle bundle) {
        return bundle == null ? null : bundle.getBaseBundleName();
    }

    public static void test(String loggerName) throws Exception {

        System.out.println("Starting test for " + loggerName);

        final ResourceBundle bundle = ResourceBundle.getBundle(LIST_BUNDLE_NAME);
        Logger foobar = Logger.getLogger(loggerName);

        // Checks that IAE is thrown if the bundle has a null base name.
        try {
            foobar.setResourceBundle(new ListBundle());
            throw new RuntimeException("Expected exception not raised!");
        } catch (IllegalArgumentException x) {
            System.out.println("Got expected exception: " + x);
        }

        // Verify that resource bundle was not set.
        if (foobar.getResourceBundle() != null) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobar.getResourceBundle());
        }
        if (foobar.getResourceBundleName() != null) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobar.getResourceBundleName());
        }

        // Set acceptable resource bundle on logger.
        foobar.setResourceBundle(bundle);

        // check that the bundle has been set correctly
        if (bundle != foobar.getResourceBundle()) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobar.getResourceBundle());
        }
        if (!Objects.equals(getBaseName(bundle), foobar.getResourceBundleName())) {
            throw new RuntimeException("Unexpected bundle name: "
                    + foobar.getResourceBundleName());
        }

        // Check that we can replace the bundle with a bundle of the same name.
        final ResourceBundle bundle_fr =
                ResourceBundle.getBundle(LIST_BUNDLE_NAME, Locale.FRENCH);
        foobar.setResourceBundle(bundle_fr);

        if (bundle_fr != foobar.getResourceBundle()) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobar.getResourceBundle());
        }
        if (!Objects.equals(getBaseName(bundle_fr), foobar.getResourceBundleName())) {
            throw new RuntimeException("Unexpected bundle name: "
                    + foobar.getResourceBundleName());
        }

        // Create a child logger
        final Logger foobaz = Logger.getLogger(loggerName + ".baz");

        if (foobar != foobaz.getParent()) {
            throw new RuntimeException("Unexpected parent: " +
                    foobaz.getParent() + " != " + foobar);
        }

        // Check that the child logger does not have a bundle set locally
        if (foobaz.getResourceBundle() != null) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobaz.getResourceBundle());
        }
        if (foobaz.getResourceBundleName() != null) {
            throw new RuntimeException("Unexpected bundle: "
                    + foobaz.getResourceBundleName());
        }


        // Add a handler on the child logger.
        final TestHandler handler = new TestHandler();
        foobaz.addHandler(handler);

        // log a message on the child logger
        foobaz.severe("dummy");

        // checks that the message has been logged with the bundle
        // inherited from the parent logger
        if (!LIST_BUNDLE_NAME.equals(handler.lastBundleName)) {
            debugLogger(foobaz, foobar, handler);
            throw new RuntimeException("Unexpected bundle name: "
                    + handler.lastBundleName);
        }
        if (!bundle_fr.equals(handler.lastBundle)) {
            debugLogger(foobaz, foobar, handler);
            throw new RuntimeException("Unexpected bundle: "
                    + handler.lastBundle);
        }

        // Check that we can get set a bundle on the child logger
        // using Logger.getLogger.
        final Logger foobaz2 = Logger.getLogger(loggerName + ".baz", PROPERTY_BUNDLE_NAME);
        if (foobaz2 != foobaz) {
            throw new RuntimeException("Unexpected logger: " + foobaz2 + " != " + foobaz);
        }
        if (foobar != foobaz.getParent()) {
            throw new RuntimeException("Unexpected parent: " +
                    foobaz.getParent() + " != " + foobar);
        }

        // check that the child logger has the correct bundle.
        // it should no longer inherit it from its parent.
        if (!PROPERTY_BUNDLE_NAME.equals(foobaz2.getResourceBundleName())) {
            throw new RuntimeException("Unexpected bundle name: "
                    + foobaz2.getResourceBundleName());
        }

        if (!PROPERTY_BUNDLE_NAME.equals(foobaz2.getResourceBundle().getBaseBundleName())) {
            throw new RuntimeException("Unexpected bundle name: "
                    + foobaz2.getResourceBundle().getBaseBundleName());
        }

        boolean found = false;
        for (Handler h : foobaz2.getHandlers()) {
            if (h == handler) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new RuntimeException("Expected handler not found in: " +
                    foobaz2.getName() + "(" + foobaz2.getClass().getName()+")" );
        }

        // log a message on the child logger
        foobaz2.severe("dummy");


        // check that the last published log record has the appropriate
        // bundle.
        if (!PROPERTY_BUNDLE_NAME.equals(handler.lastBundleName)) {
            debugLogger(foobaz2, foobar, handler);
            throw new RuntimeException("Unexpected bundle name: "
                    + handler.lastBundleName);
        }
        if (foobaz2.getResourceBundle() != handler.lastBundle) {
            debugLogger(foobaz2, foobar, handler);
            throw new RuntimeException("Unexpected bundle: "
                    + handler.lastBundle);
        }

        // try to set a bundle that has a different name, and checks that
        // it fails in IAE.
        try {
            foobaz2.setResourceBundle(bundle_fr);
            throw new RuntimeException("Expected exception not raised!");
        } catch (IllegalArgumentException x) {
            System.out.println("Got expected exception: " + x);
        }

        // Test with a subclass of logger which overrides
        // getResourceBundle() and getResourceBundleName()
        Logger customLogger = new Logger(foobar.getName()+".bie", null) {
            @Override
            public ResourceBundle getResourceBundle() {
                return bundle_fr;
            }

            @Override
            public String getResourceBundleName() {
                return PROPERTY_BUNDLE_NAME;
            }
        };

        final TestHandler handler2 = new TestHandler();
        customLogger.addHandler(handler2);
        customLogger.setLevel(Level.FINE);
        LogManager.getLogManager().addLogger(customLogger);

        Logger l = Logger.getLogger(customLogger.getName());
        if (l != customLogger) {
            throw new RuntimeException("Wrong logger: " + l);
        }

        // log on the custom logger.
        customLogger.fine("dummy");

        // check that the log record had the correct bundle.
        if (! PROPERTY_BUNDLE_NAME.equals(handler2.lastBundleName)) {
            debugLogger(customLogger, foobar, handler2);
            throw new RuntimeException("Unexpected bundle name: "
                    + handler2.lastBundleName);
        }
        if (! PROPERTY_BUNDLE_NAME.equals(customLogger.getResourceBundleName())) {
            debugLogger(customLogger, foobar, handler2);
            throw new RuntimeException("Unexpected bundle name: "
                    + customLogger.getResourceBundleName());
        }
        if (bundle_fr != handler2.lastBundle) {
            throw new RuntimeException("Unexpected bundle: "
                    + handler2.lastBundle);
        }
        if (bundle_fr != customLogger.getResourceBundle()) {
            throw new RuntimeException("Unexpected bundle: "
                    + customLogger.getResourceBundle());
        }

        // Do the same thing again with a child of the custom logger.
        Logger biebar = Logger.getLogger(customLogger.getName() + ".bar");
        biebar.fine("dummy");

        // because getResourceBundleName() is called on parent logger
        //         we will have handler2.lastBundleName = PROPERTY_BUNDLE_NAME
        if (!PROPERTY_BUNDLE_NAME.equals(handler2.lastBundleName)) {
            debugLogger(biebar, customLogger, handler2);
            throw new RuntimeException("Unexpected bundle name: "
                    + handler2.lastBundleName);
        }
        // because getResourceBundle() is not called on parent logger
        //         we will have getBaseName(handler2.lastBundle) = PROPERTY_BUNDLE_NAME
        //         and not handler2.lastBundle = bundle_fr
        if (handler2.lastBundle == null) {
            debugLogger(biebar, customLogger, handler2);
            throw new RuntimeException("Unexpected bundle: "
                    + handler2.lastBundle);
        }
        if (!PROPERTY_BUNDLE_NAME.equals(getBaseName(handler2.lastBundle))) {
            debugLogger(biebar, customLogger, handler2);
            throw new RuntimeException("Unexpected bundle name: "
                    + getBaseName(handler2.lastBundle));
        }

        // Just make sure that these loggers won't be eagerly GCed...
        if (foobar == null || !loggerName.equals(foobar.getName())) {
            throw new RuntimeException("foobar is null "
                    + "- or doesn't have the expected  name: " + foobar);
        }
        if (foobaz == null || !foobaz.getName().startsWith(loggerName)) {
            throw new RuntimeException("foobaz is null "
                    + "- or doesn't have the expected  name: " + foobaz);
        }
        if (foobaz2 == null || !foobaz2.getName().startsWith(loggerName)) {
            throw new RuntimeException("foobaz2 is null "
                    + "- or doesn't have the expected  name: " + foobaz2);
        }
        if (!customLogger.getName().startsWith(loggerName)) {
            throw new RuntimeException("customLogger "
                    + "doesn't have the expected name: " + customLogger);
        }
        if (!biebar.getName().startsWith(loggerName)) {
            throw new RuntimeException("biebar "
                    + "doesn't have the expected  name: " + biebar.getName());
        }
        System.out.println("Test passed for " + loggerName);
    }

    static void debugLogger(Logger logger, Logger expectedParent, TestHandler handler) {
        final String logName = logger.getName();
        final String prefix = "    " + logName;
        System.err.println("Logger " + logName
                + " logged with bundle name " + handler.lastBundleName
                + " (" + handler.lastBundle + ")");
        System.err.println(prefix + ".getResourceBundleName() is "
                + logger.getResourceBundleName());
        System.err.println(prefix + ".getResourceBundle() is "
                + logger.getResourceBundle());
        final Logger parent = logger.getParent();
        final String pname = parent == null ? null : parent.getName();
        final String pclass = parent == null ? ""
                : ("(" + parent.getClass().getName() + ")");
        final String presn = parent == null ? null
                : parent.getResourceBundleName();
        final ResourceBundle pres = parent == null ? null
                : parent.getResourceBundle();
        System.err.println(prefix + ".getParent() is "
                + pname + (pname == null ? ""
                        : (" " + pclass + ": " + parent)));
        System.err.println("    expected parent is :" + expectedParent);
        System.err.println(prefix + ".parent.getResourceBundleName() is "
                + presn);
        System.err.println(prefix + ".parent.getResourceBundle() is "
                + pres);
        System.err.println("    expected parent getResourceBundleName() is "
                + expectedParent.getResourceBundleName());
        System.err.println("    expected parent.getResourceBundle() is "
                + expectedParent.getResourceBundle());
    }

    public static class SimplePolicy extends Policy {

        final Permissions permissions;
        public SimplePolicy(TestCase test) {
            permissions = new Permissions();
            if (test != TestCase.PERMISSION) {
                permissions.add(new LoggingPermission("control", null));
            }
            // required for calling Locale.setDefault in the test.
            permissions.add(new PropertyPermission("user.language", "write"));
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return permissions.implies(permission) ||
                   DEFAULT_POLICY.implies(domain, permission);
        }
    }

}
