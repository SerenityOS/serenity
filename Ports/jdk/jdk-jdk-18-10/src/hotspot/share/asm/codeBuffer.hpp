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

#ifndef SHARE_ASM_CODEBUFFER_HPP
#define SHARE_ASM_CODEBUFFER_HPP

#include "code/oopRecorder.hpp"
#include "code/relocInfo.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

class CodeStrings;
class PhaseCFG;
class Compile;
class BufferBlob;
class CodeBuffer;
class Label;

class CodeOffsets: public StackObj {
public:
  enum Entries { Entry,
                 Verified_Entry,
                 Frame_Complete, // Offset in the code where the frame setup is (for forte stackwalks) is complete
                 OSR_Entry,
                 Exceptions,     // Offset where exception handler lives
                 Deopt,          // Offset where deopt handler lives
                 DeoptMH,        // Offset where MethodHandle deopt handler lives
                 UnwindHandler,  // Offset to default unwind handler
                 max_Entries };

  // special value to note codeBlobs where profile (forte) stack walking is
  // always dangerous and suspect.

  enum { frame_never_safe = -1 };

private:
  int _values[max_Entries];

public:
  CodeOffsets() {
    _values[Entry         ] = 0;
    _values[Verified_Entry] = 0;
    _values[Frame_Complete] = frame_never_safe;
    _values[OSR_Entry     ] = 0;
    _values[Exceptions    ] = -1;
    _values[Deopt         ] = -1;
    _values[DeoptMH       ] = -1;
    _values[UnwindHandler ] = -1;
  }

  int value(Entries e) { return _values[e]; }
  void set_value(Entries e, int val) { _values[e] = val; }
};

// This class represents a stream of code and associated relocations.
// There are a few in each CodeBuffer.
// They are filled concurrently, and concatenated at the end.
class CodeSection {
  friend class CodeBuffer;
 public:
  typedef int csize_t;  // code size type; would be size_t except for history

 private:
  address     _start;           // first byte of contents (instructions)
  address     _mark;            // user mark, usually an instruction beginning
  address     _end;             // current end address
  address     _limit;           // last possible (allocated) end address
  relocInfo*  _locs_start;      // first byte of relocation information
  relocInfo*  _locs_end;        // first byte after relocation information
  relocInfo*  _locs_limit;      // first byte after relocation information buf
  address     _locs_point;      // last relocated position (grows upward)
  bool        _locs_own;        // did I allocate the locs myself?
  bool        _scratch_emit;    // Buffer is used for scratch emit, don't relocate.
  char        _index;           // my section number (SECT_INST, etc.)
  CodeBuffer* _outer;           // enclosing CodeBuffer

  // (Note:  _locs_point used to be called _last_reloc_offset.)

  CodeSection() {
    _start         = NULL;
    _mark          = NULL;
    _end           = NULL;
    _limit         = NULL;
    _locs_start    = NULL;
    _locs_end      = NULL;
    _locs_limit    = NULL;
    _locs_point    = NULL;
    _locs_own      = false;
    _scratch_emit  = false;
    debug_only(_index = (char)-1);
    debug_only(_outer = (CodeBuffer*)badAddress);
  }

  void initialize_outer(CodeBuffer* outer, int index) {
    _outer = outer;
    _index = index;
  }

  void initialize(address start, csize_t size = 0) {
    assert(_start == NULL, "only one init step, please");
    _start         = start;
    _mark          = NULL;
    _end           = start;

    _limit         = start + size;
    _locs_point    = start;
  }

  void initialize_locs(int locs_capacity);
  void expand_locs(int new_capacity);
  void initialize_locs_from(const CodeSection* source_cs);

  // helper for CodeBuffer::expand()
  void take_over_code_from(CodeSection* cs) {
    _start      = cs->_start;
    _mark       = cs->_mark;
    _end        = cs->_end;
    _limit      = cs->_limit;
    _locs_point = cs->_locs_point;
  }

