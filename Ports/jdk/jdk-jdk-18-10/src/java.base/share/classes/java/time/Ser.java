/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.StreamCorruptedException;

/**
 * The shared serialization delegate for this package.
 *
 * @implNote
 * This class wraps the object being serialized, and takes a byte representing the type of the class to
 * be serialized.  This byte can also be used for versioning the serialization format.  In this case another
 * byte flag would be used in order to specify an alternative version of the type format.
 * For example {@code LOCAL_DATE_TYPE_VERSION_2 = 21}.
 * <p>
 * In order to serialize the object it writes its byte and then calls back to the appropriate class where
 * the serialization is performed.  In order to deserialize the object it read in the type byte, switching
 * in order to select which class to call back into.
 * <p>
 * The serialization format is determined on a per class basis.  In the case of field based classes each
 * of the fields is written out with an appropriate size format in descending order of the field's size.  For
 * example in the case of {@link LocalDate} year is written before month.  Composite classes, such as
 * {@link LocalDateTime} are serialized as one object.
 * <p>
 * This class is mutable and should be created once per serialization.
 *
 * @serial include
 * @since 1.8
 */
final class Ser implements Externalizable {

    /**
     * Serialization version.
     */
    @java.io.Serial
    private static final long serialVersionUID = -7683839454370182990L;

    static final byte DURATION_TYPE = 1;
    static final byte INSTANT_TYPE = 2;
    static final byte LOCAL_DATE_TYPE = 3;
    static final byte LOCAL_TIME_TYPE = 4;
    static final byte LOCAL_DATE_TIME_TYPE = 5;
    static final byte ZONE_DATE_TIME_TYPE = 6;
    static final byte ZONE_REGION_TYPE = 7;
    static final byte ZONE_OFFSET_TYPE = 8;
    static final byte OFFSET_TIME_TYPE = 9;
    static final byte OFFSET_DATE_TIME_TYPE = 10;
    static final byte YEAR_TYPE = 11;
    static final byte YEAR_MONTH_TYPE = 12;
    static final byte MONTH_DAY_TYPE = 13;
    static final byte PERIOD_TYPE = 14;

    /** The type being serialized. */
    private byte type;
    /** The object being serialized. */
    private Serializable object;

    /**
     * Constructor for deserialization.
     */
    public Ser() {
    }

    /**
     * Creates an instance for serialization.
     *
     * @param type  the type
     * @param object  the object
     */
    Ser(byte type, Serializable object) {
        this.type = type;
        this.object = object;
    }

