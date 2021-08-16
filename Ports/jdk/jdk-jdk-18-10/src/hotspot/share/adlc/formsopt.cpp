/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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

// FORMS.CPP - Definitions for ADL Parser Forms Classes
#include "adlc.hpp"

//==============================Register Allocation============================
int RegisterForm::_reg_ctr = 0;

//------------------------------RegisterForm-----------------------------------
// Constructor
RegisterForm::RegisterForm()
  : _regDef(cmpstr,hashstr, Form::arena),
    _regClass(cmpstr,hashstr, Form::arena),
    _allocClass(cmpstr,hashstr, Form::arena) {
}
RegisterForm::~RegisterForm() {
}

// record a new register definition
void RegisterForm::addRegDef(char *name, char *callingConv, char *c_conv,
                             char *idealtype, char *encoding, char* concrete) {
  RegDef *regDef = new RegDef(name, callingConv, c_conv, idealtype, encoding, concrete);
  _rdefs.addName(name);
  _regDef.Insert(name,regDef);
}

// record a new register class
template <typename T>
T* RegisterForm::addRegClass(const char* className) {
  T* regClass = new T(className);
  _rclasses.addName(className);
  _regClass.Insert(className, regClass);
  return regClass;
}

// Explicit instantiation for all supported register classes.
template RegClass* RegisterForm::addRegClass<RegClass>(const char* className);
template CodeSnippetRegClass* RegisterForm::addRegClass<CodeSnippetRegClass>(const char* className);
template ConditionalRegClass* RegisterForm::addRegClass<ConditionalRegClass>(const char* className);

// record a new register class
AllocClass *RegisterForm::addAllocClass(char *className) {
  AllocClass *allocClass = new AllocClass(className);
  _aclasses.addName(className);
  _allocClass.Insert(className,allocClass);
  return allocClass;
}

// Called after parsing the Register block.  Record the register class
// for spill-slots/regs.
void RegisterForm::addSpillRegClass() {
  // Stack slots start at the next available even register number.
  _reg_ctr = (_reg_ctr+7) & ~7;
  const char *rc_name = "stack_slots";
  RegClass* reg_class = new RegClass(rc_name);
  reg_class->set_stack_version(true);
  _rclasses.addName(rc_name);
  _regClass.Insert(rc_name,reg_class);
}

// Called after parsing the Register block.  Record the register class
// for operands which are overwritten after matching.
void RegisterForm::addDynamicRegClass() {
  const char *rc_name = "dynamic";
  RegClass* reg_class = new RegClass(rc_name);
  reg_class->set_stack_version(false);
  _rclasses.addName(rc_name);
  _regClass.Insert(rc_name,reg_class);
}

// Provide iteration over all register definitions
// in the order used by the register allocator
void        RegisterForm::reset_RegDefs() {
  _current_ac = NULL;
  _aclasses.reset();
}

RegDef     *RegisterForm::iter_RegDefs() {
  // Check if we need to get the next AllocClass
  if ( _current_ac == NULL ) {
    const char *ac_name = _aclasses.iter();
    if( ac_name == NULL )   return NULL;   // No more allocation classes
    _current_ac = (AllocClass*)_allocClass[ac_name];
    _current_ac->_regDefs.reset();
    assert( _current_ac != NULL, "Name must match an allocation class");
  }

  const char *rd_name = _current_ac->_regDefs.iter();
  if( rd_name == NULL ) {
    // At end of this allocation class, check the next
    _current_ac = NULL;
    return iter_RegDefs();
  }
  RegDef *reg_def = (RegDef*)_current_ac->_regDef[rd_name];
  assert( reg_def != NULL, "Name must match a register definition");
  return reg_def;
}

// return the register definition with name 'regName'
RegDef *RegisterForm::getRegDef(const char *regName) {
  RegDef *regDef = (RegDef*)_regDef[regName];
  return  regDef;
}

// return the register class with name 'className'
RegClass *RegisterForm::getRegClass(const char *className) {
  RegClass *regClass = (RegClass*)_regClass[className];
  return    regClass;
}


