/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.zipfs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

/**
 * @author Xueming Shen
 */
class ZipUtils {

    /**
     * The bit flag used to specify read permission by the owner.
     */
    static final int POSIX_USER_READ = 0400;

    /**
     * The bit flag used to specify write permission by the owner.
     */
    static final int POSIX_USER_WRITE = 0200;

    /**
     * The bit flag used to specify execute permission by the owner.
     */
    static final int POSIX_USER_EXECUTE = 0100;

    /**
     * The bit flag used to specify read permission by the group.
     */
    static final int POSIX_GROUP_READ = 040;

    /**
     * The bit flag used to specify write permission by the group.
     */
    static final int POSIX_GROUP_WRITE = 020;

    /**
     * The bit flag used to specify execute permission by the group.
     */
    static final int POSIX_GROUP_EXECUTE = 010;

    /**
     * The bit flag used to specify read permission by others.
     */
    static final int POSIX_OTHER_READ = 04;

    /**
     * The bit flag used to specify write permission by others.
     */
    static final int POSIX_OTHER_WRITE = 02;

    /**
     * The bit flag used to specify execute permission by others.
     */
    static final int POSIX_OTHER_EXECUTE = 01;

    /**
     * Convert a {@link PosixFilePermission} object into the appropriate bit
     * flag.
     *
     * @param perm The {@link PosixFilePermission} object.
     * @return The bit flag as int.
     */
    static int permToFlag(PosixFilePermission perm) {
        switch(perm) {
        case OWNER_READ:
            return POSIX_USER_READ;
        case OWNER_WRITE:
            return POSIX_USER_WRITE;
        case OWNER_EXECUTE:
            return POSIX_USER_EXECUTE;
        case GROUP_READ:
            return POSIX_GROUP_READ;
        case GROUP_WRITE:
            return POSIX_GROUP_WRITE;
        case GROUP_EXECUTE:
            return POSIX_GROUP_EXECUTE;
        case OTHERS_READ:
            return POSIX_OTHER_READ;
        case OTHERS_WRITE:
            return POSIX_OTHER_WRITE;
        case OTHERS_EXECUTE:
            return POSIX_OTHER_EXECUTE;
        default:
            return 0;
        }
    }

    /**
     * Converts a set of {@link PosixFilePermission}s into an int value where
     * the according bits are set.
     *
     * @param perms A Set of {@link PosixFilePermission} objects.
     *
     * @return A bit mask representing the input Set.
     */
    static int permsToFlags(Set<PosixFilePermission> perms) {
        if (perms == null) {
            return -1;
        }
        int flags = 0;
        for (PosixFilePermission perm : perms) {
            flags |= permToFlag(perm);
        }
        return flags;
    }

