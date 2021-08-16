/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_ASM_ASSEMBLER_HPP
#define SHARE_ASM_ASSEMBLER_HPP

#include "asm/codeBuffer.hpp"
#include "asm/register.hpp"
#include "code/oopRecorder.hpp"
#include "code/relocInfo.hpp"
#include "memory/allocation.hpp"
#include "utilities/debug.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

// This file contains platform-independent assembler declarations.

class MacroAssembler;
class AbstractAssembler;
class Label;

/**
 * Labels represent destinations for control transfer instructions.  Such
 * instructions can accept a Label as their target argument.  A Label is
 * bound to the current location in the code stream by calling the
 * MacroAssembler's 'bind' method, which in turn calls the Label's 'bind'
 * method.  A Label may be referenced by an instruction before it's bound
 * (i.e., 'forward referenced').  'bind' stores the current code offset
 * in the Label object.
 *
 * If an instruction references a bound Label, the offset field(s) within
 * the instruction are immediately filled in based on the Label's code
 * offset.  If an instruction references an unbound label, that
 * instruction is put on a list of instructions that must be patched
 * (i.e., 'resolved') when the Label is bound.
 *
 * 'bind' will call the platform-specific 'patch_instruction' method to
 * fill in the offset field(s) for each unresolved instruction (if there
 * are any).  'patch_instruction' lives in one of the
 * cpu/<arch>/vm/assembler_<arch>* files.
 *
 * Instead of using a linked list of unresolved instructions, a Label has
 * an array of unresolved instruction code offsets.  _patch_index
 * contains the total number of forward references.  If the Label's array
 * overflows (i.e., _patch_index grows larger than the array size), a
 * GrowableArray is allocated to hold the remaining offsets.  (The cache
 * size is 4 for now, which handles over 99.5% of the cases)
 *
 * Labels may only be used within a single CodeSection.  If you need
 * to create references between code sections, use explicit relocations.
 */
class Label {
 private:
  enum { PatchCacheSize = 4 debug_only( +4 ) };

  // _loc encodes both the binding state (via its sign)
  // and the binding locator (via its value) of a label.
  //
  // _loc >= 0   bound label, loc() encodes the target (jump) position
  // _loc == -1  unbound label
  int _loc;

  // References to instructions that jump to this unresolved label.
  // These instructions need to be patched when the label is bound
  // using the platform-specific patchInstruction() method.
  //
  // To avoid having to allocate from the C-heap each time, we provide
  // a local cache and use the overflow only if we exceed the local cache
  int _patches[PatchCacheSize];
  int _patch_index;
  GrowableArray<int>* _patch_overflow;

  NONCOPYABLE(Label);
 protected:

  // The label will be bound to a location near its users.
  bool _is_near;

#ifdef ASSERT
  // Sourcre file and line location of jump instruction
  int _lines[PatchCacheSize];
  const char* _files[PatchCacheSize];
#endif
 public:

  /**
   * After binding, be sure 'patch_instructions' is called later to link
   */
  void bind_loc(int loc) {
    assert(loc >= 0, "illegal locator");
    assert(_loc == -1, "already bound");
    _loc = loc;
  }
  void bind_loc(int pos, int sect) { bind_loc(CodeBuffer::locator(pos, sect)); }

#ifndef PRODUCT
  // Iterates over all unresolved instructions for printing
  void print_instructions(MacroAssembler* masm) const;
#endif // PRODUCT

  /**
   * Returns the position of the the Label in the code buffer
   * The position is a 'locator', which encodes both offset and section.
   */
  int loc() const {
    assert(_loc >= 0, "unbound label");
    return _loc;
  }
  int loc_pos()  const { return CodeBuffer::locator_pos(loc()); }
  int loc_sect() const { return CodeBuffer::locator_sect(loc()); }

  bool is_bound() const    { return _loc >=  0; }
  bool is_unbound() const  { return _loc == -1 && _patch_index > 0; }
  bool is_unused() const   { return _loc == -1 && _patch_index == 0; }

  // The label will be bound to a location near its users. Users can
  // optimize on this information, e.g. generate short branches.
  bool is_near()           { return _is_near; }

  /**
   * Adds a reference to an unresolved displacement instruction to
   * this unbound label
   *
   * @param cb         the code buffer being patched
   * @param branch_loc the locator of the branch instruction in the code buffer
   */
  void add_patch_at(CodeBuffer* cb, int branch_loc, const char* file = NULL, int line = 0);

  /**
   * Iterate over the list of patches, resolving the instructions
   * Call patch_instruction on each 'branch_loc' value
   */
  void patch_instructions(MacroAssembler* masm);

  void init() {
    _loc = -1;
    _patch_index = 0;
    _patch_overflow = NULL;
    _is_near = false;
  }

