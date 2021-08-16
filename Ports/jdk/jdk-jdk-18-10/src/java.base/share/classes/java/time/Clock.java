/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java.time;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

import static java.time.LocalTime.NANOS_PER_MINUTE;
import static java.time.LocalTime.NANOS_PER_SECOND;
import static java.time.LocalTime.NANOS_PER_MILLI;
import java.io.Serializable;
import java.util.Objects;
import java.util.TimeZone;
import jdk.internal.misc.VM;

/**
 * A clock providing access to the current instant, date and time using a time-zone.
 * <p>
 * Instances of this abstract class are used to access a pluggable representation of the
 * current instant, which can be interpreted using the stored time-zone to find the
 * current date and time.
 * For example, {@code Clock} can be used instead of {@link System#currentTimeMillis()}
 * and {@link TimeZone#getDefault()}.
 * <p>
 * Use of a {@code Clock} is optional. All key date-time classes also have a
 * {@code now()} factory method that uses the system clock in the default time zone.
 * The primary purpose of this abstraction is to allow alternate clocks to be
 * plugged in as and when required. Applications use an object to obtain the
 * current time rather than a static method. This can simplify testing.
 * <p>
 * As such, this abstract class does not guarantee the result actually represents the current instant
 * on the time-line. Instead, it allows the application to provide a controlled view as to what
 * the current instant and time-zone are.
 * <p>
 * Best practice for applications is to pass a {@code Clock} into any method
 * that requires the current instant and time-zone. A dependency injection framework
 * is one way to achieve this:
 * <pre>
 *  public class MyBean {
 *    private Clock clock;  // dependency inject
 *    ...
 *    public void process(LocalDate eventDate) {
 *      if (eventDate.isBefore(LocalDate.now(clock)) {
 *        ...
 *      }
 *    }
 *  }
 * </pre>
 * This approach allows an alternative clock, such as {@link #fixed(Instant, ZoneId) fixed}
 * or {@link #offset(Clock, Duration) offset} to be used during testing.
 * <p>
 * The {@code system} factory methods provide clocks based on the best available
 * system clock. This may use {@link System#currentTimeMillis()}, or a higher
 * resolution clock if one is available.
 *
 * @implSpec
 * This abstract class must be implemented with care to ensure other classes operate correctly.
 * All implementations must be thread-safe - a single instance must be capable of be invoked
 * from multiple threads without negative consequences such as race conditions.
 * <p>
 * The principal methods are defined to allow the throwing of an exception.
 * In normal use, no exceptions will be thrown, however one possible implementation would be to
 * obtain the time from a central time server across the network. Obviously, in this case the
 * lookup could fail, and so the method is permitted to throw an exception.
 * <p>
 * The returned instants from {@code Clock} work on a time-scale that ignores leap seconds,
 * as described in {@link Instant}. If the implementation wraps a source that provides leap
 * second information, then a mechanism should be used to "smooth" the leap second.
 * The Java Time-Scale mandates the use of UTC-SLS, however clock implementations may choose
 * how accurate they are with the time-scale so long as they document how they work.
 * Implementations are therefore not required to actually perform the UTC-SLS slew or to
 * otherwise be aware of leap seconds.
 * <p>
 * Implementations should implement {@code Serializable} wherever possible and must
 * document whether or not they do support serialization.
 *
 * @see InstantSource
 *
 * @since 1.8
 */
public abstract class Clock implements InstantSource {

    /**
     * Obtains a clock that returns the current instant using the best available
     * system clock, converting to date and time using the UTC time-zone.
     * <p>
     * This clock, rather than {@link #systemDefaultZone()}, should be used when
     * you need the current instant without the date or time.
     * <p>
     * This clock is based on the best available system clock.
     * This may use {@link System#currentTimeMillis()}, or a higher resolution
     * clock if one is available.
     * <p>
     * Conversion from instant to date or time uses the {@linkplain ZoneOffset#UTC UTC time-zone}.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     * It is equivalent to {@code system(ZoneOffset.UTC)}.
     *
     * @return a clock that uses the best available system clock in the UTC zone, not null
     */
    public static Clock systemUTC() {
        return SystemClock.UTC;
    }

