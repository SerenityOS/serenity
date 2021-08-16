/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SpeculationLog;

/**
 * Implements a {@link SpeculationLog} that can be used to:
 * <ul>
 * <li>Query failed speculations recorded in a native linked list of {@code FailedSpeculation}s (see
 * methodData.hpp).</li>
 * <li>Make speculations during compilation and record them in compiled code. This must only be done
 * on compilation-local {@link HotSpotSpeculationLog} objects.</li>
 * </ul>
 *
 * The choice of constructor determines whether the native failed speculations list is
 * {@linkplain #managesFailedSpeculations() managed} by a {@link HotSpotSpeculationLog} object.
 */
public class HotSpotSpeculationLog implements SpeculationLog {

    private static final byte[] NO_FLATTENED_SPECULATIONS = {};

    /**
     * Creates a speculation log that manages a failed speculation list. That is, when this object
     * dies, the native resources of the list are freed.
     *
     * @see #managesFailedSpeculations()
     * @see #getFailedSpeculationsAddress()
     */
    public HotSpotSpeculationLog() {
        managesFailedSpeculations = true;
    }

    /**
     * Creates a speculation log that reads from an externally managed failed speculation list. That
     * is, the lifetime of the list is independent of this object.
     *
     * @param failedSpeculationsAddress an address in native memory at which the pointer to the
     *            externally managed sailed speculation list resides
     */
    public HotSpotSpeculationLog(long failedSpeculationsAddress) {
        if (failedSpeculationsAddress == 0) {
            throw new IllegalArgumentException("failedSpeculationsAddress cannot be 0");
        }
        this.failedSpeculationsAddress = failedSpeculationsAddress;
        managesFailedSpeculations = false;
    }

    /**
     * Gets the address of the pointer to the native failed speculations list.
     *
     * @see #managesFailedSpeculations()
     */
    public long getFailedSpeculationsAddress() {
        if (managesFailedSpeculations) {
            synchronized (this) {
                if (failedSpeculationsAddress == 0L) {
                    failedSpeculationsAddress = UnsafeAccess.UNSAFE.allocateMemory(HotSpotJVMCIRuntime.getHostWordKind().getByteCount());
                    UnsafeAccess.UNSAFE.putAddress(failedSpeculationsAddress, 0L);
                    LogCleaner c = new LogCleaner(this, failedSpeculationsAddress);
                    assert c.address == failedSpeculationsAddress;
                }
            }
        }
        return failedSpeculationsAddress;
    }

    /**
     * Adds {@code speculation} to the native list of failed speculations. To update this object's
     * view of the failed speculations, {@link #collectFailedSpeculations()} must be called after
     * this method returns.
     *
     * This method exists primarily for testing purposes. Speculations are normally only added to
     * the list by HotSpot during deoptimization.
     *
     * @return {@code false} if the speculation could not be appended to the list
     */
    public boolean addFailedSpeculation(Speculation speculation) {
        return compilerToVM().addFailedSpeculation(getFailedSpeculationsAddress(), ((HotSpotSpeculation) speculation).encoding);
    }

    /**
     * Returns {@code true} if the value returned by {@link #getFailedSpeculationsAddress()} is only
     * valid only as long as this object is alive, {@code false} otherwise.
     */
    public boolean managesFailedSpeculations() {
        return managesFailedSpeculations;
    }

    public static final class HotSpotSpeculation extends Speculation {

        /**
         * A speculation id is a long encoding a length (low 5 bits) and an index into a
         * {@code byte[]}. Combined, the index and length denote where the {@linkplain #encoding
         * encoded speculation} is in a {@linkplain HotSpotSpeculationLog#getFlattenedSpeculations
         * flattened} speculations array.
         */
        private final JavaConstant id;

        private final byte[] encoding;

        HotSpotSpeculation(SpeculationReason reason, JavaConstant id, byte[] encoding) {
            super(reason);
            this.id = id;
            this.encoding = encoding;
        }

