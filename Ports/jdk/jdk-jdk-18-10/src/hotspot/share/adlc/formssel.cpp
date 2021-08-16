/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

//==============================Instructions===================================
//------------------------------InstructForm-----------------------------------
InstructForm::InstructForm(const char *id, bool ideal_only)
  : _ident(id), _ideal_only(ideal_only),
    _localNames(cmpstr, hashstr, Form::arena),
    _effects(cmpstr, hashstr, Form::arena),
    _is_mach_constant(false),
    _needs_constant_base(false),
    _has_call(false)
{
      _ftype = Form::INS;

      _matrule              = NULL;
      _insencode            = NULL;
      _constant             = NULL;
      _is_postalloc_expand  = false;
      _opcode               = NULL;
      _size                 = NULL;
      _attribs              = NULL;
      _predicate            = NULL;
      _exprule              = NULL;
      _rewrule              = NULL;
      _format               = NULL;
      _peephole             = NULL;
      _ins_pipe             = NULL;
      _uniq_idx             = NULL;
      _num_uniq             = 0;
      _cisc_spill_operand   = Not_cisc_spillable;// Which operand may cisc-spill
      _cisc_spill_alternate = NULL;            // possible cisc replacement
      _cisc_reg_mask_name   = NULL;
      _is_cisc_alternate    = false;
      _is_short_branch      = false;
      _short_branch_form    = NULL;
      _alignment            = 1;
}

InstructForm::InstructForm(const char *id, InstructForm *instr, MatchRule *rule)
  : _ident(id), _ideal_only(false),
    _localNames(instr->_localNames),
    _effects(instr->_effects),
    _is_mach_constant(false),
    _needs_constant_base(false),
    _has_call(false)
{
      _ftype = Form::INS;

      _matrule               = rule;
      _insencode             = instr->_insencode;
      _constant              = instr->_constant;
      _is_postalloc_expand   = instr->_is_postalloc_expand;
      _opcode                = instr->_opcode;
      _size                  = instr->_size;
      _attribs               = instr->_attribs;
      _predicate             = instr->_predicate;
      _exprule               = instr->_exprule;
      _rewrule               = instr->_rewrule;
      _format                = instr->_format;
      _peephole              = instr->_peephole;
      _ins_pipe              = instr->_ins_pipe;
      _uniq_idx              = instr->_uniq_idx;
      _num_uniq              = instr->_num_uniq;
      _cisc_spill_operand    = Not_cisc_spillable; // Which operand may cisc-spill
      _cisc_spill_alternate  = NULL;               // possible cisc replacement
      _cisc_reg_mask_name    = NULL;
      _is_cisc_alternate     = false;
      _is_short_branch       = false;
      _short_branch_form     = NULL;
      _alignment             = 1;
     // Copy parameters
     const char *name;
     instr->_parameters.reset();
     for (; (name = instr->_parameters.iter()) != NULL;)
       _parameters.addName(name);
}

InstructForm::~InstructForm() {
}

InstructForm *InstructForm::is_instruction() const {
  return (InstructForm*)this;
}

bool InstructForm::ideal_only() const {
  return _ideal_only;
}

bool InstructForm::sets_result() const {
  return (_matrule != NULL && _matrule->sets_result());
}

bool InstructForm::needs_projections() {
  _components.reset();
  for( Component *comp; (comp = _components.iter()) != NULL; ) {
    if (comp->isa(Component::KILL)) {
      return true;
    }
  }
  return false;
}


bool InstructForm::has_temps() {
  if (_matrule) {
    // Examine each component to see if it is a TEMP
    _components.reset();
    // Skip the first component, if already handled as (SET dst (...))
    Component *comp = NULL;
    if (sets_result())  comp = _components.iter();
    while ((comp = _components.iter()) != NULL) {
      if (comp->isa(Component::TEMP)) {
        return true;
      }
    }
  }

  return false;
}

uint InstructForm::num_defs_or_kills() {
  uint   defs_or_kills = 0;

  _components.reset();
  for( Component *comp; (comp = _components.iter()) != NULL; ) {
    if( comp->isa(Component::DEF) || comp->isa(Component::KILL) ) {
      ++defs_or_kills;
    }
  }

  return  defs_or_kills;
}

// This instruction has an expand rule?
bool InstructForm::expands() const {
  return ( _exprule != NULL );
}

// This instruction has a late expand rule?
bool InstructForm::postalloc_expands() const {
  return _is_postalloc_expand;
}

// This instruction has a peephole rule?
Peephole *InstructForm::peepholes() const {
  return _peephole;
}

// This instruction has a peephole rule?
void InstructForm::append_peephole(Peephole *peephole) {
  if( _peephole == NULL ) {
    _peephole = peephole;
  } else {
    _peephole->append_peephole(peephole);
  }
}


// ideal opcode enumeration
const char *InstructForm::ideal_Opcode( FormDict &globalNames )  const {
  if( !_matrule ) return "Node"; // Something weird
  // Chain rules do not really have ideal Opcodes; use their source
  // operand ideal Opcode instead.
  if( is_simple_chain_rule(globalNames) ) {
    const char *src = _matrule->_rChild->_opType;
    OperandForm *src_op = globalNames[src]->is_operand();
    assert( src_op, "Not operand class of chain rule" );
    if( !src_op->_matrule ) return "Node";
    return src_op->_matrule->_opType;
  }
  // Operand chain rules do not really have ideal Opcodes
  if( _matrule->is_chain_rule(globalNames) )
    return "Node";
  return strcmp(_matrule->_opType,"Set")
    ? _matrule->_opType
    : _matrule->_rChild->_opType;
}

// Recursive check on all operands' match rules in my match rule
bool InstructForm::is_pinned(FormDict &globals) {
  if ( ! _matrule)  return false;

  int  index   = 0;
  if (_matrule->find_type("Goto",          index)) return true;
  if (_matrule->find_type("If",            index)) return true;
  if (_matrule->find_type("CountedLoopEnd",index)) return true;
  if (_matrule->find_type("Return",        index)) return true;
  if (_matrule->find_type("Rethrow",       index)) return true;
  if (_matrule->find_type("TailCall",      index)) return true;
  if (_matrule->find_type("TailJump",      index)) return true;
  if (_matrule->find_type("Halt",          index)) return true;
  if (_matrule->find_type("Jump",          index)) return true;

  return is_parm(globals);
}

// Recursive check on all operands' match rules in my match rule
bool InstructForm::is_projection(FormDict &globals) {
  if ( ! _matrule)  return false;

  int  index   = 0;
  if (_matrule->find_type("Goto",    index)) return true;
  if (_matrule->find_type("Return",  index)) return true;
  if (_matrule->find_type("Rethrow", index)) return true;
  if (_matrule->find_type("TailCall",index)) return true;
  if (_matrule->find_type("TailJump",index)) return true;
  if (_matrule->find_type("Halt",    index)) return true;

  return false;
}

// Recursive check on all operands' match rules in my match rule
bool InstructForm::is_parm(FormDict &globals) {
  if ( ! _matrule)  return false;

  int  index   = 0;
  if (_matrule->find_type("Parm",index)) return true;

  return false;
}

bool InstructForm::is_ideal_negD() const {
  return (_matrule && _matrule->_rChild && strcmp(_matrule->_rChild->_opType, "NegD") == 0);
}

// Return 'true' if this instruction matches an ideal 'Copy*' node
int InstructForm::is_ideal_copy() const {
  return _matrule ? _matrule->is_ideal_copy() : 0;
}

// Return 'true' if this instruction is too complex to rematerialize.
int InstructForm::is_expensive() const {
  // We can prove it is cheap if it has an empty encoding.
  // This helps with platform-specific nops like ThreadLocal and RoundFloat.
  if (is_empty_encoding())
    return 0;

  if (is_tls_instruction())
    return 1;

  if (_matrule == NULL)  return 0;

  return _matrule->is_expensive();
}

// Has an empty encoding if _size is a constant zero or there
// are no ins_encode tokens.
int InstructForm::is_empty_encoding() const {
  if (_insencode != NULL) {
    _insencode->reset();
    if (_insencode->encode_class_iter() == NULL) {
      return 1;
    }
  }
  if (_size != NULL && strcmp(_size, "0") == 0) {
    return 1;
  }
  return 0;
}

int InstructForm::is_tls_instruction() const {
  if (_ident != NULL &&
      ( ! strcmp( _ident,"tlsLoadP") ||
        ! strncmp(_ident,"tlsLoadP_",9)) ) {
    return 1;
  }

  if (_matrule != NULL && _insencode != NULL) {
    const char* opType = _matrule->_opType;
    if (strcmp(opType, "Set")==0)
      opType = _matrule->_rChild->_opType;
    if (strcmp(opType,"ThreadLocal")==0) {
      fprintf(stderr, "Warning: ThreadLocal instruction %s should be named 'tlsLoadP_*'\n",
              (_ident == NULL ? "NULL" : _ident));
      return 1;
    }
  }

  return 0;
}


// Return 'true' if this instruction matches an ideal 'If' node
bool InstructForm::is_ideal_if() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_if();
}

// Return 'true' if this instruction matches an ideal 'FastLock' node
bool InstructForm::is_ideal_fastlock() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_fastlock();
}

// Return 'true' if this instruction matches an ideal 'MemBarXXX' node
bool InstructForm::is_ideal_membar() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_membar();
}

// Return 'true' if this instruction matches an ideal 'LoadPC' node
bool InstructForm::is_ideal_loadPC() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_loadPC();
}

// Return 'true' if this instruction matches an ideal 'Box' node
bool InstructForm::is_ideal_box() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_box();
}

// Return 'true' if this instruction matches an ideal 'Goto' node
bool InstructForm::is_ideal_goto() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_goto();
}

// Return 'true' if this instruction matches an ideal 'Jump' node
bool InstructForm::is_ideal_jump() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_jump();
}

// Return 'true' if instruction matches ideal 'If' | 'Goto' | 'CountedLoopEnd'
bool InstructForm::is_ideal_branch() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_if() || _matrule->is_ideal_goto();
}


// Return 'true' if this instruction matches an ideal 'Return' node
bool InstructForm::is_ideal_return() const {
  if( _matrule == NULL ) return false;

  // Check MatchRule to see if the first entry is the ideal "Return" node
  int  index   = 0;
  if (_matrule->find_type("Return",index)) return true;
  if (_matrule->find_type("Rethrow",index)) return true;
  if (_matrule->find_type("TailCall",index)) return true;
  if (_matrule->find_type("TailJump",index)) return true;

  return false;
}

// Return 'true' if this instruction matches an ideal 'Halt' node
bool InstructForm::is_ideal_halt() const {
  int  index   = 0;
  return _matrule && _matrule->find_type("Halt",index);
}

// Return 'true' if this instruction matches an ideal 'SafePoint' node
bool InstructForm::is_ideal_safepoint() const {
  int  index   = 0;
  return _matrule && _matrule->find_type("SafePoint",index);
}

// Return 'true' if this instruction matches an ideal 'Nop' node
bool InstructForm::is_ideal_nop() const {
  return _ident && _ident[0] == 'N' && _ident[1] == 'o' && _ident[2] == 'p' && _ident[3] == '_';
}

bool InstructForm::is_ideal_control() const {
  if ( ! _matrule)  return false;

  return is_ideal_return() || is_ideal_branch() || _matrule->is_ideal_jump() || is_ideal_halt();
}

// Return 'true' if this instruction matches an ideal 'Call' node
Form::CallType InstructForm::is_ideal_call() const {
  if( _matrule == NULL ) return Form::invalid_type;

  // Check MatchRule to see if the first entry is the ideal "Call" node
  int  idx   = 0;
  if(_matrule->find_type("CallStaticJava",idx))   return Form::JAVA_STATIC;
  idx = 0;
  if(_matrule->find_type("Lock",idx))             return Form::JAVA_STATIC;
  idx = 0;
  if(_matrule->find_type("Unlock",idx))           return Form::JAVA_STATIC;
  idx = 0;
  if(_matrule->find_type("CallDynamicJava",idx))  return Form::JAVA_DYNAMIC;
  idx = 0;
  if(_matrule->find_type("CallRuntime",idx))      return Form::JAVA_RUNTIME;
  idx = 0;
  if(_matrule->find_type("CallLeaf",idx))         return Form::JAVA_LEAF;
  idx = 0;
  if(_matrule->find_type("CallLeafNoFP",idx))     return Form::JAVA_LEAF;
  idx = 0;
  if(_matrule->find_type("CallLeafVector",idx))   return Form::JAVA_LEAF;
  idx = 0;
  if(_matrule->find_type("CallNative",idx))       return Form::JAVA_NATIVE;
  idx = 0;

  return Form::invalid_type;
}

// Return 'true' if this instruction matches an ideal 'Load?' node
Form::DataType InstructForm::is_ideal_load() const {
  if( _matrule == NULL ) return Form::none;

  return  _matrule->is_ideal_load();
}

// Return 'true' if this instruction matches an ideal 'LoadKlass' node
bool InstructForm::skip_antidep_check() const {
  if( _matrule == NULL ) return false;

  return  _matrule->skip_antidep_check();
}

// Return 'true' if this instruction matches an ideal 'Load?' node
Form::DataType InstructForm::is_ideal_store() const {
  if( _matrule == NULL ) return Form::none;

  return  _matrule->is_ideal_store();
}

// Return 'true' if this instruction matches an ideal vector node
bool InstructForm::is_vector() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_vector();
}


// Return the input register that must match the output register
// If this is not required, return 0
uint InstructForm::two_address(FormDict &globals) {
  uint  matching_input = 0;
  if(_components.count() == 0) return 0;

  _components.reset();
  Component *comp = _components.iter();
  // Check if there is a DEF
  if( comp->isa(Component::DEF) ) {
    // Check that this is a register
    const char  *def_type = comp->_type;
    const Form  *form     = globals[def_type];
    OperandForm *op       = form->is_operand();
    if( op ) {
      if( op->constrained_reg_class() != NULL &&
          op->interface_type(globals) == Form::register_interface ) {
        // Remember the local name for equality test later
        const char *def_name = comp->_name;
        // Check if a component has the same name and is a USE
        do {
          if( comp->isa(Component::USE) && strcmp(comp->_name,def_name)==0 ) {
            return operand_position_format(def_name);
          }
        } while( (comp = _components.iter()) != NULL);
      }
    }
  }

  return 0;
}


// when chaining a constant to an instruction, returns 'true' and sets opType
Form::DataType InstructForm::is_chain_of_constant(FormDict &globals) {
  const char *dummy  = NULL;
  const char *dummy2 = NULL;
  return is_chain_of_constant(globals, dummy, dummy2);
}
Form::DataType InstructForm::is_chain_of_constant(FormDict &globals,
                const char * &opTypeParam) {
  const char *result = NULL;

  return is_chain_of_constant(globals, opTypeParam, result);
}

Form::DataType InstructForm::is_chain_of_constant(FormDict &globals,
                const char * &opTypeParam, const char * &resultParam) {
  Form::DataType  data_type = Form::none;
  if ( ! _matrule)  return data_type;

  // !!!!!
  // The source of the chain rule is 'position = 1'
  uint         position = 1;
  const char  *result   = NULL;
  const char  *name     = NULL;
  const char  *opType   = NULL;
  // Here base_operand is looking for an ideal type to be returned (opType).
  if ( _matrule->is_chain_rule(globals)
       && _matrule->base_operand(position, globals, result, name, opType) ) {
    data_type = ideal_to_const_type(opType);

    // if it isn't an ideal constant type, just return
    if ( data_type == Form::none ) return data_type;

    // Ideal constant types also adjust the opType parameter.
    resultParam = result;
    opTypeParam = opType;
    return data_type;
  }

  return data_type;
}

// Check if a simple chain rule
bool InstructForm::is_simple_chain_rule(FormDict &globals) const {
  if( _matrule && _matrule->sets_result()
      && _matrule->_rChild->_lChild == NULL
      && globals[_matrule->_rChild->_opType]
      && globals[_matrule->_rChild->_opType]->is_opclass() ) {
    return true;
  }
  return false;
}

// check for structural rematerialization
bool InstructForm::rematerialize(FormDict &globals, RegisterForm *registers ) {
  bool   rematerialize = false;

  Form::DataType data_type = is_chain_of_constant(globals);
  if( data_type != Form::none )
    rematerialize = true;

  // Constants
  if( _components.count() == 1 && _components[0]->is(Component::USE_DEF) )
    rematerialize = true;

  // Pseudo-constants (values easily available to the runtime)
  if (is_empty_encoding() && is_tls_instruction())
    rematerialize = true;

  // 1-input, 1-output, such as copies or increments.
  if( _components.count() == 2 &&
      _components[0]->is(Component::DEF) &&
      _components[1]->isa(Component::USE) )
    rematerialize = true;

  // Check for an ideal 'Load?' and eliminate rematerialize option
  if ( is_ideal_load() != Form::none || // Ideal load?  Do not rematerialize
       is_ideal_copy() != Form::none || // Ideal copy?  Do not rematerialize
       is_expensive()  != Form::none) { // Expensive?   Do not rematerialize
    rematerialize = false;
  }

  // Always rematerialize the flags.  They are more expensive to save &
  // restore than to recompute (and possibly spill the compare's inputs).
  if( _components.count() >= 1 ) {
    Component *c = _components[0];
    const Form *form = globals[c->_type];
    OperandForm *opform = form->is_operand();
    if( opform ) {
      // Avoid the special stack_slots register classes
      const char *rc_name = opform->constrained_reg_class();
      if( rc_name ) {
        if( strcmp(rc_name,"stack_slots") ) {
          // Check for ideal_type of RegFlags
          const char *type = opform->ideal_type( globals, registers );
          if( (type != NULL) && !strcmp(type, "RegFlags") )
            rematerialize = true;
        } else
          rematerialize = false; // Do not rematerialize things target stk
      }
    }
  }

  return rematerialize;
}

