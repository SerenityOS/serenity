/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.tools;

import java.io.*;
import sun.jvm.hotspot.debugger.JVMDebugger;
import sun.jvm.hotspot.utilities.*;

public class JMap extends Tool {
    public JMap(int m) {
        mode = m;
    }

    public JMap() {
        this(MODE_PMAP);
    }

    public JMap(JVMDebugger d) {
        super(d);
    }

    protected boolean needsJavaPrefix() {
        return false;
    }

    public String getName() {
        return "jmap";
    }

    protected String getCommandFlags() {
        return "-heap|-heap:format=b[,gz=<1-9>][,file=<dumpfile>]|-heap:format=x[,file=<dumpfile>]|{-histo|-clstats|-finalizerinfo";
    }

    protected void printFlagsUsage() {
        System.out.println("    <no option>\tTo print same info as Solaris pmap.");
        System.out.println("    -heap\tTo print java heap summary.");
        System.out.println("    -heap:format=b[,gz=<1-9>][,file=<dumpfile>]  \tTo dump java heap in hprof binary format.");
        System.out.println("                                                 \tIf gz specified, the heap dump is written in gzipped format");
        System.out.println("                                                 \tusing the given compression level.");
        System.err.println("                                                 \t1 (recommended) is the fastest, 9 the strongest compression.");
        System.out.println("    -heap:format=x[,file=<dumpfile>]             \tTo dump java heap in GXL format.");
        System.out.println("                                                 \tPlease be aware that \"gz\" option is not valid for heap dump in GXL format.");
        System.out.println("    -histo\tTo print histogram of java object heap.");
        System.out.println("    -clstats\tTo print class loader statistics.");
        System.out.println("    -finalizerinfo\tTo print information on objects awaiting finalization.");
        super.printFlagsUsage();
    }

    public static final int MODE_HEAP_SUMMARY = 0;
    public static final int MODE_HISTOGRAM = 1;
    public static final int MODE_CLSTATS = 2;
    public static final int MODE_PMAP = 3;
    public static final int MODE_HEAP_GRAPH_HPROF_BIN = 4;
    public static final int MODE_HEAP_GRAPH_GXL = 5;
    public static final int MODE_FINALIZERINFO = 6;

    private static String dumpfile = "heap.bin";
    private static int gzLevel = 0;

    public void run() {
        Tool tool = null;
        switch (mode) {

        case MODE_HEAP_SUMMARY:
            tool = new HeapSummary();
            break;

        case MODE_HISTOGRAM:
            tool = new ObjectHistogram();
            break;

        case MODE_CLSTATS:
            tool = new ClassLoaderStats();
            break;

        case MODE_PMAP:
            tool = new PMap();
            break;

        case MODE_HEAP_GRAPH_HPROF_BIN:
            writeHeapHprofBin(dumpfile, gzLevel);
            return;

        case MODE_HEAP_GRAPH_GXL:
            writeHeapGXL(dumpfile);
            return;

        case MODE_FINALIZERINFO:
            tool = new FinalizerInfo();
            break;

        default:
            usage();
            break;
       }

       tool.setAgent(getAgent());
       tool.setDebugeeType(getDebugeeType());
       tool.run();
    }

    public static void main(String[] args) {
        int mode = MODE_PMAP;
        if (args.length > 1 ) {
            String modeFlag = args[0];
            boolean copyArgs = true;
            if (modeFlag.equals("-heap")) {
                mode = MODE_HEAP_SUMMARY;
            } else if (modeFlag.equals("-histo")) {
                mode = MODE_HISTOGRAM;
            } else if (modeFlag.equals("-clstats")) {
                mode = MODE_CLSTATS;
            } else if (modeFlag.equals("-finalizerinfo")) {
                mode = MODE_FINALIZERINFO;
            } else {
                int index = modeFlag.indexOf("-heap:");
                if (index != -1) {
                    String[] options = modeFlag.substring(6).split(",");
                    for (String option : options) {
                        String[] keyValue = option.split("=");
                        if (keyValue[0].equals("format")) {
                            if (keyValue[1].equals("b")) {
                                mode = MODE_HEAP_GRAPH_HPROF_BIN;
                            } else if (keyValue[1].equals("x")) {
                                mode = MODE_HEAP_GRAPH_GXL;
                            } else {
                                System.err.println("unknown heap format:" + keyValue[0]);

                                // Exit with error status
                                System.exit(1);
                            }
                        } else if (keyValue[0].equals("file")) {
                            if ((keyValue[1] == null) || keyValue[1].equals("")) {
                                System.err.println("File name must be set.");
                                System.exit(1);
                            }
                            dumpfile = keyValue[1];
                        } else if (keyValue[0].equals("gz")) {
                            if (mode == MODE_HEAP_GRAPH_GXL) {
                                System.err.println("\"gz\" option is not compatible with heap dump in GXL format");
                                System.exit(1);
                            }
                            if (keyValue.length == 1) {
                                System.err.println("Argument is expected for \"gz\"");
                                System.exit(1);
                            }
                            String level = keyValue[1];
                            try {
                                gzLevel = Integer.parseInt(level);
                            } catch (NumberFormatException e) {
                                System.err.println("\"gz\" option value not an integer ("+level+")");
                                System.exit(1);
                            }
                            if (gzLevel < 1 || gzLevel > 9) {
                                System.err.println("compression level out of range (1-9): " + level);
                                System.exit(1);
                            }
                        } else {
                            System.err.println("unknown option:" + keyValue[0]);

                            // Exit with error status
                            System.exit(1);
                        }
                    }
                } else {
                    copyArgs = false;
                }
            }

            if (copyArgs) {
                String[] newArgs = new String[args.length - 1];
                for (int i = 0; i < newArgs.length; i++) {
                    newArgs[i] = args[i + 1];
                }
                args = newArgs;
            }
        }

        JMap jmap = new JMap(mode);
        jmap.execute(args);
    }

    public boolean writeHeapHprofBin(String fileName, int gzLevel) {
        try {
            HeapGraphWriter hgw;
            if (gzLevel == 0) {
                hgw = new HeapHprofBinWriter();
            } else if (gzLevel >=1 && gzLevel <= 9) {
                hgw = new HeapHprofBinWriter(gzLevel);
            } else {
                System.err.println("Illegal compression level: " + gzLevel);
                return false;
            }
            hgw.write(fileName);
            System.out.println("heap written to " + fileName);
            return true;
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    public boolean writeHeapHprofBin() {
        return writeHeapHprofBin("heap.bin", -1);
    }

    private boolean writeHeapGXL(String fileName) {
        try {
            HeapGraphWriter hgw = new HeapGXLWriter();
            hgw.write(fileName);
            System.out.println("heap written to " + fileName);
            return true;
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    public boolean writeHeapGXL() {
        return writeHeapGXL("heap.xml");
    }

    private int mode;
}
