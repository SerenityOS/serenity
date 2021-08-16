/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "ci/ciUtilities.hpp"
#include "classfile/javaClasses.hpp"
#include "code/codeCache.hpp"
#include "compiler/disassembler.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/resourceHash.hpp"

void*       Disassembler::_library               = NULL;
bool        Disassembler::_tried_to_load_library = false;
bool        Disassembler::_library_usable        = false;

// This routine is in the shared library:
Disassembler::decode_func_virtual Disassembler::_decode_instructions_virtual = NULL;

static const char hsdis_library_name[] = "hsdis-" HOTSPOT_LIB_ARCH;
static const char decode_instructions_virtual_name[] = "decode_instructions_virtual";
#define COMMENT_COLUMN  52 LP64_ONLY(+8) /*could be an option*/
#define BYTES_COMMENT   ";..."  /* funky byte display comment */

class decode_env {
 private:
  outputStream* _output;      // where the disassembly is directed to
  CodeBlob*     _codeBlob;    // != NULL only when decoding a CodeBlob
  nmethod*      _nm;          // != NULL only when decoding a nmethod

  address       _start;       // != NULL when decoding a range of unknown type
  address       _end;         // != NULL when decoding a range of unknown type

  char          _option_buf[512];
  char          _print_raw;
  address       _cur_insn;        // address of instruction currently being decoded
  int           _bytes_per_line;  // arch-specific formatting option
  int           _pre_decode_alignment;
  int           _post_decode_alignment;
  bool          _print_file_name;
  bool          _print_help;
  bool          _helpPrinted;
  static bool   _optionsParsed;
  NOT_PRODUCT(const CodeStrings* _strings;)

  enum {
    tabspacing = 8
  };

  // Check if the event matches the expected tag
  // The tag must be a substring of the event, and
  // the tag must be a token in the event, i.e. separated by delimiters
  static bool match(const char* event, const char* tag) {
    size_t eventlen = strlen(event);
    size_t taglen   = strlen(tag);
    if (eventlen < taglen)  // size mismatch
      return false;
    if (strncmp(event, tag, taglen) != 0)  // string mismatch
      return false;
    char delim = event[taglen];
    return delim == '\0' || delim == ' ' || delim == '/' || delim == '=';
  }

  // Merge new option string with previously recorded options
  void collect_options(const char* p) {
    if (p == NULL || p[0] == '\0')  return;
    size_t opt_so_far = strlen(_option_buf);
    if (opt_so_far + 1 + strlen(p) + 1 > sizeof(_option_buf))  return;
    char* fillp = &_option_buf[opt_so_far];
    if (opt_so_far > 0) *fillp++ = ',';
    strcat(fillp, p);
    // replace white space by commas:
    char* q = fillp;
    while ((q = strpbrk(q, " \t\n")) != NULL)
      *q++ = ',';
  }

  void process_options(outputStream* ost);

  void print_insn_labels();
  void print_insn_prefix();
  void print_address(address value);

  // Properly initializes _start/_end. Overwritten too often if
  // printing of instructions is called for each instruction.
  void set_start(address s)   { _start = s; }
  void set_end  (address e)   { _end = e; }
  void set_nm   (nmethod* nm) { _nm = nm; }
  void set_output(outputStream* st) { _output = st; }

#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  // The disassembler library (sometimes) uses tabs to nicely align the instruction operands.
  // Depending on the mnemonic length and the column position where the
  // mnemonic is printed, alignment may turn out to be not so nice.
  // To improve, we assume 8-character tab spacing and left-align the mnemonic on a tab position.
  // Instruction comments are aligned 4 tab positions to the right of the mnemonic.
  void calculate_alignment() {
    _pre_decode_alignment  = ((output()->position()+tabspacing-1)/tabspacing)*tabspacing;
    _post_decode_alignment = _pre_decode_alignment + 4*tabspacing;
  }

  void start_insn(address pc) {
    _cur_insn = pc;
    output()->bol();
    print_insn_labels();
    print_insn_prefix();
  }