    /**
     * Obtains a clock that returns the current instant using the best available
     * system clock, converting to date and time using the default time-zone.
     * <p>
     * This clock is based on the best available system clock.
     * This may use {@link System#currentTimeMillis()}, or a higher resolution
     * clock if one is available.
     * <p>
     * Using this method hard codes a dependency to the default time-zone into your application.
     * It is recommended to avoid this and use a specific time-zone whenever possible.
     * The {@link #systemUTC() UTC clock} should be used when you need the current instant
     * without the date or time.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     * It is equivalent to {@code system(ZoneId.systemDefault())}.
     *
     * @return a clock that uses the best available system clock in the default zone, not null
     * @see ZoneId#systemDefault()
     */
    public static Clock systemDefaultZone() {
        return new SystemClock(ZoneId.systemDefault());
    }

    /**
     * Obtains a clock that returns the current instant using the best available
     * system clock.
     * <p>
     * This clock is based on the best available system clock.
     * This may use {@link System#currentTimeMillis()}, or a higher resolution
     * clock if one is available.
     * <p>
     * Conversion from instant to date or time uses the specified time-zone.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     *
     * @param zone  the time-zone to use to convert the instant to date-time, not null
     * @return a clock that uses the best available system clock in the specified zone, not null
     */
    public static Clock system(ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        if (zone == ZoneOffset.UTC) {
            return SystemClock.UTC;
        }
        return new SystemClock(zone);
    }

    //-------------------------------------------------------------------------
    /**
     * Obtains a clock that returns the current instant ticking in whole milliseconds
     * using the best available system clock.
     * <p>
     * This clock will always have the nano-of-second field truncated to milliseconds.
     * This ensures that the visible time ticks in whole milliseconds.
     * The underlying clock is the best available system clock, equivalent to
     * using {@link #system(ZoneId)}.
     * <p>
     * Implementations may use a caching strategy for performance reasons.
     * As such, it is possible that the start of the millisecond observed via this
     * clock will be later than that observed directly via the underlying clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     * It is equivalent to {@code tick(system(zone), Duration.ofMillis(1))}.
     *
     * @param zone  the time-zone to use to convert the instant to date-time, not null
     * @return a clock that ticks in whole milliseconds using the specified zone, not null
     * @since 9
     */
    public static Clock tickMillis(ZoneId zone) {
        return new TickClock(system(zone), NANOS_PER_MILLI);
    }

    //-------------------------------------------------------------------------
    /**
     * Obtains a clock that returns the current instant ticking in whole seconds
     * using the best available system clock.
     * <p>
     * This clock will always have the nano-of-second field set to zero.
     * This ensures that the visible time ticks in whole seconds.
     * The underlying clock is the best available system clock, equivalent to
     * using {@link #system(ZoneId)}.
     * <p>
     * Implementations may use a caching strategy for performance reasons.
     * As such, it is possible that the start of the second observed via this
     * clock will be later than that observed directly via the underlying clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     * It is equivalent to {@code tick(system(zone), Duration.ofSeconds(1))}.
     *
     * @param zone  the time-zone to use to convert the instant to date-time, not null
     * @return a clock that ticks in whole seconds using the specified zone, not null
     */
    public static Clock tickSeconds(ZoneId zone) {
        return new TickClock(system(zone), NANOS_PER_SECOND);
    }

