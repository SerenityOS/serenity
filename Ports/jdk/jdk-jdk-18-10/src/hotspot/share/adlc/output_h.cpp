/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

// output_h.cpp - Class HPP file output routines for architecture definition
#include "adlc.hpp"

// The comment delimiter used in format statements after assembler instructions.
#if defined(PPC64)
#define commentSeperator "\t//"
#else
#define commentSeperator "!"
#endif

// Generate the #define that describes the number of registers.
static void defineRegCount(FILE *fp, RegisterForm *registers) {
  if (registers) {
    int regCount =  AdlcVMDeps::Physical + registers->_rdefs.count();
    fprintf(fp,"\n");
    fprintf(fp,"// the number of reserved registers + machine registers.\n");
    fprintf(fp,"#define REG_COUNT    %d\n", regCount);
  }
}

// Output enumeration of machine register numbers
// (1)
// // Enumerate machine registers starting after reserved regs.
// // in the order of occurrence in the register block.
// enum MachRegisterNumbers {
//   EAX_num = 0,
//   ...
//   _last_Mach_Reg
// }
void ArchDesc::buildMachRegisterNumbers(FILE *fp_hpp) {
  if (_register) {
    RegDef *reg_def = NULL;

    // Output a #define for the number of machine registers
    defineRegCount(fp_hpp, _register);

    // Count all the Save_On_Entry and Always_Save registers
    int    saved_on_entry = 0;
    int  c_saved_on_entry = 0;
    _register->reset_RegDefs();
    while( (reg_def = _register->iter_RegDefs()) != NULL ) {
      if( strcmp(reg_def->_callconv,"SOE") == 0 ||
          strcmp(reg_def->_callconv,"AS")  == 0 )  ++saved_on_entry;
      if( strcmp(reg_def->_c_conv,"SOE") == 0 ||
          strcmp(reg_def->_c_conv,"AS")  == 0 )  ++c_saved_on_entry;
    }
    fprintf(fp_hpp, "\n");
    fprintf(fp_hpp, "// the number of save_on_entry + always_saved registers.\n");
    fprintf(fp_hpp, "#define MAX_SAVED_ON_ENTRY_REG_COUNT    %d\n",   max(saved_on_entry,c_saved_on_entry));
    fprintf(fp_hpp, "#define     SAVED_ON_ENTRY_REG_COUNT    %d\n",   saved_on_entry);
    fprintf(fp_hpp, "#define   C_SAVED_ON_ENTRY_REG_COUNT    %d\n", c_saved_on_entry);

    // (1)
    // Build definition for enumeration of register numbers
    fprintf(fp_hpp, "\n");
    fprintf(fp_hpp, "// Enumerate machine register numbers starting after reserved regs.\n");
    fprintf(fp_hpp, "// in the order of occurrence in the register block.\n");
    fprintf(fp_hpp, "enum MachRegisterNumbers {\n");

    // Output the register number for each register in the allocation classes
    _register->reset_RegDefs();
    int i = 0;
    while( (reg_def = _register->iter_RegDefs()) != NULL ) {
      fprintf(fp_hpp,"  %s_num,", reg_def->_regname);
      for (int j = 0; j < 20-(int)strlen(reg_def->_regname); j++) fprintf(fp_hpp, " ");
      fprintf(fp_hpp," // enum %3d, regnum %3d, reg encode %3s\n",
              i++,
              reg_def->register_num(),
              reg_def->register_encode());
    }
    // Finish defining enumeration
    fprintf(fp_hpp, "  _last_Mach_Reg            // %d\n", i);
    fprintf(fp_hpp, "};\n");
  }

  fprintf(fp_hpp, "\n// Size of register-mask in ints\n");
  fprintf(fp_hpp, "#define RM_SIZE %d\n", RegisterForm::RegMask_Size());
  fprintf(fp_hpp, "// Unroll factor for loops over the data in a RegMask\n");
  fprintf(fp_hpp, "#define FORALL_BODY ");
  int len = RegisterForm::RegMask_Size();
  for( int i = 0; i < len; i++ )
    fprintf(fp_hpp, "BODY(%d) ",i);
  fprintf(fp_hpp, "\n\n");

  fprintf(fp_hpp,"class RegMask;\n");
  // All RegMasks are declared "extern const ..." in ad_<arch>.hpp
  // fprintf(fp_hpp,"extern RegMask STACK_OR_STACK_SLOTS_mask;\n\n");
}


// Output enumeration of machine register encodings
// (2)
// // Enumerate machine registers starting after reserved regs.
// // in the order of occurrence in the alloc_class(es).
// enum MachRegisterEncodes {
//   EAX_enc = 0x00,
//   ...
// }
void ArchDesc::buildMachRegisterEncodes(FILE *fp_hpp) {
  if (_register) {
    RegDef *reg_def = NULL;
    RegDef *reg_def_next = NULL;

    // (2)
    // Build definition for enumeration of encode values
    fprintf(fp_hpp, "\n");
    fprintf(fp_hpp, "// Enumerate machine registers starting after reserved regs.\n");
    fprintf(fp_hpp, "// in the order of occurrence in the alloc_class(es).\n");
    fprintf(fp_hpp, "enum MachRegisterEncodes {\n");

    // Find max enum string length.
    size_t maxlen = 0;
    _register->reset_RegDefs();
    reg_def = _register->iter_RegDefs();
    while (reg_def != NULL) {
      size_t len = strlen(reg_def->_regname);
      if (len > maxlen) maxlen = len;
      reg_def = _register->iter_RegDefs();
    }

    // Output the register encoding for each register in the allocation classes
    _register->reset_RegDefs();
    reg_def_next = _register->iter_RegDefs();
    while( (reg_def = reg_def_next) != NULL ) {
      reg_def_next = _register->iter_RegDefs();
      fprintf(fp_hpp,"  %s_enc", reg_def->_regname);
      for (size_t i = strlen(reg_def->_regname); i < maxlen; i++) fprintf(fp_hpp, " ");
      fprintf(fp_hpp," = %3s%s\n", reg_def->register_encode(), reg_def_next == NULL? "" : "," );
    }
    // Finish defining enumeration
    fprintf(fp_hpp, "};\n");

  } // Done with register form
}


// Declare an array containing the machine register names, strings.
static void declareRegNames(FILE *fp, RegisterForm *registers) {
  if (registers) {
//    fprintf(fp,"\n");
//    fprintf(fp,"// An array of character pointers to machine register names.\n");
//    fprintf(fp,"extern const char *regName[];\n");
  }
}

// Declare an array containing the machine register sizes in 32-bit words.
void ArchDesc::declareRegSizes(FILE *fp) {
// regSize[] is not used
}

// Declare an array containing the machine register encoding values
static void declareRegEncodes(FILE *fp, RegisterForm *registers) {
  if (registers) {
    // // //
    // fprintf(fp,"\n");
    // fprintf(fp,"// An array containing the machine register encode values\n");
    // fprintf(fp,"extern const char  regEncode[];\n");
  }
}


// ---------------------------------------------------------------------------
//------------------------------Utilities to build Instruction Classes--------
// ---------------------------------------------------------------------------
static void out_RegMask(FILE *fp) {
  fprintf(fp,"  virtual const RegMask &out_RegMask() const;\n");
}

// ---------------------------------------------------------------------------
//--------Utilities to build MachOper and MachNode derived Classes------------
// ---------------------------------------------------------------------------

//------------------------------Utilities to build Operand Classes------------
static void in_RegMask(FILE *fp) {
  fprintf(fp,"  virtual const RegMask *in_RegMask(int index) const;\n");
}

static void declareConstStorage(FILE *fp, FormDict &globals, OperandForm *oper) {
  int i = 0;
  Component *comp;

  if (oper->num_consts(globals) == 0) return;
  // Iterate over the component list looking for constants
  oper->_components.reset();
  if ((comp = oper->_components.iter()) == NULL) {
    assert(oper->num_consts(globals) == 1, "Bad component list detected.\n");
    const char *type = oper->ideal_type(globals);
    if (!strcmp(type, "ConI")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  int32_t        _c%d;\n", i);
    }
    else if (!strcmp(type, "ConP")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  const TypePtr *_c%d;\n", i);
    }
    else if (!strcmp(type, "ConN")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  const TypeNarrowOop *_c%d;\n", i);
    }
    else if (!strcmp(type, "ConNKlass")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  const TypeNarrowKlass *_c%d;\n", i);
    }
    else if (!strcmp(type, "ConL")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  jlong          _c%d;\n", i);
    }
    else if (!strcmp(type, "ConF")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  jfloat         _c%d;\n", i);
    }
    else if (!strcmp(type, "ConD")) {
      if (i > 0) fprintf(fp,", ");
      fprintf(fp,"  jdouble        _c%d;\n", i);
    }
    else if (!strcmp(type, "Bool")) {
      fprintf(fp,"private:\n");
      fprintf(fp,"  BoolTest::mask _c%d;\n", i);
      fprintf(fp,"public:\n");
    }
    else {
      assert(0, "Non-constant operand lacks component list.");
    }
  } // end if NULL
  else {
    oper->_components.reset();
    while ((comp = oper->_components.iter()) != NULL) {
      if (!strcmp(comp->base_type(globals), "ConI")) {
        fprintf(fp,"  jint             _c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConP")) {
        fprintf(fp,"  const TypePtr *_c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConN")) {
        fprintf(fp,"  const TypePtr *_c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConNKlass")) {
        fprintf(fp,"  const TypePtr *_c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConL")) {
        fprintf(fp,"  jlong            _c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConF")) {
        fprintf(fp,"  jfloat           _c%d;\n", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConD")) {
        fprintf(fp,"  jdouble          _c%d;\n", i);
        i++;
      }
    }
  }
}

// Declare constructor.
// Parameters start with condition code, then all other constants
//
// (0) public:
// (1)  MachXOper(int32 ccode, int32 c0, int32 c1, ..., int32 cn)
// (2)     : _ccode(ccode), _c0(c0), _c1(c1), ..., _cn(cn) { }
//
static void defineConstructor(FILE *fp, const char *name, uint num_consts,
                              ComponentList &lst, bool is_ideal_bool,
                              Form::DataType constant_type, FormDict &globals) {
  fprintf(fp,"public:\n");
  // generate line (1)
  fprintf(fp,"  %sOper(", name);
  if( num_consts == 0 ) {
    fprintf(fp,") {}\n");
    return;
  }

  // generate parameters for constants
  uint i = 0;
  Component *comp;
  lst.reset();
  if ((comp = lst.iter()) == NULL) {
    assert(num_consts == 1, "Bad component list detected.\n");
    switch( constant_type ) {
    case Form::idealI : {
      fprintf(fp,is_ideal_bool ? "BoolTest::mask c%d" : "int32_t c%d", i);
      break;
    }
    case Form::idealN :      { fprintf(fp,"const TypeNarrowOop *c%d", i); break; }
    case Form::idealNKlass : { fprintf(fp,"const TypeNarrowKlass *c%d", i); break; }
    case Form::idealP :      { fprintf(fp,"const TypePtr *c%d", i); break; }
    case Form::idealL :      { fprintf(fp,"jlong c%d", i);   break;        }
    case Form::idealF :      { fprintf(fp,"jfloat c%d", i);  break;        }
    case Form::idealD :      { fprintf(fp,"jdouble c%d", i); break;        }
    default:
      assert(!is_ideal_bool, "Non-constant operand lacks component list.");
      break;
    }
  } // end if NULL
  else {
    lst.reset();
    while((comp = lst.iter()) != NULL) {
      if (!strcmp(comp->base_type(globals), "ConI")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"int32_t c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConP")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"const TypePtr *c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConN")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"const TypePtr *c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConNKlass")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"const TypePtr *c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConL")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"jlong c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConF")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"jfloat c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "ConD")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"jdouble c%d", i);
        i++;
      }
      else if (!strcmp(comp->base_type(globals), "Bool")) {
        if (i > 0) fprintf(fp,", ");
        fprintf(fp,"BoolTest::mask c%d", i);
        i++;
      }
    }
  }
  // finish line (1) and start line (2)
  fprintf(fp,")  : ");
  // generate initializers for constants
  i = 0;
  fprintf(fp,"_c%d(c%d)", i, i);
  for( i = 1; i < num_consts; ++i) {
    fprintf(fp,", _c%d(c%d)", i, i);
  }
  // The body for the constructor is empty
  fprintf(fp," {}\n");
}

// ---------------------------------------------------------------------------
// Utilities to generate format rules for machine operands and instructions
// ---------------------------------------------------------------------------

