/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_ADLC_FORMS_HPP
#define SHARE_ADLC_FORMS_HPP

// FORMS.HPP - ADL Parser Generic and Utility Forms Classes

#define TRUE 1
#define FALSE 0

// DEFINITIONS OF LEGAL ATTRIBUTE TYPES
#define INS_ATTR 0
#define OP_ATTR  1

// DEFINITIONS OF LEGAL CONSTRAINT TYPES

// Class List
class Form;
class InstructForm;
class MachNodeForm;
class OperandForm;
class OpClassForm;
class AttributeForm;
class RegisterForm;
class PipelineForm;
class SourceForm;
class EncodeForm;
class Component;
class Constraint;
class Predicate;
class MatchRule;
class Attribute;
class Effect;
class ExpandRule;
class RewriteRule;
class ConstructRule;
class FormatRule;
class Peephole;
class EncClass;
class Interface;
class RegInterface;
class ConstInterface;
class MemInterface;
class CondInterface;
class Opcode;
class InsEncode;
class RegDef;
class RegClass;
class CodeSnippetRegClass;
class ConditionalRegClass;
class AllocClass;
class ResourceForm;
class PipeClassForm;
class PeepMatch;
class PeepConstraint;
class PeepReplace;
class MatchList;

class ArchDesc;

//------------------------------FormDict---------------------------------------
// Dictionary containing Forms, and objects derived from forms
class FormDict {
private:
  Dict         _form;              // map names, char*, to their Form* or NULL

  // Disable public use of constructor, copy-ctor, operator =, operator ==
  FormDict( );
  FormDict &operator =( const FormDict & );
  // == compares two dictionaries; they must have the same keys (their keys
  // must match using CmpKey) and they must have the same values (pointer
  // comparison).  If so 1 is returned, if not 0 is returned.
  bool operator ==(const FormDict &d) const; // Compare dictionaries for equal

public:
  // cmp is a key comparision routine.  hash is a routine to hash a key.
  // FormDict( CmpKey cmp, Hash hash );
  FormDict( CmpKey cmp, Hash hash, Arena *arena );
  FormDict( const FormDict & fd );    // Deep-copy guts
  ~FormDict();

  // Return # of key-value pairs in dict
  int Size(void) const;

  // Insert inserts the given key-value pair into the dictionary.  The prior
  // value of the key is returned; NULL if the key was not previously defined.
  const Form  *Insert(const char *name, Form *form); // A new key-value

  // Find finds the value of a given key; or NULL if not found.
  // The dictionary is NOT changed.
  const Form  *operator [](const char *name) const;  // Do a lookup

  void dump();
};

// ***** Master Class for ADL Parser Forms *****
//------------------------------Form-------------------------------------------
class Form {
public:
  static Arena  *arena;            // arena used by forms
private:
  static Arena  *generate_arena(); // allocate arena used by forms

protected:
  int   _ftype;                    // Indicator for derived class type

public:
  // Public Data
  Form *_next;                     // Next pointer for form lists
  int   _linenum;                  // Line number for debugging

  // Dynamic type check for common forms.
  virtual OpClassForm   *is_opclass()     const;
  virtual OperandForm   *is_operand()     const;
  virtual InstructForm  *is_instruction() const;
  virtual MachNodeForm  *is_machnode()    const;
  virtual AttributeForm *is_attribute()   const;
  virtual Effect        *is_effect()      const;
  virtual ResourceForm  *is_resource()    const;
  virtual PipeClassForm *is_pipeclass()   const;

  // Check if this form is an operand usable for cisc-spilling
  virtual bool           is_cisc_reg(FormDict &globals) const { return false; }
  virtual bool           is_cisc_mem(FormDict &globals) const { return false; }

  // Public Methods
  Form(int formType=0, int line=0)
    : _next(NULL), _linenum(line), _ftype(formType) { };
  virtual ~Form() {};

  virtual bool ideal_only() const {
    assert(0,"Check of ideal status on non-instruction/operand form.\n");
    return FALSE;
  }

  // Check constraints after parsing
  virtual bool verify()    { return true; }

