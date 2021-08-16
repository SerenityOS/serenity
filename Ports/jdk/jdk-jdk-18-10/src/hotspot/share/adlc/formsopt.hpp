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

#ifndef SHARE_ADLC_FORMSOPT_HPP
#define SHARE_ADLC_FORMSOPT_HPP

// FORMSOPT.HPP - ADL Parser Target Specific Optimization Forms Classes

// Class List
class Form;
class InstructForm;
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
class PeepMatch;
class PeepConstraint;
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
class PipeClassOperandForm;
class PipeClassResourceForm;
class PeepMatch;
class PeepConstraint;
class PeepReplace;
class MatchList;

class ArchDesc;

//==============================Register Allocation============================
//------------------------------RegisterForm-----------------------------------
class RegisterForm : public Form {
private:
  AllocClass *_current_ac;         // State used by iter_RegDefs()

public:
  // Public Data
  NameList    _rdefs;              // List of register definition names
  Dict        _regDef;             // map register name to RegDef*

  NameList    _rclasses;           // List of register class names
  Dict        _regClass;           // map register class name to RegClass*

  NameList    _aclasses;           // List of allocation class names
  Dict        _allocClass;         // Dictionary of allocation classes

  static int  _reg_ctr;         // Register counter
  static int  RegMask_Size();   // Compute RegMask size

  // Public Methods
  RegisterForm();
  ~RegisterForm();

  void        addRegDef(char *regName, char *callingConv, char *c_conv,
                        char * idealtype, char *encoding, char* concreteName);
  template<typename T> T* addRegClass(const char* className);

  AllocClass *addAllocClass(char *allocName);
  void        addSpillRegClass();
  void        addDynamicRegClass();

  // Provide iteration over all register definitions
  // in the order used by the register allocator
  void        reset_RegDefs();
  RegDef     *iter_RegDefs();
  RegDef     *getRegDef  (const char *regName);

  RegClass   *getRegClass(const char *className);

  // Return register mask, compressed chunk and register #
  uint       reg_mask(char *register_class);

  // Check that register classes are compatible with chunks
  bool       verify();

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};

//------------------------------RegDef-----------------------------------------
class RegDef : public Form {
public:
  // Public Data
  const char *_regname;            // ADLC (Opto) Register name
  const char *_callconv;           // Calling convention
  const char *_c_conv;             // Native calling convention, 'C'
  const char *_idealtype;          // Ideal Type for register save/restore
  const char *_concrete;           // concrete register name

private:
  const char *_register_encode;   // The register encoding
  // The chunk and register mask bits define info for register allocation
  uint32      _register_num;      // Which register am I

public:
  // Public Methods
  RegDef(char  *regname, char *callconv, char *c_conv,
         char *idealtype, char *encoding, char *concrete);
  ~RegDef();                       // Destructor

  // Interface to define/redefine the register number
  void     set_register_num(uint32 new_register_num);

  // Bit pattern used for generating machine code
  const char *register_encode()   const;
  // Register number used in machine-independent code
  uint32   register_num()      const;

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};

//------------------------------RegClass---------------------------------------
// Generic register class. This register class is the internal representation
// for the following .ad file format:
//
//  reg_class ptr(RAX, RBX, ...);
//
// where ptr is the name of the register class, RAX and RBX are registers.
//
// This register class allows registers to be spilled onto the stack. Spilling
// is allowed is field _stack_or_reg is true.
class RegClass : public Form {
public:
  // Public Data
  const char *_classid;         // Name of class
  NameList    _regDefs;         // List of registers in class
  Dict        _regDef;          // Dictionary of registers in class
protected:
  bool        _stack_or_reg;    // Allowed on any stack slot

public:
  // Public Methods
  RegClass(const char *classid);// Constructor
  virtual ~RegClass();

  void addReg(RegDef *regDef);  // Add a register to this class

  uint size() const;            // Number of registers in class
  int regs_in_word( int wordnum, bool stack_also );

  const RegDef *get_RegDef(const char *regDef_name) const;

  // Returns the lowest numbered register in the mask.
  const RegDef* find_first_elem();

  // Iteration support
  void          reset();        // Reset the following two iterators
  RegDef       *RegDef_iter();  // which move jointly,
  const char   *rd_name_iter(); // invoking either advances both.

  void dump();                  // Debug printer
  void output(FILE *fp);        // Write info to output files

  virtual bool has_stack_version() {
    return _stack_or_reg;
  }
  virtual void set_stack_version(bool flag) {
    _stack_or_reg = flag;
  }

  virtual void declare_register_masks(FILE* fp);
  virtual void build_register_masks(FILE* fp);
};

