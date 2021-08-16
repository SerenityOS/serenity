/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.String.format;
import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;
import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.HotSpotVMConfig.config;
import static jdk.vm.ci.hotspot.UnsafeAccess.UNSAFE;

import java.util.Arrays;

import jdk.vm.ci.common.NativeImageReinitialize;
import jdk.internal.misc.Unsafe;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaMethodProfile;
import jdk.vm.ci.meta.JavaMethodProfile.ProfiledMethod;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.JavaTypeProfile.ProfiledType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

/**
 * Access to a HotSpot {@code MethodData} structure (defined in methodData.hpp).
 */
final class HotSpotMethodData {

    /**
     * VM state that can be reset when building an AOT image.
     */
    static final class VMState {
        final HotSpotVMConfig config = config();
        final HotSpotMethodDataAccessor noDataNoExceptionAccessor = new NoMethodData(this, config.dataLayoutNoTag, TriState.FALSE);
        final HotSpotMethodDataAccessor noDataExceptionPossiblyNotRecordedAccessor = new NoMethodData(this, config.dataLayoutNoTag, TriState.UNKNOWN);
        final int noDataSize = cellIndexToOffset(0);
        final int bitDataSize = cellIndexToOffset(0);
        final int bitDataNullSeenFlag = 1 << config.bitDataNullSeenFlag;
        final int counterDataSize = cellIndexToOffset(1);
        final int counterDataCountOffset = cellIndexToOffset(config.methodDataCountOffset);
        final int jumpDataSize = cellIndexToOffset(2);
        final int takenCountOffset = cellIndexToOffset(config.jumpDataTakenOffset);
        final int takenDisplacementOffset = cellIndexToOffset(config.jumpDataDisplacementOffset);
        final int typeDataRowSize = cellsToBytes(config.receiverTypeDataReceiverTypeRowCellCount);

        final int nonprofiledCountOffset = cellIndexToOffset(config.receiverTypeDataNonprofiledCountOffset);
        final int typeDataFirstTypeOffset = cellIndexToOffset(config.receiverTypeDataReceiver0Offset);
        final int typeDataFirstTypeCountOffset = cellIndexToOffset(config.receiverTypeDataCount0Offset);

        final int typeCheckDataSize = cellIndexToOffset(2) + typeDataRowSize * config.typeProfileWidth;
        final int virtualCallDataSize = cellIndexToOffset(2) + typeDataRowSize * (config.typeProfileWidth + config.methodProfileWidth);
        final int virtualCallDataFirstMethodOffset = typeDataFirstTypeOffset + typeDataRowSize * config.typeProfileWidth;
        final int virtualCallDataFirstMethodCountOffset = typeDataFirstTypeCountOffset + typeDataRowSize * config.typeProfileWidth;

        final int retDataRowSize = cellsToBytes(3);
        final int retDataSize = cellIndexToOffset(1) + retDataRowSize * config.bciProfileWidth;

        final int branchDataSize = cellIndexToOffset(3);
        final int notTakenCountOffset = cellIndexToOffset(config.branchDataNotTakenOffset);

        final int arrayDataLengthOffset = cellIndexToOffset(config.arrayDataArrayLenOffset);
        final int arrayDataStartOffset = cellIndexToOffset(config.arrayDataArrayStartOffset);

        final int multiBranchDataSize = cellIndexToOffset(1);
        final int multiBranchDataRowSizeInCells = config.multiBranchDataPerCaseCellCount;
        final int multiBranchDataRowSize = cellsToBytes(multiBranchDataRowSizeInCells);
        final int multiBranchDataFirstCountOffset = arrayDataStartOffset + cellsToBytes(0);
        final int multiBranchDataFirstDisplacementOffset = arrayDataStartOffset + cellsToBytes(1);

        final int argInfoDataSize = cellIndexToOffset(1);