// loads from memory, so must check for anti-dependence
bool InstructForm::needs_anti_dependence_check(FormDict &globals) const {
  if ( skip_antidep_check() ) return false;

  // Machine independent loads must be checked for anti-dependences
  if( is_ideal_load() != Form::none )  return true;

  // !!!!! !!!!! !!!!!
  // TEMPORARY
  // if( is_simple_chain_rule(globals) )  return false;

  // String.(compareTo/equals/indexOf) and Arrays.equals use many memorys edges,
  // but writes none
  if( _matrule && _matrule->_rChild &&
      ( strcmp(_matrule->_rChild->_opType,"StrComp"    )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrEquals"  )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrIndexOf" )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrIndexOfChar" )==0 ||
        strcmp(_matrule->_rChild->_opType,"HasNegatives" )==0 ||
        strcmp(_matrule->_rChild->_opType,"AryEq"      )==0 ))
    return true;

  // Check if instruction has a USE of a memory operand class, but no defs
  bool USE_of_memory  = false;
  bool DEF_of_memory  = false;
  Component     *comp = NULL;
  ComponentList &components = (ComponentList &)_components;

  components.reset();
  while( (comp = components.iter()) != NULL ) {
    const Form  *form = globals[comp->_type];
    if( !form ) continue;
    OpClassForm *op   = form->is_opclass();
    if( !op ) continue;
    if( form->interface_type(globals) == Form::memory_interface ) {
      if( comp->isa(Component::USE) ) USE_of_memory = true;
      if( comp->isa(Component::DEF) ) {
        OperandForm *oper = form->is_operand();
        if( oper && oper->is_user_name_for_sReg() ) {
          // Stack slots are unaliased memory handled by allocator
          oper = oper;  // debug stopping point !!!!!
        } else {
          DEF_of_memory = true;
        }
      }
    }
  }
  return (USE_of_memory && !DEF_of_memory);
}


int InstructForm::memory_operand(FormDict &globals) const {
  // Machine independent loads must be checked for anti-dependences
  // Check if instruction has a USE of a memory operand class, or a def.
  int USE_of_memory  = 0;
  int DEF_of_memory  = 0;
  const char*    last_memory_DEF = NULL; // to test DEF/USE pairing in asserts
  const char*    last_memory_USE = NULL;
  Component     *unique          = NULL;
  Component     *comp            = NULL;
  ComponentList &components      = (ComponentList &)_components;

  components.reset();
  while( (comp = components.iter()) != NULL ) {
    const Form  *form = globals[comp->_type];
    if( !form ) continue;
    OpClassForm *op   = form->is_opclass();
    if( !op ) continue;
    if( op->stack_slots_only(globals) )  continue;
    if( form->interface_type(globals) == Form::memory_interface ) {
      if( comp->isa(Component::DEF) ) {
        last_memory_DEF = comp->_name;
        DEF_of_memory++;
        unique = comp;
      } else if( comp->isa(Component::USE) ) {
        if( last_memory_DEF != NULL ) {
          assert(0 == strcmp(last_memory_DEF, comp->_name), "every memory DEF is followed by a USE of the same name");
          last_memory_DEF = NULL;
        }
        // Handles same memory being used multiple times in the case of BMI1 instructions.
        if (last_memory_USE != NULL) {
          if (strcmp(comp->_name, last_memory_USE) != 0) {
            USE_of_memory++;
          }
        } else {
          USE_of_memory++;
        }
        last_memory_USE = comp->_name;

        if (DEF_of_memory == 0)  // defs take precedence
          unique = comp;
      } else {
        assert(last_memory_DEF == NULL, "unpaired memory DEF");
      }
    }
  }
  assert(last_memory_DEF == NULL, "unpaired memory DEF");
  assert(USE_of_memory >= DEF_of_memory, "unpaired memory DEF");
  USE_of_memory -= DEF_of_memory;   // treat paired DEF/USE as one occurrence
  if( (USE_of_memory + DEF_of_memory) > 0 ) {
    if( is_simple_chain_rule(globals) ) {
      //fprintf(stderr, "Warning: chain rule is not really a memory user.\n");
      //((InstructForm*)this)->dump();
      // Preceding code prints nothing on sparc and these insns on intel:
      // leaP8 leaP32 leaPIdxOff leaPIdxScale leaPIdxScaleOff leaP8 leaP32
      // leaPIdxOff leaPIdxScale leaPIdxScaleOff
      return NO_MEMORY_OPERAND;
    }

    if( DEF_of_memory == 1 ) {
      assert(unique != NULL, "");
      if( USE_of_memory == 0 ) {
        // unique def, no uses
      } else {
        // // unique def, some uses
        // // must return bottom unless all uses match def
        // unique = NULL;
#ifdef S390
        // This case is important for move instructions on s390x.
        // On other platforms (e.g. x86), all uses always match the def.
        unique = NULL;
#endif
      }
    } else if( DEF_of_memory > 0 ) {
      // multiple defs, don't care about uses
      unique = NULL;
    } else if( USE_of_memory == 1) {
      // unique use, no defs
      assert(unique != NULL, "");
    } else if( USE_of_memory > 0 ) {
      // multiple uses, no defs
      unique = NULL;
    } else {
      assert(false, "bad case analysis");
    }
    // process the unique DEF or USE, if there is one
    if( unique == NULL ) {
      return MANY_MEMORY_OPERANDS;
    } else {
      int pos = components.operand_position(unique->_name);
      if( unique->isa(Component::DEF) ) {
        pos += 1;                // get corresponding USE from DEF
      }
      assert(pos >= 1, "I was just looking at it!");
      return pos;
    }
  }

  // missed the memory op??
  if( true ) {  // %%% should not be necessary
    if( is_ideal_store() != Form::none ) {
      fprintf(stderr, "Warning: cannot find memory opnd in instr.\n");
      ((InstructForm*)this)->dump();
      // pretend it has multiple defs and uses
      return MANY_MEMORY_OPERANDS;
    }
    if( is_ideal_load()  != Form::none ) {
      fprintf(stderr, "Warning: cannot find memory opnd in instr.\n");
      ((InstructForm*)this)->dump();
      // pretend it has multiple uses and no defs
      return MANY_MEMORY_OPERANDS;
    }
  }

  return NO_MEMORY_OPERAND;
}

// This instruction captures the machine-independent bottom_type
// Expected use is for pointer vs oop determination for LoadP
bool InstructForm::captures_bottom_type(FormDict &globals) const {
  if (_matrule && _matrule->_rChild &&
      (!strcmp(_matrule->_rChild->_opType,"CastPP")       ||  // new result type
       !strcmp(_matrule->_rChild->_opType,"CastDD")       ||
       !strcmp(_matrule->_rChild->_opType,"CastFF")       ||
       !strcmp(_matrule->_rChild->_opType,"CastII")       ||
       !strcmp(_matrule->_rChild->_opType,"CastLL")       ||
       !strcmp(_matrule->_rChild->_opType,"CastVV")       ||
       !strcmp(_matrule->_rChild->_opType,"CastX2P")      ||  // new result type
       !strcmp(_matrule->_rChild->_opType,"DecodeN")      ||
       !strcmp(_matrule->_rChild->_opType,"EncodeP")      ||
       !strcmp(_matrule->_rChild->_opType,"DecodeNKlass") ||
       !strcmp(_matrule->_rChild->_opType,"EncodePKlass") ||
       !strcmp(_matrule->_rChild->_opType,"LoadN")        ||
       !strcmp(_matrule->_rChild->_opType,"LoadNKlass")   ||
       !strcmp(_matrule->_rChild->_opType,"CreateEx")     ||  // type of exception
       !strcmp(_matrule->_rChild->_opType,"CheckCastPP")  ||
       !strcmp(_matrule->_rChild->_opType,"GetAndSetP")   ||
       !strcmp(_matrule->_rChild->_opType,"GetAndSetN")   ||
       !strcmp(_matrule->_rChild->_opType,"RotateLeft")   ||
       !strcmp(_matrule->_rChild->_opType,"RotateRight")   ||
#if INCLUDE_SHENANDOAHGC
       !strcmp(_matrule->_rChild->_opType,"ShenandoahCompareAndExchangeP") ||
       !strcmp(_matrule->_rChild->_opType,"ShenandoahCompareAndExchangeN") ||
#endif
       !strcmp(_matrule->_rChild->_opType,"StrInflatedCopy") ||
       !strcmp(_matrule->_rChild->_opType,"VectorCmpMasked")||
       !strcmp(_matrule->_rChild->_opType,"VectorMaskGen")||
       !strcmp(_matrule->_rChild->_opType,"CompareAndExchangeP") ||
       !strcmp(_matrule->_rChild->_opType,"CompareAndExchangeN"))) return true;
  else if ( is_ideal_load() == Form::idealP )                return true;
  else if ( is_ideal_store() != Form::none  )                return true;

  if (needs_base_oop_edge(globals)) return true;

  if (is_vector()) return true;
  if (is_mach_constant()) return true;

  return  false;
}


// Access instr_cost attribute or return NULL.
const char* InstructForm::cost() {
  for (Attribute* cur = _attribs; cur != NULL; cur = (Attribute*)cur->_next) {
    if( strcmp(cur->_ident,AttributeForm::_ins_cost) == 0 ) {
      return cur->_val;
    }
  }
  return NULL;
}

// Return count of top-level operands.
uint InstructForm::num_opnds() {
  int  num_opnds = _components.num_operands();

  // Need special handling for matching some ideal nodes
  // i.e. Matching a return node
  /*
  if( _matrule ) {
    if( strcmp(_matrule->_opType,"Return"   )==0 ||
        strcmp(_matrule->_opType,"Halt"     )==0 )
      return 3;
  }
    */
  return num_opnds;
}

const char* InstructForm::opnd_ident(int idx) {
  return _components.at(idx)->_name;
}

const char* InstructForm::unique_opnd_ident(uint idx) {
  uint i;
  for (i = 1; i < num_opnds(); ++i) {
    if (unique_opnds_idx(i) == idx) {
      break;
    }
  }
  return (_components.at(i) != NULL) ? _components.at(i)->_name : "";
}

// Return count of unmatched operands.
uint InstructForm::num_post_match_opnds() {
  uint  num_post_match_opnds = _components.count();
  uint  num_match_opnds = _components.match_count();
  num_post_match_opnds = num_post_match_opnds - num_match_opnds;

  return num_post_match_opnds;
}

// Return the number of leaves below this complex operand
uint InstructForm::num_consts(FormDict &globals) const {
  if ( ! _matrule) return 0;

  // This is a recursive invocation on all operands in the matchrule
  return _matrule->num_consts(globals);
}

// Constants in match rule with specified type
uint InstructForm::num_consts(FormDict &globals, Form::DataType type) const {
  if ( ! _matrule) return 0;

  // This is a recursive invocation on all operands in the matchrule
  return _matrule->num_consts(globals, type);
}


// Return the register class associated with 'leaf'.
const char *InstructForm::out_reg_class(FormDict &globals) {
  assert( false, "InstructForm::out_reg_class(FormDict &globals); Not Implemented");

  return NULL;
}



// Lookup the starting position of inputs we are interested in wrt. ideal nodes
uint InstructForm::oper_input_base(FormDict &globals) {
  if( !_matrule ) return 1;     // Skip control for most nodes

  // Need special handling for matching some ideal nodes
  // i.e. Matching a return node
  if( strcmp(_matrule->_opType,"Return"    )==0 ||
      strcmp(_matrule->_opType,"Rethrow"   )==0 ||
      strcmp(_matrule->_opType,"TailCall"  )==0 ||
      strcmp(_matrule->_opType,"TailJump"  )==0 ||
      strcmp(_matrule->_opType,"SafePoint" )==0 ||
      strcmp(_matrule->_opType,"Halt"      )==0 )
    return AdlcVMDeps::Parms;   // Skip the machine-state edges

  if( _matrule->_rChild &&
      ( strcmp(_matrule->_rChild->_opType,"AryEq"     )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrComp"   )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrEquals" )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrInflatedCopy"   )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrCompressedCopy" )==0 ||
        strcmp(_matrule->_rChild->_opType,"StrIndexOf")==0 ||
        strcmp(_matrule->_rChild->_opType,"StrIndexOfChar")==0 ||
        strcmp(_matrule->_rChild->_opType,"HasNegatives")==0 ||
        strcmp(_matrule->_rChild->_opType,"EncodeISOArray")==0)) {
        // String.(compareTo/equals/indexOf) and Arrays.equals
        // and sun.nio.cs.iso8859_1$Encoder.EncodeISOArray
        // take 1 control and 1 memory edges.
        // Also String.(compressedCopy/inflatedCopy).
    return 2;
  }

  // Check for handling of 'Memory' input/edge in the ideal world.
  // The AD file writer is shielded from knowledge of these edges.
  int base = 1;                 // Skip control
  base += _matrule->needs_ideal_memory_edge(globals);

  // Also skip the base-oop value for uses of derived oops.
  // The AD file writer is shielded from knowledge of these edges.
  base += needs_base_oop_edge(globals);

  return base;
}

// This function determines the order of the MachOper in _opnds[]
// by writing the operand names into the _components list.
//
// Implementation does not modify state of internal structures
void InstructForm::build_components() {
  // Add top-level operands to the components
  if (_matrule)  _matrule->append_components(_localNames, _components);

  // Add parameters that "do not appear in match rule".
  bool has_temp = false;
  const char *name;
  const char *kill_name = NULL;
  for (_parameters.reset(); (name = _parameters.iter()) != NULL;) {
    OpClassForm *opForm = _localNames[name]->is_opclass();
    assert(opForm != NULL, "sanity");

    Effect* e = NULL;
    {
      const Form* form = _effects[name];
      e = form ? form->is_effect() : NULL;
    }

    if (e != NULL) {
      has_temp |= e->is(Component::TEMP);

      // KILLs must be declared after any TEMPs because TEMPs are real
      // uses so their operand numbering must directly follow the real
      // inputs from the match rule.  Fixing the numbering seems
      // complex so simply enforce the restriction during parse.
      if (kill_name != NULL &&
          e->isa(Component::TEMP) && !e->isa(Component::DEF)) {
        OpClassForm* kill = _localNames[kill_name]->is_opclass();
        assert(kill != NULL, "sanity");
        globalAD->syntax_err(_linenum, "%s: %s %s must be at the end of the argument list\n",
                             _ident, kill->_ident, kill_name);
      } else if (e->isa(Component::KILL) && !e->isa(Component::USE)) {
        kill_name = name;
      }
    }

    const Component *component  = _components.search(name);
    if ( component  == NULL ) {
      if (e) {
        _components.insert(name, opForm->_ident, e->_use_def, false);
        component = _components.search(name);
        if (component->isa(Component::USE) && !component->isa(Component::TEMP) && _matrule) {
          const Form *form = globalAD->globalNames()[component->_type];
          assert( form, "component type must be a defined form");
          OperandForm *op   = form->is_operand();
          if (op->_interface && op->_interface->is_RegInterface()) {
            globalAD->syntax_err(_linenum, "%s: illegal USE of non-input: %s %s\n",
                                 _ident, opForm->_ident, name);
          }
        }
      } else {
        // This would be a nice warning but it triggers in a few places in a benign way
        // if (_matrule != NULL && !expands()) {
        //   globalAD->syntax_err(_linenum, "%s: %s %s not mentioned in effect or match rule\n",
        //                        _ident, opForm->_ident, name);
        // }
        _components.insert(name, opForm->_ident, Component::INVALID, false);
      }
    }
    else if (e) {
      // Component was found in the list
      // Check if there is a new effect that requires an extra component.
      // This happens when adding 'USE' to a component that is not yet one.
      if ((!component->isa( Component::USE) && ((e->_use_def & Component::USE) != 0))) {
        if (component->isa(Component::USE) && _matrule) {
          const Form *form = globalAD->globalNames()[component->_type];
          assert( form, "component type must be a defined form");
          OperandForm *op   = form->is_operand();
          if (op->_interface && op->_interface->is_RegInterface()) {
            globalAD->syntax_err(_linenum, "%s: illegal USE of non-input: %s %s\n",
                                 _ident, opForm->_ident, name);
          }
        }
        _components.insert(name, opForm->_ident, e->_use_def, false);
      } else {
        Component  *comp = (Component*)component;
        comp->promote_use_def_info(e->_use_def);
      }
      // Component positions are zero based.
      int  pos  = _components.operand_position(name);
      assert( ! (component->isa(Component::DEF) && (pos >= 1)),
              "Component::DEF can only occur in the first position");
    }
  }

  // Resolving the interactions between expand rules and TEMPs would
  // be complex so simply disallow it.
  if (_matrule == NULL && has_temp) {
    globalAD->syntax_err(_linenum, "%s: TEMPs without match rule isn't supported\n", _ident);
  }

  return;
}

// Return zero-based position in component list;  -1 if not in list.
int   InstructForm::operand_position(const char *name, int usedef) {
  return unique_opnds_idx(_components.operand_position(name, usedef, this));
}

int   InstructForm::operand_position_format(const char *name) {
  return unique_opnds_idx(_components.operand_position_format(name, this));
}

// Return zero-based position in component list; -1 if not in list.
int   InstructForm::label_position() {
  return unique_opnds_idx(_components.label_position());
}

int   InstructForm::method_position() {
  return unique_opnds_idx(_components.method_position());
}

// Return number of relocation entries needed for this instruction.
uint  InstructForm::reloc(FormDict &globals) {
  uint reloc_entries  = 0;
  // Check for "Call" nodes
  if ( is_ideal_call() )      ++reloc_entries;
  if ( is_ideal_return() )    ++reloc_entries;
  if ( is_ideal_safepoint() ) ++reloc_entries;


  // Check if operands MAYBE oop pointers, by checking for ConP elements
  // Proceed through the leaves of the match-tree and check for ConPs
  if ( _matrule != NULL ) {
    uint         position = 0;
    const char  *result   = NULL;
    const char  *name     = NULL;
    const char  *opType   = NULL;
    while (_matrule->base_operand(position, globals, result, name, opType)) {
      if ( strcmp(opType,"ConP") == 0 ) {
        ++reloc_entries;
      }
      ++position;
    }
  }

  // Above is only a conservative estimate
  // because it did not check contents of operand classes.
  // !!!!! !!!!!
  // Add 1 to reloc info for each operand class in the component list.
  Component  *comp;
  _components.reset();
  while ( (comp = _components.iter()) != NULL ) {
    const Form        *form = globals[comp->_type];
    assert( form, "Did not find component's type in global names");
    const OpClassForm *opc  = form->is_opclass();
    const OperandForm *oper = form->is_operand();
    if ( opc && (oper == NULL) ) {
      ++reloc_entries;
    } else if ( oper ) {
      // floats and doubles loaded out of method's constant pool require reloc info
      Form::DataType type = oper->is_base_constant(globals);
      if ( (type == Form::idealF) || (type == Form::idealD) ) {
        ++reloc_entries;
      }
    }
  }

  // Float and Double constants may come from the CodeBuffer table
  // and require relocatable addresses for access
  // !!!!!
  // Check for any component being an immediate float or double.
  Form::DataType data_type = is_chain_of_constant(globals);
  if( data_type==idealD || data_type==idealF ) {
    reloc_entries++;
  }

  return reloc_entries;
}

// Utility function defined in archDesc.cpp
extern bool is_def(int usedef);

