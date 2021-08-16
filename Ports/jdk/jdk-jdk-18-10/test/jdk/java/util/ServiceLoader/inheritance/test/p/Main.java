/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Basic test of ServiceLoader with a provider interface and 3 provider
 * implementations.
 *
 * The provider interface (test.Main.Provider) defines a static factory method
 * name "provider" that locates a provider implementation. At least one of the
 * provider implementations does not define a static "provider" method.
 */

public class Main {
    public static void main(String[] args) {
        List<S> providers = ServiceLoader.load(S.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());
        if (providers.size() != 3)
            throw new RuntimeException("Expected 3 providers");
    }

    /**
     * Service type
     */
    public static interface S {
    }

    /**
     * Base implementation, its static provider method should never be called
     */
    public static class BaseProvider implements S {
        protected BaseProvider() { }
        public static S provider() {
            throw new RuntimeException("Should not get here");
        }
    }

    /**
     * Provider implementation with public constructor.
     */
    public static class P1 extends BaseProvider {
        public P1() { }
    }

    /**
     * Provider implementation with static factory method.
     */
    public static class P2 extends BaseProvider {
        private P2() { }
        public static P2 provider() {
            return new P2();
        }
    }

    /**
     * Provider implementation with static factory method and public
     * constructor.
     */
    public static class P3 extends BaseProvider {
        public P3() {
            throw new RuntimeException("Should not get here");
        }
        private P3(int x) { }
        public static S provider() {
            return new P3(0);
        }
    }
}