// Generate the format rule for condition codes
static void defineCCodeDump(OperandForm* oper, FILE *fp, int i) {
  assert(oper != NULL, "what");
  CondInterface* cond = oper->_interface->is_CondInterface();
  fprintf(fp, "       if( _c%d == BoolTest::eq ) st->print_raw(\"%s\");\n",i,cond->_equal_format);
  fprintf(fp, "  else if( _c%d == BoolTest::ne ) st->print_raw(\"%s\");\n",i,cond->_not_equal_format);
  fprintf(fp, "  else if( _c%d == BoolTest::le ) st->print_raw(\"%s\");\n",i,cond->_less_equal_format);
  fprintf(fp, "  else if( _c%d == BoolTest::ge ) st->print_raw(\"%s\");\n",i,cond->_greater_equal_format);
  fprintf(fp, "  else if( _c%d == BoolTest::lt ) st->print_raw(\"%s\");\n",i,cond->_less_format);
  fprintf(fp, "  else if( _c%d == BoolTest::gt ) st->print_raw(\"%s\");\n",i,cond->_greater_format);
  fprintf(fp, "  else if( _c%d == BoolTest::overflow ) st->print_raw(\"%s\");\n",i,cond->_overflow_format);
  fprintf(fp, "  else if( _c%d == BoolTest::no_overflow ) st->print_raw(\"%s\");\n",i,cond->_no_overflow_format);
}