 public:
  address     start() const         { return _start; }
  address     mark() const          { return _mark; }
  address     end() const           { return _end; }
  address     limit() const         { return _limit; }
  csize_t     size() const          { return (csize_t)(_end - _start); }
  csize_t     mark_off() const      { assert(_mark != NULL, "not an offset");
                                      return (csize_t)(_mark - _start); }
  csize_t     capacity() const      { return (csize_t)(_limit - _start); }
  csize_t     remaining() const     { return (csize_t)(_limit - _end); }

  relocInfo*  locs_start() const    { return _locs_start; }
  relocInfo*  locs_end() const      { return _locs_end; }
  int         locs_count() const    { return (int)(_locs_end - _locs_start); }
  relocInfo*  locs_limit() const    { return _locs_limit; }
  address     locs_point() const    { return _locs_point; }
  csize_t     locs_point_off() const{ return (csize_t)(_locs_point - _start); }
  csize_t     locs_capacity() const { return (csize_t)(_locs_limit - _locs_start); }

  int         index() const         { return _index; }
  bool        is_allocated() const  { return _start != NULL; }
  bool        is_empty() const      { return _start == _end; }
  bool        has_locs() const      { return _locs_end != NULL; }

  // Mark scratch buffer.
  void        set_scratch_emit()    { _scratch_emit = true; }
  bool        scratch_emit()        { return _scratch_emit; }

  CodeBuffer* outer() const         { return _outer; }

  // is a given address in this section?  (2nd version is end-inclusive)
  bool contains(address pc) const   { return pc >= _start && pc <  _end; }
  bool contains2(address pc) const  { return pc >= _start && pc <= _end; }
  bool allocates(address pc) const  { return pc >= _start && pc <  _limit; }
  bool allocates2(address pc) const { return pc >= _start && pc <= _limit; }

  // checks if two CodeSections are disjoint
  //
  // limit is an exclusive address and can be the start of another
  // section.
  bool disjoint(CodeSection* cs) const { return cs->_limit <= _start || cs->_start >= _limit; }

  void    set_end(address pc)       { assert(allocates2(pc), "not in CodeBuffer memory: " INTPTR_FORMAT " <= " INTPTR_FORMAT " <= " INTPTR_FORMAT, p2i(_start), p2i(pc), p2i(_limit)); _end = pc; }
  void    set_mark(address pc)      { assert(contains2(pc), "not in codeBuffer");
                                      _mark = pc; }
  void    set_mark()                { _mark = _end; }
  void    clear_mark()              { _mark = NULL; }

  void    set_locs_end(relocInfo* p) {
    assert(p <= locs_limit(), "locs data fits in allocated buffer");
    _locs_end = p;
  }
  void    set_locs_point(address pc) {
    assert(pc >= locs_point(), "relocation addr may not decrease");
    assert(allocates2(pc),     "relocation addr must be in this section");
    _locs_point = pc;
  }

  // Code emission
  void emit_int8(int8_t x1) {
    address curr = end();
    *((int8_t*)  curr++) = x1;
    set_end(curr);
  }

  void emit_int16(int16_t x) { *((int16_t*) end()) = x; set_end(end() + sizeof(int16_t)); }
  void emit_int16(int8_t x1, int8_t x2) {
    address curr = end();
    *((int8_t*)  curr++) = x1;
    *((int8_t*)  curr++) = x2;
    set_end(curr);
  }

  void emit_int24(int8_t x1, int8_t x2, int8_t x3)  {
    address curr = end();
    *((int8_t*)  curr++) = x1;
    *((int8_t*)  curr++) = x2;
    *((int8_t*)  curr++) = x3;
    set_end(curr);
  }

  void emit_int32(int32_t x) {
    address curr = end();
    *((int32_t*) curr) = x;
    set_end(curr + sizeof(int32_t));
  }
  void emit_int32(int8_t x1, int8_t x2, int8_t x3, int8_t x4)  {
    address curr = end();
    *((int8_t*)  curr++) = x1;
    *((int8_t*)  curr++) = x2;
    *((int8_t*)  curr++) = x3;
    *((int8_t*)  curr++) = x4;
    set_end(curr);
  }

  void emit_int64( int64_t x)  { *((int64_t*) end()) = x; set_end(end() + sizeof(int64_t)); }

