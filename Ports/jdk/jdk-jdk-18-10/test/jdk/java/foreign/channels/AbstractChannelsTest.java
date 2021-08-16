/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.test.lib.RandomFactory;
import org.testng.annotations.*;
import static org.testng.Assert.*;

/**
 * Not a test, but infra for channel tests.
 */
public class AbstractChannelsTest {

    static final Class<IOException> IOE = IOException.class;
    static final Class<ExecutionException> EE = ExecutionException.class;
    static final Class<IllegalStateException> ISE = IllegalStateException.class;

    @FunctionalInterface
    interface ThrowingConsumer<T, X extends Throwable> {
        void accept(T action) throws X;
    }

    static ResourceScope closeableScopeOrNull(ResourceScope scope) {
        if (scope.isImplicit())
            return null;
        return scope;
    }

    static long remaining(ByteBuffer[] buffers) {
        return Arrays.stream(buffers).mapToLong(ByteBuffer::remaining).sum();
    }

    static ByteBuffer[] flip(ByteBuffer[] buffers) {
        Arrays.stream(buffers).forEach(ByteBuffer::flip);
        return buffers;
    }

    static ByteBuffer[] clear(ByteBuffer[] buffers) {
        Arrays.stream(buffers).forEach(ByteBuffer::clear);
        return buffers;
    }

    static final Random RANDOM = RandomFactory.getRandom();

    static ByteBuffer segmentBufferOfSize(ResourceScope scope, int size) {
        var segment = MemorySegment.allocateNative(size, 1, scope);
        for (int i = 0; i < size; i++) {
            MemoryAccess.setByteAtOffset(segment, i, ((byte)RANDOM.nextInt()));
        }
        return segment.asByteBuffer();
    }

    static ByteBuffer[] segmentBuffersOfSize(int len, ResourceScope scope, int size) {
        ByteBuffer[] bufs = new ByteBuffer[len];
        for (int i = 0; i < len; i++)
            bufs[i] = segmentBufferOfSize(scope, size);
        return bufs;
    }

    /**
     * Returns an array of mixed source byte buffers; both heap and direct,
     * where heap can be from the global scope or scope-less, and direct are
     * associated with the given scope.
     */
    static ByteBuffer[] mixedBuffersOfSize(int len, ResourceScope scope, int size) {
        ByteBuffer[] bufs;
        boolean atLeastOneScopeBuffer = false;
        do {
            bufs = new ByteBuffer[len];
            for (int i = 0; i < len; i++) {
                bufs[i] = switch (RANDOM.nextInt(3)) {
                    case 0 -> { byte[] b = new byte[size];
                                RANDOM.nextBytes(b);
                                yield ByteBuffer.wrap(b); }
                    case 1 -> { byte[] b = new byte[size];
                                RANDOM.nextBytes(b);
                                yield MemorySegment.ofArray(b).asByteBuffer(); }
                    case 2 -> { atLeastOneScopeBuffer = true;
                                yield segmentBufferOfSize(scope, size); }
                    default -> throw new AssertionError("cannot happen");
                };
            }
        } while (!atLeastOneScopeBuffer);
        return bufs;
    }

    static void assertMessage(Exception ex, String msg) {
        assertTrue(ex.getMessage().contains(msg), "Expected [%s], in: [%s]".formatted(msg, ex.getMessage()));
    }

    static void assertCauses(Throwable ex, Class<? extends Exception>... exceptions) {
        for (var expectedClass : exceptions) {
            ex = ex.getCause();
            assertTrue(expectedClass.isInstance(ex), "Expected %s, got: %s".formatted(expectedClass, ex));
        }
    }

    @DataProvider(name = "confinedScopes")
    public static Object[][] confinedScopes() {
        return new Object[][] {
                { ScopeSupplier.NEW_CONFINED          },
                { ScopeSupplier.NEW_CONFINED_EXPLICIT },
        };
    }

    @DataProvider(name = "sharedScopes")
    public static Object[][] sharedScopes() {
        return new Object[][] {
                { ScopeSupplier.NEW_SHARED          },
                { ScopeSupplier.NEW_SHARED_EXPLICIT },
        };
    }

    @DataProvider(name = "closeableScopes")
    public static Object[][] closeableScopes() {
        return Stream.of(sharedScopes(), confinedScopes())
                .flatMap(Arrays::stream)
                .toArray(Object[][]::new);
    }

    @DataProvider(name = "implicitScopes")
    public static Object[][] implicitScopes() {
        return new Object[][] {
                { ScopeSupplier.NEW_IMPLICIT },
                { ScopeSupplier.GLOBAL       },
        };
    }

    @DataProvider(name = "sharedAndImplicitScopes")
    public static Object[][] sharedAndImplicitScopes() {
        return Stream.of(sharedScopes(), implicitScopes())
                .flatMap(Arrays::stream)
                .toArray(Object[][]::new);
    }

    @DataProvider(name = "allScopes")
    public static Object[][] allScopes() {
        return Stream.of(implicitScopes(), closeableScopes())
                .flatMap(Arrays::stream)
                .toArray(Object[][]::new);
    }

    @DataProvider(name = "sharedScopesAndTimeouts")
    public static Object[][] sharedScopesAndTimeouts() {
        return new Object[][] {
                { ScopeSupplier.NEW_SHARED          ,  0 },
                { ScopeSupplier.NEW_SHARED_EXPLICIT ,  0 },
                { ScopeSupplier.NEW_SHARED          , 30 },
                { ScopeSupplier.NEW_SHARED_EXPLICIT , 30 },
        };
    }

    static class ScopeSupplier implements Supplier<ResourceScope> {

        static final Supplier<ResourceScope> NEW_CONFINED =
                new ScopeSupplier(() -> ResourceScope.newConfinedScope(), "newConfinedScope()");
        static final Supplier<ResourceScope> NEW_CONFINED_EXPLICIT =
                new ScopeSupplier(() -> ResourceScope.newConfinedScope(Cleaner.create()), "newConfinedScope(Cleaner)");
        static final Supplier<ResourceScope> NEW_SHARED =
                new ScopeSupplier(() -> ResourceScope.newSharedScope(), "newSharedScope()");
        static final Supplier<ResourceScope> NEW_SHARED_EXPLICIT =
                new ScopeSupplier(() -> ResourceScope.newSharedScope(Cleaner.create()), "newSharedScope(Cleaner)");
        static final Supplier<ResourceScope> NEW_IMPLICIT =
                new ScopeSupplier(() -> ResourceScope.newImplicitScope(), "newImplicitScope()");
        static final Supplier<ResourceScope> GLOBAL =
                new ScopeSupplier(() -> ResourceScope.globalScope(), "globalScope()");

        private final Supplier<ResourceScope> supplier;
        private final String str;
        private ScopeSupplier(Supplier<ResourceScope> supplier, String str) {
            this.supplier = supplier;
            this.str = str;
        }
        @Override public String toString() { return str; }
        @Override public ResourceScope get() { return supplier.get(); }
    }
}