  virtual void dump()      { output(stderr); }    // Debug printer
  // Write info to output files
  virtual void output(FILE *fp)    { fprintf(fp,"Form Output"); }

public:
  // ADLC types, match the last character on ideal operands and instructions
  enum DataType {
    none        =  0,  // Not a simple type
    idealI      =  1,  // Integer type
    idealP      =  2,  // Pointer types, oop(s)
    idealL      =  3,  // Long    type
    idealF      =  4,  // Float   type
    idealD      =  5,  // Double  type
    idealB      =  6,  // Byte    type
    idealC      =  7,  // Char    type
    idealS      =  8,  // String  type
    idealN      =  9,  // Narrow oop types
    idealNKlass = 10,  // Narrow klass types
    idealV      = 11   // Vector  type
  };
  // Convert ideal name to a DataType, return DataType::none if not a 'ConX'
  Form::DataType  ideal_to_const_type(const char *ideal_type_name) const;
  // Convert ideal name to a DataType, return DataType::none if not a 'sRegX
  Form::DataType  ideal_to_sReg_type(const char *name) const;
  // Convert ideal name to a DataType, return DataType::none if not a 'RegX
  Form::DataType  ideal_to_Reg_type(const char *name) const;

  // Convert ideal name to a DataType, return DataType::none if not a 'LoadX
  Form::DataType is_load_from_memory(const char *opType) const;
  // Convert ideal name to a DataType, return DataType::none if not a 'StoreX
  Form::DataType is_store_to_memory(const char *opType)  const;

  // ADLC call types, matched with ideal world
  enum CallType {
    invalid_type  =  0,  // invalid call type
    JAVA_STATIC   =  1,  // monomorphic entry
    JAVA_DYNAMIC  =  2,  // possibly megamorphic, inline cache call
    JAVA_COMPILED =  3,  // callee will be compiled java
    JAVA_INTERP   =  4,  // callee will be executed by interpreter
    JAVA_NATIVE   =  5,  // native entrypoint
    JAVA_RUNTIME  =  6,  // runtime entrypoint
    JAVA_LEAF     =  7   // calling leaf
  };

  // Interface types for operands and operand classes
  enum InterfaceType {
    no_interface          =  0,  // unknown or inconsistent interface type
    constant_interface    =  1,  // interface to constants
    register_interface    =  2,  // interface to registers
    memory_interface      =  3,  // interface to memory
    conditional_interface =  4   // interface for condition codes
  };
  virtual Form::InterfaceType interface_type(FormDict &globals) const;

  enum CiscSpillInfo {
    Not_cisc_spillable   =  AdlcVMDeps::Not_cisc_spillable,
    Maybe_cisc_spillable =   0,
    Is_cisc_spillable    =   1
    // ...
  };

  // LEGAL FORM TYPES
  enum {
    INS,
    OPER,
    OPCLASS,
    SRC,
    ADEF,
    REG,
    PIPE,
    CNST,
    PRED,
    ATTR,
    MAT,
    ENC,
    FOR,
    EXP,
    REW,
    EFF,
    RDEF,
    RCL,
    ACL,
    RES,
    PCL,
    PDEF,
    REGL,
    RESL,
    STAL,
    COMP,
    PEEP,
    RESO
  };

};

//------------------------------FormList---------------------------------------
class FormList {
private:
  Form *_root;
  Form *_tail;
  Form *_cur;
  int   _justReset;                // Set immediately after reset
  Form *_cur2;                     // Nested iterator
  int   _justReset2;

public:
  void addForm(Form * entry) {
    if (_tail==NULL) { _root = _tail = _cur = entry;}
    else { _tail->_next = entry; _tail = entry;}
  };
  Form * current() { return _cur; };
  Form * iter()    { if (_justReset) _justReset = 0;
                     else if (_cur)  _cur = _cur->_next;
                     return _cur;};
  void   reset()   { if (_root) {_cur = _root; _justReset = 1;} };

  // Second iterator, state is internal
  Form * current2(){ return _cur2; };
  Form * iter2()   { if (_justReset2) _justReset2 = 0;
                    else if (_cur2)  _cur2 = _cur2->_next;
                    return _cur2;};
  void   reset2()  { if (_root) {_cur2 = _root; _justReset2 = 1;} };

  int  count() {
    int  count = 0; reset();
    for( Form *cur; (cur =  iter()) != NULL; ) { ++count; };
    return count;
  }

  void dump() {
    reset();
    Form *cur;
    for(; (cur =  iter()) != NULL; ) {
      cur->dump();
    };
  }

  bool verify() {
    bool verified = true;

    reset();
    Form *cur;
    for(; (cur =  iter()) != NULL; ) {
      if ( ! cur->verify() ) verified = false;
    };

    return verified;
  }

