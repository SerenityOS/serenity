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
 * Copyright (c) 2009-2012, Stephen Colebourne & Michael Nascimento Santos
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
package build.tools.tzdb;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A compiler that reads a set of TZDB time-zone files and builds a single
 * combined TZDB data file.
 *
 * @since 1.8
 */
public final class TzdbZoneRulesCompiler {

    public static void main(String[] args) {
        new TzdbZoneRulesCompiler().compile(args);
    }

    private void compile(String[] args) {
        if (args.length < 2) {
            outputHelp();
            return;
        }
        Path srcDir = null;
        Path dstFile = null;
        String version = null;
        // parse args/options
        int i;
        for (i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                break;
            }
            if ("-srcdir".equals(arg)) {
                if (srcDir == null && ++i < args.length) {
                    srcDir = Paths.get(args[i]);
                    continue;
                }
            } else if ("-dstfile".equals(arg)) {
                if (dstFile == null && ++i < args.length) {
                    dstFile = Paths.get(args[i]);
                    continue;
                }
            } else if ("-verbose".equals(arg)) {
                if (!verbose) {
                    verbose = true;
                    continue;
                }
            } else if (!"-help".equals(arg)) {
                System.out.println("Unrecognised option: " + arg);
            }
            outputHelp();
            return;
        }
        // check source directory
        if (srcDir == null) {
            System.err.println("Source directory must be specified using -srcdir");
            System.exit(1);
        }
        if (!Files.isDirectory(srcDir)) {
            System.err.println("Source does not exist or is not a directory: " + srcDir);
            System.exit(1);
        }
        // parse source file names
        if (i == args.length) {
            i = 0;
            args = new String[] {"africa", "antarctica", "asia", "australasia", "europe",
                                 "northamerica","southamerica", "backward", "etcetera" };
            System.out.println("Source filenames not specified, using default set ( ");
            for (String name : args) {
                System.out.printf(name + " ");
            }
            System.out.println(")");
        }
        // source files in this directory
        List<Path> srcFiles = new ArrayList<>();
        for (; i < args.length; i++) {
            Path file = srcDir.resolve(args[i]);
            if (Files.exists(file)) {
                srcFiles.add(file);
            } else {
                System.err.println("Source directory does not contain source file: " + args[i]);
                System.exit(1);
            }
        }
        // check destination file
        if (dstFile == null) {
            dstFile = srcDir.resolve("tzdb.dat");
        } else {
            Path parent = dstFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                System.err.println("Destination directory does not exist: " + parent);
                System.exit(1);
            }
        }
        try {
            // get tzdb source version
            Matcher m = Pattern.compile("tzdata(?<ver>[0-9]{4}[A-z])")
                               .matcher(new String(Files.readAllBytes(srcDir.resolve("VERSION")),
                                                   "ISO-8859-1"));
            if (m.find()) {
                version = m.group("ver");
            } else {
                System.exit(1);
                System.err.println("Source directory does not contain file: VERSION");
            }

            // load source files
            printVerbose("Compiling TZDB version " + version);
            TzdbZoneRulesProvider provider = new TzdbZoneRulesProvider(srcFiles);

            // build zone rules
            printVerbose("Building rules");

            // Build the rules, zones and links into real zones.
            SortedMap<String, ZoneRules> builtZones = new TreeMap<>();

            // build zones
            for (String zoneId : provider.getZoneIds()) {
                printVerbose("Building zone " + zoneId);
                builtZones.put(zoneId, provider.getZoneRules(zoneId));
            }

            // build aliases
            Map<String, String> links = provider.getAliasMap();
            for (String aliasId : links.keySet()) {
                String realId = links.get(aliasId);
                printVerbose("Linking alias " + aliasId + " to " + realId);
                ZoneRules realRules = builtZones.get(realId);
                if (realRules == null) {
                    realId = links.get(realId);  // try again (handle alias liked to alias)
                    printVerbose("Relinking alias " + aliasId + " to " + realId);
                    realRules = builtZones.get(realId);
                    if (realRules == null) {
                        throw new IllegalArgumentException("Alias '" + aliasId + "' links to invalid zone '" + realId);
                    }
                    links.put(aliasId, realId);
                }
                builtZones.put(aliasId, realRules);
            }

            // output to file
            printVerbose("Outputting tzdb file: " + dstFile);
            outputFile(dstFile, version, builtZones, links);
        } catch (Exception ex) {
            System.out.println("Failed: " + ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Output usage text for the command line.
     */
    private static void outputHelp() {
        System.out.println("Usage: TzdbZoneRulesCompiler <options> <tzdb source filenames>");
        System.out.println("where options include:");
        System.out.println("   -srcdir  <directory>  Where to find tzdb source directory (required)");
        System.out.println("   -dstfile <file>       Where to output generated file (default srcdir/tzdb.dat)");
        System.out.println("   -help                 Print this usage message");
        System.out.println("   -verbose              Output verbose information during compilation");
        System.out.println(" The source directory must contain the unpacked tzdb files, such as asia or europe");
    }

    /**
     * Outputs the file.
     */
    private void outputFile(Path dstFile, String version,
                            SortedMap<String, ZoneRules> builtZones,
                            Map<String, String> links) {
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(dstFile))) {
            // file version
            out.writeByte(1);
            // group
            out.writeUTF("TZDB");
            // versions
            out.writeShort(1);
            out.writeUTF(version);
            // regions
            String[] regionArray = builtZones.keySet().toArray(new String[builtZones.size()]);
            out.writeShort(regionArray.length);
            for (String regionId : regionArray) {
                out.writeUTF(regionId);
            }
            // rules  -- remove the dup
            List<ZoneRules> rulesList = builtZones.values().stream()
                .distinct()
                .collect(Collectors.toList());
            out.writeShort(rulesList.size());
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            for (ZoneRules rules : rulesList) {
                baos.reset();
                DataOutputStream dataos = new DataOutputStream(baos);
                Ser.write(rules, dataos);
                dataos.close();
                byte[] bytes = baos.toByteArray();
                out.writeShort(bytes.length);
                out.write(bytes);
            }
            // link version-region-rules
            out.writeShort(builtZones.size());
            for (Map.Entry<String, ZoneRules> entry : builtZones.entrySet()) {
                 int regionIndex = Arrays.binarySearch(regionArray, entry.getKey());
                 int rulesIndex = rulesList.indexOf(entry.getValue());
                 out.writeShort(regionIndex);
                 out.writeShort(rulesIndex);
            }
            // alias-region
            out.writeShort(links.size());
            for (Map.Entry<String, String> entry : links.entrySet()) {
                 int aliasIndex = Arrays.binarySearch(regionArray, entry.getKey());
                 int regionIndex = Arrays.binarySearch(regionArray, entry.getValue());
                 out.writeShort(aliasIndex);
                 out.writeShort(regionIndex);
            }
            out.flush();
        } catch (Exception ex) {
            System.out.println("Failed: " + ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /** Whether to output verbose messages. */
    private boolean verbose;

    /**
     * private contructor
     */
    private TzdbZoneRulesCompiler() {}

    /**
     * Prints a verbose message.
     *
     * @param message  the message, not null
     */
    private void printVerbose(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
}