        // sorted by tag
        // @formatter:off
        final HotSpotMethodDataAccessor[] profileDataAccessors = {
            null,
            new BitData(this, config.dataLayoutBitDataTag),
            new CounterData(this, config.dataLayoutCounterDataTag),
            new JumpData(this, config.dataLayoutJumpDataTag),
            new ReceiverTypeData(this, config.dataLayoutReceiverTypeDataTag),
            new VirtualCallData(this, config.dataLayoutVirtualCallDataTag),
            new RetData(this, config.dataLayoutRetDataTag),
            new BranchData(this, config.dataLayoutBranchDataTag),
            new MultiBranchData(this, config.dataLayoutMultiBranchDataTag),
            new ArgInfoData(this, config.dataLayoutArgInfoDataTag),
            new UnknownProfileData(this, config.dataLayoutCallTypeDataTag),
            new VirtualCallTypeData(this, config.dataLayoutVirtualCallTypeDataTag),
            new UnknownProfileData(this, config.dataLayoutParametersTypeDataTag),
            new UnknownProfileData(this, config.dataLayoutSpeculativeTrapDataTag),
        };
        // @formatter:on

        private boolean checkAccessorTags() {
            int expectedTag = 0;
            for (HotSpotMethodDataAccessor accessor : profileDataAccessors) {
                if (expectedTag == 0) {
                    assert accessor == null;
                } else {
                    assert accessor.tag == expectedTag : expectedTag + " != " + accessor.tag + " " + accessor;
                }
                expectedTag++;
            }
            return true;
        }

        private VMState() {
            assert checkAccessorTags();
        }

        private static int truncateLongToInt(long value) {
            return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
        }

        private int computeFullOffset(int position, int offsetInBytes) {
            return config.methodDataOopDataOffset + position + offsetInBytes;
        }

        private int cellIndexToOffset(int cells) {
            return config.dataLayoutHeaderSize + cellsToBytes(cells);
        }

        private int cellsToBytes(int cells) {
            return cells * config.dataLayoutCellSize;
        }

        /**
         * Singleton instance lazily initialized via double-checked locking.
         */
        @NativeImageReinitialize private static volatile VMState instance;

