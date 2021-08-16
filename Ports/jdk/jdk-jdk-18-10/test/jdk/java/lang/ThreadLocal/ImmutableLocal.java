/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6387919
 * @summary Confirm ThreadLocal.set() usage is not a side effect of get()
 * @author  Pete Soper
 */
public class ImmutableLocal
{
    /**
     * {@link ThreadLocal} guaranteed to always return the same reference.
     */
    public abstract static class ImmutableThreadLocal extends ThreadLocal {
        public void set(final Object value) {
            throw new RuntimeException("ImmutableThreadLocal set called");
        }

        // force override
        protected abstract Object initialValue();
    }

    private static final ThreadLocal cache = new ImmutableThreadLocal() {
        public Object initialValue() {
            return Thread.currentThread().getName();
        }
    };

    public static void main(final String[] args) {
        System.out.println("cache.get() = " + cache.get());
    }
}