  Label() {
    init();
  }

  ~Label() {
    assert(is_bound() || is_unused(), "Label was never bound to a location, but it was used as a jmp target");
  }

  void reset() {
    init(); //leave _patch_overflow because it points to CodeBuffer.
  }
};

// A NearLabel must be bound to a location near its users. Users can
// optimize on this information, e.g. generate short branches.
class NearLabel : public Label {
 public:
  NearLabel() : Label() { _is_near = true; }
};

// A union type for code which has to assemble both constant and
// non-constant operands, when the distinction cannot be made
// statically.
class RegisterOrConstant {
 private:
  Register _r;
  intptr_t _c;

 public:
  RegisterOrConstant(): _r(noreg), _c(0) {}
  RegisterOrConstant(Register r): _r(r), _c(0) {}
  RegisterOrConstant(intptr_t c): _r(noreg), _c(c) {}

  Register as_register() const { assert(is_register(),""); return _r; }
  intptr_t as_constant() const { assert(is_constant(),""); return _c; }

  Register register_or_noreg() const { return _r; }
  intptr_t constant_or_zero() const  { return _c; }

  bool is_register() const { return _r != noreg; }
  bool is_constant() const { return _r == noreg; }
};

// The Abstract Assembler: Pure assembler doing NO optimizations on the
// instruction level; i.e., what you write is what you get.
// The Assembler is generating code into a CodeBuffer.
class AbstractAssembler : public ResourceObj  {
  friend class Label;

 protected:
  CodeSection* _code_section;          // section within the code buffer
  OopRecorder* _oop_recorder;          // support for relocInfo::oop_type

 public:
  // Code emission & accessing
  address addr_at(int pos) const { return code_section()->start() + pos; }

 protected:
  // This routine is called with a label is used for an address.
  // Labels and displacements truck in offsets, but target must return a PC.
  address target(Label& L)             { return code_section()->target(L, pc()); }

  bool is8bit(int x) const             { return -0x80 <= x && x < 0x80; }
  bool isByte(int x) const             { return 0 <= x && x < 0x100; }
  bool isShiftCount(int x) const       { return 0 <= x && x < 32; }

  // Instruction boundaries (required when emitting relocatable values).
  class InstructionMark: public StackObj {
   private:
    AbstractAssembler* _assm;

   public:
    InstructionMark(AbstractAssembler* assm) : _assm(assm) {
      assert(assm->inst_mark() == NULL, "overlapping instructions");
      _assm->set_inst_mark();
    }
    ~InstructionMark() {
      _assm->clear_inst_mark();
    }
  };
  friend class InstructionMark;
#ifdef ASSERT
  // Make it return true on platforms which need to verify
  // instruction boundaries for some operations.
  static bool pd_check_instruction_mark();

  // Add delta to short branch distance to verify that it still fit into imm8.
  int _short_branch_delta;

  int  short_branch_delta() const { return _short_branch_delta; }
  void set_short_branch_delta()   { _short_branch_delta = 32; }
  void clear_short_branch_delta() { _short_branch_delta = 0; }

  class ShortBranchVerifier: public StackObj {
   private:
    AbstractAssembler* _assm;

   public:
    ShortBranchVerifier(AbstractAssembler* assm) : _assm(assm) {
      assert(assm->short_branch_delta() == 0, "overlapping instructions");
      _assm->set_short_branch_delta();
    }
    ~ShortBranchVerifier() {
      _assm->clear_short_branch_delta();
    }
  };
#else
  // Dummy in product.
  class ShortBranchVerifier: public StackObj {
   public:
    ShortBranchVerifier(AbstractAssembler* assm) {}
  };
#endif

 public:

  // Creation
  AbstractAssembler(CodeBuffer* code);

  // ensure buf contains all code (call this before using/copying the code)
  void flush();

  void emit_int8(   int8_t x1)                                  { code_section()->emit_int8(x1); }

  void emit_int16(  int16_t x)                                  { code_section()->emit_int16(x); }
  void emit_int16(  int8_t x1, int8_t x2)                       { code_section()->emit_int16(x1, x2); }

  void emit_int24(  int8_t x1, int8_t x2, int8_t x3)            { code_section()->emit_int24(x1, x2, x3); }

  void emit_int32(  int32_t x)                                  { code_section()->emit_int32(x); }
  void emit_int32(  int8_t x1, int8_t x2, int8_t x3, int8_t x4) { code_section()->emit_int32(x1, x2, x3, x4); }

  void emit_int64(  int64_t x)                                  { code_section()->emit_int64(x); }

  void emit_float(  jfloat  x)                                  { code_section()->emit_float(x); }
  void emit_double( jdouble x)                                  { code_section()->emit_double(x); }
  void emit_address(address x)                                  { code_section()->emit_address(x); }

