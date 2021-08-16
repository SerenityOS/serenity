/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.sun.tools.sjavac.comp.CompilationService;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The clean properties transform should not be necessary.
 * Eventually we will cleanup the property file sources in the OpenJDK instead.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CleanProperties implements Transformer {
    public void setExtra(String e) {
        // Any extra information is ignored for clean properties.
    }

    public void setExtra(Options a) {
        // Any extra information is ignored for clean properties.
    }

    public boolean transform(CompilationService sjavac,
                             Map<String,Set<URI>> pkgSrcs,
                             Set<URI>             visibleSrcs,
                             Map<String,Set<String>> oldPackageDependencies,
                             URI destRoot,
                             Map<String,Set<URI>>    packageArtifacts,
                             Map<String, Map<String, Set<String>>> packageDependencies,
                             Map<String, Map<String, Set<String>>> packageCpDependencies,
                             Map<String, PubApi> packagePublicApis,
                             Map<String, PubApi> dependencyPublicApis,
                             int debugLevel,
                             boolean incremental,
                             int numCores) {
        boolean rc = true;
        for (String pkgName : pkgSrcs.keySet()) {
            String pkgNameF = pkgName.replace('.',File.separatorChar);
            for (URI u : pkgSrcs.get(pkgName)) {
                File src = new File(u);
                boolean r = clean(pkgName, pkgNameF, src, new File(destRoot), debugLevel,
                                  packageArtifacts);
                if (r == false) {
                    rc = false;
                }
            }
        }
        return rc;
    }

    boolean clean(String pkgName,
                  String pkgNameF,
                  File src,
                  File destRoot,
                  int debugLevel,
                  Map<String,Set<URI>> packageArtifacts) {
        // Load the properties file.
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(src));
        } catch (IOException e) {
            Log.error("Error reading file "+src.getPath());
            return false;
        }

        // Sort the properties in increasing key order.
        List<String> sortedKeys = new ArrayList<>();
        for (Object key : p.keySet()) {
            sortedKeys.add((String)key);
        }
        Collections.sort(sortedKeys);

        // Collect the properties into a string buffer.
        StringBuilder data = new StringBuilder();
        for (String key : sortedKeys) {
            data.append(CompileProperties.escape(key))
                .append(":")
                .append(CompileProperties.escape((String) p.get(key)))
                .append("\n");
        }

        String destFilename = destRoot.getPath()+File.separator+pkgNameF+File.separator+src.getName();
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
            // A cleaned property file exists, and its timestamp is newer than the source.
            // Assume that we do not need to clean!
            // Thus we are done.
            return true;
        }

        Log.info("Cleaning property file "+pkgNameF+File.separator+src.getName());
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest)))) {
            writer.write(data.toString());
        } catch ( IOException e ) {
            Log.error("Could not write file "+dest.getPath());
            return false;
        }
        return true;
    }
}
