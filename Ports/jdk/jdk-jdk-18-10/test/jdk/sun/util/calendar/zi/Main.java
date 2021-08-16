/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Main class for the javazic time zone data compiler.
 *
 * @since 1.4
 */
public class Main {

    private static boolean verbose = false;
    static boolean outputDoc = false;

    private List<String> ziFiles = new ArrayList<String>();
    private static String zoneNamesFile = null;
    private static String versionName = "unknown";
    private static String outputDir = "zoneinfo";
    private static String mapFile = null;

    /**
     * Parses the specified arguments and sets up the variables.
     * @param argv the arguments
     */
    void processArgs(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-h")) {
                usage();
                System.exit(0);
            } else if (arg.equals("-d")) {
                outputDir = argv[++i];
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-V")) {
                versionName = argv[++i];
            } else if (arg.equals("-doc")) {
                outputDoc = true;
            } else if (arg.equals("-map")) {
                outputDoc = true;
                mapFile = argv[++i];
            } else if (arg.equals("-f")) {
                zoneNamesFile = argv[++i];
            } else if (arg.equals("-S")) {
                try {
                    Zoneinfo.setYear(Integer.parseInt(argv[++i]));
                } catch (Exception e) {
                    error("invalid year: " + argv[i]);
                    usage();
                    System.exit(1);
                }
            } else {
                boolean isStartYear = arg.equals("-s");
                if (isStartYear || arg.equals("-e")) {
                    try {
                        int year = Integer.parseInt(argv[++i]);
                        if (isStartYear) {
                            Zoneinfo.setStartYear(year);
                        } else {
                            Zoneinfo.setEndYear(year);
                        }
                    } catch (Exception e) {
                        error("invalid year: " + argv[i]);
                        usage();
                        System.exit(1);
                    }
                } else {
                    // the rest of args are zoneinfo source files
                    while (i < argv.length) {
                        ziFiles.add(argv[i++]);
                    }
                }
            }
        }
    }

    /**
     * Parses zoneinfo source files
     */
    int compile() {
        int nFiles = ziFiles.size();
        int status = 0;
        Mappings maps = new Mappings();
        BackEnd backend = BackEnd.getBackEnd();

        for (int i = 0; i < nFiles; i++) {
            Zoneinfo frontend = Zoneinfo.parse(ziFiles.get(i));

            for (String key : frontend.getZones().keySet()) {
                info(key);

                Timezone tz = frontend.phase2(key);
                status |= backend.processZoneinfo(tz);
            }

            maps.add(frontend);
        }

        // special code for dealing with the conflicting name "MET"
        Zone.addMET();

        maps.resolve();

        status |= backend.generateSrc(maps);

        return status;
    }

    public static void main(String[] argv) {
        Main zic = new Main();

        /*
         * Parse args
         */
        zic.processArgs(argv);

        /*
         * Read target zone names
         */
        if (zoneNamesFile != null) {
            Zone.readZoneNames(zoneNamesFile);
        }

        zic.compile();
    }

    void usage() {
        System.err.println("Usage: javazic [options] file...\n"+
                           "         -f namefile  file containing zone names\n"+
                           "                      to be generated (ie, generating subset)\n"+
                           "         -d dir       output directory\n"+
                           "         -v           verbose\n"+
                           "         -V datavers  specifies the tzdata version string\n"+
                           "                      (eg, \"tzdata2000g\")"+
                           "         -S year      output only SimleTimeZone data of that year\n"+
                           "         -s year      start year (default: 1900)\n"+
                           "         -e year      end year (default: 2037)\n"+
                           "         -doc         generates HTML documents\n"+
                           "         -map mapfile generates HTML documents with map information\n"+
                           "         file...      zoneinfo source file(s)");
    }

    /**
     * @return the output directory path name
     */
    static String getOutputDir() {
        return outputDir;
    }

    /**
     * @return the map file's path and name
     */
    static String getMapFile() {
        return mapFile;
    }

    /**
     * Returns the time zone data version string specified by the -V
     * option. If it is not specified, "unknown" is returned.
     * @return the time zone data version string
     */
    static String getVersionName() {
        return versionName;
    }

    /**
     * Prints out the specified fatal error message and calls {@link
     * java.lang.System#exit System.exit(1)}.
     * @param msg the fatal error message
     */
    static void panic(String msg) {
        printMessage("fatal error", msg);
        System.exit(1);
    }

    /**
     * Prints out the specified error message.
     * @param msg the error message
     */
    static void error(String msg) {
        printMessage("error", msg);
    }

    /**
     * Prints out the specified warning message.
     * @param msg the warning message
     */
    static void warning(String msg) {
        printMessage("warning", msg);
    }

    /**
     * Prints out the informative message.
     * @param msg the informative message
     */
    static void info(String msg) {
        if (verbose) {
            printMessage(null, msg);
        }
    }

    private static void printMessage(String type, String msg) {
        if (type != null) {
            type += ": ";
        } else {
            type = "";
        }
        System.err.println("javazic: " + type + msg);
    }
}