    /**
     * Obtains a clock that returns the current instant ticking in whole minutes
     * using the best available system clock.
     * <p>
     * This clock will always have the nano-of-second and second-of-minute fields set to zero.
     * This ensures that the visible time ticks in whole minutes.
     * The underlying clock is the best available system clock, equivalent to
     * using {@link #system(ZoneId)}.
     * <p>
     * Implementations may use a caching strategy for performance reasons.
     * As such, it is possible that the start of the minute observed via this
     * clock will be later than that observed directly via the underlying clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     * It is equivalent to {@code tick(system(zone), Duration.ofMinutes(1))}.
     *
     * @param zone  the time-zone to use to convert the instant to date-time, not null
     * @return a clock that ticks in whole minutes using the specified zone, not null
     */
    public static Clock tickMinutes(ZoneId zone) {
        return new TickClock(system(zone), NANOS_PER_MINUTE);
    }

    /**
     * Obtains a clock that returns instants from the specified clock truncated
     * to the nearest occurrence of the specified duration.
     * <p>
     * This clock will only tick as per the specified duration. Thus, if the duration
     * is half a second, the clock will return instants truncated to the half second.
     * <p>
     * The tick duration must be positive. If it has a part smaller than a whole
     * millisecond, then the whole duration must divide into one second without
     * leaving a remainder. All normal tick durations will match these criteria,
     * including any multiple of hours, minutes, seconds and milliseconds, and
     * sensible nanosecond durations, such as 20ns, 250,000ns and 500,000ns.
     * <p>
     * A duration of zero or one nanosecond would have no truncation effect.
     * Passing one of these will return the underlying clock.
     * <p>
     * Implementations may use a caching strategy for performance reasons.
     * As such, it is possible that the start of the requested duration observed
     * via this clock will be later than that observed directly via the underlying clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}
     * providing that the base clock is.
     *
     * @param baseClock  the base clock to base the ticking clock on, not null
     * @param tickDuration  the duration of each visible tick, not negative, not null
     * @return a clock that ticks in whole units of the duration, not null
     * @throws IllegalArgumentException if the duration is negative, or has a
     *  part smaller than a whole millisecond such that the whole duration is not
     *  divisible into one second
     * @throws ArithmeticException if the duration is too large to be represented as nanos
     */
    public static Clock tick(Clock baseClock, Duration tickDuration) {
        Objects.requireNonNull(baseClock, "baseClock");
        Objects.requireNonNull(tickDuration, "tickDuration");
        if (tickDuration.isNegative()) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }
        long tickNanos = tickDuration.toNanos();
        if (tickNanos % 1000_000 == 0) {
            // ok, no fraction of millisecond
        } else if (1000_000_000 % tickNanos == 0) {
            // ok, divides into one second without remainder
        } else {
            throw new IllegalArgumentException("Invalid tick duration");
        }
        if (tickNanos <= 1) {
            return baseClock;
        }
        return new TickClock(baseClock, tickNanos);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains a clock that always returns the same instant.
     * <p>
     * This clock simply returns the specified instant.
     * As such, it is not a clock in the conventional sense.
     * The main use case for this is in testing, where the fixed clock ensures
     * tests are not dependent on the current clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     *
     * @param fixedInstant  the instant to use as the clock, not null
     * @param zone  the time-zone to use to convert the instant to date-time, not null
     * @return a clock that always returns the same instant, not null
     */
    public static Clock fixed(Instant fixedInstant, ZoneId zone) {
        Objects.requireNonNull(fixedInstant, "fixedInstant");
        Objects.requireNonNull(zone, "zone");
        return new FixedClock(fixedInstant, zone);
    }

