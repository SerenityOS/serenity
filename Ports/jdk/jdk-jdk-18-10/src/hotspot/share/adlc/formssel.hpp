/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_ADLC_FORMSSEL_HPP
#define SHARE_ADLC_FORMSSEL_HPP

// FORMSSEL.HPP - ADL Parser Instruction Selection Forms Classes

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
class PipeDesc;
class PipeClass;
class PeepMatch;
class PeepConstraint;
class PeepReplace;
class MatchList;

class ArchDesc;

//==============================Instructions===================================
//------------------------------InstructForm-----------------------------------
class InstructForm : public Form {
private:
  bool           _ideal_only;       // Not a user-defined instruction
  // Members used for tracking CISC-spilling
  int            _cisc_spill_operand;// Which operand may cisc-spill
  void           set_cisc_spill_operand(uint op_index) { _cisc_spill_operand = op_index; }
  bool           _is_cisc_alternate;
  InstructForm  *_cisc_spill_alternate;// cisc possible replacement
  const char    *_cisc_reg_mask_name;
  InstructForm  *_short_branch_form;
  bool           _is_short_branch;
  bool           _is_mach_constant;    // True if Node is a MachConstantNode.
  bool           _needs_constant_base; // True if Node needs the mach_constant_base input.
  uint           _alignment;

public:
  // Public Data
  const char    *_ident;               // Name of this instruction
  NameList       _parameters;          // Locally defined names
  FormDict       _localNames;          // Table of operands & their types
  MatchRule     *_matrule;             // Matching rule for this instruction
  Opcode        *_opcode;              // Encoding of the opcode for instruction
  char          *_size;                // Size of instruction
  InsEncode     *_insencode;           // Encoding class instruction belongs to
  InsEncode     *_constant;            // Encoding class constant value belongs to
  bool           _is_postalloc_expand; // Indicates that encoding just does a lateExpand.
  Attribute     *_attribs;             // List of Attribute rules
  Predicate     *_predicate;           // Predicate test for this instruction
  FormDict       _effects;             // Dictionary of effect rules
  ExpandRule    *_exprule;             // Expand rule for this instruction
  RewriteRule   *_rewrule;             // Rewrite rule for this instruction
  FormatRule    *_format;              // Format for assembly generation
  Peephole      *_peephole;            // List of peephole rules for instruction
  const char    *_ins_pipe;            // Instruction Scheduling description class

  uint          *_uniq_idx;            // Indexes of unique operands
  uint           _uniq_idx_length;     // Length of _uniq_idx array
  uint           _num_uniq;            // Number  of unique operands
  ComponentList  _components;          // List of Components matches MachNode's
                                       // operand structure

  bool           _has_call;            // contain a call and caller save registers should be saved?

  // Public Methods
  InstructForm(const char *id, bool ideal_only = false);
  InstructForm(const char *id, InstructForm *instr, MatchRule *rule);
  ~InstructForm();

  // Dynamic type check
  virtual InstructForm *is_instruction() const;

  virtual bool        ideal_only() const;

  // This instruction sets a result
  virtual bool        sets_result() const;
  // This instruction needs projections for additional DEFs or KILLs
  virtual bool        needs_projections();
  // This instruction needs extra nodes for temporary inputs
  virtual bool        has_temps();
  // This instruction defines or kills more than one object
  virtual uint        num_defs_or_kills();
  // This instruction has an expand rule?
  virtual bool        expands() const ;
  // This instruction has a late expand rule?
  virtual bool        postalloc_expands() const;
  // Return this instruction's first peephole rule, or NULL
  virtual Peephole   *peepholes() const;
  // Add a peephole rule to this instruction
  virtual void        append_peephole(Peephole *peep);

