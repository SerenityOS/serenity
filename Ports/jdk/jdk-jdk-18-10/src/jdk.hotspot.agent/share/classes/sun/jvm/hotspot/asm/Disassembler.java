/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.asm;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import sun.jvm.hotspot.code.CodeBlob;
import sun.jvm.hotspot.code.NMethod;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.debugger.DebuggerException;
import sun.jvm.hotspot.runtime.VM;

public class Disassembler {
   private static String options = "";
   private static long decode_function;

   protected long startPc;
   protected byte[] code;
   private CodeBlob blob;
   private NMethod nmethod;

   public static void decode(InstructionVisitor visitor, CodeBlob blob) {
      decode(visitor, blob, blob.codeBegin(), blob.codeEnd());
   }

   public static void decode(InstructionVisitor visitor, CodeBlob blob, Address begin, Address end) {
      int codeSize = (int)end.minus(begin);
      long startPc = VM.getAddressValue(begin);
      byte[] code = new byte[codeSize];
      for (int i = 0; i < code.length; i++)
         code[i] = begin.getJByteAt(i);
      Disassembler dis = new Disassembler(startPc, code);
      dis.decode(visitor);
   }

   private Disassembler(long startPc, byte[] code) {
      this.startPc = startPc;
      this.code = code;

      // Lazily load hsdis
      if (decode_function == 0) {
         // Search for hsdis library in the following 4 locations:
         //   1. <home>/lib/<vm>/libhsdis-<arch>.so
         //   2. <home>/lib/<vm>/hsdis-<arch>.so
         //   3. <home>/lib/hsdis-<arch>.so
         //   4. hsdis-<arch>.so  (using LD_LIBRARY_PATH)
         Properties targetSysProps = VM.getVM().getSystemProperties();
         String os = targetSysProps.getProperty("os.name");
         String ext = ".so";
         if (os.contains("Windows")) {
            ext = ".dll";
         } else if (os.contains("Mac OS")) {
            ext = ".dylib";
         }

         // Find the full path to libjvm.so (jvm.dll and libjvm.dylib on Windows and OSX).
         String jvmPattern = "^(lib)?jvm\\" + ext + "$";
         Path jvmPath = VM.getVM()
                          .getDebugger()
                          .getCDebugger()
                          .getLoadObjectList()
                          .stream()
                          .map(o -> Path.of(o.getName()))
                          .filter(p -> p.getFileName().toString().matches(jvmPattern))
                          .findAny()
                          .get();

         String arch = targetSysProps.getProperty("os.arch");
         String libname = "hsdis-" + arch + ext;

         List<String> libs = List.of(
            // 1. <home>/lib/<vm>/libhsdis-<arch>.so
            jvmPath.resolveSibling("lib" + libname).toString(),
            // 2. <home>/lib/<vm>/hsdis-<arch>.so
            jvmPath.resolveSibling(libname).toString(),
            // 3. <home>/lib/hsdis-<arch>.so
            jvmPath.getParent().resolveSibling(libname).toString(),
            // 4. hsdis-<arch>.so  (using LD_LIBRARY_PATH)
            libname
         );

         var itr = libs.iterator();
         while (itr.hasNext() && (decode_function == 0L)) {
            try {
               decode_function = load_library(itr.next());
            } catch (DebuggerException e) {
               if (!itr.hasNext()) {
                  throw e;
               }
            }
         }
      }
   }

   private static native long load_library(String hsdis_library_name);

   private native void decode(InstructionVisitor visitor, long pc, byte[] code,
                              String options, long decode_function);

   private void decode(InstructionVisitor visitor) {
      visitor.prologue();
      decode(visitor, startPc, code, options, decode_function);
      visitor.epilogue();
   }

   private boolean match(String event, String tag) {
      if (!event.startsWith(tag))
         return false;
      int taglen = tag.length();
      if (taglen == event.length()) return true;
      char delim = event.charAt(taglen);
      return delim == ' ' || delim == '/' || delim == '=';
   }

   // This is called from the native code to process various markers
   // in the dissassembly.
   private long handleEvent(InstructionVisitor visitor, String event, long arg) {
      if (match(event, "insn")) {
         try {
            visitor.beginInstruction(arg);
         } catch (Throwable e) {
            e.printStackTrace();
         }
      } else if (match(event, "/insn")) {
         try {
            visitor.endInstruction(arg);
         } catch (Throwable e) {
            e.printStackTrace();
         }
      } else if (match(event, "addr")) {
         if (arg != 0) {
            visitor.printAddress(arg);
         }
         return arg;
      } else if (match(event, "mach")) {
         // output().printf("[Disassembling for mach='%s']\n", arg);
      } else {
         // ignore unrecognized markup
      }
      return 0;
   }

   // This called from the native code to perform printing
   private  void rawPrint(InstructionVisitor visitor, String s) {
      visitor.print(s);
   }
}
