/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8035063
 *
 * @summary Tests serialization of options. The options needs to be serialized
 *          and saved in the state file since the files need to be recompiled
 *          if new options are provided.
 *
 * @modules jdk.compiler/com.sun.tools.sjavac
 *          jdk.compiler/com.sun.tools.sjavac.options
 * @build Wrapper
 * @run main Wrapper Serialization
 */

import static util.OptionTestUtil.assertEquals;

import java.io.IOException;
import java.util.Map;

import com.sun.tools.sjavac.CompileJavaPackages;
import com.sun.tools.sjavac.Transformer;
import com.sun.tools.sjavac.options.Option;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.options.SourceLocation;


public class Serialization {

    public static void main(String[] args) throws IOException {

        // Create reference options
        Options options1 = Options.parseArgs(
                Option.H.arg, "headers",
                Option.S.arg, "gensrc",
                Option.D.arg, "dest",
                Option.I.arg, "pkg/*",
                Option.X.arg, "pkg/pkg/*",
                Option.SRC.arg, "root",
                Option.SOURCEPATH.arg, "sourcepath",
                Option.CLASSPATH.arg, "classpath",
                Option.MODULE_PATH.arg, "modulepath",
                Option.PERMIT_SOURCES_WITHOUT_PACKAGE.arg,
                Option.PERMIT_UNIDENTIFIED_ARTIFACTS.arg,
                Option.TR.arg, ".prop=" + CompileJavaPackages.class.getName(),
                Option.J.arg, "999",
                "-someJavacArg",
                "-someOtherJavacArg");

        // Serialize
        String serialized = options1.getStateArgsString();

        // Deserialize
        Options options2 = Options.parseArgs(serialized.split(" "));

        // Make sure we got the same result
        assertEquals(options1.getHeaderDir(), options2.getHeaderDir());
        assertEquals(options1.getGenSrcDir(), options2.getGenSrcDir());
        assertEquals(options1.getDestDir(), options2.getDestDir());

        SourceLocation sl1 = options1.getSources().get(0);
        SourceLocation sl2 = options2.getSources().get(0);
        assertEquals(sl1.getPath(), sl2.getPath());
        assertEquals(sl1.getIncludes(), sl2.getIncludes());
        assertEquals(sl1.getExcludes(), sl2.getExcludes());

        assertEquals(options1.getClassSearchPath(), options2.getClassSearchPath());
        assertEquals(options1.getSourceSearchPaths(), options2.getSourceSearchPaths());
        assertEquals(options1.getModuleSearchPaths(), options2.getModuleSearchPaths());

        Map<String, Transformer> trRules1 = options1.getTranslationRules();
        Map<String, Transformer> trRules2 = options2.getTranslationRules();
        assertEquals(trRules1.keySet(), trRules2.keySet());
        assertEquals(trRules1.values().iterator().next().getClass(),
                     trRules2.values().iterator().next().getClass());
        assertEquals(options1.getJavacArgs(), options2.getJavacArgs());

        assertEquals(999, options1.getNumCores());
        if (options2.getNumCores() == 999)
            throw new AssertionError("Num cores should not be part of serialization");
    }

}