// Return the result of reducing an instruction
const char *InstructForm::reduce_result() {
  const char* result = "Universe";  // default
  _components.reset();
  Component *comp = _components.iter();
  if (comp != NULL && comp->isa(Component::DEF)) {
    result = comp->_type;
    // Override this if the rule is a store operation:
    if (_matrule && _matrule->_rChild &&
        is_store_to_memory(_matrule->_rChild->_opType))
      result = "Universe";
  }
  return result;
}

// Return the name of the operand on the right hand side of the binary match
// Return NULL if there is no right hand side
const char *InstructForm::reduce_right(FormDict &globals)  const {
  if( _matrule == NULL ) return NULL;
  return  _matrule->reduce_right(globals);
}

// Similar for left
const char *InstructForm::reduce_left(FormDict &globals)   const {
  if( _matrule == NULL ) return NULL;
  return  _matrule->reduce_left(globals);
}


// Base class for this instruction, MachNode except for calls
const char *InstructForm::mach_base_class(FormDict &globals)  const {
  if( is_ideal_call() == Form::JAVA_STATIC ) {
    return "MachCallStaticJavaNode";
  }
  else if( is_ideal_call() == Form::JAVA_DYNAMIC ) {
    return "MachCallDynamicJavaNode";
  }
  else if( is_ideal_call() == Form::JAVA_RUNTIME ) {
    return "MachCallRuntimeNode";
  }
  else if( is_ideal_call() == Form::JAVA_LEAF ) {
    return "MachCallLeafNode";
  }
  else if( is_ideal_call() == Form::JAVA_NATIVE ) {
    return "MachCallNativeNode";
  }
  else if (is_ideal_return()) {
    return "MachReturnNode";
  }
  else if (is_ideal_halt()) {
    return "MachHaltNode";
  }
  else if (is_ideal_safepoint()) {
    return "MachSafePointNode";
  }
  else if (is_ideal_if()) {
    return "MachIfNode";
  }
  else if (is_ideal_goto()) {
    return "MachGotoNode";
  }
  else if (is_ideal_fastlock()) {
    return "MachFastLockNode";
  }
  else if (is_ideal_nop()) {
    return "MachNopNode";
  }
  else if( is_ideal_membar()) {
    return "MachMemBarNode";
  }
  else if (is_ideal_jump()) {
    return "MachJumpNode";
  }
  else if (is_mach_constant()) {
    return "MachConstantNode";
  }
  else if (captures_bottom_type(globals)) {
    return "MachTypeNode";
  } else {
    return "MachNode";
  }
  assert( false, "ShouldNotReachHere()");
  return NULL;
}

// Compare the instruction predicates for textual equality
bool equivalent_predicates( const InstructForm *instr1, const InstructForm *instr2 ) {
  const Predicate *pred1  = instr1->_predicate;
  const Predicate *pred2  = instr2->_predicate;
  if( pred1 == NULL && pred2 == NULL ) {
    // no predicates means they are identical
    return true;
  }
  if( pred1 != NULL && pred2 != NULL ) {
    // compare the predicates
    if (ADLParser::equivalent_expressions(pred1->_pred, pred2->_pred)) {
      return true;
    }
  }

  return false;
}

// Check if this instruction can cisc-spill to 'alternate'
bool InstructForm::cisc_spills_to(ArchDesc &AD, InstructForm *instr) {
  assert( _matrule != NULL && instr->_matrule != NULL, "must have match rules");
  // Do not replace if a cisc-version has been found.
  if( cisc_spill_operand() != Not_cisc_spillable ) return false;

  int         cisc_spill_operand = Maybe_cisc_spillable;
  char       *result             = NULL;
  char       *result2            = NULL;
  const char *op_name            = NULL;
  const char *reg_type           = NULL;
  FormDict   &globals            = AD.globalNames();
  cisc_spill_operand = _matrule->matchrule_cisc_spill_match(globals, AD.get_registers(), instr->_matrule, op_name, reg_type);
  if( (cisc_spill_operand != Not_cisc_spillable) && (op_name != NULL) && equivalent_predicates(this, instr) ) {
    cisc_spill_operand = operand_position(op_name, Component::USE);
    int def_oper  = operand_position(op_name, Component::DEF);
    if( def_oper == NameList::Not_in_list && instr->num_opnds() == num_opnds()) {
      // Do not support cisc-spilling for destination operands and
      // make sure they have the same number of operands.
      _cisc_spill_alternate = instr;
      instr->set_cisc_alternate(true);
      if( AD._cisc_spill_debug ) {
        fprintf(stderr, "Instruction %s cisc-spills-to %s\n", _ident, instr->_ident);
        fprintf(stderr, "   using operand %s %s at index %d\n", reg_type, op_name, cisc_spill_operand);
      }
      // Record that a stack-version of the reg_mask is needed
      // !!!!!
      OperandForm *oper = (OperandForm*)(globals[reg_type]->is_operand());
      assert( oper != NULL, "cisc-spilling non operand");
      const char *reg_class_name = oper->constrained_reg_class();
      AD.set_stack_or_reg(reg_class_name);
      const char *reg_mask_name  = AD.reg_mask(*oper);
      set_cisc_reg_mask_name(reg_mask_name);
      const char *stack_or_reg_mask_name = AD.stack_or_reg_mask(*oper);
    } else {
      cisc_spill_operand = Not_cisc_spillable;
    }
  } else {
    cisc_spill_operand = Not_cisc_spillable;
  }

  set_cisc_spill_operand(cisc_spill_operand);
  return (cisc_spill_operand != Not_cisc_spillable);
}

// Check to see if this instruction can be replaced with the short branch
// instruction `short-branch'
bool InstructForm::check_branch_variant(ArchDesc &AD, InstructForm *short_branch) {
  if (_matrule != NULL &&
      this != short_branch &&   // Don't match myself
      !is_short_branch() &&     // Don't match another short branch variant
      reduce_result() != NULL &&
      strstr(_ident, "restoreMask") == NULL && // Don't match side effects
      strcmp(reduce_result(), short_branch->reduce_result()) == 0 &&
      _matrule->equivalent(AD.globalNames(), short_branch->_matrule)) {
    // The instructions are equivalent.

    // Now verify that both instructions have the same parameters and
    // the same effects. Both branch forms should have the same inputs
    // and resulting projections to correctly replace a long branch node
    // with corresponding short branch node during code generation.

    bool different = false;
    if (short_branch->_components.count() != _components.count()) {
       different = true;
    } else if (_components.count() > 0) {
      short_branch->_components.reset();
      _components.reset();
      Component *comp;
      while ((comp = _components.iter()) != NULL) {
        Component *short_comp = short_branch->_components.iter();
        if (short_comp == NULL ||
            short_comp->_type != comp->_type ||
            short_comp->_usedef != comp->_usedef) {
          different = true;
          break;
        }
      }
      if (short_branch->_components.iter() != NULL)
        different = true;
    }
    if (different) {
      globalAD->syntax_err(short_branch->_linenum, "Instruction %s and its short form %s have different parameters\n", _ident, short_branch->_ident);
    }
    if (AD._adl_debug > 1 || AD._short_branch_debug) {
      fprintf(stderr, "Instruction %s has short form %s\n", _ident, short_branch->_ident);
    }
    _short_branch_form = short_branch;
    return true;
  }
  return false;
}


// --------------------------- FILE *output_routines
//
// Generate the format call for the replacement variable
void InstructForm::rep_var_format(FILE *fp, const char *rep_var) {
  // Handle special constant table variables.
  if (strcmp(rep_var, "constanttablebase") == 0) {
    fprintf(fp, "char reg[128];  ra->dump_register(in(mach_constant_base_node_input()), reg);\n");
    fprintf(fp, "    st->print(\"%%s\", reg);\n");
    return;
  }
  if (strcmp(rep_var, "constantoffset") == 0) {
    fprintf(fp, "st->print(\"#%%d\", constant_offset_unchecked());\n");
    return;
  }
  if (strcmp(rep_var, "constantaddress") == 0) {
    fprintf(fp, "st->print(\"constant table base + #%%d\", constant_offset_unchecked());\n");
    return;
  }

  // Find replacement variable's type
  const Form *form   = _localNames[rep_var];
  if (form == NULL) {
    globalAD->syntax_err(_linenum, "Unknown replacement variable %s in format statement of %s.",
                         rep_var, _ident);
    return;
  }
  OpClassForm *opc   = form->is_opclass();
  assert( opc, "replacement variable was not found in local names");
  // Lookup the index position of the replacement variable
  int idx  = operand_position_format(rep_var);
  if ( idx == -1 ) {
    globalAD->syntax_err(_linenum, "Could not find replacement variable %s in format statement of %s.\n",
                         rep_var, _ident);
    assert(strcmp(opc->_ident, "label") == 0, "Unimplemented");
    return;
  }

  if (is_noninput_operand(idx)) {
    // This component isn't in the input array.  Print out the static
    // name of the register.
    OperandForm* oper = form->is_operand();
    if (oper != NULL && oper->is_bound_register()) {
      const RegDef* first = oper->get_RegClass()->find_first_elem();
      fprintf(fp, "    st->print_raw(\"%s\");\n", first->_regname);
    } else {
      globalAD->syntax_err(_linenum, "In %s can't find format for %s %s", _ident, opc->_ident, rep_var);
    }
  } else {
    // Output the format call for this operand
    fprintf(fp,"opnd_array(%d)->",idx);
    if (idx == 0)
      fprintf(fp,"int_format(ra, this, st); // %s\n", rep_var);
    else
      fprintf(fp,"ext_format(ra, this,idx%d, st); // %s\n", idx, rep_var );
  }
}

// Seach through operands to determine parameters unique positions.
void InstructForm::set_unique_opnds() {
  uint* uniq_idx = NULL;
  uint  nopnds = num_opnds();
  uint  num_uniq = nopnds;
  uint i;
  _uniq_idx_length = 0;
  if (nopnds > 0) {
    // Allocate index array.  Worst case we're mapping from each
    // component back to an index and any DEF always goes at 0 so the
    // length of the array has to be the number of components + 1.
    _uniq_idx_length = _components.count() + 1;
    uniq_idx = (uint*) AllocateHeap(sizeof(uint) * _uniq_idx_length);
    for (i = 0; i < _uniq_idx_length; i++) {
      uniq_idx[i] = i;
    }
  }
  // Do it only if there is a match rule and no expand rule.  With an
  // expand rule it is done by creating new mach node in Expand()
  // method.
  if (nopnds > 0 && _matrule != NULL && _exprule == NULL) {
    const char *name;
    uint count;
    bool has_dupl_use = false;

    _parameters.reset();
    while ((name = _parameters.iter()) != NULL) {
      count = 0;
      uint position = 0;
      uint uniq_position = 0;
      _components.reset();
      Component *comp = NULL;
      if (sets_result()) {
        comp = _components.iter();
        position++;
      }
      // The next code is copied from the method operand_position().
      for (; (comp = _components.iter()) != NULL; ++position) {
        // When the first component is not a DEF,
        // leave space for the result operand!
        if (position==0 && (!comp->isa(Component::DEF))) {
          ++position;
        }
        if (strcmp(name, comp->_name) == 0) {
          if (++count > 1) {
            assert(position < _uniq_idx_length, "out of bounds");
            uniq_idx[position] = uniq_position;
            has_dupl_use = true;
          } else {
            uniq_position = position;
          }
        }
        if (comp->isa(Component::DEF) && comp->isa(Component::USE)) {
          ++position;
          if (position != 1)
            --position;   // only use two slots for the 1st USE_DEF
        }
      }
    }
    if (has_dupl_use) {
      for (i = 1; i < nopnds; i++) {
        if (i != uniq_idx[i]) {
          break;
        }
      }
      uint j = i;
      for (; i < nopnds; i++) {
        if (i == uniq_idx[i]) {
          uniq_idx[i] = j++;
        }
      }
      num_uniq = j;
    }
  }
  _uniq_idx = uniq_idx;
  _num_uniq = num_uniq;
}

// Generate index values needed for determining the operand position
void InstructForm::index_temps(FILE *fp, FormDict &globals, const char *prefix, const char *receiver) {
  uint  idx = 0;                  // position of operand in match rule
  int   cur_num_opnds = num_opnds();

  // Compute the index into vector of operand pointers:
  // idx0=0 is used to indicate that info comes from this same node, not from input edge.
  // idx1 starts at oper_input_base()
  if ( cur_num_opnds >= 1 ) {
    fprintf(fp,"  // Start at oper_input_base() and count operands\n");
    fprintf(fp,"  unsigned %sidx0 = %d;\n", prefix, oper_input_base(globals));
    fprintf(fp,"  unsigned %sidx1 = %d;", prefix, oper_input_base(globals));
    fprintf(fp," \t// %s\n", unique_opnd_ident(1));

    // Generate starting points for other unique operands if they exist
    for ( idx = 2; idx < num_unique_opnds(); ++idx ) {
      if( *receiver == 0 ) {
        fprintf(fp,"  unsigned %sidx%d = %sidx%d + opnd_array(%d)->num_edges();",
                prefix, idx, prefix, idx-1, idx-1 );
      } else {
        fprintf(fp,"  unsigned %sidx%d = %sidx%d + %s_opnds[%d]->num_edges();",
                prefix, idx, prefix, idx-1, receiver, idx-1 );
      }
      fprintf(fp," \t// %s\n", unique_opnd_ident(idx));
    }
  }
  if( *receiver != 0 ) {
    // This value is used by generate_peepreplace when copying a node.
    // Don't emit it in other cases since it can hide bugs with the
    // use invalid idx's.
    fprintf(fp,"  unsigned %sidx%d = %sreq(); \n", prefix, idx, receiver);
  }

}

// ---------------------------
bool InstructForm::verify() {
  // !!!!! !!!!!
  // Check that a "label" operand occurs last in the operand list, if present
  return true;
}

void InstructForm::dump() {
  output(stderr);
}

void InstructForm::output(FILE *fp) {
  fprintf(fp,"\nInstruction: %s\n", (_ident?_ident:""));
  if (_matrule)   _matrule->output(fp);
  if (_insencode) _insencode->output(fp);
  if (_constant)  _constant->output(fp);
  if (_opcode)    _opcode->output(fp);
  if (_attribs)   _attribs->output(fp);
  if (_predicate) _predicate->output(fp);
  if (_effects.Size()) {
    fprintf(fp,"Effects\n");
    _effects.dump();
  }
  if (_exprule)   _exprule->output(fp);
  if (_rewrule)   _rewrule->output(fp);
  if (_format)    _format->output(fp);
  if (_peephole)  _peephole->output(fp);
}

void MachNodeForm::dump() {
  output(stderr);
}

void MachNodeForm::output(FILE *fp) {
  fprintf(fp,"\nMachNode: %s\n", (_ident?_ident:""));
}

//------------------------------build_predicate--------------------------------
// Build instruction predicates.  If the user uses the same operand name
// twice, we need to check that the operands are pointer-eequivalent in
// the DFA during the labeling process.
Predicate *InstructForm::build_predicate() {
  const int buflen = 1024;
  char buf[buflen], *s=buf;
  Dict names(cmpstr,hashstr,Form::arena);       // Map Names to counts

  MatchNode *mnode =
    strcmp(_matrule->_opType, "Set") ? _matrule : _matrule->_rChild;
  if (mnode != NULL) mnode->count_instr_names(names);

  uint first = 1;
  // Start with the predicate supplied in the .ad file.
  if (_predicate) {
    if (first) first = 0;
    strcpy(s, "("); s += strlen(s);
    strncpy(s, _predicate->_pred, buflen - strlen(s) - 1);
    s += strlen(s);
    strcpy(s, ")"); s += strlen(s);
  }
  for( DictI i(&names); i.test(); ++i ) {
    uintptr_t cnt = (uintptr_t)i._value;
    if( cnt > 1 ) {             // Need a predicate at all?
      int path_bitmask = 0;
      assert( cnt == 2, "Unimplemented" );
      // Handle many pairs
      if( first ) first=0;
      else {                    // All tests must pass, so use '&&'
        strcpy(s," && ");
        s += strlen(s);
      }
      // Add predicate to working buffer
      sprintf(s,"/*%s*/(",(char*)i._key);
      s += strlen(s);
      mnode->build_instr_pred(s,(char*)i._key, 0, path_bitmask, 0);
      s += strlen(s);
      strcpy(s," == "); s += strlen(s);
      mnode->build_instr_pred(s,(char*)i._key, 1, path_bitmask, 0);
      s += strlen(s);
      strcpy(s,")"); s += strlen(s);
    }
  }
  if( s == buf ) s = NULL;
  else {
    assert( strlen(buf) < sizeof(buf), "String buffer overflow" );
    s = strdup(buf);
  }
  return new Predicate(s);
}

//------------------------------EncodeForm-------------------------------------
// Constructor
EncodeForm::EncodeForm()
  : _encClass(cmpstr,hashstr, Form::arena) {
}
EncodeForm::~EncodeForm() {
}

// record a new register class
EncClass *EncodeForm::add_EncClass(const char *className) {
  EncClass *encClass = new EncClass(className);
  _eclasses.addName(className);
  _encClass.Insert(className,encClass);
  return encClass;
}

// Lookup the function body for an encoding class
EncClass  *EncodeForm::encClass(const char *className) {
  assert( className != NULL, "Must provide a defined encoding name");

  EncClass *encClass = (EncClass*)_encClass[className];
  return encClass;
}

// Lookup the function body for an encoding class
const char *EncodeForm::encClassBody(const char *className) {
  if( className == NULL ) return NULL;

  EncClass *encClass = (EncClass*)_encClass[className];
  assert( encClass != NULL, "Encode Class is missing.");
  encClass->_code.reset();
  const char *code = (const char*)encClass->_code.iter();
  assert( code != NULL, "Found an empty encode class body.");

  return code;
}

// Lookup the function body for an encoding class
const char *EncodeForm::encClassPrototype(const char *className) {
  assert( className != NULL, "Encode class name must be non NULL.");

  return className;
}

void EncodeForm::dump() {                  // Debug printer
  output(stderr);
}

void EncodeForm::output(FILE *fp) {          // Write info to output files
  const char *name;
  fprintf(fp,"\n");
  fprintf(fp,"-------------------- Dump EncodeForm --------------------\n");
  for (_eclasses.reset(); (name = _eclasses.iter()) != NULL;) {
    ((EncClass*)_encClass[name])->output(fp);
  }
  fprintf(fp,"-------------------- end  EncodeForm --------------------\n");
}
//------------------------------EncClass---------------------------------------
EncClass::EncClass(const char *name)
  : _localNames(cmpstr,hashstr, Form::arena), _name(name) {
}
EncClass::~EncClass() {
}