  void end_insn(address pc) {
    address pc0 = cur_insn();
    outputStream* st = output();

    if (AbstractDisassembler::show_comment()) {
      if ((_nm != NULL) && _nm->has_code_comment(pc0, pc)) {
        _nm->print_code_comment_on
               (st,
                _post_decode_alignment ? _post_decode_alignment : COMMENT_COLUMN,
                pc0, pc);
        // this calls reloc_string_for which calls oop::print_value_on
      }
      print_hook_comments(pc0, _nm != NULL);
    }
    Disassembler::annotate(pc0, output());
    // follow each complete insn by a nice newline
    st->bol();
  }
#endif

  struct SourceFileInfo {
    struct Link : public CHeapObj<mtCode> {
      const char* file;
      int line;
      Link* next;
      Link(const char* f, int l) : file(f), line(l), next(NULL) {}
    };
    Link *head, *tail;

    void append(const char* file, int line) {
      if (tail != NULL && tail->file == file && tail->line == line) {
        // Don't print duplicated lines at the same address. This could happen with C
        // macros that end up having multiple "__" tokens on the same __LINE__.
        return;
      }
      Link *link = new Link(file, line);
      if (head == NULL) {
        head = tail = link;
      } else {
        tail->next = link;
        tail = link;
      }
    }
    SourceFileInfo(const char* file, int line) : head(NULL), tail(NULL) {
      append(file, line);
    }
  };

  typedef ResourceHashtable<
      address, SourceFileInfo,
      15889,      // prime number
      ResourceObj::C_HEAP> SourceFileInfoTable;

  static SourceFileInfoTable* _src_table;
  static const char* _cached_src;
  static GrowableArray<const char*>* _cached_src_lines;

  static SourceFileInfoTable& src_table() {
    if (_src_table == NULL) {
      _src_table = new (ResourceObj::C_HEAP, mtCode)SourceFileInfoTable();
    }
    return *_src_table;
  }

 public:
  decode_env(CodeBlob*   code, outputStream* output);
  decode_env(nmethod*    code, outputStream* output);
  // Constructor for a 'decode_env' to decode an arbitrary
  // piece of memory, hopefully containing code.
  decode_env(address start, address end, outputStream* output, const CodeStrings* strings = NULL);

  // Add 'original_start' argument which is the the original address
  // the instructions were located at (if this is not equal to 'start').
  address decode_instructions(address start, address end, address original_start = NULL);

  address handle_event(const char* event, address arg);

  outputStream* output()   { return _output; }
  address       cur_insn() { return _cur_insn; }
  const char*   options()  { return _option_buf; }
  static void   hook(const char* file, int line, address pc);
  void print_hook_comments(address pc, bool newline);
};

bool decode_env::_optionsParsed = false;

decode_env::SourceFileInfoTable* decode_env::_src_table = NULL;
const char* decode_env::_cached_src = NULL;
GrowableArray<const char*>* decode_env::_cached_src_lines = NULL;

void decode_env::hook(const char* file, int line, address pc) {
  // For simplication, we never free from this table. It's really not
  // necessary as we add to the table only when PrintInterpreter is true,
  // which means we are debugging the VM and a little bit of extra
  // memory usage doesn't matter.
  SourceFileInfo* found = src_table().get(pc);
  if (found != NULL) {
    found->append(file, line);
  } else {
    SourceFileInfo sfi(file, line);
    src_table().put(pc, sfi); // sfi is copied by value
  }
}

void decode_env::print_hook_comments(address pc, bool newline) {
  SourceFileInfo* found = src_table().get(pc);
  outputStream* st = output();
  if (found != NULL) {
    for (SourceFileInfo::Link *link = found->head; link; link = link->next) {
      const char* file = link->file;
      int line = link->line;
      if (_cached_src == NULL || strcmp(_cached_src, file) != 0) {
        FILE* fp;

        // _cached_src_lines is a single cache of the lines of a source file, and we refill this cache
        // every time we need to print a line from a different source file. It's not the fastest,
        // but seems bearable.
        if (_cached_src_lines != NULL) {
          for (int i=0; i<_cached_src_lines->length(); i++) {
            os::free((void*)_cached_src_lines->at(i));
          }
          _cached_src_lines->clear();
        } else {
          _cached_src_lines = new (ResourceObj::C_HEAP, mtCode)GrowableArray<const char*>(0, mtCode);
        }

        if ((fp = fopen(file, "r")) == NULL) {
          _cached_src = NULL;
          return;
        }
        _cached_src = file;

        char line[500]; // don't write lines that are too long in your source files!
        while (fgets(line, sizeof(line), fp) != NULL) {
          size_t len = strlen(line);
          if (len > 0 && line[len-1] == '\n') {
            line[len-1] = '\0';
          }
          _cached_src_lines->append(os::strdup(line));
        }
        fclose(fp);
        _print_file_name = true;
      }

      if (_print_file_name) {
        // We print the file name whenever we switch to a new file, or when
        // Disassembler::decode is called to disassemble a new block of code.
        _print_file_name = false;
        if (newline) {
          st->cr();
        }
        st->move_to(COMMENT_COLUMN);
        st->print(";;@FILE: %s", file);
        newline = true;
      }

      int index = line - 1; // 1-based line number -> 0-based index.
      if (index >= _cached_src_lines->length()) {
        // This could happen if source file is mismatched.
      } else {
        const char* source_line = _cached_src_lines->at(index);
        if (newline) {
          st->cr();
        }
        st->move_to(COMMENT_COLUMN);
        st->print(";;%5d: %s", line, source_line);
        newline = true;
      }
    }
  }
}