  void output(FILE* fp) {
    reset();
    Form *cur;
    for( ; (cur =  iter()) != NULL; ) {
      cur->output(fp);
    };
  }

  FormList() { _justReset = 1; _justReset2 = 1; _root = NULL; _tail = NULL; _cur = NULL; _cur2 = NULL;};
  ~FormList();
};

//------------------------------NameList---------------------------------------
// Extendable list of pointers, <char *>
class NameList {
  friend class PreserveIter;

private:
  int                _cur;         // Insert next entry here; count of entries
  int                _max;         // Number of spaces allocated
  const char       **_names;       // Array of names

protected:
  int                _iter;        // position during iteration
  bool               _justReset;   // Set immediately after reset


public:
  static const char *_signal;      // reserved user-defined string
  static const char *_signal2;      // reserved user-defined string
  static const char *_signal3;      // reserved user-defined string
  enum               { Not_in_list = -1 };

  void  addName(const char *name);
  void  add_signal();
  void  clear();                   // Remove all entries

  int   count() const;

  void  reset();                   // Reset iteration
  const char *iter();              // after reset(), first element : else next
  const char *current();           // return current element in iteration.
  const char *peek(int skip = 1);  // returns element + skip in iteration if there is one

  bool  current_is_signal();       // Return 'true' if current entry is signal
  bool  is_signal(const char *entry); // Return true if entry is a signal

  bool  search(const char *);      // Search for a name in the list
  int   index(const char *);       // Return index of name in list
  const char *name (intptr_t index);// Return name at index in list

  void  dump();                    // output to stderr
  void  output(FILE *fp);          // Output list of names to 'fp'

  NameList();
  ~NameList();
};


// Convenience class to preserve iteration state since iterators are
// internal instead of being external.
class PreserveIter {
 private:
  NameList* _list;
  int _iter;
  bool _justReset;

 public:
  PreserveIter(NameList* nl) {
    _list = nl;
    _iter = _list->_iter;
    _justReset = _list->_justReset;
  }
  ~PreserveIter() {
    _list->_iter = _iter;
    _list->_justReset = _justReset;
  }

};


//------------------------------NameAndList------------------------------------
// Storage for a name and an associated list of names
class NameAndList {
private:
  const char *_name;
  NameList    _list;

public:
  NameAndList(char *name);
  ~NameAndList();

  // Add to entries in list
  void        add_entry(const char *entry);

  // Access the name and its associated list.
  const char *name() const;
  void        reset();
  const char *iter();

  int count() { return _list.count(); }

  // Return the "index" entry in the list, zero-based
  const char *operator[](int index);


  void  dump();                    // output to stderr
  void  output(FILE *fp);          // Output list of names to 'fp'
};

//------------------------------ComponentList---------------------------------
// Component lists always have match rule operands first, followed by parameter
// operands which do not appear in the match list (in order of declaration).
class ComponentList : private NameList {
private:
  int   _matchcnt;                 // Count of match rule operands

public:

  // This is a batch program.  (And I have a destructor bug!)
  void operator delete( void *ptr ) {}

  void insert(Component *component, bool mflag);
  void insert(const char *name, const char *opType, int usedef, bool mflag);

  int  count();
  int  match_count() { return _matchcnt; } // Get count of match rule opers

  Component *iter();               // after reset(), first element : else next
  Component *match_iter();         // after reset(), first element : else next
  Component *post_match_iter();    // after reset(), first element : else next
  void       reset();              // Reset iteration
  Component *current();            // return current element in iteration.

  // Return element at "position", else NULL
  Component *operator[](int position);
  Component *at(int position) { return (*this)[position]; }

  // Return first component having this name.
  const Component *search(const char *name);

  // Return number of USEs + number of DEFs
  int        num_operands();
  // Return zero-based position in list;  -1 if not in list.
  int        operand_position(const char *name, int usedef, Form *fm);
  // Find position for this name, regardless of use/def information
  int        operand_position(const char *name);
  // Find position for this name when looked up for output via "format"
  int        operand_position_format(const char *name, Form *fm);
  // Find position for the Label when looked up for output via "format"
  int        label_position();
  // Find position for the Method when looked up for output via "format"
  int        method_position();

  void       dump();               // output to stderr
  void       output(FILE *fp);     // Output list of names to 'fp'

  ComponentList();
  ~ComponentList();
};

//------------------------------SourceForm-------------------------------------
class SourceForm : public Form {
private:

public:
  // Public Data
  char *_code;                     // Buffer for storing code text