// Add a parameter <type,name> pair
void EncClass::add_parameter(const char *parameter_type, const char *parameter_name) {
  _parameter_type.addName( parameter_type );
  _parameter_name.addName( parameter_name );
}

// Verify operand types in parameter list
bool EncClass::check_parameter_types(FormDict &globals) {
  // !!!!!
  return false;
}

// Add the decomposed "code" sections of an encoding's code-block
void EncClass::add_code(const char *code) {
  _code.addName(code);
}

// Add the decomposed "replacement variables" of an encoding's code-block
void EncClass::add_rep_var(char *replacement_var) {
  _code.addName(NameList::_signal);
  _rep_vars.addName(replacement_var);
}

// Lookup the function body for an encoding class
int EncClass::rep_var_index(const char *rep_var) {
  uint        position = 0;
  const char *name     = NULL;

  _parameter_name.reset();
  while ( (name = _parameter_name.iter()) != NULL ) {
    if ( strcmp(rep_var,name) == 0 ) return position;
    ++position;
  }

  return -1;
}

// Check after parsing
bool EncClass::verify() {
  // 1!!!!
  // Check that each replacement variable, '$name' in architecture description
  // is actually a local variable for this encode class, or a reserved name
  // "primary, secondary, tertiary"
  return true;
}

void EncClass::dump() {
  output(stderr);
}

// Write info to output files
void EncClass::output(FILE *fp) {
  fprintf(fp,"EncClass: %s", (_name ? _name : ""));

  // Output the parameter list
  _parameter_type.reset();
  _parameter_name.reset();
  const char *type = _parameter_type.iter();
  const char *name = _parameter_name.iter();
  fprintf(fp, " ( ");
  for ( ; (type != NULL) && (name != NULL);
        (type = _parameter_type.iter()), (name = _parameter_name.iter()) ) {
    fprintf(fp, " %s %s,", type, name);
  }
  fprintf(fp, " ) ");

  // Output the code block
  _code.reset();
  _rep_vars.reset();
  const char *code;
  while ( (code = _code.iter()) != NULL ) {
    if ( _code.is_signal(code) ) {
      // A replacement variable
      const char *rep_var = _rep_vars.iter();
      fprintf(fp,"($%s)", rep_var);
    } else {
      // A section of code
      fprintf(fp,"%s", code);
    }
  }

}

//------------------------------Opcode-----------------------------------------
Opcode::Opcode(char *primary, char *secondary, char *tertiary)
  : _primary(primary), _secondary(secondary), _tertiary(tertiary) {
}

Opcode::~Opcode() {
}

Opcode::opcode_type Opcode::as_opcode_type(const char *param) {
  if( strcmp(param,"primary") == 0 ) {
    return Opcode::PRIMARY;
  }
  else if( strcmp(param,"secondary") == 0 ) {
    return Opcode::SECONDARY;
  }
  else if( strcmp(param,"tertiary") == 0 ) {
    return Opcode::TERTIARY;
  }
  return Opcode::NOT_AN_OPCODE;
}

bool Opcode::print_opcode(FILE *fp, Opcode::opcode_type desired_opcode) {
  // Default values previously provided by MachNode::primary()...
  const char *description = NULL;
  const char *value       = NULL;
  // Check if user provided any opcode definitions
  // Update 'value' if user provided a definition in the instruction
  switch (desired_opcode) {
  case PRIMARY:
    description = "primary()";
    if( _primary   != NULL)  { value = _primary;     }
    break;
  case SECONDARY:
    description = "secondary()";
    if( _secondary != NULL ) { value = _secondary;   }
    break;
  case TERTIARY:
    description = "tertiary()";
    if( _tertiary  != NULL ) { value = _tertiary;    }
    break;
  default:
    assert( false, "ShouldNotReachHere();");
    break;
  }

  if (value != NULL) {
    fprintf(fp, "(%s /*%s*/)", value, description);
  }
  return value != NULL;
}

void Opcode::dump() {
  output(stderr);
}

// Write info to output files
void Opcode::output(FILE *fp) {
  if (_primary   != NULL) fprintf(fp,"Primary   opcode: %s\n", _primary);
  if (_secondary != NULL) fprintf(fp,"Secondary opcode: %s\n", _secondary);
  if (_tertiary  != NULL) fprintf(fp,"Tertiary  opcode: %s\n", _tertiary);
}

//------------------------------InsEncode--------------------------------------
InsEncode::InsEncode() {
}
InsEncode::~InsEncode() {
}

// Add "encode class name" and its parameters
NameAndList *InsEncode::add_encode(char *encoding) {
  assert( encoding != NULL, "Must provide name for encoding");

  // add_parameter(NameList::_signal);
  NameAndList *encode = new NameAndList(encoding);
  _encoding.addName((char*)encode);

  return encode;
}

// Access the list of encodings
void InsEncode::reset() {
  _encoding.reset();
  // _parameter.reset();
}
const char* InsEncode::encode_class_iter() {
  NameAndList  *encode_class = (NameAndList*)_encoding.iter();
  return  ( encode_class != NULL ? encode_class->name() : NULL );
}
// Obtain parameter name from zero based index
const char *InsEncode::rep_var_name(InstructForm &inst, uint param_no) {
  NameAndList *params = (NameAndList*)_encoding.current();
  assert( params != NULL, "Internal Error");
  const char *param = (*params)[param_no];

  // Remove '$' if parser placed it there.
  return ( param != NULL && *param == '$') ? (param+1) : param;
}

void InsEncode::dump() {
  output(stderr);
}

// Write info to output files
void InsEncode::output(FILE *fp) {
  NameAndList *encoding  = NULL;
  const char  *parameter = NULL;

  fprintf(fp,"InsEncode: ");
  _encoding.reset();

  while ( (encoding = (NameAndList*)_encoding.iter()) != 0 ) {
    // Output the encoding being used
    fprintf(fp,"%s(", encoding->name() );

    // Output its parameter list, if any
    bool first_param = true;
    encoding->reset();
    while (  (parameter = encoding->iter()) != 0 ) {
      // Output the ',' between parameters
      if ( ! first_param )  fprintf(fp,", ");
      first_param = false;
      // Output the parameter
      fprintf(fp,"%s", parameter);
    } // done with parameters
    fprintf(fp,")  ");
  } // done with encodings

  fprintf(fp,"\n");
}

//------------------------------Effect-----------------------------------------
static int effect_lookup(const char *name) {
  if (!strcmp(name, "USE")) return Component::USE;
  if (!strcmp(name, "DEF")) return Component::DEF;
  if (!strcmp(name, "USE_DEF")) return Component::USE_DEF;
  if (!strcmp(name, "KILL")) return Component::KILL;
  if (!strcmp(name, "USE_KILL")) return Component::USE_KILL;
  if (!strcmp(name, "TEMP")) return Component::TEMP;
  if (!strcmp(name, "TEMP_DEF")) return Component::TEMP_DEF;
  if (!strcmp(name, "INVALID")) return Component::INVALID;
  if (!strcmp(name, "CALL")) return Component::CALL;
  assert(false,"Invalid effect name specified\n");
  return Component::INVALID;
}

const char *Component::getUsedefName() {
  switch (_usedef) {
    case Component::INVALID:  return "INVALID";  break;
    case Component::USE:      return "USE";      break;
    case Component::USE_DEF:  return "USE_DEF";  break;
    case Component::USE_KILL: return "USE_KILL"; break;
    case Component::KILL:     return "KILL";     break;
    case Component::TEMP:     return "TEMP";     break;
    case Component::TEMP_DEF: return "TEMP_DEF"; break;
    case Component::DEF:      return "DEF";      break;
    case Component::CALL:     return "CALL";     break;
    default: assert(false, "unknown effect");
  }
  return "Undefined Use/Def info";
}

Effect::Effect(const char *name) : _name(name), _use_def(effect_lookup(name)) {
  _ftype = Form::EFF;
}

Effect::~Effect() {
}

// Dynamic type check
Effect *Effect::is_effect() const {
  return (Effect*)this;
}


// True if this component is equal to the parameter.
bool Effect::is(int use_def_kill_enum) const {
  return (_use_def == use_def_kill_enum ? true : false);
}
// True if this component is used/def'd/kill'd as the parameter suggests.
bool Effect::isa(int use_def_kill_enum) const {
  return (_use_def & use_def_kill_enum) == use_def_kill_enum;
}

void Effect::dump() {
  output(stderr);
}

void Effect::output(FILE *fp) {          // Write info to output files
  fprintf(fp,"Effect: %s\n", (_name?_name:""));
}

//------------------------------ExpandRule-------------------------------------
ExpandRule::ExpandRule() : _expand_instrs(),
                           _newopconst(cmpstr, hashstr, Form::arena) {
  _ftype = Form::EXP;
}

ExpandRule::~ExpandRule() {                  // Destructor
}

void ExpandRule::add_instruction(NameAndList *instruction_name_and_operand_list) {
  _expand_instrs.addName((char*)instruction_name_and_operand_list);
}

void ExpandRule::reset_instructions() {
  _expand_instrs.reset();
}

NameAndList* ExpandRule::iter_instructions() {
  return (NameAndList*)_expand_instrs.iter();
}


void ExpandRule::dump() {
  output(stderr);
}

void ExpandRule::output(FILE *fp) {         // Write info to output files
  NameAndList *expand_instr = NULL;
  const char *opid = NULL;

  fprintf(fp,"\nExpand Rule:\n");

  // Iterate over the instructions 'node' expands into
  for(reset_instructions(); (expand_instr = iter_instructions()) != NULL; ) {
    fprintf(fp,"%s(", expand_instr->name());

    // iterate over the operand list
    for( expand_instr->reset(); (opid = expand_instr->iter()) != NULL; ) {
      fprintf(fp,"%s ", opid);
    }
    fprintf(fp,");\n");
  }
}

//------------------------------RewriteRule------------------------------------
RewriteRule::RewriteRule(char* params, char* block)
  : _tempParams(params), _tempBlock(block) { };  // Constructor
RewriteRule::~RewriteRule() {                 // Destructor
}

void RewriteRule::dump() {
  output(stderr);
}

void RewriteRule::output(FILE *fp) {         // Write info to output files
  fprintf(fp,"\nRewrite Rule:\n%s\n%s\n",
          (_tempParams?_tempParams:""),
          (_tempBlock?_tempBlock:""));
}


//==============================MachNodes======================================
//------------------------------MachNodeForm-----------------------------------
MachNodeForm::MachNodeForm(char *id)
  : _ident(id) {
}

MachNodeForm::~MachNodeForm() {
}

MachNodeForm *MachNodeForm::is_machnode() const {
  return (MachNodeForm*)this;
}

//==============================Operand Classes================================
//------------------------------OpClassForm------------------------------------
OpClassForm::OpClassForm(const char* id) : _ident(id) {
  _ftype = Form::OPCLASS;
}

OpClassForm::~OpClassForm() {
}

bool OpClassForm::ideal_only() const { return 0; }

OpClassForm *OpClassForm::is_opclass() const {
  return (OpClassForm*)this;
}

Form::InterfaceType OpClassForm::interface_type(FormDict &globals) const {
  if( _oplst.count() == 0 ) return Form::no_interface;

  // Check that my operands have the same interface type
  Form::InterfaceType  interface;
  bool  first = true;
  NameList &op_list = (NameList &)_oplst;
  op_list.reset();
  const char *op_name;
  while( (op_name = op_list.iter()) != NULL ) {
    const Form  *form    = globals[op_name];
    OperandForm *operand = form->is_operand();
    assert( operand, "Entry in operand class that is not an operand");
    if( first ) {
      first     = false;
      interface = operand->interface_type(globals);
    } else {
      interface = (interface == operand->interface_type(globals) ? interface : Form::no_interface);
    }
  }
  return interface;
}

bool OpClassForm::stack_slots_only(FormDict &globals) const {
  if( _oplst.count() == 0 ) return false;  // how?

  NameList &op_list = (NameList &)_oplst;
  op_list.reset();
  const char *op_name;
  while( (op_name = op_list.iter()) != NULL ) {
    const Form  *form    = globals[op_name];
    OperandForm *operand = form->is_operand();
    assert( operand, "Entry in operand class that is not an operand");
    if( !operand->stack_slots_only(globals) )  return false;
  }
  return true;
}


void OpClassForm::dump() {
  output(stderr);
}

void OpClassForm::output(FILE *fp) {
  const char *name;
  fprintf(fp,"\nOperand Class: %s\n", (_ident?_ident:""));
  fprintf(fp,"\nCount = %d\n", _oplst.count());
  for(_oplst.reset(); (name = _oplst.iter()) != NULL;) {
    fprintf(fp,"%s, ",name);
  }
  fprintf(fp,"\n");
}


//==============================Operands=======================================
//------------------------------OperandForm------------------------------------
OperandForm::OperandForm(const char* id)
  : OpClassForm(id), _ideal_only(false),
    _localNames(cmpstr, hashstr, Form::arena) {
      _ftype = Form::OPER;

      _matrule   = NULL;
      _interface = NULL;
      _attribs   = NULL;
      _predicate = NULL;
      _constraint= NULL;
      _construct = NULL;
      _format    = NULL;
}
OperandForm::OperandForm(const char* id, bool ideal_only)
  : OpClassForm(id), _ideal_only(ideal_only),
    _localNames(cmpstr, hashstr, Form::arena) {
      _ftype = Form::OPER;

      _matrule   = NULL;
      _interface = NULL;
      _attribs   = NULL;
      _predicate = NULL;
      _constraint= NULL;
      _construct = NULL;
      _format    = NULL;
}
OperandForm::~OperandForm() {
}


OperandForm *OperandForm::is_operand() const {
  return (OperandForm*)this;
}

bool OperandForm::ideal_only() const {
  return _ideal_only;
}

Form::InterfaceType OperandForm::interface_type(FormDict &globals) const {
  if( _interface == NULL )  return Form::no_interface;

  return _interface->interface_type(globals);
}


bool OperandForm::stack_slots_only(FormDict &globals) const {
  if( _constraint == NULL )  return false;
  return _constraint->stack_slots_only();
}


// Access op_cost attribute or return NULL.
const char* OperandForm::cost() {
  for (Attribute* cur = _attribs; cur != NULL; cur = (Attribute*)cur->_next) {
    if( strcmp(cur->_ident,AttributeForm::_op_cost) == 0 ) {
      return cur->_val;
    }
  }
  return NULL;
}

// Return the number of leaves below this complex operand
uint OperandForm::num_leaves() const {
  if ( ! _matrule) return 0;

  int num_leaves = _matrule->_numleaves;
  return num_leaves;
}

// Return the number of constants contained within this complex operand
uint OperandForm::num_consts(FormDict &globals) const {
  if ( ! _matrule) return 0;

  // This is a recursive invocation on all operands in the matchrule
  return _matrule->num_consts(globals);
}

// Return the number of constants in match rule with specified type
uint OperandForm::num_consts(FormDict &globals, Form::DataType type) const {
  if ( ! _matrule) return 0;

  // This is a recursive invocation on all operands in the matchrule
  return _matrule->num_consts(globals, type);
}

// Return the number of pointer constants contained within this complex operand
uint OperandForm::num_const_ptrs(FormDict &globals) const {
  if ( ! _matrule) return 0;

  // This is a recursive invocation on all operands in the matchrule
  return _matrule->num_const_ptrs(globals);
}

uint OperandForm::num_edges(FormDict &globals) const {
  uint edges  = 0;
  uint leaves = num_leaves();
  uint consts = num_consts(globals);

  // If we are matching a constant directly, there are no leaves.
  edges = ( leaves > consts ) ? leaves - consts : 0;

  // !!!!!
  // Special case operands that do not have a corresponding ideal node.
  if( (edges == 0) && (consts == 0) ) {
    if( constrained_reg_class() != NULL ) {
      edges = 1;
    } else {
      if( _matrule
          && (_matrule->_lChild == NULL) && (_matrule->_rChild == NULL) ) {
        const Form *form = globals[_matrule->_opType];
        OperandForm *oper = form ? form->is_operand() : NULL;
        if( oper ) {
          return oper->num_edges(globals);
        }
      }
    }
  }

  return edges;
}


// Check if this operand is usable for cisc-spilling
bool  OperandForm::is_cisc_reg(FormDict &globals) const {
  const char *ideal = ideal_type(globals);
  bool is_cisc_reg = (ideal && (ideal_to_Reg_type(ideal) != none));
  return is_cisc_reg;
}

bool  OpClassForm::is_cisc_mem(FormDict &globals) const {
  Form::InterfaceType my_interface = interface_type(globals);
  return (my_interface == memory_interface);
}


// node matches ideal 'Bool'
bool OperandForm::is_ideal_bool() const {
  if( _matrule == NULL ) return false;

  return _matrule->is_ideal_bool();
}

// Require user's name for an sRegX to be stackSlotX
Form::DataType OperandForm::is_user_name_for_sReg() const {
  DataType data_type = none;
  if( _ident != NULL ) {
    if(      strcmp(_ident,"stackSlotI") == 0 ) data_type = Form::idealI;
    else if( strcmp(_ident,"stackSlotP") == 0 ) data_type = Form::idealP;
    else if( strcmp(_ident,"stackSlotD") == 0 ) data_type = Form::idealD;
    else if( strcmp(_ident,"stackSlotF") == 0 ) data_type = Form::idealF;
    else if( strcmp(_ident,"stackSlotL") == 0 ) data_type = Form::idealL;
  }
  assert((data_type == none) || (_matrule == NULL), "No match-rule for stackSlotX");

  return data_type;
}


// Return ideal type, if there is a single ideal type for this operand
const char *OperandForm::ideal_type(FormDict &globals, RegisterForm *registers) const {
  const char *type = NULL;
  if (ideal_only()) type = _ident;
  else if( _matrule == NULL ) {
    // Check for condition code register
    const char *rc_name = constrained_reg_class();
    // !!!!!
    if (rc_name == NULL) return NULL;
    // !!!!! !!!!!
    // Check constraints on result's register class
    if( registers ) {
      RegClass *reg_class  = registers->getRegClass(rc_name);
      assert( reg_class != NULL, "Register class is not defined");

      // Check for ideal type of entries in register class, all are the same type
      reg_class->reset();
      RegDef *reg_def = reg_class->RegDef_iter();
      assert( reg_def != NULL, "No entries in register class");
      assert( reg_def->_idealtype != NULL, "Did not define ideal type for register");
      // Return substring that names the register's ideal type
      type = reg_def->_idealtype + 3;
      assert( *(reg_def->_idealtype + 0) == 'O', "Expect Op_ prefix");
      assert( *(reg_def->_idealtype + 1) == 'p', "Expect Op_ prefix");
      assert( *(reg_def->_idealtype + 2) == '_', "Expect Op_ prefix");
    }
  }
  else if( _matrule->_lChild == NULL && _matrule->_rChild == NULL ) {
    // This operand matches a single type, at the top level.
    // Check for ideal type
    type = _matrule->_opType;
    if( strcmp(type,"Bool") == 0 )
      return "Bool";
    // transitive lookup
    const Form *frm = globals[type];
    OperandForm *op = frm->is_operand();
    type = op->ideal_type(globals, registers);
  }
  return type;
}