  virtual bool        is_pinned(FormDict &globals); // should be pinned inside block
  virtual bool        is_projection(FormDict &globals); // node requires projection
  virtual bool        is_parm(FormDict &globals); // node matches ideal 'Parm'
  // ideal opcode enumeration
  virtual const char *ideal_Opcode(FormDict &globals)  const;
  virtual int         is_expensive() const;     // node matches ideal 'CosD'
  virtual int         is_empty_encoding() const; // _size=0 and/or _insencode empty
  virtual int         is_tls_instruction() const; // tlsLoadP rule or ideal ThreadLocal
  virtual int         is_ideal_copy() const;    // node matches ideal 'Copy*'
  virtual bool        is_ideal_negD() const;    // node matches ideal 'NegD'
  virtual bool        is_ideal_if()   const;    // node matches ideal 'If'
  virtual bool        is_ideal_fastlock() const; // node matches 'FastLock'
  virtual bool        is_ideal_membar() const;  // node matches ideal 'MemBarXXX'
  virtual bool        is_ideal_loadPC() const;  // node matches ideal 'LoadPC'
  virtual bool        is_ideal_box() const;     // node matches ideal 'Box'
  virtual bool        is_ideal_goto() const;    // node matches ideal 'Goto'
  virtual bool        is_ideal_branch() const;  // "" 'If' | 'Goto' | 'LoopEnd' | 'Jump'
  virtual bool        is_ideal_jump() const;    // node matches ideal 'Jump'
  virtual bool        is_ideal_return() const;  // node matches ideal 'Return'
  virtual bool        is_ideal_halt() const;    // node matches ideal 'Halt'
  virtual bool        is_ideal_safepoint() const; // node matches 'SafePoint'
  virtual bool        is_ideal_nop() const;     // node matches 'Nop'
  virtual bool        is_ideal_control() const; // control node
  virtual bool        is_vector() const;        // vector instruction

  virtual Form::CallType is_ideal_call() const; // matches ideal 'Call'
  virtual Form::DataType is_ideal_load() const; // node matches ideal 'LoadXNode'
  // Should antidep checks be disabled for this Instruct
  // See definition of MatchRule::skip_antidep_check
  bool skip_antidep_check() const;
  virtual Form::DataType is_ideal_store() const;// node matches ideal 'StoreXNode'
          bool        is_ideal_mem() const { return is_ideal_load() != Form::none || is_ideal_store() != Form::none; }
  virtual uint        two_address(FormDict &globals); // output reg must match input reg
  // when chaining a constant to an instruction, return 'true' and set opType
  virtual Form::DataType is_chain_of_constant(FormDict &globals);
  virtual Form::DataType is_chain_of_constant(FormDict &globals, const char * &opType);
  virtual Form::DataType is_chain_of_constant(FormDict &globals, const char * &opType, const char * &result_type);

  // Check if a simple chain rule
  virtual bool        is_simple_chain_rule(FormDict &globals) const;

  // check for structural rematerialization
  virtual bool        rematerialize(FormDict &globals, RegisterForm *registers);

  // loads from memory, so must check for anti-dependence
  virtual bool        needs_anti_dependence_check(FormDict &globals) const;
  virtual int         memory_operand(FormDict &globals) const;

  enum memory_operand_type {
    NO_MEMORY_OPERAND = -1,
    MANY_MEMORY_OPERANDS = 999999
  };


  // This instruction captures the machine-independent bottom_type
  // Expected use is for pointer vs oop determination for LoadP
  virtual bool        captures_bottom_type(FormDict& globals) const;

  virtual const char *cost();      // Access ins_cost attribute
  virtual uint        num_opnds(); // Count of num_opnds for MachNode class
                                   // Counts USE_DEF opnds twice.  See also num_unique_opnds().
  virtual uint        num_post_match_opnds();
  virtual uint        num_consts(FormDict &globals) const;// Constants in match rule
  // Constants in match rule with specified type
  virtual uint        num_consts(FormDict &globals, Form::DataType type) const;

  // Return the register class associated with 'leaf'.
  virtual const char *out_reg_class(FormDict &globals);

  // number of ideal node inputs to skip
  virtual uint        oper_input_base(FormDict &globals);

  // Does this instruction need a base-oop edge?
  int needs_base_oop_edge(FormDict &globals) const;

  // Build instruction predicates.  If the user uses the same operand name
  // twice, we need to check that the operands are pointer-eequivalent in
  // the DFA during the labeling process.
  Predicate *build_predicate();

  virtual void        build_components(); // top-level operands
  // Return zero-based position in component list; -1 if not in list.
  virtual int         operand_position(const char *name, int usedef);
  virtual int         operand_position_format(const char *name);

  // Return zero-based position in component list; -1 if not in list.
  virtual int         label_position();
  virtual int         method_position();
  // Return number of relocation entries needed for this instruction.
  virtual uint        reloc(FormDict &globals);

  const char         *opnd_ident(int idx);  // Name of operand #idx.
  const char         *reduce_result();
  // Return the name of the operand on the right hand side of the binary match
  // Return NULL if there is no right hand side
  const char         *reduce_right(FormDict &globals)  const;
  const char         *reduce_left(FormDict &globals)   const;

  // Base class for this instruction, MachNode except for calls
  virtual const char *mach_base_class(FormDict &globals)  const;