// Check that register classes are compatible with chunks
bool   RegisterForm::verify() {
  bool valid = true;

  // Verify Register Classes
  // check that each register class contains registers from one chunk
  const char *rc_name = NULL;
  _rclasses.reset();
  while ( (rc_name = _rclasses.iter()) != NULL ) {
    // Check the chunk value for all registers in this class
    RegClass *reg_class = getRegClass(rc_name);
    assert( reg_class != NULL, "InternalError() no matching register class");
  } // end of RegClasses

  // Verify that every register has been placed into an allocation class
  RegDef *reg_def = NULL;
  reset_RegDefs();
  uint  num_register_zero = 0;
  while ( (reg_def = iter_RegDefs()) != NULL ) {
    if( reg_def->register_num() == 0 )  ++num_register_zero;
  }
  if( num_register_zero > 1 ) {
    fprintf(stderr,
            "ERROR: More than one register has been assigned register-number 0.\n"
            "Probably because a register has not been entered into an allocation class.\n");
  }

  return  valid;
}

// Compute RegMask size
int RegisterForm::RegMask_Size() {
  // Need at least this many words
  int words_for_regs = (_reg_ctr + 31)>>5;
  // The array of Register Mask bits should be large enough to cover
  // all the machine registers and all parameters that need to be passed
  // on the stack (stack registers) up to some interesting limit.  Methods
  // that need more parameters will NOT be compiled.  On Intel, the limit
  // is something like 90+ parameters.
  // Add a few (3 words == 96 bits) for incoming & outgoing arguments to calls.
  // Round up to the next doubleword size.
  return (words_for_regs + 3 + 1) & ~1;
}

void RegisterForm::dump() {                  // Debug printer
  output(stderr);
}

void RegisterForm::output(FILE *fp) {          // Write info to output files
  const char *name;
  fprintf(fp,"\n");
  fprintf(fp,"-------------------- Dump RegisterForm --------------------\n");
  for(_rdefs.reset(); (name = _rdefs.iter()) != NULL;) {
    ((RegDef*)_regDef[name])->output(fp);
  }
  fprintf(fp,"\n");
  for (_rclasses.reset(); (name = _rclasses.iter()) != NULL;) {
    ((RegClass*)_regClass[name])->output(fp);
  }
  fprintf(fp,"\n");
  for (_aclasses.reset(); (name = _aclasses.iter()) != NULL;) {
    ((AllocClass*)_allocClass[name])->output(fp);
  }
  fprintf(fp,"-------------------- end  RegisterForm --------------------\n");
}

//------------------------------RegDef-----------------------------------------
// Constructor
RegDef::RegDef(char *regname, char *callconv, char *c_conv, char * idealtype, char * encode, char * concrete)
  : _regname(regname), _callconv(callconv), _c_conv(c_conv),
    _idealtype(idealtype),
    _register_encode(encode),
    _concrete(concrete),
    _register_num(0) {

  // Chunk and register mask are determined by the register number
  // _register_num is set when registers are added to an allocation class
}
RegDef::~RegDef() {                      // Destructor
}

void RegDef::set_register_num(uint32 register_num) {
  _register_num      = register_num;
}

// Bit pattern used for generating machine code
const char* RegDef::register_encode() const {
  return _register_encode;
}

// Register number used in machine-independent code
uint32 RegDef::register_num()    const {
  return _register_num;
}

void RegDef::dump() {
  output(stderr);
}

void RegDef::output(FILE *fp) {         // Write info to output files
  fprintf(fp,"RegDef: %s (%s) encode as %s  using number %d\n",
          _regname, (_callconv?_callconv:""), _register_encode, _register_num);
  fprintf(fp,"\n");
}


//------------------------------RegClass---------------------------------------
// Construct a register class into which registers will be inserted
RegClass::RegClass(const char* classid) : _stack_or_reg(false), _classid(classid), _regDef(cmpstr, hashstr, Form::arena) {
}