//------------------------------CodeSnippetRegClass----------------------------
// Register class that has an user-defined C++ code snippet attached to it
// to determine at runtime which register class to use. This register class is
// the internal representation for the following .ad file format:
//
//  reg_class actual_dflt_reg %{
//      if (VM_Version::has_vfp3_32()) {
//          return DFLT_REG_mask();
//      } else {
//          return DFLT_LOW_REG_mask();
//      }
//  %}
//
// where DFLT_REG_mask() and DFLT_LOW_REG_mask() are the internal names of the
// masks of register classes dflt_reg and dflt_low_reg.
//
// The attached code snippet can select also between more than two register classes.
// This register class can be, however, used only if the register class is not
// cisc-spillable (i.e., the registers of this class are not allowed on the stack,
// which is equivalent with _stack_or_reg being false).
class CodeSnippetRegClass : public RegClass {
protected:
  char* _code_snippet;
public:
  CodeSnippetRegClass(const char* classid);// Constructor
  ~CodeSnippetRegClass();

  void set_code_snippet(char* code) {
    _code_snippet = code;
  }
  char* code_snippet() {
    return _code_snippet;
  }
  void declare_register_masks(FILE* fp);
  void build_register_masks(FILE* fp) {
    // We do not need to generate register masks because we select at runtime
    // between register masks generated for other register classes.
    return;
  }
};

//------------------------------ConditionalRegClass----------------------------
// Register class that has two register classes and a runtime condition attached
// to it. The condition is evaluated at runtime and either one of the register
// attached register classes is selected. This register class is the internal
// representation for the following .ad format:
//
//  reg_class_dynamic actual_dflt_reg(dflt_reg, low_reg,
//                                    %{ VM_Version::has_vfp3_32() }%
//                                    );
//
// This example is equivalent to the example used with the CodeSnippetRegClass
// register class. A ConditionalRegClass works also if a register class is cisc-spillable
// (i.e., _stack_or_reg is true), but if can select only between two register classes.
class ConditionalRegClass : public RegClass {
protected:
  // reference to condition code
  char* _condition_code;  // C++ condition code to dynamically determine which register class to use.

                          // Example syntax (equivalent to previous example):
                          //
                          //  reg_class actual_dflt_reg(dflt_reg, low_reg,
                          //                            %{ VM_Version::has_vfp3_32() }%
                          //                            );
  // reference to conditional register classes
  RegClass* _rclasses[2]; // 0 is the register class selected if the condition code returns true
                          // 1 is the register class selected if the condition code returns false
public:
  ConditionalRegClass(const char* classid);// Constructor
  ~ConditionalRegClass();

  virtual void set_stack_version(bool flag) {
    RegClass::set_stack_version(flag);
    assert((_rclasses[0] != NULL), "Register class NULL for condition code == true");
    assert((_rclasses[1] != NULL), "Register class NULL for condition code == false");
    _rclasses[0]->set_stack_version(flag);
    _rclasses[1]->set_stack_version(flag);
  }
  void declare_register_masks(FILE* fp);
  void build_register_masks(FILE* fp) {
    // We do not need to generate register masks because we select at runtime
    // between register masks generated for other register classes.
    return;
  }
  void set_rclass_at_index(int index, RegClass* rclass) {
    assert((0 <= index && index < 2), "Condition code can select only between two register classes");
    _rclasses[index] = rclass;
  }
  void set_condition_code(char* code) {
    _condition_code = code;
  }
  char* condition_code() {
    return _condition_code;
  }
};

//------------------------------AllocClass-------------------------------------
class AllocClass : public Form {
private:

public:
  // Public Data
  char    *_classid;            // Name of class
  NameList _regDefs;            // List of registers in class
  Dict     _regDef;             // Dictionary of registers in class

  // Public Methods
  AllocClass(char *classid);    // Constructor

  void addReg(RegDef *regDef);  // Add a register to this class
  uint size() {return _regDef.Size();} // Number of registers in class

  void dump();                  // Debug printer
  void output(FILE *fp);        // Write info to output files
};


//==============================Frame Handling================================
//------------------------------FrameForm-------------------------------------
class FrameForm : public Form {
private:

public:
  // Public Data
  char *_sync_stack_slots;
  char *_inline_cache_reg;
  char *_interpreter_frame_pointer_reg;
  char *_cisc_spilling_operand_name;
  char *_frame_pointer;
  char *_c_frame_pointer;
  char *_alignment;
  bool  _return_addr_loc;
  bool  _c_return_addr_loc;
  char *_return_addr;
  char *_c_return_addr;
  char *_varargs_C_out_slots_killed;
  char *_return_value;
  char *_c_return_value;

  // Public Methods
  FrameForm();
  ~FrameForm();

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};