        public JavaConstant getEncoding() {
            return id;
        }

        @Override
        public String toString() {
            long indexAndLength = id.asLong();
            int index = decodeIndex(indexAndLength);
            int length = decodeLength(indexAndLength);
            return String.format("{0x%016x[index: %d, len: %d, hash: 0x%x]: %s}", indexAndLength, index, length, Arrays.hashCode(encoding), getReason());
        }
    }

    /**
     * Address of a pointer to a set of failed speculations. The address is recorded in the nmethod
     * compiled with this speculation log such that when it fails a speculation, the speculation is
     * added to the list.
     */
    private long failedSpeculationsAddress;

    private final boolean managesFailedSpeculations;

    /**
     * The list of failed speculations read from native memory via
     * {@link CompilerToVM#getFailedSpeculations}.
     */
    private byte[][] failedSpeculations;

    /**
     * Speculations made during the compilation associated with this log.
     */
    private List<byte[]> speculations;
    private List<SpeculationReason> speculationReasons;

    @Override
    public void collectFailedSpeculations() {
        if (failedSpeculationsAddress != 0 && UnsafeAccess.UNSAFE.getLong(failedSpeculationsAddress) != 0) {
            failedSpeculations = compilerToVM().getFailedSpeculations(failedSpeculationsAddress, failedSpeculations);
            assert failedSpeculations.getClass() == byte[][].class;
        }
    }

    byte[] getFlattenedSpeculations(boolean validate) {
        if (speculations == null) {
            return NO_FLATTENED_SPECULATIONS;
        }
        if (validate) {
            int newFailuresStart = failedSpeculations == null ? 0 : failedSpeculations.length;
            collectFailedSpeculations();
            if (failedSpeculations != null && failedSpeculations.length != newFailuresStart) {
                for (SpeculationReason reason : speculationReasons) {
                    byte[] encoding = encode(reason);
                    // Only check against new failures
                    if (contains(failedSpeculations, newFailuresStart, encoding)) {
                        throw new BailoutException(false, "Speculation failed: " + reason);
                    }
                }
            }
        }
        int size = 0;
        for (byte[] s : speculations) {
            size += s.length;
        }
        byte[] result = new byte[size];
        size = 0;
        for (byte[] s : speculations) {
            System.arraycopy(s, 0, result, size, s.length);
            size += s.length;
        }
        return result;
    }

    @Override
    public boolean maySpeculate(SpeculationReason reason) {
        if (failedSpeculations == null) {
            collectFailedSpeculations();
        }
        if (failedSpeculations != null && failedSpeculations.length != 0) {
            byte[] encoding = encode(reason);
            return !contains(failedSpeculations, 0, encoding);
        }
        return true;
    }

    /**
     * @return {@code true} if {@code needle} is in {@code haystack[fromIndex..haystack.length-1]}
     */
    private static boolean contains(byte[][] haystack, int fromIndex, byte[] needle) {
        for (int i = fromIndex; i < haystack.length; i++) {
            byte[] fs = haystack[i];

            if (Arrays.equals(fs, needle)) {
                return true;
            }
        }
        return false;
    }

    private static long encodeIndexAndLength(int index, int length) {
        if (length > HotSpotSpeculationEncoding.MAX_LENGTH || length < 0) {
            throw new InternalError(String.format("Invalid encoded speculation length: %d (0x%x)", length, length));
        }
        if (index < 0) {
            throw new JVMCIError("Encoded speculation index is negative: %d (0x%x)", index, index);
        }
        return (index << HotSpotSpeculationEncoding.LENGTH_BITS) | length;
    }

    private static int decodeIndex(long indexAndLength) {
        return (int) (indexAndLength >>> HotSpotSpeculationEncoding.LENGTH_BITS);
    }

    private static int decodeLength(long indexAndLength) {
        return (int) (indexAndLength & HotSpotSpeculationEncoding.LENGTH_MASK);
    }