  // Check if this instruction can cisc-spill to 'alternate'
  bool                cisc_spills_to(ArchDesc &AD, InstructForm *alternate);
  InstructForm       *cisc_spill_alternate() { return _cisc_spill_alternate; }
  int                 cisc_spill_operand() const { return _cisc_spill_operand; }
  bool                is_cisc_alternate() const { return _is_cisc_alternate; }
  void                set_cisc_alternate(bool val) { _is_cisc_alternate = val; }
  const char         *cisc_reg_mask_name() const { return _cisc_reg_mask_name; }
  void                set_cisc_reg_mask_name(const char *rm_name) { _cisc_reg_mask_name = rm_name; }
  // Output cisc-method prototypes and method bodies
  void                declare_cisc_version(ArchDesc &AD, FILE *fp_cpp);
  bool                define_cisc_version (ArchDesc &AD, FILE *fp_cpp);

  bool                check_branch_variant(ArchDesc &AD, InstructForm *short_branch);

  bool                is_short_branch() { return _is_short_branch; }
  void                set_short_branch(bool val) { _is_short_branch = val; }

  bool                    is_mach_constant() const { return _is_mach_constant;     }
  void                set_is_mach_constant(bool x) {        _is_mach_constant = x; }
  bool                    needs_constant_base() const { return _needs_constant_base;     }
  void                set_needs_constant_base(bool x) {        _needs_constant_base = x; }

  InstructForm       *short_branch_form() { return _short_branch_form; }
  bool                has_short_branch_form() { return _short_branch_form != NULL; }
  // Output short branch prototypes and method bodies
  void                declare_short_branch_methods(FILE *fp_cpp);
  bool                define_short_branch_methods(ArchDesc &AD, FILE *fp_cpp);

  uint                alignment() { return _alignment; }
  void                set_alignment(uint val) { _alignment = val; }

  // Seach through operands to determine operands unique positions.
  void                set_unique_opnds();
  uint                num_unique_opnds() { return _num_uniq; }
  uint                unique_opnds_idx(int idx) {
    if (_uniq_idx != NULL && idx > 0) {
      assert((uint)idx < _uniq_idx_length, "out of bounds");
      return _uniq_idx[idx];
    } else {
      return idx;
    }
  }
  const char         *unique_opnd_ident(uint idx);  // Name of operand at unique idx.

  // Operands which are only KILLs aren't part of the input array and
  // require special handling in some cases.  Their position in this
  // operand list is higher than the number of unique operands.
  bool is_noninput_operand(uint idx) {
    return (idx >= num_unique_opnds());
  }

  // --------------------------- FILE *output_routines
  //
  // Generate the format call for the replacement variable
  void                rep_var_format(FILE *fp, const char *rep_var);
  // Generate index values needed for determining the operand position
  void                index_temps   (FILE *fp, FormDict &globals, const char *prefix = "", const char *receiver = "");
  // ---------------------------

  virtual bool verify();           // Check consistency after parsing

  virtual void dump();             // Debug printer
  virtual void output(FILE *fp);   // Write to output files
};

//------------------------------EncodeForm-------------------------------------
class EncodeForm : public Form {
private:

public:
  // Public Data
  NameList  _eclasses;            // List of encode class names
  Dict      _encClass;            // Map encode class names to EncClass objects

  // Public Methods
  EncodeForm();
  ~EncodeForm();

  EncClass   *add_EncClass(const char *className);
  EncClass   *encClass(const char *className);

  const char *encClassPrototype(const char *className);
  const char *encClassBody(const char *className);

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};

//------------------------------EncClass---------------------------------------
class EncClass : public Form {
public:
  // NameList for parameter type and name
  NameList       _parameter_type;
  NameList       _parameter_name;

  // Breakdown the encoding into strings separated by $replacement_variables
  // There is an entry in _strings, perhaps NULL, that precedes each _rep_vars
  NameList       _code;            // Strings passed through to tty->print
  NameList       _rep_vars;        // replacement variables

  NameList       _parameters;      // Locally defined names
  FormDict       _localNames;      // Table of components & their types

public:
  // Public Data
  const char    *_name;            // encoding class name

  // Public Methods
  EncClass(const char *name);
  ~EncClass();

  // --------------------------- Parameters
  // Add a parameter <type,name> pair
  void add_parameter(const char *parameter_type, const char *parameter_name);
  // Verify operand types in parameter list
  bool check_parameter_types(FormDict &globals);
  // Obtain the zero-based index corresponding to a replacement variable
  int         rep_var_index(const char *rep_var);
  int         num_args() { return _parameter_name.count(); }

