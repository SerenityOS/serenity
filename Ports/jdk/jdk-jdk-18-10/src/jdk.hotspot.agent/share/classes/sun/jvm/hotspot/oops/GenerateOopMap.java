/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** Minimal port of the VM's oop map generator for interpreted frames */

public class GenerateOopMap {
  interface JumpClosure {
    public void process(GenerateOopMap c, int bcpDelta, int[] data);
  }

  // Used for debugging this code
  private static final boolean DEBUG = false;

  // These two should be removed. But requires som code to be cleaned up
  private static final int MAXARGSIZE     =   256;      // This should be enough
  private static final int MAX_LOCAL_VARS = 65536;      // 16-bit entry
  private static final boolean TraceMonitorMismatch = true;
  private static final boolean TraceOopMapRewrites = true;

  // Commonly used constants
  static CellTypeState[] epsilonCTS = { CellTypeState.bottom };
  static CellTypeState   refCTS     = CellTypeState.ref;
  static CellTypeState   valCTS     = CellTypeState.value;
  static CellTypeState[]    vCTS    = { CellTypeState.value, CellTypeState.bottom };
  static CellTypeState[]    rCTS    = { CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[]   rrCTS    = { CellTypeState.ref,   CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[]   vrCTS    = { CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[]   vvCTS    = { CellTypeState.value, CellTypeState.value, CellTypeState.bottom };
  static CellTypeState[]  rvrCTS    = { CellTypeState.ref,   CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[]  vvrCTS    = { CellTypeState.value, CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[]  vvvCTS    = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.bottom };
  static CellTypeState[] vvvrCTS    = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.ref,   CellTypeState.bottom };
  static CellTypeState[] vvvvCTS    = { CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.value, CellTypeState.bottom };

  /** Specialization of SignatureIterator - compute the effects of a call */
  static class ComputeCallStack extends SignatureIterator {
    CellTypeStateList _effect;
    int _idx;

    void set(CellTypeState state)         { _effect.get(_idx++).set(state); }
    int  length()                         { return _idx; };

    public void doBool  ()              { set(CellTypeState.value); }
    public void doChar  ()              { set(CellTypeState.value); }
    public void doFloat ()              { set(CellTypeState.value); }
    public void doByte  ()              { set(CellTypeState.value); }
    public void doShort ()              { set(CellTypeState.value); }
    public void doInt   ()              { set(CellTypeState.value); }
    public void doVoid  ()              { set(CellTypeState.bottom);}
    public void doObject(int begin, int end) { set(CellTypeState.ref); }
    public void doArray (int begin, int end) { set(CellTypeState.ref); }

    public void doDouble()              { set(CellTypeState.value);
                                          set(CellTypeState.value); }
    public void doLong  ()              { set(CellTypeState.value);
                                          set(CellTypeState.value); }

    ComputeCallStack(Symbol signature) {
      super(signature);
    }

    // Compute methods
    int computeForParameters(boolean is_static, CellTypeStateList effect) {
      _idx    = 0;
      _effect = effect;

      if (!is_static) {
        effect.get(_idx++).set(CellTypeState.ref);
      }

      iterateParameters();

      return length();
    };

    int computeForReturntype(CellTypeStateList effect) {
      _idx    = 0;
      _effect = effect;
      iterateReturntype();
      set(CellTypeState.bottom);  // Always terminate with a bottom state, so ppush works

      return length();
    }
  }

  /** Specialization of SignatureIterator - in order to set up first stack frame */
  static class ComputeEntryStack extends SignatureIterator {
    CellTypeStateList _effect;
    int _idx;

    void set(CellTypeState state)         { _effect.get(_idx++).set(state); }
    int  length()                         { return _idx; };

    public void doBool  ()              { set(CellTypeState.value); }
    public void doChar  ()              { set(CellTypeState.value); }
    public void doFloat ()              { set(CellTypeState.value); }
    public void doByte  ()              { set(CellTypeState.value); }
    public void doShort ()              { set(CellTypeState.value); }
    public void doInt   ()              { set(CellTypeState.value); }
    public void doVoid  ()              { set(CellTypeState.bottom);}
    public void doObject(int begin, int end) { set(CellTypeState.makeSlotRef(_idx)); }
    public void doArray (int begin, int end) { set(CellTypeState.makeSlotRef(_idx)); }

    public void doDouble()              { set(CellTypeState.value);
                                          set(CellTypeState.value); }
    public void doLong  ()              { set(CellTypeState.value);
                                          set(CellTypeState.value); }

    ComputeEntryStack(Symbol signature) {
      super(signature);
    }

    // Compute methods
    int computeForParameters(boolean is_static, CellTypeStateList effect) {
      _idx    = 0;
      _effect = effect;

      if (!is_static) {
        effect.get(_idx++).set(CellTypeState.makeSlotRef(0));
      }

      iterateParameters();

      return length();
    };

    int computeForReturntype(CellTypeStateList effect) {
      _idx    = 0;
      _effect = effect;
      iterateReturntype();
      set(CellTypeState.bottom);  // Always terminate with a bottom state, so ppush works

      return length();
    }
  }

  /** Contains maping between jsr targets and there return addresses.
      One-to-many mapping. */
  static class RetTableEntry {
    private static int _init_nof_jsrs; // Default size of jsrs list
    private int _target_bci;           // Target PC address of jump (bytecode index)
    private List<Integer> _jsrs;       // List of return addresses  (bytecode index)
    private RetTableEntry _next;       // Link to next entry

    RetTableEntry(int target, RetTableEntry next) {
      _target_bci = target;
      _jsrs = new ArrayList<>(_init_nof_jsrs);
      _next = next;
    }

    // Query
    int targetBci()  { return _target_bci; }
    int nofJsrs()    { return _jsrs.size(); }
    int jsrs(int i)  { return _jsrs.get(i).intValue(); }

    // Update entry
    void addJsr  (int return_bci)     { _jsrs.add(return_bci); }
    void addDelta(int bci, int delta) {
      if (_target_bci > bci) {
        _target_bci += delta;
      }

      for (int k = 0; k < nofJsrs(); k++) {
        int jsr = jsrs(k);
        if (jsr > bci) {
          _jsrs.set(k, jsr + delta);
        }
      }
    }
    RetTableEntry next()               { return _next; }
  }

  static class RetTable {
    private RetTableEntry _first;
    private static int _init_nof_entries;

    private void addJsr(int return_bci, int target_bci) {
      RetTableEntry entry = _first;

      // Scan table for entry
      for (;(entry != null) && (entry.targetBci() != target_bci); entry = entry.next());

      if (entry == null) {
        // Allocate new entry and put in list
        entry = new RetTableEntry(target_bci, _first);
        _first = entry;
      }

      // Now "entry" is set.  Make sure that the entry is initialized
      // and has room for the new jsr.
      entry.addJsr(return_bci);
    }

    RetTable() {}
    void computeRetTable(Method method) {
      BytecodeStream i = new BytecodeStream(method);
      int bytecode;

      while( (bytecode = i.next()) >= 0) {
        switch (bytecode) {
        case Bytecodes._jsr:
          addJsr(i.nextBCI(), i.dest());
          break;
        case Bytecodes._jsr_w:
          addJsr(i.nextBCI(), i.dest_w());
          break;
        }
      }
    }
    void updateRetTable(int bci, int delta) {
      RetTableEntry cur = _first;
      while(cur != null) {
        cur.addDelta(bci, delta);
        cur = cur.next();
      }
    }
    RetTableEntry findJsrsForTarget(int targBci) {
      RetTableEntry cur = _first;

      while(cur != null) {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(cur.targetBci() != -1, "sanity check");
        }
        if (cur.targetBci() == targBci) {
          return cur;
        }
        cur = cur.next();
      }
      throw new RuntimeException("Should not reach here");
    }
  }

  static class BasicBlock {
    private boolean _changed;              // Reached a fixpoint or not
    static final int _dead_basic_block = -2;
    // Alive but not yet reached by analysis
    static final int _unreached        = -1;
    // >=0: Alive and has a merged state

    int                     _bci;          // Start of basic block
    int                     _end_bci;      // Bci of last instruction in basicblock
    int                     _max_locals;   // Determines split between vars and stack
    int                     _max_stack;    // Determines split between stack and monitors
    CellTypeStateList       _state;        // State (vars, stack) at entry.
    int                     _stack_top;    // -1 indicates bottom stack value.
    int                     _monitor_top;  // -1 indicates bottom monitor stack value.

    CellTypeStateList       vars()  { return _state; }
    CellTypeStateList       stack() { return _state.subList(_max_locals, _state.size()); }

    boolean changed()               { return _changed; }
    void    setChanged(boolean s)   { _changed = s; }

    // Analysis has reached this basicblock
    boolean isReachable()           { return _stack_top >= 0; }

    // All basicblocks that are unreachable are going to have a _stack_top == _dead_basic_block.
    // This info. is setup in a pre-parse before the real abstract interpretation starts.
    boolean isDead()                { return _stack_top == _dead_basic_block; }
    boolean isAlive()               { return _stack_top != _dead_basic_block; }
    void    markAsAlive()           {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(isDead(), "must be dead");
        _stack_top = _unreached;
      }
    }
  }

  //----------------------------------------------------------------------
  // Protected routines for GenerateOopMap
  //

  // _monitor_top is set to this constant to indicate that a monitor matching
  // problem was encountered prior to this point in control flow.
  protected static final int bad_monitors = -1;

  // Main variables
  Method   _method;         // The method we are examining
  RetTable _rt;             // Contains the return address mappings
  int      _max_locals;     // Cached value of no. of locals
  int      _max_stack;      // Cached value of max. stack depth
  int      _max_monitors;   // Cached value of max. monitor stack depth
  boolean  _has_exceptions; // True, if exceptions exist for method
  boolean  _got_error;      // True, if an error occured during interpretation.
  String   _error_msg;      // Error message. Set if _got_error is true.
  //  bool     _did_rewriting;  // was bytecodes rewritten
  //  bool     _did_relocation; // was relocation neccessary
  boolean  _monitor_safe;   // The monitors in this method have been determined
                            // to be safe.

  // Working Cell type state
  int               _state_len;     // Size of states
  CellTypeStateList _state;         // list of states
  char[]            _state_vec_buf; // Buffer used to print a readable version of a state
  int               _stack_top;
  int               _monitor_top;

  // Timing and statistics
  //  static elapsedTimer _total_oopmap_time;   // Holds cumulative oopmap generation time
  //  static long         _total_byte_count;    // Holds cumulative number of bytes inspected

  // Monitor query logic
  int _report_for_exit_bci;
  int _matching_enter_bci;

  // Cell type methods
  void            initState() {
    _state_len = _max_locals + _max_stack + _max_monitors;
    _state     = new CellTypeStateList(_state_len);
    _state_vec_buf = new char[Math.max(_max_locals, Math.max(_max_stack, Math.max(_max_monitors, 1)))];
  }
  void            makeContextUninitialized () {
    CellTypeStateList vs = vars();

    for (int i = 0; i < _max_locals; i++)
      vs.get(i).set(CellTypeState.uninit);

    _stack_top = 0;
    _monitor_top = 0;
  }

  int             methodsigToEffect          (Symbol signature, boolean isStatic, CellTypeStateList effect) {
    ComputeEntryStack ces = new ComputeEntryStack(signature);
    return ces.computeForParameters(isStatic, effect);
  }

  boolean         mergeStateVectors          (CellTypeStateList cts, CellTypeStateList bbts) {
    int i;
    int len = _max_locals + _stack_top;
    boolean change = false;

    for (i = len - 1; i >= 0; i--) {
      CellTypeState v = cts.get(i).merge(bbts.get(i), i);
      change = change || !v.equal(bbts.get(i));
      bbts.get(i).set(v);
    }

    if (_max_monitors > 0 && _monitor_top != bad_monitors) {
      // If there are no monitors in the program, or there has been
      // a monitor matching error before this point in the program,
      // then we do not merge in the monitor state.

      int base = _max_locals + _max_stack;
      len = base + _monitor_top;
      for (i = len - 1; i >= base; i--) {
        CellTypeState v = cts.get(i).merge(bbts.get(i), i);

        // Can we prove that, when there has been a change, it will already
        // have been detected at this point?  That would make this equal
        // check here unnecessary.
        change = change || !v.equal(bbts.get(i));
        bbts.get(i).set(v);
      }
    }

    return change;
  }

  void            copyState                  (CellTypeStateList dst, CellTypeStateList src) {
    int len = _max_locals + _stack_top;
    for (int i = 0; i < len; i++) {
      if (src.get(i).isNonlockReference()) {
        dst.get(i).set(CellTypeState.makeSlotRef(i));
      } else {
        dst.get(i).set(src.get(i));
      }
    }
    if (_max_monitors > 0 && _monitor_top != bad_monitors) {
      int base = _max_locals + _max_stack;
      len = base + _monitor_top;
      for (int i = base; i < len; i++) {
        dst.get(i).set(src.get(i));
      }
    }
  }

  void            mergeStateIntoBB           (BasicBlock bb) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bb.isAlive(), "merging state into a dead basicblock");
    }

    if (_stack_top == bb._stack_top) {
      if (_monitor_top == bb._monitor_top) {
        if (mergeStateVectors(_state, bb._state)) {
          bb.setChanged(true);
        }
      } else {
        if (TraceMonitorMismatch) {
          reportMonitorMismatch("monitor stack height merge conflict");
        }
        // When the monitor stacks are not matched, we set _monitor_top to
        // bad_monitors.  This signals that, from here on, the monitor stack cannot
        // be trusted.  In particular, monitorexit bytecodes may throw
        // exceptions.  We mark this block as changed so that the change
        // propagates properly.
        bb._monitor_top = bad_monitors;
        bb.setChanged(true);
        _monitor_safe = false;
      }
    } else if (!bb.isReachable()) {
      // First time we look at this  BB
      copyState(bb._state, _state);
      bb._stack_top = _stack_top;
      bb._monitor_top = _monitor_top;
      bb.setChanged(true);
    } else {
      throw new RuntimeException("stack height conflict: " +
                                 _stack_top + " vs. " + bb._stack_top);
    }
  }

  void            mergeState                 (int bci, int[] data) {
    mergeStateIntoBB(getBasicBlockAt(bci));
  }

  void            setVar                     (int localNo, CellTypeState cts) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(cts.isReference() || cts.isValue() || cts.isAddress(),
                  "wrong celltypestate");
    }
    if (localNo < 0 || localNo > _max_locals) {
      throw new RuntimeException("variable write error: r" + localNo);
    }
    vars().get(localNo).set(cts);
  }

  CellTypeState   getVar                     (int localNo) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(localNo < _max_locals + _nof_refval_conflicts, "variable read error");
    }
    if (localNo < 0 || localNo > _max_locals) {
      throw new RuntimeException("variable read error: r" + localNo);
    }
    return vars().get(localNo).copy();
  }

  CellTypeState   pop                        () {
    if ( _stack_top <= 0) {
      throw new RuntimeException("stack underflow");
    }
    return  stack().get(--_stack_top).copy();
  }

  void            push                       (CellTypeState cts) {
    if ( _stack_top >= _max_stack) {
      if (DEBUG) {
        System.err.println("Method: " + method().getName().asString() + method().getSignature().asString() +
                           " _stack_top: " + _stack_top + " _max_stack: " + _max_stack);
      }
      throw new RuntimeException("stack overflow");
    }
    stack().get(_stack_top++).set(cts);
    if (DEBUG) {
      System.err.println("After push: _stack_top: " + _stack_top +
                         " _max_stack: " + _max_stack +
                         " just pushed: " + cts.toChar());
    }
  }

  CellTypeState   monitorPop                 () {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(_monitor_top != bad_monitors, "monitorPop called on error monitor stack");
    }
    if (_monitor_top == 0) {
      // We have detected a pop of an empty monitor stack.
      _monitor_safe = false;
      _monitor_top = bad_monitors;

      if (TraceMonitorMismatch) {
        reportMonitorMismatch("monitor stack underflow");
      }
      return CellTypeState.ref; // just to keep the analysis going.
    }
    return  monitors().get(--_monitor_top).copy();
  }

  void            monitorPush                (CellTypeState cts) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(_monitor_top != bad_monitors, "monitorPush called on error monitor stack");
    }
    if (_monitor_top >= _max_monitors) {
      // Some monitorenter is being executed more than once.
      // This means that the monitor stack cannot be simulated.
      _monitor_safe = false;
      _monitor_top = bad_monitors;

      if (TraceMonitorMismatch) {
        reportMonitorMismatch("monitor stack overflow");
      }
      return;
    }
    monitors().get(_monitor_top++).set(cts);
  }

  CellTypeStateList vars    ()     { return _state; }
  CellTypeStateList stack   ()     { return _state.subList(_max_locals, _state.size()); }
  CellTypeStateList monitors()     { return _state.subList(_max_locals+_max_stack, _state.size()); }

  void            replaceAllCTSMatches       (CellTypeState match,
                                              CellTypeState replace) {
    int i;
    int len = _max_locals + _stack_top;
    boolean change = false;

    for (i = len - 1; i >= 0; i--) {
      if (match.equal(_state.get(i))) {
        _state.get(i).set(replace);
      }
    }

    if (_monitor_top > 0) {
      int base = _max_locals + _max_stack;
      len = base + _monitor_top;
      for (i = len - 1; i >= base; i--) {
        if (match.equal(_state.get(i))) {
          _state.get(i).set(replace);
        }
      }
    }
  }

  void            printStates                (PrintStream tty, CellTypeStateList vector, int num) {
    for (int i = 0; i < num; i++) {
      vector.get(i).print(tty);
    }
  }

  void            printCurrentState          (PrintStream tty,
                                              BytecodeStream currentBC,
                                              boolean        detailed) {
    if (detailed) {
      tty.print("     " + currentBC.bci() + " vars     = ");
      printStates(tty, vars(), _max_locals);
      tty.print("    " + Bytecodes.name(currentBC.code()));
      switch(currentBC.code()) {
      case Bytecodes._invokevirtual:
      case Bytecodes._invokespecial:
      case Bytecodes._invokestatic:
      case Bytecodes._invokeinterface:
      case Bytecodes._invokedynamic:
        // FIXME: print signature of referenced method (need more
        // accessors in ConstantPool and ConstantPoolCache)
        int idx = currentBC.hasIndexU4() ? currentBC.getIndexU4() : currentBC.getIndexU2();
        tty.print(" idx " + idx);
        /*
          int idx = currentBC.getIndexU2();
          ConstantPool cp       = method().getConstants();
          int nameAndTypeIdx    = cp.name_and_type_ref_index_at(idx);
          int signatureIdx      = cp.signature_ref_index_at(nameAndTypeIdx);
          Symbol* signature     = cp.symbol_at(signatureIdx);
          tty.print("%s", signature.as_C_string());
        */
      }
      tty.println();
      tty.print("          stack    = ");
      printStates(tty, stack(), _stack_top);
      tty.println();
      if (_monitor_top != bad_monitors) {
        tty.print("          monitors = ");
        printStates(tty, monitors(), _monitor_top);
      } else {
        tty.print("          [bad monitor stack]");
      }
      tty.println();
    } else {
      tty.print("    " + currentBC.bci() + "  vars = '" +
                stateVecToString(vars(), _max_locals) + "' ");
      tty.print("     stack = '" + stateVecToString(stack(), _stack_top) + "' ");
      if (_monitor_top != bad_monitors) {
        tty.print("  monitors = '" + stateVecToString(monitors(), _monitor_top) + "'  \t" +
                  Bytecodes.name(currentBC.code()));
      } else {
        tty.print("  [bad monitor stack]");
      }
      switch(currentBC.code()) {
      case Bytecodes._invokevirtual:
      case Bytecodes._invokespecial:
      case Bytecodes._invokestatic:
      case Bytecodes._invokeinterface:
      case Bytecodes._invokedynamic:
        // FIXME: print signature of referenced method (need more
        // accessors in ConstantPool and ConstantPoolCache)
        int idx = currentBC.hasIndexU4() ? currentBC.getIndexU4() : currentBC.getIndexU2();
        tty.print(" idx " + idx);
        /*
          int idx = currentBC.getIndexU2();
          ConstantPool* cp    = method().constants();
          int nameAndTypeIdx    = cp.name_and_type_ref_index_at(idx);
          int signatureIdx      = cp.signature_ref_index_at(nameAndTypeIdx);
          Symbol* signature     = cp.symbol_at(signatureIdx);
          tty.print("%s", signature.as_C_string());
        */
      }
      tty.println();
    }
  }

  void            reportMonitorMismatch      (String msg) {
    if (Assert.ASSERTS_ENABLED) {
      System.err.print("    Monitor mismatch in method ");
      method().printValueOn(System.err);
      System.err.println(": " + msg);
    }
  }

  // Basicblock info
  BasicBlock[]    _basic_blocks;             // Array of basicblock info
  int             _gc_points;
  int             _bb_count;
  BitMap          _bb_hdr_bits;

  // Basicblocks methods
  void          initializeBB               () {
    _gc_points = 0;
    _bb_count  = 0;
    _bb_hdr_bits = new BitMap((int) _method.getCodeSize());
  }

  void          markBBHeadersAndCountGCPoints() {
    initializeBB();

    boolean fellThrough = false;  // False to get first BB marked.

    // First mark all exception handlers as start of a basic-block
    if (method().hasExceptionTable()) {
      ExceptionTableElement[] excps = method().getExceptionTable();
      for(int i = 0; i < excps.length; i++) {
        markBB(excps[i].getHandlerPC(), null);
      }
    }

    // Then iterate through the code
    BytecodeStream bcs = new BytecodeStream(_method);
    int bytecode;

    while( (bytecode = bcs.next()) >= 0) {
      int bci = bcs.bci();

      if (!fellThrough)
        markBB(bci, null);

      fellThrough = jumpTargetsDo(bcs,
                                  new JumpClosure() {
                                      public void process(GenerateOopMap c, int bcpDelta, int[] data) {
                                        c.markBB(bcpDelta, data);
                                      }
                                    },
                                  null);

      /* We will also mark successors of jsr's as basic block headers. */
      switch (bytecode) {
      case Bytecodes._jsr:
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(!fellThrough, "should not happen");
        }
        markBB(bci + Bytecodes.lengthFor(bytecode), null);
        break;
      case Bytecodes._jsr_w:
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(!fellThrough, "should not happen");
        }
        markBB(bci + Bytecodes.lengthFor(bytecode), null);
        break;
      }

      if (possibleGCPoint(bcs))
        _gc_points++;
    }
  }

  boolean       isBBHeader                  (int bci) {
    return _bb_hdr_bits.at(bci);
  }

  int           gcPoints                    () {
    return _gc_points;
  }

  int           bbCount                     () {
    return _bb_count;
  }

  void          setBBMarkBit                (int bci) {
    _bb_hdr_bits.atPut(bci, true);
  }

  void          clear_bbmark_bit            (int bci) {
    _bb_hdr_bits.atPut(bci, false);
  }

  BasicBlock    getBasicBlockAt             (int bci) {
    BasicBlock bb = getBasicBlockContaining(bci);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bb._bci == bci, "should have found BB");
    }
    return bb;
  }

  BasicBlock    getBasicBlockContaining     (int bci) {
    BasicBlock[] bbs = _basic_blocks;
    int lo = 0, hi = _bb_count - 1;

    while (lo <= hi) {
      int m = (lo + hi) / 2;
      int mbci = bbs[m]._bci;
      int nbci;

      if ( m == _bb_count-1) {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that( bci >= mbci && bci < method().getCodeSize(), "sanity check failed");
        }
        return bbs[m];
      } else {
        nbci = bbs[m+1]._bci;
      }

      if ( mbci <= bci && bci < nbci) {
        return bbs[m];
      } else if (mbci < bci) {
        lo = m + 1;
      } else {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(mbci > bci, "sanity check");
        }
        hi = m - 1;
      }
    }

    throw new RuntimeException("should have found BB");
  }

  void          interpBB                    (BasicBlock bb) {
    // We do not want to do anything in case the basic-block has not been initialized. This
    // will happen in the case where there is dead-code hang around in a method.
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bb.isReachable(), "should be reachable or deadcode exist");
    }
    restoreState(bb);

    BytecodeStream itr = new BytecodeStream(_method);

    // Set iterator interval to be the current basicblock
    int lim_bci = nextBBStartPC(bb);
    itr.setInterval(bb._bci, lim_bci);

    if (DEBUG) {
      System.err.println("interpBB: method = " + method().getName().asString() +
                         method().getSignature().asString() +
                         ", BCI interval [" + bb._bci + ", " + lim_bci + ")");
      {
        System.err.print("Bytecodes:");
        for (int i = bb._bci; i < lim_bci; i++) {
          System.err.print(" 0x" + Long.toHexString(method().getBytecodeOrBPAt(i)));
        }
        System.err.println();
      }
    }

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(lim_bci != bb._bci, "must be at least one instruction in a basicblock");
    }
    itr.next(); // read first instruction

    // Iterates through all bytecodes except the last in a basic block.
    // We handle the last one special, since there is controlflow change.
    while(itr.nextBCI() < lim_bci && !_got_error) {
      if (_has_exceptions || (_monitor_top != 0)) {
        // We do not need to interpret the results of exceptional
        // continuation from this instruction when the method has no
        // exception handlers and the monitor stack is currently
        // empty.
        doExceptionEdge(itr);
      }
      interp1(itr);
      itr.next();
    }

    // Handle last instruction.
    if (!_got_error) {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(itr.nextBCI() == lim_bci, "must point to end");
      }
      if (_has_exceptions || (_monitor_top != 0)) {
        doExceptionEdge(itr);
      }
      interp1(itr);

      boolean fall_through = jumpTargetsDo(itr, new JumpClosure() {
          public void process(GenerateOopMap c, int bcpDelta, int[] data) {
            c.mergeState(bcpDelta, data);
          }
        }, null);
      if (_got_error)  return;

      if (itr.code() == Bytecodes._ret) {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(!fall_through, "cannot be set if ret instruction");
        }
        // Automatically handles 'wide' ret indicies
        retJumpTargetsDo(itr, new JumpClosure() {
            public void process(GenerateOopMap c, int bcpDelta, int[] data) {
              c.mergeState(bcpDelta, data);
            }
          }, itr.getIndex(), null);
      } else if (fall_through) {
        // Hit end of BB, but the instr. was a fall-through instruction,
        // so perform transition as if the BB ended in a "jump".
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(lim_bci == _basic_blocks[bbIndex(bb) + 1]._bci, "there must be another bb");
        }
        mergeStateIntoBB(_basic_blocks[bbIndex(bb) + 1]);
      }
    }
  }

  void          restoreState                (BasicBlock bb) {
    for (int i = 0; i < _state_len; i++) {
      _state.get(i).set(bb._state.get(i));
    }
    _stack_top   = bb._stack_top;
    _monitor_top = bb._monitor_top;
  }

  int           nextBBStartPC               (BasicBlock bb) {
    int bbNum = bbIndex(bb) + 1;
    if (bbNum == _bb_count)
      return (int) method().getCodeSize();

    return _basic_blocks[bbNum]._bci;
  }

  void          updateBasicBlocks           (int bci, int delta) {
    BitMap bbBits = new BitMap((int) (_method.getCodeSize() + delta));
    for(int k = 0; k < _bb_count; k++) {
      if (_basic_blocks[k]._bci > bci) {
        _basic_blocks[k]._bci     += delta;
        _basic_blocks[k]._end_bci += delta;
      }
      bbBits.atPut(_basic_blocks[k]._bci, true);
    }
    _bb_hdr_bits = bbBits;
  }

  void markBB(int bci, int[] data) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bci>= 0 && bci < method().getCodeSize(), "index out of bounds");
    }
    if (isBBHeader(bci))
      return;

    // FIXME: remove
    //    if (TraceNewOopMapGeneration) {
    //      tty.print_cr("Basicblock#%d begins at: %d", c._bb_count, bci);
    //    }
    setBBMarkBit(bci);
    _bb_count++;
  }

  // Dead code detection
  void          markReachableCode() {
    final int[] change = new int[1];
    change[0] = 1;

    // Mark entry basic block as alive and all exception handlers
    _basic_blocks[0].markAsAlive();
    if (method().hasExceptionTable()) {
      ExceptionTableElement[] excps = method().getExceptionTable();
      for(int i = 0; i < excps.length; i ++) {
        BasicBlock bb = getBasicBlockAt(excps[i].getHandlerPC());
        // If block is not already alive (due to multiple exception handlers to same bb), then
        // make it alive
        if (bb.isDead())
          bb.markAsAlive();
      }
    }

    BytecodeStream bcs = new BytecodeStream(_method);

    // Iterate through all basic blocks until we reach a fixpoint
    while (change[0] != 0) {
      change[0] = 0;

      for (int i = 0; i < _bb_count; i++) {
        BasicBlock bb = _basic_blocks[i];
        if (bb.isAlive()) {
          // Position bytecodestream at last bytecode in basicblock
          bcs.setStart(bb._end_bci);
          bcs.next();
          int bytecode = bcs.code();
          int bci = bcs.bci();
          if (Assert.ASSERTS_ENABLED) {
            Assert.that(bci == bb._end_bci, "wrong bci");
          }

          boolean fell_through = jumpTargetsDo(bcs, new JumpClosure() {
              public void process(GenerateOopMap c, int bciDelta, int[] change) {
                c.reachableBasicblock(bciDelta, change);
              }
            }, change);

          // We will also mark successors of jsr's as alive.
          switch (bytecode) {
          case Bytecodes._jsr:
          case Bytecodes._jsr_w:
            if (Assert.ASSERTS_ENABLED) {
              Assert.that(!fell_through, "should not happen");
            }
            reachableBasicblock(bci + Bytecodes.lengthFor(bytecode), change);
            break;
          }
          if (fell_through) {
            // Mark successor as alive
            if (_basic_blocks[i+1].isDead()) {
              _basic_blocks[i+1].markAsAlive();
              change[0] = 1;
            }
          }
        }
      }
    }
  }

  void  reachableBasicblock        (int bci, int[] data) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bci>= 0 && bci < method().getCodeSize(), "index out of bounds");
    }
    BasicBlock bb = getBasicBlockAt(bci);
    if (bb.isDead()) {
      bb.markAsAlive();
      data[0] = 1; // Mark basicblock as changed
    }
  }

  // Interpretation methods (primary)
  void  doInterpretation                    () {
    // "i" is just for debugging, so we can detect cases where this loop is
    // iterated more than once.
    int i = 0;
    do {
      // FIXME: remove
      //      if (TraceNewOopMapGeneration) {
      //        tty.print("\n\nIteration #%d of do_interpretation loop, method:\n", i);
      //        method().print_name(tty);
      //        tty.print("\n\n");
      //      }
      _conflict = false;
      _monitor_safe = true;
      // init_state is now called from init_basic_blocks.  The length of a
      // state vector cannot be determined until we have made a pass through
      // the bytecodes counting the possible monitor entries.
      if (!_got_error) initBasicBlocks();
      if (!_got_error) setupMethodEntryState();
      if (!_got_error) interpAll();
      if (!_got_error) rewriteRefvalConflicts();
      i++;
    } while (_conflict && !_got_error);
  }

  void  initBasicBlocks                     () {
    // Note: Could consider reserving only the needed space for each BB's state
    // (entry stack may not be of maximal height for every basic block).
    // But cumbersome since we don't know the stack heights yet.  (Nor the
    // monitor stack heights...)

    _basic_blocks = new BasicBlock[_bb_count];
    for (int i = 0; i < _bb_count; i++) {
      _basic_blocks[i] = new BasicBlock();
    }

    // Make a pass through the bytecodes.  Count the number of monitorenters.
    // This can be used an upper bound on the monitor stack depth in programs
    // which obey stack discipline with their monitor usage.  Initialize the
    // known information about basic blocks.
    BytecodeStream j = new BytecodeStream(_method);
    int bytecode;

    int bbNo = 0;
    int monitor_count = 0;
    int prev_bci = -1;
    while( (bytecode = j.next()) >= 0) {
      if (j.code() == Bytecodes._monitorenter) {
        monitor_count++;
      }

      int bci = j.bci();
      if (isBBHeader(bci)) {
        // Initialize the basicblock structure
        BasicBlock bb    = _basic_blocks[bbNo];
        bb._bci          = bci;
        bb._max_locals   = _max_locals;
        bb._max_stack    = _max_stack;
        bb.setChanged(false);
        bb._stack_top    = BasicBlock._dead_basic_block; // Initialize all basicblocks are dead.
        bb._monitor_top  = bad_monitors;

        if (bbNo > 0) {
          _basic_blocks[bbNo - 1]._end_bci = prev_bci;
        }

        bbNo++;
      }
      // Remember prevous bci.
      prev_bci = bci;
    }
    // Set
    _basic_blocks[bbNo-1]._end_bci = prev_bci;

    _max_monitors = monitor_count;

    // Now that we have a bound on the depth of the monitor stack, we can
    // initialize the CellTypeState-related information.
    initState();

    // We allocate space for all state-vectors for all basicblocks in one huge chuck.
    // Then in the next part of the code, we set a pointer in each _basic_block that
    // points to each piece.
    CellTypeStateList basicBlockState = new CellTypeStateList(bbNo * _state_len);

    // Make a pass over the basicblocks and assign their state vectors.
    for (int blockNum=0; blockNum < bbNo; blockNum++) {
      BasicBlock bb = _basic_blocks[blockNum];
      bb._state = basicBlockState.subList(blockNum * _state_len, (blockNum + 1) * _state_len);

      if (Assert.ASSERTS_ENABLED) {
        if (blockNum + 1 < bbNo) {
          int bc_len = Bytecodes.javaLengthAt(_method, bb._end_bci);
          Assert.that(bb._end_bci + bc_len == _basic_blocks[blockNum + 1]._bci,
                      "unmatched bci info in basicblock");
        }
      }
    }
    if (Assert.ASSERTS_ENABLED) {
      BasicBlock bb = _basic_blocks[bbNo-1];
      int bc_len = Bytecodes.javaLengthAt(_method, bb._end_bci);
      Assert.that(bb._end_bci + bc_len == _method.getCodeSize(), "wrong end bci");
    }

    // Check that the correct number of basicblocks was found
    if (bbNo !=_bb_count) {
      if (bbNo < _bb_count) {
        throw new RuntimeException("jump into the middle of instruction?");
      } else {
        throw new RuntimeException("extra basic blocks - should not happen?");
      }
    }

    // Mark all alive blocks
    markReachableCode();
  }

  void  setupMethodEntryState               () {
    // Initialize all locals to 'uninit' and set stack-height to 0
    makeContextUninitialized();

    // Initialize CellState type of arguments
    methodsigToEffect(method().getSignature(), method().isStatic(), vars());

    // If some references must be pre-assigned to null, then set that up
    initializeVars();

    // This is the start state
    mergeStateIntoBB(_basic_blocks[0]);

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(_basic_blocks[0].changed(), "we are not getting off the ground");
    }
  }

  void  interpAll                           () {
    boolean change = true;

    while (change && !_got_error) {
      change = false;
      for (int i = 0; i < _bb_count && !_got_error; i++) {
        BasicBlock bb = _basic_blocks[i];
        if (bb.changed()) {
          if (_got_error) return;
          change = true;
          bb.setChanged(false);
          interpBB(bb);
        }
      }
    }
  }

  //
  // Interpretation methods (secondary)
  //

  /** Sets the current state to be the state after executing the
      current instruction, starting in the current state. */
  void  interp1                             (BytecodeStream itr) {
    if (DEBUG) {
      System.err.println(" - bci " + itr.bci() + " " + itr.code());
      printCurrentState(System.err, itr, false);
    }

    //    if (TraceNewOopMapGeneration) {
    //      print_current_state(tty, itr, TraceNewOopMapGenerationDetailed);
    //    }

    // Should we report the results? Result is reported *before* the
    // instruction at the current bci is executed.  However, not for
    // calls. For calls we do not want to include the arguments, so we
    // postpone the reporting until they have been popped (in method
    // ppl).
    if (_report_result == true) {
      switch(itr.code()) {
      case Bytecodes._invokevirtual:
      case Bytecodes._invokespecial:
      case Bytecodes._invokestatic:
      case Bytecodes._invokeinterface:
      case Bytecodes._invokedynamic:
        _itr_send = itr;
        _report_result_for_send = true;
        break;
      default:
        fillStackmapForOpcodes(itr, vars(), stack(), _stack_top);
        break;
      }
    }

    // abstract interpretation of current opcode
    switch(itr.code()) {
    case Bytecodes._nop:               break;
    case Bytecodes._goto:              break;
    case Bytecodes._goto_w:            break;
    case Bytecodes._iinc:              break;
    case Bytecodes._return:            doReturnMonitorCheck();
      break;

    case Bytecodes._aconst_null:
    case Bytecodes._new:               ppush1(CellTypeState.makeLineRef(itr.bci()));
      break;

    case Bytecodes._iconst_m1:
    case Bytecodes._iconst_0:
    case Bytecodes._iconst_1:
    case Bytecodes._iconst_2:
    case Bytecodes._iconst_3:
    case Bytecodes._iconst_4:
    case Bytecodes._iconst_5:
    case Bytecodes._fconst_0:
    case Bytecodes._fconst_1:
    case Bytecodes._fconst_2:
    case Bytecodes._bipush:
    case Bytecodes._sipush:            ppush1(valCTS);             break;

    case Bytecodes._lconst_0:
    case Bytecodes._lconst_1:
    case Bytecodes._dconst_0:
    case Bytecodes._dconst_1:          ppush(vvCTS);               break;

    case Bytecodes._ldc2_w:            ppush(vvCTS);               break;

    case Bytecodes._ldc:               doLdc(itr.bci());           break;
    case Bytecodes._ldc_w:             doLdc(itr.bci());           break;

    case Bytecodes._iload:
    case Bytecodes._fload:             ppload(vCTS, itr.getIndex()); break;

    case Bytecodes._lload:
    case Bytecodes._dload:             ppload(vvCTS,itr.getIndex()); break;

    case Bytecodes._aload:             ppload(rCTS, itr.getIndex()); break;

    case Bytecodes._iload_0:
    case Bytecodes._fload_0:           ppload(vCTS, 0);            break;
    case Bytecodes._iload_1:
    case Bytecodes._fload_1:           ppload(vCTS, 1);            break;
    case Bytecodes._iload_2:
    case Bytecodes._fload_2:           ppload(vCTS, 2);            break;
    case Bytecodes._iload_3:
    case Bytecodes._fload_3:           ppload(vCTS, 3);            break;

    case Bytecodes._lload_0:
    case Bytecodes._dload_0:           ppload(vvCTS, 0);           break;
    case Bytecodes._lload_1:
    case Bytecodes._dload_1:           ppload(vvCTS, 1);           break;
    case Bytecodes._lload_2:
    case Bytecodes._dload_2:           ppload(vvCTS, 2);           break;
    case Bytecodes._lload_3:
    case Bytecodes._dload_3:           ppload(vvCTS, 3);           break;

    case Bytecodes._aload_0:           ppload(rCTS, 0);            break;
    case Bytecodes._aload_1:           ppload(rCTS, 1);            break;
    case Bytecodes._aload_2:           ppload(rCTS, 2);            break;
    case Bytecodes._aload_3:           ppload(rCTS, 3);            break;

    case Bytecodes._iaload:
    case Bytecodes._faload:
    case Bytecodes._baload:
    case Bytecodes._caload:
    case Bytecodes._saload:            pp(vrCTS, vCTS); break;

    case Bytecodes._laload:            pp(vrCTS, vvCTS);  break;
    case Bytecodes._daload:            pp(vrCTS, vvCTS); break;

    case Bytecodes._aaload:            ppNewRef(vrCTS, itr.bci()); break;

    case Bytecodes._istore:
    case Bytecodes._fstore:            ppstore(vCTS, itr.getIndex()); break;

    case Bytecodes._lstore:
    case Bytecodes._dstore:            ppstore(vvCTS, itr.getIndex()); break;

    case Bytecodes._astore:            doAstore(itr.getIndex());     break;

    case Bytecodes._istore_0:
    case Bytecodes._fstore_0:          ppstore(vCTS, 0);           break;
    case Bytecodes._istore_1:
    case Bytecodes._fstore_1:          ppstore(vCTS, 1);           break;
    case Bytecodes._istore_2:
    case Bytecodes._fstore_2:          ppstore(vCTS, 2);           break;
    case Bytecodes._istore_3:
    case Bytecodes._fstore_3:          ppstore(vCTS, 3);           break;

    case Bytecodes._lstore_0:
    case Bytecodes._dstore_0:          ppstore(vvCTS, 0);          break;
    case Bytecodes._lstore_1:
    case Bytecodes._dstore_1:          ppstore(vvCTS, 1);          break;
    case Bytecodes._lstore_2:
    case Bytecodes._dstore_2:          ppstore(vvCTS, 2);          break;
    case Bytecodes._lstore_3:
    case Bytecodes._dstore_3:          ppstore(vvCTS, 3);          break;

    case Bytecodes._astore_0:          doAstore(0);                break;
    case Bytecodes._astore_1:          doAstore(1);                break;
    case Bytecodes._astore_2:          doAstore(2);                break;
    case Bytecodes._astore_3:          doAstore(3);                break;

    case Bytecodes._iastore:
    case Bytecodes._fastore:
    case Bytecodes._bastore:
    case Bytecodes._castore:
    case Bytecodes._sastore:           ppop(vvrCTS);               break;
    case Bytecodes._lastore:
    case Bytecodes._dastore:           ppop(vvvrCTS);              break;
    case Bytecodes._aastore:           ppop(rvrCTS);               break;

    case Bytecodes._pop:               ppopAny(1);                 break;
    case Bytecodes._pop2:              ppopAny(2);                 break;

    case Bytecodes._dup:               ppdupswap(1, "11");         break;
    case Bytecodes._dup_x1:            ppdupswap(2, "121");        break;
    case Bytecodes._dup_x2:            ppdupswap(3, "1321");       break;
    case Bytecodes._dup2:              ppdupswap(2, "2121");       break;
    case Bytecodes._dup2_x1:           ppdupswap(3, "21321");      break;
    case Bytecodes._dup2_x2:           ppdupswap(4, "214321");     break;
    case Bytecodes._swap:              ppdupswap(2, "12");         break;

    case Bytecodes._iadd:
    case Bytecodes._fadd:
    case Bytecodes._isub:
    case Bytecodes._fsub:
    case Bytecodes._imul:
    case Bytecodes._fmul:
    case Bytecodes._idiv:
    case Bytecodes._fdiv:
    case Bytecodes._irem:
    case Bytecodes._frem:
    case Bytecodes._ishl:
    case Bytecodes._ishr:
    case Bytecodes._iushr:
    case Bytecodes._iand:
    case Bytecodes._ior:
    case Bytecodes._ixor:
    case Bytecodes._l2f:
    case Bytecodes._l2i:
    case Bytecodes._d2f:
    case Bytecodes._d2i:
    case Bytecodes._fcmpl:
    case Bytecodes._fcmpg:             pp(vvCTS, vCTS); break;

    case Bytecodes._ladd:
    case Bytecodes._dadd:
    case Bytecodes._lsub:
    case Bytecodes._dsub:
    case Bytecodes._lmul:
    case Bytecodes._dmul:
    case Bytecodes._ldiv:
    case Bytecodes._ddiv:
    case Bytecodes._lrem:
    case Bytecodes._drem:
    case Bytecodes._land:
    case Bytecodes._lor:
    case Bytecodes._lxor:              pp(vvvvCTS, vvCTS); break;

    case Bytecodes._ineg:
    case Bytecodes._fneg:
    case Bytecodes._i2f:
    case Bytecodes._f2i:
    case Bytecodes._i2c:
    case Bytecodes._i2s:
    case Bytecodes._i2b:               pp(vCTS, vCTS); break;

    case Bytecodes._lneg:
    case Bytecodes._dneg:
    case Bytecodes._l2d:
    case Bytecodes._d2l:               pp(vvCTS, vvCTS); break;

    case Bytecodes._lshl:
    case Bytecodes._lshr:
    case Bytecodes._lushr:             pp(vvvCTS, vvCTS); break;

    case Bytecodes._i2l:
    case Bytecodes._i2d:
    case Bytecodes._f2l:
    case Bytecodes._f2d:               pp(vCTS, vvCTS); break;

    case Bytecodes._lcmp:              pp(vvvvCTS, vCTS); break;
    case Bytecodes._dcmpl:
    case Bytecodes._dcmpg:             pp(vvvvCTS, vCTS); break;

    case Bytecodes._ifeq:
    case Bytecodes._ifne:
    case Bytecodes._iflt:
    case Bytecodes._ifge:
    case Bytecodes._ifgt:
    case Bytecodes._ifle:
    case Bytecodes._tableswitch:       ppop1(valCTS);
      break;
    case Bytecodes._ireturn:
    case Bytecodes._freturn:           doReturnMonitorCheck();
      ppop1(valCTS);
      break;
    case Bytecodes._if_icmpeq:
    case Bytecodes._if_icmpne:
    case Bytecodes._if_icmplt:
    case Bytecodes._if_icmpge:
    case Bytecodes._if_icmpgt:
    case Bytecodes._if_icmple:         ppop(vvCTS);
      break;

    case Bytecodes._lreturn:           doReturnMonitorCheck();
      ppop(vvCTS);
      break;

    case Bytecodes._dreturn:           doReturnMonitorCheck();
      ppop(vvCTS);
      break;

    case Bytecodes._if_acmpeq:
    case Bytecodes._if_acmpne:         ppop(rrCTS);                 break;

    case Bytecodes._jsr:               doJsr(itr.dest());          break;
    case Bytecodes._jsr_w:             doJsr(itr.dest_w());        break;

    case Bytecodes._getstatic:         doField(true,  true,  itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._putstatic:         doField(false, true,  itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._getfield:          doField(true,  false, itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._putfield:          doField(false, false, itr.getIndexU2Cpcache(), itr.bci()); break;

    case Bytecodes._invokevirtual:
    case Bytecodes._invokespecial:     doMethod(false, false, itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._invokestatic:      doMethod(true,  false, itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._invokedynamic:     doMethod(true,  false, itr.getIndexU4(),        itr.bci()); break;
    case Bytecodes._invokeinterface:   doMethod(false,  true, itr.getIndexU2Cpcache(), itr.bci()); break;
    case Bytecodes._newarray:
    case Bytecodes._anewarray:         ppNewRef(vCTS, itr.bci()); break;
    case Bytecodes._checkcast:         doCheckcast(); break;
    case Bytecodes._arraylength:
    case Bytecodes._instanceof:        pp(rCTS, vCTS); break;
    case Bytecodes._monitorenter:      doMonitorenter(itr.bci()); break;
    case Bytecodes._monitorexit:       doMonitorexit(itr.bci()); break;

    case Bytecodes._athrow:            // handled by do_exception_edge() BUT ...
      // vlh(apple): doExceptionEdge() does not get
      // called if method has no exception handlers
      if ((!_has_exceptions) && (_monitor_top > 0)) {
        _monitor_safe = false;
      }
      break;

    case Bytecodes._areturn:           doReturnMonitorCheck();
      ppop1(refCTS);
      break;
    case Bytecodes._ifnull:
    case Bytecodes._ifnonnull:         ppop1(refCTS); break;
    case Bytecodes._multianewarray:    doMultianewarray(itr.codeAt(itr.bci() + 3), itr.bci()); break;

    case Bytecodes._wide:              throw new RuntimeException("Iterator should skip this bytecode");
    case Bytecodes._ret:                                           break;

      // Java opcodes
    case Bytecodes._fast_aaccess_0:     ppNewRef(rCTS, itr.bci()); break; // Pair bytecode for (aload_0, _fast_agetfield)

    case Bytecodes._fast_iaccess_0:     ppush1(valCTS);            break; // Pair bytecode for (aload_0, _fast_igetfield)

    case Bytecodes._fast_igetfield:     pp(rCTS, vCTS);            break;

    case Bytecodes._fast_agetfield:     ppNewRef(rCTS, itr.bci()); break;

    case Bytecodes._fast_aload_0:       ppload(rCTS,  0);          break;

    case Bytecodes._lookupswitch:
    case Bytecodes._fast_linearswitch:
    case Bytecodes._fast_binaryswitch:  ppop1(valCTS);             break;

    default:
      throw new RuntimeException("unexpected opcode: " + itr.code());
    }
  }

  void  doExceptionEdge                     (BytecodeStream itr) {
    // Only check exception edge, if bytecode can trap
    if (!Bytecodes.canTrap(itr.code())) return;
    switch (itr.code()) {
    case Bytecodes._aload_0:
    case Bytecodes._fast_aload_0:
      // These bytecodes can trap for rewriting.  We need to assume that
      // they do not throw exceptions to make the monitor analysis work.
      return;

    case Bytecodes._ireturn:
    case Bytecodes._lreturn:
    case Bytecodes._freturn:
    case Bytecodes._dreturn:
    case Bytecodes._areturn:
    case Bytecodes._return:
      // If the monitor stack height is not zero when we leave the method,
      // then we are either exiting with a non-empty stack or we have
      // found monitor trouble earlier in our analysis.  In either case,
      // assume an exception could be taken here.
      if (_monitor_top == 0) {
        return;
      }
      break;

    case Bytecodes._monitorexit:
      // If the monitor stack height is bad_monitors, then we have detected a
      // monitor matching problem earlier in the analysis.  If the
      // monitor stack height is 0, we are about to pop a monitor
      // off of an empty stack.  In either case, the bytecode
      // could throw an exception.
      if (_monitor_top != bad_monitors && _monitor_top != 0) {
        return;
      }
      break;
    }

    if (_has_exceptions) {
      int bci = itr.bci();
      ExceptionTableElement[] exct   = method().getExceptionTable();
      for(int i = 0; i< exct.length; i++) {
        int start_pc   = exct[i].getStartPC();
        int end_pc     = exct[i].getEndPC();
        int handler_pc = exct[i].getHandlerPC();
        int catch_type = exct[i].getCatchTypeIndex();

        if (start_pc <= bci && bci < end_pc) {
          BasicBlock excBB = getBasicBlockAt(handler_pc);
          CellTypeStateList excStk  = excBB.stack();
          CellTypeStateList cOpStck = stack();
          CellTypeState cOpStck_0 = cOpStck.get(0).copy();
          int cOpStackTop = _stack_top;

          // Exception stacks are always the same.
          if (Assert.ASSERTS_ENABLED) {
            Assert.that(method().getMaxStack() > 0, "sanity check");
          }

          // We remembered the size and first element of "cOpStck"
          // above; now we temporarily set them to the appropriate
          // values for an exception handler.
          cOpStck.get(0).set(CellTypeState.makeSlotRef(_max_locals));
          _stack_top = 1;

          mergeStateIntoBB(excBB);

          // Now undo the temporary change.
          cOpStck.get(0).set(cOpStck_0);
          _stack_top = cOpStackTop;

          // If this is a "catch all" handler, then we do not need to
          // consider any additional handlers.
          if (catch_type == 0) {
            return;
          }
        }
      }
    }

    // It is possible that none of the exception handlers would have caught
    // the exception.  In this case, we will exit the method.  We must
    // ensure that the monitor stack is empty in this case.
    if (_monitor_top == 0) {
      return;
    }

    // We pessimistically assume that this exception can escape the
    // method. (It is possible that it will always be caught, but
    // we don't care to analyse the types of the catch clauses.)

    // We don't set _monitor_top to bad_monitors because there are no successors
    // to this exceptional exit.

    if (TraceMonitorMismatch && _monitor_safe) {
      // We check _monitor_safe so that we only report the first mismatched
      // exceptional exit.
      reportMonitorMismatch("non-empty monitor stack at exceptional exit");
    }
    _monitor_safe = false;
  }

  void  checkType                           (CellTypeState expected, CellTypeState actual) {
    if (!expected.equalKind(actual)) {
      throw new RuntimeException("wrong type on stack (found: " +
                                 actual.toChar() + " expected: " +
                                 expected.toChar() + ")");
    }
  }

  void  ppstore                             (CellTypeState[] in,  int loc_no) {
    for (int i = 0; i < in.length && !in[i].equal(CellTypeState.bottom); i++) {
      CellTypeState expected = in[i];
      CellTypeState actual   = pop();
      checkType(expected, actual);
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(loc_no >= 0, "sanity check");
      }
      setVar(loc_no++, actual);
    }
  }

  void  ppload                              (CellTypeState[] out, int loc_no) {
    for (int i = 0; i < out.length && !out[i].equal(CellTypeState.bottom); i++) {
      CellTypeState out1 = out[i];
      CellTypeState vcts = getVar(loc_no);
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(out1.canBeReference() || out1.canBeValue(),
                    "can only load refs. and values.");
      }
      if (out1.isReference()) {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(loc_no>=0, "sanity check");
        }
        if (!vcts.isReference()) {
          // We were asked to push a reference, but the type of the
          // variable can be something else
          _conflict = true;
          if (vcts.canBeUninit()) {
            // It is a ref-uninit conflict (at least). If there are other
            // problems, we'll get them in the next round
            addToRefInitSet(loc_no);
            vcts = out1;
          } else {
            // It wasn't a ref-uninit conflict. So must be a
            // ref-val or ref-pc conflict. Split the variable.
            recordRefvalConflict(loc_no);
            vcts = out1;
          }
          push(out1); // recover...
        } else {
          push(vcts); // preserve reference.
        }
        // Otherwise it is a conflict, but one that verification would
        // have caught if illegal. In particular, it can't be a topCTS
        // resulting from mergeing two difference pcCTS's since the verifier
        // would have rejected any use of such a merge.
      } else {
        push(out1); // handle val/init conflict
      }
      loc_no++;
    }
  }

  void  ppush1                              (CellTypeState in) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(in.isReference() | in.isValue(), "sanity check");
    }
    if (DEBUG) {
      System.err.println("   - pushing " + in.toChar());
    }
    push(in);
  }

  void  ppush                               (CellTypeState[] in) {
    for (int i = 0; i < in.length && !in[i].equal(CellTypeState.bottom); i++) {
      ppush1(in[i]);
    }
  }

  void  ppush                               (CellTypeStateList in) {
    for (int i = 0; i < in.size() && !in.get(i).equal(CellTypeState.bottom); i++) {
      ppush1(in.get(i));
    }
  }

  void  ppop1                               (CellTypeState out) {
    CellTypeState actual = pop();
    if (DEBUG) {
      System.err.println("   - popping " + actual.toChar() + ", expecting " + out.toChar());
    }
    checkType(out, actual);
  }

  void  ppop                                (CellTypeState[] out) {
    for (int i = 0; i < out.length && !out[i].equal(CellTypeState.bottom); i++) {
      ppop1(out[i]);
    }
  }

  void  ppopAny                             (int poplen) {
    if (_stack_top >= poplen) {
      _stack_top -= poplen;
    } else {
      throw new RuntimeException("stack underflow");
    }
  }

  void  pp                                  (CellTypeState[] in, CellTypeState[] out) {
    ppop(in);
    ppush(out);
  }

  void  ppNewRef                            (CellTypeState[] in, int bci) {
    ppop(in);
    ppush1(CellTypeState.makeLineRef(bci));
  }

  void  ppdupswap                           (int poplen, String out) {
    CellTypeState[] actual = new CellTypeState[5];
    Assert.that(poplen < 5, "this must be less than length of actual vector");

    // pop all arguments
    for(int i = 0; i < poplen; i++) actual[i] = pop();

    // put them back
    for (int i = 0; i < out.length(); i++) {
      char push_ch = out.charAt(i);
      int idx = push_ch - '1';
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(idx >= 0 && idx < poplen, "wrong arguments");
      }
      push(actual[idx]);
    }
  }

  void  doLdc                               (int bci) {
    BytecodeLoadConstant ldc = BytecodeLoadConstant.at(_method, bci);
    ConstantPool  cp  = method().getConstants();
    BasicType     bt = ldc.resultType();
    CellTypeState cts = (bt == BasicType.T_OBJECT) ? CellTypeState.makeLineRef(bci) : valCTS;
    ppush1(cts);
  }

  void  doAstore                            (int idx) {
    CellTypeState r_or_p = pop();
    if (!r_or_p.isAddress() && !r_or_p.isReference()) {
      // We actually expected ref or pc, but we only report that we expected a ref. It does not
      // really matter (at least for now)
      throw new RuntimeException("wrong type on stack (found: " +
                                 r_or_p.toChar() + ", expected: {pr})");
    }
    setVar(idx, r_or_p);
  }

  void  doJsr                               (int targBCI) {
    push(CellTypeState.makeAddr(targBCI));
  }

  void  doField                             (boolean is_get, boolean is_static, int idx, int bci) {
    // Dig up signature for field in constant pool
    ConstantPool cp        = method().getConstants();
    int nameAndTypeIdx     = cp.getNameAndTypeRefIndexAt(idx);
    int signatureIdx       = cp.getSignatureRefIndexAt(nameAndTypeIdx);
    Symbol signature       = cp.getSymbolAt(signatureIdx);

    if (DEBUG) {
      System.err.println("doField: signature = " + signature.asString() + ", idx = " + idx +
                         ", nameAndTypeIdx = " + nameAndTypeIdx + ", signatureIdx = " + signatureIdx + ", bci = " + bci);
    }

    // Parse signature (espcially simple for fields)
    // The signature is UFT8 encoded, but the first char is always ASCII for signatures.
    char sigch = (char) signature.getByteAt(0);
    CellTypeState[] temp = new CellTypeState[4];
    CellTypeState[] eff  = sigcharToEffect(sigch, bci, temp);

    CellTypeState[] in = new CellTypeState[4];
    CellTypeState[] out;
    int i =  0;

    if (is_get) {
      out = eff;
    } else {
      out = epsilonCTS;
      i   = copyCTS(in, eff);
    }
    if (!is_static) in[i++] = CellTypeState.ref;
    in[i] = CellTypeState.bottom;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(i<=3, "sanity check");
    }
    pp(in, out);
  }

  void  doMethod                            (boolean is_static, boolean is_interface, int idx, int bci) {
    // Dig up signature for field in constant pool
    ConstantPool cp       = _method.getConstants();
    Symbol signature      = cp.getSignatureRefAt(idx);

    // Parse method signature
    CellTypeStateList out = new CellTypeStateList(4);
    CellTypeStateList in  = new CellTypeStateList(MAXARGSIZE+1);   // Includes result
    ComputeCallStack cse  = new ComputeCallStack(signature);

    // Compute return type
    int res_length = cse.computeForReturntype(out);

    // Temporary hack.
    if (out.get(0).equal(CellTypeState.ref) && out.get(1).equal(CellTypeState.bottom)) {
      out.get(0).set(CellTypeState.makeLineRef(bci));
    }

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(res_length<=4, "max value should be vv");
    }

    // Compute arguments
    int arg_length = cse.computeForParameters(is_static, in);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(arg_length<=MAXARGSIZE, "too many locals");
    }

    // Pop arguments
    for (int i = arg_length - 1; i >= 0; i--) ppop1(in.get(i));// Do args in reverse order.

    // Report results
    if (_report_result_for_send == true) {
      fillStackmapForOpcodes(_itr_send, vars(), stack(), _stack_top);
      _report_result_for_send = false;
    }

    // Push return address
    ppush(out);
  }

  void  doMultianewarray                    (int dims, int bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(dims >= 1, "sanity check");
    }
    for(int i = dims -1; i >=0; i--) {
      ppop1(valCTS);
    }
    ppush1(CellTypeState.makeLineRef(bci));
  }

  void  doMonitorenter                      (int bci) {
    CellTypeState actual = pop();
    if (_monitor_top == bad_monitors) {
      return;
    }

    // Bail out when we get repeated locks on an identical monitor.  This case
    // isn't too hard to handle and can be made to work if supporting nested
    // redundant synchronized statements becomes a priority.
    //
    // See also "Note" in do_monitorexit(), below.
    if (actual.isLockReference()) {
      _monitor_top = bad_monitors;
      _monitor_safe = false;

      if (TraceMonitorMismatch) {
        reportMonitorMismatch("nested redundant lock -- bailout...");
      }
      return;
    }

    CellTypeState lock = CellTypeState.makeLockRef(bci);
    checkType(refCTS, actual);
    if (!actual.isInfoTop()) {
      replaceAllCTSMatches(actual, lock);
      monitorPush(lock);
    }
  }

  void  doMonitorexit                       (int bci) {
    CellTypeState actual = pop();
    if (_monitor_top == bad_monitors) {
      return;
    }
    checkType(refCTS, actual);
    CellTypeState expected = monitorPop();
    if (!actual.isLockReference() || !expected.equal(actual)) {
      // The monitor we are exiting is not verifiably the one
      // on the top of our monitor stack.  This causes a monitor
      // mismatch.
      _monitor_top = bad_monitors;
      _monitor_safe = false;

      // We need to mark this basic block as changed so that
      // this monitorexit will be visited again.  We need to
      // do this to ensure that we have accounted for the
      // possibility that this bytecode will throw an
      // exception.
      BasicBlock bb = getBasicBlockContaining(bci);
      bb.setChanged(true);
      bb._monitor_top = bad_monitors;

      if (TraceMonitorMismatch) {
        reportMonitorMismatch("improper monitor pair");
      }
    } else {
      // This code is a fix for the case where we have repeated
      // locking of the same object in straightline code.  We clear
      // out the lock when it is popped from the monitor stack
      // and replace it with an unobtrusive reference value that can
      // be locked again.
      //
      // Note: when generateOopMap is fixed to properly handle repeated,
      //       nested, redundant locks on the same object, then this
      //       fix will need to be removed at that time.
      replaceAllCTSMatches(actual, CellTypeState.makeLineRef(bci));
    }

    if (_report_for_exit_bci == bci) {
      _matching_enter_bci = expected.getMonitorSource();
    }
  }

  void  doReturnMonitorCheck                () {
    if (_monitor_top > 0) {
      // The monitor stack must be empty when we leave the method
      // for the monitors to be properly matched.
      _monitor_safe = false;

      // Since there are no successors to the *return bytecode, it
      // isn't necessary to set _monitor_top to bad_monitors.

      if (TraceMonitorMismatch) {
        reportMonitorMismatch("non-empty monitor stack at return");
      }
    }
  }

  void  doCheckcast                         () {
    CellTypeState actual = pop();
    checkType(refCTS, actual);
    push(actual);
  }

  CellTypeState[] sigcharToEffect           (char sigch, int bci, CellTypeState[] out) {
    // Object and array
    if (sigch=='L' || sigch=='[') {
      out[0] = CellTypeState.makeLineRef(bci);
      out[1] = CellTypeState.bottom;
      return out;
    }
    if (sigch == 'J' || sigch == 'D' ) return vvCTS;  // Long and Double
    if (sigch == 'V' ) return epsilonCTS;             // Void
    return vCTS;                                      // Otherwise
  }

  // Copies (optionally bottom/zero terminated) CTS string from "src" into "dst".
  //   Does NOT terminate with a bottom. Returns the number of cells copied.
  int copyCTS                               (CellTypeState[] dst, CellTypeState[] src) {
    int idx = 0;
    for (; idx < src.length && !src[idx].isBottom(); idx++) {
      dst[idx] = src[idx];
    }
    return idx;
  }

  // Create result set
  boolean  _report_result;
  boolean  _report_result_for_send;         // Unfortunatly, stackmaps for sends are special, so we need some extra
  BytecodeStream _itr_send;                 // variables to handle them properly.

  void  reportResult                        () {
    //    if (TraceNewOopMapGeneration) tty.print_cr("Report result pass");

    // We now want to report the result of the parse
    _report_result = true;

    // Prolog code
    fillStackmapProlog(_gc_points);

    // Mark everything changed, then do one interpretation pass.
    for (int i = 0; i<_bb_count; i++) {
      if (_basic_blocks[i].isReachable()) {
        _basic_blocks[i].setChanged(true);
        interpBB(_basic_blocks[i]);
      }
    }

    // Note: Since we are skipping dead-code when we are reporting results, then
    // the no. of encountered gc-points might be fewer than the previously number
    // we have counted. (dead-code is a pain - it should be removed before we get here)
    fillStackmapEpilog();

    // Report initvars
    fillInitVars(_init_vars);

    _report_result = false;
  }

  // Initvars
  List<Integer> _init_vars;

  void  initializeVars                      () {
    for (int k = 0; k < _init_vars.size(); k++)
      _state.get((_init_vars.get(k)).intValue()).set(CellTypeState.makeSlotRef(k));
  }

  void  addToRefInitSet                     (int localNo) {
    //    if (TraceNewOopMapGeneration)
    //      tty.print_cr("Added init vars: %d", localNo);

    Integer local = localNo;

    // Is it already in the set?
    if (_init_vars.contains(local))
      return;

    _init_vars.add(local);
  }

  // Conflicts rewrite logic
  boolean   _conflict;                      // True, if a conflict occured during interpretation
  int       _nof_refval_conflicts;          // No. of conflicts that require rewrites
  int[]     _new_var_map;

  void recordRefvalConflict                 (int varNo) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(varNo>=0 && varNo< _max_locals, "index out of range");
    }

    if (TraceOopMapRewrites) {
      System.err.println("### Conflict detected (local no: " + varNo + ")");
    }

    if (_new_var_map == null) {
      _new_var_map = new int[_max_locals];
      for (int k = 0; k < _max_locals; k++)  _new_var_map[k] = k;
    }

    if ( _new_var_map[varNo] == varNo) {
      // Check if max. number of locals has been reached
      if (_max_locals + _nof_refval_conflicts >= MAX_LOCAL_VARS) {
        throw new RuntimeException("Rewriting exceeded local variable limit");
      }
      _new_var_map[varNo] = _max_locals + _nof_refval_conflicts;
      _nof_refval_conflicts++;
    }
  }

  void rewriteRefvalConflicts               () {
    if (_nof_refval_conflicts > 0) {
      if (VM.getVM().isDebugging()) {
        throw new RuntimeException("Should not reach here (method rewriting should have been done by the VM already)");
      } else {
        throw new RuntimeException("Method rewriting not yet implemented in Java");
      }
    }
  }
  // Rewriting-related routines are not needed here
  //  void rewrite_refval_conflict              (int from, int to);
  //  bool rewrite_refval_conflict_inst         (BytecodeStream *i, int from, int to);
  //  bool rewrite_load_or_store                (BytecodeStream *i, Bytecodes.Code bc, Bytecodes.Code bc0, unsigned int varNo);

  //  bool expand_current_instr                 (int bci, int ilen, int newIlen, u_char inst_buffer[]);
  //  bool is_astore                            (BytecodeStream *itr, int *index);
  //  bool is_aload                             (BytecodeStream *itr, int *index);

  // List of bci's where a return address is on top of the stack
  //  GrowableArray<intptr_t> *_ret_adr_tos;

  //  bool stack_top_holds_ret_addr             (int bci);
  //  void compute_ret_adr_at_TOS               ();
  //  void update_ret_adr_at_TOS                (int bci, int delta);

  String stateVecToString                   (CellTypeStateList vec, int len) {
    for (int i = 0; i < len; i++) {
      _state_vec_buf[i] = vec.get(i).toChar();
    }
    return new String(_state_vec_buf, 0, len);
  }

  // Helper method. Can be used in subclasses to fx. calculate gc_points. If the current instuction
  // is a control transfer, then calls the jmpFct all possible destinations.
  void  retJumpTargetsDo                    (BytecodeStream bcs, JumpClosure closure, int varNo, int[] data) {
    CellTypeState ra = vars().get(varNo);
    if (!ra.isGoodAddress()) {
      throw new RuntimeException("ret returns from two jsr subroutines?");
    }
    int target = ra.getInfo();

    RetTableEntry rtEnt = _rt.findJsrsForTarget(target);
    int bci = bcs.bci();
    for (int i = 0; i < rtEnt.nofJsrs(); i++) {
      int target_bci = rtEnt.jsrs(i);
      // Make sure a jrtRet does not set the changed bit for dead basicblock.
      BasicBlock jsr_bb    = getBasicBlockContaining(target_bci - 1);
      if (Assert.ASSERTS_ENABLED) {
        BasicBlock target_bb = _basic_blocks[1 + bbIndex(jsr_bb)];
        Assert.that(target_bb  == getBasicBlockAt(target_bci), "wrong calc. of successor basicblock");
      }
      boolean alive = jsr_bb.isAlive();
      //      if (TraceNewOopMapGeneration) {
      //        tty.print("pc = %d, ret . %d alive: %s\n", bci, target_bci, alive ? "true" : "false");
      //      }
      if (alive) {
        closure.process(this, target_bci, data);
      }
    }
  }

  /** If the current instruction in "c" has no effect on control flow,
      returns "true".  Otherwise, calls "closure.process()" one or
      more times, with "c", an appropriate "pcDelta", and "data" as
      arguments, then returns "false".  There is one exception: if the
      current instruction is a "ret", returns "false" without calling
      "jmpFct". Arrangements for tracking the control flow of a "ret"
      must be made externally. */
  boolean jumpTargetsDo                     (BytecodeStream bcs, JumpClosure closure, int[] data) {
    int bci = bcs.bci();

    switch (bcs.code()) {
    case Bytecodes._ifeq:
    case Bytecodes._ifne:
    case Bytecodes._iflt:
    case Bytecodes._ifge:
    case Bytecodes._ifgt:
    case Bytecodes._ifle:
    case Bytecodes._if_icmpeq:
    case Bytecodes._if_icmpne:
    case Bytecodes._if_icmplt:
    case Bytecodes._if_icmpge:
    case Bytecodes._if_icmpgt:
    case Bytecodes._if_icmple:
    case Bytecodes._if_acmpeq:
    case Bytecodes._if_acmpne:
    case Bytecodes._ifnull:
    case Bytecodes._ifnonnull:
      closure.process(this, bcs.dest(), data);
      closure.process(this, bci + 3, data);
      break;

    case Bytecodes._goto:
      closure.process(this, bcs.dest(), data);
      break;
    case Bytecodes._goto_w:
      closure.process(this, bcs.dest_w(), data);
      break;
    case Bytecodes._tableswitch:
      {
        BytecodeTableswitch tableswitch = BytecodeTableswitch.at(bcs);
        int len = tableswitch.length();

        closure.process(this, bci + tableswitch.defaultOffset(), data); /* Default. jump address */
        while (--len >= 0) {
          closure.process(this, bci + tableswitch.destOffsetAt(len), data);
        }
        break;
      }

    case Bytecodes._fast_linearswitch:     // Java opcodes
    case Bytecodes._fast_binaryswitch:     // get_int_table handles conversions
    case Bytecodes._lookupswitch:
      {
        BytecodeLookupswitch lookupswitch = BytecodeLookupswitch.at(bcs);
        int npairs = lookupswitch.numberOfPairs();
        closure.process(this, bci + lookupswitch.defaultOffset(), data); /* Default. */
        while(--npairs >= 0) {
          LookupswitchPair pair = lookupswitch.pairAt(npairs);
          closure.process(this, bci + pair.offset(), data);
        }
        break;
      }
    case Bytecodes._jsr:
      Assert.that(bcs.isWide()==false, "sanity check");
      closure.process(this, bcs.dest(), data);
      break;
    case Bytecodes._jsr_w:
      closure.process(this, bcs.dest_w(), data);
      break;
    case Bytecodes._wide:
      throw new RuntimeException("Should not reach here");
    case Bytecodes._athrow:
    case Bytecodes._ireturn:
    case Bytecodes._lreturn:
    case Bytecodes._freturn:
    case Bytecodes._dreturn:
    case Bytecodes._areturn:
    case Bytecodes._return:
    case Bytecodes._ret:
      break;
    default:
      return true;
    }
    return false;
  }

  // Monitor matching
  //  int fill_out_arrays                       (int *enter, int *exit, int max);

  //  friend class RelocCallback;

  //----------------------------------------------------------------------
  // Public routines for GenerateOopMap
  //
  public GenerateOopMap(Method method) {
    // We have to initialize all variables here, that can be queried direcly
    _method = method;
    _max_locals=0;
    _init_vars = null;
    _rt = new RetTable();
  }


  // Compute the map.
  public void computeMap() {
    if (DEBUG) {
      System.err.println("*** GenerateOopMap: computing for " +
                         method().getMethodHolder().getName().asString() + "." +
                         method().getName().asString() +
                         method().getSignature().asString());
    }

    // Initialize values
    _got_error      = false;
    _conflict       = false;
    _max_locals     = (int) method().getMaxLocals();
    _max_stack      = (int) method().getMaxStack();
    _has_exceptions = (method().hasExceptionTable());
    _nof_refval_conflicts = 0;
    _init_vars      = new ArrayList<>(5);  // There are seldom more than 5 init_vars
    _report_result  = false;
    _report_result_for_send = false;
    _report_for_exit_bci = -1;
    _new_var_map    = null;
    //    _ret_adr_tos    = new GrowableArray<intptr_t>(5);  // 5 seems like a good number;
    //    _did_rewriting  = false;
    //    _did_relocation = false;

    // FIXME: remove
    /*
    if (TraceNewOopMapGeneration) {
      tty.print("Method name: %s\n", method().name().as_C_string());
      if (Verbose) {
        _method.print_codes();
        tty.print_cr("Exception table:");
        typeArrayOop excps = method().exception_table();
        for(int i = 0; i < excps.length(); i += 4) {
          tty.print_cr("[%d - %d] . %d", excps.int_at(i + 0), excps.int_at(i + 1), excps.int_at(i + 2));
        }
      }
    }
    */

    // if no code - do nothing
    // compiler needs info
    if (method().getCodeSize() == 0 || _max_locals + method().getMaxStack() == 0) {
      fillStackmapProlog(0);
      fillStackmapEpilog();
      return;
    }
    // Step 1: Compute all jump targets and their return value
    if (!_got_error)
      _rt.computeRetTable(_method);

    // Step 2: Find all basic blocks and count GC points
    if (!_got_error)
      markBBHeadersAndCountGCPoints();

    // Step 3: Calculate stack maps
    if (!_got_error)
      doInterpretation();

    // Step 4:Return results
    if (!_got_error && reportResults())
      reportResult();

    if (_got_error) {
      // We could expand this code to throw somekind of exception (e.g., VerifyException). However,
      // an exception thrown in this part of the code is likly to mean that we are executing some
      // illegal bytecodes (that the verifier should have caught if turned on), so we will just exit
      // with a fatal.
      throw new RuntimeException("Illegal bytecode sequence encountered while generating interpreter pointer maps - method should be rejected by verifier.");
    }
  }

  // Do a callback on fill_stackmap_for_opcodes for basicblock containing bci
  public void resultForBasicblock(int bci) {
    // FIXME: remove
    //    if (TraceNewOopMapGeneration) tty.print_cr("Report result pass for basicblock");

    // We now want to report the result of the parse
    _report_result = true;

    // Find basicblock and report results
    BasicBlock bb = getBasicBlockContaining(bci);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bb.isReachable(), "getting result from unreachable basicblock");
    }
    bb.setChanged(true);
    interpBB(bb);
  }

  // Query
  public int maxLocals()                                  { return _max_locals; }
  public Method method()                                  { return _method; }

  //  bool did_rewriting()                             { return _did_rewriting; }
  //  bool did_relocation()                            { return _did_relocation; }

  //  static void print_time();

  // Monitor query
  public boolean monitorSafe()                            { return _monitor_safe; }
  // Takes as input the bci of a monitorexit bytecode.
  // Returns the bci of the corresponding monitorenter.
  // Can only be called safely after computeMap() is run.
  public int  getMonitorMatch(int bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(_monitor_safe, "Attempt to match monitor in broken code.");
    }

    //    if (TraceNewOopMapGeneration)
    //      tty.print_cr("Report monitor match for bci : %d", bci);

    // We want to report the line number of the monitorenter.
    _report_for_exit_bci = bci;
    _matching_enter_bci = -1;

    // Find basicblock and report results
    BasicBlock bb = getBasicBlockContaining(bci);
    if (bb.isReachable()) {
      bb.setChanged(true);
      interpBB(bb);
      _report_for_exit_bci = -1;
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(_matching_enter_bci != -1, "monitor matching invariant");
      }
    }
    return _matching_enter_bci;
  }

  // Returns a Arena allocated object that contains pairing info.
  //  MonitorPairs* get_pairing(Arena *arena);

  // copies monitor pairing info into area; area_count specifies maximum
  // possible number of monitor pairs
  //  int copy_pairing(int pair_count, MonitorPairs* pairs);

  private int bbIndex(BasicBlock bb) {
    for (int i = 0; i < _basic_blocks.length; i++) {
      if (_basic_blocks[i] == bb) {
        return i;
      }
    }
    throw new RuntimeException("Should have found block");
  }

  //----------------------------------------------------------------------
  // Specialization methods. Intended use:
  // - possibleGCPoint must return true for every bci for which the
  //   stackmaps must be returned
  // - fillStackmapProlog is called just before the result is
  //   reported. The arguments tells the estimated number of gc points
  // - fillStackmapForOpcodes is called once for each bytecode index
  //   in order (0...code_length-1)
  // - fillStackmapEpilog is called after all results has been
  //   reported. Note: Since the algorithm does not report stackmaps for
  //   deadcode, fewer gc_points might have been encounted than assumed
  //   during the epilog. It is the responsibility of the subclass to
  //   count the correct number.
  // - fillInitVars are called once with the result of the init_vars
  //   computation
  //
  // All these methods are used during a call to computeMap. Note:
  // None of the return results are valid after computeMap returns,
  // since all values are allocated as resource objects.
  //
  // All virtual method must be implemented in subclasses
  public boolean allowRewrites            ()                              { return false; }
  public boolean reportResults            ()                              { return true;  }
  public boolean reportInitVars           ()                              { return true;  }
  public boolean possibleGCPoint          (BytecodeStream bcs)            { throw new RuntimeException("ShouldNotReachHere"); }
  public void fillStackmapProlog          (int nofGCPoints)               { throw new RuntimeException("ShouldNotReachHere"); }
  public void fillStackmapEpilog          ()                              { throw new RuntimeException("ShouldNotReachHere"); }
  public void fillStackmapForOpcodes      (BytecodeStream bcs,
                                           CellTypeStateList vars,
                                           CellTypeStateList stack,
                                           int stackTop)                  { throw new RuntimeException("ShouldNotReachHere"); }
  public void fillInitVars                (List<Integer> init_vars)       { throw new RuntimeException("ShouldNotReachHere"); }
}