RegClass::~RegClass() {
}

// record a register in this class
void RegClass::addReg(RegDef *regDef) {
  _regDefs.addName(regDef->_regname);
  _regDef.Insert((void*)regDef->_regname, regDef);
}

// Number of registers in class
uint RegClass::size() const {
  return _regDef.Size();
}

const RegDef *RegClass::get_RegDef(const char *rd_name) const {
  return  (const RegDef*)_regDef[rd_name];
}

void RegClass::reset() {
  _regDefs.reset();
}

const char *RegClass::rd_name_iter() {
  return _regDefs.iter();
}

RegDef *RegClass::RegDef_iter() {
  const char *rd_name  = rd_name_iter();
  RegDef     *reg_def  = rd_name ? (RegDef*)_regDef[rd_name] : NULL;
  return      reg_def;
}

const RegDef* RegClass::find_first_elem() {
  const RegDef* first = NULL;
  const RegDef* def = NULL;

  reset();
  while ((def = RegDef_iter()) != NULL) {
    if (first == NULL || def->register_num() < first->register_num()) {
      first = def;
    }
  }

  assert(first != NULL, "empty mask?");
  return first;;
}

// Collect all the registers in this register-word.  One bit per register.
int RegClass::regs_in_word( int wordnum, bool stack_also ) {
  int         word = 0;
  const char *name;
  for(_regDefs.reset(); (name = _regDefs.iter()) != NULL;) {
    int rnum = ((RegDef*)_regDef[name])->register_num();
    if( (rnum >> 5) == wordnum )
      word |= (1 << (rnum & 31));
  }
  if( stack_also ) {
    // Now also collect stack bits
    for( int i = 0; i < 32; i++ )
      if( wordnum*32+i >= RegisterForm::_reg_ctr )
        word |= (1 << i);
  }

  return word;
}

void RegClass::dump() {
  output(stderr);
}

void RegClass::output(FILE *fp) {           // Write info to output files
  fprintf(fp,"RegClass: %s\n",_classid);
  const char *name;
  for(_regDefs.reset(); (name = _regDefs.iter()) != NULL;) {
    ((RegDef*)_regDef[name])->output(fp);
  }
  fprintf(fp,"--- done with entries for reg_class %s\n\n",_classid);
}

void RegClass::declare_register_masks(FILE* fp) {
  const char* prefix = "";
  const char* rc_name_to_upper = toUpper(_classid);
  fprintf(fp, "extern const RegMask _%s%s_mask;\n", prefix,  rc_name_to_upper);
  fprintf(fp, "inline const RegMask &%s%s_mask() { return _%s%s_mask; }\n", prefix, rc_name_to_upper, prefix, rc_name_to_upper);
  if (_stack_or_reg) {
    fprintf(fp, "extern const RegMask _%sSTACK_OR_%s_mask;\n", prefix, rc_name_to_upper);
    fprintf(fp, "inline const RegMask &%sSTACK_OR_%s_mask() { return _%sSTACK_OR_%s_mask; }\n", prefix, rc_name_to_upper, prefix, rc_name_to_upper);
  }
  delete[] rc_name_to_upper;
}

void RegClass::build_register_masks(FILE* fp) {
  int len = RegisterForm::RegMask_Size();
  const char *prefix = "";
  const char* rc_name_to_upper = toUpper(_classid);
  fprintf(fp, "const RegMask _%s%s_mask(", prefix, rc_name_to_upper);

  int i;
  for(i = 0; i < len - 1; i++) {
    fprintf(fp," 0x%x,", regs_in_word(i, false));
  }
  fprintf(fp," 0x%x );\n", regs_in_word(i, false));

  if (_stack_or_reg) {
    fprintf(fp, "const RegMask _%sSTACK_OR_%s_mask(", prefix, rc_name_to_upper);
    for(i = 0; i < len - 1; i++) {
      fprintf(fp," 0x%x,", regs_in_word(i, true));
    }
    fprintf(fp," 0x%x );\n", regs_in_word(i, true));
  }
  delete[] rc_name_to_upper;
}

