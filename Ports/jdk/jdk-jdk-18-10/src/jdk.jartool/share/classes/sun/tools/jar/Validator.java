/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.jar.JarFile.MANIFEST_NAME;
import static sun.tools.jar.Main.VERSIONS_DIR;
import static sun.tools.jar.Main.VERSIONS_DIR_LENGTH;
import static sun.tools.jar.Main.MODULE_INFO;
import static sun.tools.jar.Main.getMsg;
import static sun.tools.jar.Main.formatMsg;
import static sun.tools.jar.Main.formatMsg2;
import static sun.tools.jar.Main.toBinaryName;

final class Validator {

    private final Map<String,FingerPrint> classes = new HashMap<>();
    private final Main main;
    private final ZipFile zf;
    private boolean isValid = true;
    private Set<String> concealedPkgs = Collections.emptySet();
    private ModuleDescriptor md;
    private String mdName;

    private Validator(Main main, ZipFile zf) {
        this.main = main;
        this.zf = zf;
        checkModuleDescriptor(MODULE_INFO);
    }

    static boolean validate(Main main, ZipFile zf) throws IOException {
        return new Validator(main, zf).validate();
    }

    private boolean validate() {
        try {
            zf.stream()
              .filter(e -> e.getName().endsWith(".class"))
              .map(this::getFingerPrint)
              .filter(FingerPrint::isClass)    // skip any non-class entry
              .collect(Collectors.groupingBy(
                      FingerPrint::mrversion,
                      TreeMap::new,
                      Collectors.toMap(FingerPrint::className,
                                       Function.identity(),
                                       this::sameNameFingerPrint)))
              .forEach((version, entries) -> {
                      if (version == 0)
                          validateBase(entries);
                      else
                          validateVersioned(entries);
                  });
        } catch (InvalidJarException e) {
            errorAndInvalid(e.getMessage());
        }
        return isValid;
    }

    static class InvalidJarException extends RuntimeException {
        private static final long serialVersionUID = -3642329147299217726L;
        InvalidJarException(String msg) {
            super(msg);
        }
    }

    private FingerPrint sameNameFingerPrint(FingerPrint fp1, FingerPrint fp2) {
        checkClassName(fp1);
        checkClassName(fp2);
        // entries/classes with same name, return fp2 for now ?
        return fp2;
    }

    private FingerPrint getFingerPrint(ZipEntry ze) {
        // figure out the version and basename from the ZipEntry
        String ename = ze.getName();
        String bname = ename;
        int version = 0;

        if (ename.startsWith(VERSIONS_DIR)) {
            int n = ename.indexOf("/", VERSIONS_DIR_LENGTH);
            if (n == -1) {
                throw new InvalidJarException(
                    formatMsg("error.validator.version.notnumber", ename));
            }
            try {
                version = Integer.parseInt(ename, VERSIONS_DIR_LENGTH, n, 10);
            } catch (NumberFormatException x) {
                throw new InvalidJarException(
                    formatMsg("error.validator.version.notnumber", ename));
            }
            if (n == ename.length()) {
                throw new InvalidJarException(
                    formatMsg("error.validator.entryname.tooshort", ename));
            }
            bname = ename.substring(n + 1);
        }

        // return the cooresponding fingerprint entry
        try (InputStream is = zf.getInputStream(ze)) {
            return new FingerPrint(bname, ename, version, is.readAllBytes());
        } catch (IOException x) {
           throw new InvalidJarException(x.getMessage());
        }
    }

    /*
     *  Validates (a) if there is any isolated nested class, and (b) if the
     *  class name in class file (by asm) matches the entry's basename.
     */
    public void validateBase(Map<String, FingerPrint> fps) {
        fps.values().forEach( fp -> {
            if (!checkClassName(fp)) {
                return;
            }
            if (fp.isNestedClass()) {
                checkNestedClass(fp, fps);
            }
            classes.put(fp.className(), fp);
        });
    }

    public void validateVersioned(Map<String, FingerPrint> fps) {

        fps.values().forEach( fp -> {

            // validate the versioned module-info
            if (MODULE_INFO.equals(fp.basename())) {
                checkModuleDescriptor(fp.entryName());
                return;
            }
            // process a versioned entry, look for previous entry with same name
            FingerPrint matchFp = classes.get(fp.className());
            if (matchFp == null) {
                // no match found
                if (fp.isNestedClass()) {
                    checkNestedClass(fp, fps);
                    return;
                }
                if (fp.isPublicClass()) {
                    if (!isConcealed(fp.className())) {
                        errorAndInvalid(formatMsg("error.validator.new.public.class",
                                                  fp.entryName()));
                        return;
                     }
                     // entry is a public class entry in a concealed package
                     warn(formatMsg("warn.validator.concealed.public.class",
                                   fp.entryName()));
                }
                classes.put(fp.className(), fp);
                return;
            }

            // are the two classes/resources identical?
            if (fp.isIdentical(matchFp)) {
                warn(formatMsg("warn.validator.identical.entry", fp.entryName()));
                return;    // it's okay, just takes up room
            }

            // ok, not identical, check for compatible class version and api
            if (fp.isNestedClass()) {
                checkNestedClass(fp, fps);
                return;    // fall through, need check nested public class??
            }
            if (!fp.isCompatibleVersion(matchFp)) {
                errorAndInvalid(formatMsg("error.validator.incompatible.class.version",
                                          fp.entryName()));
                return;
            }
            if (!fp.isSameAPI(matchFp)) {
                errorAndInvalid(formatMsg("error.validator.different.api",
                                          fp.entryName()));
                return;
            }
            if (!checkClassName(fp)) {
                return;
            }
            classes.put(fp.className(), fp);

            return;
        });
    }

