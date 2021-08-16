/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package java.util.stream;

import java.util.EnumMap;
import java.util.Map;
import java.util.Spliterator;

/**
 * Flags corresponding to characteristics of streams and operations. Flags are
 * utilized by the stream framework to control, specialize or optimize
 * computation.
 *
 * <p>
 * Stream flags may be used to describe characteristics of several different
 * entities associated with streams: stream sources, intermediate operations,
 * and terminal operations.  Not all stream flags are meaningful for all
 * entities; the following table summarizes which flags are meaningful in what
 * contexts:
 *
 * <div>
 * <table class="borderless">
 *   <caption>Type Characteristics</caption>
 *   <thead class="tableSubHeadingColor">
 *     <tr>
 *       <th colspan="2">&nbsp;</th>
 *       <th>{@code DISTINCT}</th>
 *       <th>{@code SORTED}</th>
 *       <th>{@code ORDERED}</th>
 *       <th>{@code SIZED}</th>
 *       <th>{@code SHORT_CIRCUIT}</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *      <tr>
 *        <th colspan="2" class="tableSubHeadingColor">Stream source</th>
 *        <td>Y</td>
 *        <td>Y</td>
 *        <td>Y</td>
 *        <td>Y</td>
 *        <td>N</td>
 *      </tr>
 *      <tr>
 *        <th colspan="2" class="tableSubHeadingColor">Intermediate operation</th>
 *        <td>PCI</td>
 *        <td>PCI</td>
 *        <td>PCI</td>
 *        <td>PC</td>
 *        <td>PI</td>
 *      </tr>
 *      <tr>
 *        <th colspan="2" class="tableSubHeadingColor">Terminal operation</th>
 *        <td>N</td>
 *        <td>N</td>
 *        <td>PC</td>
 *        <td>N</td>
 *        <td>PI</td>
 *      </tr>
 *   </tbody>
 *   <tfoot>
 *       <tr>
 *         <th class="tableSubHeadingColor" colspan="2">Legend</th>
 *         <th colspan="6" rowspan="7">&nbsp;</th>
 *       </tr>
 *       <tr>
 *         <th class="tableSubHeadingColor">Flag</th>
 *         <th class="tableSubHeadingColor">Meaning</th>
 *         <th colspan="6"></th>
 *       </tr>
 *       <tr><td>Y</td><td>Allowed</td></tr>
 *       <tr><td>N</td><td>Invalid</td></tr>
 *       <tr><td>P</td><td>Preserves</td></tr>
 *       <tr><td>C</td><td>Clears</td></tr>
 *       <tr><td>I</td><td>Injects</td></tr>
 *   </tfoot>
 * </table>
 * </div>
 *
 * <p>In the above table, "PCI" means "may preserve, clear, or inject"; "PC"
 * means "may preserve or clear", "PI" means "may preserve or inject", and "N"
 * means "not valid".
 *
 * <p>Stream flags are represented by unioned bit sets, so that a single word
 * may describe all the characteristics of a given stream entity, and that, for
 * example, the flags for a stream source can be efficiently combined with the
 * flags for later operations on that stream.
 *
 * <p>The bit masks {@link #STREAM_MASK}, {@link #OP_MASK}, and
 * {@link #TERMINAL_OP_MASK} can be ANDed with a bit set of stream flags to
 * produce a mask containing only the valid flags for that entity type.
 *
 * <p>When describing a stream source, one only need describe what
 * characteristics that stream has; when describing a stream operation, one need
 * describe whether the operation preserves, injects, or clears that
 * characteristic.  Accordingly, two bits are used for each flag, so as to allow
 * representing not only the presence of a characteristic, but how an
 * operation modifies that characteristic.  There are two common forms in which
 * flag bits are combined into an {@code int} bit set.  <em>Stream flags</em>
 * are a unioned bit set constructed by ORing the enum characteristic values of
 * {@link #set()} (or, more commonly, ORing the corresponding static named
 * constants prefixed with {@code IS_}).  <em>Operation flags</em> are a unioned
 * bit set constructed by ORing the enum characteristic values of {@link #set()}
 * or {@link #clear()} (to inject, or clear, respectively, the corresponding
 * flag), or more commonly ORing the corresponding named constants prefixed with
 * {@code IS_} or {@code NOT_}.  Flags that are not marked with {@code IS_} or
 * {@code NOT_} are implicitly treated as preserved.  Care must be taken when
 * combining bitsets that the correct combining operations are applied in the
 * correct order.
 *
 * <p>
 * With the exception of {@link #SHORT_CIRCUIT}, stream characteristics can be
 * derived from the equivalent {@link java.util.Spliterator} characteristics:
 * {@link java.util.Spliterator#DISTINCT}, {@link java.util.Spliterator#SORTED},
 * {@link java.util.Spliterator#ORDERED}, and
 * {@link java.util.Spliterator#SIZED}.  A spliterator characteristics bit set
 * can be converted to stream flags using the method
 * {@link #fromCharacteristics(java.util.Spliterator)} and converted back using
 * {@link #toCharacteristics(int)}.  (The bit set
 * {@link #SPLITERATOR_CHARACTERISTICS_MASK} is used to AND with a bit set to
 * produce a valid spliterator characteristics bit set that can be converted to
 * stream flags.)
 *
 * <p>
 * The source of a stream encapsulates a spliterator. The characteristics of
 * that source spliterator when transformed to stream flags will be a proper
 * subset of stream flags of that stream.
 * For example:
 * <pre> {@code
 *     Spliterator s = ...;
 *     Stream stream = Streams.stream(s);
 *     flagsFromSplitr = fromCharacteristics(s.characteristics());
 *     assert(flagsFromSplitr & stream.getStreamFlags() == flagsFromSplitr);
 * }</pre>
 *
 * <p>
 * An intermediate operation, performed on an input stream to create a new
 * output stream, may preserve, clear or inject stream or operation
 * characteristics.  Similarly, a terminal operation, performed on an input
 * stream to produce an output result may preserve, clear or inject stream or
 * operation characteristics.  Preservation means that if that characteristic
 * is present on the input, then it is also present on the output.  Clearing
 * means that the characteristic is not present on the output regardless of the
 * input.  Injection means that the characteristic is present on the output
 * regardless of the input.  If a characteristic is not cleared or injected then
 * it is implicitly preserved.
 *
 * <p>
 * A pipeline consists of a stream source encapsulating a spliterator, one or
 * more intermediate operations, and finally a terminal operation that produces
 * a result.  At each stage of the pipeline, a combined stream and operation
 * flags can be calculated, using {@link #combineOpFlags(int, int)}.  Such flags
 * ensure that preservation, clearing and injecting information is retained at
 * each stage.
 *
 * The combined stream and operation flags for the source stage of the pipeline
 * is calculated as follows:
 * <pre> {@code
 *     int flagsForSourceStage = combineOpFlags(sourceFlags, INITIAL_OPS_VALUE);
 * }</pre>
 *
 * The combined stream and operation flags of each subsequent intermediate
 * operation stage in the pipeline is calculated as follows:
 * <pre> {@code
 *     int flagsForThisStage = combineOpFlags(flagsForPreviousStage, thisOpFlags);
 * }</pre>
 *
 * Finally the flags output from the last intermediate operation of the pipeline
 * are combined with the operation flags of the terminal operation to produce
 * the flags output from the pipeline.
 *
 * <p>Those flags can then be used to apply optimizations. For example, if
 * {@code SIZED.isKnown(flags)} returns true then the stream size remains
 * constant throughout the pipeline, this information can be utilized to
 * pre-allocate data structures and combined with
 * {@link java.util.Spliterator#SUBSIZED} that information can be utilized to
 * perform concurrent in-place updates into a shared array.
 *
 * For specific details see the {@link AbstractPipeline} constructors.
 *
 * @since 1.8
 */