// If there is a single ideal type for this interface field, return it.
const char *OperandForm::interface_ideal_type(FormDict &globals,
                                              const char *field) const {
  const char  *ideal_type = NULL;
  const char  *value      = NULL;

  // Check if "field" is valid for this operand's interface
  if ( ! is_interface_field(field, value) )   return ideal_type;

  // !!!!! !!!!! !!!!!
  // If a valid field has a constant value, identify "ConI" or "ConP" or ...

  // Else, lookup type of field's replacement variable

  return ideal_type;
}


RegClass* OperandForm::get_RegClass() const {
  if (_interface && !_interface->is_RegInterface()) return NULL;
  return globalAD->get_registers()->getRegClass(constrained_reg_class());
}


bool OperandForm::is_bound_register() const {
  RegClass* reg_class = get_RegClass();
  if (reg_class == NULL) {
    return false;
  }

  const char* name = ideal_type(globalAD->globalNames());
  if (name == NULL) {
    return false;
  }

  uint size = 0;
  if (strcmp(name, "RegFlags") == 0) size = 1;
  if (strcmp(name, "RegI") == 0) size = 1;
  if (strcmp(name, "RegF") == 0) size = 1;
  if (strcmp(name, "RegD") == 0) size = 2;
  if (strcmp(name, "RegL") == 0) size = 2;
  if (strcmp(name, "RegN") == 0) size = 1;
  if (strcmp(name, "VecX") == 0) size = 4;
  if (strcmp(name, "VecY") == 0) size = 8;
  if (strcmp(name, "VecZ") == 0) size = 16;
  if (strcmp(name, "RegP") == 0) size = globalAD->get_preproc_def("_LP64") ? 2 : 1;
  if (size == 0) {
    return false;
  }
  return size == reg_class->size();
}


// Check if this is a valid field for this operand,
// Return 'true' if valid, and set the value to the string the user provided.
bool  OperandForm::is_interface_field(const char *field,
                                      const char * &value) const {
  return false;
}


// Return register class name if a constraint specifies the register class.
const char *OperandForm::constrained_reg_class() const {
  const char *reg_class  = NULL;
  if ( _constraint ) {
    // !!!!!
    Constraint *constraint = _constraint;
    if ( strcmp(_constraint->_func,"ALLOC_IN_RC") == 0 ) {
      reg_class = _constraint->_arg;
    }
  }

  return reg_class;
}


// Return the register class associated with 'leaf'.
const char *OperandForm::in_reg_class(uint leaf, FormDict &globals) {
  const char *reg_class = NULL; // "RegMask::Empty";

  if((_matrule == NULL) || (_matrule->is_chain_rule(globals))) {
    reg_class = constrained_reg_class();
    return reg_class;
  }
  const char *result   = NULL;
  const char *name     = NULL;
  const char *type     = NULL;
  // iterate through all base operands
  // until we reach the register that corresponds to "leaf"
  // This function is not looking for an ideal type.  It needs the first
  // level user type associated with the leaf.
  for(uint idx = 0;_matrule->base_operand(idx,globals,result,name,type);++idx) {
    const Form *form = (_localNames[name] ? _localNames[name] : globals[result]);
    OperandForm *oper = form ? form->is_operand() : NULL;
    if( oper ) {
      reg_class = oper->constrained_reg_class();
      if( reg_class ) {
        reg_class = reg_class;
      } else {
        // ShouldNotReachHere();
      }
    } else {
      // ShouldNotReachHere();
    }

    // Increment our target leaf position if current leaf is not a candidate.
    if( reg_class == NULL)    ++leaf;
    // Exit the loop with the value of reg_class when at the correct index
    if( idx == leaf )         break;
    // May iterate through all base operands if reg_class for 'leaf' is NULL
  }
  return reg_class;
}


// Recursive call to construct list of top-level operands.
// Implementation does not modify state of internal structures
void OperandForm::build_components() {
  if (_matrule)  _matrule->append_components(_localNames, _components);

  // Add parameters that "do not appear in match rule".
  const char *name;
  for (_parameters.reset(); (name = _parameters.iter()) != NULL;) {
    OpClassForm *opForm = _localNames[name]->is_opclass();
    assert(opForm != NULL, "sanity");

    if ( _components.operand_position(name) == -1 ) {
      _components.insert(name, opForm->_ident, Component::INVALID, false);
    }
  }

  return;
}

int OperandForm::operand_position(const char *name, int usedef) {
  return _components.operand_position(name, usedef, this);
}


// Return zero-based position in component list, only counting constants;
// Return -1 if not in list.
int OperandForm::constant_position(FormDict &globals, const Component *last) {
  // Iterate through components and count constants preceding 'constant'
  int position = 0;
  Component *comp;
  _components.reset();
  while( (comp = _components.iter()) != NULL  && (comp != last) ) {
    // Special case for operands that take a single user-defined operand
    // Skip the initial definition in the component list.
    if( strcmp(comp->_name,this->_ident) == 0 ) continue;

    const char *type = comp->_type;
    // Lookup operand form for replacement variable's type
    const Form *form = globals[type];
    assert( form != NULL, "Component's type not found");
    OperandForm *oper = form ? form->is_operand() : NULL;
    if( oper ) {
      if( oper->_matrule->is_base_constant(globals) != Form::none ) {
        ++position;
      }
    }
  }

  // Check for being passed a component that was not in the list
  if( comp != last )  position = -1;

  return position;
}
// Provide position of constant by "name"
int OperandForm::constant_position(FormDict &globals, const char *name) {
  const Component *comp = _components.search(name);
  int idx = constant_position( globals, comp );

  return idx;
}


// Return zero-based position in component list, only counting constants;
// Return -1 if not in list.
int OperandForm::register_position(FormDict &globals, const char *reg_name) {
  // Iterate through components and count registers preceding 'last'
  uint  position = 0;
  Component *comp;
  _components.reset();
  while( (comp = _components.iter()) != NULL
         && (strcmp(comp->_name,reg_name) != 0) ) {
    // Special case for operands that take a single user-defined operand
    // Skip the initial definition in the component list.
    if( strcmp(comp->_name,this->_ident) == 0 ) continue;

    const char *type = comp->_type;
    // Lookup operand form for component's type
    const Form *form = globals[type];
    assert( form != NULL, "Component's type not found");
    OperandForm *oper = form ? form->is_operand() : NULL;
    if( oper ) {
      if( oper->_matrule->is_base_register(globals) ) {
        ++position;
      }
    }
  }

  return position;
}


const char *OperandForm::reduce_result()  const {
  return _ident;
}
// Return the name of the operand on the right hand side of the binary match
// Return NULL if there is no right hand side
const char *OperandForm::reduce_right(FormDict &globals)  const {
  return  ( _matrule ? _matrule->reduce_right(globals) : NULL );
}

// Similar for left
const char *OperandForm::reduce_left(FormDict &globals)   const {
  return  ( _matrule ? _matrule->reduce_left(globals) : NULL );
}


// --------------------------- FILE *output_routines
//
// Output code for disp_is_oop, if true.
void OperandForm::disp_is_oop(FILE *fp, FormDict &globals) {
  //  Check it is a memory interface with a non-user-constant disp field
  if ( this->_interface == NULL ) return;
  MemInterface *mem_interface = this->_interface->is_MemInterface();
  if ( mem_interface == NULL )    return;
  const char   *disp  = mem_interface->_disp;
  if ( *disp != '$' )             return;

  // Lookup replacement variable in operand's component list
  const char   *rep_var = disp + 1;
  const Component *comp = this->_components.search(rep_var);
  assert( comp != NULL, "Replacement variable not found in components");
  // Lookup operand form for replacement variable's type
  const char      *type = comp->_type;
  Form            *form = (Form*)globals[type];
  assert( form != NULL, "Replacement variable's type not found");
  OperandForm     *op   = form->is_operand();
  assert( op, "Memory Interface 'disp' can only emit an operand form");
  // Check if this is a ConP, which may require relocation
  if ( op->is_base_constant(globals) == Form::idealP ) {
    // Find the constant's index:  _c0, _c1, _c2, ... , _cN
    uint idx  = op->constant_position( globals, rep_var);
    fprintf(fp,"  virtual relocInfo::relocType disp_reloc() const {");
    fprintf(fp,  "  return _c%d->reloc();", idx);
    fprintf(fp, " }\n");
  }
}

// Generate code for internal and external format methods
//
// internal access to reg# node->_idx
// access to subsumed constant _c0, _c1,
void  OperandForm::int_format(FILE *fp, FormDict &globals, uint index) {
  Form::DataType dtype;
  if (_matrule && (_matrule->is_base_register(globals) ||
                   strcmp(ideal_type(globalAD->globalNames()), "RegFlags") == 0)) {
    // !!!!! !!!!!
    fprintf(fp,"  { char reg_str[128];\n");
    fprintf(fp,"    ra->dump_register(node,reg_str);\n");
    fprintf(fp,"    st->print(\"%cs\",reg_str);\n",'%');
    fprintf(fp,"  }\n");
  } else if (_matrule && (dtype = _matrule->is_base_constant(globals)) != Form::none) {
    format_constant( fp, index, dtype );
  } else if (ideal_to_sReg_type(_ident) != Form::none) {
    // Special format for Stack Slot Register
    fprintf(fp,"  { char reg_str[128];\n");
    fprintf(fp,"    ra->dump_register(node,reg_str);\n");
    fprintf(fp,"    st->print(\"%cs\",reg_str);\n",'%');
    fprintf(fp,"  }\n");
  } else {
    fprintf(fp,"  st->print(\"No format defined for %s\n\");\n", _ident);
    fflush(fp);
    fprintf(stderr,"No format defined for %s\n", _ident);
    dump();
    assert( false,"Internal error:\n  output_internal_operand() attempting to output other than a Register or Constant");
  }
}

// Similar to "int_format" but for cases where data is external to operand
// external access to reg# node->in(idx)->_idx,
void  OperandForm::ext_format(FILE *fp, FormDict &globals, uint index) {
  Form::DataType dtype;
  if (_matrule && (_matrule->is_base_register(globals) ||
                   strcmp(ideal_type(globalAD->globalNames()), "RegFlags") == 0)) {
    fprintf(fp,"  { char reg_str[128];\n");
    fprintf(fp,"    ra->dump_register(node->in(idx");
    if ( index != 0 ) fprintf(fp,              "+%d",index);
    fprintf(fp,                                      "),reg_str);\n");
    fprintf(fp,"    st->print(\"%cs\",reg_str);\n",'%');
    fprintf(fp,"  }\n");
  } else if (_matrule && (dtype = _matrule->is_base_constant(globals)) != Form::none) {
    format_constant( fp, index, dtype );
  } else if (ideal_to_sReg_type(_ident) != Form::none) {
    // Special format for Stack Slot Register
    fprintf(fp,"  { char reg_str[128];\n");
    fprintf(fp,"    ra->dump_register(node->in(idx");
    if ( index != 0 ) fprintf(fp,                  "+%d",index);
    fprintf(fp,                                       "),reg_str);\n");
    fprintf(fp,"    st->print(\"%cs\",reg_str);\n",'%');
    fprintf(fp,"  }\n");
  } else {
    fprintf(fp,"  st->print(\"No format defined for %s\n\");\n", _ident);
    assert( false,"Internal error:\n  output_external_operand() attempting to output other than a Register or Constant");
  }
}

void OperandForm::format_constant(FILE *fp, uint const_index, uint const_type) {
  switch(const_type) {
  case Form::idealI: fprintf(fp,"  st->print(\"#%%d\", _c%d);\n", const_index); break;
  case Form::idealP: fprintf(fp,"  if (_c%d) _c%d->dump_on(st);\n", const_index, const_index); break;
  case Form::idealNKlass:
  case Form::idealN: fprintf(fp,"  if (_c%d) _c%d->dump_on(st);\n", const_index, const_index); break;
  case Form::idealL: fprintf(fp,"  st->print(\"#\" INT64_FORMAT, (int64_t)_c%d);\n", const_index); break;
  case Form::idealF: fprintf(fp,"  st->print(\"#%%f\", _c%d);\n", const_index); break;
  case Form::idealD: fprintf(fp,"  st->print(\"#%%f\", _c%d);\n", const_index); break;
  default:
    assert( false, "ShouldNotReachHere()");
  }
}

// Return the operand form corresponding to the given index, else NULL.
OperandForm *OperandForm::constant_operand(FormDict &globals,
                                           uint      index) {
  // !!!!!
  // Check behavior on complex operands
  uint n_consts = num_consts(globals);
  if( n_consts > 0 ) {
    uint i = 0;
    const char *type;
    Component  *comp;
    _components.reset();
    if ((comp = _components.iter()) == NULL) {
      assert(n_consts == 1, "Bad component list detected.\n");
      // Current operand is THE operand
      if ( index == 0 ) {
        return this;
      }
    } // end if NULL
    else {
      // Skip the first component, it can not be a DEF of a constant
      do {
        type = comp->base_type(globals);
        // Check that "type" is a 'ConI', 'ConP', ...
        if ( ideal_to_const_type(type) != Form::none ) {
          // When at correct component, get corresponding Operand
          if ( index == 0 ) {
            return globals[comp->_type]->is_operand();
          }
          // Decrement number of constants to go
          --index;
        }
      } while((comp = _components.iter()) != NULL);
    }
  }

  // Did not find a constant for this index.
  return NULL;
}

// If this operand has a single ideal type, return its type
Form::DataType OperandForm::simple_type(FormDict &globals) const {
  const char *type_name = ideal_type(globals);
  Form::DataType type   = type_name ? ideal_to_const_type( type_name )
                                    : Form::none;
  return type;
}

Form::DataType OperandForm::is_base_constant(FormDict &globals) const {
  if ( _matrule == NULL )    return Form::none;

  return _matrule->is_base_constant(globals);
}

// "true" if this operand is a simple type that is swallowed
bool  OperandForm::swallowed(FormDict &globals) const {
  Form::DataType type   = simple_type(globals);
  if( type != Form::none ) {
    return true;
  }

  return false;
}

// Output code to access the value of the index'th constant
void OperandForm::access_constant(FILE *fp, FormDict &globals,
                                  uint const_index) {
  OperandForm *oper = constant_operand(globals, const_index);
  assert( oper, "Index exceeds number of constants in operand");
  Form::DataType dtype = oper->is_base_constant(globals);

  switch(dtype) {
  case idealI: fprintf(fp,"_c%d",           const_index); break;
  case idealP: fprintf(fp,"_c%d->get_con()",const_index); break;
  case idealL: fprintf(fp,"_c%d",           const_index); break;
  case idealF: fprintf(fp,"_c%d",           const_index); break;
  case idealD: fprintf(fp,"_c%d",           const_index); break;
  default:
    assert( false, "ShouldNotReachHere()");
  }
}


void OperandForm::dump() {
  output(stderr);
}

void OperandForm::output(FILE *fp) {
  fprintf(fp,"\nOperand: %s\n", (_ident?_ident:""));
  if (_matrule)    _matrule->dump();
  if (_interface)  _interface->dump();
  if (_attribs)    _attribs->dump();
  if (_predicate)  _predicate->dump();
  if (_constraint) _constraint->dump();
  if (_construct)  _construct->dump();
  if (_format)     _format->dump();
}

//------------------------------Constraint-------------------------------------
Constraint::Constraint(const char *func, const char *arg)
  : _func(func), _arg(arg) {
}
Constraint::~Constraint() { /* not owner of char* */
}

bool Constraint::stack_slots_only() const {
  return strcmp(_func, "ALLOC_IN_RC") == 0
      && strcmp(_arg,  "stack_slots") == 0;
}

void Constraint::dump() {
  output(stderr);
}

void Constraint::output(FILE *fp) {           // Write info to output files
  assert((_func != NULL && _arg != NULL),"missing constraint function or arg");
  fprintf(fp,"Constraint: %s ( %s )\n", _func, _arg);
}

//------------------------------Predicate--------------------------------------
Predicate::Predicate(char *pr)
  : _pred(pr) {
}
Predicate::~Predicate() {
}

void Predicate::dump() {
  output(stderr);
}

void Predicate::output(FILE *fp) {
  fprintf(fp,"Predicate");  // Write to output files
}
//------------------------------Interface--------------------------------------
Interface::Interface(const char *name) : _name(name) {
}
Interface::~Interface() {
}

Form::InterfaceType Interface::interface_type(FormDict &globals) const {
  Interface *thsi = (Interface*)this;
  if ( thsi->is_RegInterface()   ) return Form::register_interface;
  if ( thsi->is_MemInterface()   ) return Form::memory_interface;
  if ( thsi->is_ConstInterface() ) return Form::constant_interface;
  if ( thsi->is_CondInterface()  ) return Form::conditional_interface;

  return Form::no_interface;
}

RegInterface   *Interface::is_RegInterface() {
  if ( strcmp(_name,"REG_INTER") != 0 )
    return NULL;
  return (RegInterface*)this;
}
MemInterface   *Interface::is_MemInterface() {
  if ( strcmp(_name,"MEMORY_INTER") != 0 )  return NULL;
  return (MemInterface*)this;
}
ConstInterface *Interface::is_ConstInterface() {
  if ( strcmp(_name,"CONST_INTER") != 0 )  return NULL;
  return (ConstInterface*)this;
}
CondInterface  *Interface::is_CondInterface() {
  if ( strcmp(_name,"COND_INTER") != 0 )  return NULL;
  return (CondInterface*)this;
}


void Interface::dump() {
  output(stderr);
}

// Write info to output files
void Interface::output(FILE *fp) {
  fprintf(fp,"Interface: %s\n", (_name ? _name : "") );
}

//------------------------------RegInterface-----------------------------------
RegInterface::RegInterface() : Interface("REG_INTER") {
}
RegInterface::~RegInterface() {
}

void RegInterface::dump() {
  output(stderr);
}

// Write info to output files
void RegInterface::output(FILE *fp) {
  Interface::output(fp);
}

//------------------------------ConstInterface---------------------------------
ConstInterface::ConstInterface() : Interface("CONST_INTER") {
}
ConstInterface::~ConstInterface() {
}

