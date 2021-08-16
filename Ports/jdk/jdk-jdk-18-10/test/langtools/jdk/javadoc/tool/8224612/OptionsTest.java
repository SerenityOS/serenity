/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224612
 * @key randomness
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main/othervm OptionsTest
 */

import javadoc.tester.JavadocTester;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public class OptionsTest extends JavadocTester {

    public static void main(String... args) throws Exception {
        new OptionsTest().runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    @Test
    public void testEmptySupportedOptionsDoclet(Path base) {
        test(EmptySupportedOptionsDoclet.class);
    }

    private void test(Class<? extends Doclet> _class) {
        javadoc("-doclet", _class.getName(),
                "-docletpath", System.getProperty("test.classes", "."),
                "--help");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, false, "javadoc: error - fatal error encountered: java.lang.NullPointerException");
        checkOutput(Output.OUT, false, "Provided by the %s doclet:".formatted(_class.getSimpleName()));
    }

    @Test
    public void testNullSupportedOptionsDoclet(Path base) {
        test(NullSupportedOptionsDoclet.class);
    }

    public static final class EmptySupportedOptionsDoclet implements Doclet {

        private final Random random;

        public EmptySupportedOptionsDoclet() {
            long seed = Long.getLong("jdk.test.lib.random.seed", System.currentTimeMillis());
            System.out.println("new java.util.Random(" + seed + ")");
            this.random = new Random(seed);
        }

        @Override
        public void init(Locale locale, Reporter reporter) {
        }

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public Set<? extends Option> getSupportedOptions() {
            return randomEmptySet();
        }

        /*
         * This method is used to check that emptiness of a set is determined
         * by value (or in this case, by behavior), rather than by reference
         * (i.e. there's no code like `s == Collections.EMPTY_SET`, etc.)
         */
        private Set<? extends Option> randomEmptySet() {
            List<Supplier<Set<? extends Option>>> emptySets = List.of(
                    Set::of,
                    Collections::emptySet,
                    HashSet::new,
                    TreeSet::new,
                    LinkedHashSet::new
            );
            int idx = random.nextInt(emptySets.size());
            return emptySets.get(idx).get();
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean run(DocletEnvironment environment) {
            return true;
        }
    }

    /**
     * An implementation of an otherwise well-behaving Doclet, that returns
     * {@code null} from {@link #getSupportedOptions}.
     */
    public static final class NullSupportedOptionsDoclet implements Doclet {

        @Override
        public void init(Locale locale, Reporter reporter) {
        }

        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public Set<? extends Option> getSupportedOptions() {
            return null;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean run(DocletEnvironment environment) {
            return true;
        }
    }
}