    //-------------------------------------------------------------------------
    /**
     * Obtains a clock that returns instants from the specified clock with the
     * specified duration added.
     * <p>
     * This clock wraps another clock, returning instants that are later by the
     * specified duration. If the duration is negative, the instants will be
     * earlier than the current date and time.
     * The main use case for this is to simulate running in the future or in the past.
     * <p>
     * A duration of zero would have no offsetting effect.
     * Passing zero will return the underlying clock.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}
     * providing that the base clock is.
     *
     * @param baseClock  the base clock to add the duration to, not null
     * @param offsetDuration  the duration to add, not null
     * @return a clock based on the base clock with the duration added, not null
     */
    public static Clock offset(Clock baseClock, Duration offsetDuration) {
        Objects.requireNonNull(baseClock, "baseClock");
        Objects.requireNonNull(offsetDuration, "offsetDuration");
        if (offsetDuration.equals(Duration.ZERO)) {
            return baseClock;
        }
        return new OffsetClock(baseClock, offsetDuration);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor accessible by subclasses.
     */
    protected Clock() {
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the time-zone being used to create dates and times.
     * <p>
     * A clock will typically obtain the current instant and then convert that
     * to a date or time using a time-zone. This method returns the time-zone used.
     *
     * @return the time-zone being used to interpret instants, not null
     */
    public abstract ZoneId getZone();

    /**
     * Returns a copy of this clock with a different time-zone.
     * <p>
     * A clock will typically obtain the current instant and then convert that
     * to a date or time using a time-zone. This method returns a clock with
     * similar properties but using a different time-zone.
     *
     * @param zone  the time-zone to change to, not null
     * @return a clock based on this clock with the specified time-zone, not null
     */
    @Override
    public abstract Clock withZone(ZoneId zone);

    //-------------------------------------------------------------------------
    /**
     * Gets the current millisecond instant of the clock.
     * <p>
     * This returns the millisecond-based instant, measured from 1970-01-01T00:00Z (UTC).
     * This is equivalent to the definition of {@link System#currentTimeMillis()}.
     * <p>
     * Most applications should avoid this method and use {@link Instant} to represent
     * an instant on the time-line rather than a raw millisecond value.
     * This method is provided to allow the use of the clock in high performance use cases
     * where the creation of an object would be unacceptable.
     * <p>
     * The default implementation currently calls {@link #instant}.
     *
     * @return the current millisecond instant from this clock, measured from
     *  the Java epoch of 1970-01-01T00:00Z (UTC), not null
     * @throws DateTimeException if the instant cannot be obtained, not thrown by most implementations
     */
    @Override
    public long millis() {
        return instant().toEpochMilli();
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the current instant of the clock.
     * <p>
     * This returns an instant representing the current instant as defined by the clock.
     *
     * @return the current instant from this clock, not null
     * @throws DateTimeException if the instant cannot be obtained, not thrown by most implementations
     */
    @Override
    public abstract Instant instant();

    //-----------------------------------------------------------------------
    /**
     * Checks if this clock is equal to another clock.
     * <p>
     * Clocks should override this method to compare equals based on
     * their state and to meet the contract of {@link Object#equals}.
     * If not overridden, the behavior is defined by {@link Object#equals}
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other clock
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * A hash code for this clock.
     * <p>
     * Clocks should override this method based on
     * their state and to meet the contract of {@link Object#hashCode}.
     * If not overridden, the behavior is defined by {@link Object#hashCode}
     *
     * @return a suitable hash code
     */
    @Override
    public  int hashCode() {
        return super.hashCode();
    }

    //-----------------------------------------------------------------------
    // initial offset
    private static final long OFFSET_SEED = System.currentTimeMillis() / 1000 - 1024;
    // We don't actually need a volatile here.
    // We don't care if offset is set or read concurrently by multiple
    // threads - we just need a value which is 'recent enough' - in other
    // words something that has been updated at least once in the last
    // 2^32 secs (~136 years). And even if we by chance see an invalid
    // offset, the worst that can happen is that we will get a -1 value
    // from getNanoTimeAdjustment, forcing us to update the offset
    // once again.
    private static long offset = OFFSET_SEED;

    static Instant currentInstant() {
        // Take a local copy of offset. offset can be updated concurrently
        // by other threads (even if we haven't made it volatile) so we will
        // work with a local copy.
        long localOffset = offset;
        long adjustment = VM.getNanoTimeAdjustment(localOffset);

        if (adjustment == -1) {
            // -1 is a sentinel value returned by VM.getNanoTimeAdjustment
            // when the offset it is given is too far off the current UTC
            // time. In principle, this should not happen unless the
            // JVM has run for more than ~136 years (not likely) or
            // someone is fiddling with the system time, or the offset is
            // by chance at 1ns in the future (very unlikely).
            // We can easily recover from all these conditions by bringing
            // back the offset in range and retry.

            // bring back the offset in range. We use -1024 to make
            // it more unlikely to hit the 1ns in the future condition.
            localOffset = System.currentTimeMillis() / 1000 - 1024;

            // retry
            adjustment = VM.getNanoTimeAdjustment(localOffset);

            if (adjustment == -1) {
                // Should not happen: we just recomputed a new offset.
                // It should have fixed the issue.
                throw new InternalError("Offset " + localOffset + " is not in range");
            } else {
                // OK - recovery succeeded. Update the offset for the
                // next call...
                offset = localOffset;
            }
        }
        return Instant.ofEpochSecond(localOffset, adjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * An instant source that always returns the latest time from
     * {@link System#currentTimeMillis()} or equivalent.
     */
    static final class SystemInstantSource implements InstantSource, Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 3232399674412L;
        // this is a singleton, but the class is coded such that it is not a
        // problem if someone hacks around and creates another instance
        static final SystemInstantSource INSTANCE = new SystemInstantSource();

        SystemInstantSource() {
        }
        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.system(zone);
        }
        @Override
        public long millis() {
            // System.currentTimeMillis() and VM.getNanoTimeAdjustment(offset)
            // use the same time source - System.currentTimeMillis() simply
            // limits the resolution to milliseconds.
            // So we take the faster path and call System.currentTimeMillis()
            // directly - in order to avoid the performance penalty of
            // VM.getNanoTimeAdjustment(offset) which is less efficient.
            return System.currentTimeMillis();
        }
        @Override
        public Instant instant() {
            return currentInstant();
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof SystemInstantSource;
        }
        @Override
        public int hashCode() {
            return SystemInstantSource.class.hashCode();
        }
        @Override
        public String toString() {
            return "SystemInstantSource";
        }
        @java.io.Serial
        private Object readResolve() throws ObjectStreamException {
            return SystemInstantSource.INSTANCE;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of a clock that always returns the latest time from
     * {@code SystemInstantSource.INSTANCE}.
     */
    static final class SystemClock extends Clock implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 6740630888130243051L;
        static final SystemClock UTC = new SystemClock(ZoneOffset.UTC);

        private final ZoneId zone;

        SystemClock(ZoneId zone) {
            this.zone = zone;
        }
        @Override
        public ZoneId getZone() {
            return zone;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(this.zone)) {  // intentional NPE
                return this;
            }
            return new SystemClock(zone);
        }
        @Override
        public long millis() {
            // inline of SystemInstantSource.INSTANCE.millis()
            return System.currentTimeMillis();
        }
        @Override
        public Instant instant() {
            // inline of SystemInstantSource.INSTANCE.instant()
            return currentInstant();
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SystemClock) {
                return zone.equals(((SystemClock) obj).zone);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return zone.hashCode() + 1;
        }
        @Override
        public String toString() {
            return "SystemClock[" + zone + "]";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of a clock that always returns the same instant.
     * This is typically used for testing.
     */
    static final class FixedClock extends Clock implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 7430389292664866958L;
        private final Instant instant;
        private final ZoneId zone;

        FixedClock(Instant fixedInstant, ZoneId zone) {
            this.instant = fixedInstant;
            this.zone = zone;
        }
        @Override
        public ZoneId getZone() {
            return zone;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(this.zone)) {  // intentional NPE
                return this;
            }
            return new FixedClock(instant, zone);
        }
        @Override
        public long millis() {
            return instant.toEpochMilli();
        }
        @Override
        public Instant instant() {
            return instant;
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof FixedClock other
                    && instant.equals(other.instant)
                    && zone.equals(other.zone);
        }
        @Override
        public int hashCode() {
            return instant.hashCode() ^ zone.hashCode();
        }
        @Override
        public String toString() {
            return "FixedClock[" + instant + "," + zone + "]";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of a clock that adds an offset to an underlying clock.
     */
    static final class OffsetClock extends Clock implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 2007484719125426256L;
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private final Clock baseClock;
        private final Duration offset;

        OffsetClock(Clock baseClock, Duration offset) {
            this.baseClock = baseClock;
            this.offset = offset;
        }
        @Override
        public ZoneId getZone() {
            return baseClock.getZone();
        }
        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(baseClock.getZone())) {  // intentional NPE
                return this;
            }
            return new OffsetClock(baseClock.withZone(zone), offset);
        }
        @Override
        public long millis() {
            return Math.addExact(baseClock.millis(), offset.toMillis());
        }
        @Override
        public Instant instant() {
            return baseClock.instant().plus(offset);
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof OffsetClock other
                    && baseClock.equals(other.baseClock)
                    && offset.equals(other.offset);
        }
        @Override
        public int hashCode() {
            return baseClock.hashCode() ^ offset.hashCode();
        }
        @Override
        public String toString() {
            return "OffsetClock[" + baseClock + "," + offset + "]";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of a clock that reduces the tick frequency of an underlying clock.
     */
    static final class TickClock extends Clock implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 6504659149906368850L;
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private final Clock baseClock;
        private final long tickNanos;

        TickClock(Clock baseClock, long tickNanos) {
            this.baseClock = baseClock;
            this.tickNanos = tickNanos;
        }
        @Override
        public ZoneId getZone() {
            return baseClock.getZone();
        }
        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(baseClock.getZone())) {  // intentional NPE
                return this;
            }
            return new TickClock(baseClock.withZone(zone), tickNanos);
        }
        @Override
        public long millis() {
            long millis = baseClock.millis();
            return millis - Math.floorMod(millis, tickNanos / 1000_000L);
        }
        @Override
        public Instant instant() {
            if ((tickNanos % 1000_000) == 0) {
                long millis = baseClock.millis();
                return Instant.ofEpochMilli(millis - Math.floorMod(millis, tickNanos / 1000_000L));
            }
            Instant instant = baseClock.instant();
            long nanos = instant.getNano();
            long adjust = Math.floorMod(nanos, tickNanos);
            return instant.minusNanos(adjust);
        }
        @Override
        public boolean equals(Object obj) {
            return (obj instanceof TickClock other)
                    && tickNanos == other.tickNanos
                    && baseClock.equals(other.baseClock);
        }
        @Override
        public int hashCode() {
            return baseClock.hashCode() ^ ((int) (tickNanos ^ (tickNanos >>> 32)));
        }
        @Override
        public String toString() {
            return "TickClock[" + baseClock + "," + Duration.ofNanos(tickNanos) + "]";
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implementation of a clock based on an {@code InstantSource}.
     */
    static final class SourceClock extends Clock implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 235386528762398L;
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private final InstantSource baseSource;
        private final ZoneId zone;

        SourceClock(InstantSource baseSource, ZoneId zone) {
            this.baseSource = baseSource;
            this.zone = zone;
        }
        @Override
        public ZoneId getZone() {
            return zone;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            if (zone.equals(this.zone)) {  // intentional NPE
                return this;
            }
            return new SourceClock(baseSource, zone);
        }
        @Override
        public long millis() {
            return baseSource.millis();
        }
        @Override
        public Instant instant() {
            return baseSource.instant();
        }
        @Override
        public boolean equals(Object obj) {
            return (obj instanceof SourceClock other)
                    && zone.equals(other.zone)
                    && baseSource.equals(other.baseSource);
        }
        @Override
        public int hashCode() {
            return baseSource.hashCode() ^ zone.hashCode();
        }
        @Override
        public String toString() {
            return "SourceClock[" + baseSource + "," + zone + "]";
        }
    }

}
