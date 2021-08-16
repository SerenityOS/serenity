/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.gc.shared;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** <P> The (supported) Generation hierarchy currently looks like this: </P>

    <ul>
    <li> Generation
      <ul>
      <li> CardGeneration
        <ul>
        <li> TenuredGeneration
        </ul>
      <li> DefNewGeneration
      </ul>
    </ul>
*/


public abstract class Generation extends VMObject {
  private static long          reservedFieldOffset;
  private static long          virtualSpaceFieldOffset;
  protected static final int  K = 1024;
  // Fields for class StatRecord
  private static Field         statRecordField;
  private static CIntegerField invocationField;

  // constants from Name enum
  private static int NAME_DEF_NEW;
  private static int NAME_PAR_NEW;
  private static int NAME_MARK_SWEEP_COMPACT;
  private static int NAME_CONCURRENT_MARK_SWEEP;
  private static int NAME_OTHER;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("Generation");

    reservedFieldOffset     = type.getField("_reserved").getOffset();
    virtualSpaceFieldOffset = type.getField("_virtual_space").getOffset();
    // StatRecord
    statRecordField         = type.getField("_stat_record");
    type                    = db.lookupType("Generation::StatRecord");
    invocationField         = type.getCIntegerField("invocations");

    // constants from Generation::Name
    NAME_DEF_NEW = db.lookupIntConstant("Generation::DefNew").intValue();
    NAME_MARK_SWEEP_COMPACT = db.lookupIntConstant("Generation::MarkSweepCompact").intValue();
    NAME_OTHER = db.lookupIntConstant("Generation::Other").intValue();
  }

  public Generation(Address addr) {
    super(addr);
  }

  public static class Name {
    public static final Name DEF_NEW = new Name("DefNew");
    public static final Name MARK_SWEEP_COMPACT = new Name("MarkSweepCompact");
    public static final Name OTHER = new Name("Other");

    private Name(String value) {
      this.value = value;
    }

    private String value;
    public String toString() {
      return value;
    }
  }

  public Generation.Name kind() {
    return Generation.Name.OTHER;
  }

  static Generation.Name nameForEnum(int value) {
     if (value == NAME_DEF_NEW) {
        return Name.DEF_NEW;
     } else if (value == NAME_MARK_SWEEP_COMPACT) {
        return Name.MARK_SWEEP_COMPACT;
     } else if (value == NAME_OTHER) {
        return Name.OTHER;
     } else {
        throw new RuntimeException("should not reach here");
     }
  }

  public int invocations() {
    return getStatRecord().getInvocations();
  }

  /** The maximum number of object bytes the generation can currently
      hold. */
  public abstract long capacity();

  /** The number of used bytes in the gen. */
  public abstract long used();

  /** The number of free bytes in the gen. */
  public abstract long free();

  /** The largest number of contiguous free words in the generation,
      including expansion. (VM's version assumes it is called at a
      safepoint.)  */
  public abstract long contiguousAvailable();

  public MemRegion reserved() {
    return new MemRegion(addr.addOffsetTo(reservedFieldOffset));
  }

  /** Returns a region guaranteed to contain all the objects in the
      generation. */
  public MemRegion usedRegion() {
    return reserved();
  }

  /* Returns "TRUE" iff "p" points into an allocated object in the
     generation. */
  public boolean isIn(Address p) {
    GenerationIsInClosure blk = new GenerationIsInClosure(p);
    spaceIterate(blk);
    return (blk.space() != null);
  }

  /** Returns "TRUE" iff "p" points into the reserved area of the
     generation. */
  public boolean isInReserved(Address p) {
    return reserved().contains(p);
  }

  protected VirtualSpace virtualSpace() {
    return (VirtualSpace) VMObjectFactory.newObject(VirtualSpace.class, addr.addOffsetTo(virtualSpaceFieldOffset));
  }

  public abstract String name();

  /** Equivalent to spaceIterate(blk, false) */
  public void spaceIterate(SpaceClosure blk) {
    spaceIterate(blk, false);
  }

  /** Iteration - do not use for time critical operations */
  public abstract void spaceIterate(SpaceClosure blk, boolean usedOnly);
  public abstract void liveRegionsIterate(LiveRegionsClosure closure);

  public void print() { printOn(System.out); }
  public abstract void printOn(PrintStream tty);

  public static class StatRecord extends VMObject {
    public StatRecord(Address addr) {
      super(addr);
    }

    public int getInvocations() {
      return (int) invocationField.getValue(addr);
    }

  }

  private StatRecord getStatRecord() {
    return (StatRecord) VMObjectFactory.newObject(Generation.StatRecord.class, addr.addOffsetTo(statRecordField.getOffset()));
  }
}