//------------------------------CodeSnippetRegClass---------------------------
CodeSnippetRegClass::CodeSnippetRegClass(const char* classid) : RegClass(classid), _code_snippet(NULL) {
}

CodeSnippetRegClass::~CodeSnippetRegClass() {
  delete _code_snippet;
}

void CodeSnippetRegClass::declare_register_masks(FILE* fp) {
  const char* prefix = "";
  const char* rc_name_to_upper = toUpper(_classid);
  fprintf(fp, "inline const RegMask &%s%s_mask() { %s }\n", prefix, rc_name_to_upper, _code_snippet);
  delete[] rc_name_to_upper;
}

//------------------------------ConditionalRegClass---------------------------
ConditionalRegClass::ConditionalRegClass(const char *classid) : RegClass(classid), _condition_code(NULL) {
}

ConditionalRegClass::~ConditionalRegClass() {
  delete _condition_code;
}

void ConditionalRegClass::declare_register_masks(FILE* fp) {
  const char* prefix = "";
  const char* rc_name_to_upper = toUpper(_classid);
  const char* rclass_0_to_upper = toUpper(_rclasses[0]->_classid);
  const char* rclass_1_to_upper = toUpper(_rclasses[1]->_classid);
  fprintf(fp, "inline const RegMask &%s%s_mask() {"
              " return (%s) ?"
              " %s%s_mask() :"
              " %s%s_mask(); }\n",
              prefix, rc_name_to_upper,
              _condition_code,
              prefix, rclass_0_to_upper,
              prefix, rclass_1_to_upper);
  if (_stack_or_reg) {
    fprintf(fp, "inline const RegMask &%sSTACK_OR_%s_mask() {"
                  " return (%s) ?"
                  " %sSTACK_OR_%s_mask() :"
                  " %sSTACK_OR_%s_mask(); }\n",
                  prefix, rc_name_to_upper,
                  _condition_code,
                  prefix, rclass_0_to_upper,
                  prefix, rclass_1_to_upper);
  }
  delete[] rc_name_to_upper;
  delete[] rclass_0_to_upper;
  delete[] rclass_1_to_upper;
  return;
}

//------------------------------AllocClass-------------------------------------
AllocClass::AllocClass(char *classid) : _classid(classid), _regDef(cmpstr,hashstr, Form::arena) {
}

// record a register in this class
void AllocClass::addReg(RegDef *regDef) {
  assert( regDef != NULL, "Can not add a NULL to an allocation class");
  regDef->set_register_num( RegisterForm::_reg_ctr++ );
  // Add regDef to this allocation class
  _regDefs.addName(regDef->_regname);
  _regDef.Insert((void*)regDef->_regname, regDef);
}

void AllocClass::dump() {
  output(stderr);
}

void AllocClass::output(FILE *fp) {       // Write info to output files
  fprintf(fp,"AllocClass: %s \n",_classid);
  const char *name;
  for(_regDefs.reset(); (name = _regDefs.iter()) != NULL;) {
    ((RegDef*)_regDef[name])->output(fp);
  }
  fprintf(fp,"--- done with entries for alloc_class %s\n\n",_classid);
}

//==============================Frame Handling=================================
//------------------------------FrameForm--------------------------------------
FrameForm::FrameForm() {
  _frame_pointer = NULL;
  _c_frame_pointer = NULL;
  _alignment = NULL;
  _return_addr = NULL;
  _c_return_addr = NULL;
  _varargs_C_out_slots_killed = NULL;
  _return_value = NULL;
  _c_return_value = NULL;
  _interpreter_frame_pointer_reg = NULL;
}

FrameForm::~FrameForm() {
}

void FrameForm::dump() {
  output(stderr);
}

void FrameForm::output(FILE *fp) {           // Write info to output files
  fprintf(fp,"\nFrame:\n");
}

