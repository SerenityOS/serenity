/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/*
 * @test
 * @bug 8029891
 * @summary Test Properties methods that do not synchronize any more
 * @run main CheckUnsynchronized
 */
public class CheckUnsynchronized {
    public static void main(String[] args) {
        Properties props = new Properties();
        synchronized (props) {
            props.setProperty("key", "value");
            System.out.println("contains(value)? " +
                    CompletableFuture.supplyAsync(() -> props.contains("value")).join());
            System.out.println("containsKey(key)? " +
                    CompletableFuture.supplyAsync(() -> props.containsKey("key")).join());
            System.out.println("containsValue(value)? " +
                    CompletableFuture.supplyAsync(() -> props.containsValue("value")).join());
            Enumeration<Object> elems =
                    CompletableFuture.supplyAsync(() -> props.elements()).join();
            System.out.println("first value from elements(): " + elems.nextElement());
            System.out.println("value from get(): " +
                    CompletableFuture.supplyAsync(() -> props.getProperty("key")).join());
            System.out.println("getOrDefault(\"missing\"): " +
                    CompletableFuture.supplyAsync(() -> props.getOrDefault("missing", "default")).join());
            System.out.println("isEmpty()? " +
                    CompletableFuture.supplyAsync(() -> props.isEmpty()).join());
            Enumeration<Object> keys =
                    CompletableFuture.supplyAsync(() -> props.keys()).join();
            System.out.println("first key from keys(): " + keys.nextElement());
            System.out.println("size(): " +
                    CompletableFuture.supplyAsync(() -> props.size()).join());
        }
    }
}