enum StreamOpFlag {

    /*
     * Each characteristic takes up 2 bits in a bit set to accommodate
     * preserving, clearing and setting/injecting information.
     *
     * This applies to stream flags, intermediate/terminal operation flags, and
     * combined stream and operation flags. Even though the former only requires
     * 1 bit of information per characteristic, is it more efficient when
     * combining flags to align set and inject bits.
     *
     * Characteristics belong to certain types, see the Type enum. Bit masks for
     * the types are constructed as per the following table:
     *
     *                        DISTINCT  SORTED  ORDERED  SIZED  SHORT_CIRCUIT
     *          SPLITERATOR      01       01       01      01        00
     *               STREAM      01       01       01      01        00
     *                   OP      11       11       11      10        01
     *          TERMINAL_OP      00       00       10      00        01
     * UPSTREAM_TERMINAL_OP      00       00       10      00        00
     *
     * 01 = set/inject
     * 10 = clear
     * 11 = preserve
     *
     * Construction of the columns is performed using a simple builder for
     * non-zero values.
     */


    // The following flags correspond to characteristics on Spliterator
    // and the values MUST be equal.
    //

    /**
     * Characteristic value signifying that, for each pair of
     * encountered elements in a stream {@code x, y}, {@code !x.equals(y)}.
     * <p>
     * A stream may have this value or an intermediate operation can preserve,
     * clear or inject this value.
     */
    // 0, 0x00000001
    // Matches Spliterator.DISTINCT
    DISTINCT(0,
             set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP)),

    /**
     * Characteristic value signifying that encounter order follows a natural
     * sort order of comparable elements.
     * <p>
     * A stream can have this value or an intermediate operation can preserve,
     * clear or inject this value.
     * <p>
     * Note: The {@link java.util.Spliterator#SORTED} characteristic can define
     * a sort order with an associated non-null comparator.  Augmenting flag
     * state with addition properties such that those properties can be passed
     * to operations requires some disruptive changes for a singular use-case.
     * Furthermore, comparing comparators for equality beyond that of identity
     * is likely to be unreliable.  Therefore the {@code SORTED} characteristic
     * for a defined non-natural sort order is not mapped internally to the
     * {@code SORTED} flag.
     */
    // 1, 0x00000004
    // Matches Spliterator.SORTED
    SORTED(1,
           set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP)),

    /**
     * Characteristic value signifying that an encounter order is
     * defined for stream elements.
     * <p>
     * A stream can have this value, an intermediate operation can preserve,
     * clear or inject this value, or a terminal operation can preserve or clear
     * this value.
     */
    // 2, 0x00000010
    // Matches Spliterator.ORDERED
    ORDERED(2,
            set(Type.SPLITERATOR).set(Type.STREAM).setAndClear(Type.OP).clear(Type.TERMINAL_OP)
                    .clear(Type.UPSTREAM_TERMINAL_OP)),

    /**
     * Characteristic value signifying that size of the stream
     * is of a known finite size that is equal to the known finite
     * size of the source spliterator input to the first stream
     * in the pipeline.
     * <p>
     * A stream can have this value or an intermediate operation can preserve or
     * clear this value.
     */
    // 3, 0x00000040
    // Matches Spliterator.SIZED
    SIZED(3,
          set(Type.SPLITERATOR).set(Type.STREAM).clear(Type.OP)),

    // The following Spliterator characteristics are not currently used but a
    // gap in the bit set is deliberately retained to enable corresponding
    // stream flags if//when required without modification to other flag values.
    //
    // 4, 0x00000100 NONNULL(4, ...
    // 5, 0x00000400 IMMUTABLE(5, ...
    // 6, 0x00001000 CONCURRENT(6, ...
    // 7, 0x00004000 SUBSIZED(7, ...

    // The following 4 flags are currently undefined and a free for any further
    // spliterator characteristics.
    //
    //  8, 0x00010000
    //  9, 0x00040000
    // 10, 0x00100000
    // 11, 0x00400000

    // The following flags are specific to streams and operations
    //

    /**
     * Characteristic value signifying that an operation may short-circuit the
     * stream.
     * <p>
     * An intermediate operation can preserve or inject this value,
     * or a terminal operation can preserve or inject this value.
     */
    // 12, 0x01000000
    SHORT_CIRCUIT(12,
                  set(Type.OP).set(Type.TERMINAL_OP)),

    /**
     * Characteristic value signifying that an operation may adjust the
     * total size of the stream.
     * <p>
     * The flag, if present, is only valid when SIZED is present;
     * and is only valid for sequential streams.
     * <p>
     * An intermediate operation can preserve or inject this value.
     */
    // 13, 0x04000000
    SIZE_ADJUSTING(13,
                   set(Type.OP));

    // The following 2 flags are currently undefined and a free for any further
    // stream flags if/when required
    //
    // 14, 0x10000000
    // 15, 0x40000000

    /**
     * Type of a flag
     */
    enum Type {
        /**
         * The flag is associated with spliterator characteristics.
         */
        SPLITERATOR,

        /**
         * The flag is associated with stream flags.
         */
        STREAM,

        /**
         * The flag is associated with intermediate operation flags.
         */
        OP,

        /**
         * The flag is associated with terminal operation flags.
         */
        TERMINAL_OP,

        /**
         * The flag is associated with terminal operation flags that are
         * propagated upstream across the last stateful operation boundary
         */
        UPSTREAM_TERMINAL_OP
    }

    /**
     * The bit pattern for setting/injecting a flag.
     */
    private static final int SET_BITS = 0b01;

    /**
     * The bit pattern for clearing a flag.
     */
    private static final int CLEAR_BITS = 0b10;

    /**
     * The bit pattern for preserving a flag.
     */
    private static final int PRESERVE_BITS = 0b11;

    private static MaskBuilder set(Type t) {
        return new MaskBuilder(new EnumMap<>(Type.class)).set(t);
    }

    private static class MaskBuilder {
        final Map<Type, Integer> map;

        MaskBuilder(Map<Type, Integer> map) {
            this.map = map;
        }

        MaskBuilder mask(Type t, Integer i) {
            map.put(t, i);
            return this;
        }

        MaskBuilder set(Type t) {
            return mask(t, SET_BITS);
        }

        MaskBuilder clear(Type t) {
            return mask(t, CLEAR_BITS);
        }

        MaskBuilder setAndClear(Type t) {
            return mask(t, PRESERVE_BITS);
        }

        Map<Type, Integer> build() {
            for (Type t : Type.values()) {
                map.putIfAbsent(t, 0b00);
            }
            return map;
        }
    }

    /**
     * The mask table for a flag, this is used to determine if a flag
     * corresponds to a certain flag type and for creating mask constants.
     */
    private final Map<Type, Integer> maskTable;

    /**
     * The bit position in the bit mask.
     */
    private final int bitPosition;

    /**
     * The set 2 bit set offset at the bit position.
     */
    private final int set;

    /**
     * The clear 2 bit set offset at the bit position.
     */
    private final int clear;

    /**
     * The preserve 2 bit set offset at the bit position.
     */
    private final int preserve;

    private StreamOpFlag(int position, MaskBuilder maskBuilder) {
        this.maskTable = maskBuilder.build();
        // Two bits per flag
        position *= 2;
        this.bitPosition = position;
        this.set = SET_BITS << position;
        this.clear = CLEAR_BITS << position;
        this.preserve = PRESERVE_BITS << position;
    }

    /**
     * Gets the bitmap associated with setting this characteristic.
     *
     * @return the bitmap for setting this characteristic
     */
    int set() {
        return set;
    }

    /**
     * Gets the bitmap associated with clearing this characteristic.
     *
     * @return the bitmap for clearing this characteristic
     */
    int clear() {
        return clear;
    }

    /**
     * Determines if this flag is a stream-based flag.
     *
     * @return true if a stream-based flag, otherwise false.
     */
    boolean isStreamFlag() {
        return maskTable.get(Type.STREAM) > 0;
    }

    /**
     * Checks if this flag is set on stream flags, injected on operation flags,
     * and injected on combined stream and operation flags.
     *
     * @param flags the stream flags, operation flags, or combined stream and
     *        operation flags
     * @return true if this flag is known, otherwise false.
     */
    boolean isKnown(int flags) {
        return (flags & preserve) == set;
    }

    /**
     * Checks if this flag is cleared on operation flags or combined stream and
     * operation flags.
     *
     * @param flags the operation flags or combined stream and operations flags.
     * @return true if this flag is preserved, otherwise false.
     */
    boolean isCleared(int flags) {
        return (flags & preserve) == clear;
    }

    /**
     * Checks if this flag is preserved on combined stream and operation flags.
     *
     * @param flags the combined stream and operations flags.
     * @return true if this flag is preserved, otherwise false.
     */
    boolean isPreserved(int flags) {
        return (flags & preserve) == preserve;
    }

    /**
     * Determines if this flag can be set for a flag type.
     *
     * @param t the flag type.
     * @return true if this flag can be set for the flag type, otherwise false.
     */
    boolean canSet(Type t) {
        return (maskTable.get(t) & SET_BITS) > 0;
    }

    /**
     * The bit mask for spliterator characteristics
     */
    static final int SPLITERATOR_CHARACTERISTICS_MASK = createMask(Type.SPLITERATOR);

    /**
     * The bit mask for source stream flags.
     */
    static final int STREAM_MASK = createMask(Type.STREAM);

    /**
     * The bit mask for intermediate operation flags.
     */
    static final int OP_MASK = createMask(Type.OP);

    /**
     * The bit mask for terminal operation flags.
     */
    static final int TERMINAL_OP_MASK = createMask(Type.TERMINAL_OP);

    /**
     * The bit mask for upstream terminal operation flags.
     */
    static final int UPSTREAM_TERMINAL_OP_MASK = createMask(Type.UPSTREAM_TERMINAL_OP);

    private static int createMask(Type t) {
        int mask = 0;
        for (StreamOpFlag flag : StreamOpFlag.values()) {
            mask |= flag.maskTable.get(t) << flag.bitPosition;
        }
        return mask;
    }

    /**
     * Complete flag mask.
     */
    private static final int FLAG_MASK = createFlagMask();

    private static int createFlagMask() {
        int mask = 0;
        for (StreamOpFlag flag : StreamOpFlag.values()) {
            mask |= flag.preserve;
        }
        return mask;
    }

    /**
     * Flag mask for stream flags that are set.
     */
    private static final int FLAG_MASK_IS = STREAM_MASK;

    /**
     * Flag mask for stream flags that are cleared.
     */
    private static final int FLAG_MASK_NOT = STREAM_MASK << 1;

    /**
     * The initial value to be combined with the stream flags of the first
     * stream in the pipeline.
     */
    static final int INITIAL_OPS_VALUE = FLAG_MASK_IS | FLAG_MASK_NOT;

    /**
     * The bit value to set or inject {@link #DISTINCT}.
     */
    static final int IS_DISTINCT = DISTINCT.set;

    /**
     * The bit value to clear {@link #DISTINCT}.
     */
    static final int NOT_DISTINCT = DISTINCT.clear;

    /**
     * The bit value to set or inject {@link #SORTED}.
     */
    static final int IS_SORTED = SORTED.set;

    /**
     * The bit value to clear {@link #SORTED}.
     */
    static final int NOT_SORTED = SORTED.clear;

    /**
     * The bit value to set or inject {@link #ORDERED}.
     */
    static final int IS_ORDERED = ORDERED.set;

    /**
     * The bit value to clear {@link #ORDERED}.
     */
    static final int NOT_ORDERED = ORDERED.clear;

    /**
     * The bit value to set {@link #SIZED}.
     */
    static final int IS_SIZED = SIZED.set;

    /**
     * The bit value to clear {@link #SIZED}.
     */
    static final int NOT_SIZED = SIZED.clear;

    /**
     * The bit value to inject {@link #SHORT_CIRCUIT}.
     */
    static final int IS_SHORT_CIRCUIT = SHORT_CIRCUIT.set;

    /**
     * The bit value to inject {@link #SIZE_ADJUSTING}.
     */
    static final int IS_SIZE_ADJUSTING = SIZE_ADJUSTING.set;

    private static int getMask(int flags) {
        return (flags == 0)
               ? FLAG_MASK
               : ~(flags | ((FLAG_MASK_IS & flags) << 1) | ((FLAG_MASK_NOT & flags) >> 1));
    }

    /**
     * Combines stream or operation flags with previously combined stream and
     * operation flags to produce updated combined stream and operation flags.
     * <p>
     * A flag set on stream flags or injected on operation flags,
     * and injected combined stream and operation flags,
     * will be injected on the updated combined stream and operation flags.
     *
     * <p>
     * A flag set on stream flags or injected on operation flags,
     * and cleared on the combined stream and operation flags,
     * will be cleared on the updated combined stream and operation flags.
     *
     * <p>
     * A flag set on the stream flags or injected on operation flags,
     * and preserved on the combined stream and operation flags,
     * will be injected on the updated combined stream and operation flags.
     *
     * <p>
     * A flag not set on the stream flags or cleared/preserved on operation
     * flags, and injected on the combined stream and operation flags,
     * will be injected on the updated combined stream and operation flags.
     *
     * <p>
     * A flag not set on the stream flags or cleared/preserved on operation
     * flags, and cleared on the combined stream and operation flags,
     * will be cleared on the updated combined stream and operation flags.
     *
     * <p>
     * A flag not set on the stream flags,
     * and preserved on the combined stream and operation flags
     * will be preserved on the updated combined stream and operation flags.
     *
     * <p>
     * A flag cleared on operation flags,
     * and preserved on the combined stream and operation flags
     * will be cleared on the updated combined stream and operation flags.
     *
     * <p>
     * A flag preserved on operation flags,
     * and preserved on the combined stream and operation flags
     * will be preserved on the updated combined stream and operation flags.
     *
     * @param newStreamOrOpFlags the stream or operation flags.
     * @param prevCombOpFlags previously combined stream and operation flags.
     *        The value {#link INITIAL_OPS_VALUE} must be used as the seed value.
     * @return the updated combined stream and operation flags.
     */
    static int combineOpFlags(int newStreamOrOpFlags, int prevCombOpFlags) {
        // 0x01 or 0x10 nibbles are transformed to 0x11
        // 0x00 nibbles remain unchanged
        // Then all the bits are flipped
        // Then the result is logically or'ed with the operation flags.
        return (prevCombOpFlags & StreamOpFlag.getMask(newStreamOrOpFlags)) | newStreamOrOpFlags;
    }

    /**
     * Converts combined stream and operation flags to stream flags.
     *
     * <p>Each flag injected on the combined stream and operation flags will be
     * set on the stream flags.
     *
     * @param combOpFlags the combined stream and operation flags.
     * @return the stream flags.
     */
    static int toStreamFlags(int combOpFlags) {
        // By flipping the nibbles 0x11 become 0x00 and 0x01 become 0x10
        // Shift left 1 to restore set flags and mask off anything other than the set flags
        return ((~combOpFlags) >> 1) & FLAG_MASK_IS & combOpFlags;
    }

    /**
     * Converts stream flags to a spliterator characteristic bit set.
     *
     * @param streamFlags the stream flags.
     * @return the spliterator characteristic bit set.
     */
    static int toCharacteristics(int streamFlags) {
        return streamFlags & SPLITERATOR_CHARACTERISTICS_MASK;
    }

    /**
     * Converts a spliterator characteristic bit set to stream flags.
     *
     * @implSpec
     * If the spliterator is naturally {@code SORTED} (the associated
     * {@code Comparator} is {@code null}) then the characteristic is converted
     * to the {@link #SORTED} flag, otherwise the characteristic is not
     * converted.
     *
     * @param spliterator the spliterator from which to obtain characteristic
     *        bit set.
     * @return the stream flags.
     */
    static int fromCharacteristics(Spliterator<?> spliterator) {
        int characteristics = spliterator.characteristics();
        if ((characteristics & Spliterator.SORTED) != 0 && spliterator.getComparator() != null) {
            // Do not propagate the SORTED characteristic if it does not correspond
            // to a natural sort order
            return characteristics & SPLITERATOR_CHARACTERISTICS_MASK & ~Spliterator.SORTED;
        }
        else {
            return characteristics & SPLITERATOR_CHARACTERISTICS_MASK;
        }
    }

    /**
     * Converts a spliterator characteristic bit set to stream flags.
     *
     * @param characteristics the spliterator characteristic bit set.
     * @return the stream flags.
     */
    static int fromCharacteristics(int characteristics) {
        return characteristics & SPLITERATOR_CHARACTERISTICS_MASK;
    }
}
