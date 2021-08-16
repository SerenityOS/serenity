/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.tools.sjavac.comp.CompilationService;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The copy file transform simply copies a matching file from -src to -d .
 * Such files are typically images, xml documents and other data files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CopyFile implements Transformer {

    public void setExtra(String e) {
    }

    public void setExtra(Options a) {
    }

    public boolean transform(CompilationService compilationService,
                             Map<String,Set<URI>> pkgSrcs,
                             Set<URI> visibleSrcs,
                             Map<String,Set<String>> oldPackageDependents,
                             URI destRoot,
                             Map<String,Set<URI>>    packageArtifacts,
                             Map<String,Map<String, Set<String>>> packageDependencies,
                             Map<String,Map<String, Set<String>>> packageCpDependencies,
                             Map<String, PubApi> packagePubapis,
                             Map<String, PubApi> dependencyPubapis,
                             int debugLevel,
                             boolean incremental,
                             int numCores)
    {
        boolean rc = true;
        String dest_filename;
        File dest;

        for (String pkgName : pkgSrcs.keySet()) {
            String pkgNameF = Util.toFileSystemPath(pkgName);
            for (URI u : pkgSrcs.get(pkgName)) {
                File src = new File(u);
                File destDir;
                destDir = new File(destRoot.getPath()+File.separator+pkgNameF);
                dest_filename = destRoot.getPath()+File.separator+pkgNameF+File.separator+src.getName();
                dest = new File(dest_filename);

                if (!destDir.isDirectory()) {
                    if (!destDir.mkdirs()) {
                       Log.error("Error: The copier could not create the directory "+
                                           destDir.getPath());
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
                    // A copied file exists, and its timestamp is newer than the source.
                    continue;
                }

                Log.info("Copying "+pkgNameF+File.separator+src.getName());

                try {
                    Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                catch(IOException e){
                    Log.error("Could not copy the file "+src.getPath()+" to "+dest.getPath());
                    rc = false;
                }
            }
        }
        return rc;
    }
}