//==============================Scheduling=====================================
//------------------------------PipelineForm-----------------------------------
class PipelineForm : public Form {
private:

public:
  // Public Data
  NameList   _reslist;            // List of pipeline resources
  FormDict   _resdict;            // Resource Name -> ResourceForm mapping
  int        _rescount;           // Number of resources (ignores OR cases)
  int        _maxcycleused;       // Largest cycle used relative to beginning of instruction

  NameList   _stages;             // List of pipeline stages on architecture
  int        _stagecnt;           // Number of stages listed

  NameList   _classlist;          // List of pipeline classes
  FormDict   _classdict;          // Class Name -> PipeClassForm mapping
  int        _classcnt;           // Number of classes

  NameList   _noplist;            // List of NOP instructions
  int        _nopcnt;             // Number of nop instructions

  bool       _variableSizeInstrs; // Indicates if this architecture has variable sized instructions
  bool       _branchHasDelaySlot; // Indicates that branches have delay slot instructions
  int        _maxInstrsPerBundle; // Indicates the maximum number of instructions for ILP
  int        _maxBundlesPerCycle; // Indicates the maximum number of bundles for ILP
  int        _instrUnitSize;      // The minimum instruction unit size, in bytes
  int        _bundleUnitSize;     // The bundle unit size, in bytes
  int        _instrFetchUnitSize; // The size of the I-fetch unit, in bytes [must be power of 2]
  int        _instrFetchUnits;    // The number of I-fetch units processed per cycle

  // Public Methods
  PipelineForm();
  ~PipelineForm();

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};

//------------------------------ResourceForm-----------------------------------
class ResourceForm : public Form {
public:
  unsigned mask() const { return _resmask; };

private:
  // Public Data
  unsigned _resmask;         // Resource Mask (OR of resource specifier bits)

public:

  // Virtual Methods
  virtual ResourceForm  *is_resource()    const;

  // Public Methods
  ResourceForm(unsigned resmask); // Constructor
  ~ResourceForm();                // Destructor

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};

//------------------------------PipeClassOperandForm-----------------------------
class PipeClassOperandForm : public Form {
private:

public:
  // Public Data
  const char *_stage;             // Name of Stage
  unsigned _iswrite;              // Read or Write
  unsigned _more_instrs;          // Additional Instructions

  // Public Methods
  PipeClassOperandForm(const char *stage, unsigned iswrite, unsigned more_instrs)
  : _stage(stage)
  , _iswrite(iswrite)
  , _more_instrs(more_instrs)
 {};

  ~PipeClassOperandForm() {};     // Destructor

  bool isWrite() const { return _iswrite != 0; }

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};

//------------------------------PipeClassResourceForm--------------------------
class PipeClassResourceForm : public Form {
private:

public:
  // Public Data
  const char *_resource;          // Resource
  const char *_stage;             // Stage the resource is used in
  int         _cycles;            // Number of cycles the resource is used

  // Public Methods
  PipeClassResourceForm(const char *resource, const char *stage, int cycles)
                                  // Constructor
    : _resource(resource)
    , _stage(stage)
    , _cycles(cycles)
    {};

  ~PipeClassResourceForm() {};    // Destructor

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};

//------------------------------PipeClassForm----------------------------------
class PipeClassForm : public Form {
private:

public:

  // Public Data
  const char       *_ident;             // Name of class
  int               _num;               // Used in name of MachNode subclass
  NameList          _parameters;        // Locally defined names
  FormDict          _localNames;        // Table of operands & their types
  FormDict          _localUsage;        // Table of operand usage
  FormList          _resUsage;          // List of resource usage
  NameList          _instructs;         // List of instructions and machine nodes that use this pipeline class
  bool              _has_fixed_latency; // Always takes this number of cycles
  int               _fixed_latency;     // Always takes this number of cycles
  int               _instruction_count; // Number of instructions in first bundle
  bool              _has_multiple_bundles;  // Indicates if 1 or multiple bundles
  bool              _has_branch_delay_slot; // Has branch delay slot as last instruction
  bool              _force_serialization;   // This node serializes relative to surrounding nodes
  bool              _may_have_no_code;      // This node may generate no code based on register allocation

  // Virtual Methods
  virtual PipeClassForm  *is_pipeclass()    const;

  // Public Methods
  PipeClassForm(const char *id, int num);
                                  // Constructor
  ~PipeClassForm();               // Destructor

  bool hasFixedLatency() { return _has_fixed_latency; }
  int fixedLatency() { return _fixed_latency; }

  void setFixedLatency(int fixed_latency) { _has_fixed_latency = 1; _fixed_latency = fixed_latency; }

  void setInstructionCount(int i)    { _instruction_count = i; }
  void setMultipleBundles(bool b)    { _has_multiple_bundles = b; }
  void setBranchDelay(bool s)        { _has_branch_delay_slot = s; }
  void setForceSerialization(bool s) { _force_serialization = s; }
  void setMayHaveNoCode(bool s)      { _may_have_no_code = s; }