// Output code that dumps constant values, increment "i" if type is constant
static uint dump_spec_constant(FILE *fp, const char *ideal_type, uint i, OperandForm* oper) {
  if (!strcmp(ideal_type, "ConI")) {
    fprintf(fp,"   st->print(\"#%%d\", _c%d);\n", i);
    fprintf(fp,"   st->print(\"/0x%%08x\", _c%d);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConP")) {
    fprintf(fp,"    _c%d->dump_on(st);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConN")) {
    fprintf(fp,"    _c%d->dump_on(st);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConNKlass")) {
    fprintf(fp,"    _c%d->dump_on(st);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConL")) {
    fprintf(fp,"    st->print(\"#\" INT64_FORMAT, (int64_t)_c%d);\n", i);
    fprintf(fp,"    st->print(\"/\" PTR64_FORMAT, (uint64_t)_c%d);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConF")) {
    fprintf(fp,"    st->print(\"#%%f\", _c%d);\n", i);
    fprintf(fp,"    jint _c%di = JavaValue(_c%d).get_jint();\n", i, i);
    fprintf(fp,"    st->print(\"/0x%%x/\", _c%di);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "ConD")) {
    fprintf(fp,"    st->print(\"#%%f\", _c%d);\n", i);
    fprintf(fp,"    jlong _c%dl = JavaValue(_c%d).get_jlong();\n", i, i);
    fprintf(fp,"    st->print(\"/\" PTR64_FORMAT, (uint64_t)_c%dl);\n", i);
    ++i;
  }
  else if (!strcmp(ideal_type, "Bool")) {
    defineCCodeDump(oper, fp,i);
    ++i;
  }

  return i;
}

// Generate the format rule for an operand
void gen_oper_format(FILE *fp, FormDict &globals, OperandForm &oper, bool for_c_file = false) {
  if (!for_c_file) {
    // invoked after output #ifndef PRODUCT to ad_<arch>.hpp
    // compile the bodies separately, to cut down on recompilations
    fprintf(fp,"  virtual void           int_format(PhaseRegAlloc *ra, const MachNode *node, outputStream *st) const;\n");
    fprintf(fp,"  virtual void           ext_format(PhaseRegAlloc *ra, const MachNode *node, int idx, outputStream *st) const;\n");
    return;
  }

  // Local pointer indicates remaining part of format rule
  int idx = 0;                   // position of operand in match rule

  // Generate internal format function, used when stored locally
  fprintf(fp, "\n#ifndef PRODUCT\n");
  fprintf(fp,"void %sOper::int_format(PhaseRegAlloc *ra, const MachNode *node, outputStream *st) const {\n", oper._ident);
  // Generate the user-defined portion of the format
  if (oper._format) {
    if ( oper._format->_strings.count() != 0 ) {
      // No initialization code for int_format

      // Build the format from the entries in strings and rep_vars
      const char  *string  = NULL;
      oper._format->_rep_vars.reset();
      oper._format->_strings.reset();
      while ( (string = oper._format->_strings.iter()) != NULL ) {

        // Check if this is a standard string or a replacement variable
        if ( string != NameList::_signal ) {
          // Normal string
          // Pass through to st->print
          fprintf(fp,"  st->print_raw(\"%s\");\n", string);
        } else {
          // Replacement variable
          const char *rep_var = oper._format->_rep_vars.iter();
          // Check that it is a local name, and an operand
          const Form* form = oper._localNames[rep_var];
          if (form == NULL) {
            globalAD->syntax_err(oper._linenum,
                                 "\'%s\' not found in format for %s\n", rep_var, oper._ident);
            assert(form, "replacement variable was not found in local names");
          }
          OperandForm *op      = form->is_operand();
          // Get index if register or constant
          if ( op->_matrule && op->_matrule->is_base_register(globals) ) {
            idx  = oper.register_position( globals, rep_var);
          }
          else if (op->_matrule && op->_matrule->is_base_constant(globals)) {
            idx  = oper.constant_position( globals, rep_var);
          } else {
            idx = 0;
          }

          // output invocation of "$..."s format function
          if ( op != NULL ) op->int_format(fp, globals, idx);

          if ( idx == -1 ) {
            fprintf(stderr,
                    "Using a name, %s, that isn't in match rule\n", rep_var);
            assert( strcmp(op->_ident,"label")==0, "Unimplemented");
          }
        } // Done with a replacement variable
      } // Done with all format strings
    } else {
      // Default formats for base operands (RegI, RegP, ConI, ConP, ...)
      oper.int_format(fp, globals, 0);
    }

  } else { // oper._format == NULL
    // Provide a few special case formats where the AD writer cannot.
    if ( strcmp(oper._ident,"Universe")==0 ) {
      fprintf(fp, "  st->print(\"$$univ\");\n");
    }
    // labelOper::int_format is defined in ad_<...>.cpp
  }
  // ALWAYS! Provide a special case output for condition codes.
  if( oper.is_ideal_bool() ) {
    defineCCodeDump(&oper, fp,0);
  }
  fprintf(fp,"}\n");

  // Generate external format function, when data is stored externally
  fprintf(fp,"void %sOper::ext_format(PhaseRegAlloc *ra, const MachNode *node, int idx, outputStream *st) const {\n", oper._ident);
  // Generate the user-defined portion of the format
  if (oper._format) {
    if ( oper._format->_strings.count() != 0 ) {

      // Check for a replacement string "$..."
      if ( oper._format->_rep_vars.count() != 0 ) {
        // Initialization code for ext_format
      }

      // Build the format from the entries in strings and rep_vars
      const char  *string  = NULL;
      oper._format->_rep_vars.reset();
      oper._format->_strings.reset();
      while ( (string = oper._format->_strings.iter()) != NULL ) {

        // Check if this is a standard string or a replacement variable
        if ( string != NameList::_signal ) {
          // Normal string
          // Pass through to st->print
          fprintf(fp,"  st->print_raw(\"%s\");\n", string);
        } else {
          // Replacement variable
          const char *rep_var = oper._format->_rep_vars.iter();
         // Check that it is a local name, and an operand
          const Form* form = oper._localNames[rep_var];
          if (form == NULL) {
            globalAD->syntax_err(oper._linenum,
                                 "\'%s\' not found in format for %s\n", rep_var, oper._ident);
            assert(form, "replacement variable was not found in local names");
          }
          OperandForm *op      = form->is_operand();
          // Get index if register or constant
          if ( op->_matrule && op->_matrule->is_base_register(globals) ) {
            idx  = oper.register_position( globals, rep_var);
          }
          else if (op->_matrule && op->_matrule->is_base_constant(globals)) {
            idx  = oper.constant_position( globals, rep_var);
          } else {
            idx = 0;
          }
          // output invocation of "$..."s format function
          if ( op != NULL )   op->ext_format(fp, globals, idx);

          // Lookup the index position of the replacement variable
          idx      = oper._components.operand_position_format(rep_var, &oper);
          if ( idx == -1 ) {
            fprintf(stderr,
                    "Using a name, %s, that isn't in match rule\n", rep_var);
            assert( strcmp(op->_ident,"label")==0, "Unimplemented");
          }
        } // Done with a replacement variable
      } // Done with all format strings

    } else {
      // Default formats for base operands (RegI, RegP, ConI, ConP, ...)
      oper.ext_format(fp, globals, 0);
    }
  } else { // oper._format == NULL
    // Provide a few special case formats where the AD writer cannot.
    if ( strcmp(oper._ident,"Universe")==0 ) {
      fprintf(fp, "  st->print(\"$$univ\");\n");
    }
    // labelOper::ext_format is defined in ad_<...>.cpp
  }
  // ALWAYS! Provide a special case output for condition codes.
  if( oper.is_ideal_bool() ) {
    defineCCodeDump(&oper, fp,0);
  }
  fprintf(fp, "}\n");
  fprintf(fp, "#endif\n");
}


// Generate the format rule for an instruction
void gen_inst_format(FILE *fp, FormDict &globals, InstructForm &inst, bool for_c_file = false) {
  if (!for_c_file) {
    // compile the bodies separately, to cut down on recompilations
    // #ifndef PRODUCT region generated by caller
    fprintf(fp,"  virtual void           format(PhaseRegAlloc *ra, outputStream *st) const;\n");
    return;
  }

  // Define the format function
  fprintf(fp, "#ifndef PRODUCT\n");
  fprintf(fp, "void %sNode::format(PhaseRegAlloc *ra, outputStream *st) const {\n", inst._ident);

  // Generate the user-defined portion of the format
  if( inst._format ) {
    // If there are replacement variables,
    // Generate index values needed for determining the operand position
    if( inst._format->_rep_vars.count() )
      inst.index_temps(fp, globals);

    // Build the format from the entries in strings and rep_vars
    const char  *string  = NULL;
    inst._format->_rep_vars.reset();
    inst._format->_strings.reset();
    while( (string = inst._format->_strings.iter()) != NULL ) {
      fprintf(fp,"  ");
      // Check if this is a standard string or a replacement variable
      if( string == NameList::_signal ) { // Replacement variable
        const char* rep_var =  inst._format->_rep_vars.iter();
        inst.rep_var_format( fp, rep_var);
      } else if( string == NameList::_signal3 ) { // Replacement variable in raw text
        const char* rep_var =  inst._format->_rep_vars.iter();
        const Form *form   = inst._localNames[rep_var];
        if (form == NULL) {
          fprintf(stderr, "unknown replacement variable in format statement: '%s'\n", rep_var);
          assert(false, "ShouldNotReachHere()");
        }
        OpClassForm *opc   = form->is_opclass();
        assert( opc, "replacement variable was not found in local names");
        // Lookup the index position of the replacement variable
        int idx  = inst.operand_position_format(rep_var);
        if ( idx == -1 ) {
          assert( strcmp(opc->_ident,"label")==0, "Unimplemented");
          assert( false, "ShouldNotReachHere()");
        }

        if (inst.is_noninput_operand(idx)) {
          assert( false, "ShouldNotReachHere()");
        } else {
          // Output the format call for this operand
          fprintf(fp,"opnd_array(%d)",idx);
        }
        rep_var =  inst._format->_rep_vars.iter();
        inst._format->_strings.iter();
        if ( strcmp(rep_var,"$constant") == 0 && opc->is_operand()) {
          Form::DataType constant_type = form->is_operand()->is_base_constant(globals);
          if ( constant_type == Form::idealD ) {
            fprintf(fp,"->constantD()");
          } else if ( constant_type == Form::idealF ) {
            fprintf(fp,"->constantF()");
          } else if ( constant_type == Form::idealL ) {
            fprintf(fp,"->constantL()");
          } else {
            fprintf(fp,"->constant()");
          }
        } else if ( strcmp(rep_var,"$cmpcode") == 0) {
            fprintf(fp,"->ccode()");
        } else {
          assert( false, "ShouldNotReachHere()");
        }
      } else if( string == NameList::_signal2 ) // Raw program text
        fputs(inst._format->_strings.iter(), fp);
      else
        fprintf(fp,"st->print_raw(\"%s\");\n", string);
    } // Done with all format strings
  } // Done generating the user-defined portion of the format

  // Add call debug info automatically
  Form::CallType call_type = inst.is_ideal_call();
  if( call_type != Form::invalid_type ) {
    switch( call_type ) {
    case Form::JAVA_DYNAMIC:
      fprintf(fp,"  _method->print_short_name(st);\n");
      break;
    case Form::JAVA_STATIC:
      fprintf(fp,"  if( _method ) _method->print_short_name(st);\n");
      fprintf(fp,"  else st->print(\" wrapper for: %%s\", _name);\n");
      fprintf(fp,"  if( !_method ) dump_trap_args(st);\n");
      break;
    case Form::JAVA_COMPILED:
    case Form::JAVA_INTERP:
      break;
    case Form::JAVA_RUNTIME:
    case Form::JAVA_LEAF:
    case Form::JAVA_NATIVE:
      fprintf(fp,"  st->print(\" %%s\", _name);");
      break;
    default:
      assert(0,"ShouldNotReachHere");
    }
    fprintf(fp,  "  st->cr();\n" );
    fprintf(fp,  "  if (_jvms) _jvms->format(ra, this, st); else st->print_cr(\"        No JVM State Info\");\n" );
    fprintf(fp,  "  st->print(\"        # \");\n" );
    fprintf(fp,  "  if( _jvms && _oop_map ) _oop_map->print_on(st);\n");
  }
  else if(inst.is_ideal_safepoint()) {
    fprintf(fp,  "  st->print_raw(\"\");\n" );
    fprintf(fp,  "  if (_jvms) _jvms->format(ra, this, st); else st->print_cr(\"        No JVM State Info\");\n" );
    fprintf(fp,  "  st->print(\"        # \");\n" );
    fprintf(fp,  "  if( _jvms && _oop_map ) _oop_map->print_on(st);\n");
  }
  else if( inst.is_ideal_if() ) {
    fprintf(fp,  "  st->print(\"  P=%%f C=%%f\",_prob,_fcnt);\n" );
  }
  else if( inst.is_ideal_mem() ) {
    // Print out the field name if available to improve readability
    fprintf(fp,  "  if (ra->C->alias_type(adr_type())->field() != NULL) {\n");
    fprintf(fp,  "    ciField* f = ra->C->alias_type(adr_type())->field();\n");
    fprintf(fp,  "    st->print(\" %s Field: \");\n", commentSeperator);
    fprintf(fp,  "    if (f->is_volatile())\n");
    fprintf(fp,  "      st->print(\"volatile \");\n");
    fprintf(fp,  "    f->holder()->name()->print_symbol_on(st);\n");
    fprintf(fp,  "    st->print(\".\");\n");
    fprintf(fp,  "    f->name()->print_symbol_on(st);\n");
    fprintf(fp,  "    if (f->is_constant())\n");
    fprintf(fp,  "      st->print(\" (constant)\");\n");
    fprintf(fp,  "  } else {\n");
    // Make sure 'Volatile' gets printed out
    fprintf(fp,  "    if (ra->C->alias_type(adr_type())->is_volatile())\n");
    fprintf(fp,  "      st->print(\" volatile!\");\n");
    fprintf(fp,  "  }\n");
  }

  // Complete the definition of the format function
  fprintf(fp, "}\n#endif\n");
}

void ArchDesc::declare_pipe_classes(FILE *fp_hpp) {
  if (!_pipeline)
    return;

  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "// Pipeline_Use_Cycle_Mask Class\n");
  fprintf(fp_hpp, "class Pipeline_Use_Cycle_Mask {\n");

  if (_pipeline->_maxcycleused <= 32) {
    fprintf(fp_hpp, "protected:\n");
    fprintf(fp_hpp, "  %s _mask;\n\n", _pipeline->_maxcycleused <= 32 ? "uint" : "uint64_t" );
    fprintf(fp_hpp, "public:\n");
    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask() : _mask(0) {}\n\n");
    if (_pipeline->_maxcycleused <= 32)
      fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask(uint mask) : _mask(mask) {}\n\n");
    else {
      fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask(uint mask1, uint mask2) : _mask((((uint64_t)mask1) << 32) | mask2) {}\n\n");
      fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask(uint64_t mask) : _mask(mask) {}\n\n");
    }
    fprintf(fp_hpp, "  bool overlaps(const Pipeline_Use_Cycle_Mask &in2) const {\n");
    fprintf(fp_hpp, "    return ((_mask & in2._mask) != 0);\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask& operator<<=(int n) {\n");
    fprintf(fp_hpp, "    _mask <<= n;\n");
    fprintf(fp_hpp, "    return *this;\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  void Or(const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_hpp, "    _mask |= in2._mask;\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  friend Pipeline_Use_Cycle_Mask operator&(const Pipeline_Use_Cycle_Mask &, const Pipeline_Use_Cycle_Mask &);\n");
    fprintf(fp_hpp, "  friend Pipeline_Use_Cycle_Mask operator|(const Pipeline_Use_Cycle_Mask &, const Pipeline_Use_Cycle_Mask &);\n\n");
  }
  else {
    fprintf(fp_hpp, "protected:\n");
    uint masklen = (_pipeline->_maxcycleused + 31) >> 5;
    uint l;
    fprintf(fp_hpp, "  uint ");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "_mask%d%s", l, l < masklen ? ", " : ";\n\n");
    fprintf(fp_hpp, "public:\n");
    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask() : ");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "_mask%d(0)%s", l, l < masklen ? ", " : " {}\n\n");
    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask(");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "uint mask%d%s", l, l < masklen ? ", " : ") : ");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "_mask%d(mask%d)%s", l, l, l < masklen ? ", " : " {}\n\n");

    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask intersect(const Pipeline_Use_Cycle_Mask &in2) {\n");
    fprintf(fp_hpp, "    Pipeline_Use_Cycle_Mask out;\n");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "    out._mask%d = _mask%d & in2._mask%d;\n", l, l, l);
    fprintf(fp_hpp, "    return out;\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  bool overlaps(const Pipeline_Use_Cycle_Mask &in2) const {\n");
    fprintf(fp_hpp, "    return (");
    for (l = 1; l <= masklen; l++)
      fprintf(fp_hpp, "((_mask%d & in2._mask%d) != 0)%s", l, l, l < masklen ? " || " : "");
    fprintf(fp_hpp, ") ? true : false;\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask& operator<<=(int n) {\n");
    fprintf(fp_hpp, "    if (n >= 32)\n");
    fprintf(fp_hpp, "      do {\n       ");
    for (l = masklen; l > 1; l--)
      fprintf(fp_hpp, " _mask%d = _mask%d;", l, l-1);
    fprintf(fp_hpp, " _mask%d = 0;\n", 1);
    fprintf(fp_hpp, "      } while ((n -= 32) >= 32);\n\n");
    fprintf(fp_hpp, "    if (n > 0) {\n");
    fprintf(fp_hpp, "      uint m = 32 - n;\n");
    fprintf(fp_hpp, "      uint mask = (1 << n) - 1;\n");
    fprintf(fp_hpp, "      uint temp%d = mask & (_mask%d >> m); _mask%d <<= n;\n", 2, 1, 1);
    for (l = 2; l < masklen; l++) {
      fprintf(fp_hpp, "      uint temp%d = mask & (_mask%d >> m); _mask%d <<= n; _mask%d |= temp%d;\n", l+1, l, l, l, l);
    }
    fprintf(fp_hpp, "      _mask%d <<= n; _mask%d |= temp%d;\n", masklen, masklen, masklen);
    fprintf(fp_hpp, "    }\n");

    fprintf(fp_hpp, "    return *this;\n");
    fprintf(fp_hpp, "  }\n\n");
    fprintf(fp_hpp, "  void Or(const Pipeline_Use_Cycle_Mask &);\n\n");
    fprintf(fp_hpp, "  friend Pipeline_Use_Cycle_Mask operator&(const Pipeline_Use_Cycle_Mask &, const Pipeline_Use_Cycle_Mask &);\n");
    fprintf(fp_hpp, "  friend Pipeline_Use_Cycle_Mask operator|(const Pipeline_Use_Cycle_Mask &, const Pipeline_Use_Cycle_Mask &);\n\n");
  }

  fprintf(fp_hpp, "  friend class Pipeline_Use;\n\n");
  fprintf(fp_hpp, "  friend class Pipeline_Use_Element;\n\n");
  fprintf(fp_hpp, "};\n\n");

  uint rescount = 0;
  const char *resource;

  for ( _pipeline->_reslist.reset(); (resource = _pipeline->_reslist.iter()) != NULL; ) {
      int mask = _pipeline->_resdict[resource]->is_resource()->mask();
      if ((mask & (mask-1)) == 0)
        rescount++;
    }

  fprintf(fp_hpp, "// Pipeline_Use_Element Class\n");
  fprintf(fp_hpp, "class Pipeline_Use_Element {\n");
  fprintf(fp_hpp, "protected:\n");
  fprintf(fp_hpp, "  // Mask of used functional units\n");
  fprintf(fp_hpp, "  uint _used;\n\n");
  fprintf(fp_hpp, "  // Lower and upper bound of functional unit number range\n");
  fprintf(fp_hpp, "  uint _lb, _ub;\n\n");
  fprintf(fp_hpp, "  // Indicates multiple functionals units available\n");
  fprintf(fp_hpp, "  bool _multiple;\n\n");
  fprintf(fp_hpp, "  // Mask of specific used cycles\n");
  fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask _mask;\n\n");
  fprintf(fp_hpp, "public:\n");
  fprintf(fp_hpp, "  Pipeline_Use_Element() {}\n\n");
  fprintf(fp_hpp, "  Pipeline_Use_Element(uint used, uint lb, uint ub, bool multiple, Pipeline_Use_Cycle_Mask mask)\n");
  fprintf(fp_hpp, "  : _used(used), _lb(lb), _ub(ub), _multiple(multiple), _mask(mask) {}\n\n");
  fprintf(fp_hpp, "  uint used() const { return _used; }\n\n");
  fprintf(fp_hpp, "  uint lowerBound() const { return _lb; }\n\n");
  fprintf(fp_hpp, "  uint upperBound() const { return _ub; }\n\n");
  fprintf(fp_hpp, "  bool multiple() const { return _multiple; }\n\n");
  fprintf(fp_hpp, "  Pipeline_Use_Cycle_Mask mask() const { return _mask; }\n\n");
  fprintf(fp_hpp, "  bool overlaps(const Pipeline_Use_Element &in2) const {\n");
  fprintf(fp_hpp, "    return ((_used & in2._used) != 0 && _mask.overlaps(in2._mask));\n");
  fprintf(fp_hpp, "  }\n\n");
  fprintf(fp_hpp, "  void step(uint cycles) {\n");
  fprintf(fp_hpp, "    _used = 0;\n");
  fprintf(fp_hpp, "    _mask <<= cycles;\n");
  fprintf(fp_hpp, "  }\n\n");
  fprintf(fp_hpp, "  friend class Pipeline_Use;\n");
  fprintf(fp_hpp, "};\n\n");

  fprintf(fp_hpp, "// Pipeline_Use Class\n");
  fprintf(fp_hpp, "class Pipeline_Use {\n");
  fprintf(fp_hpp, "protected:\n");
  fprintf(fp_hpp, "  // These resources can be used\n");
  fprintf(fp_hpp, "  uint _resources_used;\n\n");
  fprintf(fp_hpp, "  // These resources are used; excludes multiple choice functional units\n");
  fprintf(fp_hpp, "  uint _resources_used_exclusively;\n\n");
  fprintf(fp_hpp, "  // Number of elements\n");
  fprintf(fp_hpp, "  uint _count;\n\n");
  fprintf(fp_hpp, "  // This is the array of Pipeline_Use_Elements\n");
  fprintf(fp_hpp, "  Pipeline_Use_Element * _elements;\n\n");
  fprintf(fp_hpp, "public:\n");
  fprintf(fp_hpp, "  Pipeline_Use(uint resources_used, uint resources_used_exclusively, uint count, Pipeline_Use_Element *elements)\n");
  fprintf(fp_hpp, "  : _resources_used(resources_used)\n");
  fprintf(fp_hpp, "  , _resources_used_exclusively(resources_used_exclusively)\n");
  fprintf(fp_hpp, "  , _count(count)\n");
  fprintf(fp_hpp, "  , _elements(elements)\n");
  fprintf(fp_hpp, "  {}\n\n");
  fprintf(fp_hpp, "  uint resourcesUsed() const { return _resources_used; }\n\n");
  fprintf(fp_hpp, "  uint resourcesUsedExclusively() const { return _resources_used_exclusively; }\n\n");
  fprintf(fp_hpp, "  uint count() const { return _count; }\n\n");
  fprintf(fp_hpp, "  Pipeline_Use_Element * element(uint i) const { return &_elements[i]; }\n\n");
  fprintf(fp_hpp, "  uint full_latency(uint delay, const Pipeline_Use &pred) const;\n\n");
  fprintf(fp_hpp, "  void add_usage(const Pipeline_Use &pred);\n\n");
  fprintf(fp_hpp, "  void reset() {\n");
  fprintf(fp_hpp, "    _resources_used = _resources_used_exclusively = 0;\n");
  fprintf(fp_hpp, "  };\n\n");
  fprintf(fp_hpp, "  void step(uint cycles) {\n");
  fprintf(fp_hpp, "    reset();\n");
  fprintf(fp_hpp, "    for (uint i = 0; i < %d; i++)\n",
    rescount);
  fprintf(fp_hpp, "      (&_elements[i])->step(cycles);\n");
  fprintf(fp_hpp, "  };\n\n");
  fprintf(fp_hpp, "  static const Pipeline_Use         elaborated_use;\n");
  fprintf(fp_hpp, "  static const Pipeline_Use_Element elaborated_elements[%d];\n\n",
    rescount);
  fprintf(fp_hpp, "  friend class Pipeline;\n");
  fprintf(fp_hpp, "};\n\n");

  fprintf(fp_hpp, "// Pipeline Class\n");
  fprintf(fp_hpp, "class Pipeline {\n");
  fprintf(fp_hpp, "public:\n");

  fprintf(fp_hpp, "  static bool enabled() { return %s; }\n\n",
    _pipeline ? "true" : "false" );

  assert( _pipeline->_maxInstrsPerBundle &&
        ( _pipeline->_instrUnitSize || _pipeline->_bundleUnitSize) &&
          _pipeline->_instrFetchUnitSize &&
          _pipeline->_instrFetchUnits,
    "unspecified pipeline architecture units");

  uint unitSize = _pipeline->_instrUnitSize ? _pipeline->_instrUnitSize : _pipeline->_bundleUnitSize;

  fprintf(fp_hpp, "  enum {\n");
  fprintf(fp_hpp, "    _variable_size_instructions = %d,\n",
    _pipeline->_variableSizeInstrs ? 1 : 0);
  fprintf(fp_hpp, "    _fixed_size_instructions = %d,\n",
    _pipeline->_variableSizeInstrs ? 0 : 1);
  fprintf(fp_hpp, "    _branch_has_delay_slot = %d,\n",
    _pipeline->_branchHasDelaySlot ? 1 : 0);
  fprintf(fp_hpp, "    _max_instrs_per_bundle = %d,\n",
    _pipeline->_maxInstrsPerBundle);
  fprintf(fp_hpp, "    _max_bundles_per_cycle = %d,\n",
    _pipeline->_maxBundlesPerCycle);
  fprintf(fp_hpp, "    _max_instrs_per_cycle = %d\n",
    _pipeline->_maxBundlesPerCycle * _pipeline->_maxInstrsPerBundle);
  fprintf(fp_hpp, "  };\n\n");

  fprintf(fp_hpp, "  static bool instr_has_unit_size() { return %s; }\n\n",
    _pipeline->_instrUnitSize != 0 ? "true" : "false" );
  if( _pipeline->_bundleUnitSize != 0 )
    if( _pipeline->_instrUnitSize != 0 )
      fprintf(fp_hpp, "// Individual Instructions may be bundled together by the hardware\n\n");
    else
      fprintf(fp_hpp, "// Instructions exist only in bundles\n\n");
  else
    fprintf(fp_hpp, "// Bundling is not supported\n\n");
  if( _pipeline->_instrUnitSize != 0 )
    fprintf(fp_hpp, "  // Size of an instruction\n");
  else
    fprintf(fp_hpp, "  // Size of an individual instruction does not exist - unsupported\n");
  fprintf(fp_hpp, "  static uint instr_unit_size() {");
  if( _pipeline->_instrUnitSize == 0 )
    fprintf(fp_hpp, " assert( false, \"Instructions are only in bundles\" );");
  fprintf(fp_hpp, " return %d; };\n\n", _pipeline->_instrUnitSize);

  if( _pipeline->_bundleUnitSize != 0 )
    fprintf(fp_hpp, "  // Size of a bundle\n");
  else
    fprintf(fp_hpp, "  // Bundles do not exist - unsupported\n");
  fprintf(fp_hpp, "  static uint bundle_unit_size() {");
  if( _pipeline->_bundleUnitSize == 0 )
    fprintf(fp_hpp, " assert( false, \"Bundles are not supported\" );");
  fprintf(fp_hpp, " return %d; };\n\n", _pipeline->_bundleUnitSize);

  fprintf(fp_hpp, "  static bool requires_bundling() { return %s; }\n\n",
    _pipeline->_bundleUnitSize != 0 && _pipeline->_instrUnitSize == 0 ? "true" : "false" );

  fprintf(fp_hpp, "private:\n");
  fprintf(fp_hpp, "  Pipeline();  // Not a legal constructor\n");
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "  const unsigned char                   _read_stage_count;\n");
  fprintf(fp_hpp, "  const unsigned char                   _write_stage;\n");
  fprintf(fp_hpp, "  const unsigned char                   _fixed_latency;\n");
  fprintf(fp_hpp, "  const unsigned char                   _instruction_count;\n");
  fprintf(fp_hpp, "  const bool                            _has_fixed_latency;\n");
  fprintf(fp_hpp, "  const bool                            _has_branch_delay;\n");
  fprintf(fp_hpp, "  const bool                            _has_multiple_bundles;\n");
  fprintf(fp_hpp, "  const bool                            _force_serialization;\n");
  fprintf(fp_hpp, "  const bool                            _may_have_no_code;\n");
  fprintf(fp_hpp, "  const enum machPipelineStages * const _read_stages;\n");
  fprintf(fp_hpp, "  const enum machPipelineStages * const _resource_stage;\n");
  fprintf(fp_hpp, "  const uint                    * const _resource_cycles;\n");
  fprintf(fp_hpp, "  const Pipeline_Use                    _resource_use;\n");
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "public:\n");
  fprintf(fp_hpp, "  Pipeline(uint                            write_stage,\n");
  fprintf(fp_hpp, "           uint                            count,\n");
  fprintf(fp_hpp, "           bool                            has_fixed_latency,\n");
  fprintf(fp_hpp, "           uint                            fixed_latency,\n");
  fprintf(fp_hpp, "           uint                            instruction_count,\n");
  fprintf(fp_hpp, "           bool                            has_branch_delay,\n");
  fprintf(fp_hpp, "           bool                            has_multiple_bundles,\n");
  fprintf(fp_hpp, "           bool                            force_serialization,\n");
  fprintf(fp_hpp, "           bool                            may_have_no_code,\n");
  fprintf(fp_hpp, "           enum machPipelineStages * const dst,\n");
  fprintf(fp_hpp, "           enum machPipelineStages * const stage,\n");
  fprintf(fp_hpp, "           uint                    * const cycles,\n");
  fprintf(fp_hpp, "           Pipeline_Use                    resource_use)\n");
  fprintf(fp_hpp, "  : _read_stage_count(count)\n");
  fprintf(fp_hpp, "  , _write_stage(write_stage)\n");
  fprintf(fp_hpp, "  , _fixed_latency(fixed_latency)\n");
  fprintf(fp_hpp, "  , _instruction_count(instruction_count)\n");
  fprintf(fp_hpp, "  , _has_fixed_latency(has_fixed_latency)\n");
  fprintf(fp_hpp, "  , _has_branch_delay(has_branch_delay)\n");
  fprintf(fp_hpp, "  , _has_multiple_bundles(has_multiple_bundles)\n");
  fprintf(fp_hpp, "  , _force_serialization(force_serialization)\n");
  fprintf(fp_hpp, "  , _may_have_no_code(may_have_no_code)\n");
  fprintf(fp_hpp, "  , _read_stages(dst)\n");
  fprintf(fp_hpp, "  , _resource_stage(stage)\n");
  fprintf(fp_hpp, "  , _resource_cycles(cycles)\n");
  fprintf(fp_hpp, "  , _resource_use(resource_use)\n");
  fprintf(fp_hpp, "  {};\n");
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "  uint writeStage() const {\n");
  fprintf(fp_hpp, "    return (_write_stage);\n");
  fprintf(fp_hpp, "  }\n");
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "  enum machPipelineStages readStage(int ndx) const {\n");
  fprintf(fp_hpp, "    return (ndx < _read_stage_count ? _read_stages[ndx] : stage_undefined);");
  fprintf(fp_hpp, "  }\n\n");
  fprintf(fp_hpp, "  uint resourcesUsed() const {\n");
  fprintf(fp_hpp, "    return _resource_use.resourcesUsed();\n  }\n\n");
  fprintf(fp_hpp, "  uint resourcesUsedExclusively() const {\n");
  fprintf(fp_hpp, "    return _resource_use.resourcesUsedExclusively();\n  }\n\n");
  fprintf(fp_hpp, "  bool hasFixedLatency() const {\n");
  fprintf(fp_hpp, "    return (_has_fixed_latency);\n  }\n\n");
  fprintf(fp_hpp, "  uint fixedLatency() const {\n");
  fprintf(fp_hpp, "    return (_fixed_latency);\n  }\n\n");
  fprintf(fp_hpp, "  uint functional_unit_latency(uint start, const Pipeline *pred) const;\n\n");
  fprintf(fp_hpp, "  uint operand_latency(uint opnd, const Pipeline *pred) const;\n\n");
  fprintf(fp_hpp, "  const Pipeline_Use& resourceUse() const {\n");
  fprintf(fp_hpp, "    return (_resource_use); }\n\n");
  fprintf(fp_hpp, "  const Pipeline_Use_Element * resourceUseElement(uint i) const {\n");
  fprintf(fp_hpp, "    return (&_resource_use._elements[i]); }\n\n");
  fprintf(fp_hpp, "  uint resourceUseCount() const {\n");
  fprintf(fp_hpp, "    return (_resource_use._count); }\n\n");
  fprintf(fp_hpp, "  uint instructionCount() const {\n");
  fprintf(fp_hpp, "    return (_instruction_count); }\n\n");
  fprintf(fp_hpp, "  bool hasBranchDelay() const {\n");
  fprintf(fp_hpp, "    return (_has_branch_delay); }\n\n");
  fprintf(fp_hpp, "  bool hasMultipleBundles() const {\n");
  fprintf(fp_hpp, "    return (_has_multiple_bundles); }\n\n");
  fprintf(fp_hpp, "  bool forceSerialization() const {\n");
  fprintf(fp_hpp, "    return (_force_serialization); }\n\n");
  fprintf(fp_hpp, "  bool mayHaveNoCode() const {\n");
  fprintf(fp_hpp, "    return (_may_have_no_code); }\n\n");
  fprintf(fp_hpp, "//const Pipeline_Use_Cycle_Mask& resourceUseMask(int resource) const {\n");
  fprintf(fp_hpp, "//  return (_resource_use_masks[resource]); }\n\n");
  fprintf(fp_hpp, "\n#ifndef PRODUCT\n");
  fprintf(fp_hpp, "  static const char * stageName(uint i);\n");
  fprintf(fp_hpp, "#endif\n");
  fprintf(fp_hpp, "};\n\n");

  fprintf(fp_hpp, "// Bundle class\n");
  fprintf(fp_hpp, "class Bundle {\n");

  uint mshift = 0;
  for (uint msize = _pipeline->_maxInstrsPerBundle * _pipeline->_maxBundlesPerCycle; msize != 0; msize >>= 1)
    mshift++;

  uint rshift = rescount;

  fprintf(fp_hpp, "protected:\n");
  fprintf(fp_hpp, "  enum {\n");
  fprintf(fp_hpp, "    _unused_delay                   = 0x%x,\n", 0);
  fprintf(fp_hpp, "    _use_nop_delay                  = 0x%x,\n", 1);
  fprintf(fp_hpp, "    _use_unconditional_delay        = 0x%x,\n", 2);
  fprintf(fp_hpp, "    _use_conditional_delay          = 0x%x,\n", 3);
  fprintf(fp_hpp, "    _used_in_conditional_delay      = 0x%x,\n", 4);
  fprintf(fp_hpp, "    _used_in_unconditional_delay    = 0x%x,\n", 5);
  fprintf(fp_hpp, "    _used_in_all_conditional_delays = 0x%x,\n", 6);
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "    _use_delay                      = 0x%x,\n", 3);
  fprintf(fp_hpp, "    _used_in_delay                  = 0x%x\n",  4);
  fprintf(fp_hpp, "  };\n\n");
  fprintf(fp_hpp, "  uint _flags          : 3,\n");
  fprintf(fp_hpp, "       _starts_bundle  : 1,\n");
  fprintf(fp_hpp, "       _instr_count    : %d,\n",   mshift);
  fprintf(fp_hpp, "       _resources_used : %d;\n",   rshift);
  fprintf(fp_hpp, "public:\n");
  fprintf(fp_hpp, "  Bundle() : _flags(_unused_delay), _starts_bundle(0), _instr_count(0), _resources_used(0) {}\n\n");
  fprintf(fp_hpp, "  void set_instr_count(uint i) { _instr_count  = i; }\n");
  fprintf(fp_hpp, "  void set_resources_used(uint i) { _resources_used   = i; }\n");
  fprintf(fp_hpp, "  void clear_usage() { _flags = _unused_delay; }\n");
  fprintf(fp_hpp, "  void set_starts_bundle() { _starts_bundle = true; }\n");

  fprintf(fp_hpp, "  uint flags() const { return (_flags); }\n");
  fprintf(fp_hpp, "  uint instr_count() const { return (_instr_count); }\n");
  fprintf(fp_hpp, "  uint resources_used() const { return (_resources_used); }\n");
  fprintf(fp_hpp, "  bool starts_bundle() const { return (_starts_bundle != 0); }\n");

  fprintf(fp_hpp, "  void set_use_nop_delay() { _flags = _use_nop_delay; }\n");
  fprintf(fp_hpp, "  void set_use_unconditional_delay() { _flags = _use_unconditional_delay; }\n");
  fprintf(fp_hpp, "  void set_use_conditional_delay() { _flags = _use_conditional_delay; }\n");
  fprintf(fp_hpp, "  void set_used_in_unconditional_delay() { _flags = _used_in_unconditional_delay; }\n");
  fprintf(fp_hpp, "  void set_used_in_conditional_delay() { _flags = _used_in_conditional_delay; }\n");
  fprintf(fp_hpp, "  void set_used_in_all_conditional_delays() { _flags = _used_in_all_conditional_delays; }\n");

  fprintf(fp_hpp, "  bool use_nop_delay() { return (_flags == _use_nop_delay); }\n");
  fprintf(fp_hpp, "  bool use_unconditional_delay() { return (_flags == _use_unconditional_delay); }\n");
  fprintf(fp_hpp, "  bool use_conditional_delay() { return (_flags == _use_conditional_delay); }\n");
  fprintf(fp_hpp, "  bool used_in_unconditional_delay() { return (_flags == _used_in_unconditional_delay); }\n");
  fprintf(fp_hpp, "  bool used_in_conditional_delay() { return (_flags == _used_in_conditional_delay); }\n");
  fprintf(fp_hpp, "  bool used_in_all_conditional_delays() { return (_flags == _used_in_all_conditional_delays); }\n");
  fprintf(fp_hpp, "  bool use_delay() { return ((_flags & _use_delay) != 0); }\n");
  fprintf(fp_hpp, "  bool used_in_delay() { return ((_flags & _used_in_delay) != 0); }\n\n");

  fprintf(fp_hpp, "  enum {\n");
  fprintf(fp_hpp, "    _nop_count = %d\n",
    _pipeline->_nopcnt);
  fprintf(fp_hpp, "  };\n\n");
  fprintf(fp_hpp, "  static void initialize_nops(MachNode *nop_list[%d]);\n\n",
    _pipeline->_nopcnt);
  fprintf(fp_hpp, "#ifndef PRODUCT\n");
  fprintf(fp_hpp, "  void dump(outputStream *st = tty) const;\n");
  fprintf(fp_hpp, "#endif\n");
  fprintf(fp_hpp, "};\n\n");

