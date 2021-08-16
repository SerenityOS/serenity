/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.time;

import java.time.Clock.SourceClock;
import java.time.Clock.SystemInstantSource;
import java.util.Objects;

/**
 * Provides access to the current instant.
 * <p>
 * Instances of this interface are used to access a pluggable representation of the current instant.
 * For example, {@code InstantSource} can be used instead of {@link System#currentTimeMillis()}.
 * <p>
 * The primary purpose of this abstraction is to allow alternate instant sources to be
 * plugged in as and when required. Applications use an object to obtain the
 * current time rather than a static method. This can simplify testing.
 * <p>
 * As such, this interface does not guarantee the result actually represents the current instant
 * on the time-line. Instead, it allows the application to provide a controlled view as to what
 * the current instant is.
 * <p>
 * Best practice for applications is to pass an {@code InstantSource} into any method
 * that requires the current instant. A dependency injection framework is one
 * way to achieve this:
 * <pre>
 *  public class MyBean {
 *    private InstantSource source;  // dependency inject
 *    ...
 *    public void process(Instant endInstant) {
 *      if (source.instant().isAfter(endInstant) {
 *        ...
 *      }
 *    }
 *  }
 * </pre>
 * This approach allows an alternative source, such as {@link #fixed(Instant) fixed}
 * or {@link #offset(InstantSource, Duration) offset} to be used during testing.
 * <p>
 * The {@code system} factory method provides a source based on the best available
 * system clock. This may use {@link System#currentTimeMillis()}, or a higher
 * resolution clock if one is available.
 *
 * @implSpec
 * This interface must be implemented with care to ensure other classes operate correctly.
 * All implementations must be thread-safe - a single instance must be capable of be invoked
 * from multiple threads without negative consequences such as race conditions.
 * <p>
 * The principal methods are defined to allow the throwing of an exception.
 * In normal use, no exceptions will be thrown, however one possible implementation would be to
 * obtain the time from a central time server across the network. Obviously, in this case the
 * lookup could fail, and so the method is permitted to throw an exception.
 * <p>
 * The returned instants from {@code InstantSource} work on a time-scale that ignores leap seconds,
 * as described in {@link Instant}. If the implementation wraps a source that provides leap
 * second information, then a mechanism should be used to "smooth" the leap second.
 * The Java Time-Scale mandates the use of UTC-SLS, however implementations may choose
 * how accurate they are with the time-scale so long as they document how they work.
 * Implementations are therefore not required to actually perform the UTC-SLS slew or to
 * otherwise be aware of leap seconds.
 * <p>
 * Implementations should implement {@code Serializable} wherever possible and must
 * document whether or not they do support serialization.
 *
 * @implNote
 * The implementation provided here is based on the same underlying system clock
 * as {@link System#currentTimeMillis()}, but may have a precision finer than
 * milliseconds if available.
 * However, little to no guarantee is provided about the accuracy of the
 * underlying system clock. Applications requiring a more accurate system clock must
 * implement this abstract class themselves using a different external system clock,
 * such as an NTP server.
 *
 * @since 17
 */
public interface InstantSource {

    /**
     * Obtains a source that returns the current instant using the best available
     * system clock.
     * <p>
     * This source is based on the best available system clock. This may use
     * {@link System#currentTimeMillis()}, or a higher resolution system clock if
     * one is available.
     * <p>
     * The returned implementation is immutable, thread-safe and
     * {@code Serializable}.
     *
     * @return a source that uses the best available system clock, not null
     */
    static InstantSource system() {
        return SystemInstantSource.INSTANCE;
    }