  void emit_float( jfloat  x)  { *((jfloat*)  end()) = x; set_end(end() + sizeof(jfloat)); }
  void emit_double(jdouble x)  { *((jdouble*) end()) = x; set_end(end() + sizeof(jdouble)); }
  void emit_address(address x) { *((address*) end()) = x; set_end(end() + sizeof(address)); }

  // Share a scratch buffer for relocinfo.  (Hacky; saves a resource allocation.)
  void initialize_shared_locs(relocInfo* buf, int length);

  // Manage labels and their addresses.
  address target(Label& L, address branch_pc);

  // Emit a relocation.
  void relocate(address at, RelocationHolder const& rspec, int format = 0);
  void relocate(address at,    relocInfo::relocType rtype, int format = 0, jint method_index = 0);

  // alignment requirement for starting offset
  // Requirements are that the instruction area and the
  // stubs area must start on CodeEntryAlignment, and
  // the ctable on sizeof(jdouble)
  int alignment() const             { return MAX2((int)sizeof(jdouble), (int)CodeEntryAlignment); }

  // Slop between sections, used only when allocating temporary BufferBlob buffers.
  static csize_t end_slop()         { return MAX2((int)sizeof(jdouble), (int)CodeEntryAlignment); }

  csize_t align_at_start(csize_t off) const { return (csize_t) align_up(off, alignment()); }

  // Ensure there's enough space left in the current section.
  // Return true if there was an expansion.
  bool maybe_expand_to_ensure_remaining(csize_t amount);

#ifndef PRODUCT
  void decode();
  void print(const char* name);
#endif //PRODUCT
};

class CodeString;
class CodeStrings {
private:
#ifndef PRODUCT
  CodeString* _strings;
  CodeString* _strings_last;
#ifdef ASSERT
  // Becomes true after copy-out, forbids further use.
  bool _defunct; // Zero bit pattern is "valid", see memset call in decode_env::decode_env
#endif
  static const char* _prefix; // defaults to " ;; "

  CodeString* find(intptr_t offset) const;
  CodeString* find_last(intptr_t offset) const;

  void set_null_and_invalidate() {
    _strings = NULL;
    _strings_last = NULL;
#ifdef ASSERT
    _defunct = true;
#endif
  }
#endif

public:
  CodeStrings() {
#ifndef PRODUCT
    _strings = NULL;
    _strings_last = NULL;
#ifdef ASSERT
    _defunct = false;
#endif
#endif
  }

#ifndef PRODUCT
  bool is_null() {
#ifdef ASSERT
    return _strings == NULL;
#else
    return true;
#endif
  }

  const char* add_string(const char * string);

  void add_comment(intptr_t offset, const char * comment);
  void print_block_comment(outputStream* stream, intptr_t offset) const;
  int  count() const;
  // COPY strings from other to this; leave other valid.
  void copy(CodeStrings& other);
  // FREE strings; invalidate this.
  void free();

  // Guarantee that _strings are used at most once; assign and free invalidate a buffer.
  inline void check_valid() const {
    assert(!_defunct, "Use of invalid CodeStrings");
  }

  static void set_prefix(const char *prefix) {
    _prefix = prefix;
  }
#endif // !PRODUCT
};

// A CodeBuffer describes a memory space into which assembly
// code is generated.  This memory space usually occupies the
// interior of a single BufferBlob, but in some cases it may be
// an arbitrary span of memory, even outside the code cache.
//
// A code buffer comes in two variants:
//
// (1) A CodeBuffer referring to an already allocated piece of memory:
//     This is used to direct 'static' code generation (e.g. for interpreter
//     or stubroutine generation, etc.).  This code comes with NO relocation
//     information.
//
// (2) A CodeBuffer referring to a piece of memory allocated when the
//     CodeBuffer is allocated.  This is used for nmethod generation.
//
// The memory can be divided up into several parts called sections.
// Each section independently accumulates code (or data) an relocations.
// Sections can grow (at the expense of a reallocation of the BufferBlob
// and recopying of all active sections).  When the buffered code is finally
// written to an nmethod (or other CodeBlob), the contents (code, data,
// and relocations) of the sections are padded to an alignment and concatenated.
// Instructions and data in one section can contain relocatable references to
// addresses in a sibling section.