//  const char *classname;
//  for (_pipeline->_classlist.reset(); (classname = _pipeline->_classlist.iter()) != NULL; ) {
//    PipeClassForm *pipeclass = _pipeline->_classdict[classname]->is_pipeclass();
//    fprintf(fp_hpp, "// Pipeline Class Instance for \"%s\"\n", classname);
//  }
}

//------------------------------declareClasses---------------------------------
// Construct the class hierarchy of MachNode classes from the instruction &
// operand lists
void ArchDesc::declareClasses(FILE *fp) {

  // Declare an array containing the machine register names, strings.
  declareRegNames(fp, _register);

  // Declare an array containing the machine register encoding values
  declareRegEncodes(fp, _register);

  // Generate declarations for the total number of operands
  fprintf(fp,"\n");
  fprintf(fp,"// Total number of operands defined in architecture definition\n");
  int num_operands = 0;
  OperandForm *op;
  for (_operands.reset(); (op = (OperandForm*)_operands.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if (op->ideal_only()) continue;

    ++num_operands;
  }
  int first_operand_class = num_operands;
  OpClassForm *opc;
  for (_opclass.reset(); (opc = (OpClassForm*)_opclass.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if (opc->ideal_only()) continue;

    ++num_operands;
  }
  fprintf(fp,"#define FIRST_OPERAND_CLASS   %d\n", first_operand_class);
  fprintf(fp,"#define NUM_OPERANDS          %d\n", num_operands);
  fprintf(fp,"\n");
  // Generate declarations for the total number of instructions
  fprintf(fp,"// Total number of instructions defined in architecture definition\n");
  fprintf(fp,"#define NUM_INSTRUCTIONS   %d\n",instructFormCount());


  // Generate Machine Classes for each operand defined in AD file
  fprintf(fp,"\n");
  fprintf(fp,"//----------------------------Declare classes derived from MachOper----------\n");
  // Iterate through all operands
  _operands.reset();
  OperandForm *oper;
  for( ; (oper = (OperandForm*)_operands.iter()) != NULL;) {
    // Ensure this is a machine-world instruction
    if (oper->ideal_only() ) continue;
    // The declaration of labelOper is in machine-independent file: machnode
    if ( strcmp(oper->_ident,"label")  == 0 ) continue;
    // The declaration of methodOper is in machine-independent file: machnode
    if ( strcmp(oper->_ident,"method") == 0 ) continue;

    // Build class definition for this operand
    fprintf(fp,"\n");
    fprintf(fp,"class %sOper : public MachOper { \n",oper->_ident);
    fprintf(fp,"private:\n");
    // Operand definitions that depend upon number of input edges
    {
      uint num_edges = oper->num_edges(_globalNames);
      if( num_edges != 1 ) { // Use MachOper::num_edges() {return 1;}
        fprintf(fp,"  virtual uint           num_edges() const { return %d; }\n",
              num_edges );
      }
      if( num_edges > 0 ) {
        in_RegMask(fp);
      }
    }

    // Support storing constants inside the MachOper
    declareConstStorage(fp,_globalNames,oper);

    // Support storage of the condition codes
    if( oper->is_ideal_bool() ) {
      fprintf(fp,"  virtual int ccode() const { \n");
      fprintf(fp,"    switch (_c0) {\n");
      fprintf(fp,"    case  BoolTest::eq : return equal();\n");
      fprintf(fp,"    case  BoolTest::gt : return greater();\n");
      fprintf(fp,"    case  BoolTest::lt : return less();\n");
      fprintf(fp,"    case  BoolTest::ne : return not_equal();\n");
      fprintf(fp,"    case  BoolTest::le : return less_equal();\n");
      fprintf(fp,"    case  BoolTest::ge : return greater_equal();\n");
      fprintf(fp,"    case  BoolTest::overflow : return overflow();\n");
      fprintf(fp,"    case  BoolTest::no_overflow: return no_overflow();\n");
      fprintf(fp,"    default : ShouldNotReachHere(); return 0;\n");
      fprintf(fp,"    }\n");
      fprintf(fp,"  };\n");
    }

    // Support storage of the condition codes
    if( oper->is_ideal_bool() ) {
      fprintf(fp,"  virtual void negate() { \n");
      fprintf(fp,"    _c0 = (BoolTest::mask)((int)_c0^0x4); \n");
      fprintf(fp,"  };\n");
    }

    // Declare constructor.
    // Parameters start with condition code, then all other constants
    //
    // (1)  MachXOper(int32 ccode, int32 c0, int32 c1, ..., int32 cn)
    // (2)     : _ccode(ccode), _c0(c0), _c1(c1), ..., _cn(cn) { }
    //
    Form::DataType constant_type = oper->simple_type(_globalNames);
    defineConstructor(fp, oper->_ident, oper->num_consts(_globalNames),
                      oper->_components, oper->is_ideal_bool(),
                      constant_type, _globalNames);

    // Clone function
    fprintf(fp,"  virtual MachOper      *clone() const;\n");

    // Support setting a spill offset into a constant operand.
    // We only support setting an 'int' offset, while in the
    // LP64 build spill offsets are added with an AddP which
    // requires a long constant.  Thus we don't support spilling
    // in frames larger than 4Gig.
    if( oper->has_conI(_globalNames) ||
        oper->has_conL(_globalNames) )
      fprintf(fp, "  virtual void set_con( jint c0 ) { _c0 = c0; }\n");

    // virtual functions for encoding and format
    //    fprintf(fp,"  virtual void           encode()   const {\n    %s }\n",
    //            (oper->_encrule)?(oper->_encrule->_encrule):"");
    // Check the interface type, and generate the correct query functions
    // encoding queries based upon MEMORY_INTER, REG_INTER, CONST_INTER.

    fprintf(fp,"  virtual uint           opcode() const { return %s; }\n",
            machOperEnum(oper->_ident));

    // virtual function to look up ideal return type of machine instruction
    //
    // (1)  virtual const Type    *type() const { return .....; }
    //
    if ((oper->_matrule) && (oper->_matrule->_lChild == NULL) &&
        (oper->_matrule->_rChild == NULL)) {
      unsigned int position = 0;
      const char  *opret, *opname, *optype;
      oper->_matrule->base_operand(position,_globalNames,opret,opname,optype);
      fprintf(fp,"  virtual const Type    *type() const {");
      const char *type = getIdealType(optype);
      if( type != NULL ) {
        Form::DataType data_type = oper->is_base_constant(_globalNames);
        // Check if we are an ideal pointer type
        if( data_type == Form::idealP || data_type == Form::idealN || data_type == Form::idealNKlass ) {
          // Return the ideal type we already have: <TypePtr *>
          fprintf(fp," return _c0;");
        } else {
          // Return the appropriate bottom type
          fprintf(fp," return %s;", getIdealType(optype));
        }
      } else {
        fprintf(fp," ShouldNotCallThis(); return Type::BOTTOM;");
      }
      fprintf(fp," }\n");
    } else {
      // Check for user-defined stack slots, based upon sRegX
      Form::DataType data_type = oper->is_user_name_for_sReg();
      if( data_type != Form::none ){
        const char *type = NULL;
        switch( data_type ) {
        case Form::idealI: type = "TypeInt::INT";   break;
        case Form::idealP: type = "TypePtr::BOTTOM";break;
        case Form::idealF: type = "Type::FLOAT";    break;
        case Form::idealD: type = "Type::DOUBLE";   break;
        case Form::idealL: type = "TypeLong::LONG"; break;
        case Form::none: // fall through
        default:
          assert( false, "No support for this type of stackSlot");
        }
        fprintf(fp,"  virtual const Type    *type() const { return %s; } // stackSlotX\n", type);
      }
    }


    //
    // virtual functions for defining the encoding interface.
    //
    // Access the linearized ideal register mask,
    // map to physical register encoding
    if ( oper->_matrule && oper->_matrule->is_base_register(_globalNames) ) {
      // Just use the default virtual 'reg' call
    } else if ( oper->ideal_to_sReg_type(oper->_ident) != Form::none ) {
      // Special handling for operand 'sReg', a Stack Slot Register.
      // Map linearized ideal register mask to stack slot number
      fprintf(fp,"  virtual int            reg(PhaseRegAlloc *ra_, const Node *node) const {\n");
      fprintf(fp,"    return (int)OptoReg::reg2stack(ra_->get_reg_first(node));/* sReg */\n");
      fprintf(fp,"  }\n");
      fprintf(fp,"  virtual int            reg(PhaseRegAlloc *ra_, const Node *node, int idx) const {\n");
      fprintf(fp,"    return (int)OptoReg::reg2stack(ra_->get_reg_first(node->in(idx)));/* sReg */\n");
      fprintf(fp,"  }\n");
    }

    // Output the operand specific access functions used by an enc_class
    // These are only defined when we want to override the default virtual func
    if (oper->_interface != NULL) {
      fprintf(fp,"\n");
      // Check if it is a Memory Interface
      if ( oper->_interface->is_MemInterface() != NULL ) {
        MemInterface *mem_interface = oper->_interface->is_MemInterface();
        const char *base = mem_interface->_base;
        if( base != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "base", base);
        }
        char *index = mem_interface->_index;
        if( index != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "index", index);
        }
        const char *scale = mem_interface->_scale;
        if( scale != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "scale", scale);
        }
        const char *disp = mem_interface->_disp;
        if( disp != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "disp", disp);
          oper->disp_is_oop(fp, _globalNames);
        }
        if( oper->stack_slots_only(_globalNames) ) {
          // should not call this:
          fprintf(fp,"  virtual int       constant_disp() const { return Type::OffsetBot; }");
        } else if ( disp != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "constant_disp", disp);
        }
      } // end Memory Interface
      // Check if it is a Conditional Interface
      else if (oper->_interface->is_CondInterface() != NULL) {
        CondInterface *cInterface = oper->_interface->is_CondInterface();
        const char *equal = cInterface->_equal;
        if( equal != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "equal", equal);
        }
        const char *not_equal = cInterface->_not_equal;
        if( not_equal != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "not_equal", not_equal);
        }
        const char *less = cInterface->_less;
        if( less != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "less", less);
        }
        const char *greater_equal = cInterface->_greater_equal;
        if( greater_equal != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "greater_equal", greater_equal);
        }
        const char *less_equal = cInterface->_less_equal;
        if( less_equal != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "less_equal", less_equal);
        }
        const char *greater = cInterface->_greater;
        if( greater != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "greater", greater);
        }
        const char *overflow = cInterface->_overflow;
        if( overflow != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "overflow", overflow);
        }
        const char *no_overflow = cInterface->_no_overflow;
        if( no_overflow != NULL ) {
          define_oper_interface(fp, *oper, _globalNames, "no_overflow", no_overflow);
        }
      } // end Conditional Interface
      // Check if it is a Constant Interface
      else if (oper->_interface->is_ConstInterface() != NULL ) {
        assert( oper->num_consts(_globalNames) == 1,
                "Must have one constant when using CONST_INTER encoding");
        if (!strcmp(oper->ideal_type(_globalNames), "ConI")) {
          // Access the locally stored constant
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " return (intptr_t)_c0;");
          fprintf(fp,"  }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConP")) {
          // Access the locally stored constant
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " return _c0->get_con();");
          fprintf(fp, " }\n");
          // Generate query to determine if this pointer is an oop
          fprintf(fp,"  virtual relocInfo::relocType           constant_reloc() const {");
          fprintf(fp,   " return _c0->reloc();");
          fprintf(fp, " }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConN")) {
          // Access the locally stored constant
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " return _c0->get_ptrtype()->get_con();");
          fprintf(fp, " }\n");
          // Generate query to determine if this pointer is an oop
          fprintf(fp,"  virtual relocInfo::relocType           constant_reloc() const {");
          fprintf(fp,   " return _c0->get_ptrtype()->reloc();");
          fprintf(fp, " }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConNKlass")) {
          // Access the locally stored constant
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " return _c0->get_ptrtype()->get_con();");
          fprintf(fp, " }\n");
          // Generate query to determine if this pointer is an oop
          fprintf(fp,"  virtual relocInfo::relocType           constant_reloc() const {");
          fprintf(fp,   " return _c0->get_ptrtype()->reloc();");
          fprintf(fp, " }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConL")) {
          fprintf(fp,"  virtual intptr_t       constant() const {");
          // We don't support addressing modes with > 4Gig offsets.
          // Truncate to int.
          fprintf(fp,   "  return (intptr_t)_c0;");
          fprintf(fp, " }\n");
          fprintf(fp,"  virtual jlong          constantL() const {");
          fprintf(fp,   " return _c0;");
          fprintf(fp, " }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConF")) {
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " ShouldNotReachHere(); return 0; ");
          fprintf(fp, " }\n");
          fprintf(fp,"  virtual jfloat         constantF() const {");
          fprintf(fp,   " return (jfloat)_c0;");
          fprintf(fp, " }\n");
        }
        else if (!strcmp(oper->ideal_type(_globalNames), "ConD")) {
          fprintf(fp,"  virtual intptr_t       constant() const {");
          fprintf(fp,   " ShouldNotReachHere(); return 0; ");
          fprintf(fp, " }\n");
          fprintf(fp,"  virtual jdouble        constantD() const {");
          fprintf(fp,   " return _c0;");
          fprintf(fp, " }\n");
        }
      }
      else if (oper->_interface->is_RegInterface() != NULL) {
        // make sure that a fixed format string isn't used for an
        // operand which might be assiged to multiple registers.
        // Otherwise the opto assembly output could be misleading.
        if (oper->_format->_strings.count() != 0 && !oper->is_bound_register()) {
          syntax_err(oper->_linenum,
                     "Only bound registers can have fixed formats: %s\n",
                     oper->_ident);
        }
      }
      else {
        assert( false, "ShouldNotReachHere();");
      }
    }

    fprintf(fp,"\n");
    // // Currently all XXXOper::hash() methods are identical (990820)
    // declare_hash(fp);
    // // Currently all XXXOper::Cmp() methods are identical (990820)
    // declare_cmp(fp);

    // Do not place dump_spec() and Name() into PRODUCT code
    // int_format and ext_format are not needed in PRODUCT code either
    fprintf(fp, "#ifndef PRODUCT\n");

    // Declare int_format() and ext_format()
    gen_oper_format(fp, _globalNames, *oper);

    // Machine independent print functionality for debugging
    // IF we have constants, create a dump_spec function for the derived class
    //
    // (1)  virtual void           dump_spec() const {
    // (2)    st->print("#%d", _c#);        // Constant != ConP
    //  OR    _c#->dump_on(st);             // Type ConP
    //  ...
    // (3)  }
    uint num_consts = oper->num_consts(_globalNames);
    if( num_consts > 0 ) {
      // line (1)
      fprintf(fp, "  virtual void           dump_spec(outputStream *st) const {\n");
      // generate format string for st->print
      // Iterate over the component list & spit out the right thing
      uint i = 0;
      const char *type = oper->ideal_type(_globalNames);
      Component  *comp;
      oper->_components.reset();
      if ((comp = oper->_components.iter()) == NULL) {
        assert(num_consts == 1, "Bad component list detected.\n");
        i = dump_spec_constant( fp, type, i, oper );
        // Check that type actually matched
        assert( i != 0, "Non-constant operand lacks component list.");
      } // end if NULL
      else {
        // line (2)
        // dump all components
        oper->_components.reset();
        while((comp = oper->_components.iter()) != NULL) {
          type = comp->base_type(_globalNames);
          i = dump_spec_constant( fp, type, i, NULL );
        }
      }
      // finish line (3)
      fprintf(fp,"  }\n");
    }

    fprintf(fp,"  virtual const char    *Name() const { return \"%s\";}\n",
            oper->_ident);

    fprintf(fp,"#endif\n");

    // Close definition of this XxxMachOper
    fprintf(fp,"};\n");
  }


  // Generate Machine Classes for each instruction defined in AD file
  fprintf(fp,"\n");
  fprintf(fp,"//----------------------------Declare classes for Pipelines-----------------\n");
  declare_pipe_classes(fp);

  // Generate Machine Classes for each instruction defined in AD file
  fprintf(fp,"\n");
  fprintf(fp,"//----------------------------Declare classes derived from MachNode----------\n");
  _instructions.reset();
  InstructForm *instr;
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Ensure this is a machine-world instruction
    if ( instr->ideal_only() ) continue;

    // Build class definition for this instruction
    fprintf(fp,"\n");
    fprintf(fp,"class %sNode : public %s { \n",
            instr->_ident, instr->mach_base_class(_globalNames) );
    fprintf(fp,"private:\n");
    fprintf(fp,"  MachOper *_opnd_array[%d];\n", instr->num_opnds() );
    if ( instr->is_ideal_jump() ) {
      fprintf(fp, "  GrowableArray<Label*> _index2label;\n");
    }

    fprintf(fp, "public:\n");

    Attribute *att = instr->_attribs;
    // Fields of the node specified in the ad file.
    while (att != NULL) {
      if (strncmp(att->_ident, "ins_field_", 10) == 0) {
        const char *field_name = att->_ident+10;
        const char *field_type = att->_val;
        fprintf(fp, "  %s _%s;\n", field_type, field_name);
      }
      att = (Attribute *)att->_next;
    }

    fprintf(fp,"  MachOper *opnd_array(uint operand_index) const {\n");
    fprintf(fp,"    assert(operand_index < _num_opnds, \"invalid _opnd_array index\");\n");
    fprintf(fp,"    return _opnd_array[operand_index];\n");
    fprintf(fp,"  }\n");
    fprintf(fp,"  void      set_opnd_array(uint operand_index, MachOper *operand) {\n");
    fprintf(fp,"    assert(operand_index < _num_opnds, \"invalid _opnd_array index\");\n");
    fprintf(fp,"    _opnd_array[operand_index] = operand;\n");
    fprintf(fp,"  }\n");
    fprintf(fp,"  virtual uint           rule() const { return %s_rule; }\n",
            instr->_ident);
    fprintf(fp,"private:\n");
    if ( instr->is_ideal_jump() ) {
      fprintf(fp,"  virtual void           add_case_label(int index_num, Label* blockLabel) {\n");
      fprintf(fp,"    _index2label.at_put_grow(index_num, blockLabel);\n");
      fprintf(fp,"  }\n");
    }
    if( can_cisc_spill() && (instr->cisc_spill_alternate() != NULL) ) {
      fprintf(fp,"  const RegMask  *_cisc_RegMask;\n");
    }

    out_RegMask(fp);                      // output register mask

    // If this instruction contains a labelOper
    // Declare Node::methods that set operand Label's contents
    int label_position = instr->label_position();
    if( label_position != -1 ) {
      // Set/Save the label, stored in labelOper::_branch_label
      fprintf(fp,"  virtual void           label_set( Label* label, uint block_num );\n");
      fprintf(fp,"  virtual void           save_label( Label** label, uint* block_num );\n");
    }

    // If this instruction contains a methodOper
    // Declare Node::methods that set operand method's contents
    int method_position = instr->method_position();
    if( method_position != -1 ) {
      // Set the address method, stored in methodOper::_method
      fprintf(fp,"  virtual void           method_set( intptr_t method );\n");
    }

    // virtual functions for attributes
    //
    // Each instruction attribute results in a virtual call of same name.
    // The ins_cost is not handled here.
    Attribute *attr = instr->_attribs;
    Attribute *avoid_back_to_back_attr = NULL;
    while (attr != NULL) {
      if (strcmp (attr->_ident, "ins_is_TrapBasedCheckNode") == 0) {
        fprintf(fp, "  virtual bool           is_TrapBasedCheckNode() const { return %s; }\n", attr->_val);
      } else if (strcmp (attr->_ident, "ins_cost") != 0 &&
          strncmp(attr->_ident, "ins_field_", 10) != 0 &&
          // Must match function in node.hpp: return type bool, no prefix "ins_".
          strcmp (attr->_ident, "ins_is_TrapBasedCheckNode") != 0 &&
          strcmp (attr->_ident, "ins_short_branch") != 0) {
        fprintf(fp, "  virtual int            %s() const { return %s; }\n", attr->_ident, attr->_val);
      }
      if (strcmp(attr->_ident, "ins_avoid_back_to_back") == 0) {
        avoid_back_to_back_attr = attr;
      }
      attr = (Attribute *)attr->_next;
    }

    // virtual functions for encode and format

    // Virtual function for evaluating the constant.
    if (instr->is_mach_constant()) {
      fprintf(fp,"  virtual void           eval_constant(Compile* C);\n");
    }

    // Output the opcode function and the encode function here using the
    // encoding class information in the _insencode slot.
    if ( instr->_insencode ) {
      if (instr->postalloc_expands()) {
        fprintf(fp,"  virtual bool           requires_postalloc_expand() const { return true; }\n");
        fprintf(fp,"  virtual void           postalloc_expand(GrowableArray <Node *> *nodes, PhaseRegAlloc *ra_);\n");
      } else {
        fprintf(fp,"  virtual void           emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const;\n");
      }
    }

    // virtual function for getting the size of an instruction
    if ( instr->_size ) {
      fprintf(fp,"  virtual uint           size(PhaseRegAlloc *ra_) const;\n");
    }

    // Return the top-level ideal opcode.
    // Use MachNode::ideal_Opcode() for nodes based on MachNode class
    // if the ideal_Opcode == Op_Node.
    if ( strcmp("Node", instr->ideal_Opcode(_globalNames)) != 0 ||
         strcmp("MachNode", instr->mach_base_class(_globalNames)) != 0 ) {
      fprintf(fp,"  virtual int            ideal_Opcode() const { return Op_%s; }\n",
            instr->ideal_Opcode(_globalNames) );
    }

    if (instr->needs_constant_base() &&
        !instr->is_mach_constant()) {  // These inherit the funcion from MachConstantNode.
      fprintf(fp,"  virtual uint           mach_constant_base_node_input() const { ");
      if (instr->is_ideal_call() != Form::invalid_type &&
          instr->is_ideal_call() != Form::JAVA_LEAF) {
        // MachConstantBase goes behind arguments, but before jvms.
        fprintf(fp,"assert(tf() && tf()->domain(), \"\"); return tf()->domain()->cnt();");
      } else {
        fprintf(fp,"return req()-1;");
      }
      fprintf(fp," }\n");
    }

    // Allow machine-independent optimization, invert the sense of the IF test
    if( instr->is_ideal_if() ) {
      fprintf(fp,"  virtual void           negate() { \n");
      // Identify which operand contains the negate(able) ideal condition code
      int   idx = 0;
      instr->_components.reset();
      for( Component *comp; (comp = instr->_components.iter()) != NULL; ) {
        // Check that component is an operand
        Form *form = (Form*)_globalNames[comp->_type];
        OperandForm *opForm = form ? form->is_operand() : NULL;
        if( opForm == NULL ) continue;

        // Lookup the position of the operand in the instruction.
        if( opForm->is_ideal_bool() ) {
          idx = instr->operand_position(comp->_name, comp->_usedef);
          assert( idx != NameList::Not_in_list, "Did not find component in list that contained it.");
          break;
        }
      }
      fprintf(fp,"    opnd_array(%d)->negate();\n", idx);
      fprintf(fp,"    _prob = 1.0f - _prob;\n");
      fprintf(fp,"  };\n");
    }


    // Identify which input register matches the input register.
    uint  matching_input = instr->two_address(_globalNames);

    // Generate the method if it returns != 0 otherwise use MachNode::two_adr()
    if( matching_input != 0 ) {
      fprintf(fp,"  virtual uint           two_adr() const  ");
      fprintf(fp,"{ return oper_input_base()");
      for( uint i = 2; i <= matching_input; i++ )
        fprintf(fp," + opnd_array(%d)->num_edges()",i-1);
      fprintf(fp,"; }\n");
    }

    // Declare cisc_version, if applicable
    //   MachNode *cisc_version( int offset /* ,... */ );
    instr->declare_cisc_version(*this, fp);

    // If there is an explicit peephole rule, build it
    if ( instr->peepholes() != NULL ) {
      fprintf(fp,"  virtual MachNode      *peephole(Block *block, int block_index, PhaseRegAlloc *ra_, int &deleted);\n");
    }

    // Output the declaration for number of relocation entries
    if ( instr->reloc(_globalNames) != 0 ) {
      fprintf(fp,"  virtual int            reloc() const;\n");
    }

    if (instr->alignment() != 1) {
      fprintf(fp,"  virtual int            alignment_required() const { return %d; }\n", instr->alignment());
      fprintf(fp,"  virtual int            compute_padding(int current_offset) const;\n");
    }

    // Starting point for inputs matcher wants.
    // Use MachNode::oper_input_base() for nodes based on MachNode class
    // if the base == 1.
    if ( instr->oper_input_base(_globalNames) != 1 ||
         strcmp("MachNode", instr->mach_base_class(_globalNames)) != 0 ) {
      fprintf(fp,"  virtual uint           oper_input_base() const { return %d; }\n",
            instr->oper_input_base(_globalNames));
    }

    // Make the constructor and following methods 'public:'
    fprintf(fp,"public:\n");

    // Constructor
    if ( instr->is_ideal_jump() ) {
      fprintf(fp,"  %sNode() : _index2label(MinJumpTableSize*2) { ", instr->_ident);
    } else {
      fprintf(fp,"  %sNode() { ", instr->_ident);
      if( can_cisc_spill() && (instr->cisc_spill_alternate() != NULL) ) {
        fprintf(fp,"_cisc_RegMask = NULL; ");
      }
    }

    fprintf(fp," _num_opnds = %d; _opnds = _opnd_array; ", instr->num_opnds());

    bool node_flags_set = false;
    // flag: if this instruction matches an ideal 'Copy*' node
    if ( instr->is_ideal_copy() != 0 ) {
      fprintf(fp,"init_flags(Flag_is_Copy");
      node_flags_set = true;
    }

    // Is an instruction is a constant?  If so, get its type
    Form::DataType  data_type;
    const char     *opType = NULL;
    const char     *result = NULL;
    data_type    = instr->is_chain_of_constant(_globalNames, opType, result);
    // Check if this instruction is a constant
    if ( data_type != Form::none ) {
      if ( node_flags_set ) {
        fprintf(fp," | Flag_is_Con");
      } else {
        fprintf(fp,"init_flags(Flag_is_Con");
        node_flags_set = true;
      }
    }

    // flag: if this instruction is cisc alternate
    if ( can_cisc_spill() && instr->is_cisc_alternate() ) {
      if ( node_flags_set ) {
        fprintf(fp," | Flag_is_cisc_alternate");
      } else {
        fprintf(fp,"init_flags(Flag_is_cisc_alternate");
        node_flags_set = true;
      }
    }

    // flag: if this instruction has short branch form
    if ( instr->has_short_branch_form() ) {
      if ( node_flags_set ) {
        fprintf(fp," | Flag_may_be_short_branch");
      } else {
        fprintf(fp,"init_flags(Flag_may_be_short_branch");
        node_flags_set = true;
      }
    }

    // flag: if this instruction should not be generated back to back.
    if (avoid_back_to_back_attr != NULL) {
      if (node_flags_set) {
        fprintf(fp," | (%s)", avoid_back_to_back_attr->_val);
      } else {
        fprintf(fp,"init_flags((%s)", avoid_back_to_back_attr->_val);
        node_flags_set = true;
      }
    }

    // Check if machine instructions that USE memory, but do not DEF memory,
    // depend upon a node that defines memory in machine-independent graph.
    if ( instr->needs_anti_dependence_check(_globalNames) ) {
      if ( node_flags_set ) {
        fprintf(fp," | Flag_needs_anti_dependence_check");
      } else {
        fprintf(fp,"init_flags(Flag_needs_anti_dependence_check");
        node_flags_set = true;
      }
    }

    // flag: if this instruction is implemented with a call
    if ( instr->_has_call ) {
      if ( node_flags_set ) {
        fprintf(fp," | Flag_has_call");
      } else {
        fprintf(fp,"init_flags(Flag_has_call");
        node_flags_set = true;
      }
    }

    if ( node_flags_set ) {
      fprintf(fp,"); ");
    }

    fprintf(fp,"}\n");

    // size_of, used by base class's clone to obtain the correct size.
    fprintf(fp,"  virtual uint           size_of() const {");
    fprintf(fp,   " return sizeof(%sNode);", instr->_ident);
    fprintf(fp, " }\n");

    // Virtual methods which are only generated to override base class
    if( instr->expands() || instr->needs_projections() ||
        instr->has_temps() ||
        instr->is_mach_constant() ||
        instr->needs_constant_base() ||
        (instr->_matrule != NULL &&
         instr->num_opnds() != instr->num_unique_opnds()) ) {
      fprintf(fp,"  virtual MachNode      *Expand(State *state, Node_List &proj_list, Node* mem);\n");
    }

    if (instr->is_pinned(_globalNames)) {
      fprintf(fp,"  virtual bool           pinned() const { return ");
      if (instr->is_parm(_globalNames)) {
        fprintf(fp,"_in[0]->pinned();");
      } else {
        fprintf(fp,"true;");
      }
      fprintf(fp," }\n");
    }
    if (instr->is_projection(_globalNames)) {
      fprintf(fp,"  virtual const Node *is_block_proj() const { return this; }\n");
    }
    if ( instr->num_post_match_opnds() != 0
         || instr->is_chain_of_constant(_globalNames) ) {
      fprintf(fp,"  friend MachNode *State::MachNodeGenerator(int opcode);\n");
    }
    if ( instr->rematerialize(_globalNames, get_registers()) ) {
      fprintf(fp,"  // Rematerialize %s\n", instr->_ident);
    }

    // Declare short branch methods, if applicable
    instr->declare_short_branch_methods(fp);

    // See if there is an "ins_pipe" declaration for this instruction
    if (instr->_ins_pipe) {
      fprintf(fp,"  static  const Pipeline *pipeline_class();\n");
      fprintf(fp,"  virtual const Pipeline *pipeline() const;\n");
    }

    // Generate virtual function for MachNodeX::bottom_type when necessary
    //
    // Note on accuracy:  Pointer-types of machine nodes need to be accurate,
    // or else alias analysis on the matched graph may produce bad code.
    // Moreover, the aliasing decisions made on machine-node graph must be
    // no less accurate than those made on the ideal graph, or else the graph
    // may fail to schedule.  (Reason:  Memory ops which are reordered in
    // the ideal graph might look interdependent in the machine graph,
    // thereby removing degrees of scheduling freedom that the optimizer
    // assumed would be available.)
    //
    // %%% We should handle many of these cases with an explicit ADL clause:
    // instruct foo() %{ ... bottom_type(TypeRawPtr::BOTTOM); ... %}
    if( data_type != Form::none ) {
      // A constant's bottom_type returns a Type containing its constant value

      // !!!!!
      // Convert all ints, floats, ... to machine-independent TypeXs
      // as is done for pointers
      //
      // Construct appropriate constant type containing the constant value.
      fprintf(fp,"  virtual const class Type *bottom_type() const {\n");
      switch( data_type ) {
      case Form::idealI:
        fprintf(fp,"    return  TypeInt::make(opnd_array(1)->constant());\n");
        break;
      case Form::idealP:
      case Form::idealN:
      case Form::idealNKlass:
        fprintf(fp,"    return  opnd_array(1)->type();\n");
        break;
      case Form::idealD:
        fprintf(fp,"    return  TypeD::make(opnd_array(1)->constantD());\n");
        break;
      case Form::idealF:
        fprintf(fp,"    return  TypeF::make(opnd_array(1)->constantF());\n");
        break;
      case Form::idealL:
        fprintf(fp,"    return  TypeLong::make(opnd_array(1)->constantL());\n");
        break;
      default:
        assert( false, "Unimplemented()" );
        break;
      }
      fprintf(fp,"  };\n");
    }