  // --------------------------- Code Block
  // Add code
  void add_code(const char *string_preceding_replacement_var);
  // Add a replacement variable or one of its subfields
  // Subfields are stored with a leading '$'
  void add_rep_var(char *replacement_var);

  bool verify();
  void dump();
  void output(FILE *fp);
};

//------------------------------MachNode---------------------------------------
class MachNodeForm: public Form {
private:

public:
  char          *_ident;           // Name of this instruction
  const char    *_machnode_pipe;   // Instruction Scheduline description class

  // Public Methods
  MachNodeForm(char *id);
  ~MachNodeForm();

  virtual MachNodeForm *is_machnode() const;

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};

//------------------------------Opcode-----------------------------------------
class Opcode : public Form {
private:

public:
  // Public Data
  // Strings representing instruction opcodes, user defines placement in emit
  char *_primary;
  char *_secondary;
  char *_tertiary;

  enum opcode_type {
    NOT_AN_OPCODE = -1,
    PRIMARY   = 1,
    SECONDARY = 2,
    TERTIARY  = 3
  };

  // Public Methods
  Opcode(char *primary, char *secondary, char *tertiary);
  ~Opcode();

  static Opcode::opcode_type as_opcode_type(const char *designator);

  void dump();
  void output(FILE *fp);

  // --------------------------- FILE *output_routines
  bool print_opcode(FILE *fp, Opcode::opcode_type desired_opcode);
};

//------------------------------InsEncode--------------------------------------
class InsEncode : public Form {
private:
  // Public Data (access directly only for reads)
  // The encodings can only have the values predefined by the ADLC:
  // blank, RegReg, RegMem, MemReg, ...
  NameList    _encoding;
  // NameList    _parameter;
  // The parameters for each encoding are preceeded by a NameList::_signal
  // and follow the parameters for the previous encoding.

  // char *_encode;                  // Type of instruction encoding

public:
  // Public Methods
  InsEncode();
  ~InsEncode();

  // Add "encode class name" and its parameters
  NameAndList  *add_encode(char *encode_method_name);
  // Parameters are added to the returned "NameAndList" by the parser

  // Access the list of encodings
  void          reset();
  const char   *encode_class_iter();

  // Returns the number of arguments to the current encoding in the iteration
  int current_encoding_num_args() {
    return ((NameAndList*)_encoding.current())->count();
  }

  // --------------------------- Parameters
  // The following call depends upon position within encode_class_iteration
  //
  // Obtain parameter name from zero based index
  const char   *rep_var_name(InstructForm &inst, uint param_no);
  // ---------------------------

  void dump();
  void output(FILE *fp);
};

//------------------------------Effect-----------------------------------------
class Effect : public Form {
private:

public:
  // Public Data
  const char  *_name;            // Pre-defined name for effect
  int         _use_def;          // Enumeration value of effect

  // Public Methods
  Effect(const char *name);      // Constructor
  ~Effect();                     // Destructor

  // Dynamic type check
  virtual Effect *is_effect() const;

  // Return 'true' if this use def info equals the parameter
  bool  is(int use_def_kill_enum) const;
  // Return 'true' if this use def info is a superset of parameter
  bool  isa(int use_def_kill_enum) const;

  void dump();                   // Debug printer
  void output(FILE *fp);         // Write info to output files
};

//------------------------------ExpandRule-------------------------------------
class ExpandRule : public Form {
private:
  NameList _expand_instrs;        // ordered list of instructions and operands

public:
  // Public Data
  NameList _newopers;             // List of newly created operands
  Dict     _newopconst;           // Map new operands to their constructors

  void     add_instruction(NameAndList *instruction_name_and_operand_list);
  void     reset_instructions();
  NameAndList *iter_instructions();

  // Public Methods
  ExpandRule();                   // Constructor
  ~ExpandRule();                  // Destructor

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};

//------------------------------RewriteRule------------------------------------
class RewriteRule : public Form {
private:

public:
  // Public Data
  SourceForm     *_condition;      // Rewrite condition code
  InstructForm   *_instrs;         // List of instructions to expand to
  OperandForm    *_opers;          // List of operands generated by expand
  char           *_tempParams;     // Hold string until parser is finished.
  char           *_tempBlock;      // Hold string until parser is finished.

  // Public Methods
  RewriteRule(char* params, char* block) ;
  ~RewriteRule();                  // Destructor
  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};