    //-------------------------------------------------------------------------
    /**
     * Obtains a source that returns instants from the specified source truncated to
     * the nearest occurrence of the specified duration.
     * <p>
     * This source will only tick as per the specified duration. Thus, if the
     * duration is half a second, the source will return instants truncated to the
     * half second.
     * <p>
     * The tick duration must be positive. If it has a part smaller than a whole
     * millisecond, then the whole duration must divide into one second without
     * leaving a remainder. All normal tick durations will match these criteria,
     * including any multiple of hours, minutes, seconds and milliseconds, and
     * sensible nanosecond durations, such as 20ns, 250,000ns and 500,000ns.
     * <p>
     * A duration of zero or one nanosecond would have no truncation effect. Passing
     * one of these will return the underlying source.
     * <p>
     * Implementations may use a caching strategy for performance reasons. As such,
     * it is possible that the start of the requested duration observed via this
     * source will be later than that observed directly via the underlying source.
     * <p>
     * The returned implementation is immutable, thread-safe and
     * {@code Serializable} providing that the base source is.
     *
     * @param baseSource  the base source to base the ticking source on, not null
     * @param tickDuration  the duration of each visible tick, not negative, not null
     * @return a source that ticks in whole units of the duration, not null
     * @throws IllegalArgumentException if the duration is negative, or has a
     *  part smaller than a whole millisecond such that the whole duration is not
     *  divisible into one second
     * @throws ArithmeticException if the duration is too large to be represented as nanos
     */
    static InstantSource tick(InstantSource baseSource, Duration tickDuration) {
        Objects.requireNonNull(baseSource, "baseSource");
        return Clock.tick(baseSource.withZone(ZoneOffset.UTC), tickDuration);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains a source that always returns the same instant.
     * <p>
     * This source simply returns the specified instant.
     * As such, it is not a source that represents the current instant.
     * The main use case for this is in testing, where the fixed source ensures
     * tests are not dependent on the current source.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}.
     *
     * @param fixedInstant  the instant to use, not null
     * @return a source that always returns the same instant, not null
     */
    static InstantSource fixed(Instant fixedInstant) {
        return Clock.fixed(fixedInstant, ZoneOffset.UTC);
    }

    //-------------------------------------------------------------------------
    /**
     * Obtains a source that returns instants from the specified source with the
     * specified duration added.
     * <p>
     * This source wraps another source, returning instants that are later by the
     * specified duration. If the duration is negative, the instants will be
     * earlier than the current date and time.
     * The main use case for this is to simulate running in the future or in the past.
     * <p>
     * A duration of zero would have no offsetting effect.
     * Passing zero will return the underlying source.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}
     * providing that the base source is.
     *
     * @param baseSource  the base source to add the duration to, not null
     * @param offsetDuration  the duration to add, not null
     * @return a source based on the base source with the duration added, not null
     */
    static InstantSource offset(InstantSource baseSource, Duration offsetDuration) {
        Objects.requireNonNull(baseSource, "baseSource");
        return Clock.offset(baseSource.withZone(ZoneOffset.UTC), offsetDuration);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the current instant of the source.
     * <p>
     * This returns an instant representing the current instant as defined by the source.
     *
     * @return the current instant from this source, not null
     * @throws DateTimeException if the instant cannot be obtained, not thrown by most implementations
     */
    Instant instant();

    //-------------------------------------------------------------------------
    /**
     * Gets the current millisecond instant of the source.
     * <p>
     * This returns the millisecond-based instant, measured from 1970-01-01T00:00Z (UTC).
     * This is equivalent to the definition of {@link System#currentTimeMillis()}.
     * <p>
     * Most applications should avoid this method and use {@link Instant} to represent
     * an instant on the time-line rather than a raw millisecond value.
     * This method is provided to allow the use of the source in high performance use cases
     * where the creation of an object would be unacceptable.
     *
     * @implSpec
     * The default implementation calls {@link #instant()}.
     *
     * @return the current millisecond instant from this source, measured from
     *  the Java epoch of 1970-01-01T00:00Z (UTC), not null
     * @throws DateTimeException if the instant cannot be obtained, not thrown by most implementations
     */
    default long millis() {
        return instant().toEpochMilli();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a clock with the specified time-zone.
     * <p>
     * This returns a {@link Clock}, which is an extension of this interface
     * that combines this source and the specified time-zone.
     * <p>
     * The returned implementation is immutable, thread-safe and {@code Serializable}
     * providing that this source is.
     *
     * @implSpec
     * The default implementation returns an immutable, thread-safe and
     * {@code Serializable} subclass of {@link Clock} that combines this
     * source and the specified zone.
     *
     * @param zone  the time-zone to use, not null
     * @return a clock based on this source with the specified time-zone, not null
     */
    default Clock withZone(ZoneId zone) {
        return new SourceClock(this, zone);
    }

}