void ConstInterface::dump() {
  output(stderr);
}

// Write info to output files
void ConstInterface::output(FILE *fp) {
  Interface::output(fp);
}

//------------------------------MemInterface-----------------------------------
MemInterface::MemInterface(char *base, char *index, char *scale, char *disp)
  : Interface("MEMORY_INTER"), _base(base), _index(index), _scale(scale), _disp(disp) {
}
MemInterface::~MemInterface() {
  // not owner of any character arrays
}

void MemInterface::dump() {
  output(stderr);
}

// Write info to output files
void MemInterface::output(FILE *fp) {
  Interface::output(fp);
  if ( _base  != NULL ) fprintf(fp,"  base  == %s\n", _base);
  if ( _index != NULL ) fprintf(fp,"  index == %s\n", _index);
  if ( _scale != NULL ) fprintf(fp,"  scale == %s\n", _scale);
  if ( _disp  != NULL ) fprintf(fp,"  disp  == %s\n", _disp);
  // fprintf(fp,"\n");
}

//------------------------------CondInterface----------------------------------
CondInterface::CondInterface(const char* equal,         const char* equal_format,
                             const char* not_equal,     const char* not_equal_format,
                             const char* less,          const char* less_format,
                             const char* greater_equal, const char* greater_equal_format,
                             const char* less_equal,    const char* less_equal_format,
                             const char* greater,       const char* greater_format,
                             const char* overflow,      const char* overflow_format,
                             const char* no_overflow,   const char* no_overflow_format)
  : Interface("COND_INTER"),
    _equal(equal),                 _equal_format(equal_format),
    _not_equal(not_equal),         _not_equal_format(not_equal_format),
    _less(less),                   _less_format(less_format),
    _greater_equal(greater_equal), _greater_equal_format(greater_equal_format),
    _less_equal(less_equal),       _less_equal_format(less_equal_format),
    _greater(greater),             _greater_format(greater_format),
    _overflow(overflow),           _overflow_format(overflow_format),
    _no_overflow(no_overflow),     _no_overflow_format(no_overflow_format) {
}
CondInterface::~CondInterface() {
  // not owner of any character arrays
}

void CondInterface::dump() {
  output(stderr);
}

// Write info to output files
void CondInterface::output(FILE *fp) {
  Interface::output(fp);
  if ( _equal  != NULL )     fprintf(fp," equal        == %s\n", _equal);
  if ( _not_equal  != NULL ) fprintf(fp," not_equal    == %s\n", _not_equal);
  if ( _less  != NULL )      fprintf(fp," less         == %s\n", _less);
  if ( _greater_equal  != NULL ) fprintf(fp," greater_equal    == %s\n", _greater_equal);
  if ( _less_equal  != NULL ) fprintf(fp," less_equal   == %s\n", _less_equal);
  if ( _greater  != NULL )    fprintf(fp," greater      == %s\n", _greater);
  if ( _overflow != NULL )    fprintf(fp," overflow     == %s\n", _overflow);
  if ( _no_overflow != NULL ) fprintf(fp," no_overflow  == %s\n", _no_overflow);
  // fprintf(fp,"\n");
}

//------------------------------ConstructRule----------------------------------
ConstructRule::ConstructRule(char *cnstr)
  : _construct(cnstr) {
}
ConstructRule::~ConstructRule() {
}

void ConstructRule::dump() {
  output(stderr);
}

void ConstructRule::output(FILE *fp) {
  fprintf(fp,"\nConstruct Rule\n");  // Write to output files
}


//==============================Shared Forms===================================
//------------------------------AttributeForm----------------------------------
int         AttributeForm::_insId   = 0;           // start counter at 0
int         AttributeForm::_opId    = 0;           // start counter at 0
const char* AttributeForm::_ins_cost = "ins_cost"; // required name
const char* AttributeForm::_op_cost  = "op_cost";  // required name

AttributeForm::AttributeForm(char *attr, int type, char *attrdef)
  : Form(Form::ATTR), _attrname(attr), _atype(type), _attrdef(attrdef) {
    if (type==OP_ATTR) {
      id = ++_opId;
    }
    else if (type==INS_ATTR) {
      id = ++_insId;
    }
    else assert( false,"");
}
AttributeForm::~AttributeForm() {
}

// Dynamic type check
AttributeForm *AttributeForm::is_attribute() const {
  return (AttributeForm*)this;
}


// inlined  // int  AttributeForm::type() { return id;}

void AttributeForm::dump() {
  output(stderr);
}

void AttributeForm::output(FILE *fp) {
  if( _attrname && _attrdef ) {
    fprintf(fp,"\n// AttributeForm \nstatic const int %s = %s;\n",
            _attrname, _attrdef);
  }
  else {
    fprintf(fp,"\n// AttributeForm missing name %s or definition %s\n",
            (_attrname?_attrname:""), (_attrdef?_attrdef:"") );
  }
}

//------------------------------Component--------------------------------------
Component::Component(const char *name, const char *type, int usedef)
  : _name(name), _type(type), _usedef(usedef) {
    _ftype = Form::COMP;
}
Component::~Component() {
}

// True if this component is equal to the parameter.
bool Component::is(int use_def_kill_enum) const {
  return (_usedef == use_def_kill_enum ? true : false);
}
// True if this component is used/def'd/kill'd as the parameter suggests.
bool Component::isa(int use_def_kill_enum) const {
  return (_usedef & use_def_kill_enum) == use_def_kill_enum;
}

// Extend this component with additional use/def/kill behavior
int Component::promote_use_def_info(int new_use_def) {
  _usedef |= new_use_def;

  return _usedef;
}

// Check the base type of this component, if it has one
const char *Component::base_type(FormDict &globals) {
  const Form *frm = globals[_type];
  if (frm == NULL) return NULL;
  OperandForm *op = frm->is_operand();
  if (op == NULL) return NULL;
  if (op->ideal_only()) return op->_ident;
  return (char *)op->ideal_type(globals);
}

void Component::dump() {
  output(stderr);
}

void Component::output(FILE *fp) {
  fprintf(fp,"Component:");  // Write to output files
  fprintf(fp, "  name = %s", _name);
  fprintf(fp, ", type = %s", _type);
  assert(_usedef != 0, "unknown effect");
  fprintf(fp, ", use/def = %s\n", getUsedefName());
}


//------------------------------ComponentList---------------------------------
ComponentList::ComponentList() : NameList(), _matchcnt(0) {
}
ComponentList::~ComponentList() {
  // // This list may not own its elements if copied via assignment
  // Component *component;
  // for (reset(); (component = iter()) != NULL;) {
  //   delete component;
  // }
}

void   ComponentList::insert(Component *component, bool mflag) {
  NameList::addName((char *)component);
  if(mflag) _matchcnt++;
}
void   ComponentList::insert(const char *name, const char *opType, int usedef,
                             bool mflag) {
  Component * component = new Component(name, opType, usedef);
  insert(component, mflag);
}
Component *ComponentList::current() { return (Component*)NameList::current(); }
Component *ComponentList::iter()    { return (Component*)NameList::iter(); }
Component *ComponentList::match_iter() {
  if(_iter < _matchcnt) return (Component*)NameList::iter();
  return NULL;
}
Component *ComponentList::post_match_iter() {
  Component *comp = iter();
  // At end of list?
  if ( comp == NULL ) {
    return comp;
  }
  // In post-match components?
  if (_iter > match_count()-1) {
    return comp;
  }

  return post_match_iter();
}

void       ComponentList::reset()   { NameList::reset(); }
int        ComponentList::count()   { return NameList::count(); }

Component *ComponentList::operator[](int position) {
  // Shortcut complete iteration if there are not enough entries
  if (position >= count()) return NULL;

  int        index     = 0;
  Component *component = NULL;
  for (reset(); (component = iter()) != NULL;) {
    if (index == position) {
      return component;
    }
    ++index;
  }

  return NULL;
}

const Component *ComponentList::search(const char *name) {
  PreserveIter pi(this);
  reset();
  for( Component *comp = NULL; ((comp = iter()) != NULL); ) {
    if( strcmp(comp->_name,name) == 0 ) return comp;
  }

  return NULL;
}

// Return number of USEs + number of DEFs
// When there are no components, or the first component is a USE,
// then we add '1' to hold a space for the 'result' operand.
int ComponentList::num_operands() {
  PreserveIter pi(this);
  uint       count = 1;           // result operand
  uint       position = 0;

  Component *component  = NULL;
  for( reset(); (component = iter()) != NULL; ++position ) {
    if( component->isa(Component::USE) ||
        ( position == 0 && (! component->isa(Component::DEF))) ) {
      ++count;
    }
  }

  return count;
}

// Return zero-based position of operand 'name' in list;  -1 if not in list.
// if parameter 'usedef' is ::USE, it will match USE, USE_DEF, ...
int ComponentList::operand_position(const char *name, int usedef, Form *fm) {
  PreserveIter pi(this);
  int position = 0;
  int num_opnds = num_operands();
  Component *component;
  Component* preceding_non_use = NULL;
  Component* first_def = NULL;
  for (reset(); (component = iter()) != NULL; ++position) {
    // When the first component is not a DEF,
    // leave space for the result operand!
    if ( position==0 && (! component->isa(Component::DEF)) ) {
      ++position;
      ++num_opnds;
    }
    if (strcmp(name, component->_name)==0 && (component->isa(usedef))) {
      // When the first entry in the component list is a DEF and a USE
      // Treat them as being separate, a DEF first, then a USE
      if( position==0
          && usedef==Component::USE && component->isa(Component::DEF) ) {
        assert(position+1 < num_opnds, "advertised index in bounds");
        return position+1;
      } else {
        if( preceding_non_use && strcmp(component->_name, preceding_non_use->_name) ) {
          fprintf(stderr, "the name '%s(%s)' should not precede the name '%s(%s)'",
                  preceding_non_use->_name, preceding_non_use->getUsedefName(),
                  name, component->getUsedefName());
          if (fm && fm->is_instruction()) fprintf(stderr,  "in form '%s'", fm->is_instruction()->_ident);
          if (fm && fm->is_operand()) fprintf(stderr,  "in form '%s'", fm->is_operand()->_ident);
          fprintf(stderr,  "\n");
        }
        if( position >= num_opnds ) {
          fprintf(stderr, "the name '%s' is too late in its name list", name);
          if (fm && fm->is_instruction()) fprintf(stderr,  "in form '%s'", fm->is_instruction()->_ident);
          if (fm && fm->is_operand()) fprintf(stderr,  "in form '%s'", fm->is_operand()->_ident);
          fprintf(stderr,  "\n");
        }
        assert(position < num_opnds, "advertised index in bounds");
        return position;
      }
    }
    if( component->isa(Component::DEF)
        && component->isa(Component::USE) ) {
      ++position;
      if( position != 1 )  --position;   // only use two slots for the 1st USE_DEF
    }
    if( component->isa(Component::DEF) && !first_def ) {
      first_def = component;
    }
    if( !component->isa(Component::USE) && component != first_def ) {
      preceding_non_use = component;
    } else if( preceding_non_use && !strcmp(component->_name, preceding_non_use->_name) ) {
      preceding_non_use = NULL;
    }
  }
  return Not_in_list;
}

// Find position for this name, regardless of use/def information
int ComponentList::operand_position(const char *name) {
  PreserveIter pi(this);
  int position = 0;
  Component *component;
  for (reset(); (component = iter()) != NULL; ++position) {
    // When the first component is not a DEF,
    // leave space for the result operand!
    if ( position==0 && (! component->isa(Component::DEF)) ) {
      ++position;
    }
    if (strcmp(name, component->_name)==0) {
      return position;
    }
    if( component->isa(Component::DEF)
        && component->isa(Component::USE) ) {
      ++position;
      if( position != 1 )  --position;   // only use two slots for the 1st USE_DEF
    }
  }
  return Not_in_list;
}

int ComponentList::operand_position_format(const char *name, Form *fm) {
  PreserveIter pi(this);
  int  first_position = operand_position(name);
  int  use_position   = operand_position(name, Component::USE, fm);

  return ((first_position < use_position) ? use_position : first_position);
}

int ComponentList::label_position() {
  PreserveIter pi(this);
  int position = 0;
  reset();
  for( Component *comp; (comp = iter()) != NULL; ++position) {
    // When the first component is not a DEF,
    // leave space for the result operand!
    if ( position==0 && (! comp->isa(Component::DEF)) ) {
      ++position;
    }
    if (strcmp(comp->_type, "label")==0) {
      return position;
    }
    if( comp->isa(Component::DEF)
        && comp->isa(Component::USE) ) {
      ++position;
      if( position != 1 )  --position;   // only use two slots for the 1st USE_DEF
    }
  }

  return -1;
}

int ComponentList::method_position() {
  PreserveIter pi(this);
  int position = 0;
  reset();
  for( Component *comp; (comp = iter()) != NULL; ++position) {
    // When the first component is not a DEF,
    // leave space for the result operand!
    if ( position==0 && (! comp->isa(Component::DEF)) ) {
      ++position;
    }
    if (strcmp(comp->_type, "method")==0) {
      return position;
    }
    if( comp->isa(Component::DEF)
        && comp->isa(Component::USE) ) {
      ++position;
      if( position != 1 )  --position;   // only use two slots for the 1st USE_DEF
    }
  }

  return -1;
}

void ComponentList::dump() { output(stderr); }

void ComponentList::output(FILE *fp) {
  PreserveIter pi(this);
  fprintf(fp, "\n");
  Component *component;
  for (reset(); (component = iter()) != NULL;) {
    component->output(fp);
  }
  fprintf(fp, "\n");
}

//------------------------------MatchNode--------------------------------------
MatchNode::MatchNode(ArchDesc &ad, const char *result, const char *mexpr,
                     const char *opType, MatchNode *lChild, MatchNode *rChild)
  : _AD(ad), _result(result), _name(mexpr), _opType(opType),
    _lChild(lChild), _rChild(rChild), _internalop(0), _numleaves(0),
    _commutative_id(0) {
  _numleaves = (lChild ? lChild->_numleaves : 0)
               + (rChild ? rChild->_numleaves : 0);
}

MatchNode::MatchNode(ArchDesc &ad, MatchNode& mnode)
  : _AD(ad), _result(mnode._result), _name(mnode._name),
    _opType(mnode._opType), _lChild(mnode._lChild), _rChild(mnode._rChild),
    _internalop(0), _numleaves(mnode._numleaves),
    _commutative_id(mnode._commutative_id) {
}

MatchNode::MatchNode(ArchDesc &ad, MatchNode& mnode, int clone)
  : _AD(ad), _result(mnode._result), _name(mnode._name),
    _opType(mnode._opType),
    _internalop(0), _numleaves(mnode._numleaves),
    _commutative_id(mnode._commutative_id) {
  if (mnode._lChild) {
    _lChild = new MatchNode(ad, *mnode._lChild, clone);
  } else {
    _lChild = NULL;
  }
  if (mnode._rChild) {
    _rChild = new MatchNode(ad, *mnode._rChild, clone);
  } else {
    _rChild = NULL;
  }
}

MatchNode::~MatchNode() {
  // // This node may not own its children if copied via assignment
  // if( _lChild ) delete _lChild;
  // if( _rChild ) delete _rChild;
}

bool  MatchNode::find_type(const char *type, int &position) const {
  if ( (_lChild != NULL) && (_lChild->find_type(type, position)) ) return true;
  if ( (_rChild != NULL) && (_rChild->find_type(type, position)) ) return true;

  if (strcmp(type,_opType)==0)  {
    return true;
  } else {
    ++position;
  }
  return false;
}

// Recursive call collecting info on top-level operands, not transitive.
// Implementation does not modify state of internal structures.
void MatchNode::append_components(FormDict& locals, ComponentList& components,
                                  bool def_flag) const {
  int usedef = def_flag ? Component::DEF : Component::USE;
  FormDict &globals = _AD.globalNames();

  assert (_name != NULL, "MatchNode::build_components encountered empty node\n");
  // Base case
  if (_lChild==NULL && _rChild==NULL) {
    // If _opType is not an operation, do not build a component for it #####
    const Form *f = globals[_opType];
    if( f != NULL ) {
      // Add non-ideals that are operands, operand-classes,
      if( ! f->ideal_only()
          && (f->is_opclass() || f->is_operand()) ) {
        components.insert(_name, _opType, usedef, true);
      }
    }
    return;
  }
  // Promote results of "Set" to DEF
  bool tmpdef_flag = (!strcmp(_opType, "Set")) ? true : false;
  if (_lChild) _lChild->append_components(locals, components, tmpdef_flag);
  tmpdef_flag = false;   // only applies to component immediately following 'Set'
  if (_rChild) _rChild->append_components(locals, components, tmpdef_flag);
}

// Find the n'th base-operand in the match node,
// recursively investigates match rules of user-defined operands.
//
// Implementation does not modify state of internal structures since they
// can be shared.
bool MatchNode::base_operand(uint &position, FormDict &globals,
                             const char * &result, const char * &name,
                             const char * &opType) const {
  assert (_name != NULL, "MatchNode::base_operand encountered empty node\n");
  // Base case
  if (_lChild==NULL && _rChild==NULL) {
    // Check for special case: "Universe", "label"
    if (strcmp(_opType,"Universe") == 0 || strcmp(_opType,"label")==0 ) {
      if (position == 0) {
        result = _result;
        name   = _name;
        opType = _opType;
        return 1;
      } else {
        -- position;
        return 0;
      }
    }

    const Form *form = globals[_opType];
    MatchNode *matchNode = NULL;
    // Check for user-defined type
    if (form) {
      // User operand or instruction?
      OperandForm  *opForm = form->is_operand();
      InstructForm *inForm = form->is_instruction();
      if ( opForm ) {
        matchNode = (MatchNode*)opForm->_matrule;
      } else if ( inForm ) {
        matchNode = (MatchNode*)inForm->_matrule;
      }
    }
    // if this is user-defined, recurse on match rule
    // User-defined operand and instruction forms have a match-rule.
    if (matchNode) {
      return (matchNode->base_operand(position,globals,result,name,opType));
    } else {
      // Either not a form, or a system-defined form (no match rule).
      if (position==0) {
        result = _result;
        name   = _name;
        opType = _opType;
        return 1;
      } else {
        --position;
        return 0;
      }
    }

  } else {
    // Examine the left child and right child as well
    if (_lChild) {
      if (_lChild->base_operand(position, globals, result, name, opType))
        return 1;
    }

    if (_rChild) {
      if (_rChild->base_operand(position, globals, result, name, opType))
        return 1;
    }
  }

  return 0;
}

