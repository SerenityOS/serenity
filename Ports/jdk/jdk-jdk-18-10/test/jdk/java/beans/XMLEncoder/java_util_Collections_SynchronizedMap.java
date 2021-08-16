/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6505888
 * @summary Tests SynchronizedMap encoding
 * @run main/othervm -Djava.security.manager=allow java_util_Collections_SynchronizedMap
 * @author Sergey Malenkov
 */

import java.util.Collections;
import java.util.Map;

public final class java_util_Collections_SynchronizedMap extends AbstractTest<Map<String, String>> {
    public static void main(String[] args) {
        new java_util_Collections_SynchronizedMap().test(true);
    }

    protected Map<String, String> getObject() {
        Map<String, String> map = Collections.singletonMap("key", "value");
        return Collections.synchronizedMap(map);
    }

    protected Map<String, String> getAnotherObject() {
        Map<String, String> map = Collections.emptyMap();
        return Collections.synchronizedMap(map);
    }
}