class CodeBuffer: public StackObj {
  friend class CodeSection;
  friend class StubCodeGenerator;

 private:
  // CodeBuffers must be allocated on the stack except for a single
  // special case during expansion which is handled internally.  This
  // is done to guarantee proper cleanup of resources.
  void* operator new(size_t size) throw() { return ResourceObj::operator new(size); }
  void  operator delete(void* p)          { ShouldNotCallThis(); }

 public:
  typedef int csize_t;  // code size type; would be size_t except for history
  enum {
    // Here is the list of all possible sections.  The order reflects
    // the final layout.
    SECT_FIRST = 0,
    SECT_CONSTS = SECT_FIRST, // Non-instruction data:  Floats, jump tables, etc.
    SECT_INSTS,               // Executable instructions.
    SECT_STUBS,               // Outbound trampolines for supporting call sites.
    SECT_LIMIT, SECT_NONE = -1
  };

 private:
  enum {
    sect_bits = 2,      // assert (SECT_LIMIT <= (1<<sect_bits))
    sect_mask = (1<<sect_bits)-1
  };

  const char*  _name;

  CodeSection  _consts;             // constants, jump tables
  CodeSection  _insts;              // instructions (the main section)
  CodeSection  _stubs;              // stubs (call site support), deopt, exception handling

  CodeBuffer*  _before_expand;  // dead buffer, from before the last expansion

  BufferBlob*  _blob;           // optional buffer in CodeCache for generated code
  address      _total_start;    // first address of combined memory buffer
  csize_t      _total_size;     // size in bytes of combined memory buffer

  OopRecorder* _oop_recorder;

  OopRecorder  _default_oop_recorder;  // override with initialize_oop_recorder
  Arena*       _overflow_arena;

  address      _last_insn;      // used to merge consecutive memory barriers, loads or stores.

#ifndef PRODUCT
  CodeStrings  _code_strings;
  bool         _collect_comments; // Indicate if we need to collect block comments at all.
  address      _decode_begin;     // start address for decode
  address      decode_begin();
#endif

  void initialize_misc(const char * name) {
    // all pointers other than code_start/end and those inside the sections
    assert(name != NULL, "must have a name");
    _name            = name;
    _before_expand   = NULL;
    _blob            = NULL;
    _oop_recorder    = NULL;
    _overflow_arena  = NULL;
    _last_insn       = NULL;

#ifndef PRODUCT
    _decode_begin    = NULL;
    _code_strings    = CodeStrings();
    // Collect block comments, but restrict collection to cases where a disassembly is output.
    _collect_comments = ( PrintAssembly
                       || PrintStubCode
                       || PrintMethodHandleStubs
                       || PrintInterpreter
                       || PrintSignatureHandlers
                       || UnlockDiagnosticVMOptions
                        );
#endif
  }

  void initialize(address code_start, csize_t code_size) {
    _consts.initialize_outer(this,  SECT_CONSTS);
    _insts.initialize_outer(this,   SECT_INSTS);
    _stubs.initialize_outer(this,   SECT_STUBS);
    _total_start = code_start;
    _total_size  = code_size;
    // Initialize the main section:
    _insts.initialize(code_start, code_size);
    assert(!_stubs.is_allocated(),  "no garbage here");
    assert(!_consts.is_allocated(), "no garbage here");
    _oop_recorder = &_default_oop_recorder;
  }

  void initialize_section_size(CodeSection* cs, csize_t size);

  // helper for CodeBuffer::expand()
  void take_over_code_from(CodeBuffer* cs);

  // ensure sections are disjoint, ordered, and contained in the blob
  void verify_section_allocation();

  // copies combined relocations to the blob, returns bytes copied
  // (if target is null, it is a dry run only, just for sizing)
  csize_t copy_relocations_to(CodeBlob* blob) const;

  // copies combined code to the blob (assumes relocs are already in there)
  void copy_code_to(CodeBlob* blob);

  // moves code sections to new buffer (assumes relocs are already in there)
  void relocate_code_to(CodeBuffer* cb) const;

