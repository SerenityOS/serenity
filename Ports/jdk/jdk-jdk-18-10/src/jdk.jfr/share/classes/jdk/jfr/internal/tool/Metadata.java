/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.tool;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.internal.PlatformEventType;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.Type;
import jdk.jfr.internal.TypeLibrary;
import jdk.jfr.internal.consumer.JdkJfrConsumer;

final class Metadata extends Command {

    private static final JdkJfrConsumer PRIVATE_ACCESS = JdkJfrConsumer.instance();

    private static class TypeComparator implements Comparator<Type> {

        @Override
        public int compare(Type t1, Type t2) {
            int g1 = groupValue(t1);
            int g2 = groupValue(t2);
            if (g1 == g2) {
                String n1 = t1.getName();
                String n2 = t2.getName();
                String package1 = n1.substring(0, n1.lastIndexOf('.') + 1);
                String package2 = n2.substring(0, n2.lastIndexOf('.') + 1);

                if (package1.equals(package2)) {
                    return n1.compareTo(n2);
                } else {
                    // Ensure that jdk.* are printed first
                    // This makes it easier to find user defined events at the end.
                    if (Type.SUPER_TYPE_EVENT.equals(t1.getSuperType()) && !package1.equals(package2)) {
                        if (package1.equals("jdk.jfr")) {
                            return -1;
                        }
                        if (package2.equals("jdk.jfr")) {
                            return 1;
                        }
                    }
                    return package1.compareTo(package2);
                }
            } else {
                return Integer.compare(groupValue(t1), groupValue(t2));
            }
        }

        int groupValue(Type t) {
            String superType = t.getSuperType();
            if (superType == null) {
                return 1;
            }
            if (Type.SUPER_TYPE_ANNOTATION.equals(superType)) {
                return 3;
            }
            if (Type.SUPER_TYPE_SETTING.equals(superType)) {
                return 4;
            }
            if (Type.SUPER_TYPE_EVENT.equals(superType)) {
                return 5;
            }
            return 2; // reserved for enums in the future
        }
    }

    @Override
    public String getName() {
        return "metadata";
    }

    @Override
    public List<String> getOptionSyntax() {
        List<String> list = new ArrayList<>();
        list.add("[--categories <filter>]");
        list.add("[--events <filter>]");
        list.add("[<file>]");
        return list;
    }

    @Override
    protected String getTitle() {
        return "Display event metadata, such as labels, descriptions and field layout";
    }

    @Override
    public String getDescription() {
        return getTitle() + ". See 'jfr help metadata' for details.";
    }

    @Override
    public void displayOptionUsage(PrintStream stream) {
        char q = quoteCharacter();
        stream.println("  --categories <filter>   Select events matching a category name.");
        stream.println("                          The filter is a comma-separated list of names,");
        stream.println("                          simple and/or qualified, and/or quoted glob patterns");
        stream.println();
        stream.println("  --events <filter>       Select events matching an event name.");
        stream.println("                          The filter is a comma-separated list of names,");
        stream.println("                          simple and/or qualified, and/or quoted glob patterns");
        stream.println();
        stream.println("  <file>                  Location of the recording file (.jfr)");
        stream.println();
        stream.println("If the <file> parameter is omitted, metadata from the JDK where");
        stream.println("the " + q + "jfr" + q + " tool is located will be used");
        stream.println();
        stream.println();
        stream.println("Example usage:");
        stream.println();
        stream.println(" jfr metadata");
        stream.println();
        stream.println(" jfr metadata --events jdk.ThreadStart recording.jfr");
        stream.println();
        stream.println(" jfr metadata --events CPULoad,GarbageCollection");
        stream.println();
        stream.println(" jfr metadata --categories " + q + "GC,JVM,Java*" + q);
        stream.println();
        stream.println(" jfr metadata --events " + q + "Thread*" + q);
        stream.println();
    }

    @Override
    public void execute(Deque<String> options) throws UserSyntaxException, UserDataException {
        Path file = getOptionalJFRInputFile(options);

        boolean showIds = false;
        boolean foundEventFilter = false;
        boolean foundCategoryFilter = false;
        Predicate<EventType> filter = null;
        int optionCount = options.size();
        while (optionCount > 0) {
            // internal option, doest not export to users
            if (acceptSingleOption(options, "--ids")) {
                showIds = true;
            }
            if (acceptFilterOption(options, "--events")) {
                if (foundEventFilter) {
                    throw new UserSyntaxException("use --events event1,event2,event3 to include multiple events");
                }
                foundEventFilter = true;
                String filterStr = options.remove();
                warnForWildcardExpansion("--events", filterStr);
                filter = addEventFilter(filterStr, filter);
            }
            if (acceptFilterOption(options, "--categories")) {
                if (foundCategoryFilter) {
                    throw new UserSyntaxException("use --categories category1,category2 to include multiple categories");
                }
                foundCategoryFilter = true;
                String filterStr = options.remove();
                warnForWildcardExpansion("--categories", filterStr);
                filter = addCategoryFilter(filterStr, filter);
            }
            if (optionCount == options.size()) {
                // No progress made
                checkCommonError(options, "--event", "--events");
                checkCommonError(options, "--category", "--categories");
                throw new UserSyntaxException("unknown option " + options.peek());
            }
            optionCount = options.size();
        }

        try (PrintWriter pw = new PrintWriter(System.out, false, Charset.forName("UTF-8"))) {
            PrettyWriter prettyWriter = new PrettyWriter(pw);
            prettyWriter.setShowIds(showIds);
            if (filter != null) {
                filter = addCache(filter, type -> type.getId());
            }

            List<Type> types = findTypes(file);
            Collections.sort(types, new TypeComparator());
            for (Type type : types) {
                if (filter != null) {
                    // If --events or --categories, only operate on events
                    if (Type.SUPER_TYPE_EVENT.equals(type.getSuperType())) {
                        EventType et = PrivateAccess.getInstance().newEventType((PlatformEventType) type);
                        if (filter.test(et)) {
                            prettyWriter.printType(type);
                        }
                    }
                } else {
                    prettyWriter.printType(type);
                }
            }
            prettyWriter.flush(true);
            pw.flush();
        }
    }

    private List<Type> findTypes(Path file) throws UserDataException {
        // Determine whether reading from recording file or reading from the JDK where
        // the jfr tool is located will be used
        if (file == null) {
            // Force initialization
            FlightRecorder.getFlightRecorder().getEventTypes();
            return TypeLibrary.getInstance().getTypes();
        }
        try (RecordingFile rf = new RecordingFile(file)) {
            return PRIVATE_ACCESS.readTypes(rf);
        } catch (IOException ioe) {
            couldNotReadError(file, ioe);
        }
        return null; // Can't reach
    }

    private Path getOptionalJFRInputFile(Deque<String> options) throws UserDataException {
        if (!options.isEmpty()) {
            String file = options.getLast();
            if (!file.startsWith("--")) {
                Path tmp = Paths.get(file).toAbsolutePath();
                if (tmp.toString().endsWith(".jfr")) {
                    ensureAccess(tmp);
                    options.removeLast();
                    return tmp;
                }
            }
        }
        return null;
    }

    private static boolean acceptSingleOption(Deque<String> options, String expected) {
        if (expected.equals(options.peek())) {
            options.remove();
            return true;
        }
        return false;
    }
}
