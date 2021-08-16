/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.tools.sjavac.comp.CompilationService;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * Compile properties transform a properties file into a Java source file.
 * Java has built in support for reading properties from either a text file
 * in the source or a compiled java source file.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CompileProperties implements Transformer {
    // Any extra information passed from the command line, for example if:
    // -tr .proppp=com.sun.tools.javac.smart.CompileProperties,sun.util.resources.LocaleNamesBundle
    // then extra will be "sun.util.resources.LocaleNamesBundle"
    String extra;

    public void setExtra(String e) {
        extra = e;
    }

    public void setExtra(Options a) {
    }

    public boolean transform(CompilationService compilationService,
                             Map<String,Set<URI>> pkgSrcs,
                             Set<URI>             visibleSrcs,
                             Map<String,Set<String>> oldPackageDependents,
                             URI destRoot,
                             Map<String,Set<URI>>    packageArtifacts,
                             Map<String,Map<String, Set<String>>> packageDependencies,
                             Map<String,Map<String, Set<String>>> packageCpDependencies,
                             Map<String, PubApi> packagePublicApis,
                             Map<String, PubApi> dependencyPublicApis,
                             int debugLevel,
                             boolean incremental,
                             int numCores) {
        boolean rc = true;
        for (String pkgName : pkgSrcs.keySet()) {
            String pkgNameF = Util.toFileSystemPath(pkgName);
            for (URI u : pkgSrcs.get(pkgName)) {
                File src = new File(u);
                boolean r = compile(pkgName, pkgNameF, src, new File(destRoot), debugLevel,
                                    packageArtifacts);
                if (r == false) {
                    rc = false;
                }
            }
        }
        return rc;
    }

    boolean compile(String pkgName, String pkgNameF, File src, File destRoot, int debugLevel,
                    Map<String,Set<URI>> packageArtifacts)
    {
        String superClass = "java.util.ListResourceBundle";

        if (extra != null) {
            superClass = extra;
        }
        // Load the properties file.
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(src));
        } catch (IOException e) {
            Log.error("Error reading file "+src.getPath());
            return false;
        }

        // Calculate the name of the Java source file to be generated.
        int dp = src.getName().lastIndexOf(".");
        String classname = src.getName().substring(0,dp);

        // Sort the properties in increasing key order.
        List<String> sortedKeys = new ArrayList<>();
        for (Object key : p.keySet()) {
            sortedKeys.add((String)key);
        }
        Collections.sort(sortedKeys);
        Iterator<String> keys = sortedKeys.iterator();

        // Collect the properties into a string buffer.
        StringBuilder data = new StringBuilder();
        while (keys.hasNext()) {
            String key = keys.next();
            data.append("            { \"" + escape(key) + "\", \"" +
                        escape((String)p.get(key)) + "\" },\n");
        }

        // Create dest file name. It is derived from the properties file name.
        String destFilename = destRoot.getPath()+File.separator+pkgNameF+File.separator+classname+".java";
        File dest = new File(destFilename);

        // Make sure the dest directories exist.
        if (!dest.getParentFile().isDirectory()) {
            if (!dest.getParentFile().mkdirs()) {
                Log.error("Could not create the directory "+dest.getParentFile().getPath());
                return false;
            }
        }

        Set<URI> as = packageArtifacts.get(pkgName);
        if (as == null) {
            as = new HashSet<>();
            packageArtifacts.put(pkgName, as);
        }
        as.add(dest.toURI());

        if (dest.exists() && dest.lastModified() > src.lastModified()) {
            // A generated file exists, and its timestamp is newer than the source.
            // Assume that we do not need to regenerate the dest file!
            // Thus we are done.
            return true;
        }

        String packageString = "package " + pkgNameF.replace(File.separatorChar,'.') + ";\n\n";

        Log.info("Compiling property file "+pkgNameF+File.separator+src.getName());
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest)))) {
            MessageFormat format = new MessageFormat(FORMAT);
            writer.write(format.format(new Object[] { packageString, classname, superClass, data }));
        } catch ( IOException e ) {
            Log.error("Could not write file "+dest.getPath());
            return false;
        }
        return true;
    }

    private static final String FORMAT =
            "{0}" +
            "public final class {1} extends {2} '{'\n" +
            "    protected final Object[][] getContents() '{'\n" +
            "        return new Object[][] '{'\n" +
            "{3}" +
            "        };\n" +
            "    }\n" +
            "}\n";

    public static String escape(String theString) {
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len*2);

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
                        if (aChar == '"') {
                            outBuffer.append('\\');
                        }
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    private static final char[] hexDigit = {
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
}
