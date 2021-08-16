/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166472 8162810
 * @summary Align javac support for at-files with launcher support
 * @modules jdk.compiler/com.sun.tools.javac.main
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.javac.main.CommandLine.Tokenizer;

public class AtFileTest {

    public static void main(String... args) throws IOException {
        AtFileTest t = new AtFileTest();
        if (args.length > 0) {
            System.out.println(String.join(" ", args));
        } else {
            t.run();
        }
    }

    void run() throws IOException {
        test("-version -cp \"c:\\\\java libs\\\\one.jar\" \n",
                "-version", "-cp", "c:\\java libs\\one.jar");

        // note the open quote at the end
        test("com.foo.Panda \"Furious 5\"\fand\t'Shi Fu' \"escape\tprison",
                "com.foo.Panda", "Furious 5", "and", "Shi Fu", "escape\tprison");

        test("escaped chars testing \"\\a\\b\\c\\f\\n\\r\\t\\v\\9\\6\\23\\82\\28\\377\\477\\278\\287\"",
                "escaped", "chars", "testing", "abc\f\n\r\tv96238228377477278287");

        test("\"mix 'single quote' in double\" 'mix \"double quote\" in single' partial\"quote me\"this",
                "mix 'single quote' in double", "mix \"double quote\" in single", "partialquote methis");

        test("line one #comment\n'line #2' #rest are comment\r\n#comment on line 3\nline 4 #comment to eof",
                "line", "one", "line #2", "line", "4");

        test("This is an \"open quote \n    across line\n\t, note for WS.",
                "This", "is", "an", "open quote ", "across", "line", ",", "note", "for", "WS.");

        test("Try \"this \\\\\\\\ escape\\n double quote \\\" in open quote",
                "Try", "this \\\\ escape\n double quote \" in open quote");

        test("'-Dmy.quote.single'='Property in single quote. Here a double quote\" Add some slashes \\\\/'",
                "-Dmy.quote.single=Property in single quote. Here a double quote\" Add some slashes \\/");

        test("\"Open quote to \n  new \"line \\\n\r   third\\\n\r\\\tand\ffourth\"",
                "Open quote to ", "new", "line third\tand\ffourth");

        test("c:\\\"partial quote\"\\lib",
                "c:\\partial quote\\lib");
    }

    void test(String full, String... expect) throws IOException {
        System.out.println("test: >>>" + full + "<<<");
        List<String> found = expand(full);
        if (found.equals(Arrays.asList(expect))) {
            System.out.println("OK");
        } else {
            for (int i = 0; i < Math.max(found.size(), expect.length); i++) {
                if (i < found.size()) {
                    System.out.println("found[" + i + "]:  >>>" + found.get(i) + "<<<");
                }
                if (i < expect.length) {
                    System.out.println("expect[" + i + "]: >>>" + expect[i] + "<<<");
                }
            }
        }
        System.out.println();
    }

    List<String> expand(String full) throws IOException {
        Tokenizer t = new Tokenizer(new StringReader(full));
        List<String> result = new ArrayList<>();
        String s;
        while ((s = t.nextToken()) != null) {
//            System.err.println("token: " + s);
            result.add(s);
        }
        return result;

    }
}
