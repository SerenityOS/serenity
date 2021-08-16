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

#ifndef SHARE_ADLC_ARCHDESC_HPP
#define SHARE_ADLC_ARCHDESC_HPP

// Definitions for Error Flags
#define  WARN   0
#define  SYNERR 1
#define  SEMERR 2
#define  INTERNAL_ERR 3

// Minimal declarations for include files
class  OutputMap;
class  ProductionState;
class  Expr;

// STRUCTURE FOR HANDLING INPUT AND OUTPUT FILES
typedef BufferedFile ADLFILE;

//---------------------------ChainList-----------------------------------------
class ChainList {
  NameList _name;
  NameList _cost;
  NameList _rule;

public:
  void insert(const char *name, const char *cost, const char *rule);
  bool search(const char *name);

  void reset();
  bool iter(const char * &name, const char * &cost, const char * &rule);

  void dump();
  void output(FILE *fp);

  ChainList();
  ~ChainList();
};

//---------------------------MatchList-----------------------------------------
class MatchList {
private:
  MatchList  *_next;
  Predicate  *_pred;          // Predicate which applies to this match rule
  const char *_cost;

public:
  const char *_opcode;
  const char *_resultStr;
  const char *_lchild;
  const char *_rchild;

  MatchList(MatchList *nxt, Predicate *prd): _next(nxt), _pred(prd), _cost(NULL){
    _resultStr = _lchild = _rchild = _opcode = NULL; }

  MatchList(MatchList *nxt, Predicate *prd, const char *cost,
            const char *opcode, const char *resultStr, const char *lchild,
            const char *rchild)
    : _next(nxt), _pred(prd), _cost(cost), _opcode(opcode),
      _resultStr(resultStr), _lchild(lchild), _rchild(rchild) { }

  MatchList  *get_next(void)  { return _next; }
  char       *get_pred(void)  { return (_pred?_pred->_pred:NULL); }
  Predicate  *get_pred_obj(void)  { return _pred; }
  const char *get_cost(void) { return _cost == NULL ? "0" :_cost; }
  bool        search(const char *opc, const char *res, const char *lch,
                    const char *rch, Predicate *pr);

  void        dump();
  void        output(FILE *fp);
};

//---------------------------ArchDesc------------------------------------------
class ArchDesc {
private:
  FormDict      _globalNames;        // Global names
  Dict          _idealIndex;         // Map ideal names to index in enumeration
  ExprDict      _globalDefs;         // Global definitions, #defines
  int           _internalOpCounter;  // Internal Operand Counter

  FormList      _header;             // List of Source Code Forms for hpp file
  FormList      _pre_header;         // ditto for the very top of the hpp file
  FormList      _source;             // List of Source Code Forms for output
  FormList      _instructions;       // List of Instruction Forms for output
  FormList      _machnodes;          // List of Node Classes (special for pipelining)
  FormList      _operands;           // List of Operand Forms for output
  FormList      _opclass;            // List of Operand Class Forms for output
  FormList      _attributes;         // List of Attribute Forms for parsing
  RegisterForm *_register;           // Only one Register Form allowed
  FrameForm    *_frame;              // Describe stack-frame layout
  EncodeForm   *_encode;             // Only one Encode Form allowed
  PipelineForm *_pipeline;           // Pipeline Form for output

  bool _has_match_rule[_last_opcode];  // found AD rule for ideal node in <arch>.ad

  MatchList    *_mlistab[_last_opcode]; // Array of MatchLists

  // The Architecture Description identifies which user-defined operand can be used
  // to access [stack_pointer + offset]
  OperandForm  *_cisc_spill_operand;

  // If a Call node uses $constanttablebase, it gets MachConstantBaseNode
  // by the matcher and the matcher will modify the jvms. If so, jvm states
  // always have to be deep cloned when a node is cloned. Adlc generates
  // Compile::needs_deep_clone_jvms() accordingly.
  bool _needs_deep_clone_jvms;

  // Methods for outputting the DFA
  void gen_match(FILE *fp, MatchList &mlist, ProductionState &status, Dict &operands_chained_from);
  void chain_rule(FILE *fp, const char *indent, const char *ideal,
                  const Expr *icost, const char *irule,
                  Dict &operands_chained_from, ProductionState &status);
  void expand_opclass(FILE *fp, const char *indent, const Expr *cost,
                      const char *result_type, ProductionState &status);
  Expr *calc_cost(FILE *fp, const char *spaces, MatchList &mList, ProductionState &status);
  void prune_matchlist(Dict &minimize, MatchList &mlist);