/*    else if ( instr->_matrule && instr->_matrule->_rChild &&
        (  strcmp("ConvF2I",instr->_matrule->_rChild->_opType)==0
        || strcmp("ConvD2I",instr->_matrule->_rChild->_opType)==0 ) ) {
      // !!!!! !!!!!
      // Provide explicit bottom type for conversions to int
      // On Intel the result operand is a stackSlot, untyped.
      fprintf(fp,"  virtual const class Type *bottom_type() const {");
      fprintf(fp,   " return  TypeInt::INT;");
      fprintf(fp, " };\n");
    }*/
    else if( instr->is_ideal_copy() &&
              !strcmp(instr->_matrule->_lChild->_opType,"stackSlotP") ) {
      // !!!!!
      // Special hack for ideal Copy of pointer.  Bottom type is oop or not depending on input.
      fprintf(fp,"  const Type            *bottom_type() const { return in(1)->bottom_type(); } // Copy?\n");
    }
    else if( instr->is_ideal_loadPC() ) {
      // LoadPCNode provides the return address of a call to native code.
      // Define its bottom type to be TypeRawPtr::BOTTOM instead of TypePtr::BOTTOM
      // since it is a pointer to an internal VM location and must have a zero offset.
      // Allocation detects derived pointers, in part, by their non-zero offsets.
      fprintf(fp,"  const Type            *bottom_type() const { return TypeRawPtr::BOTTOM; } // LoadPC?\n");
    }
    else if( instr->is_ideal_box() ) {
      // BoxNode provides the address of a stack slot.
      // Define its bottom type to be TypeRawPtr::BOTTOM instead of TypePtr::BOTTOM
      // This prevent s insert_anti_dependencies from complaining. It will
      // complain if it sees that the pointer base is TypePtr::BOTTOM since
      // it doesn't understand what that might alias.
      fprintf(fp,"  const Type            *bottom_type() const { return TypeRawPtr::BOTTOM; } // Box?\n");
    }
    else if( instr->_matrule && instr->_matrule->_rChild && !strcmp(instr->_matrule->_rChild->_opType,"CMoveP") ) {
      int offset = 1;
      // Special special hack to see if the Cmp? has been incorporated in the conditional move
      MatchNode *rl = instr->_matrule->_rChild->_lChild;
      if( rl && !strcmp(rl->_opType, "Binary") ) {
          MatchNode *rlr = rl->_rChild;
          if (rlr && strncmp(rlr->_opType, "Cmp", 3) == 0)
            offset = 2;
      }
      // Special hack for ideal CMoveP; ideal type depends on inputs
      fprintf(fp,"  const Type            *bottom_type() const { const Type *t = in(oper_input_base()+%d)->bottom_type(); return (req() <= oper_input_base()+%d) ? t : t->meet(in(oper_input_base()+%d)->bottom_type()); } // CMoveP\n",
        offset, offset+1, offset+1);
    }
    else if( instr->_matrule && instr->_matrule->_rChild && !strcmp(instr->_matrule->_rChild->_opType,"CMoveN") ) {
      int offset = 1;
      // Special special hack to see if the Cmp? has been incorporated in the conditional move
      MatchNode *rl = instr->_matrule->_rChild->_lChild;
      if( rl && !strcmp(rl->_opType, "Binary") ) {
          MatchNode *rlr = rl->_rChild;
          if (rlr && strncmp(rlr->_opType, "Cmp", 3) == 0)
            offset = 2;
      }
      // Special hack for ideal CMoveN; ideal type depends on inputs
      fprintf(fp,"  const Type            *bottom_type() const { const Type *t = in(oper_input_base()+%d)->bottom_type(); return (req() <= oper_input_base()+%d) ? t : t->meet(in(oper_input_base()+%d)->bottom_type()); } // CMoveN\n",
        offset, offset+1, offset+1);
    }
    else if (instr->is_tls_instruction()) {
      // Special hack for tlsLoadP
      fprintf(fp,"  const Type            *bottom_type() const { return TypeRawPtr::BOTTOM; } // tlsLoadP\n");
    }
    else if ( instr->is_ideal_if() ) {
      fprintf(fp,"  const Type            *bottom_type() const { return TypeTuple::IFBOTH; } // matched IfNode\n");
    }
    else if ( instr->is_ideal_membar() ) {
      fprintf(fp,"  const Type            *bottom_type() const { return TypeTuple::MEMBAR; } // matched MemBar\n");
    }

    // Check where 'ideal_type' must be customized
    /*
    if ( instr->_matrule && instr->_matrule->_rChild &&
        (  strcmp("ConvF2I",instr->_matrule->_rChild->_opType)==0
        || strcmp("ConvD2I",instr->_matrule->_rChild->_opType)==0 ) ) {
      fprintf(fp,"  virtual uint           ideal_reg() const { return Compile::current()->matcher()->base2reg[Type::Int]; }\n");
    }*/

    // Analyze machine instructions that either USE or DEF memory.
    int memory_operand = instr->memory_operand(_globalNames);
    if ( memory_operand != InstructForm::NO_MEMORY_OPERAND ) {
      if( memory_operand == InstructForm::MANY_MEMORY_OPERANDS ) {
        fprintf(fp,"  virtual const TypePtr *adr_type() const;\n");
      }
      fprintf(fp,"  virtual const MachOper *memory_operand() const;\n");
    }

    fprintf(fp, "#ifndef PRODUCT\n");

    // virtual function for generating the user's assembler output
    gen_inst_format(fp, _globalNames,*instr);

    // Machine independent print functionality for debugging
    fprintf(fp,"  virtual const char    *Name() const { return \"%s\";}\n",
            instr->_ident);

    fprintf(fp, "#endif\n");

    // Close definition of this XxxMachNode
    fprintf(fp,"};\n");
  };

}

