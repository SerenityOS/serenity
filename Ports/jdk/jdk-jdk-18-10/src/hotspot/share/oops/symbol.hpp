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

#ifndef SHARE_OOPS_SYMBOL_HPP
#define SHARE_OOPS_SYMBOL_HPP

#include "memory/allocation.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmEnums.hpp"

// A Symbol is a canonicalized string.
// All Symbols reside in global SymbolTable and are reference counted.

// Reference counting
//
// All Symbols are allocated and added to the SymbolTable.
// When a class is unloaded, the reference counts of the Symbol pointers in
// the ConstantPool and in InstanceKlass (see release_C_heap_structures) are
// decremented.  When the reference count for a Symbol goes to 0, the garbage
// collector can free the Symbol and remove it from the SymbolTable.
//
// 0) Symbols need to be reference counted when a pointer to the Symbol is
// saved in persistent storage.  This does not include the pointer
// in the SymbolTable bucket (the _literal field in HashtableEntry)
// that points to the Symbol.  All other stores of a Symbol*
// to a field of a persistent variable (e.g., the _name filed in
// fieldDescriptor or _ptr in a CPSlot) is reference counted.
//
// 1) The lookup of a "name" in the SymbolTable either creates a Symbol F for
// "name" and returns a pointer to F or finds a pre-existing Symbol F for
// "name" and returns a pointer to it. In both cases the reference count for F
// is incremented under the assumption that a pointer to F will be created from
// the return value. Thus the increment of the reference count is on the lookup
// and not on the assignment to the new Symbol*.  That is
//    Symbol* G = lookup()
//                ^ increment on lookup()
// and not
//    Symbol* G = lookup()
//              ^ increment on assignmnet
// The reference count must be decremented manually when the copy of the
// pointer G is destroyed.
//
// 2) For a local Symbol* A that is a copy of an existing Symbol* B, the
// reference counting is elided when the scope of B is greater than the scope
// of A.  For example, in the code fragment
// below "klass" is passed as a parameter to the method.  Symbol* "kn"
// is a copy of the name in "klass".
//
//   Symbol*  kn = klass->name();
//   unsigned int d_hash = dictionary()->compute_hash(kn, class_loader);
//
// The scope of "klass" is greater than the scope of "kn" so the reference
// counting for "kn" is elided.
//
// Symbol* copied from ConstantPool entries are good candidates for reference
// counting elision.  The ConstantPool entries for a class C exist until C is
// unloaded.  If a Symbol* is copied out of the ConstantPool into Symbol* X,
// the Symbol* in the ConstantPool will in general out live X so the reference
// counting on X can be elided.
//
// For cases where the scope of A is not greater than the scope of B,
// the reference counting is explicitly done.  See ciSymbol,
// ResolutionErrorEntry and ClassVerifier for examples.
//
// 3) When a Symbol K is created for temporary use, generally for substrings of
// an existing symbol or to create a new symbol, assign it to a
// TempNewSymbol. The SymbolTable methods new_symbol(), lookup()
// and probe() all potentially return a pointer to a new Symbol.
// The allocation (or lookup) of K increments the reference count for K
// and the destructor decrements the reference count.
//
// This cannot be inherited from ResourceObj because it cannot have a vtable.
// Since sometimes this is allocated from Metadata, pick a base allocation
// type without virtual functions.
class ClassLoaderData;

// Set _refcount to PERM_REFCOUNT to prevent the Symbol from being freed.
#ifndef PERM_REFCOUNT
#define PERM_REFCOUNT 0xffff
#endif

class Symbol : public MetaspaceObj {
  friend class VMStructs;
  friend class SymbolTable;
  friend class vmSymbols;
  friend class JVMCIVMStructs;

 private:

  // This is an int because it needs atomic operation on the refcount.  Mask hash
  // in high half word. length is the number of UTF8 characters in the symbol
  volatile uint32_t _hash_and_refcount;
  u2 _length;
  u1 _body[2];

  static Symbol* _vm_symbols[];

  enum {
    max_symbol_length = 0xffff
  };

  static int byte_size(int length) {
    // minimum number of natural words needed to hold these bits (no non-heap version)
    return (int)(sizeof(Symbol) + (length > 2 ? length - 2 : 0));
  }
  static int size(int length) {
    // minimum number of natural words needed to hold these bits (no non-heap version)
    return (int)heap_word_size(byte_size(length));
  }

  Symbol(const u1* name, int length, int refcount);
  void* operator new(size_t size, int len) throw();
  void* operator new(size_t size, int len, Arena* arena) throw();

  void  operator delete(void* p);

  static short extract_hash(uint32_t value)   { return (short)(value >> 16); }
  static int extract_refcount(uint32_t value) { return value & 0xffff; }
  static uint32_t pack_hash_and_refcount(short hash, int refcount);

  int length() const   { return _length; }

 public:
  // Low-level access (used with care, since not GC-safe)
  const u1* base() const { return &_body[0]; }

  int size()                { return size(utf8_length()); }
  int byte_size()           { return byte_size(utf8_length()); }

  // Symbols should be stored in the read-only region of CDS archive.
  static bool is_read_only_by_default() { return true; }

  // Returns the largest size symbol we can safely hold.
  static int max_length() { return max_symbol_length; }
  unsigned identity_hash() const {
    unsigned addr_bits = (unsigned)((uintptr_t)this >> (LogBytesPerWord + 3));
    return ((unsigned)extract_hash(_hash_and_refcount) & 0xffff) |
           ((addr_bits ^ (length() << 8) ^ (( _body[0] << 8) | _body[1])) << 16);
  }

