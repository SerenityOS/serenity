/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7020047
 * @summary Test null handling of try-with-resources statement
 */

public class TwrNullTests {
    /*
     * Each try-with-resources statement generates two calls to the
     * close method for each resource: one for when there is a primary
     * exception present and the second for when a primary exception
     * is absent.  The null handling of both cases needs to be
     * checked.
     */
    public static void main(String... args) {
        testNormalCompletion();
        testNoSuppression();
    }

    /*
     * Verify empty try-with-resources on a null resource completes
     * normally; no NPE from the generated close call.
     */
    private static void testNormalCompletion() {
        try(AutoCloseable resource = null) {
            return; // Nothing to see here, move along.
        } catch (Exception e) {
            throw new AssertionError("Should not be reached", e);
        }
    }

    /*
     * Verify that a NPE on a null resource is <em>not</em> added as a
     * suppressed exception to an exception from try block.
     */
    private static void testNoSuppression() {
        try(AutoCloseable resource = null) {
            throw new java.io.IOException();
        } catch(java.io.IOException ioe) {
            Throwable[] suppressed = ioe.getSuppressed();
            if (suppressed.length != 0) {
                throw new AssertionError("Non-empty suppressed exceptions",
                                         ioe);
            }
        } catch (Exception e) {
            throw new AssertionError("Should not be reached", e);
        }
    }
}