// Recursive call on all operands' match rules in my match rule.
uint  MatchNode::num_consts(FormDict &globals) const {
  uint        index      = 0;
  uint        num_consts = 0;
  const char *result;
  const char *name;
  const char *opType;

  for (uint position = index;
       base_operand(position,globals,result,name,opType); position = index) {
    ++index;
    if( ideal_to_const_type(opType) )        num_consts++;
  }

  return num_consts;
}

// Recursive call on all operands' match rules in my match rule.
// Constants in match rule subtree with specified type
uint  MatchNode::num_consts(FormDict &globals, Form::DataType type) const {
  uint        index      = 0;
  uint        num_consts = 0;
  const char *result;
  const char *name;
  const char *opType;

  for (uint position = index;
       base_operand(position,globals,result,name,opType); position = index) {
    ++index;
    if( ideal_to_const_type(opType) == type ) num_consts++;
  }

  return num_consts;
}

// Recursive call on all operands' match rules in my match rule.
uint  MatchNode::num_const_ptrs(FormDict &globals) const {
  return  num_consts( globals, Form::idealP );
}

bool  MatchNode::sets_result() const {
  return   ( (strcmp(_name,"Set") == 0) ? true : false );
}

const char *MatchNode::reduce_right(FormDict &globals) const {
  // If there is no right reduction, return NULL.
  const char      *rightStr    = NULL;

  // If we are a "Set", start from the right child.
  const MatchNode *const mnode = sets_result() ?
    (const MatchNode *)this->_rChild :
    (const MatchNode *)this;

  // If our right child exists, it is the right reduction
  if ( mnode->_rChild ) {
    rightStr = mnode->_rChild->_internalop ? mnode->_rChild->_internalop
      : mnode->_rChild->_opType;
  }
  // Else, May be simple chain rule: (Set dst operand_form), rightStr=NULL;
  return rightStr;
}

const char *MatchNode::reduce_left(FormDict &globals) const {
  // If there is no left reduction, return NULL.
  const char  *leftStr  = NULL;

  // If we are a "Set", start from the right child.
  const MatchNode *const mnode = sets_result() ?
    (const MatchNode *)this->_rChild :
    (const MatchNode *)this;

  // If our left child exists, it is the left reduction
  if ( mnode->_lChild ) {
    leftStr = mnode->_lChild->_internalop ? mnode->_lChild->_internalop
      : mnode->_lChild->_opType;
  } else {
    // May be simple chain rule: (Set dst operand_form_source)
    if ( sets_result() ) {
      OperandForm *oper = globals[mnode->_opType]->is_operand();
      if( oper ) {
        leftStr = mnode->_opType;
      }
    }
  }
  return leftStr;
}

//------------------------------count_instr_names------------------------------
// Count occurrences of operands names in the leaves of the instruction
// match rule.
void MatchNode::count_instr_names( Dict &names ) {
  if( _lChild ) _lChild->count_instr_names(names);
  if( _rChild ) _rChild->count_instr_names(names);
  if( !_lChild && !_rChild ) {
    uintptr_t cnt = (uintptr_t)names[_name];
    cnt++;                      // One more name found
    names.Insert(_name,(void*)cnt);
  }
}

//------------------------------build_instr_pred-------------------------------
// Build a path to 'name' in buf.  Actually only build if cnt is zero, so we
// can skip some leading instances of 'name'.
int MatchNode::build_instr_pred( char *buf, const char *name, int cnt, int path_bitmask, int level) {
  if( _lChild ) {
    cnt = _lChild->build_instr_pred(buf, name, cnt, path_bitmask, level+1);
    if( cnt < 0 ) {
      return cnt;   // Found it, all done
    }
  }
  if( _rChild ) {
    path_bitmask |= 1 << level;
    cnt = _rChild->build_instr_pred( buf, name, cnt, path_bitmask, level+1);
    if( cnt < 0 ) {
      return cnt;   // Found it, all done
    }
  }
  if( !_lChild && !_rChild ) {  // Found a leaf
    // Wrong name?  Give up...
    if( strcmp(name,_name) ) return cnt;
    if( !cnt )  {
      for(int i = 0; i < level; i++) {
        int kid = path_bitmask &  (1 << i);
        if (0 == kid) {
          strcpy( buf, "_kids[0]->" );
        } else {
          strcpy( buf, "_kids[1]->" );
        }
        buf += 10;
      }
      strcpy( buf, "_leaf" );
    }
    return cnt-1;
  }
  return cnt;
}


//------------------------------build_internalop-------------------------------
// Build string representation of subtree
void MatchNode::build_internalop( ) {
  char *iop, *subtree;
  const char *lstr, *rstr;
  // Build string representation of subtree
  // Operation lchildType rchildType
  int len = (int)strlen(_opType) + 4;
  lstr = (_lChild) ? ((_lChild->_internalop) ?
                       _lChild->_internalop : _lChild->_opType) : "";
  rstr = (_rChild) ? ((_rChild->_internalop) ?
                       _rChild->_internalop : _rChild->_opType) : "";
  len += (int)strlen(lstr) + (int)strlen(rstr);
  subtree = (char *)AllocateHeap(len);
  sprintf(subtree,"_%s_%s_%s", _opType, lstr, rstr);
  // Hash the subtree string in _internalOps; if a name exists, use it
  iop = (char *)_AD._internalOps[subtree];
  // Else create a unique name, and add it to the hash table
  if (iop == NULL) {
    iop = subtree;
    _AD._internalOps.Insert(subtree, iop);
    _AD._internalOpNames.addName(iop);
    _AD._internalMatch.Insert(iop, this);
  }
  // Add the internal operand name to the MatchNode
  _internalop = iop;
  _result = iop;
}


void MatchNode::dump() {
  output(stderr);
}

void MatchNode::output(FILE *fp) {
  if (_lChild==0 && _rChild==0) {
    fprintf(fp," %s",_name);    // operand
  }
  else {
    fprintf(fp," (%s ",_name);  // " (opcodeName "
    if(_lChild) _lChild->output(fp); //               left operand
    if(_rChild) _rChild->output(fp); //                    right operand
    fprintf(fp,")");                 //                                 ")"
  }
}

int MatchNode::needs_ideal_memory_edge(FormDict &globals) const {
  static const char *needs_ideal_memory_list[] = {
    "StoreI","StoreL","StoreP","StoreN","StoreNKlass","StoreD","StoreF" ,
    "StoreB","StoreC","Store" ,"StoreFP",
    "LoadI", "LoadL", "LoadP" ,"LoadN", "LoadD" ,"LoadF"  ,
    "LoadB" , "LoadUB", "LoadUS" ,"LoadS" ,"Load" ,
    "StoreVector", "LoadVector", "LoadVectorGather", "StoreVectorScatter", "LoadVectorMasked", "StoreVectorMasked",
    "LoadRange", "LoadKlass", "LoadNKlass", "LoadL_unaligned", "LoadD_unaligned",
    "LoadPLocked",
    "StorePConditional", "StoreIConditional", "StoreLConditional",
    "CompareAndSwapB", "CompareAndSwapS", "CompareAndSwapI", "CompareAndSwapL", "CompareAndSwapP", "CompareAndSwapN",
    "WeakCompareAndSwapB", "WeakCompareAndSwapS", "WeakCompareAndSwapI", "WeakCompareAndSwapL", "WeakCompareAndSwapP", "WeakCompareAndSwapN",
    "CompareAndExchangeB", "CompareAndExchangeS", "CompareAndExchangeI", "CompareAndExchangeL", "CompareAndExchangeP", "CompareAndExchangeN",
#if INCLUDE_SHENANDOAHGC
    "ShenandoahCompareAndSwapN", "ShenandoahCompareAndSwapP", "ShenandoahWeakCompareAndSwapP", "ShenandoahWeakCompareAndSwapN", "ShenandoahCompareAndExchangeP", "ShenandoahCompareAndExchangeN",
#endif
    "StoreCM",
    "GetAndSetB", "GetAndSetS", "GetAndAddI", "GetAndSetI", "GetAndSetP",
    "GetAndAddB", "GetAndAddS", "GetAndAddL", "GetAndSetL", "GetAndSetN",
    "ClearArray"
  };
  int cnt = sizeof(needs_ideal_memory_list)/sizeof(char*);
  if( strcmp(_opType,"PrefetchAllocation")==0 )
    return 1;
  if( strcmp(_opType,"CacheWB")==0 )
    return 1;
  if( strcmp(_opType,"CacheWBPreSync")==0 )
    return 1;
  if( strcmp(_opType,"CacheWBPostSync")==0 )
    return 1;
  if( _lChild ) {
    const char *opType = _lChild->_opType;
    for( int i=0; i<cnt; i++ )
      if( strcmp(opType,needs_ideal_memory_list[i]) == 0 )
        return 1;
    if( _lChild->needs_ideal_memory_edge(globals) )
      return 1;
  }
  if( _rChild ) {
    const char *opType = _rChild->_opType;
    for( int i=0; i<cnt; i++ )
      if( strcmp(opType,needs_ideal_memory_list[i]) == 0 )
        return 1;
    if( _rChild->needs_ideal_memory_edge(globals) )
      return 1;
  }

  return 0;
}

// TRUE if defines a derived oop, and so needs a base oop edge present
// post-matching.
int MatchNode::needs_base_oop_edge() const {
  if( !strcmp(_opType,"AddP") ) return 1;
  if( strcmp(_opType,"Set") ) return 0;
  return !strcmp(_rChild->_opType,"AddP");
}

int InstructForm::needs_base_oop_edge(FormDict &globals) const {
  if( is_simple_chain_rule(globals) ) {
    const char *src = _matrule->_rChild->_opType;
    OperandForm *src_op = globals[src]->is_operand();
    assert( src_op, "Not operand class of chain rule" );
    return src_op->_matrule ? src_op->_matrule->needs_base_oop_edge() : 0;
  }                             // Else check instruction

  return _matrule ? _matrule->needs_base_oop_edge() : 0;
}


//-------------------------cisc spilling methods-------------------------------
// helper routines and methods for detecting cisc-spilling instructions
//-------------------------cisc_spill_merge------------------------------------
int MatchNode::cisc_spill_merge(int left_spillable, int right_spillable) {
  int cisc_spillable  = Maybe_cisc_spillable;

  // Combine results of left and right checks
  if( (left_spillable == Maybe_cisc_spillable) && (right_spillable == Maybe_cisc_spillable) ) {
    // neither side is spillable, nor prevents cisc spilling
    cisc_spillable = Maybe_cisc_spillable;
  }
  else if( (left_spillable == Maybe_cisc_spillable) && (right_spillable > Maybe_cisc_spillable) ) {
    // right side is spillable
    cisc_spillable = right_spillable;
  }
  else if( (right_spillable == Maybe_cisc_spillable) && (left_spillable > Maybe_cisc_spillable) ) {
    // left side is spillable
    cisc_spillable = left_spillable;
  }
  else if( (left_spillable == Not_cisc_spillable) || (right_spillable == Not_cisc_spillable) ) {
    // left or right prevents cisc spilling this instruction
    cisc_spillable = Not_cisc_spillable;
  }
  else {
    // Only allow one to spill
    cisc_spillable = Not_cisc_spillable;
  }

  return cisc_spillable;
}

//-------------------------root_ops_match--------------------------------------
bool static root_ops_match(FormDict &globals, const char *op1, const char *op2) {
  // Base Case: check that the current operands/operations match
  assert( op1, "Must have op's name");
  assert( op2, "Must have op's name");
  const Form *form1 = globals[op1];
  const Form *form2 = globals[op2];

  return (form1 == form2);
}

//-------------------------cisc_spill_match_node-------------------------------
// Recursively check two MatchRules for legal conversion via cisc-spilling
int MatchNode::cisc_spill_match(FormDict& globals, RegisterForm* registers, MatchNode* mRule2, const char* &operand, const char* &reg_type) {
  int cisc_spillable  = Maybe_cisc_spillable;
  int left_spillable  = Maybe_cisc_spillable;
  int right_spillable = Maybe_cisc_spillable;

  // Check that each has same number of operands at this level
  if( (_lChild && !(mRule2->_lChild)) || (_rChild && !(mRule2->_rChild)) )
    return Not_cisc_spillable;

  // Base Case: check that the current operands/operations match
  // or are CISC spillable
  assert( _opType, "Must have _opType");
  assert( mRule2->_opType, "Must have _opType");
  const Form *form  = globals[_opType];
  const Form *form2 = globals[mRule2->_opType];
  if( form == form2 ) {
    cisc_spillable = Maybe_cisc_spillable;
  } else {
    const InstructForm *form2_inst = form2 ? form2->is_instruction() : NULL;
    const char *name_left  = mRule2->_lChild ? mRule2->_lChild->_opType : NULL;
    const char *name_right = mRule2->_rChild ? mRule2->_rChild->_opType : NULL;
    DataType data_type = Form::none;
    if (form->is_operand()) {
      // Make sure the loadX matches the type of the reg
      data_type = form->ideal_to_Reg_type(form->is_operand()->ideal_type(globals));
    }
    // Detect reg vs (loadX memory)
    if( form->is_cisc_reg(globals)
        && form2_inst
        && data_type != Form::none
        && (is_load_from_memory(mRule2->_opType) == data_type) // reg vs. (load memory)
        && (name_left != NULL)       // NOT (load)
        && (name_right == NULL) ) {  // NOT (load memory foo)
      const Form *form2_left = globals[name_left];
      if( form2_left && form2_left->is_cisc_mem(globals) ) {
        cisc_spillable = Is_cisc_spillable;
        operand        = _name;
        reg_type       = _result;
        return Is_cisc_spillable;
      } else {
        cisc_spillable = Not_cisc_spillable;
      }
    }
    // Detect reg vs memory
    else if (form->is_cisc_reg(globals) && form2 != NULL && form2->is_cisc_mem(globals)) {
      cisc_spillable = Is_cisc_spillable;
      operand        = _name;
      reg_type       = _result;
      return Is_cisc_spillable;
    } else {
      cisc_spillable = Not_cisc_spillable;
    }
  }

  // If cisc is still possible, check rest of tree
  if( cisc_spillable == Maybe_cisc_spillable ) {
    // Check that each has same number of operands at this level
    if( (_lChild && !(mRule2->_lChild)) || (_rChild && !(mRule2->_rChild)) ) return Not_cisc_spillable;

    // Check left operands
    if( (_lChild == NULL) && (mRule2->_lChild == NULL) ) {
      left_spillable = Maybe_cisc_spillable;
    } else  if (_lChild != NULL) {
      left_spillable = _lChild->cisc_spill_match(globals, registers, mRule2->_lChild, operand, reg_type);
    }

    // Check right operands
    if( (_rChild == NULL) && (mRule2->_rChild == NULL) ) {
      right_spillable =  Maybe_cisc_spillable;
    } else if (_rChild != NULL) {
      right_spillable = _rChild->cisc_spill_match(globals, registers, mRule2->_rChild, operand, reg_type);
    }

    // Combine results of left and right checks
    cisc_spillable = cisc_spill_merge(left_spillable, right_spillable);
  }

  return cisc_spillable;
}

//---------------------------cisc_spill_match_rule------------------------------
// Recursively check two MatchRules for legal conversion via cisc-spilling
// This method handles the root of Match tree,
// general recursive checks done in MatchNode
int  MatchRule::matchrule_cisc_spill_match(FormDict& globals, RegisterForm* registers,
                                           MatchRule* mRule2, const char* &operand,
                                           const char* &reg_type) {
  int cisc_spillable  = Maybe_cisc_spillable;
  int left_spillable  = Maybe_cisc_spillable;
  int right_spillable = Maybe_cisc_spillable;

  // Check that each sets a result
  if( !(sets_result() && mRule2->sets_result()) ) return Not_cisc_spillable;
  // Check that each has same number of operands at this level
  if( (_lChild && !(mRule2->_lChild)) || (_rChild && !(mRule2->_rChild)) ) return Not_cisc_spillable;

  // Check left operands: at root, must be target of 'Set'
  if( (_lChild == NULL) || (mRule2->_lChild == NULL) ) {
    left_spillable = Not_cisc_spillable;
  } else {
    // Do not support cisc-spilling instruction's target location
    if( root_ops_match(globals, _lChild->_opType, mRule2->_lChild->_opType) ) {
      left_spillable = Maybe_cisc_spillable;
    } else {
      left_spillable = Not_cisc_spillable;
    }
  }

  // Check right operands: recursive walk to identify reg->mem operand
  if (_rChild == NULL) {
    if (mRule2->_rChild == NULL) {
      right_spillable =  Maybe_cisc_spillable;
    } else {
      assert(0, "_rChild should not be NULL");
    }
  } else {
    right_spillable = _rChild->cisc_spill_match(globals, registers, mRule2->_rChild, operand, reg_type);
  }

  // Combine results of left and right checks
  cisc_spillable = cisc_spill_merge(left_spillable, right_spillable);

  return cisc_spillable;
}

//----------------------------- equivalent ------------------------------------
// Recursively check to see if two match rules are equivalent.
// This rule handles the root.
bool MatchRule::equivalent(FormDict &globals, MatchNode *mRule2) {
  // Check that each sets a result
  if (sets_result() != mRule2->sets_result()) {
    return false;
  }

  // Check that the current operands/operations match
  assert( _opType, "Must have _opType");
  assert( mRule2->_opType, "Must have _opType");
  const Form *form  = globals[_opType];
  const Form *form2 = globals[mRule2->_opType];
  if( form != form2 ) {
    return false;
  }

  if (_lChild ) {
    if( !_lChild->equivalent(globals, mRule2->_lChild) )
      return false;
  } else if (mRule2->_lChild) {
    return false; // I have NULL left child, mRule2 has non-NULL left child.
  }

  if (_rChild ) {
    if( !_rChild->equivalent(globals, mRule2->_rChild) )
      return false;
  } else if (mRule2->_rChild) {
    return false; // I have NULL right child, mRule2 has non-NULL right child.
  }

  // We've made it through the gauntlet.
  return true;
}

//----------------------------- equivalent ------------------------------------
// Recursively check to see if two match rules are equivalent.
// This rule handles the operands.
bool MatchNode::equivalent(FormDict &globals, MatchNode *mNode2) {
  if( !mNode2 )
    return false;

  // Check that the current operands/operations match
  assert( _opType, "Must have _opType");
  assert( mNode2->_opType, "Must have _opType");
  const Form *form  = globals[_opType];
  const Form *form2 = globals[mNode2->_opType];
  if( form != form2 ) {
    return false;
  }

  // Check that their children also match
  if (_lChild ) {
    if( !_lChild->equivalent(globals, mNode2->_lChild) )
      return false;
  } else if (mNode2->_lChild) {
    return false; // I have NULL left child, mNode2 has non-NULL left child.
  }

  if (_rChild ) {
    if( !_rChild->equivalent(globals, mNode2->_rChild) )
      return false;
  } else if (mNode2->_rChild) {
    return false; // I have NULL right child, mNode2 has non-NULL right child.
  }

  // We've made it through the gauntlet.
  return true;
}

