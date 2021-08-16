/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package catalog;

import javax.xml.catalog.CatalogResolver;

import org.testng.Assert;

/*
 * Utilities for checking catalog resolution.
 */
class ResolutionChecker {

    /* ********** Checks normal resolution ********** */

    /*
     * Checks the resolution result for specified external identifier.
     */
    static void checkExtIdResolution(CatalogResolver resolver,
            String publicId, String systemId, String matchedUri) {
        Assert.assertEquals(
                resolver.resolveEntity(publicId, getNotSpecified(systemId)).getSystemId(),
                matchedUri);
    }

    /*
     * Checks the resolution result for specified system identifier.
     */
    static void checkSysIdResolution(CatalogResolver resolver,
            String systemId, String matchedUri) {
        checkExtIdResolution(resolver, null, systemId, matchedUri);
    }

    /*
     * Checks the resolution result for specified public identifier.
     */
    static void checkPubIdResolution(CatalogResolver resolver,
            String publicId, String matchedUri) {
        checkExtIdResolution(resolver, publicId, null, matchedUri);
    }

    /*
     * Checks the resolution result for specified URI references
     * with the specified base location.
     */
    static void checkUriResolution(CatalogResolver resolver,
            String href, String base, String matchedUri) {
        Assert.assertEquals(resolver.resolve(href, base).getSystemId(),
                matchedUri);
    }

    /*
     * Checks the resolution result for specified URI references.
     */
    static void checkUriResolution(CatalogResolver resolver,
            String href, String matchedUri) {
        checkUriResolution(resolver, href, null, matchedUri);
    }

    /* ********** Checks no match is found ********** */

    /*
     * With strict resolution, if no match is found,
     * CatalogResolver should throw CatalogException.
     */
    static void checkNoMatch(CatalogResolver resolver) {
        resolver.resolveEntity("-//EXTID//DTD NOMATCH DOCNOMATCH XML//EN",
                "http://extId/noMatch/docNoMatch.dtd");
    }

    /*
     * With strict resolution, if no match is found,
     * CatalogResolver should throw CatalogException.
     */
    static void checkNoUriMatch(CatalogResolver resolver) {
        resolver.resolve("http://uri/noMatch/docNoMatch.dtd", getNotSpecified(null));
    }

    /* ********** Checks expected exception ********** */

    /*
     * Checks the expected exception during the resolution for specified
     * external identifier.
     */
    static <T extends Throwable> void expectExceptionOnExtId(
            CatalogResolver resolver, String publicId, String systemId,
            Class<T> expectedExceptionClass) {
        expectThrows(expectedExceptionClass, () -> {
            resolver.resolveEntity(publicId, getNotSpecified(systemId));
        });
    }

    /*
     * Checks the expected exception during the resolution for specified
     * system identifier.
     */
    static <T extends Throwable> void expectExceptionOnSysId(
            CatalogResolver resolver, String systemId,
            Class<? extends Throwable> expectedExceptionClass) {
        expectExceptionOnExtId(resolver, null, systemId,
                expectedExceptionClass);
    }

    /*
     * Checks the expected exception during the resolution for specified
     * public identifier.
     */
    static <T extends Throwable> void expectExceptionOnPubId(
            CatalogResolver resolver, String publicId,
            Class<T> expectedExceptionClass) {
        expectExceptionOnExtId(resolver, publicId, null,
                expectedExceptionClass);
    }

    /*
     * Checks the expected exception during the resolution for specified
     * URI reference with a specified base location.
     */
    static <T extends Throwable> void expectExceptionOnUri(
            CatalogResolver resolver, String href, String base,
            Class<T> expectedExceptionClass) {
        expectThrows(expectedExceptionClass, () -> {
            resolver.resolve(href, base);
        });
    }

    /*
     * Checks the expected exception during the resolution for specified
     * URI reference without any specified base location.
     */
    static <T extends Throwable> void expectExceptionOnUri(
            CatalogResolver resolver, String href,
            Class<T> expectedExceptionClass) {
        expectExceptionOnUri(resolver, href, null, expectedExceptionClass);
    }

    // The TestNG distribution in current JTREG build doesn't support
    // method Assert.expectThrows().
    private static <T extends Throwable> T expectThrows(Class<T> throwableClass,
            ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            if (throwableClass.isInstance(t)) {
                return throwableClass.cast(t);
            } else {
                String mismatchMessage = String.format(
                        "Expected %s to be thrown, but %s was thrown",
                        throwableClass.getSimpleName(),
                        t.getClass().getSimpleName());

                throw new AssertionError(mismatchMessage, t);
            }
        }

        String message = String.format(
                "Expected %s to be thrown, but nothing was thrown",
                throwableClass.getSimpleName());
        throw new AssertionError(message);
    }

    /*
     * SystemId can never be null in XML. For publicId tests, if systemId is null,
     * it will be considered as not-specified instead. A non-existent systemId
     * is returned to make sure there's no match by the systemId.
    */
    private static String getNotSpecified(String systemId) {
        if (systemId == null) {
            return "not-specified-systemId.dtd";
        }
        return systemId;
    }

    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