decode_env::decode_env(CodeBlob* code, outputStream* output) :
  _output(output ? output : tty),
  _codeBlob(code),
  _nm(_codeBlob != NULL && _codeBlob->is_nmethod() ? (nmethod*) code : NULL),
  _start(NULL),
  _end(NULL),
  _option_buf(),
  _print_raw(0),
  _cur_insn(NULL),
  _bytes_per_line(0),
  _pre_decode_alignment(0),
  _post_decode_alignment(0),
  _print_file_name(false),
  _print_help(false),
  _helpPrinted(false)
  NOT_PRODUCT(COMMA _strings(NULL)) {

  memset(_option_buf, 0, sizeof(_option_buf));
  process_options(_output);

}

decode_env::decode_env(nmethod* code, outputStream* output) :
  _output(output ? output : tty),
  _codeBlob(NULL),
  _nm(code),
  _start(_nm->code_begin()),
  _end(_nm->code_end()),
  _option_buf(),
  _print_raw(0),
  _cur_insn(NULL),
  _bytes_per_line(0),
  _pre_decode_alignment(0),
  _post_decode_alignment(0),
  _print_file_name(false),
  _print_help(false),
  _helpPrinted(false)
  NOT_PRODUCT(COMMA _strings(NULL))  {

  memset(_option_buf, 0, sizeof(_option_buf));
  process_options(_output);
}

// Constructor for a 'decode_env' to decode a memory range [start, end)
// of unknown origin, assuming it contains code.
decode_env::decode_env(address start, address end, outputStream* output, const CodeStrings* c) :
  _output(output ? output : tty),
  _codeBlob(NULL),
  _nm(NULL),
  _start(start),
  _end(end),
  _option_buf(),
  _print_raw(0),
  _cur_insn(NULL),
  _bytes_per_line(0),
  _pre_decode_alignment(0),
  _post_decode_alignment(0),
  _print_file_name(false),
  _print_help(false),
  _helpPrinted(false)
  NOT_PRODUCT(COMMA _strings(c))  {

  assert(start < end, "Range must have a positive size, [" PTR_FORMAT ".." PTR_FORMAT ").", p2i(start), p2i(end));
  memset(_option_buf, 0, sizeof(_option_buf));
  process_options(_output);
}