//-------------------------- has_commutative_op -------------------------------
// Recursively check for commutative operations with subtree operands
// which could be swapped.
void MatchNode::count_commutative_op(int& count) {
  static const char *commut_op_list[] = {
    "AddI","AddL","AddF","AddD",
    "AddVB","AddVS","AddVI","AddVL","AddVF","AddVD",
    "AndI","AndL",
    "AndV",
    "MaxI","MinI","MaxF","MinF","MaxD","MinD",
    "MaxV", "MinV",
    "MulI","MulL","MulF","MulD",
    "MulVB","MulVS","MulVI","MulVL","MulVF","MulVD",
    "OrI","OrL",
    "OrV",
    "XorI","XorL",
    "XorV"
  };
  int cnt = sizeof(commut_op_list)/sizeof(char*);

  if( _lChild && _rChild && (_lChild->_lChild || _rChild->_lChild) ) {
    // Don't swap if right operand is an immediate constant.
    bool is_const = false;
    if( _rChild->_lChild == NULL && _rChild->_rChild == NULL ) {
      FormDict &globals = _AD.globalNames();
      const Form *form = globals[_rChild->_opType];
      if ( form ) {
        OperandForm  *oper = form->is_operand();
        if( oper && oper->interface_type(globals) == Form::constant_interface )
          is_const = true;
      }
    }
    if( !is_const ) {
      for( int i=0; i<cnt; i++ ) {
        if( strcmp(_opType, commut_op_list[i]) == 0 ) {
          count++;
          _commutative_id = count; // id should be > 0
          break;
        }
      }
    }
  }
  if( _lChild )
    _lChild->count_commutative_op(count);
  if( _rChild )
    _rChild->count_commutative_op(count);
}

//-------------------------- swap_commutative_op ------------------------------
// Recursively swap specified commutative operation with subtree operands.
void MatchNode::swap_commutative_op(bool atroot, int id) {
  if( _commutative_id == id ) { // id should be > 0
    assert(_lChild && _rChild && (_lChild->_lChild || _rChild->_lChild ),
            "not swappable operation");
    MatchNode* tmp = _lChild;
    _lChild = _rChild;
    _rChild = tmp;
    // Don't exit here since we need to build internalop.
  }

  bool is_set = ( strcmp(_opType, "Set") == 0 );
  if( _lChild )
    _lChild->swap_commutative_op(is_set, id);
  if( _rChild )
    _rChild->swap_commutative_op(is_set, id);

  // If not the root, reduce this subtree to an internal operand
  if( !atroot && (_lChild || _rChild) ) {
    build_internalop();
  }
}

//-------------------------- swap_commutative_op ------------------------------
// Recursively swap specified commutative operation with subtree operands.
void MatchRule::matchrule_swap_commutative_op(const char* instr_ident, int count, int& match_rules_cnt) {
  assert(match_rules_cnt < 100," too many match rule clones");
  // Clone
  MatchRule* clone = new MatchRule(_AD, this);
  // Swap operands of commutative operation
  ((MatchNode*)clone)->swap_commutative_op(true, count);
  char* buf = (char*) AllocateHeap(strlen(instr_ident) + 4);
  sprintf(buf, "%s_%d", instr_ident, match_rules_cnt++);
  clone->_result = buf;

  clone->_next = this->_next;
  this-> _next = clone;
  if( (--count) > 0 ) {
    this-> matchrule_swap_commutative_op(instr_ident, count, match_rules_cnt);
    clone->matchrule_swap_commutative_op(instr_ident, count, match_rules_cnt);
  }
}

//------------------------------MatchRule--------------------------------------
MatchRule::MatchRule(ArchDesc &ad)
  : MatchNode(ad), _depth(0), _construct(NULL), _numchilds(0) {
    _next = NULL;
}

MatchRule::MatchRule(ArchDesc &ad, MatchRule* mRule)
  : MatchNode(ad, *mRule, 0), _depth(mRule->_depth),
    _construct(mRule->_construct), _numchilds(mRule->_numchilds) {
    _next = NULL;
}

MatchRule::MatchRule(ArchDesc &ad, MatchNode* mroot, int depth, char *cnstr,
                     int numleaves)
  : MatchNode(ad,*mroot), _depth(depth), _construct(cnstr),
    _numchilds(0) {
      _next = NULL;
      mroot->_lChild = NULL;
      mroot->_rChild = NULL;
      delete mroot;
      _numleaves = numleaves;
      _numchilds = (_lChild ? 1 : 0) + (_rChild ? 1 : 0);
}
MatchRule::~MatchRule() {
}

// Recursive call collecting info on top-level operands, not transitive.
// Implementation does not modify state of internal structures.
void MatchRule::append_components(FormDict& locals, ComponentList& components, bool def_flag) const {
  assert (_name != NULL, "MatchNode::build_components encountered empty node\n");

  MatchNode::append_components(locals, components,
                               false /* not necessarily a def */);
}

// Recursive call on all operands' match rules in my match rule.
// Implementation does not modify state of internal structures  since they
// can be shared.
// The MatchNode that is called first treats its
bool MatchRule::base_operand(uint &position0, FormDict &globals,
                             const char *&result, const char * &name,
                             const char * &opType)const{
  uint position = position0;

  return (MatchNode::base_operand( position, globals, result, name, opType));
}


bool MatchRule::is_base_register(FormDict &globals) const {
  uint   position = 1;
  const char  *result   = NULL;
  const char  *name     = NULL;
  const char  *opType   = NULL;
  if (!base_operand(position, globals, result, name, opType)) {
    position = 0;
    if( base_operand(position, globals, result, name, opType) &&
        (strcmp(opType,"RegI")==0 ||
         strcmp(opType,"RegP")==0 ||
         strcmp(opType,"RegN")==0 ||
         strcmp(opType,"RegL")==0 ||
         strcmp(opType,"RegF")==0 ||
         strcmp(opType,"RegD")==0 ||
         strcmp(opType,"RegVectMask")==0 ||
         strcmp(opType,"VecA")==0 ||
         strcmp(opType,"VecS")==0 ||
         strcmp(opType,"VecD")==0 ||
         strcmp(opType,"VecX")==0 ||
         strcmp(opType,"VecY")==0 ||
         strcmp(opType,"VecZ")==0 ||
         strcmp(opType,"Reg" )==0) ) {
      return 1;
    }
  }
  return 0;
}

Form::DataType MatchRule::is_base_constant(FormDict &globals) const {
  uint         position = 1;
  const char  *result   = NULL;
  const char  *name     = NULL;
  const char  *opType   = NULL;
  if (!base_operand(position, globals, result, name, opType)) {
    position = 0;
    if (base_operand(position, globals, result, name, opType)) {
      return ideal_to_const_type(opType);
    }
  }
  return Form::none;
}

bool MatchRule::is_chain_rule(FormDict &globals) const {

  // Check for chain rule, and do not generate a match list for it
  if ((_lChild == NULL) && (_rChild == NULL) ) {
    const Form *form = globals[_opType];
    // If this is ideal, then it is a base match, not a chain rule.
    if ( form && form->is_operand() && (!form->ideal_only())) {
      return true;
    }
  }
  // Check for "Set" form of chain rule, and do not generate a match list
  if (_rChild) {
    const char *rch = _rChild->_opType;
    const Form *form = globals[rch];
    if ((!strcmp(_opType,"Set") &&
         ((form) && form->is_operand()))) {
      return true;
    }
  }
  return false;
}

int MatchRule::is_ideal_copy() const {
  if (is_chain_rule(_AD.globalNames()) &&
      _lChild && strncmp(_lChild->_opType, "stackSlot", 9) == 0) {
    return 1;
  }
  return 0;
}

int MatchRule::is_expensive() const {
  if( _rChild ) {
    const char  *opType = _rChild->_opType;
    if( strcmp(opType,"AtanD")==0 ||
        strcmp(opType,"DivD")==0 ||
        strcmp(opType,"DivF")==0 ||
        strcmp(opType,"DivI")==0 ||
        strcmp(opType,"Log10D")==0 ||
        strcmp(opType,"ModD")==0 ||
        strcmp(opType,"ModF")==0 ||
        strcmp(opType,"ModI")==0 ||
        strcmp(opType,"SqrtD")==0 ||
        strcmp(opType,"SqrtF")==0 ||
        strcmp(opType,"TanD")==0 ||
        strcmp(opType,"ConvD2F")==0 ||
        strcmp(opType,"ConvD2I")==0 ||
        strcmp(opType,"ConvD2L")==0 ||
        strcmp(opType,"ConvF2D")==0 ||
        strcmp(opType,"ConvF2I")==0 ||
        strcmp(opType,"ConvF2L")==0 ||
        strcmp(opType,"ConvI2D")==0 ||
        strcmp(opType,"ConvI2F")==0 ||
        strcmp(opType,"ConvI2L")==0 ||
        strcmp(opType,"ConvL2D")==0 ||
        strcmp(opType,"ConvL2F")==0 ||
        strcmp(opType,"ConvL2I")==0 ||
        strcmp(opType,"DecodeN")==0 ||
        strcmp(opType,"EncodeP")==0 ||
        strcmp(opType,"EncodePKlass")==0 ||
        strcmp(opType,"DecodeNKlass")==0 ||
        strcmp(opType,"FmaD") == 0 ||
        strcmp(opType,"FmaF") == 0 ||
        strcmp(opType,"RoundDouble")==0 ||
        strcmp(opType,"RoundDoubleMode")==0 ||
        strcmp(opType,"RoundFloat")==0 ||
        strcmp(opType,"ReverseBytesI")==0 ||
        strcmp(opType,"ReverseBytesL")==0 ||
        strcmp(opType,"ReverseBytesUS")==0 ||
        strcmp(opType,"ReverseBytesS")==0 ||
        strcmp(opType,"ReplicateB")==0 ||
        strcmp(opType,"ReplicateS")==0 ||
        strcmp(opType,"ReplicateI")==0 ||
        strcmp(opType,"ReplicateL")==0 ||
        strcmp(opType,"ReplicateF")==0 ||
        strcmp(opType,"ReplicateD")==0 ||
        strcmp(opType,"AddReductionVI")==0 ||
        strcmp(opType,"AddReductionVL")==0 ||
        strcmp(opType,"AddReductionVF")==0 ||
        strcmp(opType,"AddReductionVD")==0 ||
        strcmp(opType,"MulReductionVI")==0 ||
        strcmp(opType,"MulReductionVL")==0 ||
        strcmp(opType,"MulReductionVF")==0 ||
        strcmp(opType,"MulReductionVD")==0 ||
        strcmp(opType,"MinReductionV")==0 ||
        strcmp(opType,"MaxReductionV")==0 ||
        strcmp(opType,"AndReductionV")==0 ||
        strcmp(opType,"OrReductionV")==0 ||
        strcmp(opType,"XorReductionV")==0 ||
        0 /* 0 to line up columns nicely */ )
      return 1;
  }
  return 0;
}

bool MatchRule::is_ideal_if() const {
  if( !_opType ) return false;
  return
    !strcmp(_opType,"If"            ) ||
    !strcmp(_opType,"CountedLoopEnd");
}

bool MatchRule::is_ideal_fastlock() const {
  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    return (strcmp(_rChild->_opType,"FastLock") == 0);
  }
  return false;
}

bool MatchRule::is_ideal_membar() const {
  if( !_opType ) return false;
  return
    !strcmp(_opType,"MemBarAcquire") ||
    !strcmp(_opType,"MemBarRelease") ||
    !strcmp(_opType,"MemBarAcquireLock") ||
    !strcmp(_opType,"MemBarReleaseLock") ||
    !strcmp(_opType,"LoadFence" ) ||
    !strcmp(_opType,"StoreFence") ||
    !strcmp(_opType,"MemBarVolatile") ||
    !strcmp(_opType,"MemBarCPUOrder") ||
    !strcmp(_opType,"MemBarStoreStore") ||
    !strcmp(_opType,"OnSpinWait");
}

bool MatchRule::is_ideal_loadPC() const {
  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    return (strcmp(_rChild->_opType,"LoadPC") == 0);
  }
  return false;
}

bool MatchRule::is_ideal_box() const {
  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    return (strcmp(_rChild->_opType,"Box") == 0);
  }
  return false;
}

bool MatchRule::is_ideal_goto() const {
  bool   ideal_goto = false;

  if( _opType && (strcmp(_opType,"Goto") == 0) ) {
    ideal_goto = true;
  }
  return ideal_goto;
}

bool MatchRule::is_ideal_jump() const {
  if( _opType ) {
    if( !strcmp(_opType,"Jump") )
      return true;
  }
  return false;
}

bool MatchRule::is_ideal_bool() const {
  if( _opType ) {
    if( !strcmp(_opType,"Bool") )
      return true;
  }
  return false;
}


Form::DataType MatchRule::is_ideal_load() const {
  Form::DataType ideal_load = Form::none;

  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    const char *opType = _rChild->_opType;
    ideal_load = is_load_from_memory(opType);
  }

  return ideal_load;
}

bool MatchRule::is_vector() const {
  static const char *vector_list[] = {
    "AddVB","AddVS","AddVI","AddVL","AddVF","AddVD",
    "SubVB","SubVS","SubVI","SubVL","SubVF","SubVD",
    "MulVB","MulVS","MulVI","MulVL","MulVF","MulVD",
    "CMoveVD", "CMoveVF",
    "DivVF","DivVD",
    "AbsVB","AbsVS","AbsVI","AbsVL","AbsVF","AbsVD",
    "NegVF","NegVD","NegVI",
    "SqrtVD","SqrtVF",
    "AndV" ,"XorV" ,"OrV",
    "MaxV", "MinV",
    "AddReductionVI", "AddReductionVL",
    "AddReductionVF", "AddReductionVD",
    "MulReductionVI", "MulReductionVL",
    "MulReductionVF", "MulReductionVD",
    "MaxReductionV", "MinReductionV",
    "AndReductionV", "OrReductionV", "XorReductionV",
    "MulAddVS2VI", "MacroLogicV",
    "LShiftCntV","RShiftCntV",
    "LShiftVB","LShiftVS","LShiftVI","LShiftVL",
    "RShiftVB","RShiftVS","RShiftVI","RShiftVL",
    "URShiftVB","URShiftVS","URShiftVI","URShiftVL",
    "ReplicateB","ReplicateS","ReplicateI","ReplicateL","ReplicateF","ReplicateD",
    "RoundDoubleModeV","RotateLeftV" , "RotateRightV", "LoadVector","StoreVector",
    "LoadVectorGather", "StoreVectorScatter",
    "VectorTest", "VectorLoadMask", "VectorStoreMask", "VectorBlend", "VectorInsert",
    "VectorRearrange","VectorLoadShuffle", "VectorLoadConst",
    "VectorCastB2X", "VectorCastS2X", "VectorCastI2X",
    "VectorCastL2X", "VectorCastF2X", "VectorCastD2X",
    "VectorMaskWrapper", "VectorMaskCmp", "VectorReinterpret","LoadVectorMasked","StoreVectorMasked",
    "FmaVD", "FmaVF","PopCountVI",
    // Next are not supported currently.
    "PackB","PackS","PackI","PackL","PackF","PackD","Pack2L","Pack2D",
    "ExtractB","ExtractUB","ExtractC","ExtractS","ExtractI","ExtractL","ExtractF","ExtractD",
    "VectorMaskCast"
  };
  int cnt = sizeof(vector_list)/sizeof(char*);
  if (_rChild) {
    const char  *opType = _rChild->_opType;
    for (int i=0; i<cnt; i++)
      if (strcmp(opType,vector_list[i]) == 0)
        return true;
  }
  return false;
}


bool MatchRule::skip_antidep_check() const {
  // Some loads operate on what is effectively immutable memory so we
  // should skip the anti dep computations.  For some of these nodes
  // the rewritable field keeps the anti dep logic from triggering but
  // for certain kinds of LoadKlass it does not since they are
  // actually reading memory which could be rewritten by the runtime,
  // though never by generated code.  This disables it uniformly for
  // the nodes that behave like this: LoadKlass, LoadNKlass and
  // LoadRange.
  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    const char *opType = _rChild->_opType;
    if (strcmp("LoadKlass", opType) == 0 ||
        strcmp("LoadNKlass", opType) == 0 ||
        strcmp("LoadRange", opType) == 0) {
      return true;
    }
  }

  return false;
}


Form::DataType MatchRule::is_ideal_store() const {
  Form::DataType ideal_store = Form::none;

  if ( _opType && (strcmp(_opType,"Set") == 0) && _rChild ) {
    const char *opType = _rChild->_opType;
    ideal_store = is_store_to_memory(opType);
  }

  return ideal_store;
}


void MatchRule::dump() {
  output(stderr);
}

// Write just one line.
void MatchRule::output_short(FILE *fp) {
  fprintf(fp,"MatchRule: ( %s",_name);
  if (_lChild) _lChild->output(fp);
  if (_rChild) _rChild->output(fp);
  fprintf(fp," )");
}

void MatchRule::output(FILE *fp) {
  output_short(fp);
  fprintf(fp,"\n   nesting depth = %d\n", _depth);
  if (_result) fprintf(fp,"   Result Type = %s", _result);
  fprintf(fp,"\n");
}

//------------------------------Attribute--------------------------------------
Attribute::Attribute(char *id, char* val, int type)
  : _ident(id), _val(val), _atype(type) {
}
Attribute::~Attribute() {
}

int Attribute::int_val(ArchDesc &ad) {
  // Make sure it is an integer constant:
  int result = 0;
  if (!_val || !ADLParser::is_int_token(_val, result)) {
    ad.syntax_err(0, "Attribute %s must have an integer value: %s",
                  _ident, _val ? _val : "");
  }
  return result;
}

void Attribute::dump() {
  output(stderr);
} // Debug printer

// Write to output files
void Attribute::output(FILE *fp) {
  fprintf(fp,"Attribute: %s  %s\n", (_ident?_ident:""), (_val?_val:""));
}

//------------------------------FormatRule----------------------------------
FormatRule::FormatRule(char *temp)
  : _temp(temp) {
}
FormatRule::~FormatRule() {
}

void FormatRule::dump() {
  output(stderr);
}

// Write to output files
void FormatRule::output(FILE *fp) {
  fprintf(fp,"\nFormat Rule: \n%s", (_temp?_temp:""));
  fprintf(fp,"\n");
}
