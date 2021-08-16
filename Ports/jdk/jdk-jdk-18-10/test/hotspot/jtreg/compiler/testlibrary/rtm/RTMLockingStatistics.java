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

package compiler.testlibrary.rtm;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for +UsePreciseRTMLockingStatistics output.
 *
 * Example of locking statistics:
 *
 * java/lang/ClassLoader.loadClass@7
 * # rtm locks total (estimated): 6656
 * # rtm lock aborts (total): 10000
 * # rtm lock aborts 0 (abort instruction   ): 9999
 * # rtm lock aborts 1 (may succeed on retry): 9999
 * # rtm lock aborts 2 (thread conflict     ): 0
 * # rtm lock aborts 3 (buffer overflow     ): 1
 * # rtm lock aborts 4 (debug or trap hit   ): 0
 * # rtm lock aborts 5 (maximum nested depth): 0
 */
public class RTMLockingStatistics {
    /**
     * Pattern for aborts per abort type entries.
     */
    private static final Pattern ABORT_PATTERN;

    /**
     * Pattern for whole statistics.
     */
    private static final Pattern RTM_LOCKING_STATISTICS_PATTERN;

    static {
        String abortRe
                = "# rtm lock aborts\\s+(?<type>[0-9]+)\\s+\\([a-z\\s]+\\):\\s(?<count>[0-9]+)";

        ABORT_PATTERN = Pattern.compile(abortRe);
        RTM_LOCKING_STATISTICS_PATTERN = Pattern.compile(
                "(?<className>[^.\n]+)\\." +
                "(?<methodName>[^@\n]+)@(?<bci>[0-9]+)\n" +
                "# rtm locks total \\(estimated\\):\\s*" +
                "(?<totalLocks>[0-9]+)\n" +
                "# rtm lock aborts\\s+\\(total\\):\\s*(?<totalAborts>[0-9]+)\n" +
                "(?<abortStats>(" + abortRe + "\n)+)");
    }

    private final long totalLocks;
    private final long totalAborts;
    private final String className;
    private final String methodName;
    private final int bci;
    private final Map<AbortType, Long> aborts = new EnumMap<>(AbortType.class);

    /**
     * Constructs RTMLockingStatistics from matcher captured statistics entry.
     * @param matcher Matcher captured statistics entry.
     */
    private RTMLockingStatistics(Matcher matcher) {
        className = matcher.group("className");
        methodName = matcher.group("methodName");
        bci = Integer.valueOf(matcher.group("bci"));
        totalLocks = Long.valueOf(matcher.group("totalLocks"));
        totalAborts = Long.valueOf(matcher.group("totalAborts"));

        Matcher abortMatcher = ABORT_PATTERN.matcher(matcher.
                group("abortStats"));

        while (abortMatcher.find()) {
            int type = Integer.valueOf(abortMatcher.group("type"));
            long count = Long.valueOf(abortMatcher.group("count"));
            setAborts(AbortType.lookup(type), count);
        }
    }


    /**
     * Parses string and return all founded RTM locking statistics entries.
     *
     * @param str the string to be parsed.
     * @return list with all founded RTM locking statistics entries or
     *         empty list if nothing was found.
     */
    public static List<RTMLockingStatistics> fromString(String str) {
        List<RTMLockingStatistics> statistics = new LinkedList<>();
        Matcher matcher = RTM_LOCKING_STATISTICS_PATTERN.matcher(str);

        while (matcher.find()) {
            RTMLockingStatistics lock = new RTMLockingStatistics(matcher);
            statistics.add(lock);
        }

        return statistics;
    }

    /**
     * Parses string and return all founded RTM locking statistics entries
     * for locks in method {@code methodName}.
     *
     * @param methodName a name of the method for locks from which statistics
     *                   should be gathered.
     * @param str the string to be parsed.
     * @return list with all founded RTM locking statistics entries or
     *         empty list if nothing was found.
     */
    public static List<RTMLockingStatistics> fromString(String methodName,
            String str) {
        String formattedMethodName = formatMethodName(methodName);

        List<RTMLockingStatistics> statisticsForMethod = new LinkedList<>();
        for (RTMLockingStatistics statistics : fromString(str)) {
            if (statistics.getLockName().startsWith(formattedMethodName)) {
                statisticsForMethod.add(statistics);
            }
        }
        return statisticsForMethod;
    }

    /**
     * Formats method's name so it will have the same format as
     * in rtm locking statistics.
     *
     * <pre>
     * Example:
     * com/example/Klass::method =&gt; com/example/Klass.method
     * com/example/Klass.method  =&gt; com/example/Klass.method
     * com.example.Klass::method =&gt; com/example/Klass.method
     * com.example.Klass.method  =&gt; com/example/Klass.method
     * </pre>
     *
     * @param methodName method's name that should be formatted.
     * @return formatted method's name.
     */
    private static String formatMethodName(String methodName) {
        String m[];
        if (methodName.contains("::")) {
            m = methodName.split("::");
        } else {
            int splitAt = methodName.lastIndexOf('.');
            m = new String[2];
            m[0] = methodName.substring(0, splitAt);
            m[1] = methodName.substring(splitAt + 1);
        }
        return String.format("%s.%s", m[0].replaceAll("\\.", "/"), m[1]);
    }

    /**
     * Returns name of lock for which this statistics was collected.
     * Lock name has following format:
     * &lt;class name&gt;.&lt;method name&gt;@&lt;bci&gt;
     *
     * @return name of lock.
     */
    public String getLockName() {
        return String.format("%s.%s@%d", className, methodName, bci);
    }

    /**
     * Returns aborts count for specified abort type.
     *
     * @param type an abort type.
     * @return count of aborts.
     */
    public long getAborts(AbortType type) {
        return aborts.getOrDefault(type, 0L);
    }

    /**
     * Sets aborts count for specified abort type.
     *
     * @param type an abort type.
     * @param count count of aborts.
     */
    public void setAborts(AbortType type, long count) {
        aborts.put(type, count);
    }

    public long getTotalLocks() {
        return totalLocks;
    }

    public long getTotalAborts() {
        return totalAborts;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getLockName()).append('\n');
        builder.append(String.format("# rtm locks total (estimated): %d\n",
                getTotalLocks()));
        builder.append(String.format("# rtm lock aborts: %d\n",
                getTotalLocks()));

        for (AbortType type : AbortType.values()) {
            builder.append(String.format("# rtm lock aborts %s %d\n",
                    type.toString(), getAborts(type)));
        }
        return builder.toString();
    }
}