void decode_env::process_options(outputStream* ost) {
  // by default, output pc but not bytes:
  _print_help      = false;
  _bytes_per_line  = Disassembler::pd_instruction_alignment();
  _print_file_name = true;

  // parse the global option string
  // We need to fill the options buffer for each newly created
  // decode_env instance. The hsdis_* library looks for options
  // in that buffer.
  collect_options(Disassembler::pd_cpu_opts());
  collect_options(PrintAssemblyOptions);

  if (strstr(options(), "print-raw")) {
    _print_raw = (strstr(options(), "xml") ? 2 : 1);
  }

  if (_optionsParsed) return;  // parse only once

  if (strstr(options(), "help")) {
    _print_help = true;
  }
  if (strstr(options(), "align-instr")) {
    AbstractDisassembler::toggle_align_instr();
  }
  if (strstr(options(), "show-pc")) {
    AbstractDisassembler::toggle_show_pc();
  }
  if (strstr(options(), "show-offset")) {
    AbstractDisassembler::toggle_show_offset();
  }
  if (strstr(options(), "show-bytes")) {
    AbstractDisassembler::toggle_show_bytes();
  }
  if (strstr(options(), "show-data-hex")) {
    AbstractDisassembler::toggle_show_data_hex();
  }
  if (strstr(options(), "show-data-int")) {
    AbstractDisassembler::toggle_show_data_int();
  }
  if (strstr(options(), "show-data-float")) {
    AbstractDisassembler::toggle_show_data_float();
  }
  if (strstr(options(), "show-structs")) {
    AbstractDisassembler::toggle_show_structs();
  }
  if (strstr(options(), "show-comment")) {
    AbstractDisassembler::toggle_show_comment();
  }
  if (strstr(options(), "show-block-comment")) {
    AbstractDisassembler::toggle_show_block_comment();
  }
  _optionsParsed = true;

  if (_print_help && ! _helpPrinted) {
    _helpPrinted = true;
    ost->print_cr("PrintAssemblyOptions help:");
    ost->print_cr("  print-raw       test plugin by requesting raw output");
    ost->print_cr("  print-raw-xml   test plugin by requesting raw xml");
    ost->cr();
    ost->print_cr("  show-pc            toggle printing current pc,        currently %s", AbstractDisassembler::show_pc()            ? "ON" : "OFF");
    ost->print_cr("  show-offset        toggle printing current offset,    currently %s", AbstractDisassembler::show_offset()        ? "ON" : "OFF");
    ost->print_cr("  show-bytes         toggle printing instruction bytes, currently %s", AbstractDisassembler::show_bytes()         ? "ON" : "OFF");
    ost->print_cr("  show-data-hex      toggle formatting data as hex,     currently %s", AbstractDisassembler::show_data_hex()      ? "ON" : "OFF");
    ost->print_cr("  show-data-int      toggle formatting data as int,     currently %s", AbstractDisassembler::show_data_int()      ? "ON" : "OFF");
    ost->print_cr("  show-data-float    toggle formatting data as float,   currently %s", AbstractDisassembler::show_data_float()    ? "ON" : "OFF");
    ost->print_cr("  show-structs       toggle compiler data structures,   currently %s", AbstractDisassembler::show_structs()       ? "ON" : "OFF");
    ost->print_cr("  show-comment       toggle instruction comments,       currently %s", AbstractDisassembler::show_comment()       ? "ON" : "OFF");
    ost->print_cr("  show-block-comment toggle block comments,             currently %s", AbstractDisassembler::show_block_comment() ? "ON" : "OFF");
    ost->print_cr("  align-instr        toggle instruction alignment,      currently %s", AbstractDisassembler::align_instr()        ? "ON" : "OFF");
    ost->print_cr("combined options: %s", options());
  }
}