  int  InstructionCount()   const { return _instruction_count; }
  bool hasMultipleBundles() const { return _has_multiple_bundles; }
  bool hasBranchDelay()     const { return _has_branch_delay_slot; }
  bool forceSerialization() const { return _force_serialization; }
  bool mayHaveNoCode()      const { return _may_have_no_code; }

  void dump();                    // Debug printer
  void output(FILE *fp);          // Write info to output files
};


//==============================Peephole Optimization==========================
//------------------------------Peephole---------------------------------------
class Peephole : public Form {
private:
  static int      _peephole_counter;// Incremented by each peephole rule parsed
  int             _peephole_number;// Remember my order in architecture description
  PeepMatch      *_match;          // Instruction pattern to match
  PeepConstraint *_constraint;     // List of additional constraints
  PeepReplace    *_replace;        // Instruction pattern to substitute in

  Peephole *_next;

public:
  // Public Methods
  Peephole();
  ~Peephole();

  // Append a peephole rule with the same root instruction
  void append_peephole(Peephole *next_peephole);

  // Store the components of this peephole rule
  void add_match(PeepMatch *only_one_match);
  void append_constraint(PeepConstraint *next_constraint);
  void add_replace(PeepReplace *only_one_replacement);

  // Access the components of this peephole rule
  int             peephole_number() { return _peephole_number; }
  PeepMatch      *match()       { return _match; }
  PeepConstraint *constraints() { return _constraint; }
  PeepReplace    *replacement() { return _replace; }
  Peephole       *next()        { return _next; }

  void dump();                     // Debug printer
  void output(FILE *fp);           // Write info to output files
};


class PeepMatch : public Form {
private:
  char *_rule;
  // NameList  _depth;                // Depth of this instruction
  NameList  _parent;
  NameList  _position;
  NameList  _instrs;               // List of instructions in match rule
  NameList  _input;                // input position in parent's instruction
  int       _max_position;

public:
  // Public Methods
  PeepMatch(char *rule);
  ~PeepMatch();

  // Insert info into the match-rule
  void  add_instruction(int parent, int position, const char *name, int input);

  // Access info about instructions in the peep-match rule
  int   max_position();
  const char *instruction_name(int position);
  // Iterate through all info on matched instructions
  void  reset();
  void  next_instruction(int &parent, int &position, const char* &name, int &input);
  // 'true' if current position in iteration is a placeholder, not matched.
  bool  is_placeholder();

  void dump();
  void output(FILE *fp);
};


class PeepConstraint : public Form {
private:
  PeepConstraint *_next;           // Additional constraints ANDed together

public:
  const int   _left_inst;
  const char* _left_op;
  const char* _relation;
  const int   _right_inst;
  const char* _right_op;

public:
  // Public Methods
  PeepConstraint(int left_inst,  char* left_op, char* relation,
                 int right_inst, char* right_op);
  ~PeepConstraint();

  // Check if constraints use instruction at position
  bool constrains_instruction(int position);

  // Add another constraint
  void append(PeepConstraint *next_peep_constraint);
  // Access the next constraint in the list
  PeepConstraint *next();

  void dump();
  void output(FILE *fp);
};


class PeepReplace : public Form {
private:
  char *_rule;
  NameList _instruction;
  NameList _operand_inst_num;
  NameList _operand_op_name;

public:

  // Public Methods
  PeepReplace(char *rule);
  ~PeepReplace();

  // Add contents of peepreplace
  void  add_instruction(char *root);
  void  add_operand( int inst_num, char *inst_operand );

  // Access contents of peepreplace
  void  reset();
  void  next_instruction(const char * &root);
  void  next_operand(int &inst_num, const char * &inst_operand );

  // Utilities
  void dump();
  void output(FILE *fp);
};


class PeepChild : public Form {
public:
  const int   _inst_num;         // Number of instruction (-1 if only named)
  const char *_inst_op;          // Instruction's operand, NULL if number == -1
  const char *_inst_name;        // Name of the instruction

public:
  PeepChild(char *inst_name)
    : _inst_num(-1), _inst_op(NULL), _inst_name(inst_name) {};
  PeepChild(int inst_num, char *inst_op, char *inst_name)
    : _inst_num(inst_num), _inst_op(inst_op), _inst_name(inst_name) {};
  ~PeepChild();

  bool  use_leaf_operand()        { return _inst_num != -1; };
  bool  generate_an_instruction() { return _inst_num == -1; }

  void dump();
  void output(FILE *fp);
};

#endif // SHARE_ADLC_FORMSOPT_HPP
