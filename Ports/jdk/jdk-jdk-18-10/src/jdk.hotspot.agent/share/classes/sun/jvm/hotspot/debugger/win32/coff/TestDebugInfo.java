/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.win32.coff;

public class TestDebugInfo implements DebugVC50SubsectionTypes, DebugVC50SymbolTypes, DebugVC50TypeLeafIndices {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: java TestParser [file name]");
      System.err.println("File name may be an .exe, .dll or .obj");
      System.exit(1);
    }

    try {
      COFFFile file = COFFFileParser.getParser().parse(args[0]);
      if (file.isImage()) {
        System.out.println("PE Image detected.");
      } else {
        System.out.println("PE Image NOT detected, assuming object file.");
      }

      DebugVC50 vc50 = getDebugVC50(file);
      if (vc50 == null) {
        System.out.println("No debug information found.");
        System.exit(1);
      } else {
        System.out.println("Debug information found!");
      }

      DebugVC50SubsectionDirectory dir = vc50.getSubsectionDirectory();
      for (int i = 0; i < dir.getNumEntries(); i++) {
        DebugVC50Subsection sec = dir.getSubsection(i);
        switch (sec.getSubsectionType()) {
        case SST_MODULE: System.out.println("  SST_MODULE"); break;
        case SST_TYPES: System.out.println("  SST_TYPES"); break;
        case SST_PUBLIC: System.out.println("  SST_PUBLIC"); break;
        case SST_PUBLIC_SYM: System.out.println("  SST_PUBLIC_SYM"); break;
        case SST_SYMBOLS: System.out.println("  SST_SYMBOLS"); break;
        case SST_ALIGN_SYM: System.out.println("  SST_ALIGN_SYM"); printSymbolTable(((DebugVC50SSAlignSym) sec).getSymbolIterator()); break;
        case SST_SRC_LN_SEG: System.out.println("  SST_SRC_LN_SEG"); break;
        case SST_SRC_MODULE: System.out.println("  SST_SRC_MODULE"); break;
        case SST_LIBRARIES: System.out.println("  SST_LIBRARIES"); break;
        case SST_GLOBAL_SYM: System.out.println("  SST_GLOBAL_SYM"); printSymbolTable(sec); break;
        case SST_GLOBAL_PUB: System.out.println("  SST_GLOBAL_PUB"); printSymbolTable(sec); break;
        case SST_GLOBAL_TYPES: System.out.println("  SST_GLOBAL_TYPES"); printTypeTable(sec); break;
        case SST_MPC: System.out.println("  SST_MPC"); break;
        case SST_SEG_MAP: System.out.println("  SST_SEG_MAP"); break;
        case SST_SEG_NAME: System.out.println("  SST_SEG_NAME"); break;
        case SST_PRE_COMP: System.out.println("  SST_PRE_COMP"); break;
        case SST_UNUSED: System.out.println("  SST_UNUSED"); break;
        case SST_OFFSET_MAP_16: System.out.println("  SST_OFFSET_MAP_16"); break;
        case SST_OFFSET_MAP_32: System.out.println("  SST_OFFSET_MAP_32"); break;
        case SST_FILE_INDEX: System.out.println("  SST_FILE_INDEX"); break;
        case SST_STATIC_SYM: System.out.println("  SST_STATIC_SYM"); printSymbolTable(sec); break;
        default: System.out.println("  (Unknown subsection type " + sec.getSubsectionType() + ")"); break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static DebugVC50 getDebugVC50(COFFFile file) {
    COFFHeader header = file.getHeader();
    OptionalHeader opt = header.getOptionalHeader();
    if (opt == null) {
      System.out.println("Optional header not found.");
      return null;
    }
    OptionalHeaderDataDirectories dd = opt.getDataDirectories();
    if (dd == null) {
      System.out.println("Optional header data directories not found.");
      return null;
    }
    DebugDirectory debug = dd.getDebugDirectory();
    if (debug == null) {
      System.out.println("Debug directory not found.");
      return null;
    }
    for (int i = 0; i < debug.getNumEntries(); i++) {
      DebugDirectoryEntry entry = debug.getEntry(i);
      if (entry.getType() == DebugTypes.IMAGE_DEBUG_TYPE_CODEVIEW) {
        System.out.println("Debug Directory Entry " + i + " has debug type IMAGE_DEBUG_TYPE_CODEVIEW");
        return entry.getDebugVC50();
      }
    }

    return null;
  }

  private static void printSymbolTable(DebugVC50Subsection sec) {
    DebugVC50SSSymbolBase sym = (DebugVC50SSSymbolBase) sec;
    DebugVC50SymbolIterator iter = sym.getSymbolIterator();
    printSymbolTable(iter);
  }

  private static void printSymbolTable(DebugVC50SymbolIterator iter) {
    while (!iter.done()) {
      int type = iter.getType() & 0xFFFF;
      switch (type) {
      case S_COMPILE: System.out.println("    S_COMPILE"); break;
      case S_SSEARCH: System.out.println("    S_SSEARCH"); break;
      case S_END: System.out.println("    S_END"); break;
      case S_SKIP: System.out.println("    S_SKIP"); break;
      case S_CVRESERVE: System.out.println("    S_CVRESERVE"); break;
      case S_OBJNAME: System.out.println("    S_OBJNAME"); break;
      case S_ENDARG: System.out.println("    S_ENDARG"); break;
      case S_COBOLUDT: System.out.println("    S_COBOLUDT"); break;
      case S_MANYREG: System.out.println("    S_MANYREG"); break;
      case S_RETURN: System.out.println("    S_RETURN"); break;
      case S_ENTRYTHIS: System.out.println("    S_ENTRYTHIS"); break;
      case S_REGISTER: System.out.println("    S_REGISTER"); break;
      case S_CONSTANT: System.out.println("    S_CONSTANT"); break;
      case S_UDT: System.out.println("    S_UDT"); break;
      case S_COBOLUDT2: System.out.println("    S_COBOLUDT2"); break;
      case S_MANYREG2: System.out.println("    S_MANYREG2"); break;
      case S_BPREL32: System.out.println("    S_BPREL32"); break;
      case S_LDATA32: System.out.println("    S_LDATA32"); break;
      case S_GDATA32: System.out.println("    S_GDATA32"); break;
      case S_PUB32: System.out.println("    S_PUB32"); break;
      case S_LPROC32: System.out.println("    S_LPROC32"); break;
      case S_GPROC32: System.out.println("    S_GPROC32"); break;
      case S_THUNK32: System.out.println("    S_THUNK32"); break;
      case S_BLOCK32: System.out.println("    S_BLOCK32"); break;
      case S_WITH32: System.out.println("    S_WITH32"); break;
      case S_LABEL32: System.out.println("    S_LABEL32"); break;
      case S_CEXMODEL32: System.out.println("    S_CEXMODEL32"); break;
      case S_VFTTABLE32: System.out.println("    S_VFTTABLE32"); break;
      case S_REGREL32: System.out.println("    S_REGREL32"); break;
      case S_LTHREAD32: System.out.println("    S_LTHREAD32"); break;
      case S_GTHREAD32: System.out.println("    S_GTHREAD32"); break;
      case S_LPROCMIPS: System.out.println("    S_LPROCMIPS"); break;
      case S_GPROCMIPS: System.out.println("    S_GPROCMIPS"); break;
      case S_PROCREF: System.out.println("    S_PROCREF"); break;
      case S_DATAREF: System.out.println("    S_DATAREF"); break;
      case S_ALIGN: System.out.println("    S_ALIGN"); break;
      default: System.out.println("    (Unknown symbol type " + type + ")"); break;
      }

      iter.next();
    }
  }

  private static void printTypeTable(DebugVC50Subsection sec) {
    DebugVC50SSGlobalTypes types = (DebugVC50SSGlobalTypes) sec;

    DebugVC50TypeIterator iter = types.getTypeIterator();
    while (!iter.done()) {
      System.out.print("    Type string: ");
      while (!iter.typeStringDone()) {
        int leaf = iter.typeStringLeaf() & 0xFFFF;
        switch (leaf) {
        case LF_MODIFIER: System.out.print("LF_MODIFIER "); break;
        case LF_POINTER: System.out.print("LF_POINTER "); break;
        case LF_ARRAY: System.out.print("LF_ARRAY "); break;
        case LF_CLASS: System.out.print("LF_CLASS "); break;
        case LF_STRUCTURE: System.out.print("LF_STRUCTURE "); break;
        case LF_UNION: System.out.print("LF_UNION "); break;
        case LF_ENUM: System.out.print("LF_ENUM "); break;
        case LF_PROCEDURE: System.out.print("LF_PROCEDURE "); break;
        case LF_MFUNCTION: System.out.print("LF_MFUNCTION "); break;
        case LF_VTSHAPE: System.out.print("LF_VTSHAPE "); break;
        case LF_COBOL0: System.out.print("LF_COBOL0 "); break;
        case LF_COBOL1: System.out.print("LF_COBOL1 "); break;
        case LF_BARRAY: System.out.print("LF_BARRAY "); break;
        case LF_LABEL: System.out.print("LF_LABEL "); break;
        case LF_NULL: System.out.print("LF_NULL "); break;
        case LF_NOTTRAN: System.out.print("LF_NOTTRAN "); break;
        case LF_DIMARRAY: System.out.print("LF_DIMARRAY "); break;
        case LF_VFTPATH: System.out.print("LF_VFTPATH "); break;
        case LF_PRECOMP: System.out.print("LF_PRECOMP "); break;
        case LF_ENDPRECOMP: System.out.print("LF_ENDPRECOMP "); break;
        case LF_OEM: System.out.print("LF_OEM "); break;
        case LF_TYPESERVER: System.out.print("LF_TYPESERVER "); break;
        case LF_SKIP: System.out.print("LF_SKIP "); break;
        case LF_ARGLIST: System.out.print("LF_ARGLIST "); break;
        case LF_DEFARG: System.out.print("LF_DEFARG "); break;
        case LF_FIELDLIST: System.out.print("LF_FIELDLIST "); break;
        case LF_DERIVED: System.out.print("LF_DERIVED "); break;
        case LF_BITFIELD: System.out.print("LF_BITFIELD "); break;
        case LF_METHODLIST: System.out.print("LF_METHODLIST "); break;
        case LF_DIMCONU: System.out.print("LF_DIMCONU "); break;
        case LF_DIMCONLU: System.out.print("LF_DIMCONLU "); break;
        case LF_DIMVARU: System.out.print("LF_DIMVARU "); break;
        case LF_DIMVARLU: System.out.print("LF_DIMVARLU "); break;
        case LF_REFSYM: System.out.print("LF_REFSYM "); break;
        case LF_BCLASS: System.out.print("LF_BCLASS "); break;
        case LF_VBCLASS: System.out.print("LF_VBCLASS "); break;
        case LF_IVBCLASS: System.out.print("LF_IVBCLASS "); break;
        case LF_ENUMERATE: System.out.print("LF_ENUMERATE "); break;
        case LF_FRIENDFCN: System.out.print("LF_FRIENDFCN "); break;
        case LF_INDEX: System.out.print("LF_INDEX "); break;
        case LF_MEMBER: System.out.print("LF_MEMBER ");  System.out.print(iter.getMemberName() + " "); break;
        case LF_STMEMBER: System.out.print("LF_STMEMBER "); break;
        case LF_METHOD: System.out.print("LF_METHOD "); System.out.print(iter.getMethodName() + " "); break;
        case LF_NESTTYPE: System.out.print("LF_NESTTYPE "); break;
        case LF_VFUNCTAB: System.out.print("LF_VFUNCTAB "); break;
        case LF_FRIENDCLS: System.out.print("LF_FRIENDCLS "); break;
        case LF_ONEMETHOD: System.out.print("LF_ONEMETHOD "); System.out.print(iter.getOneMethodName() + " "); break;
        case LF_VFUNCOFF: System.out.print("LF_VFUNCOFF "); break;
        case LF_NESTTYPEEX: System.out.print("LF_NESTTYPEEX "); break;
        case LF_MEMBERMODIFY: System.out.print("LF_MEMBERMODIFY "); break;
        case LF_CHAR: System.out.print("LF_CHAR "); break;
        case LF_SHORT: System.out.print("LF_SHORT "); break;
        case LF_USHORT: System.out.print("LF_USHORT "); break;
        case LF_LONG: System.out.print("LF_LONG "); break;
        case LF_ULONG: System.out.print("LF_ULONG "); break;
        case LF_REAL32: System.out.print("LF_REAL32 "); break;
        case LF_REAL64: System.out.print("LF_REAL64 "); break;
        case LF_REAL80: System.out.print("LF_REAL80 "); break;
        case LF_REAL128: System.out.print("LF_REAL128 "); break;
        case LF_QUADWORD: System.out.print("LF_QUADWORD "); break;
        case LF_UQUADWORD: System.out.print("LF_UQUADWORD "); break;
        case LF_REAL48: System.out.print("LF_REAL48 "); break;
        case LF_COMPLEX32: System.out.print("LF_COMPLEX32 "); break;
        case LF_COMPLEX64: System.out.print("LF_COMPLEX64 "); break;
        case LF_COMPLEX80: System.out.print("LF_COMPLEX80 "); break;
        case LF_COMPLEX128: System.out.print("LF_COMPLEX128 "); break;
        case LF_VARSTRING: System.out.print("LF_VARSTRING "); break;
        case LF_PAD0: System.out.print("LF_PAD0 "); break;
        case LF_PAD1: System.out.print("LF_PAD1 "); break;
        case LF_PAD2: System.out.print("LF_PAD2 "); break;
        case LF_PAD3: System.out.print("LF_PAD3 "); break;
        case LF_PAD4: System.out.print("LF_PAD4 "); break;
        case LF_PAD5: System.out.print("LF_PAD5 "); break;
        case LF_PAD6: System.out.print("LF_PAD6 "); break;
        case LF_PAD7: System.out.print("LF_PAD7 "); break;
        case LF_PAD8: System.out.print("LF_PAD8 "); break;
        case LF_PAD9: System.out.print("LF_PAD9 "); break;
        case LF_PAD10: System.out.print("LF_PAD10 "); break;
        case LF_PAD11: System.out.print("LF_PAD11 "); break;
        case LF_PAD12: System.out.print("LF_PAD12 "); break;
        case LF_PAD13: System.out.print("LF_PAD13 "); break;
        case LF_PAD14: System.out.print("LF_PAD14 "); break;
        case LF_PAD15: System.out.print("LF_PAD15 "); break;
        default: System.out.print("(Unknown leaf " + leaf + ")");
        }

        iter.typeStringNext();
      }

      System.out.println("");
      iter.next();
    }
  }
}