void ArchDesc::defineStateClass(FILE *fp) {
  static const char *state__valid    = "_rule[index] & 0x1";

  fprintf(fp,"\n");
  fprintf(fp,"// MACROS to inline and constant fold State::valid(index)...\n");
  fprintf(fp,"// when given a constant 'index' in dfa_<arch>.cpp\n");
  fprintf(fp,"#define STATE__NOT_YET_VALID(index) ");
  fprintf(fp,"  ( (%s) == 0 )\n", state__valid);
  fprintf(fp,"\n");
  fprintf(fp,"#define STATE__VALID_CHILD(state,index) ");
  fprintf(fp,"  ( state && (state->%s) )\n", state__valid);
  fprintf(fp,"\n");
  fprintf(fp,
          "//---------------------------State-------------------------------------------\n");
  fprintf(fp,"// State contains an integral cost vector, indexed by machine operand opcodes,\n");
  fprintf(fp,"// a rule vector consisting of machine operand/instruction opcodes, and also\n");
  fprintf(fp,"// indexed by machine operand opcodes, pointers to the children in the label\n");
  fprintf(fp,"// tree generated by the Label routines in ideal nodes (currently limited to\n");
  fprintf(fp,"// two for convenience, but this could change).\n");
  fprintf(fp,"class State : public ResourceObj {\n");
  fprintf(fp,"private:\n");
  fprintf(fp,"  unsigned int _cost[_LAST_MACH_OPER];  // Costs, indexed by operand opcodes\n");
  fprintf(fp,"  uint16_t     _rule[_LAST_MACH_OPER];  // Rule and validity, indexed by operand opcodes\n");
  fprintf(fp,"                                        // Lowest bit encodes validity\n");

  fprintf(fp,"public:\n");
  fprintf(fp,"  int    _id;                           // State identifier\n");
  fprintf(fp,"  Node  *_leaf;                         // Ideal (non-machine-node) leaf of match tree\n");
  fprintf(fp,"  State *_kids[2];                      // Children of state node in label tree\n");
  fprintf(fp,"\n");
  fprintf(fp,"  State(void);\n");
  fprintf(fp,"  DEBUG_ONLY( ~State(void); )\n");
  fprintf(fp,"\n");
  fprintf(fp,"  // Methods created by ADLC and invoked by Reduce\n");
  fprintf(fp,"  MachOper *MachOperGenerator(int opcode);\n");
  fprintf(fp,"  MachNode *MachNodeGenerator(int opcode);\n");
  fprintf(fp,"\n");
  fprintf(fp,"  // Assign a state to a node, definition of method produced by ADLC\n");
  fprintf(fp,"  bool DFA( int opcode, const Node *ideal );\n");
  fprintf(fp,"\n");
  fprintf(fp,"  bool valid(uint index) {\n");
  fprintf(fp,"    return %s;\n", state__valid);
  fprintf(fp,"  }\n");
  fprintf(fp,"  unsigned int rule(uint index) {\n");
  fprintf(fp,"    return _rule[index] >> 1;\n");
  fprintf(fp,"  }\n");
  fprintf(fp,"  unsigned int cost(uint index) {\n");
  fprintf(fp,"    return _cost[index];\n");
  fprintf(fp,"  }\n");
  fprintf(fp,"\n");
  fprintf(fp,"#ifndef PRODUCT\n");
  fprintf(fp,"  void dump();                // Debugging prints\n");
  fprintf(fp,"  void dump(int depth);\n");
  fprintf(fp,"#endif\n");
  if (_dfa_small) {
    // Generate the routine name we'll need
    for (int i = 1; i < _last_opcode; i++) {
      if (_mlistab[i] == NULL) continue;
      fprintf(fp, "  void  _sub_Op_%s(const Node *n);\n", NodeClassNames[i]);
    }
  }
  fprintf(fp,"};\n");
  fprintf(fp,"\n");
  fprintf(fp,"\n");

}