// Disassembly Event Handler.
// This method receives events from the disassembler library hsdis
// via event_to_env for each decoding step (installed by
// Disassembler::decode_instructions(), replacing the default
// callback method). This enables dumping additional info
// and custom line formatting.
// In a future extension, calling a custom decode method will be
// supported. We can use such a method to decode instructions the
// binutils decoder does not handle to our liking (suboptimal
// formatting, incomplete information, ...).
// Returns:
// - NULL for all standard invocations. The function result is not
//        examined (as of now, 20190409) by the hsdis decoder loop.
// - next for 'insn0' invocations.
//        next == arg: the custom decoder didn't do anything.
//        next >  arg: the custom decoder did decode the instruction.
//                     next points to the next undecoded instruction
//                     (continuation point for decoder loop).
//
// "Normal" sequence of events:
//  insns   - start of instruction stream decoding
//  mach    - display architecture
//  format  - display bytes-per-line
//  for each instruction:
//    insn    - start of instruction decoding
//    insn0   - custom decoder invocation (if any)
//    addr    - print address value
//    /insn   - end of instruction decoding
//  /insns  - premature end of instruction stream due to no progress
//
address decode_env::handle_event(const char* event, address arg) {

#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)

  //---<  Event: end decoding loop (error, no progress)  >---
  if (decode_env::match(event, "/insns")) {
    // Nothing to be done here.
    return NULL;
  }

  //---<  Event: start decoding loop  >---
  if (decode_env::match(event, "insns")) {
    // Nothing to be done here.
    return NULL;
  }

  //---<  Event: finish decoding an instruction  >---
  if (decode_env::match(event, "/insn")) {
    output()->fill_to(_post_decode_alignment);
    end_insn(arg);
    return NULL;
  }

  //---<  Event: start decoding an instruction  >---
  if (decode_env::match(event, "insn")) {
    start_insn(arg);
  } else if (match(event, "/insn")) {
    end_insn(arg);
  } else if (match(event, "addr")) {
    if (arg != NULL) {
      print_address(arg);
      return arg;
    }
    calculate_alignment();
    output()->fill_to(_pre_decode_alignment);
    return NULL;
  }

  //---<  Event: call custom decoder (platform specific)  >---
  if (decode_env::match(event, "insn0")) {
    return Disassembler::decode_instruction0(arg, output(), arg);
  }

  //---<  Event: Print address  >---
  if (decode_env::match(event, "addr")) {
    print_address(arg);
    return arg;
  }

  //---<  Event: mach (inform about machine architecture)  >---
  // This event is problematic because it messes up the output.
  // The event is fired after the instruction address has already
  // been printed. The decoded instruction (event "insn") is
  // printed afterwards. That doesn't look nice.
  if (decode_env::match(event, "mach")) {
    guarantee(arg != NULL, "event_to_env - arg must not be NULL for event 'mach'");
    static char buffer[64] = { 0, };
    // Output suppressed because it messes up disassembly.
    // Only print this when the mach changes.
    if (false && (strcmp(buffer, (const char*)arg) != 0 ||
                  strlen((const char*)arg) > sizeof(buffer) - 1)) {
      // Only print this when the mach changes
      strncpy(buffer, (const char*)arg, sizeof(buffer) - 1);
      buffer[sizeof(buffer) - 1] = '\0';
      output()->print_cr("[Disassembling for mach='%s']", (const char*)arg);
    }
    return NULL;
  }

  //---<  Event: format bytes-per-line  >---
  if (decode_env::match(event, "format bytes-per-line")) {
    _bytes_per_line = (int) (intptr_t) arg;
    return NULL;
  }
#endif
  return NULL;
}

static void* event_to_env(void* env_pv, const char* event, void* arg) {
  decode_env* env = (decode_env*) env_pv;
  return env->handle_event(event, (address) arg);
}

// called by the disassembler to print out jump targets and data addresses
void decode_env::print_address(address adr) {
  outputStream* st = output();

  if (adr == NULL) {
    st->print("NULL");
    return;
  }

  int small_num = (int)(intptr_t)adr;
  if ((intptr_t)adr == (intptr_t)small_num
      && -1 <= small_num && small_num <= 9) {
    st->print("%d", small_num);
    return;
  }

  if (Universe::is_fully_initialized()) {
    if (StubRoutines::contains(adr)) {
      StubCodeDesc* desc = StubCodeDesc::desc_for(adr);
      if (desc == NULL) {
        desc = StubCodeDesc::desc_for(adr + frame::pc_return_offset);
      }
      if (desc != NULL) {
        st->print("Stub::%s", desc->name());
        if (desc->begin() != adr) {
          st->print(INTX_FORMAT_W(+) " " PTR_FORMAT, adr - desc->begin(), p2i(adr));
        } else if (WizardMode) {
          st->print(" " PTR_FORMAT, p2i(adr));
        }
        return;
      }
      st->print("Stub::<unknown> " PTR_FORMAT, p2i(adr));
      return;
    }

    BarrierSet* bs = BarrierSet::barrier_set();
    if (bs->is_a(BarrierSet::CardTableBarrierSet) &&
        adr == ci_card_table_address_as<address>()) {
      st->print("word_map_base");
      if (WizardMode) st->print(" " INTPTR_FORMAT, p2i(adr));
      return;
    }
  }

  if (_nm == NULL) {
    // Don't do this for native methods, as the function name will be printed in
    // nmethod::reloc_string_for().
    // Allocate the buffer on the stack instead of as RESOURCE array.
    // In case we do DecodeErrorFile, Thread will not be initialized,
    // causing a "assert(current != __null) failed" failure.
    const int buflen = 1024;
    char buf[buflen];
    int offset;
    if (os::dll_address_to_function_name(adr, buf, buflen, &offset)) {
      st->print(PTR_FORMAT " = %s",  p2i(adr), buf);
      if (offset != 0) {
        st->print("+%d", offset);
      }
      return;
    }
  }

  // Fall through to a simple (hexadecimal) numeral.
  st->print(PTR_FORMAT, p2i(adr));
}