  // Helper function that outputs code to generate an instruction in MachNodeGenerator
  void buildMachNode(FILE *fp_cpp, InstructForm *inst, const char *indent);

public:
  ArchDesc();
  ~ArchDesc();

  // Option flags which control miscellaneous behaviors throughout the code
  int   _TotalLines;                    // Line Counter
  int   _no_output;                     // Flag to disable output of DFA, etc.
  int   _quiet_mode;                    // Do not output banner messages, etc.
  int   _disable_warnings;              // Do not output warning messages
  int   _dfa_debug;                     // Debug Flag for generated DFA
  int   _dfa_small;                     // Debug Flag for generated DFA
  int   _adl_debug;                     // Debug Flag for ADLC
  int   _adlocation_debug;              // Debug Flag to use ad file locations
  bool  _cisc_spill_debug;              // Debug Flag to see cisc-spill-instructions
  bool  _short_branch_debug;            // Debug Flag to see short branch instructions

  // Error/Warning Counts
  int _syntax_errs;                  // Count of syntax errors
  int _semantic_errs;                // Count of semantic errors
  int _warnings;                     // Count warnings
  int _internal_errs;                // Count of internal errors

  // Accessor for private data.
  void has_match_rule(int opc, const bool b) { _has_match_rule[opc] = b; }

  // I/O Files
  ADLFILE  _ADL_file;          // Input Architecture Description File
  // Machine dependent files, built from architecture definition
  ADLFILE  _DFA_file;          // File for definition of Matcher::DFA
  ADLFILE  _HPP_file;          // File for ArchNode class declarations
  ADLFILE  _CPP_file;          // File for ArchNode class defintions
  ADLFILE  _CPP_CLONE_file;    // File for MachNode/Oper clone defintions
  ADLFILE  _CPP_EXPAND_file;   // File for MachNode expand methods
  ADLFILE  _CPP_FORMAT_file;   // File for MachNode/Oper format defintions
  ADLFILE  _CPP_GEN_file;      // File for MachNode/Oper generator methods
  ADLFILE  _CPP_MISC_file;     // File for miscellaneous MachNode/Oper tables & methods
  ADLFILE  _CPP_PEEPHOLE_file; // File for MachNode peephole methods
  ADLFILE  _CPP_PIPELINE_file; // File for MachNode pipeline defintions
  ADLFILE  _VM_file;           // File for constants needed in VM code
  ADLFILE  _bug_file;          // DFA debugging file

  // I/O helper methods
  int  open_file(bool required, ADLFILE & adf, const char *action);
  void close_file(int delete_out, ADLFILE & adf);
  int  open_files(void);
  void close_files(int delete_out);

  Dict _chainRules;            // Maps user operand names to ChainRules
  Dict _internalOps;           // Maps match strings to internal operand names
  NameList _internalOpNames;   // List internal operand names
  Dict _internalMatch;         // Map internal name to its MatchNode

  NameList      _preproc_list; // Preprocessor flag names
  FormDict      _preproc_table;// Preprocessor flag bindings
  char* get_preproc_def(const char* flag);
  void  set_preproc_def(const char* flag, const char* def);

  FormDict& globalNames() {return _globalNames;} // map global names to forms
  void initKeywords(FormDict& globals);  // Add keywords to global name table

  ExprDict& globalDefs()  {return _globalDefs;}  // map global names to expressions

  OperandForm *constructOperand(const char *ident, bool ideal_only);
  void initBaseOpTypes();            // Import predefined base types.

  void addForm(PreHeaderForm *ptr);  // Add objects to pre-header list
  void addForm(HeaderForm *ptr);     // Add objects to header list
  void addForm(SourceForm *ptr);     // Add objects to source list
  void addForm(EncodeForm *ptr);     // Add objects to encode list
  void addForm(InstructForm *ptr);   // Add objects to the instruct list
  void addForm(OperandForm *ptr);    // Add objects to the operand list
  void addForm(OpClassForm *ptr);    // Add objects to the opclasss list
  void addForm(AttributeForm *ptr);  // Add objects to the attributes list
  void addForm(RegisterForm *ptr);   // Add objects to the register list
  void addForm(FrameForm    *ptr);   // Add objects to the frame list
  void addForm(PipelineForm *ptr);   // Add objects to the pipeline list
  void addForm(MachNodeForm *ptr);   // Add objects to the machnode list

