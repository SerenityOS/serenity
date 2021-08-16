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
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import resources.ListBundle;

/**
 * @test
 * @bug 8013839
 * @summary tests Logger.logrb(..., ResourceBundle);
 * @build TestLogrbResourceBundle resources.ListBundle resources.ListBundle_fr
 * @run main TestLogrbResourceBundle
 * @author danielfuchs
 */
public class TestLogrbResourceBundle {

    static final String LIST_BUNDLE_NAME = "resources.ListBundle";
    static final String PROPERTY_BUNDLE_NAME = "resources.PropertyBundle";

    /**
     * A dummy handler class that we can use to check the bundle/bundle name
     * that was present in the last LogRecord instance published.
     */
    static final class TestHandler extends Handler {
        ResourceBundle lastBundle = null;
        String lastBundleName = null;
        Object[] lastParams = null;
        Throwable lastThrown = null;
        String lastMessage = null;
        @Override
        public void publish(LogRecord record) {
            lastBundle = record.getResourceBundle();
            lastBundleName = record.getResourceBundleName();
            lastParams = record.getParameters();
            lastThrown = record.getThrown();
            lastMessage = record.getMessage();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * We're going to do the same test with each of the different new logrb
     * forms.
     * <ul>
     *    <li> LOGRB_NO_ARGS: calling logrb with no message argument.
     *    <li> LOGRB_SINGLE_ARG: calling logrb with a single message argument.
     *    <li> LOGRB_ARG_ARRAY: calling logrb with an array of message arguments.
     *    <li> LOGRB_VARARGS: calling logrb with a variable list of message arguments.
     *    <li> LOGRB_THROWABLE: calling logrb with an exception.
     * </ul>
     */
    private static enum TestCase {
        LOGRB_NO_ARGS, LOGRB_SINGLE_ARG, LOGRB_ARG_ARRAY, LOGRB_VARARGS, LOGRB_THROWABLE;

        public void logrb(Logger logger, ResourceBundle bundle) {
            switch(this) {
                case LOGRB_NO_ARGS:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy");
                    break;
                case LOGRB_SINGLE_ARG:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy", "bar");
                    break;
                case LOGRB_ARG_ARRAY:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            new Object[] { "bar", "baz"} );
                    break;
                case LOGRB_VARARGS:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            "bar", "baz" );
                    break;
                case LOGRB_THROWABLE:
                    logger.logrb(Level.CONFIG,
                            TestLogrbResourceBundle.class.getName(),
                            "main", bundle, "dummy",
                            new Exception("dummy exception") );
                    break;
                default:
            }
        }

        /**
         * Checks that the last published logged record had the expected data.
         * @param handler the TestHandler through which the record was published.
         */
        public void checkLogged(TestHandler handler) {
            checkLogged(handler.lastMessage, handler.lastParams, handler.lastThrown);
        }

        private void checkLogged(String message, Object[] parameters, Throwable thrown) {
            switch(this) {
                case LOGRB_NO_ARGS:
                    if ("dummy".equals(message) && thrown == null
                            && (parameters == null || parameters.length == 0)) {
                        return; // OK: all was as expected.
                    }
                    break;
                case LOGRB_SINGLE_ARG:
                    if ("dummy".equals(message) && thrown == null
                            && parameters != null
                            && parameters.length == 1
                            && "bar".equals(parameters[0])) {
                        return; // OK: all was as expected.
                    }
                    break;
                case LOGRB_VARARGS:
                case LOGRB_ARG_ARRAY:
                    if ("dummy".equals(message) && thrown == null
                            && parameters != null
                            && parameters.length > 1
                            && Arrays.deepEquals(new Object[] { "bar", "baz"},
                                    parameters)) {
                        return; // OK: all was as expected.
                    }
                    break;
                case LOGRB_THROWABLE:
                    if ("dummy".equals(message) && thrown != null
                            && thrown.getClass() == Exception.class
                            && "dummy exception".equals(thrown.getMessage())) {
                        return; // OK: all was as expected.
                    }
                    break;
                default:
            }

            // We had some unexpected stuff: throw exception.
            throw new RuntimeException(this + ": "
                    + "Unexpected content in last published log record: "
                    + "\n\tmessage=\"" + message + "\""
                    + "\n\tparameters=" + Arrays.toString(parameters)
                    + "\n\tthrown=" + thrown);
        }
    }

    static String getBaseName(ResourceBundle bundle) {
        return bundle == null ? null : bundle.getBaseBundleName();
    }