void decode_env::print_insn_labels() {
  if (AbstractDisassembler::show_block_comment()) {
    address       p  = cur_insn();
    outputStream* st = output();

    //---<  Block comments for nmethod  >---
    // Outputs a bol() before and a cr() after, but only if a comment is printed.
    // Prints nmethod_section_label as well.
    if (_nm != NULL) {
      _nm->print_block_comment(st, p);
    }
    if (_codeBlob != NULL) {
      _codeBlob->print_block_comment(st, p);
    }
#ifndef PRODUCT
    if (_strings != NULL) {
      _strings->print_block_comment(st, (intptr_t)(p - _start));
    }
#endif
  }
}

void decode_env::print_insn_prefix() {
  address       p  = cur_insn();
  outputStream* st = output();
  AbstractDisassembler::print_location(p, _start, _end, st, false, false);
  AbstractDisassembler::print_instruction(p, Assembler::instr_len(p), Assembler::instr_maxlen(), st, true, false);
}

ATTRIBUTE_PRINTF(2, 3)
static int printf_to_env(void* env_pv, const char* format, ...) {
  decode_env* env = (decode_env*) env_pv;
  outputStream* st = env->output();
  size_t flen = strlen(format);
  const char* raw = NULL;
  if (flen == 0)  return 0;
  if (flen == 1 && format[0] == '\n') { st->bol(); return 1; }
  if (flen < 2 ||
      strchr(format, '%') == NULL) {
    raw = format;
  } else if (format[0] == '%' && format[1] == '%' &&
             strchr(format+2, '%') == NULL) {
    // happens a lot on machines with names like %foo
    flen--;
    raw = format+1;
  }
  if (raw != NULL) {
    st->print_raw(raw, flen);
    return (int) flen;
  }
  va_list ap;
  va_start(ap, format);
  julong cnt0 = st->count();
  st->vprint(format, ap);
  julong cnt1 = st->count();
  va_end(ap);
  return (int)(cnt1 - cnt0);
}

// The 'original_start' argument holds the the original address where
// the instructions were located in the originating system. If zero (NULL)
// is passed in, there is no original address.
address decode_env::decode_instructions(address start, address end, address original_start /* = 0*/) {
  // CodeComment in Stubs.
  // Properly initialize _start/_end. Overwritten too often if
  // printing of instructions is called for each instruction.
  assert((_start == NULL) || (start == NULL) || (_start == start), "don't overwrite CTOR values");
  assert((_end   == NULL) || (end   == NULL) || (_end   == end  ), "don't overwrite CTOR values");
  if (start != NULL) set_start(start);
  if (end   != NULL) set_end(end);
  if (original_start == NULL) {
    original_start = start;
  }

  //---<  Check (and correct) alignment  >---
  // Don't check alignment of end, it is not aligned.
  if (((uint64_t)start & ((uint64_t)Disassembler::pd_instruction_alignment() - 1)) != 0) {
    output()->print_cr("Decode range start:" PTR_FORMAT ": ... (unaligned)", p2i(start));
    start = (address)((uint64_t)start & ~((uint64_t)Disassembler::pd_instruction_alignment() - 1));
  }

  // Trying to decode instructions doesn't make sense if we
  // couldn't load the disassembler library.
  if (Disassembler::is_abstract()) {
    return NULL;
  }

  // decode a series of instructions and return the end of the last instruction

  if (_print_raw) {
    // Print whatever the library wants to print, w/o fancy callbacks.
    // This is mainly for debugging the library itself.
    FILE* out = stdout;
    FILE* xmlout = (_print_raw > 1 ? out : NULL);
    return
      (address)
      (*Disassembler::_decode_instructions_virtual)((uintptr_t)start, (uintptr_t)end,
                                                    start, end - start,
                                                    NULL, (void*) xmlout,
                                                    NULL, (void*) out,
                                                    options(), 0/*nice new line*/);
  }

  return
    (address)
    (*Disassembler::_decode_instructions_virtual)((uintptr_t)start, (uintptr_t)end,
                                                  start, end - start,
                                                  &event_to_env,  (void*) this,
                                                  &printf_to_env, (void*) this,
                                                  options(), 0/*nice new line*/);
}