  // set up a model of the final layout of my contents
  void compute_final_layout(CodeBuffer* dest) const;

  // Expand the given section so at least 'amount' is remaining.
  // Creates a new, larger BufferBlob, and rewrites the code & relocs.
  void expand(CodeSection* which_cs, csize_t amount);

  // Helper for expand.
  csize_t figure_expanded_capacities(CodeSection* which_cs, csize_t amount, csize_t* new_capacity);

 public:
  // (1) code buffer referring to pre-allocated instruction memory
  CodeBuffer(address code_start, csize_t code_size) {
    assert(code_start != NULL, "sanity");
    initialize_misc("static buffer");
    initialize(code_start, code_size);
    debug_only(verify_section_allocation();)
  }

  // (2) CodeBuffer referring to pre-allocated CodeBlob.
  CodeBuffer(CodeBlob* blob);

  // (3) code buffer allocating codeBlob memory for code & relocation
  // info but with lazy initialization.  The name must be something
  // informative.
  CodeBuffer(const char* name) {
    initialize_misc(name);
  }

  // (4) code buffer allocating codeBlob memory for code & relocation
  // info.  The name must be something informative and code_size must
  // include both code and stubs sizes.
  CodeBuffer(const char* name, csize_t code_size, csize_t locs_size) {
    initialize_misc(name);
    initialize(code_size, locs_size);
  }

  ~CodeBuffer();

  // Initialize a CodeBuffer constructed using constructor 3.  Using
  // constructor 4 is equivalent to calling constructor 3 and then
  // calling this method.  It's been factored out for convenience of
  // construction.
  void initialize(csize_t code_size, csize_t locs_size);

  CodeSection* consts() { return &_consts; }
  CodeSection* insts() { return &_insts; }
  CodeSection* stubs() { return &_stubs; }

  const CodeSection* insts() const { return &_insts; }

  // present sections in order; return NULL at end; consts is #0, etc.
  CodeSection* code_section(int n) {
    // This makes the slightly questionable but portable assumption
    // that the various members (_consts, _insts, _stubs, etc.) are
    // adjacent in the layout of CodeBuffer.
    CodeSection* cs = &_consts + n;
    assert(cs->index() == n || !cs->is_allocated(), "sanity");
    return cs;
  }
  const CodeSection* code_section(int n) const {  // yucky const stuff
    return ((CodeBuffer*)this)->code_section(n);
  }
  static const char* code_section_name(int n);
  int section_index_of(address addr) const;
  bool contains(address addr) const {
    // handy for debugging
    return section_index_of(addr) > SECT_NONE;
  }

  // A stable mapping between 'locators' (small ints) and addresses.
  static int locator_pos(int locator)   { return locator >> sect_bits; }
  static int locator_sect(int locator)  { return locator &  sect_mask; }
  static int locator(int pos, int sect) { return (pos << sect_bits) | sect; }
  int        locator(address addr) const;
  address    locator_address(int locator) const {
    if (locator < 0)  return NULL;
    address start = code_section(locator_sect(locator))->start();
    return start + locator_pos(locator);
  }

  // Heuristic for pre-packing the taken/not-taken bit of a predicted branch.
  bool is_backward_branch(Label& L);

  // Properties
  const char* name() const                  { return _name; }
  void set_name(const char* name)           { _name = name; }
  CodeBuffer* before_expand() const         { return _before_expand; }
  BufferBlob* blob() const                  { return _blob; }
  void    set_blob(BufferBlob* blob);
  void   free_blob();                       // Free the blob, if we own one.

  // Properties relative to the insts section:
  address       insts_begin() const      { return _insts.start();      }
  address       insts_end() const        { return _insts.end();        }
  void      set_insts_end(address end)   {        _insts.set_end(end); }
  address       insts_mark() const       { return _insts.mark();       }
  void      set_insts_mark()             {        _insts.set_mark();   }

  // is there anything in the buffer other than the current section?
  bool    is_pure() const                { return insts_size() == total_content_size(); }

  // size in bytes of output so far in the insts sections
  csize_t insts_size() const             { return _insts.size(); }