    /*
     * Checks whether or not the given versioned module descriptor's attributes
     * are valid when compared against the root/base module descriptor.
     *
     * A versioned module descriptor must be identical to the root/base module
     * descriptor, with two exceptions:
     *  - A versioned descriptor can have different non-public `requires`
     *    clauses of platform ( `java.*` and `jdk.*` ) modules, and
     *  - A versioned descriptor can have different `uses` clauses, even of
    *    service types defined outside of the platform modules.
     */
    private void checkModuleDescriptor(String miName) {
        ZipEntry ze = zf.getEntry(miName);
        if (ze != null) {
            try (InputStream jis = zf.getInputStream(ze)) {
                ModuleDescriptor md = ModuleDescriptor.read(jis);
                // Initialize the base md if it's not yet. A "base" md can be either the
                // root module-info.class or the first versioned module-info.class
                ModuleDescriptor base = this.md;

                if (base == null) {
                    concealedPkgs = new HashSet<>(md.packages());
                    md.exports().stream().map(Exports::source).forEach(concealedPkgs::remove);
                    md.opens().stream().map(Opens::source).forEach(concealedPkgs::remove);
                    // must have the implementation class of the services it 'provides'.
                    if (md.provides().stream().map(Provides::providers)
                          .flatMap(List::stream)
                          .filter(p -> zf.getEntry(toBinaryName(p)) == null)
                          .peek(p -> error(formatMsg("error.missing.provider", p)))
                          .count() != 0) {
                        isValid = false;
                        return;
                    }
                    this.md = md;
                    this.mdName = miName;
                    return;
                }

                if (!base.name().equals(md.name())) {
                    errorAndInvalid(getMsg("error.validator.info.name.notequal"));
                }
                if (!base.requires().equals(md.requires())) {
                    Set<Requires> baseRequires = base.requires();
                    for (Requires r : md.requires()) {
                        if (baseRequires.contains(r))
                            continue;
                        if (r.modifiers().contains(Requires.Modifier.TRANSITIVE)) {
                            errorAndInvalid(getMsg("error.validator.info.requires.transitive"));
                        } else if (!isPlatformModule(r.name())) {
                            errorAndInvalid(getMsg("error.validator.info.requires.added"));
                        }
                    }
                    for (Requires r : baseRequires) {
                        Set<Requires> mdRequires = md.requires();
                        if (mdRequires.contains(r))
                            continue;
                        if (!isPlatformModule(r.name())) {
                            errorAndInvalid(getMsg("error.validator.info.requires.dropped"));
                        }
                    }
                }
                if (!base.exports().equals(md.exports())) {
                    errorAndInvalid(getMsg("error.validator.info.exports.notequal"));
                }
                if (!base.opens().equals(md.opens())) {
                    errorAndInvalid(getMsg("error.validator.info.opens.notequal"));
                }
                if (!base.provides().equals(md.provides())) {
                    errorAndInvalid(getMsg("error.validator.info.provides.notequal"));
                }
                if (!base.mainClass().equals(md.mainClass())) {
                    errorAndInvalid(formatMsg("error.validator.info.manclass.notequal",
                                              ze.getName()));
                }
                if (!base.version().equals(md.version())) {
                    errorAndInvalid(formatMsg("error.validator.info.version.notequal",
                                              ze.getName()));
                }
            } catch (Exception x) {
                errorAndInvalid(x.getMessage() + " : " + miName);
            }
        }
    }

    private boolean checkClassName(FingerPrint fp) {
        if (fp.className().equals(className(fp.basename()))) {
            return true;
        }
        error(formatMsg2("error.validator.names.mismatch",
                         fp.entryName(), fp.className().replace("/", ".")));
        return isValid = false;
    }

    private boolean checkNestedClass(FingerPrint fp, Map<String, FingerPrint> outerClasses) {
        if (outerClasses.containsKey(fp.outerClassName())) {
            return true;
        }
        // outer class was not available

        error(formatMsg("error.validator.isolated.nested.class", fp.entryName()));
        return isValid = false;
    }

    private boolean isConcealed(String className) {
        if (concealedPkgs.isEmpty()) {
            return false;
        }
        int idx = className.lastIndexOf('/');
        String pkgName = idx != -1 ? className.substring(0, idx).replace('/', '.') : "";
        return concealedPkgs.contains(pkgName);
    }

    private static boolean isPlatformModule(String name) {
        return name.startsWith("java.") || name.startsWith("jdk.");
    }

    private static String className(String entryName) {
        return entryName.endsWith(".class") ? entryName.substring(0, entryName.length() - 6) : null;
    }

    private void error(String msg) {
        main.error(msg);
    }

    private void errorAndInvalid(String msg) {
        main.error(msg);
        isValid = false;
    }

    private void warn(String msg) {
        main.warn(msg);
    }
}
