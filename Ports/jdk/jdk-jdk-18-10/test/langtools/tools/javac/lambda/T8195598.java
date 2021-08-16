/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8195598
 * @summary Reference to overloaded method is ambiguous with 3 methods but works with 2
 * @compile T8195598.java
 */

import java.util.concurrent.*;
import java.util.function.*;

class T8195598 {
    public CompletableFuture<?> test() {
        return ok(() -> System.out.append("aaa"));
    }
    public <T> CompletableFuture<T> ok(Supplier<T> action) {
        return CompletableFuture.supplyAsync(action);
    }
    public <T> CompletableFuture<T> ok(T body) {
        return CompletableFuture.completedFuture(body);
    }
    public CompletableFuture<Void> ok(Runnable action) {
        return CompletableFuture.runAsync(action);
    }
}
