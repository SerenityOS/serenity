/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198882
 * @summary Tests that get attributes of a missing child entry results
 *          in NameNotFoundException.
 * @modules java.base/sun.security.util
 * @library ../lib/
 * @run main GetAttrsNotFound
 */

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;

public class GetAttrsNotFound extends GetAttrsBase {

    public GetAttrsNotFound() {
        // set new test data instead of default value
        setKey("notfound");
    }

    public static void main(String[] args) throws Exception {
        new GetAttrsNotFound().run(args);
    }

    @Override public void runTest() throws Exception {
        initContext();
        Attributes retAttrs = getAttributes();
        DNSTestUtils.debug(retAttrs);
        throw new RuntimeException(
                "Failed: getAttributes succeeded unexpectedly");
    }

    /*
     * Tests that get attributes of a missing child entry results
     * in NameNotFoundException.
     */
    @Override public Attributes getAttributes() throws Exception {
        return context().getAttributes(getKey());
    }

    @Override public boolean handleException(Exception e) {
        if (e instanceof NameNotFoundException) {
            System.out.println("Got exception as expected : " + e);
            return true;
        }

        return super.handleException(e);
    }
}