    public static void main(String... args) throws Exception {

        Locale defaultLocale = Locale.getDefault();

        final ResourceBundle bundle = ResourceBundle.getBundle(LIST_BUNDLE_NAME);
        final ResourceBundle bundle_fr =
                    ResourceBundle.getBundle(LIST_BUNDLE_NAME, Locale.FRENCH);
        final ResourceBundle propertyBundle = ResourceBundle.getBundle(PROPERTY_BUNDLE_NAME);
        final ResourceBundle propertyBundle_fr =
                    ResourceBundle.getBundle(PROPERTY_BUNDLE_NAME, Locale.FRENCH);
        Logger foobar = Logger.getLogger("foo.bar");
        final TestHandler handler = new TestHandler();
        foobar.addHandler(handler);
        foobar.setLevel(Level.CONFIG);

        final ResourceBundle anonBundle = new ListBundle();
        try {
            // First we're going to call logrb on a logger that
            // has no bundle set...

            // For each possible logrb form...
            for (TestCase test : TestCase.values()) {
                // For various resource bundles
                for (ResourceBundle b : new ResourceBundle[] {
                    anonBundle, bundle, bundle_fr, propertyBundle,
                    anonBundle, null, propertyBundle_fr,
                }) {
                    // Prints the resource bundle base name (can be null,
                    //   we don't enforce non-null names in logrb.
                    final String baseName = getBaseName(b);
                    System.out.println("Testing " + test + " with " + baseName);

                    // log in the 'foobar' logger using bundle 'b'
                    test.logrb(foobar, b);

                    // check that the correct bundle was set in the published
                    // LogRecord
                    if (handler.lastBundle != b) {
                        throw new RuntimeException("Unexpected bundle: "
                                + handler.lastBundle);
                    }

                    // check that the correct bundle name was set in the published
                    // LogRecord
                    if (!Objects.equals(handler.lastBundleName, baseName)) {
                        throw new RuntimeException("Unexpected bundle name: "
                                + handler.lastBundleName);
                    }

                    // check that calling foobar.logrb() had no side effect on
                    // the bundle used by foobar. foobar should still have no
                    // bundle set.
                    if (foobar.getResourceBundle() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundle());
                    }
                    if (foobar.getResourceBundleName() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundleName());
                    }

                    // Test that the last published log record had all the
                    // data that this test case had logged (message, parameters,
                    // thrown...
                    test.checkLogged(handler);
                }
            }

            // No we're going to set a resource bundle on the foobar logger
            // and do it all again...

            // For the same bundle in two different locales
            for (ResourceBundle propBundle : new ResourceBundle[] {
                propertyBundle, propertyBundle_fr,
            }) {

                // set the bundle on foobar...
                foobar.setResourceBundle(propBundle);

                // check the bundle was correctly set...
                if (!propBundle.equals(foobar.getResourceBundle())) {
                    throw new RuntimeException("Unexpected bundle: "
                            + foobar.getResourceBundle());
                }
                if (!Objects.equals(getBaseName(propBundle), foobar.getResourceBundleName())) {
                    throw new RuntimeException("Unexpected bundle name: "
                            + foobar.getResourceBundleName());
                }

                System.out.println("Configuring " + foobar.getName() + " with "
                        + propBundle);

                // for each possible logrb form...
                for (TestCase test : TestCase.values()) {

                    // for various resource bundles
                    for (ResourceBundle b : new ResourceBundle[] {
                        anonBundle, bundle, null, bundle_fr, propertyBundle,
                        anonBundle, propertyBundle_fr,
                    }) {

                        final String baseName = getBaseName(b);
                        System.out.println("Testing " + test + " with " + baseName);

                        // call foobar.logrb
                        test.logrb(foobar, b);

                        // check which resource bundle was used (should be
                        // the one passed to logrb)
                        if (handler.lastBundle != b) {
                            throw new RuntimeException("Unexpected bundle: "
                                    + handler.lastBundle);
                        }
                        if (!Objects.equals(handler.lastBundleName, baseName)) {
                            throw new RuntimeException("Unexpected bundle name: "
                                    + handler.lastBundleName);
                        }

                        // Verify there was no side effect on the bundle that
                        // had been previously set on the logger...
                        if (foobar.getResourceBundle() != propBundle) {
                            throw new RuntimeException("Unexpected bundle: "
                                + foobar.getResourceBundle());
                        }
                        if (!Objects.equals(getBaseName(propBundle),
                                foobar.getResourceBundleName())) {
                            throw new RuntimeException("Unexpected bundle name: "
                                + foobar.getResourceBundleName());
                        }

                        // Checked that the published LogRecord had the
                        // expected content logged by this test case.
                        test.checkLogged(handler);
                    }
                }
            }

            // Now we're going to the same thing, but with a logger which
            // has an inherited resource bundle.
            Logger foobaz = Logger.getLogger("foo.bar.baz");

            // check that foobaz has no bundle set locally.
            if (foobaz.getResourceBundle() != null) {
                throw new RuntimeException("Unexpected bundle: "
                        + foobaz.getResourceBundle());
            }
            if (foobaz.getResourceBundleName() != null) {
                throw new RuntimeException("Unexpected bundle: "
                        + foobaz.getResourceBundle());
            }

            // The current locale should have no effect on logrb.
            Locale.setDefault(Locale.GERMAN); // shouldn't change anything...

            // for each possible logrb form
            for (TestCase test : TestCase.values()) {

                // for various resource bundle
                for (ResourceBundle b : new ResourceBundle[] {
                    anonBundle, bundle, bundle_fr, propertyBundle, null,
                     anonBundle, propertyBundle_fr,
                }) {
                    final String baseName = getBaseName(b);
                    System.out.println("Testing " + test + " with "
                            + foobaz.getName() + " and "
                            + baseName);

                    // call foobaz.logrb with the bundle
                    test.logrb(foobaz, b);

                    // check that the bundle passed to logrb was used.
                    if (handler.lastBundle != b) {
                        throw new RuntimeException("Unexpected bundle: "
                                + handler.lastBundle);
                    }
                    if (!Objects.equals(handler.lastBundleName, baseName)) {
                        throw new RuntimeException("Unexpected bundle name: "
                                + handler.lastBundleName);
                    }

                    // check that there was no effect on the bundle set
                    // on foobaz: it should still be null.
                    if (foobaz.getResourceBundle() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobaz.getResourceBundle());
                    }
                    if (foobaz.getResourceBundleName() != null) {
                        throw new RuntimeException("Unexpected bundle: "
                            + foobaz.getResourceBundleName());
                    }

                    // check that the last published log record had all the
                    // data that was logged by this testcase.
                    test.checkLogged(handler);
                }
            }

        } finally {
            Locale.setDefault(defaultLocale);
        }

    }
}