// ----------------------------------------------------------------------------
// Disassembler
// Used as a static wrapper for decode_env.
// Each method will create a decode_env before decoding.
// You can call the decode_env methods directly if you already have one.

void* Disassembler::dll_load(char* buf, int buflen, int offset, char* ebuf, int ebuflen, outputStream* st) {
  int sz = buflen - offset;
  int written = jio_snprintf(&buf[offset], sz, "%s%s", hsdis_library_name, os::dll_file_extension());
  if (written < sz) { // written successfully, not truncated.
    if (Verbose) st->print_cr("Trying to load: %s", buf);
    return os::dll_load(buf, ebuf, ebuflen);
  } else if (Verbose) {
    st->print_cr("Try to load hsdis library failed: the length of path is beyond the OS limit");
  }
  return NULL;
}

bool Disassembler::load_library(outputStream* st) {
  // Do not try to load multiple times. Failed once -> fails always.
  // To force retry in debugger: assign _tried_to_load_library=0
  if (_tried_to_load_library) {
    return _library_usable;
  }

#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  // Print to given stream, if any.
  // Print to tty if Verbose is on and no stream given.
  st = ((st == NULL) && Verbose) ? tty : st;

  // Compute fully qualified library name.
  char ebuf[1024];
  char buf[JVM_MAXPATHLEN];
  os::jvm_path(buf, sizeof(buf));
  int jvm_offset = -1;
  int lib_offset = -1;
#ifdef STATIC_BUILD
  char* p = strrchr(buf, '/');
  *p = '\0';
  strcat(p, "/lib/");
  lib_offset = jvm_offset = strlen(buf);
#else
  {
    // Match "libjvm" instead of "jvm" on *nix platforms. Creates better matches.
    // Match "[lib]jvm[^/]*" in jvm_path.
    const char* base = buf;
    const char* p = strrchr(buf, *os::file_separator());
    if (p != NULL) lib_offset = p - base + 1; // this points to the first char after separator
#ifdef _WIN32
    p = strstr(p ? p : base, "jvm");
    if (p != NULL) jvm_offset = p - base;     // this points to 'j' in jvm.
#else
    p = strstr(p ? p : base, "libjvm");
    if (p != NULL) jvm_offset = p - base + 3; // this points to 'j' in libjvm.
#endif
  }
#endif

  // Find the disassembler shared library.
  // Search for several paths derived from libjvm, in this order:
  // 1. <home>/lib/<vm>/libhsdis-<arch>.so  (for compatibility)
  // 2. <home>/lib/<vm>/hsdis-<arch>.so
  // 3. <home>/lib/hsdis-<arch>.so
  // 4. hsdis-<arch>.so  (using LD_LIBRARY_PATH)
  if (jvm_offset >= 0) {
    // 1. <home>/lib/<vm>/libhsdis-<arch>.so
    _library = dll_load(buf, sizeof buf, jvm_offset, ebuf, sizeof ebuf, st);
    if (_library == NULL && lib_offset >= 0) {
      // 2. <home>/lib/<vm>/hsdis-<arch>.so
      _library = dll_load(buf, sizeof buf, lib_offset, ebuf, sizeof ebuf, st);
    }
    if (_library == NULL && lib_offset > 0) {
      // 3. <home>/lib/hsdis-<arch>.so
      buf[lib_offset - 1] = '\0';
      const char* p = strrchr(buf, *os::file_separator());
      if (p != NULL) {
        lib_offset = p - buf + 1;
        _library = dll_load(buf, sizeof buf, lib_offset, ebuf, sizeof ebuf, st);
      }
    }
  }
  if (_library == NULL) {
    _library = dll_load(buf, sizeof buf, 0, ebuf, sizeof ebuf, st);
  }

  // load the decoder function to use.
  if (_library != NULL) {
    _decode_instructions_virtual = CAST_TO_FN_PTR(Disassembler::decode_func_virtual,
                                          os::dll_lookup(_library, decode_instructions_virtual_name));
  }
  _tried_to_load_library = true;
  _library_usable        = _decode_instructions_virtual != NULL;

  // Create a dummy environment to initialize PrintAssemblyOptions.
  // The PrintAssemblyOptions must be known for abstract disassemblies as well.
  decode_env dummy((unsigned char*)(&buf[0]), (unsigned char*)(&buf[1]), st);

  // Report problems during dll_load or dll_lookup, if any.
  if (st != NULL) {
    // Success.
    if (_library_usable) {
      st->print_cr("Loaded disassembler from %s", buf);
    } else {
      st->print_cr("Could not load %s; %s; %s",
                   buf,
                   ((_library != NULL)
                    ? "entry point is missing"
                    : ((WizardMode || PrintMiscellaneous)
                       ? (const char*)ebuf
                       : "library not loadable")),
                   "PrintAssembly defaults to abstract disassembly.");
    }
  }
#endif
  return _library_usable;
}