    //-----------------------------------------------------------------------
    /**
     * Implements the {@code Externalizable} interface to write the object.
     * @serialData
     *
     * Each serializable class is mapped to a type that is the first byte
     * in the stream.  Refer to each class {@code writeReplace}
     * serialized form for the value of the type and sequence of values for the type.
     * <ul>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Duration">Duration.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Instant">Instant.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalDate">LocalDate.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalDateTime">LocalDateTime.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalTime">LocalTime.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.MonthDay">MonthDay.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.OffsetTime">OffsetTime.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.OffsetDateTime">OffsetDateTime.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Period">Period.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Year">Year.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.YearMonth">YearMonth.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZoneId">ZoneId.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZoneOffset">ZoneOffset.writeReplace</a>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZonedDateTime">ZonedDateTime.writeReplace</a>
     * </ul>
     *
     * @param out  the data stream to write to, not null
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeInternal(type, object, out);
    }

    static void writeInternal(byte type, Object object, ObjectOutput out) throws IOException {
        out.writeByte(type);
        switch (type) {
            case DURATION_TYPE         -> ((Duration) object).writeExternal(out);
            case INSTANT_TYPE          -> ((Instant) object).writeExternal(out);
            case LOCAL_DATE_TYPE       -> ((LocalDate) object).writeExternal(out);
            case LOCAL_DATE_TIME_TYPE  -> ((LocalDateTime) object).writeExternal(out);
            case LOCAL_TIME_TYPE       -> ((LocalTime) object).writeExternal(out);
            case ZONE_REGION_TYPE      -> ((ZoneRegion) object).writeExternal(out);
            case ZONE_OFFSET_TYPE      -> ((ZoneOffset) object).writeExternal(out);
            case ZONE_DATE_TIME_TYPE   -> ((ZonedDateTime) object).writeExternal(out);
            case OFFSET_TIME_TYPE      -> ((OffsetTime) object).writeExternal(out);
            case OFFSET_DATE_TIME_TYPE -> ((OffsetDateTime) object).writeExternal(out);
            case YEAR_TYPE             -> ((Year) object).writeExternal(out);
            case YEAR_MONTH_TYPE       -> ((YearMonth) object).writeExternal(out);
            case MONTH_DAY_TYPE        -> ((MonthDay) object).writeExternal(out);
            case PERIOD_TYPE           -> ((Period) object).writeExternal(out);
            default -> throw new InvalidClassException("Unknown serialized type");
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implements the {@code Externalizable} interface to read the object.
     * @serialData
     *
     * The streamed type and parameters defined by the type's {@code writeReplace}
     * method are read and passed to the corresponding static factory for the type
     * to create a new instance.  That instance is returned as the de-serialized
     * {@code Ser} object.
     *
     * <ul>
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Duration">Duration</a> -
     *          {@code Duration.ofSeconds(seconds, nanos);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Instant">Instant</a> -
     *          {@code Instant.ofEpochSecond(seconds, nanos);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalDate">LocalDate</a> -
     *          {@code LocalDate.of(year, month, day);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalDateTime">LocalDateTime</a> -
     *          {@code LocalDateTime.of(date, time);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.LocalTime">LocalTime</a> -
     *          {@code LocalTime.of(hour, minute, second, nano);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.MonthDay">MonthDay</a> -
     *          {@code MonthDay.of(month, day);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.OffsetTime">OffsetTime</a> -
     *          {@code OffsetTime.of(time, offset);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.OffsetDateTime">OffsetDateTime</a> -
     *          {@code OffsetDateTime.of(dateTime, offset);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Period">Period</a> -
     *          {@code Period.of(years, months, days);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.Year">Year</a> -
     *          {@code Year.of(year);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.YearMonth">YearMonth</a> -
     *          {@code YearMonth.of(year, month);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZonedDateTime">ZonedDateTime</a> -
     *          {@code ZonedDateTime.ofLenient(dateTime, offset, zone);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZoneId">ZoneId</a> -
     *          {@code ZoneId.of(id);}
     * <li><a href="{@docRoot}/serialized-form.html#java.time.ZoneOffset">ZoneOffset</a> -
     *          {@code (offsetByte == 127 ? ZoneOffset.ofTotalSeconds(in.readInt()) :
     *          ZoneOffset.ofTotalSeconds(offsetByte * 900));}
     * </ul>
     *
     * @param in  the data to read, not null
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = in.readByte();
        object = readInternal(type, in);
    }

    static Serializable read(ObjectInput in) throws IOException, ClassNotFoundException {
        byte type = in.readByte();
        return readInternal(type, in);
    }

    private static Serializable readInternal(byte type, ObjectInput in)
            throws IOException, ClassNotFoundException {
        return switch (type) {
            case DURATION_TYPE         -> Duration.readExternal(in);
            case INSTANT_TYPE          -> Instant.readExternal(in);
            case LOCAL_DATE_TYPE       -> LocalDate.readExternal(in);
            case LOCAL_DATE_TIME_TYPE  -> LocalDateTime.readExternal(in);
            case LOCAL_TIME_TYPE       -> LocalTime.readExternal(in);
            case ZONE_DATE_TIME_TYPE   -> ZonedDateTime.readExternal(in);
            case ZONE_OFFSET_TYPE      -> ZoneOffset.readExternal(in);
            case ZONE_REGION_TYPE      -> ZoneRegion.readExternal(in);
            case OFFSET_TIME_TYPE      -> OffsetTime.readExternal(in);
            case OFFSET_DATE_TIME_TYPE -> OffsetDateTime.readExternal(in);
            case YEAR_TYPE             -> Year.readExternal(in);
            case YEAR_MONTH_TYPE       -> YearMonth.readExternal(in);
            case MONTH_DAY_TYPE        -> MonthDay.readExternal(in);
            case PERIOD_TYPE           -> Period.readExternal(in);
            default -> throw new StreamCorruptedException("Unknown serialized type");
        };
    }

    /**
     * Returns the object that will replace this one.
     *
     * @return the read object, should never be null
     */
    @java.io.Serial
    private Object readResolve() {
         return object;
    }

}