//---------------------------buildMachOperEnum---------------------------------
// Build enumeration for densely packed operands.
// This enumeration is used to index into the arrays in the State objects
// that indicate cost and a successfull rule match.

// Information needed to generate the ReduceOp mapping for the DFA
class OutputMachOperands : public OutputMap {
public:
  OutputMachOperands(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "MachOperands") {};

  void declaration() { }
  void definition()  { fprintf(_cpp, "enum MachOperands {\n"); }
  void closing()     { fprintf(_cpp, "  _LAST_MACH_OPER\n");
                       OutputMap::closing();
  }
  void map(OpClassForm &opc)  {
    const char* opc_ident_to_upper = _AD.machOperEnum(opc._ident);
    fprintf(_cpp, "  %s", opc_ident_to_upper);
    delete[] opc_ident_to_upper;
  }
  void map(OperandForm &oper) {
    const char* oper_ident_to_upper = _AD.machOperEnum(oper._ident);
    fprintf(_cpp, "  %s", oper_ident_to_upper);
    delete[] oper_ident_to_upper;
  }
  void map(char *name) {
    const char* name_to_upper = _AD.machOperEnum(name);
    fprintf(_cpp, "  %s", name_to_upper);
    delete[] name_to_upper;
  }

  bool do_instructions()      { return false; }
  void map(InstructForm &inst){ assert( false, "ShouldNotCallThis()"); }
};