  enum { min_simm10 = -512 };

  // Test if x is within signed immediate range for width.
  static bool is_simm(int64_t x, uint w) {
    precond(1 < w && w < 64);
    int64_t limes = INT64_C(1) << (w - 1);
    return -limes <= x && x < limes;
  }

  static bool is_simm8(int64_t x) { return is_simm(x, 8); }
  static bool is_simm9(int64_t x) { return is_simm(x, 9); }
  static bool is_simm10(int64_t x) { return is_simm(x, 10); }
  static bool is_simm16(int64_t x) { return is_simm(x, 16); }
  static bool is_simm32(int64_t x) { return is_simm(x, 32); }

  // Test if x is within unsigned immediate range for width.
  static bool is_uimm(uint64_t x, uint w) {
    precond(0 < w && w < 64);
    uint64_t limes = UINT64_C(1) << w;
    return x < limes;
  }

  static bool is_uimm12(uint64_t x) { return is_uimm(x, 12); }

  // Accessors
  CodeSection*  code_section() const   { return _code_section; }
  CodeBuffer*   code()         const   { return code_section()->outer(); }
  int           sect()         const   { return code_section()->index(); }
  address       pc()           const   { return code_section()->end();   }
  int           offset()       const   { return code_section()->size();  }
  int           locator()      const   { return CodeBuffer::locator(offset(), sect()); }

  OopRecorder*  oop_recorder() const   { return _oop_recorder; }
  void      set_oop_recorder(OopRecorder* r) { _oop_recorder = r; }

  address       inst_mark() const { return code_section()->mark();       }
  void      set_inst_mark()       {        code_section()->set_mark();   }
  void    clear_inst_mark()       {        code_section()->clear_mark(); }

  // Constants in code
  void relocate(RelocationHolder const& rspec, int format = 0) {
    assert(!pd_check_instruction_mark()
        || inst_mark() == NULL || inst_mark() == code_section()->end(),
        "call relocate() between instructions");
    code_section()->relocate(code_section()->end(), rspec, format);
  }
  void relocate(   relocInfo::relocType rtype, int format = 0) {
    code_section()->relocate(code_section()->end(), rtype, format);
  }

  static int code_fill_byte();         // used to pad out odd-sized code buffers

  // Associate a comment with the current offset.  It will be printed
  // along with the disassembly when printing nmethods.  Currently
  // only supported in the instruction section of the code buffer.
  void block_comment(const char* comment);
  // Copy str to a buffer that has the same lifetime as the CodeBuffer
  const char* code_string(const char* str);

  // Label functions
  void bind(Label& L); // binds an unbound label L to the current code position

  // Move to a different section in the same code buffer.
  void set_code_section(CodeSection* cs);

  // Inform assembler when generating stub code and relocation info
  address    start_a_stub(int required_space);
  void       end_a_stub();
  // Ditto for constants.
  address    start_a_const(int required_space, int required_align = sizeof(double));
  void       end_a_const(CodeSection* cs);  // Pass the codesection to continue in (insts or stubs?).

  // constants support
  //
  // We must remember the code section (insts or stubs) in c1
  // so we can reset to the proper section in end_a_const().
  address int_constant(jint c) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      emit_int32(c);
      end_a_const(c1);
    }
    return ptr;
  }
  address long_constant(jlong c) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      emit_int64(c);
      end_a_const(c1);
    }
    return ptr;
  }
  address double_constant(jdouble c) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      emit_double(c);
      end_a_const(c1);
    }
    return ptr;
  }
  address float_constant(jfloat c) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      emit_float(c);
      end_a_const(c1);
    }
    return ptr;
  }
  address address_constant(address c) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      emit_address(c);
      end_a_const(c1);
    }
    return ptr;
  }
  address address_constant(address c, RelocationHolder const& rspec) {
    CodeSection* c1 = _code_section;
    address ptr = start_a_const(sizeof(c), sizeof(c));
    if (ptr != NULL) {
      relocate(rspec);
      emit_address(c);
      end_a_const(c1);
    }
    return ptr;
  }

  // Bang stack to trigger StackOverflowError at a safe location
  // implementation delegates to machine-specific bang_stack_with_offset
  void generate_stack_overflow_check( int frame_size_in_bytes );
  virtual void bang_stack_with_offset(int offset) = 0;


  /**
   * A platform-dependent method to patch a jump instruction that refers
   * to this label.
   *
   * @param branch the location of the instruction to patch
   * @param masm the assembler which generated the branch
   */
  void pd_patch_instruction(address branch, address target, const char* file, int line);

};

#include CPU_HEADER(assembler)

#endif // SHARE_ASM_ASSEMBLER_HPP
