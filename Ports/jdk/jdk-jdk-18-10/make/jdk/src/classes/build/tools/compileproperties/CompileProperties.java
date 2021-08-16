/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.compileproperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/** Translates a .properties file into a .java file containing the
 *  definition of a java.util.Properties subclass which can then be
 *  compiled with javac. <P>
 *
 *  Usage: java -jar compileproperties.jar [path to .properties file] [path to .java file to be output] [super class]
 *
 *  Infers the package by looking at the common suffix of the two
 *  inputs, eliminating "classes" from it.
 *
 * @author Scott Violet
 * @author Kenneth Russell
 */

public class CompileProperties {
    private static final String FORMAT =
            "{0}" +
            "import java.util.ListResourceBundle;\n\n" +
            "public final class {1} extends {2} '{'\n" +
            "    protected final Object[][] getContents() '{'\n" +
            "        return new Object[][] '{'\n" +
            "{3}" +
            "        };\n" +
            "    }\n" +
            "}\n";


    // This comes from Properties
    private static final char[] hexDigit = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

    // Note: different from that in Properties
    private static final String specialSaveChars = "\"";

    // This comes from Properties
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    private static void error(String msg, Exception e) {
        System.err.println("ERROR: compileproperties: " + msg);
        if ( e != null ) {
            System.err.println("EXCEPTION: " + e.toString());
            e.printStackTrace();
        }
    }

    private static String propfiles[];
    private static String outfiles[] ;
    private static String supers[]   ;
    private static int compileCount = 0;
    private static boolean quiet = false;

    private static boolean parseOptions(String args[]) {
        boolean ok = true;
        if ( compileCount > 0 ) {
            String new_propfiles[] = new String[compileCount + args.length];
            String new_outfiles[]  = new String[compileCount + args.length];
            String new_supers[]    = new String[compileCount + args.length];
            System.arraycopy(propfiles, 0, new_propfiles, 0, compileCount);
            System.arraycopy(outfiles, 0, new_outfiles, 0, compileCount);
            System.arraycopy(supers, 0, new_supers, 0, compileCount);
            propfiles = new_propfiles;
            outfiles  = new_outfiles;
            supers    = new_supers;
        } else {
            propfiles = new String[args.length];
            outfiles  = new String[args.length];
            supers    = new String[args.length];
        }
        for ( int i = 0; i < args.length ; i++ ) {
            if ( "-compile".equals(args[i]) && i+3 < args.length ) {
                propfiles[compileCount] = args[++i];
                outfiles[compileCount]  = args[++i];
                supers[compileCount]    = args[++i];
                compileCount++;
            } else if ( args[i].charAt(0) == '@') {
                String filename = args[i].substring(1);
                FileInputStream finput = null;
                byte contents[] = null;
                try {
                    finput = new FileInputStream(filename);
                    int byteCount = finput.available();
                    if ( byteCount <= 0 ) {
                        error("The @file is empty", null);
                        ok = false;
                    } else {
                        contents = new byte[byteCount];
                        int bytesRead = finput.read(contents);
                        if ( byteCount != bytesRead ) {
                            error("Cannot read all of @file", null);
                            ok = false;
                        }
                    }
                } catch ( IOException e ) {
                    error("cannot open " + filename, e);
                    ok = false;
                }
                if ( finput != null ) {
                    try {
                        finput.close();
                    } catch ( IOException e ) {
                        ok = false;
                        error("cannot close " + filename, e);
                    }
                }
                if ( ok && contents != null ) {
                    String tokens[] = (new String(contents)).split("\\s+");
                    if ( tokens.length > 0 ) {
                        ok = parseOptions(tokens);
                    }
                }
                if ( !ok ) {
                    break;
                }
            } else {
                error("argument error", null);
                ok = false;
            }
        }
        return ok;
    }

