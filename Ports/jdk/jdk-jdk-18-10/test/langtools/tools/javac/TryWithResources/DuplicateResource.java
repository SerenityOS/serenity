/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6911256 6964740 6965277 7013420
 * @author Maurizio Cimadamore
 * @summary Check that lowered try-with-resources block does not end up creating resource twice
 */

import java.util.ArrayList;

public class DuplicateResource {

    static class TestResource implements AutoCloseable {
        TestResource() {
            resources.add(this);
        }
        boolean isClosed = false;
        public void close() throws Exception {
            isClosed = true;
        }
    }

    static ArrayList<TestResource> resources = new ArrayList<TestResource>();

    public static void main(String[] args) {
        try(TestResource tr = new TestResource()) {
           //do something
        } catch (Exception e) {
            throw new AssertionError("Shouldn't reach here", e);
        }
        check();
    }

    public static void check() {
       if (resources.size() != 1) {
           throw new AssertionError("Expected one resource, found: " + resources.size());
       }
       TestResource resource = resources.get(0);
       if (!resource.isClosed) {
           throw new AssertionError("Resource used in try-with-resources block has not been automatically closed");
       }
    }
}