// Directly disassemble code blob.
void Disassembler::decode(CodeBlob* cb, outputStream* st) {
#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  if (cb->is_nmethod()) {
    // If we  have an nmethod at hand,
    // call the specialized decoder directly.
    decode((nmethod*)cb, st);
    return;
  }

  decode_env env(cb, st);
  env.output()->print_cr("--------------------------------------------------------------------------------");
  env.output()->print("Decoding CodeBlob");
  if (cb->name() != NULL) {
    env.output()->print(", name: %s,", cb->name());
  }
  env.output()->print_cr(" at  [" PTR_FORMAT ", " PTR_FORMAT "]  " JLONG_FORMAT " bytes", p2i(cb->code_begin()), p2i(cb->code_end()), ((jlong)(cb->code_end() - cb->code_begin())));

  if (is_abstract()) {
    AbstractDisassembler::decode_abstract(cb->code_begin(), cb->code_end(), env.output(), Assembler::instr_maxlen());
  } else {
    env.decode_instructions(cb->code_begin(), cb->code_end());
  }
  env.output()->print_cr("--------------------------------------------------------------------------------");
#endif
}

// Decode a nmethod.
// This includes printing the constant pool and all code segments.
// The nmethod data structures (oop maps, relocations and the like) are not printed.
void Disassembler::decode(nmethod* nm, outputStream* st) {
#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  ttyLocker ttyl;

  decode_env env(nm, st);
  env.output()->print_cr("--------------------------------------------------------------------------------");
  nm->print_constant_pool(env.output());
  env.output()->print_cr("--------------------------------------------------------------------------------");
  env.output()->cr();
  if (is_abstract()) {
    AbstractDisassembler::decode_abstract(nm->code_begin(), nm->code_end(), env.output(), Assembler::instr_maxlen());
  } else {
    env.decode_instructions(nm->code_begin(), nm->code_end());
  }
  env.output()->print_cr("--------------------------------------------------------------------------------");
#endif
}

// Decode a range, given as [start address, end address)
void Disassembler::decode(address start, address end, outputStream* st, const CodeStrings* c) {
#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  //---<  Test memory before decoding  >---
  if (!os::is_readable_range(start, end)) {
    //---<  Allow output suppression, but prevent writing to a NULL stream. Could happen with +PrintStubCode.  >---
    if (st != NULL) {
      st->print("Memory range [" PTR_FORMAT ".." PTR_FORMAT "] not readable", p2i(start), p2i(end));
    }
    return;
  }

  if (is_abstract()) {
    AbstractDisassembler::decode_abstract(start, end, st, Assembler::instr_maxlen());
    return;
  }

// Don't do that fancy stuff. If we just have two addresses, live with it
// and treat the memory contents as "amorphic" piece of code.
#if 0
  CodeBlob* cb = CodeCache::find_blob_unsafe(start);
  if (cb != NULL) {
    // If we  have an CodeBlob at hand,
    // call the specialized decoder directly.
    decode(cb, st, c);
  } else
#endif
  {
    // This seems to be just a chunk of memory.
    decode_env env(start, end, st, c);
    env.output()->print_cr("--------------------------------------------------------------------------------");
    env.decode_instructions(start, end);
    env.output()->print_cr("--------------------------------------------------------------------------------");
  }
#endif
}

// To prevent excessive code expansion in the interpreter generator, we
// do not inline this function into Disassembler::hook().
void Disassembler::_hook(const char* file, int line, MacroAssembler* masm) {
  decode_env::hook(file, line, masm->code_section()->end());
}