//==============================Scheduling=====================================
//------------------------------PipelineForm-----------------------------------
PipelineForm::PipelineForm()
  :  _reslist               ()
  ,  _resdict               (cmpstr, hashstr, Form::arena)
  ,  _classdict             (cmpstr, hashstr, Form::arena)
  ,  _rescount              (0)
  ,  _maxcycleused          (0)
  ,  _stages                ()
  ,  _stagecnt              (0)
  ,  _classlist             ()
  ,  _classcnt              (0)
  ,  _noplist               ()
  ,  _nopcnt                (0)
  ,  _variableSizeInstrs    (false)
  ,  _branchHasDelaySlot    (false)
  ,  _maxInstrsPerBundle    (0)
  ,  _maxBundlesPerCycle    (1)
  ,  _instrUnitSize         (0)
  ,  _bundleUnitSize        (0)
  ,  _instrFetchUnitSize    (0)
  ,  _instrFetchUnits       (0) {
}
PipelineForm::~PipelineForm() {
}

void PipelineForm::dump() {
  output(stderr);
}

void PipelineForm::output(FILE *fp) {           // Write info to output files
  const char *res;
  const char *stage;
  const char *cls;
  const char *nop;
  int count = 0;

  fprintf(fp,"\nPipeline:");
  if (_variableSizeInstrs)
    if (_instrUnitSize > 0)
      fprintf(fp," variable-sized instructions in %d byte units", _instrUnitSize);
    else
      fprintf(fp," variable-sized instructions");
  else
    if (_instrUnitSize > 0)
      fprintf(fp," fixed-sized instructions of %d bytes", _instrUnitSize);
    else if (_bundleUnitSize > 0)
      fprintf(fp," fixed-sized bundles of %d bytes", _bundleUnitSize);
    else
      fprintf(fp," fixed-sized instructions");
  if (_branchHasDelaySlot)
    fprintf(fp,", branch has delay slot");
  if (_maxInstrsPerBundle > 0)
    fprintf(fp,", max of %d instruction%s in parallel",
      _maxInstrsPerBundle, _maxInstrsPerBundle > 1 ? "s" : "");
  if (_maxBundlesPerCycle > 0)
    fprintf(fp,", max of %d bundle%s in parallel",
      _maxBundlesPerCycle, _maxBundlesPerCycle > 1 ? "s" : "");
  if (_instrFetchUnitSize > 0 && _instrFetchUnits)
    fprintf(fp, ", fetch %d x % d bytes per cycle", _instrFetchUnits, _instrFetchUnitSize);

  fprintf(fp,"\nResource:");
  for ( _reslist.reset(); (res = _reslist.iter()) != NULL; )
    fprintf(fp," %s(0x%08x)", res, _resdict[res]->is_resource()->mask());
  fprintf(fp,"\n");

  fprintf(fp,"\nDescription:\n");
  for ( _stages.reset(); (stage = _stages.iter()) != NULL; )
    fprintf(fp," %s(%d)", stage, count++);
  fprintf(fp,"\n");

  fprintf(fp,"\nClasses:\n");
  for ( _classlist.reset(); (cls = _classlist.iter()) != NULL; )
    _classdict[cls]->is_pipeclass()->output(fp);

  fprintf(fp,"\nNop Instructions:");
  for ( _noplist.reset(); (nop = _noplist.iter()) != NULL; )
    fprintf(fp, " \"%s\"", nop);
  fprintf(fp,"\n");
}


//------------------------------ResourceForm-----------------------------------
ResourceForm::ResourceForm(unsigned resmask)
: _resmask(resmask) {
}
ResourceForm::~ResourceForm() {
}

ResourceForm  *ResourceForm::is_resource() const {
  return (ResourceForm *)(this);
}

void ResourceForm::dump() {
  output(stderr);
}

void ResourceForm::output(FILE *fp) {          // Write info to output files
  fprintf(fp, "resource: 0x%08x;\n", mask());
}


//------------------------------PipeClassOperandForm----------------------------------

void PipeClassOperandForm::dump() {
  output(stderr);
}