//==============================Operands=======================================
//------------------------------OpClassForm------------------------------------
class OpClassForm : public Form {
public:
  // Public Data
  const char    *_ident;           // Name of this operand
  NameList       _oplst;           // List of operand forms included in class

  // Public Methods
  OpClassForm(const char *id);
  ~OpClassForm();

  // dynamic type check
  virtual OpClassForm         *is_opclass() const;
  virtual Form::InterfaceType  interface_type(FormDict &globals) const;
  virtual bool                 stack_slots_only(FormDict &globals) const;

  virtual bool                 is_cisc_mem(FormDict &globals) const;


  // Min and Max opcodes of operands in this operand class
  int _minCode;
  int _maxCode;

  virtual bool ideal_only() const;
  virtual void dump();             // Debug printer
  virtual void output(FILE *fp);   // Write to output files
};

//------------------------------OperandForm------------------------------------
class OperandForm : public OpClassForm {
private:
  bool         _ideal_only; // Not a user-defined instruction

public:
  // Public Data
  NameList       _parameters; // Locally defined names
  FormDict       _localNames; // Table of components & their types
  MatchRule     *_matrule;    // Matching rule for this operand
  Interface     *_interface;  // Encoding interface for this operand
  Attribute     *_attribs;    // List of Attribute rules
  Predicate     *_predicate;  // Predicate test for this operand
  Constraint    *_constraint; // Constraint Rule for this operand
  ConstructRule *_construct;  // Construction of operand data after matching
  FormatRule    *_format;     // Format for assembly generation
  NameList       _classes;    // List of opclasses which contain this oper

  ComponentList _components;  //

  // Public Methods
  OperandForm(const char *id);
  OperandForm(const char *id, bool ideal_only);
  ~OperandForm();

  // Dynamic type check
  virtual OperandForm *is_operand() const;

  virtual bool        ideal_only() const;
  virtual Form::InterfaceType interface_type(FormDict &globals) const;
  virtual bool                 stack_slots_only(FormDict &globals) const;

  virtual const char *cost();  // Access ins_cost attribute
  virtual uint        num_leaves() const;// Leaves in complex operand
  // Constants in operands' match rules
  virtual uint        num_consts(FormDict &globals) const;
  // Constants in operand's match rule with specified type
  virtual uint        num_consts(FormDict &globals, Form::DataType type) const;
  // Pointer Constants in operands' match rules
  virtual uint        num_const_ptrs(FormDict &globals) const;
  // The number of input edges in the machine world == num_leaves - num_consts
  virtual uint        num_edges(FormDict &globals) const;

  // Check if this operand is usable for cisc-spilling
  virtual bool        is_cisc_reg(FormDict &globals) const;

  // node matches ideal 'Bool', grab condition codes from the ideal world
  virtual bool        is_ideal_bool()  const;

  // Has an integer constant suitable for spill offsets
  bool has_conI(FormDict &globals) const {
    return (num_consts(globals,idealI) == 1) && !is_ideal_bool(); }
  bool has_conL(FormDict &globals) const {
    return (num_consts(globals,idealL) == 1) && !is_ideal_bool(); }

  // Node is user-defined operand for an sRegX
  virtual Form::DataType is_user_name_for_sReg() const;

  // Return ideal type, if there is a single ideal type for this operand
  virtual const char *ideal_type(FormDict &globals, RegisterForm *registers = NULL) const;
  // If there is a single ideal type for this interface field, return it.
  virtual const char *interface_ideal_type(FormDict   &globals,
                                           const char *field_name) const;

  // Return true if this operand represents a bound register class
  bool is_bound_register() const;

  // Return the Register class for this operand.  Returns NULL if
  // operand isn't a register form.
  RegClass* get_RegClass() const;

  virtual       bool  is_interface_field(const char   *field_name,
                                         const char   * &value) const;

  // If this operand has a single ideal type, return its type
  virtual Form::DataType simple_type(FormDict &globals) const;
  // If this operand is an ideal constant, return its type
  virtual Form::DataType is_base_constant(FormDict &globals) const;

  // "true" if this operand is a simple type that is swallowed
  virtual bool        swallowed(FormDict &globals) const;

  // Return register class name if a constraint specifies the register class.
  virtual const char *constrained_reg_class() const;
  // Return the register class associated with 'leaf'.
  virtual const char *in_reg_class(uint leaf, FormDict &globals);

  // Build component list from MatchRule and operand's parameter list
  virtual void        build_components(); // top-level operands

  // Return zero-based position in component list; -1 if not in list.
  virtual int         operand_position(const char *name, int usedef);