        static VMState instance() {
            VMState result = instance;
            if (result == null) {
                synchronized (VMState.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new VMState();
                    }
                }
            }
            return result;
        }
    }

    /**
     * Reference to the C++ MethodData object.
     */
    final long metaspaceMethodData;
    private final HotSpotResolvedJavaMethodImpl method;
    private final VMState state;

    HotSpotMethodData(long metaspaceMethodData, HotSpotResolvedJavaMethodImpl method) {
        this.metaspaceMethodData = metaspaceMethodData;
        this.method = method;
        this.state = VMState.instance();
    }

    /**
     * @return value of the MethodData::_data_size field
     */
    private int normalDataSize() {
        return UNSAFE.getInt(metaspaceMethodData + state.config.methodDataDataSize);
    }

    /**
     * Returns the size of the extra data records. This method does the same calculation as
     * MethodData::extra_data_size().
     *
     * @return size of extra data records
     */
    private int extraDataSize() {
        final int extraDataBase = state.config.methodDataOopDataOffset + normalDataSize();
        final int extraDataLimit = UNSAFE.getInt(metaspaceMethodData + state.config.methodDataSize);
        return extraDataLimit - extraDataBase;
    }

    public boolean hasNormalData() {
        return normalDataSize() > 0;
    }

    public boolean hasExtraData() {
        return extraDataSize() > 0;
    }

    public int getExtraDataBeginOffset() {
        return normalDataSize();
    }

    public boolean isWithin(int position) {
        return position >= 0 && position < normalDataSize() + extraDataSize();
    }

    public int getDeoptimizationCount(DeoptimizationReason reason) {
        HotSpotMetaAccessProvider metaAccess = (HotSpotMetaAccessProvider) runtime().getHostJVMCIBackend().getMetaAccess();
        int reasonIndex = metaAccess.convertDeoptReason(reason);
        return UNSAFE.getByte(metaspaceMethodData + state.config.methodDataOopTrapHistoryOffset + reasonIndex) & 0xFF;
    }

    public int getOSRDeoptimizationCount(DeoptimizationReason reason) {
        HotSpotMetaAccessProvider metaAccess = (HotSpotMetaAccessProvider) runtime().getHostJVMCIBackend().getMetaAccess();
        int reasonIndex = metaAccess.convertDeoptReason(reason);
        return UNSAFE.getByte(metaspaceMethodData + state.config.methodDataOopTrapHistoryOffset + state.config.deoptReasonOSROffset + reasonIndex) & 0xFF;
    }

    public int getDecompileCount() {
        return UNSAFE.getInt(metaspaceMethodData + state.config.methodDataDecompiles);
    }

    public int getOverflowRecompileCount() {
        return UNSAFE.getInt(metaspaceMethodData + state.config.methodDataOverflowRecompiles);
    }

    public int getOverflowTrapCount() {
        return UNSAFE.getInt(metaspaceMethodData + state.config.methodDataOverflowTraps);
    }

    public HotSpotMethodDataAccessor getNormalData(int position) {
        if (position >= normalDataSize()) {
            return null;
        }

        return getData(position);
    }

    public HotSpotMethodDataAccessor getExtraData(int position) {
        if (position >= normalDataSize() + extraDataSize()) {
            return null;
        }
        HotSpotMethodDataAccessor data = getData(position);
        if (data != null) {
            return data;
        }
        return data;
    }

    public static HotSpotMethodDataAccessor getNoDataAccessor(boolean exceptionPossiblyNotRecorded) {
        if (exceptionPossiblyNotRecorded) {
            return VMState.instance().noDataExceptionPossiblyNotRecordedAccessor;
        } else {
            return VMState.instance().noDataNoExceptionAccessor;
        }
    }

    private HotSpotMethodDataAccessor getData(int position) {
        assert position >= 0 : "out of bounds";
        final int tag = HotSpotMethodDataAccessor.readTag(state.config, this, position);
        HotSpotMethodDataAccessor accessor = state.profileDataAccessors[tag];
        assert accessor == null || accessor.getTag() == tag : "wrong data accessor " + accessor + " for tag " + tag;
        return accessor;
    }

    int readUnsignedByte(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return UNSAFE.getByte(metaspaceMethodData + fullOffsetInBytes) & 0xFF;
    }

    int readUnsignedShort(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return UNSAFE.getShort(metaspaceMethodData + fullOffsetInBytes) & 0xFFFF;
    }

    /**
     * Since the values are stored in cells (platform words) this method uses
     * {@link Unsafe#getAddress} to read the right value on both little and big endian machines.
     */
    private long readUnsignedInt(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return UNSAFE.getAddress(metaspaceMethodData + fullOffsetInBytes) & 0xFFFFFFFFL;
    }

    private int readUnsignedIntAsSignedInt(int position, int offsetInBytes) {
        long value = readUnsignedInt(position, offsetInBytes);
        return VMState.truncateLongToInt(value);
    }

    /**
     * Since the values are stored in cells (platform words) this method uses
     * {@link Unsafe#getAddress} to read the right value on both little and big endian machines.
     */
    private int readInt(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return (int) UNSAFE.getAddress(metaspaceMethodData + fullOffsetInBytes);
    }

    private HotSpotResolvedJavaMethod readMethod(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return compilerToVM().getResolvedJavaMethod(null, metaspaceMethodData + fullOffsetInBytes);
    }

    private HotSpotResolvedObjectTypeImpl readKlass(int position, int offsetInBytes) {
        long fullOffsetInBytes = state.computeFullOffset(position, offsetInBytes);
        return compilerToVM().getResolvedJavaType(metaspaceMethodData + fullOffsetInBytes, false);
    }

    /**
     * Returns whether profiling ran long enough that the profile information is mature. Other
     * informational data will still be valid even if the profile isn't mature.
     */
    public boolean isProfileMature() {
        return runtime().getCompilerToVM().isMature(metaspaceMethodData);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String nl = String.format("%n");
        String nlIndent = String.format("%n%38s", "");
        sb.append("Raw method data for ");
        sb.append(method.format("%H.%n(%p)"));
        sb.append(":");
        sb.append(nl);
        sb.append(String.format("nof_decompiles(%d) nof_overflow_recompiles(%d) nof_overflow_traps(%d)%n",
                        getDecompileCount(), getOverflowRecompileCount(), getOverflowTrapCount()));
        if (hasNormalData()) {
            int pos = 0;
            HotSpotMethodDataAccessor data;
            while ((data = getNormalData(pos)) != null) {
                if (pos != 0) {
                    sb.append(nl);
                }
                int bci = data.getBCI(this, pos);
                sb.append(String.format("%-6d bci: %-6d%-20s", pos, bci, data.getClass().getSimpleName()));
                sb.append(data.appendTo(new StringBuilder(), this, pos).toString().replace(nl, nlIndent));
                pos = pos + data.getSize(this, pos);
            }
        }

        if (hasExtraData()) {
            int pos = getExtraDataBeginOffset();
            HotSpotMethodDataAccessor data;
            while ((data = getExtraData(pos)) != null) {
                if (pos == getExtraDataBeginOffset()) {
                    sb.append(nl).append("--- Extra data:");
                }
                int bci = data.getBCI(this, pos);
                sb.append(String.format("%n%-6d bci: %-6d%-20s", pos, bci, data.getClass().getSimpleName()));
                sb.append(data.appendTo(new StringBuilder(), this, pos).toString().replace(nl, nlIndent));
                pos = pos + data.getSize(this, pos);
            }

        }
        return sb.toString();
    }

    static class NoMethodData extends HotSpotMethodDataAccessor {

        private final TriState exceptionSeen;

        protected NoMethodData(VMState state, int tag, TriState exceptionSeen) {
            super(state, tag, state.noDataSize);
            this.exceptionSeen = exceptionSeen;
        }

        @Override
        public int getBCI(HotSpotMethodData data, int position) {
            return -1;
        }

        @Override
        public TriState getExceptionSeen(HotSpotMethodData data, int position) {
            return exceptionSeen;
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            return sb;
        }
    }

    static class BitData extends HotSpotMethodDataAccessor {

        private BitData(VMState state, int tag) {
            super(state, tag, state.bitDataSize);
        }

        protected BitData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public TriState getNullSeen(HotSpotMethodData data, int position) {
            return TriState.get((getFlags(data, position) & state.bitDataNullSeenFlag) != 0);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            return sb.append(format("exception_seen(%s)", getExceptionSeen(data, pos)));
        }
    }

    static class CounterData extends BitData {

        CounterData(VMState state, int tag) {
            super(state, tag, state.counterDataSize);
        }

        protected CounterData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            return getCounterValue(data, position);
        }

        protected int getCounterValue(HotSpotMethodData data, int position) {
            return data.readUnsignedIntAsSignedInt(position, state.counterDataCountOffset);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            return sb.append(format("count(%d) null_seen(%s) exception_seen(%s)", getCounterValue(data, pos), getNullSeen(data, pos), getExceptionSeen(data, pos)));
        }
    }

    static class JumpData extends HotSpotMethodDataAccessor {

        JumpData(VMState state, int tag) {
            super(state, tag, state.jumpDataSize);
        }

        protected JumpData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public double getBranchTakenProbability(HotSpotMethodData data, int position) {
            return getExecutionCount(data, position) != 0 ? 1 : 0;
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            return data.readUnsignedIntAsSignedInt(position, state.takenCountOffset);
        }

        public int getTakenDisplacement(HotSpotMethodData data, int position) {
            return data.readInt(position, state.takenDisplacementOffset);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            return sb.append(format("taken(%d) displacement(%d)", getExecutionCount(data, pos), getTakenDisplacement(data, pos)));
        }
    }

    static class RawItemProfile<T> {
        final int entries;
        final T[] items;
        final long[] counts;
        final long totalCount;

        RawItemProfile(int entries, T[] items, long[] counts, long totalCount) {
            this.entries = entries;
            this.items = items;
            this.counts = counts;
            this.totalCount = totalCount;
        }
    }

    abstract static class AbstractTypeData extends CounterData {

        protected AbstractTypeData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public JavaTypeProfile getTypeProfile(HotSpotMethodData data, int position) {
            return createTypeProfile(getNullSeen(data, position), getRawTypeProfile(data, position));
        }

        private RawItemProfile<ResolvedJavaType> getRawTypeProfile(HotSpotMethodData data, int position) {
            int typeProfileWidth = config.typeProfileWidth;

            ResolvedJavaType[] types = new ResolvedJavaType[typeProfileWidth];
            long[] counts = new long[typeProfileWidth];
            long totalCount = 0;
            int entries = 0;

            outer: for (int i = 0; i < typeProfileWidth; i++) {
                HotSpotResolvedObjectTypeImpl receiverKlass = data.readKlass(position, getTypeOffset(i));
                if (receiverKlass != null) {
                    HotSpotResolvedObjectTypeImpl klass = receiverKlass;
                    long count = data.readUnsignedInt(position, getTypeCountOffset(i));
                    /*
                     * Because of races in the profile collection machinery it's possible for a
                     * class to appear multiple times so merge them to make the profile look
                     * rational.
                     */
                    for (int j = 0; j < entries; j++) {
                        if (types[j].equals(klass)) {
                            totalCount += count;
                            counts[j] += count;
                            continue outer;
                        }
                    }
                    types[entries] = klass;
                    totalCount += count;
                    counts[entries] = count;
                    entries++;
                }
            }

            totalCount += getTypesNotRecordedExecutionCount(data, position);
            return new RawItemProfile<>(entries, types, counts, totalCount);
        }

        protected abstract long getTypesNotRecordedExecutionCount(HotSpotMethodData data, int position);

        public int getNonprofiledCount(HotSpotMethodData data, int position) {
            return data.readUnsignedIntAsSignedInt(position, state.nonprofiledCountOffset);
        }

        private JavaTypeProfile createTypeProfile(TriState nullSeen, RawItemProfile<ResolvedJavaType> profile) {
            if (profile.entries <= 0 || profile.totalCount <= 0) {
                return null;
            }

            ProfiledType[] ptypes = new ProfiledType[profile.entries];
            double totalProbability = 0.0;
            for (int i = 0; i < profile.entries; i++) {
                double p = profile.counts[i];
                p = p / profile.totalCount;
                totalProbability += p;
                ptypes[i] = new ProfiledType(profile.items[i], p);
            }

            Arrays.sort(ptypes);

            double notRecordedTypeProbability = profile.entries < config.typeProfileWidth ? 0.0 : Math.min(1.0, Math.max(0.0, 1.0 - totalProbability));
            assert notRecordedTypeProbability == 0 || profile.entries == config.typeProfileWidth;
            return new JavaTypeProfile(nullSeen, notRecordedTypeProbability, ptypes);
        }

        private int getTypeOffset(int row) {
            return state.typeDataFirstTypeOffset + row * state.typeDataRowSize;
        }

        protected int getTypeCountOffset(int row) {
            return state.typeDataFirstTypeCountOffset + row * state.typeDataRowSize;
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            RawItemProfile<ResolvedJavaType> profile = getRawTypeProfile(data, pos);
            TriState nullSeen = getNullSeen(data, pos);
            TriState exceptionSeen = getExceptionSeen(data, pos);
            sb.append(format("count(%d) null_seen(%s) exception_seen(%s) nonprofiled_count(%d) entries(%d)", getCounterValue(data, pos), nullSeen, exceptionSeen,
                            getNonprofiledCount(data, pos), profile.entries));
            for (int i = 0; i < profile.entries; i++) {
                long count = profile.counts[i];
                sb.append(format("%n  %s (%d, %4.2f)", profile.items[i].toJavaName(), count, (double) count / profile.totalCount));
            }
            return sb;
        }
    }

    static class ReceiverTypeData extends AbstractTypeData {

        ReceiverTypeData(VMState state, int tag) {
            super(state, tag, state.typeCheckDataSize);
        }

        protected ReceiverTypeData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            return -1;
        }

        @Override
        protected long getTypesNotRecordedExecutionCount(HotSpotMethodData data, int position) {
            return getNonprofiledCount(data, position);
        }
    }

    static class VirtualCallData extends ReceiverTypeData {

        VirtualCallData(VMState state, int tag) {
            super(state, tag, state.virtualCallDataSize);
        }

        protected VirtualCallData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            final int typeProfileWidth = config.typeProfileWidth;

            long total = 0;
            for (int i = 0; i < typeProfileWidth; i++) {
                total += data.readUnsignedInt(position, getTypeCountOffset(i));
            }

            total += getCounterValue(data, position);
            return VMState.truncateLongToInt(total);
        }

        @Override
        protected long getTypesNotRecordedExecutionCount(HotSpotMethodData data, int position) {
            return getCounterValue(data, position);
        }

        private long getMethodsNotRecordedExecutionCount(HotSpotMethodData data, int position) {
            return data.readUnsignedIntAsSignedInt(position, state.nonprofiledCountOffset);
        }

        @Override
        public JavaMethodProfile getMethodProfile(HotSpotMethodData data, int position) {
            return createMethodProfile(getRawMethodProfile(data, position));
        }

        private RawItemProfile<ResolvedJavaMethod> getRawMethodProfile(HotSpotMethodData data, int position) {
            int profileWidth = config.methodProfileWidth;

            ResolvedJavaMethod[] methods = new ResolvedJavaMethod[profileWidth];
            long[] counts = new long[profileWidth];
            long totalCount = 0;
            int entries = 0;

            for (int i = 0; i < profileWidth; i++) {
                HotSpotResolvedJavaMethod method = data.readMethod(position, getMethodOffset(i));
                if (method != null) {
                    methods[entries] = method;
                    long count = data.readUnsignedInt(position, getMethodCountOffset(i));
                    totalCount += count;
                    counts[entries] = count;

                    entries++;
                }
            }

            totalCount += getMethodsNotRecordedExecutionCount(data, position);

            // Fixup the case of C1's inability to optimize profiling of a statically bindable call
            // site. If it's a monomorphic call site, attribute all the counts to the first type (if
            // any is recorded).
            if (entries == 1) {
                counts[0] = totalCount;
            }

            return new RawItemProfile<>(entries, methods, counts, totalCount);
        }

        private JavaMethodProfile createMethodProfile(RawItemProfile<ResolvedJavaMethod> profile) {
            if (profile.entries <= 0 || profile.totalCount <= 0) {
                return null;
            }

            ProfiledMethod[] pmethods = new ProfiledMethod[profile.entries];
            double totalProbability = 0.0;
            for (int i = 0; i < profile.entries; i++) {
                double p = profile.counts[i];
                p = p / profile.totalCount;
                totalProbability += p;
                pmethods[i] = new ProfiledMethod(profile.items[i], p);
            }

            Arrays.sort(pmethods);

            double notRecordedMethodProbability = profile.entries < config.methodProfileWidth ? 0.0 : Math.min(1.0, Math.max(0.0, 1.0 - totalProbability));
            assert notRecordedMethodProbability == 0 || profile.entries == config.methodProfileWidth;
            return new JavaMethodProfile(notRecordedMethodProbability, pmethods);
        }

        private int getMethodOffset(int row) {
            return state.virtualCallDataFirstMethodOffset + row * state.typeDataRowSize;
        }

        private int getMethodCountOffset(int row) {
            return state.virtualCallDataFirstMethodCountOffset + row * state.typeDataRowSize;
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            RawItemProfile<ResolvedJavaMethod> profile = getRawMethodProfile(data, pos);
            super.appendTo(sb.append(format("exception_seen(%s) ", getExceptionSeen(data, pos))), data, pos).append(format("%nmethod_entries(%d)", profile.entries));
            for (int i = 0; i < profile.entries; i++) {
                long count = profile.counts[i];
                sb.append(format("%n  %s (%d, %4.2f)", profile.items[i].format("%H.%n(%p)"), count, (double) count / profile.totalCount));
            }
            return sb;
        }
    }

    static class VirtualCallTypeData extends VirtualCallData {

        VirtualCallTypeData(VMState state, int tag) {
            super(state, tag, 0);
        }

        @Override
        protected int getDynamicSize(HotSpotMethodData data, int position) {
            assert staticSize == 0;
            return HotSpotJVMCIRuntime.runtime().compilerToVm.methodDataProfileDataSize(data.metaspaceMethodData, position);
        }
    }

    static class RetData extends CounterData {

        RetData(VMState state, int tag) {
            super(state, tag, state.retDataSize);
        }
    }

    static class BranchData extends JumpData {

        BranchData(VMState state, int tag) {
            super(state, tag, state.branchDataSize);
        }

        @Override
        public double getBranchTakenProbability(HotSpotMethodData data, int position) {
            long takenCount = data.readUnsignedInt(position, state.takenCountOffset);
            long notTakenCount = data.readUnsignedInt(position, state.notTakenCountOffset);
            long total = takenCount + notTakenCount;

            return total <= 0 ? -1 : takenCount / (double) total;
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            long count = data.readUnsignedInt(position, state.takenCountOffset) + data.readUnsignedInt(position, state.notTakenCountOffset);
            return VMState.truncateLongToInt(count);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            long taken = data.readUnsignedInt(pos, state.takenCountOffset);
            long notTaken = data.readUnsignedInt(pos, state.notTakenCountOffset);
            double takenProbability = getBranchTakenProbability(data, pos);
            return sb.append(format("taken(%d, %4.2f) not_taken(%d, %4.2f) displacement(%d)", taken, takenProbability, notTaken, 1.0D - takenProbability, getTakenDisplacement(data, pos)));
        }
    }

    static class ArrayData extends HotSpotMethodDataAccessor {

        ArrayData(VMState state, int tag, int staticSize) {
            super(state, tag, staticSize);
        }

        @Override
        protected int getDynamicSize(HotSpotMethodData data, int position) {
            return state.cellsToBytes(getLength(data, position));
        }

        protected int getLength(HotSpotMethodData data, int position) {
            return data.readInt(position, state.arrayDataLengthOffset);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            return sb.append(format("length(%d)", getLength(data, pos)));
        }
    }

    static class MultiBranchData extends ArrayData {

        MultiBranchData(VMState state, int tag) {
            super(state, tag, state.multiBranchDataSize);
        }

        @Override
        public double[] getSwitchProbabilities(HotSpotMethodData data, int position) {
            int arrayLength = getLength(data, position);
            assert arrayLength > 0 : "switch must have at least the default case";
            assert arrayLength % state.multiBranchDataRowSizeInCells == 0 : "array must have full rows";

            int length = arrayLength / state.multiBranchDataRowSizeInCells;
            long totalCount = 0;
            double[] result = new double[length];

            // default case is first in HotSpot but last for the compiler
            long count = readCount(data, position, 0);
            totalCount += count;
            result[length - 1] = count;

            for (int i = 1; i < length; i++) {
                count = readCount(data, position, i);
                totalCount += count;
                result[i - 1] = count;
            }

            if (totalCount <= 0) {
                return null;
            } else {
                for (int i = 0; i < length; i++) {
                    result[i] = result[i] / totalCount;
                }
                return result;
            }
        }

        private long readCount(HotSpotMethodData data, int position, int i) {
            int offset;
            long count;
            offset = getCountOffset(i);
            count = data.readUnsignedInt(position, offset);
            return count;
        }

        @Override
        public int getExecutionCount(HotSpotMethodData data, int position) {
            int arrayLength = getLength(data, position);
            assert arrayLength > 0 : "switch must have at least the default case";
            assert arrayLength % state.multiBranchDataRowSizeInCells == 0 : "array must have full rows";

            int length = arrayLength / state.multiBranchDataRowSizeInCells;
            long totalCount = 0;
            for (int i = 0; i < length; i++) {
                int offset = getCountOffset(i);
                totalCount += data.readUnsignedInt(position, offset);
            }

            return VMState.truncateLongToInt(totalCount);
        }

        private int getCountOffset(int index) {
            return state.multiBranchDataFirstCountOffset + index * state.multiBranchDataRowSize;
        }

        private int getDisplacementOffset(int index) {
            return state.multiBranchDataFirstDisplacementOffset + index * state.multiBranchDataRowSize;
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            int entries = getLength(data, pos) / state.multiBranchDataRowSizeInCells;
            sb.append(format("entries(%d)", entries));
            for (int i = 0; i < entries; i++) {
                sb.append(format("%n  %d: count(%d) displacement(%d)", i, data.readUnsignedInt(pos, getCountOffset(i)), data.readUnsignedInt(pos, getDisplacementOffset(i))));
            }
            return sb;
        }
    }

    static class ArgInfoData extends ArrayData {

        ArgInfoData(VMState state, int tag) {
            super(state, tag, state.argInfoDataSize);
        }
    }

    static class UnknownProfileData extends HotSpotMethodDataAccessor {
        UnknownProfileData(VMState state, int tag) {
            super(state, tag, 0);
        }

        @Override
        protected int getDynamicSize(HotSpotMethodData data, int position) {
            assert staticSize == 0;
            return HotSpotJVMCIRuntime.runtime().compilerToVm.methodDataProfileDataSize(data.metaspaceMethodData, position);
        }

        @Override
        public StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos) {
            sb.append("unknown profile data with tag: " + tag);
            return sb;
        }
    }

    public void setCompiledIRSize(int size) {
        UNSAFE.putInt(metaspaceMethodData + state.config.methodDataIRSizeOffset, size);
    }

    public int getCompiledIRSize() {
        return UNSAFE.getInt(metaspaceMethodData + state.config.methodDataIRSizeOffset);
    }
}