void PipeClassOperandForm::output(FILE *fp) {         // Write info to output files
  fprintf(stderr,"PipeClassOperandForm: %s", _stage);
  fflush(stderr);
  if (_more_instrs > 0)
    fprintf(stderr,"+%d", _more_instrs);
  fprintf(stderr," (%s)\n", _iswrite ? "write" : "read");
  fflush(stderr);
  fprintf(fp,"PipeClassOperandForm: %s", _stage);
  if (_more_instrs > 0)
    fprintf(fp,"+%d", _more_instrs);
  fprintf(fp," (%s)\n", _iswrite ? "write" : "read");
}


//------------------------------PipeClassResourceForm----------------------------------

void PipeClassResourceForm::dump() {
  output(stderr);
}

void PipeClassResourceForm::output(FILE *fp) {         // Write info to output files
  fprintf(fp,"PipeClassResourceForm: %s at stage %s for %d cycles\n",
     _resource, _stage, _cycles);
}


//------------------------------PipeClassForm----------------------------------
PipeClassForm::PipeClassForm(const char *id, int num)
  : _ident(id)
  , _num(num)
  , _localNames(cmpstr, hashstr, Form::arena)
  , _localUsage(cmpstr, hashstr, Form::arena)
  , _has_fixed_latency(0)
  , _fixed_latency(0)
  , _instruction_count(0)
  , _has_multiple_bundles(false)
  , _has_branch_delay_slot(false)
  , _force_serialization(false)
  , _may_have_no_code(false) {
}

PipeClassForm::~PipeClassForm() {
}

PipeClassForm  *PipeClassForm::is_pipeclass() const {
  return (PipeClassForm *)(this);
}

void PipeClassForm::dump() {
  output(stderr);
}

void PipeClassForm::output(FILE *fp) {         // Write info to output files
  fprintf(fp,"PipeClassForm: #%03d", _num);
  if (_ident)
     fprintf(fp," \"%s\":", _ident);
  if (_has_fixed_latency)
     fprintf(fp," latency %d", _fixed_latency);
  if (_force_serialization)
     fprintf(fp, ", force serialization");
  if (_may_have_no_code)
     fprintf(fp, ", may have no code");
  fprintf(fp, ", %d instruction%s\n", InstructionCount(), InstructionCount() != 1 ? "s" : "");
}


//==============================Peephole Optimization==========================
int Peephole::_peephole_counter = 0;
//------------------------------Peephole---------------------------------------
Peephole::Peephole() : _match(NULL), _constraint(NULL), _replace(NULL), _next(NULL) {
  _peephole_number = _peephole_counter++;
}
Peephole::~Peephole() {
}

// Append a peephole rule with the same root instruction
void Peephole::append_peephole(Peephole *next_peephole) {
  if( _next == NULL ) {
    _next = next_peephole;
  } else {
    _next->append_peephole( next_peephole );
  }
}

// Store the components of this peephole rule
void Peephole::add_match(PeepMatch *match) {
  assert( _match == NULL, "fatal()" );
  _match = match;
}

void Peephole::append_constraint(PeepConstraint *next_constraint) {
  if( _constraint == NULL ) {
    _constraint = next_constraint;
  } else {
    _constraint->append( next_constraint );
  }
}

void Peephole::add_replace(PeepReplace *replace) {
  assert( _replace == NULL, "fatal()" );
  _replace = replace;
}

// class Peephole accessor methods are in the declaration.


void Peephole::dump() {
  output(stderr);
}

void Peephole::output(FILE *fp) {         // Write info to output files
  fprintf(fp,"Peephole:\n");
  if( _match != NULL )       _match->output(fp);
  if( _constraint != NULL )  _constraint->output(fp);
  if( _replace != NULL )     _replace->output(fp);
  // Output the next entry
  if( _next ) _next->output(fp);
}

//------------------------------PeepMatch--------------------------------------
PeepMatch::PeepMatch(char *rule) : _max_position(0), _rule(rule) {
}
PeepMatch::~PeepMatch() {
}