  // Return zero-based position in component list; -1 if not in list.
  virtual int         constant_position(FormDict &globals, const Component *comp);
  virtual int         constant_position(FormDict &globals, const char *local_name);
  // Return the operand form corresponding to the given index, else NULL.
  virtual OperandForm *constant_operand(FormDict &globals, uint const_index);

  // Return zero-based position in component list; -1 if not in list.
  virtual int         register_position(FormDict &globals, const char *regname);

  const char         *reduce_result() const;
  // Return the name of the operand on the right hand side of the binary match
  // Return NULL if there is no right hand side
  const char         *reduce_right(FormDict &globals)  const;
  const char         *reduce_left(FormDict &globals)   const;


  // --------------------------- FILE *output_routines
  //
  // Output code for disp_is_oop, if true.
  void                disp_is_oop(FILE *fp, FormDict &globals);
  // Generate code for internal and external format methods
  void                int_format(FILE *fp, FormDict &globals, uint index);
  void                ext_format(FILE *fp, FormDict &globals, uint index);
  void                format_constant(FILE *fp, uint con_index, uint con_type);
  // Output code to access the value of the index'th constant
  void                access_constant(FILE *fp, FormDict &globals,
                                      uint con_index);
  // ---------------------------


  virtual void dump();             // Debug printer
  virtual void output(FILE *fp);   // Write to output files
};

//------------------------------Constraint-------------------------------------
class Constraint : public Form {
private:

public:
  const char      *_func;          // Constraint function
  const char      *_arg;           // function's argument

  // Public Methods
  Constraint(const char *func, const char *arg); // Constructor
  ~Constraint();

  bool stack_slots_only() const;

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};

//------------------------------Predicate--------------------------------------
class Predicate : public Form {
private:

public:
  // Public Data
  char *_pred;                     // C++ source string for predicate

  // Public Methods
  Predicate(char *pr);
  ~Predicate();

  void dump();
  void output(FILE *fp);
};

//------------------------------Interface--------------------------------------
class Interface : public Form {
private:

public:
  // Public Data
  const char *_name;               // String representing the interface name

  // Public Methods
  Interface(const char *name);
  ~Interface();

  virtual Form::InterfaceType interface_type(FormDict &globals) const;

  RegInterface   *is_RegInterface();
  MemInterface   *is_MemInterface();
  ConstInterface *is_ConstInterface();
  CondInterface  *is_CondInterface();


  void dump();
  void output(FILE *fp);
};

//------------------------------RegInterface-----------------------------------
class RegInterface : public Interface {
private:

public:
  // Public Methods
  RegInterface();
  ~RegInterface();

  void dump();
  void output(FILE *fp);
};

//------------------------------ConstInterface---------------------------------
class ConstInterface : public Interface {
private:

public:
  // Public Methods
  ConstInterface();
  ~ConstInterface();

  void dump();
  void output(FILE *fp);
};

//------------------------------MemInterface-----------------------------------
class MemInterface : public Interface {
private:

public:
  // Public Data
  char *_base;                     // Base address
  char *_index;                    // index
  char *_scale;                    // scaling factor
  char *_disp;                     // displacement

  // Public Methods
  MemInterface(char *base, char *index, char *scale, char *disp);
  ~MemInterface();

  void dump();
  void output(FILE *fp);
};

//------------------------------CondInterface----------------------------------
class CondInterface : public Interface {
private:

public:
  const char *_equal;
  const char *_not_equal;
  const char *_less;
  const char *_greater_equal;
  const char *_less_equal;
  const char *_greater;
  const char *_overflow;
  const char *_no_overflow;
  const char *_equal_format;
  const char *_not_equal_format;
  const char *_less_format;
  const char *_greater_equal_format;
  const char *_less_equal_format;
  const char *_greater_format;
  const char *_overflow_format;
  const char *_no_overflow_format;

  // Public Methods
  CondInterface(const char* equal,         const char* equal_format,
                const char* not_equal,     const char* not_equal_format,
                const char* less,          const char* less_format,
                const char* greater_equal, const char* greater_equal_format,
                const char* less_equal,    const char* less_equal_format,
                const char* greater,       const char* greater_format,
                const char* overflow,      const char* overflow_format,
                const char* no_overflow,   const char* no_overflow_format);
  ~CondInterface();

  void dump();
  void output(FILE *fp);
};

//------------------------------ConstructRule----------------------------------
class ConstructRule : public Form {
private:

public:
  // Public Data
  char *_expr;                     // String representing the match expression
  char *_construct;                // String representing C++ constructor code