  int  operandFormCount();           // Count number of OperandForms defined
  int  opclassFormCount();           // Count number of OpClassForms defined
  int  instructFormCount();          // Count number of InstructForms defined

  inline void getForm(EncodeForm **ptr)     { *ptr = _encode; }

  bool verify();
  void dump();

  // Helper utility that gets MatchList components from inside MatchRule
  void check_optype(MatchRule *mrule);
  void build_chain_rule(OperandForm *oper);
  void add_chain_rule_entry(const char *src, const char *cost,
                            const char *result);
  const char *getMatchListIndex(MatchRule &mrule);
  void generateMatchLists();         // Build MatchList array and populate it
  void inspectOperands();            // Build MatchLists for all operands
  void inspectOpClasses();           // Build MatchLists for all operands
  void inspectInstructions();        // Build MatchLists for all operands
  void buildDFA(FILE *fp);           // Driver for constructing the DFA
  void gen_dfa_state_body(FILE *fp, Dict &minmize, ProductionState &status, Dict &chained, int i);    // Driver for constructing the DFA state bodies

  // Helper utilities to generate reduction maps for internal operands
  const char *reduceLeft (char *internalName);
  const char *reduceRight(char *internalName);

  // Build enumerations, (1) dense operand index, (2) operands and opcodes
  const char *machOperEnum(const char *opName);       // create dense index names using static function
  static const char *getMachOperEnum(const char *opName);// create dense index name
  void buildMachOperEnum(FILE *fp_hpp);// dense enumeration for operands
  void buildMachOpcodesEnum(FILE *fp_hpp);// enumeration for MachOpers & MachNodes

  // Helper utilities to generate Register Masks
  RegisterForm *get_registers() { return _register; }
  const char *reg_mask(OperandForm  &opForm);
  const char *reg_mask(InstructForm &instForm);
  const char *reg_class_to_reg_mask(const char *reg_class);
  char *stack_or_reg_mask(OperandForm  &opForm);  // name of cisc_spillable version
  // This register class should also generate a stack_or_reg_mask
  void  set_stack_or_reg(const char *reg_class_name); // for cisc-spillable reg classes
  // Generate an enumeration of register mask names and the RegMask objects.
  void  declare_register_masks(FILE *fp_cpp);
  void  build_register_masks(FILE *fp_cpp);
  // Generate enumeration of machine register numbers
  void  buildMachRegisterNumbers(FILE *fp_hpp);
  // Generate enumeration of machine register encodings
  void  buildMachRegisterEncodes(FILE *fp_hpp);
  // Generate Regsiter Size Array
  void  declareRegSizes(FILE *fp_hpp);
  // Generate Pipeline Class information
  void declare_pipe_classes(FILE *fp_hpp);
  // Generate Pipeline definitions
  void build_pipeline_enums(FILE *fp_cpp);
  // Generate Pipeline Class information
  void build_pipe_classes(FILE *fp_cpp);

  // Declare and define mappings from rules to result and input types
  void build_map(OutputMap &map);
  void buildReduceMaps(FILE *fp_hpp, FILE *fp_cpp);
  // build flags for signaling that our machine needs this instruction cloned
  void buildMustCloneMap(FILE *fp_hpp, FILE *fp_cpp);

  // output SUN copyright info
  void addSunCopyright(char* legal, int size, FILE *fp);
  // output the start of an include guard.
  void addIncludeGuardStart(ADLFILE &adlfile, const char* guardString);
  // output the end of an include guard.
  void addIncludeGuardEnd(ADLFILE &adlfile, const char* guardString);
  // output the #include line for this file.
  void addInclude(ADLFILE &adlfile, const char* fileName);
  void addInclude(ADLFILE &adlfile, const char* includeDir, const char* fileName);
  // Output C preprocessor code to verify the backend compilation environment.
  void addPreprocessorChecks(FILE *fp);
  // Output C source and header (source_hpp) blocks.
  void addPreHeaderBlocks(FILE *fp_hpp);
  void addHeaderBlocks(FILE *fp_hpp);
  void addSourceBlocks(FILE *fp_cpp);
  void generate_needs_deep_clone_jvms(FILE *fp_cpp);
  void generate_adlc_verification(FILE *fp_cpp);

  // output declaration of class State
  void defineStateClass(FILE *fp);

  // Generator for MachOper objects given integer type
  void buildMachOperGenerator(FILE *fp_cpp);
  // Generator for MachNode objects given integer type
  void buildMachNodeGenerator(FILE *fp_cpp);