    @Override
    public Speculation speculate(SpeculationReason reason) {
        byte[] encoding = encode(reason);
        JavaConstant id;
        if (speculations == null) {
            speculations = new ArrayList<>();
            speculationReasons = new ArrayList<>();
            id = JavaConstant.forLong(encodeIndexAndLength(0, encoding.length));
            speculations.add(encoding);
            speculationReasons.add(reason);
        } else {
            id = null;
            int flattenedIndex = 0;
            for (byte[] fs : speculations) {
                if (Arrays.equals(fs, encoding)) {
                    id = JavaConstant.forLong(encodeIndexAndLength(flattenedIndex, fs.length));
                    break;
                }
                flattenedIndex += fs.length;
            }
            if (id == null) {
                id = JavaConstant.forLong(encodeIndexAndLength(flattenedIndex, encoding.length));
                speculations.add(encoding);
                speculationReasons.add(reason);
            }
        }

        return new HotSpotSpeculation(reason, id, encoding);
    }

    private static byte[] encode(SpeculationReason reason) {
        HotSpotSpeculationEncoding encoding = (HotSpotSpeculationEncoding) reason.encode(HotSpotSpeculationEncoding::new);
        byte[] result = encoding == null ? null : encoding.getByteArray();
        if (result == null) {
            throw new IllegalArgumentException(HotSpotSpeculationLog.class.getName() + " expects " + reason.getClass().getName() + ".encode() to return a non-empty encoding");
        }
        return result;
    }

    @Override
    public boolean hasSpeculations() {
        return speculations != null;
    }

    @Override
    public Speculation lookupSpeculation(JavaConstant constant) {
        if (constant.isDefaultForKind()) {
            return NO_SPECULATION;
        }
        int flattenedIndex = decodeIndex(constant.asLong());
        int index = 0;
        for (byte[] s : speculations) {
            if (flattenedIndex == 0) {
                SpeculationReason reason = speculationReasons.get(index);
                return new HotSpotSpeculation(reason, constant, s);
            }
            index++;
            flattenedIndex -= s.length;
        }
        throw new IllegalArgumentException("Unknown encoded speculation: " + constant);
    }

    @Override
    public String toString() {
        Formatter buf = new Formatter();
        buf.format("{managed:%s, failedSpeculationsAddress:0x%x, failedSpeculations:[", managesFailedSpeculations, failedSpeculationsAddress);

        String sep = "";
        if (failedSpeculations != null) {
            for (int i = 0; i < failedSpeculations.length; i++) {
                buf.format("%s{len:%d, hash:0x%x}", sep, failedSpeculations[i].length, Arrays.hashCode(failedSpeculations[i]));
                sep = ", ";
            }
        }

        buf.format("], speculations:[");

        int size = 0;
        if (speculations != null) {
            sep = "";
            for (int i = 0; i < speculations.size(); i++) {
                byte[] s = speculations.get(i);
                size += s.length;
                buf.format("%s{len:%d, hash:0x%x, reason:{%s}}", sep, s.length, Arrays.hashCode(s), speculationReasons.get(i));
                sep = ", ";
            }
        }
        buf.format("], len:%d, hash:0x%x}", size, Arrays.hashCode(getFlattenedSpeculations(false)));
        return buf.toString();
    }

    /**
     * Frees the native memory resources associated with {@link HotSpotSpeculationLog}s once they
     * become reclaimable.
     */
    private static final class LogCleaner extends Cleaner {

        LogCleaner(HotSpotSpeculationLog referent, long address) {
            super(referent);
            this.address = address;
        }

        @Override
        void doCleanup() {
            long pointer = UnsafeAccess.UNSAFE.getAddress(address);
            if (pointer != 0) {
                compilerToVM().releaseFailedSpeculations(address);
            }
            UnsafeAccess.UNSAFE.freeMemory(address);
        }

        final long address;
    }
}