  // Public Methods
  ConstructRule(char *cnstr);
  ~ConstructRule();

  void dump();
  void output(FILE *fp);
};


//==============================Shared=========================================
//------------------------------AttributeForm----------------------------------
class AttributeForm : public Form {
private:
  // counters for unique instruction or operand ID
  static int   _insId;             // user-defined machine instruction types
  static int   _opId;              // user-defined operand types

  int  id;                         // hold type for this object

public:
  // Public Data
  char *_attrname;                 // Name of attribute
  int   _atype;                    // Either INS_ATTR or OP_ATTR
  char *_attrdef;                  // C++ source which evaluates to constant

  // Public Methods
  AttributeForm(char *attr, int type, char *attrdef);
  ~AttributeForm();

  // Dynamic type check
  virtual AttributeForm *is_attribute() const;

  int  type() { return id;}        // return this object's "id"

  static const char* _ins_cost;        // "ins_cost"
  static const char* _op_cost;         // "op_cost"

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write output files
};

//------------------------------Component--------------------------------------
class Component : public Form {
private:

public:
  // Public Data
  const char *_name;              // Name of this component
  const char *_type;              // Type of this component
  int         _usedef;            // Value of component

  // Public Methods
  Component(const char *name, const char *type, int usedef);
  ~Component();


  // Return 'true' if this use def info equals the parameter
  bool  is(int use_def_kill_enum) const;
  // Return 'true' if this use def info is a superset of parameter
  bool  isa(int use_def_kill_enum) const;
  int   promote_use_def_info(int new_use_def);
  const char *base_type(FormDict &globals);
  // Form::DataType is_base_constant(FormDict &globals);

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write to output files
  const char* getUsedefName();

public:
  // Implementation depends upon working bit intersection and union.
  enum use_def_enum {
    INVALID   = 0x0,
    USE       = 0x1,
    DEF       = 0x2,
    USE_DEF   = USE | DEF,
    KILL      = 0x4,
    USE_KILL  = USE | KILL,
    SYNTHETIC = 0x8,
    TEMP      = USE | SYNTHETIC,
    TEMP_DEF  = TEMP | DEF,
    CALL      = 0x10
  };
};


//------------------------------MatchNode--------------------------------------
class MatchNode : public Form {
private:

public:
  // Public Data
  const char  *_result;            // The name of the output of this node
  const char  *_name;              // The name that appeared in the match rule
  const char  *_opType;            // The Operation/Type matched
  MatchNode   *_lChild;            // Left child in expression tree
  MatchNode   *_rChild;            // Right child in expression tree
  int         _numleaves;          // Sum of numleaves for all direct children
  ArchDesc    &_AD;                // Reference to ArchDesc object
  char        *_internalop;        // String representing internal operand
  int         _commutative_id;     // id of commutative operation

  // Public Methods
  MatchNode(ArchDesc &ad, const char *result = 0, const char *expr = 0,
            const char *opType=0, MatchNode *lChild=NULL,
            MatchNode *rChild=NULL);
  MatchNode(ArchDesc &ad, MatchNode& mNode); // Shallow copy constructor;
  MatchNode(ArchDesc &ad, MatchNode& mNode, int clone); // Construct clone
  ~MatchNode();

  // return 0 if not found:
  // return 1 if found and position is incremented by operand offset in rule
  bool       find_name(const char *str, int &position) const;
  bool       find_type(const char *str, int &position) const;
  virtual void append_components(FormDict& locals, ComponentList& components,
                                 bool def_flag = false) const;
  bool       base_operand(uint &position, FormDict &globals,
                         const char * &result, const char * &name,
                         const char * &opType) const;
  // recursive count on operands
  uint       num_consts(FormDict &globals) const;
  uint       num_const_ptrs(FormDict &globals) const;
  // recursive count of constants with specified type
  uint       num_consts(FormDict &globals, Form::DataType type) const;
  // uint       num_consts() const;   // Local inspection only
  int        needs_ideal_memory_edge(FormDict &globals) const;
  int        needs_base_oop_edge() const;

  // Help build instruction predicates.  Search for operand names.
  void count_instr_names( Dict &names );
  int build_instr_pred( char *buf, const char *name, int cnt, int path_bitmask, int level);
  void build_internalop( );

  // Return the name of the operands associated with reducing to this operand:
  // The result type, plus the left and right sides of the binary match
  // Return NULL if there is no left or right hand side
  bool       sets_result()   const;    // rule "Set"s result of match
  const char *reduce_right(FormDict &globals)  const;
  const char *reduce_left (FormDict &globals)  const;