    /*
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    public static void writeShort(OutputStream os, int v) throws IOException {
        os.write(v & 0xff);
        os.write((v >>> 8) & 0xff);
    }

    /*
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    public static void writeInt(OutputStream os, long v) throws IOException {
        os.write((int)(v & 0xff));
        os.write((int)((v >>>  8) & 0xff));
        os.write((int)((v >>> 16) & 0xff));
        os.write((int)((v >>> 24) & 0xff));
    }

    /*
     * Writes a 64-bit int to the output stream in little-endian byte order.
     */
    public static void writeLong(OutputStream os, long v) throws IOException {
        os.write((int)(v & 0xff));
        os.write((int)((v >>>  8) & 0xff));
        os.write((int)((v >>> 16) & 0xff));
        os.write((int)((v >>> 24) & 0xff));
        os.write((int)((v >>> 32) & 0xff));
        os.write((int)((v >>> 40) & 0xff));
        os.write((int)((v >>> 48) & 0xff));
        os.write((int)((v >>> 56) & 0xff));
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    public static void writeBytes(OutputStream os, byte[] b)
        throws IOException
    {
        os.write(b, 0, b.length);
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    public static void writeBytes(OutputStream os, byte[] b, int off, int len)
        throws IOException
    {
        os.write(b, off, len);
    }

    /*
     * Append a slash at the end, if it does not have one yet
     */
    public static byte[] toDirectoryPath(byte[] dir) {
        if (dir.length != 0 && dir[dir.length - 1] != '/') {
            dir = Arrays.copyOf(dir, dir.length + 1);
            dir[dir.length - 1] = '/';
        }
        return dir;
    }

    /*
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    public static long dosToJavaTime(long dtime) {
        int year = (int) (((dtime >> 25) & 0x7f) + 1980);
        int month = (int) ((dtime >> 21) & 0x0f);
        int day = (int) ((dtime >> 16) & 0x1f);
        int hour = (int) ((dtime >> 11) & 0x1f);
        int minute = (int) ((dtime >> 5) & 0x3f);
        int second = (int) ((dtime << 1) & 0x3e);

        if (month > 0 && month < 13 && day > 0 && hour < 24 && minute < 60 && second < 60) {
            try {
                LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second);
                return TimeUnit.MILLISECONDS.convert(ldt.toEpochSecond(
                        ZoneId.systemDefault().getRules().getOffset(ldt)), TimeUnit.SECONDS);
            } catch (DateTimeException dte) {
                // ignore
            }
        }
        return overflowDosToJavaTime(year, month, day, hour, minute, second);
    }

    /*
     * Deal with corner cases where an arguably mal-formed DOS time is used
     */
    @SuppressWarnings("deprecation") // Use of Date constructor
    private static long overflowDosToJavaTime(int year, int month, int day,
                                              int hour, int minute, int second) {
        return new Date(year - 1900, month - 1, day, hour, minute, second).getTime();
    }

    /*
     * Converts Java time to DOS time.
     */
    public static long javaToDosTime(long time) {
        Instant instant = Instant.ofEpochMilli(time);
        LocalDateTime ldt = LocalDateTime.ofInstant(
                instant, ZoneId.systemDefault());
        int year = ldt.getYear() - 1980;
        if (year < 0) {
            return (1 << 21) | (1 << 16);
        }
        return (year << 25 |
            ldt.getMonthValue() << 21 |
            ldt.getDayOfMonth() << 16 |
            ldt.getHour() << 11 |
            ldt.getMinute() << 5 |
            ldt.getSecond() >> 1) & 0xffffffffL;
    }

    // used to adjust values between Windows and java epoch
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;
    public static final long winToJavaTime(long wtime) {
        return TimeUnit.MILLISECONDS.convert(
               wtime / 10 + WINDOWS_EPOCH_IN_MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    public static final long javaToWinTime(long time) {
        return (TimeUnit.MICROSECONDS.convert(time, TimeUnit.MILLISECONDS)
               - WINDOWS_EPOCH_IN_MICROSECONDS) * 10;
    }

    public static final long unixToJavaTime(long utime) {
        return TimeUnit.MILLISECONDS.convert(utime, TimeUnit.SECONDS);
    }

    public static final long javaToUnixTime(long time) {
        return TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS);
    }

    private static final String regexMetaChars = ".^$+{[]|()";
    private static final String globMetaChars = "\\*?[{";
    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf(c) != -1;
    }
    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf(c) != -1;
    }
    private static char EOL = 0;  //TBD
    private static char next(String glob, int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }

    /*
     * Creates a regex pattern from the given glob expression.
     *
     * @throws  PatternSyntaxException
     */
    public static String toRegexPattern(String globPattern) {
        boolean inGroup = false;
        StringBuilder regex = new StringBuilder("^");

        int i = 0;
        while (i < globPattern.length()) {
            char c = globPattern.charAt(i++);
            switch (c) {
                case '\\':
                    // escape special characters
                    if (i == globPattern.length()) {
                        throw new PatternSyntaxException("No character to escape",
                                globPattern, i - 1);
                    }
                    char next = globPattern.charAt(i++);
                    if (isGlobMeta(next) || isRegexMeta(next)) {
                        regex.append('\\');
                    }
                    regex.append(next);
                    break;
                case '/':
                    regex.append(c);
                    break;
                case '[':
                    // don't match name separator in class
                    regex.append("[[^/]&&[");
                    if (next(globPattern, i) == '^') {
                        // escape the regex negation char if it appears
                        regex.append("\\^");
                        i++;
                    } else {
                        // negation
                        if (next(globPattern, i) == '!') {
                            regex.append('^');
                            i++;
                        }
                        // hyphen allowed at start
                        if (next(globPattern, i) == '-') {
                            regex.append('-');
                            i++;
                        }
                    }
                    boolean hasRangeStart = false;
                    char last = 0;
                    while (i < globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if (c == ']') {
                            break;
                        }
                        if (c == '/') {
                            throw new PatternSyntaxException("Explicit 'name separator' in class",
                                    globPattern, i - 1);
                        }
                        // TBD: how to specify ']' in a class?
                        if (c == '\\' || c == '[' ||
                                c == '&' && next(globPattern, i) == '&') {
                            // escape '\', '[' or "&&" for regex class
                            regex.append('\\');
                        }
                        regex.append(c);

                        if (c == '-') {
                            if (!hasRangeStart) {
                                throw new PatternSyntaxException("Invalid range",
                                        globPattern, i - 1);
                            }
                            if ((c = next(globPattern, i++)) == EOL || c == ']') {
                                break;
                            }
                            if (c < last) {
                                throw new PatternSyntaxException("Invalid range",
                                        globPattern, i - 3);
                            }
                            regex.append(c);
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    if (c != ']') {
                        throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
                    }
                    regex.append("]]");
                    break;
                case '{':
                    if (inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups",
                                globPattern, i - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                case '}':
                    if (inGroup) {
                        regex.append("))");
                        inGroup = false;
                    } else {
                        regex.append('}');
                    }
                    break;
                case ',':
                    if (inGroup) {
                        regex.append(")|(?:");
                    } else {
                        regex.append(',');
                    }
                    break;
                case '*':
                    if (next(globPattern, i) == '*') {
                        // crosses directory boundaries
                        regex.append(".*");
                        i++;
                    } else {
                        // within directory boundary
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                   regex.append("[^/]");
                   break;
                default:
                    if (isRegexMeta(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
            }
        }
        if (inGroup) {
            throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
        }
        return regex.append('$').toString();
    }
}
