/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verifies the creation and cleaup of entries in the Protection Domain Table
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver CleanProtectionDomain
 */

import java.security.ProtectionDomain;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class CleanProtectionDomain {

  public static void main(String args[]) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                                  "-Xlog:protectiondomain+table=debug",
                                  "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                                  "-XX:+UnlockDiagnosticVMOptions",
                                  "-XX:+WhiteBoxAPI",
                                  "-Xbootclasspath/a:.",
                                  "-Djava.security.manager=allow",
                                  Test.class.getName());
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldContain("protection domain added");
    output.shouldContain("protection domain unlinked");
    output.shouldHaveExitValue(0);
  }

  static class Test {
    public static void test() throws Exception {
      TestClassLoader classloader = new TestClassLoader();
      ProtectionDomain pd = new ProtectionDomain(null, null);
      byte klassbuf[] = InMemoryJavaCompiler.compile("TestClass", "class TestClass { }");
      Class<?> klass = classloader.defineClass("TestClass", klassbuf, pd);
    }

    public static void main(String[] args) throws Exception {
      WhiteBox wb = WhiteBox.getWhiteBox();
      int removedCountOrig =  wb.protectionDomainRemovedCount();
      int removedCount;

      test();

      // Wait until ServiceThread cleans ProtectionDomain table.
      // When the TestClassLoader is unloaded by GC, at least one
      // ProtectionDomainCacheEntry will be eligible for removal.
      int cnt = 0;
      while (true) {
        if (cnt++ % 30 == 0) {
          System.gc();
        }
        removedCount = wb.protectionDomainRemovedCount();
        if (removedCountOrig != removedCount) {
          break;
        }
        Thread.sleep(100);
      }
    }

    private static class TestClassLoader extends ClassLoader {
      public TestClassLoader() {
        super();
      }

      public Class<?> defineClass(String name, byte[] bytes, ProtectionDomain pd) {
        return defineClass(name, bytes, 0, bytes.length, pd);
      }
    }
  }
}