  // Generator for Expand methods for instructions with expand rules
  void defineExpand      (FILE *fp, InstructForm *node);
  // Generator for Peephole methods for instructions with peephole rules
  void definePeephole    (FILE *fp, InstructForm *node);
  // Generator for Size methods for instructions
  void defineSize        (FILE *fp, InstructForm &node);

public:
  // Generator for EvalConstantValue methods for instructions
  void defineEvalConstant(FILE *fp, InstructForm &node);
  // Generator for Emit methods for instructions
  void defineEmit        (FILE *fp, InstructForm &node);
  // Generator for postalloc_expand methods for instructions.
  void define_postalloc_expand(FILE *fp, InstructForm &node);

  // Define a MachOper encode method
  void define_oper_interface(FILE *fp, OperandForm &oper, FormDict &globals,
                             const char *name, const char *encoding);

  // Methods to construct the MachNode class hierarchy
  // Return the type signature for the ideal operation
  const char *getIdealType(const char *idealOp);
  // Declare and define the classes derived from MachOper and MachNode
  void declareClasses(FILE *fp_hpp);
  void defineClasses(FILE *fp_cpp);

  // Emit an ADLC message
  void internal_err( const char *fmt, ...);
  void syntax_err  ( int lineno, const char *fmt, ...);
  int  emit_msg(int quiet, int flag, int linenum, const char *fmt,
       va_list args);

  // Generator for has_match_rule methods
  void buildInstructMatchCheck(FILE *fp_cpp) const;

  // Generator for Frame Methods
  void buildFrameMethods(FILE *fp_cpp);

  // Generate CISC_spilling oracle and MachNode::cisc_spill() methods
  void          build_cisc_spill_instructions(FILE *fp_hpp, FILE *fp_cpp);
  void          identify_cisc_spill_instructions();
  void          identify_short_branches();
  void          identify_unique_operands();
  void          set_cisc_spill_operand(OperandForm *opForm) { _cisc_spill_operand = opForm; }
  OperandForm  *cisc_spill_operand() { return _cisc_spill_operand; }
  bool          can_cisc_spill() { return _cisc_spill_operand != NULL; }


protected:
  // build MatchList from MatchRule
  void buildMatchList(MatchRule *mrule, const char *resultStr,
                      const char *rootOp, Predicate *pred, const char *cost);

  void buildMList(MatchNode *node, const char *rootOp, const char *resultOp,
                  Predicate *pred, const char *cost);

  friend class ADLParser;

};


// -------------------------------- maps ------------------------------------

// Base class for generating a mapping from rule number to value.
// Used with ArchDesc::build_map() for all maps except "enum MachOperands"
// A derived class defines the appropriate output for a specific mapping.
class OutputMap {
protected:
  FILE       *_hpp;
  FILE       *_cpp;
  FormDict   &_globals;
  ArchDesc   &_AD;
  const char *_name;
public:
  OutputMap (FILE *decl_file, FILE *def_file, FormDict &globals, ArchDesc &AD, const char *name)
    : _hpp(decl_file), _cpp(def_file), _globals(globals), _AD(AD), _name(name) {};
  // Access files used by this routine
  FILE        *decl_file() { return _hpp; }
  FILE        *def_file()  { return _cpp; }
  // Positions in iteration that derived class will be told about
  enum position { BEGIN_OPERANDS,
                  BEGIN_OPCLASSES,
                  BEGIN_INTERNALS,
                  BEGIN_INSTRUCTIONS,
                  BEGIN_INST_CHAIN_RULES,
                  END_INST_CHAIN_RULES,
                  BEGIN_REMATERIALIZE,
                  END_REMATERIALIZE,
                  END_INSTRUCTIONS
  };
  // Output routines specific to the derived class
  virtual void declaration() {}
  virtual void definition()  {}
  virtual void closing()     {  fprintf(_cpp, "};\n"); }
  virtual void map(OperandForm  &oper) { }
  virtual void map(OpClassForm  &opc)  { }
  virtual void map(char         *internal_name) { }
  // Allow enum-MachOperands to turn-off instructions
  virtual bool do_instructions()       { return true; }
  virtual void map(InstructForm &inst) { }
  // Allow derived class to output name and position specific info
  virtual void record_position(OutputMap::position place, int index) {}
};

#endif // SHARE_ADLC_ARCHDESC_HPP