void ArchDesc::buildMachOperEnum(FILE *fp_hpp) {
  // Construct the table for MachOpcodes
  OutputMachOperands output_mach_operands(fp_hpp, fp_hpp, _globalNames, *this);
  build_map(output_mach_operands);
}


//---------------------------buildMachEnum----------------------------------
// Build enumeration for all MachOpers and all MachNodes

// Information needed to generate the ReduceOp mapping for the DFA
class OutputMachOpcodes : public OutputMap {
  int begin_inst_chain_rule;
  int end_inst_chain_rule;
  int begin_rematerialize;
  int end_rematerialize;
  int end_instructions;
public:
  OutputMachOpcodes(FILE *hpp, FILE *cpp, FormDict &globals, ArchDesc &AD)
    : OutputMap(hpp, cpp, globals, AD, "MachOpcodes"),
      begin_inst_chain_rule(-1), end_inst_chain_rule(-1),
      begin_rematerialize(-1), end_rematerialize(-1),
      end_instructions(-1)
  {};

  void declaration() { }
  void definition()  { fprintf(_cpp, "enum MachOpcodes {\n"); }
  void closing()     {
    if( begin_inst_chain_rule != -1 )
      fprintf(_cpp, "  _BEGIN_INST_CHAIN_RULE = %d,\n", begin_inst_chain_rule);
    if( end_inst_chain_rule   != -1 )
      fprintf(_cpp, "  _END_INST_CHAIN_RULE  = %d,\n", end_inst_chain_rule);
    if( begin_rematerialize   != -1 )
      fprintf(_cpp, "  _BEGIN_REMATERIALIZE   = %d,\n", begin_rematerialize);
    if( end_rematerialize     != -1 )
      fprintf(_cpp, "  _END_REMATERIALIZE    = %d,\n", end_rematerialize);
    // always execute since do_instructions() is true, and avoids trailing comma
    fprintf(_cpp, "  _last_Mach_Node  = %d \n",  end_instructions);
    OutputMap::closing();
  }
  void map(OpClassForm &opc)  { fprintf(_cpp, "  %s_rule", opc._ident ); }
  void map(OperandForm &oper) { fprintf(_cpp, "  %s_rule", oper._ident ); }
  void map(char        *name) { if (name) fprintf(_cpp, "  %s_rule", name);
                                else      fprintf(_cpp, "  0"); }
  void map(InstructForm &inst) {fprintf(_cpp, "  %s_rule", inst._ident ); }

  void record_position(OutputMap::position place, int idx ) {
    switch(place) {
    case OutputMap::BEGIN_INST_CHAIN_RULES :
      begin_inst_chain_rule = idx;
      break;
    case OutputMap::END_INST_CHAIN_RULES :
      end_inst_chain_rule   = idx;
      break;
    case OutputMap::BEGIN_REMATERIALIZE :
      begin_rematerialize   = idx;
      break;
    case OutputMap::END_REMATERIALIZE :
      end_rematerialize     = idx;
      break;
    case OutputMap::END_INSTRUCTIONS :
      end_instructions      = idx;
      break;
    default:
      break;
    }
  }
};


void ArchDesc::buildMachOpcodesEnum(FILE *fp_hpp) {
  // Construct the table for MachOpcodes
  OutputMachOpcodes output_mach_opcodes(fp_hpp, fp_hpp, _globalNames, *this);
  build_map(output_mach_opcodes);
}


// Generate an enumeration of the pipeline states, and both
// the functional units (resources) and the masks for
// specifying resources
void ArchDesc::build_pipeline_enums(FILE *fp_hpp) {
  int stagelen = (int)strlen("undefined");
  int stagenum = 0;

  if (_pipeline) {              // Find max enum string length
    const char *stage;
    for ( _pipeline->_stages.reset(); (stage = _pipeline->_stages.iter()) != NULL; ) {
      int len = (int)strlen(stage);
      if (stagelen < len) stagelen = len;
    }
  }

  // Generate a list of stages
  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "// Pipeline Stages\n");
  fprintf(fp_hpp, "enum machPipelineStages {\n");
  fprintf(fp_hpp, "   stage_%-*s = 0,\n", stagelen, "undefined");

  if( _pipeline ) {
    const char *stage;
    for ( _pipeline->_stages.reset(); (stage = _pipeline->_stages.iter()) != NULL; )
      fprintf(fp_hpp, "   stage_%-*s = %d,\n", stagelen, stage, ++stagenum);
  }

  fprintf(fp_hpp, "   stage_%-*s = %d\n", stagelen, "count", stagenum);
  fprintf(fp_hpp, "};\n");

  fprintf(fp_hpp, "\n");
  fprintf(fp_hpp, "// Pipeline Resources\n");
  fprintf(fp_hpp, "enum machPipelineResources {\n");
  int rescount = 0;

  if( _pipeline ) {
    const char *resource;
    int reslen = 0;

    // Generate a list of resources, and masks
    for ( _pipeline->_reslist.reset(); (resource = _pipeline->_reslist.iter()) != NULL; ) {
      int len = (int)strlen(resource);
      if (reslen < len)
        reslen = len;
    }

    for ( _pipeline->_reslist.reset(); (resource = _pipeline->_reslist.iter()) != NULL; ) {
      const ResourceForm *resform = _pipeline->_resdict[resource]->is_resource();
      int mask = resform->mask();
      if ((mask & (mask-1)) == 0)
        fprintf(fp_hpp, "   resource_%-*s = %d,\n", reslen, resource, rescount++);
    }
    fprintf(fp_hpp, "\n");
    for ( _pipeline->_reslist.reset(); (resource = _pipeline->_reslist.iter()) != NULL; ) {
      const ResourceForm *resform = _pipeline->_resdict[resource]->is_resource();
      fprintf(fp_hpp, "   res_mask_%-*s = 0x%08x,\n", reslen, resource, resform->mask());
    }
    fprintf(fp_hpp, "\n");
  }
  fprintf(fp_hpp, "   resource_count = %d\n", rescount);
  fprintf(fp_hpp, "};\n");
}
