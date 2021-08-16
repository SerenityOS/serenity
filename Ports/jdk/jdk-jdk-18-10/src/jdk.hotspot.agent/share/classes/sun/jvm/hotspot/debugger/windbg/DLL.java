/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.windbg;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.win32.coff.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.utilities.Assert;
import sun.jvm.hotspot.utilities.memo.*;

/** Provides a simple wrapper around the COFF library which handles
    relocation. A DLL can represent either a DLL or an EXE file. */

public class DLL implements LoadObject {

  public DLL(WindbgDebugger dbg, String filename, long size, Address relocation) throws COFFException {
    this.dbg     = dbg;
    fullPathName = filename;
    this.size    = size;
    file = new MemoizedObject() {
        public Object computeValue() {
          return COFFFileParser.getParser().parse(fullPathName);
        }
      };
    addr = relocation;
  }

  /** This constructor was originally used to fetch the DLL's name out
      of the target process to match it up with the known DLL names,
      before the fetching of the DLL names and bases was folded into
      one command. It is no longer used. If it is used, getName() will
      return null and getSize() will return 0. */
  public DLL(Address base) throws COFFException {
    this.addr = base;
    file = new MemoizedObject() {
        public Object computeValue() {
          return COFFFileParser.getParser().parse(new AddressDataSource(addr));
        }
      };
  }

  /** Indicates whether this is really a DLL or actually a .EXE
      file. */
  public boolean isDLL() {
    return getFile().getHeader().hasCharacteristic(Characteristics.IMAGE_FILE_DLL);
  }

  /** Look up a symbol; returns absolute address or null if symbol was
      not found. */
  public Address lookupSymbol(String symbol) throws COFFException {
    if (!isDLL()) {
      return null;
    }
    ExportDirectoryTable exports = getExportDirectoryTable();
    return lookupSymbol(symbol, exports,
                        0, exports.getNumberOfNamePointers() - 1);
  }

  public Address getBase() {
    return addr;
  }

  /** Returns the full path name of this DLL/EXE, or null if this DLL
      object was created by parsing the target process's address
      space. */
  public String getName() {
    return fullPathName;
  }

  public long getSize() {
    return size;
  }

  public CDebugInfoDataBase getDebugInfoDataBase() throws DebuggerException {
    if (db != null) {
      return db;
    }

    // Try to parse
    if (dbg == null) {
      return null; // Need WindbgDebugger
    }

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(fullPathName != null, "Need full path name to build debug info database");
    }

    db = new WindbgCDebugInfoBuilder(dbg).buildDataBase(fullPathName, addr);
    return db;
  }

  public BlockSym debugInfoForPC(Address pc) throws DebuggerException {
    CDebugInfoDataBase db = getDebugInfoDataBase();
    if (db == null) {
      return null;
    }
    return db.debugInfoForPC(pc);
  }

  public ClosestSymbol closestSymbolToPC(Address pcAsAddr) throws DebuggerException {
    ExportDirectoryTable exports = getExportDirectoryTable();
    if (exports == null) {
      return null;
    }
    String name = null;
    long   pc   = dbg.getAddressValue(pcAsAddr);
    long   diff = Long.MAX_VALUE;
    long   base = dbg.getAddressValue(addr);
    for (int i = 0; i < exports.getNumberOfNamePointers(); i++) {
      if (!exports.isExportAddressForwarder(exports.getExportOrdinal(i))) {
        long tmp = base + (exports.getExportAddress(exports.getExportOrdinal(i)) & 0xFFFFFFFF);
        if ((tmp <= pc) && ((pc - tmp) < diff)) {
          diff = pc - tmp;
          name = exports.getExportName(i);
        }
      }
    }
    if (name == null) {
      return null;
    }
    return new ClosestSymbol(name, diff);
  }

  public LineNumberInfo lineNumberForPC(Address pc) throws DebuggerException {
    CDebugInfoDataBase db = getDebugInfoDataBase();
    if (db == null) {
      return null;
    }
    return db.lineNumberForPC(pc);
  }

  public void close() {
    getFile().close();
    file = null;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private COFFFile getFile() {
    return (COFFFile) file.getValue();
  }

  private Address lookupSymbol(String symbol, ExportDirectoryTable exports,
                               int loIdx, int hiIdx) {
    do {
      int curIdx = ((loIdx + hiIdx) >> 1);
      String cur = exports.getExportName(curIdx);
      if (symbol.equals(cur)) {
        return addr.addOffsetTo(
          ((long) exports.getExportAddress(exports.getExportOrdinal(curIdx))) & 0xFFFFFFFFL
        );
      }
      if (symbol.compareTo(cur) < 0) {
        if (hiIdx == curIdx) {
          hiIdx = curIdx - 1;
        } else {
          hiIdx = curIdx;
        }
      } else {
        if (loIdx == curIdx) {
          loIdx = curIdx + 1;
        } else {
          loIdx = curIdx;
        }
      }
    } while (loIdx <= hiIdx);

    return null;
  }

  private ExportDirectoryTable getExportDirectoryTable() {
    return
      getFile().getHeader().getOptionalHeader().getDataDirectories().getExportDirectoryTable();
  }

  private WindbgDebugger  dbg;
  private String         fullPathName;
  private long           size;
  // MemoizedObject contains a COFFFile
  private MemoizedObject file;
  // Base address of module in target process
  private Address        addr;
  // Debug info database for this DLL
  private CDebugInfoDataBase db;
}