    public static void main(String[] args) {
        boolean ok = true;
        if (args.length >= 1 && args[0].equals("-quiet"))
        {
            quiet = true;
            String[] newargs = new String[args.length-1];
            System.arraycopy(args, 1, newargs, 0, newargs.length);
            args = newargs;
        }
        /* Original usage */
        if (args.length == 2 && args[0].charAt(0) != '-' ) {
            ok = createFile(args[0], args[1], "ListResourceBundle");
        } else if (args.length == 3) {
            ok = createFile(args[0], args[1], args[2]);
        } else if (args.length == 0) {
            usage();
            ok = false;
        } else {
            /* New batch usage */
            ok = parseOptions(args);
            if ( ok && compileCount == 0 ) {
                error("options parsed but no files to compile", null);
                ok = false;
            }
            /* Need at least one file. */
            if ( !ok ) {
                usage();
            } else {
                /* Process files */
                for ( int i = 0; i < compileCount && ok ; i++ ) {
                    ok = createFile(propfiles[i], outfiles[i], supers[i]);
                }
            }
        }
        if ( !ok ) {
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("usage:");
        System.err.println("    java -jar compileproperties.jar path_to_properties_file path_to_java_output_file [super_class]");
        System.err.println("      -OR-");
        System.err.println("    java -jar compileproperties.jar {-compile path_to_properties_file path_to_java_output_file super_class} -or- @filename");
        System.err.println("");
        System.err.println("Example:");
        System.err.println("    java -jar compileproperties.jar -compile test.properties test.java ListResourceBundle");
        System.err.println("    java -jar compileproperties.jar @option_file");
        System.err.println("option_file contains: -compile test.properties test.java ListResourceBundle");
    }

    private static boolean createFile(String propertiesPath, String outputPath,
            String superClass) {
        boolean ok = true;
        if (!quiet) {
            System.out.println("parsing: " + propertiesPath);
        }
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(propertiesPath));
        } catch ( FileNotFoundException e ) {
            ok = false;
            error("Cannot find file " + propertiesPath, e);
        } catch ( IOException e ) {
            ok = false;
            error("IO error on file " + propertiesPath, e);
        }
        if ( ok ) {
            String packageName = inferPackageName(propertiesPath, outputPath);
            if (!quiet) {
                System.out.println("inferred package name: " + packageName);
            }
            List<String> sortedKeys = new ArrayList<>();
            for ( Object key : p.keySet() ) {
                sortedKeys.add((String)key);
            }
            Collections.sort(sortedKeys);

            StringBuffer data = new StringBuffer();

            for (String key : sortedKeys) {
                data.append("            { \"" + escape(key) + "\", \"" +
                        escape((String)p.get(key)) + "\" },\n");
            }

            // Get class name from java filename, not the properties filename.
            //   (zh_TW properties might be used to create zh_HK files)
            File file = new File(outputPath);
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            String className;
            if (dotIndex == -1) {
                className = name;
            } else {
                className = name.substring(0, dotIndex);
            }

            String packageString = "";
            if (packageName != null && !packageName.equals("")) {
                packageString = "package " + packageName + ";\n\n";
            }

            Writer writer = null;
            try {
                writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(outputPath), "8859_1"));
                MessageFormat format = new MessageFormat(FORMAT);
                writer.write(format.format(new Object[] { packageString, className, superClass, data }));
            } catch ( IOException e ) {
                ok = false;
                error("IO error writing to file " + outputPath, e);
            }
            if ( writer != null ) {
                try {
                    writer.flush();
                } catch ( IOException e ) {
                    ok = false;
                    error("IO error flush " + outputPath, e);
                }
                try {
                    writer.close();
                } catch ( IOException e ) {
                    ok = false;
                    error("IO error close " + outputPath, e);
                }
            }
            if (!quiet) {
                System.out.println("wrote: " + outputPath);
            }
        }
        return ok;
    }

    private static String escape(String theString) {
        // This is taken from Properties.saveConvert with changes for Java strings
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len*2);

        for(int x=0; x<len; x++) {
            char aChar = theString.charAt(x);
            switch(aChar) {
                case '\\':outBuffer.append('\\'); outBuffer.append('\\');
                break;
                case '\t':outBuffer.append('\\'); outBuffer.append('t');
                break;
                case '\n':outBuffer.append('\\'); outBuffer.append('n');
                break;
                case '\r':outBuffer.append('\\'); outBuffer.append('r');
                break;
                case '\f':outBuffer.append('\\'); outBuffer.append('f');
                break;
                default:
                    if ((aChar < 0x0020) || (aChar > 0x007e)) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >>  8) & 0xF));
                        outBuffer.append(toHex((aChar >>  4) & 0xF));
                        outBuffer.append(toHex( aChar        & 0xF));
                    } else {
                        if (specialSaveChars.indexOf(aChar) != -1) {
                            outBuffer.append('\\');
                        }
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static String inferPackageName(String inputPath, String outputPath) {
        // Normalize file names
        inputPath  = new File(inputPath).getPath();
        outputPath = new File(outputPath).getPath();
        // Split into components
        String sep;
        if (File.separatorChar == '\\') {
            sep = "\\\\";
        } else {
            sep = File.separator;
        }
        String[] inputs  = inputPath.split(sep);
        String[] outputs = outputPath.split(sep);
        // Match common names, eliminating first "classes" entry from
        // each if present
        int inStart  = 0;
        int inEnd    = inputs.length - 2;
        int outEnd   = outputs.length - 2;
        int i = inEnd;
        int j = outEnd;
        while (i >= 0 && j >= 0) {
            if (!inputs[i].equals(outputs[j]) ||
                    (inputs[i].equals("gensrc") && inputs[j].equals("gensrc"))) {
                ++i;
                ++j;
                break;
            }
            --i;
            --j;
        }
        String result;
        if (i < 0 || j < 0 || i >= inEnd || j >= outEnd) {
            result = "";
        } else {
            if (inputs[i].equals("classes") && outputs[j].equals("classes")) {
                ++i;
            }
            if (i > 0 && inputs[i-1].equals("modules")) {
                ++i;
            }
            inStart = i;
            StringBuffer buf = new StringBuffer();
            for (i = inStart; i <= inEnd; i++) {
                buf.append(inputs[i]);
                if (i < inEnd) {
                    buf.append('.');
                }
            }
            result = buf.toString();
        }
        return result;
    }
}