  // Reference counting.  See comments above this class for when to use.
  int refcount() const { return extract_refcount(_hash_and_refcount); }
  bool try_increment_refcount();
  void increment_refcount();
  void decrement_refcount();
  bool is_permanent() const {
    return (refcount() == PERM_REFCOUNT);
  }
  void update_identity_hash() NOT_CDS_RETURN;
  void set_permanent() NOT_CDS_RETURN;
  void make_permanent();

  // Function char_at() returns the Symbol's selected u1 byte as a char type.
  //
  // Note that all multi-byte chars have the sign bit set on all their bytes.
  // No single byte chars have their sign bit set.
  char char_at(int index) const {
    assert(index >=0 && index < length(), "symbol index overflow");
    return (char)base()[index];
  }

  const u1* bytes() const { return base(); }

  int utf8_length() const { return length(); }

  // Compares the symbol with a string.
  bool equals(const char* str, int len) const {
    int l = utf8_length();
    if (l != len) return false;
    return contains_utf8_at(0, str, len);
  }
  bool equals(const char* str) const { return equals(str, (int) strlen(str)); }

  // Tests if the symbol starts with the given prefix.
  bool starts_with(const char* prefix, int len) const {
    return contains_utf8_at(0, prefix, len);
  }
  bool starts_with(const char* prefix) const {
    return starts_with(prefix, (int) strlen(prefix));
  }
  bool starts_with(int prefix_char) const {
    return contains_byte_at(0, prefix_char);
  }
  // Tests if the symbol ends with the given suffix.
  bool ends_with(const char* suffix, int len) const {
    return contains_utf8_at(utf8_length() - len, suffix, len);
  }
  bool ends_with(const char* suffix) const {
    return ends_with(suffix, (int) strlen(suffix));
  }
  bool ends_with(int suffix_char) const {
    return contains_byte_at(utf8_length() - 1, suffix_char);
  }

  // Tests if the symbol contains the given utf8 substring
  // at the given byte position.
  bool contains_utf8_at(int position, const char* substring, int len) const {
    assert(len >= 0 && substring != NULL, "substring must be valid");
    if (position < 0)  return false;  // can happen with ends_with
    if (position + len > utf8_length()) return false;
    return (memcmp((char*)base() + position, substring, len) == 0);
  }

  // Tests if the symbol contains the given byte at the given position.
  bool contains_byte_at(int position, char code_byte) const {
    if (position < 0)  return false;  // can happen with ends_with
    if (position >= utf8_length()) return false;
    return code_byte == char_at(position);
  }

  // Tests if the symbol starts with the given prefix.
  int index_of_at(int i, const char* str, int len) const;

  // Three-way compare for sorting; returns -1/0/1 if receiver is </==/> than arg
  // note that the ordering is not alfabetical
  inline int fast_compare(const Symbol* other) const;

  // Returns receiver converted to null-terminated UTF-8 string; string is
  // allocated in resource area, or in the char buffer provided by caller.
  char* as_C_string() const;
  char* as_C_string(char* buf, int size) const;

  // Returns an escaped form of a Java string.
  char* as_quoted_ascii() const;

  // Returns a null terminated utf8 string in a resource array
  char* as_utf8() const { return as_C_string(); }

  jchar* as_unicode(int& length) const;

  // Treating this symbol as a class name, returns the Java name for the class.
  // String is allocated in resource area if buffer is not provided.
  // See Klass::external_name()
  const char* as_klass_external_name() const;
  const char* as_klass_external_name(char* buf, int size) const;

  // Treating the symbol as a signature, print the return
  // type to the outputStream. Prints external names as 'double' or
  // 'java.lang.Object[][]'.
  void print_as_signature_external_return_type(outputStream *os);
  // Treating the symbol as a signature, print the parameter types
  // seperated by ', ' to the outputStream.  Prints external names as
  //  'double' or 'java.lang.Object[][]'.
  void print_as_signature_external_parameters(outputStream *os);

  void metaspace_pointers_do(MetaspaceClosure* it);
  MetaspaceObj::Type type() const { return SymbolType; }

  // Printing
  void print_symbol_on(outputStream* st = NULL) const;
  void print_utf8_on(outputStream* st) const;
  void print_on(outputStream* st) const;         // First level print
  void print_value_on(outputStream* st) const;   // Second level print.

  // printing on default output stream
  void print() const;
  void print_value() const;

  static bool is_valid(Symbol* s);

  static bool is_valid_id(vmSymbolID vm_symbol_id) PRODUCT_RETURN_(return true;);

  static Symbol* vm_symbol_at(vmSymbolID vm_symbol_id) {
    assert(is_valid_id(vm_symbol_id), "must be");
    return _vm_symbols[static_cast<int>(vm_symbol_id)];
  }

#ifndef PRODUCT
  // Empty constructor to create a dummy symbol object on stack
  // only for getting its vtable pointer.
  Symbol() { }

  static size_t _total_count;
#endif
};

// Note: this comparison is used for vtable sorting only; it doesn't matter
// what order it defines, as long as it is a total, time-invariant order
// Since Symbol*s are in C_HEAP, their relative order in memory never changes,
// so use address comparison for speed
int Symbol::fast_compare(const Symbol* other) const {
 return (((uintptr_t)this < (uintptr_t)other) ? -1
   : ((uintptr_t)this == (uintptr_t) other) ? 0 : 1);
}
#endif // SHARE_OOPS_SYMBOL_HPP
