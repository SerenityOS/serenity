/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
  * @bug 8253700
  * @summary spurious "extends Throwable" at end of method declaration
  * throws section.  Make sure that the link is below a Throws heading.
  * @library /tools/lib ../../lib
  * @modules jdk.javadoc/jdk.javadoc.internal.tool
  * @build javadoc.tester.* toolbox.ToolBox
  * @run main TestThrows
  */

 import java.io.IOException;
 import java.nio.file.Path;

 import toolbox.ToolBox;

 import javadoc.tester.JavadocTester;

 public class TestThrows extends JavadocTester {

     public static void main(String... args) throws Exception {
         TestThrows tester = new TestThrows();
         tester.runTests(m -> new Object[] { Path.of(m.getName()) });
     }

     private final ToolBox tb = new ToolBox();

     @Test
     public void testThrowsWithBound(Path base) throws IOException {
         Path src = base.resolve("src");
         tb.writeJavaFiles(src,
                 """
                     /**
                      * This is interface C.
                      */
                     public interface C {
                         /**
                          * Method m.
                          * @param <T> the throwable
                          * @throws T if a specific error occurs
                          * @throws Exception if an exception occurs
                          */
                         <T extends Throwable> void m() throws T, Exception;
                     }
                     """);

         javadoc("-d", base.resolve("out").toString(),
                 "--no-platform-links",
                 src.resolve("C.java").toString());
         checkExit(Exit.OK);

         checkOutput("C.html", true,
                 """
                     <div class="member-signature"><span class="type-parameters">&lt;T extends java\
                     .lang.Throwable&gt;</span>&nbsp;<span class="return-type">void</span>&nbsp;<sp\
                     an class="element-name">m</span>()
                                                     throws <span class="exceptions">T,
                     java.lang.Exception</span></div>
                     """,
                 """
                     <dl class="notes">
                     <dt>Type Parameters:</dt>
                     <dd><code>T</code> - the throwable</dd>
                     <dt>Throws:</dt>
                     <dd><code>T</code> - if a specific error occurs</dd>
                     <dd><code>java.lang.Exception</code> - if an exception occurs</dd>
                     </dl>
                     """);
     }
}