  // same as insts_size(), except that it asserts there is no non-code here
  csize_t pure_insts_size() const        { assert(is_pure(), "no non-code");
                                           return insts_size(); }
  // capacity in bytes of the insts sections
  csize_t insts_capacity() const         { return _insts.capacity(); }

  // number of bytes remaining in the insts section
  csize_t insts_remaining() const        { return _insts.remaining(); }

  // is a given address in the insts section?  (2nd version is end-inclusive)
  bool insts_contains(address pc) const  { return _insts.contains(pc); }
  bool insts_contains2(address pc) const { return _insts.contains2(pc); }

  // Record any extra oops required to keep embedded metadata alive
  void finalize_oop_references(const methodHandle& method);

  // Allocated size in all sections, when aligned and concatenated
  // (this is the eventual state of the content in its final
  // CodeBlob).
  csize_t total_content_size() const;

  // Combined offset (relative to start of first section) of given
  // section, as eventually found in the final CodeBlob.
  csize_t total_offset_of(const CodeSection* cs) const;

  // allocated size of all relocation data, including index, rounded up
  csize_t total_relocation_size() const;

  csize_t copy_relocations_to(address buf, csize_t buf_limit, bool only_inst) const;

  // allocated size of any and all recorded oops
  csize_t total_oop_size() const {
    OopRecorder* recorder = oop_recorder();
    return (recorder == NULL)? 0: recorder->oop_size();
  }

  // allocated size of any and all recorded metadata
  csize_t total_metadata_size() const {
    OopRecorder* recorder = oop_recorder();
    return (recorder == NULL)? 0: recorder->metadata_size();
  }

  // Configuration functions, called immediately after the CB is constructed.
  // The section sizes are subtracted from the original insts section.
  // Note:  Call them in reverse section order, because each steals from insts.
  void initialize_consts_size(csize_t size)            { initialize_section_size(&_consts,  size); }
  void initialize_stubs_size(csize_t size)             { initialize_section_size(&_stubs,   size); }
  // Override default oop recorder.
  void initialize_oop_recorder(OopRecorder* r);

  OopRecorder* oop_recorder() const { return _oop_recorder; }

  address last_insn() const { return _last_insn; }
  void set_last_insn(address a) { _last_insn = a; }
  void clear_last_insn() { set_last_insn(NULL); }

#ifndef PRODUCT
  CodeStrings& strings() { return _code_strings; }

  void free_strings() {
    if (!_code_strings.is_null()) {
      _code_strings.free(); // sets _strings Null as a side-effect.
    }
  }
#endif

  // Code generation
  void relocate(address at, RelocationHolder const& rspec, int format = 0) {
    _insts.relocate(at, rspec, format);
  }
  void relocate(address at,    relocInfo::relocType rtype, int format = 0) {
    _insts.relocate(at, rtype, format);
  }

  // Management of overflow storage for binding of Labels.
  GrowableArray<int>* create_patch_overflow();

  // NMethod generation
  void copy_code_and_locs_to(CodeBlob* blob) {
    assert(blob != NULL, "sane");
    copy_relocations_to(blob);
    copy_code_to(blob);
  }
  void copy_values_to(nmethod* nm) {
    if (!oop_recorder()->is_unused()) {
      oop_recorder()->copy_values_to(nm);
    }
  }

  void block_comment(intptr_t offset, const char * comment) PRODUCT_RETURN;
  const char* code_string(const char* str) PRODUCT_RETURN_(return NULL;);

  // Log a little info about section usage in the CodeBuffer
  void log_section_sizes(const char* name);

#ifndef PRODUCT
 public:
  // Printing / Decoding
  // decodes from decode_begin() to code_end() and sets decode_begin to end
  void    decode();
  void    print();
#endif
  // Directly disassemble code buffer.
  void    decode(address start, address end);

  // The following header contains architecture-specific implementations
#include CPU_HEADER(codeBuffer)

};

inline bool CodeSection::maybe_expand_to_ensure_remaining(csize_t amount) {
  if (remaining() < amount) { _outer->expand(this, amount); return true; }
  return false;
}

#endif // SHARE_ASM_CODEBUFFER_HPP