  // Recursive version of check in MatchRule
  int        cisc_spill_match(FormDict& globals, RegisterForm* registers,
                              MatchNode* mRule2, const char* &operand,
                              const char* &reg_type);
  int        cisc_spill_merge(int left_result, int right_result);

  virtual bool equivalent(FormDict& globals, MatchNode* mNode2);

  void       count_commutative_op(int& count);
  void       swap_commutative_op(bool atroot, int count);

  void dump();
  void output(FILE *fp);
};

//------------------------------MatchRule--------------------------------------
class MatchRule : public MatchNode {
private:

public:
  // Public Data
  const char *_machType;            // Machine type index
  int         _depth;               // Expression tree depth
  char       *_construct;           // String representing C++ constructor code
  int         _numchilds;           // Number of direct children
  MatchRule  *_next;                // Pointer to next match rule

  // Public Methods
  MatchRule(ArchDesc &ad);
  MatchRule(ArchDesc &ad, MatchRule* mRule); // Shallow copy constructor;
  MatchRule(ArchDesc &ad, MatchNode* mroot, int depth, char* construct, int numleaves);
  ~MatchRule();

  virtual void append_components(FormDict& locals, ComponentList& components, bool def_flag = false) const;
  // Recursive call on all operands' match rules in my match rule.
  bool       base_operand(uint &position, FormDict &globals,
                         const char * &result, const char * &name,
                         const char * &opType) const;


  bool       is_base_register(FormDict &globals) const;
  Form::DataType is_base_constant(FormDict &globals) const;
  bool       is_chain_rule(FormDict &globals) const;
  int        is_ideal_copy() const;
  int        is_expensive() const;     // node matches ideal 'CosD'
  bool       is_ideal_if()   const;    // node matches ideal 'If'
  bool       is_ideal_fastlock() const; // node matches ideal 'FastLock'
  bool       is_ideal_jump()   const;  // node matches ideal 'Jump'
  bool       is_ideal_membar() const;  // node matches ideal 'MemBarXXX'
  bool       is_ideal_loadPC() const;  // node matches ideal 'LoadPC'
  bool       is_ideal_box() const;     // node matches ideal 'Box'
  bool       is_ideal_goto() const;    // node matches ideal 'Goto'
  bool       is_ideal_loopEnd() const; // node matches ideal 'LoopEnd'
  bool       is_ideal_bool() const;    // node matches ideal 'Bool'
  bool       is_vector() const;        // vector instruction
  Form::DataType is_ideal_load() const;// node matches ideal 'LoadXNode'
  // Should antidep checks be disabled for this rule
  // See definition of MatchRule::skip_antidep_check
  bool skip_antidep_check() const;
  Form::DataType is_ideal_store() const;// node matches ideal 'StoreXNode'

  // Check if 'mRule2' is a cisc-spill variant of this MatchRule
  int        matchrule_cisc_spill_match(FormDict &globals, RegisterForm* registers,
                                        MatchRule* mRule2, const char* &operand,
                                        const char* &reg_type);

  // Check if 'mRule2' is equivalent to this MatchRule
  virtual bool equivalent(FormDict& globals, MatchNode* mRule2);

  void       matchrule_swap_commutative_op(const char* instr_ident, int count, int& match_rules_cnt);

  void dump();
  void output_short(FILE *fp);
  void output(FILE *fp);
};

//------------------------------Attribute--------------------------------------
class Attribute : public Form {
private:

public:
  // Public Data
  char *_ident;                    // Name of predefined attribute
  char *_val;                      // C++ source which evaluates to constant
  int   _atype;                    // Either INS_ATTR or OP_ATTR
  int   int_val(ArchDesc &ad);     // Return atoi(_val), ensuring syntax.

  // Public Methods
  Attribute(char *id, char* val, int type);
  ~Attribute();

  void dump();
  void output(FILE *fp);
};

//------------------------------FormatRule-------------------------------------
class FormatRule : public Form {
private:

public:
  // Public Data
  // There is an entry in _strings, perhaps NULL, that precedes each _rep_vars
  NameList  _strings;              // Strings passed through to tty->print
  NameList  _rep_vars;             // replacement variables
  char     *_temp;                 // String representing the assembly code

  // Public Methods
  FormatRule(char *temp);
  ~FormatRule();

  void dump();
  void output(FILE *fp);
};

#endif // SHARE_ADLC_FORMSSEL_HPP