// Insert info into the match-rule
void  PeepMatch::add_instruction(int parent, int position, const char *name,
                                 int input) {
  if( position > _max_position ) _max_position = position;

  _parent.addName((char*) (intptr_t) parent);
  _position.addName((char*) (intptr_t) position);
  _instrs.addName(name);
  _input.addName((char*) (intptr_t) input);
}

// Access info about instructions in the peep-match rule
int   PeepMatch::max_position() {
  return _max_position;
}

const char *PeepMatch::instruction_name(int position) {
  return _instrs.name(position);
}

// Iterate through all info on matched instructions
void  PeepMatch::reset() {
  _parent.reset();
  _position.reset();
  _instrs.reset();
  _input.reset();
}

void  PeepMatch::next_instruction(int &parent, int &position, const char* &name, int &input) {
  parent   = (int) (intptr_t) _parent.iter();
  position = (int) (intptr_t) _position.iter();
  name     = _instrs.iter();
  input    = (int) (intptr_t) _input.iter();
}

// 'true' if current position in iteration is a placeholder, not matched.
bool  PeepMatch::is_placeholder() {
  return _instrs.current_is_signal();
}


void PeepMatch::dump() {
  output(stderr);
}

void PeepMatch::output(FILE *fp) {        // Write info to output files
  fprintf(fp,"PeepMatch:\n");
}

//------------------------------PeepConstraint---------------------------------
PeepConstraint::PeepConstraint(int left_inst,  char* left_op, char* relation,
                               int right_inst, char* right_op)
  : _left_inst(left_inst), _left_op(left_op), _relation(relation),
    _right_inst(right_inst), _right_op(right_op), _next(NULL) {}
PeepConstraint::~PeepConstraint() {
}

// Check if constraints use instruction at position
bool PeepConstraint::constrains_instruction(int position) {
  // Check local instruction constraints
  if( _left_inst  == position ) return true;
  if( _right_inst == position ) return true;

  // Check remaining constraints in list
  if( _next == NULL )  return false;
  else                 return _next->constrains_instruction(position);
}

// Add another constraint
void PeepConstraint::append(PeepConstraint *next_constraint) {
  if( _next == NULL ) {
    _next = next_constraint;
  } else {
    _next->append( next_constraint );
  }
}

// Access the next constraint in the list
PeepConstraint *PeepConstraint::next() {
  return _next;
}


void PeepConstraint::dump() {
  output(stderr);
}

void PeepConstraint::output(FILE *fp) {   // Write info to output files
  fprintf(fp,"PeepConstraint:\n");
}

//------------------------------PeepReplace------------------------------------
PeepReplace::PeepReplace(char *rule) : _rule(rule) {
}
PeepReplace::~PeepReplace() {
}

// Add contents of peepreplace
void  PeepReplace::add_instruction(char *root) {
  _instruction.addName(root);
  _operand_inst_num.add_signal();
  _operand_op_name.add_signal();
}
void  PeepReplace::add_operand( int inst_num, char *inst_operand ) {
  _instruction.add_signal();
  _operand_inst_num.addName((char*) (intptr_t) inst_num);
  _operand_op_name.addName(inst_operand);
}

// Access contents of peepreplace
void  PeepReplace::reset() {
  _instruction.reset();
  _operand_inst_num.reset();
  _operand_op_name.reset();
}
void  PeepReplace::next_instruction(const char* &inst){
  inst                     = _instruction.iter();
  int         inst_num     = (int) (intptr_t) _operand_inst_num.iter();
  const char* inst_operand = _operand_op_name.iter();
}
void  PeepReplace::next_operand(int &inst_num, const char* &inst_operand) {
  const char* inst = _instruction.iter();
  inst_num         = (int) (intptr_t) _operand_inst_num.iter();
  inst_operand     = _operand_op_name.iter();
}



void PeepReplace::dump() {
  output(stderr);
}

void PeepReplace::output(FILE *fp) {      // Write info to output files
  fprintf(fp,"PeepReplace:\n");
}