  // Public Methods
  SourceForm(char* code);
  ~SourceForm();

  virtual const char* classname() { return "SourceForm"; }

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write output files
};

class HeaderForm : public SourceForm {
public:
  HeaderForm(char* code) : SourceForm(code) { }

  virtual const char* classname() { return "HeaderForm"; }
};

class PreHeaderForm : public SourceForm {
public:
  PreHeaderForm(char* code) : SourceForm(code) { }

  virtual const char* classname() { return "PreHeaderForm"; }
};




//------------------------------Expr------------------------------------------
#define STRING_BUFFER_LENGTH  2048
// class Expr represents integer expressions containing constants and addition
// Value must be in range zero through maximum positive integer. 32bits.
// Expected use: instruction and operand costs
class Expr {
public:
  enum {
    Zero     = 0,
    Max      = 0x7fffffff
  };
  const char *_external_name;  // if !NULL, then print this instead of _expr
  const char *_expr;
  int         _min_value;
  int         _max_value;

  Expr();
  Expr(const char *cost);
  Expr(const char *name, const char *expression, int min_value, int max_value);
  Expr *clone() const;

  bool  is_unknown() const { return (this == Expr::get_unknown()); }
  bool  is_zero()    const { return (_min_value == Expr::Zero && _max_value == Expr::Zero); }
  bool  less_than_or_equal(const Expr *c) const { return (_max_value <= c->_min_value); }

  void  add(const Expr *c);
  void  add(const char *c);
  void  add(const char *c, ArchDesc &AD);   // check if 'c' is defined in <arch>.ad
  void  set_external_name(const char *name) { _external_name = name; }

  const char *as_string()  const { return (_external_name != NULL ? _external_name : _expr); }
  void  print()            const;
  void  print_define(FILE *fp) const;
  void  print_assert(FILE *fp) const;

  static Expr *get_unknown();   // Returns pointer to shared unknown cost instance

  static char *buffer()         { return &external_buffer[0]; }
  static bool  init_buffers();  // Fill buffers with 0
  static bool  check_buffers(); // if buffer use may have overflowed, assert

private:
  static Expr *_unknown_expr;
  static char string_buffer[STRING_BUFFER_LENGTH];
  static char external_buffer[STRING_BUFFER_LENGTH];
  static bool _init_buffers;
  const char *compute_expr(const Expr *c1, const Expr *c2);  // cost as string after adding 'c1' and 'c2'
  int         compute_min (const Expr *c1, const Expr *c2);  // minimum after adding 'c1' and 'c2'
  int         compute_max (const Expr *c1, const Expr *c2);  // maximum after adding 'c1' and 'c2'
  const char *compute_external(const Expr *c1, const Expr *c2);  // external name after adding 'c1' and 'c2'
};

//------------------------------ExprDict---------------------------------------
// Dictionary containing Exprs
class ExprDict {
private:
  Dict         _expr;              // map names, char*, to their Expr* or NULL
  NameList     _defines;           // record the order of definitions entered with define call

  // Disable public use of constructor, copy-ctor, operator =, operator ==
  ExprDict( );
  ExprDict( const ExprDict & );    // Deep-copy guts
  ExprDict &operator =( const ExprDict & );
  // == compares two dictionaries; they must have the same keys (their keys
  // must match using CmpKey) and they must have the same values (pointer
  // comparison).  If so 1 is returned, if not 0 is returned.
  bool operator ==(const ExprDict &d) const; // Compare dictionaries for equal

public:
  // cmp is a key comparision routine.  hash is a routine to hash a key.
  ExprDict( CmpKey cmp, Hash hash, Arena *arena );
  ~ExprDict();

  // Return # of key-value pairs in dict
  int Size(void) const;

  // define inserts the given key-value pair into the dictionary,
  // and records the name in order for later output, ...
  const Expr  *define(const char *name, Expr *expr);

  // Insert inserts the given key-value pair into the dictionary.  The prior
  // value of the key is returned; NULL if the key was not previously defined.
  const Expr  *Insert(const char *name, Expr *expr); // A new key-value

  // Find finds the value of a given key; or NULL if not found.
  // The dictionary is NOT changed.
  const Expr  *operator [](const char *name) const;  // Do a lookup

  void print_defines(FILE *fp);
  void print_asserts(FILE *fp);
  void dump();
};

#endif // SHARE_ADLC_FORMS_HPP
