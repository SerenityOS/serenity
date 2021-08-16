/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

// ADLPARSE.CPP - Architecture Description Language Parser
// Authors: Chris Vick and Mike Paleczny
#include "adlc.hpp"

//----------------------------ADLParser----------------------------------------
// Create a new ADL parser
ADLParser::ADLParser(FileBuff& buffer, ArchDesc& archDesc)
  : _buf(buffer), _AD(archDesc),
    _globalNames(archDesc.globalNames()) {
  _AD._syntax_errs = _AD._semantic_errs = 0; // No errors so far this file
  _AD._warnings    = 0;                      // No warnings either
  _curline         = _ptr = NULL;            // No pointers into buffer yet

  _preproc_depth = 0;
  _preproc_not_taken = 0;

  // Delimit command-line definitions from in-file definitions:
  _AD._preproc_list.add_signal();
}

//------------------------------~ADLParser-------------------------------------
// Delete an ADL parser.
ADLParser::~ADLParser() {
  if (!_AD._quiet_mode)
    fprintf(stderr,"---------------------------- Errors and Warnings ----------------------------\n");
#ifndef ASSERT
  if (!_AD._quiet_mode) {
    fprintf(stderr, "**************************************************************\n");
    fprintf(stderr, "***** WARNING: ASSERT is undefined, assertions disabled. *****\n");
    fprintf(stderr, "**************************************************************\n");
  }
#endif
  if( _AD._syntax_errs + _AD._semantic_errs + _AD._warnings == 0 ) {
    if (!_AD._quiet_mode)
      fprintf(stderr,"No errors or warnings to report from phase-1 parse.\n" );
  }
  else {
    if( _AD._syntax_errs ) {      // Any syntax errors?
      fprintf(stderr,"%s:  Found %d syntax error", _buf._fp->_name, _AD._syntax_errs);
      if( _AD._syntax_errs > 1 ) fprintf(stderr,"s.\n\n");
      else fprintf(stderr,".\n\n");
    }
    if( _AD._semantic_errs ) {    // Any semantic errors?
      fprintf(stderr,"%s:  Found %d semantic error", _buf._fp->_name, _AD._semantic_errs);
      if( _AD._semantic_errs > 1 ) fprintf(stderr,"s.\n\n");
      else fprintf(stderr,".\n\n");
    }
    if( _AD._warnings ) {         // Any warnings?
      fprintf(stderr,"%s:  Found %d warning", _buf._fp->_name, _AD._warnings);
      if( _AD._warnings > 1 ) fprintf(stderr,"s.\n\n");
      else fprintf(stderr,".\n\n");
    }
  }
  if (!_AD._quiet_mode)
    fprintf(stderr,"-----------------------------------------------------------------------------\n");
  _AD._TotalLines += linenum()-1;     // -1 for overshoot in "nextline" routine

  // Write out information we have stored
  // // UNIXism == fsync(stderr);
}

//------------------------------parse------------------------------------------
// Each top-level keyword should appear as the first non-whitespace on a line.
//
void ADLParser::parse() {
  char *ident;

  // Iterate over the lines in the file buffer parsing Level 1 objects
  for( next_line(); _curline != NULL; next_line()) {
    _ptr = _curline;             // Reset ptr to start of new line
    skipws();                    // Skip any leading whitespace
    ident = get_ident();         // Get first token
    if (ident == NULL) {         // Empty line
      continue;                  // Get the next line
    }
         if (!strcmp(ident, "instruct"))   instr_parse();
    else if (!strcmp(ident, "operand"))    oper_parse();
    else if (!strcmp(ident, "opclass"))    opclass_parse();
    else if (!strcmp(ident, "ins_attrib")) ins_attr_parse();
    else if (!strcmp(ident, "op_attrib"))  op_attr_parse();
    else if (!strcmp(ident, "source"))     source_parse();
    else if (!strcmp(ident, "source_hpp")) source_hpp_parse();
    else if (!strcmp(ident, "register"))   reg_parse();
    else if (!strcmp(ident, "frame"))      frame_parse();
    else if (!strcmp(ident, "encode"))     encode_parse();
    else if (!strcmp(ident, "pipeline"))   pipe_parse();
    else if (!strcmp(ident, "definitions")) definitions_parse();
    else if (!strcmp(ident, "peephole"))   peep_parse();
    else if (!strcmp(ident, "#line"))      preproc_line();
    else if (!strcmp(ident, "#define"))    preproc_define();
    else if (!strcmp(ident, "#undef"))     preproc_undef();
    else {
      parse_err(SYNERR, "expected one of - instruct, operand, ins_attrib, op_attrib, source, register, pipeline, encode\n     Found %s",ident);
    }
  }
  // Add reg_class spill_regs after parsing.
  RegisterForm *regBlock = _AD.get_registers();
  if (regBlock == NULL) {
    parse_err(SEMERR, "Did not declare 'register' definitions");
  }
  regBlock->addSpillRegClass();
  regBlock->addDynamicRegClass();

  // Done with parsing, check consistency.

  if (_preproc_depth != 0) {
    parse_err(SYNERR, "End of file inside #ifdef");
  }

  // AttributeForms ins_cost and op_cost must be defined for default behaviour
  if (_globalNames[AttributeForm::_ins_cost] == NULL) {
    parse_err(SEMERR, "Did not declare 'ins_cost' attribute");
  }
  if (_globalNames[AttributeForm::_op_cost] == NULL) {
    parse_err(SEMERR, "Did not declare 'op_cost' attribute");
  }
}

// ******************** Private Level 1 Parse Functions ********************
//------------------------------instr_parse------------------------------------
// Parse the contents of an instruction definition, build the InstructForm to
// represent that instruction, and add it to the InstructForm list.
void ADLParser::instr_parse(void) {
  char          *ident;
  InstructForm  *instr;
  MatchRule     *rule;
  int            match_rules_cnt = 0;

  // First get the name of the instruction
  if( (ident = get_unique_ident(_globalNames,"instruction")) == NULL )
    return;
  instr = new InstructForm(ident); // Create new instruction form
  instr->_linenum = linenum();
  _globalNames.Insert(ident, instr); // Add name to the name table
  // Debugging Stuff
  if (_AD._adl_debug > 1)
    fprintf(stderr,"Parsing Instruction Form %s\n", ident);

  // Then get the operands
  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' in instruct definition\n");
  }
  // Parse the operand list
  else get_oplist(instr->_parameters, instr->_localNames);
  skipws();                        // Skip leading whitespace
  // Check for block delimiter
  if ( (_curchar != '%')
       || ( next_char(),  (_curchar != '{')) ) {
    parse_err(SYNERR, "missing '%%{' in instruction definition\n");
    return;
  }
  next_char();                     // Maintain the invariant
  do {
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at %c\n", _curchar);
      continue;
    }
    if      (!strcmp(ident, "predicate")) instr->_predicate = pred_parse();
    else if      (!strcmp(ident, "match")) {
      // Allow one instruction have several match rules.
      rule = instr->_matrule;
      if (rule == NULL) {
        // This is first match rule encountered
        rule = match_parse(instr->_localNames);
        if (rule) {
          instr->_matrule = rule;
          // Special case the treatment of Control instructions.
          if( instr->is_ideal_control() ) {
            // Control instructions return a special result, 'Universe'
            rule->_result = "Universe";
          }
          // Check for commutative operations with tree operands.
          matchrule_clone_and_swap(rule, instr->_ident, match_rules_cnt);
        }
      } else {
        // Find the end of the match rule list
        while (rule->_next != NULL)
          rule = rule->_next;
        // Add the new match rule to the list
        rule->_next = match_parse(instr->_localNames);
        if (rule->_next) {
          rule = rule->_next;
          if( instr->is_ideal_control() ) {
            parse_err(SYNERR, "unique match rule expected for %s\n", rule->_name);
            return;
          }
          assert(match_rules_cnt < 100," too many match rule clones");
          char* buf = (char*) AllocateHeap(strlen(instr->_ident) + 4);
          sprintf(buf, "%s_%d", instr->_ident, match_rules_cnt++);
          rule->_result = buf;
          // Check for commutative operations with tree operands.
          matchrule_clone_and_swap(rule, instr->_ident, match_rules_cnt);
        }
      }
    }
    else if (!strcmp(ident, "encode"))  {
      parse_err(SYNERR, "Instructions specify ins_encode, not encode\n");
    }
    else if (!strcmp(ident, "ins_encode"))       ins_encode_parse(*instr);
    // Parse late expand keyword.
    else if (!strcmp(ident, "postalloc_expand")) postalloc_expand_parse(*instr);
    else if (!strcmp(ident, "opcode"))           instr->_opcode    = opcode_parse(instr);
    else if (!strcmp(ident, "size"))             instr->_size      = size_parse(instr);
    else if (!strcmp(ident, "effect"))           effect_parse(instr);
    else if (!strcmp(ident, "expand"))           instr->_exprule   = expand_parse(instr);
    else if (!strcmp(ident, "rewrite"))          instr->_rewrule   = rewrite_parse();
    else if (!strcmp(ident, "constraint")) {
      parse_err(SYNERR, "Instructions do not specify a constraint\n");
    }
    else if (!strcmp(ident, "construct")) {
      parse_err(SYNERR, "Instructions do not specify a construct\n");
    }
    else if (!strcmp(ident, "format"))           instr->_format    = format_parse();
    else if (!strcmp(ident, "interface")) {
      parse_err(SYNERR, "Instructions do not specify an interface\n");
    }
    else if (!strcmp(ident, "ins_pipe"))        ins_pipe_parse(*instr);
    else {  // Done with staticly defined parts of instruction definition
      // Check identifier to see if it is the name of an attribute
      const Form    *form = _globalNames[ident];
      AttributeForm *attr = form ? form->is_attribute() : NULL;
      if (attr && (attr->_atype == INS_ATTR)) {
        // Insert the new attribute into the linked list.
        Attribute *temp = attr_parse(ident);
        temp->_next = instr->_attribs;
        instr->_attribs = temp;
      } else {
        parse_err(SYNERR, "expected one of:\n predicate, match, encode, or the name of"
                  " an instruction attribute at %s\n", ident);
      }
    }
    skipws();
  } while(_curchar != '%');
  next_char();
  if (_curchar != '}') {
    parse_err(SYNERR, "missing '%%}' in instruction definition\n");
    return;
  }
  // Check for "Set" form of chain rule
  adjust_set_rule(instr);
  if (_AD._pipeline) {
    // No pipe required for late expand.
    if (instr->expands() || instr->postalloc_expands()) {
      if (instr->_ins_pipe) {
        parse_err(WARN, "ins_pipe and expand rule both specified for instruction \"%s\";"
                  " ins_pipe will be unused\n", instr->_ident);
      }
    } else {
      if (!instr->_ins_pipe) {
        parse_err(WARN, "No ins_pipe specified for instruction \"%s\"\n", instr->_ident);
      }
    }
  }
  // Add instruction to tail of instruction list
  _AD.addForm(instr);

  // Create instruction form for each additional match rule
  rule = instr->_matrule;
  if (rule != NULL) {
    rule = rule->_next;
    while (rule != NULL) {
      ident = (char*)rule->_result;
      InstructForm *clone = new InstructForm(ident, instr, rule); // Create new instruction form
      _globalNames.Insert(ident, clone); // Add name to the name table
      // Debugging Stuff
      if (_AD._adl_debug > 1)
        fprintf(stderr,"Parsing Instruction Form %s\n", ident);
      // Check for "Set" form of chain rule
      adjust_set_rule(clone);
      // Add instruction to tail of instruction list
      _AD.addForm(clone);
      rule = rule->_next;
      clone->_matrule->_next = NULL; // One match rule per clone
    }
  }
}

//------------------------------matchrule_clone_and_swap-----------------------
// Check for commutative operations with subtree operands,
// create clones and swap operands.
void ADLParser::matchrule_clone_and_swap(MatchRule* rule, const char* instr_ident, int& match_rules_cnt) {
  // Check for commutative operations with tree operands.
  int count = 0;
  rule->count_commutative_op(count);
  if (count > 0) {
    // Clone match rule and swap commutative operation's operands.
    rule->matchrule_swap_commutative_op(instr_ident, count, match_rules_cnt);
  }
}

//------------------------------adjust_set_rule--------------------------------
// Check for "Set" form of chain rule
void ADLParser::adjust_set_rule(InstructForm *instr) {
  if (instr->_matrule == NULL || instr->_matrule->_rChild == NULL) return;
  const char *rch = instr->_matrule->_rChild->_opType;
  const Form *frm = _globalNames[rch];
  if( (! strcmp(instr->_matrule->_opType,"Set")) &&
      frm && frm->is_operand() && (! frm->ideal_only()) ) {
    // Previous implementation, which missed leaP*, but worked for loadCon*
    unsigned    position = 0;
    const char *result   = NULL;
    const char *name     = NULL;
    const char *optype   = NULL;
    MatchNode  *right    = instr->_matrule->_rChild;
    if (right->base_operand(position, _globalNames, result, name, optype)) {
      position = 1;
      const char *result2  = NULL;
      const char *name2    = NULL;
      const char *optype2  = NULL;
      // Can not have additional base operands in right side of match!
      if ( ! right->base_operand( position, _globalNames, result2, name2, optype2) ) {
        if (instr->_predicate != NULL)
          parse_err(SYNERR, "ADLC does not support instruction chain rules with predicates");
        // Chain from input  _ideal_operand_type_,
        // Needed for shared roots of match-trees
        ChainList *lst = (ChainList *)_AD._chainRules[optype];
        if (lst == NULL) {
          lst = new ChainList();
          _AD._chainRules.Insert(optype, lst);
        }
        if (!lst->search(instr->_matrule->_lChild->_opType)) {
          const char *cost = instr->cost();
          if (cost == NULL) {
            cost = ((AttributeForm*)_globalNames[AttributeForm::_ins_cost])->_attrdef;
          }
          // The ADLC does not support chaining from the ideal operand type
          // of a predicated user-defined operand
          if( frm->is_operand() == NULL || frm->is_operand()->_predicate == NULL ) {
            lst->insert(instr->_matrule->_lChild->_opType,cost,instr->_ident);
          }
        }
        // Chain from input  _user_defined_operand_type_,
        lst = (ChainList *)_AD._chainRules[result];
        if (lst == NULL) {
          lst = new ChainList();
          _AD._chainRules.Insert(result, lst);
        }
        if (!lst->search(instr->_matrule->_lChild->_opType)) {
          const char *cost = instr->cost();
          if (cost == NULL) {
            cost = ((AttributeForm*)_globalNames[AttributeForm::_ins_cost])->_attrdef;
          }
          // It is safe to chain from the top-level user-defined operand even
          // if it has a predicate, since the predicate is checked before
          // the user-defined type is available.
          lst->insert(instr->_matrule->_lChild->_opType,cost,instr->_ident);
        }
      } else {
        // May have instruction chain rule if root of right-tree is an ideal
        OperandForm *rightOp = _globalNames[right->_opType]->is_operand();
        if( rightOp ) {
          const Form *rightRoot = _globalNames[rightOp->_matrule->_opType];
          if( rightRoot && rightRoot->ideal_only() ) {
            const char *chain_op = NULL;
            if( rightRoot->is_instruction() )
              chain_op = rightOp->_ident;
            if( chain_op ) {
              // Look-up the operation in chain rule table
              ChainList *lst = (ChainList *)_AD._chainRules[chain_op];
              if (lst == NULL) {
                lst = new ChainList();
                _AD._chainRules.Insert(chain_op, lst);
              }
              // if (!lst->search(instr->_matrule->_lChild->_opType)) {
              const char *cost = instr->cost();
              if (cost == NULL) {
                cost = ((AttributeForm*)_globalNames[AttributeForm::_ins_cost])->_attrdef;
              }
              // This chains from a top-level operand whose predicate, if any,
              // has been checked.
              lst->insert(instr->_matrule->_lChild->_opType,cost,instr->_ident);
              // }
            }
          }
        }
      } // end chain rule from right-tree's ideal root
    }
  }
}


//------------------------------oper_parse-------------------------------------
void ADLParser::oper_parse(void) {
  char          *ident;
  OperandForm   *oper;
  AttributeForm *attr;
  MatchRule     *rule;

  // First get the name of the operand
  skipws();
  if( (ident = get_unique_ident(_globalNames,"operand")) == NULL )
    return;
  oper = new OperandForm(ident);        // Create new operand form
  oper->_linenum = linenum();
  _globalNames.Insert(ident, oper); // Add name to the name table

  // Debugging Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Parsing Operand Form %s\n", ident);

  // Get the component operands
  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' in operand definition\n");
    return;
  }
  else get_oplist(oper->_parameters, oper->_localNames); // Parse the component operand list
  skipws();
  // Check for block delimiter
  if ((_curchar != '%') || (*(_ptr+1) != '{')) { // If not open block
    parse_err(SYNERR, "missing '%%{' in operand definition\n");
    return;
  }
  next_char(); next_char();        // Skip over "%{" symbol
  do {
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at %c\n", _curchar);
      continue;
    }
    if      (!strcmp(ident, "predicate")) oper->_predicate = pred_parse();
    else if (!strcmp(ident, "match"))     {
      // Find the end of the match rule list
      rule = oper->_matrule;
      if (rule) {
        while (rule->_next) rule = rule->_next;
        // Add the new match rule to the list
        rule->_next = match_parse(oper->_localNames);
        if (rule->_next) {
          rule->_next->_result = oper->_ident;
        }
      }
      else {
        // This is first match rule encountered
        oper->_matrule = match_parse(oper->_localNames);
        if (oper->_matrule) {
          oper->_matrule->_result = oper->_ident;
        }
      }
    }
    else if (!strcmp(ident, "encode"))    oper->_interface = interface_parse();
    else if (!strcmp(ident, "ins_encode")) {
      parse_err(SYNERR, "Operands specify 'encode', not 'ins_encode'\n");
    }
    else if (!strcmp(ident, "opcode"))    {
      parse_err(SYNERR, "Operands do not specify an opcode\n");
    }
    else if (!strcmp(ident, "effect"))    {
      parse_err(SYNERR, "Operands do not specify an effect\n");
    }
    else if (!strcmp(ident, "expand"))    {
      parse_err(SYNERR, "Operands do not specify an expand\n");
    }
    else if (!strcmp(ident, "rewrite"))   {
      parse_err(SYNERR, "Operands do not specify a rewrite\n");
    }
    else if (!strcmp(ident, "constraint"))oper->_constraint= constraint_parse();
    else if (!strcmp(ident, "construct")) oper->_construct = construct_parse();
    else if (!strcmp(ident, "format"))    oper->_format    = format_parse();
    else if (!strcmp(ident, "interface")) oper->_interface = interface_parse();
    // Check identifier to see if it is the name of an attribute
    else if (((attr = _globalNames[ident]->is_attribute()) != NULL) &&
             (attr->_atype == OP_ATTR))   oper->_attribs   = attr_parse(ident);
    else {
      parse_err(SYNERR, "expected one of - constraint, predicate, match, encode, format, construct, or the name of a defined operand attribute at %s\n", ident);
    }
    skipws();
  } while(_curchar != '%');
  next_char();
  if (_curchar != '}') {
    parse_err(SYNERR, "missing '%%}' in operand definition\n");
    return;
  }
  // Add operand to tail of operand list
  _AD.addForm(oper);
}

//------------------------------opclass_parse----------------------------------
// Operand Classes are a block with a comma delimited list of operand names
void ADLParser::opclass_parse(void) {
  char          *ident;
  OpClassForm   *opc;
  OperandForm   *opForm;

  // First get the name of the operand class
  skipws();
  if( (ident = get_unique_ident(_globalNames,"opclass")) == NULL )
    return;
  opc = new OpClassForm(ident);             // Create new operand class form
  _globalNames.Insert(ident, opc);  // Add name to the name table

  // Debugging Stuff
  if (_AD._adl_debug > 1)
    fprintf(stderr,"Parsing Operand Class Form %s\n", ident);

  // Get the list of operands
  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' in operand definition\n");
    return;
  }
  do {
    next_char();                            // Skip past open paren or comma
    ident = get_ident();                    // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at %c\n", _curchar);
      continue;
    }
    // Check identifier to see if it is the name of an operand
    const Form *form = _globalNames[ident];
    opForm     = form ? form->is_operand() : NULL;
    if ( opForm ) {
      opc->_oplst.addName(ident);           // Add operand to opclass list
      opForm->_classes.addName(opc->_ident);// Add opclass to operand list
    }
    else {
      parse_err(SYNERR, "expected name of a defined operand at %s\n", ident);
    }
    skipws();                               // skip trailing whitespace
  } while (_curchar == ',');                // Check for the comma
  // Check for closing ')'
  if (_curchar != ')') {
    parse_err(SYNERR, "missing ')' or ',' in opclass definition\n");
    return;
  }
  next_char();                              // Consume the ')'
  skipws();
  // Check for closing ';'
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in opclass definition\n");
    return;
  }
  next_char();                             // Consume the ';'
  // Add operand to tail of operand list
  _AD.addForm(opc);
}

//------------------------------ins_attr_parse---------------------------------
void ADLParser::ins_attr_parse(void) {
  char          *ident;
  char          *aexpr;
  AttributeForm *attrib;

  // get name for the instruction attribute
  skipws();                      // Skip leading whitespace
  if( (ident = get_unique_ident(_globalNames,"inst_attrib")) == NULL )
    return;
  // Debugging Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Parsing Ins_Attribute Form %s\n", ident);

  // Get default value of the instruction attribute
  skipws();                      // Skip whitespace
  if ((aexpr = get_paren_expr("attribute default expression string")) == NULL) {
    parse_err(SYNERR, "missing '(' in ins_attrib definition\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Attribute Expression: %s\n", aexpr);

  // Check for terminator
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in ins_attrib definition\n");
    return;
  }
  next_char();                    // Advance past the ';'

  // Construct the attribute, record global name, and store in ArchDesc
  attrib = new AttributeForm(ident, INS_ATTR, aexpr);
  _globalNames.Insert(ident, attrib);  // Add name to the name table
  _AD.addForm(attrib);
}

//------------------------------op_attr_parse----------------------------------
void ADLParser::op_attr_parse(void) {
  char          *ident;
  char          *aexpr;
  AttributeForm *attrib;

  // get name for the operand attribute
  skipws();                      // Skip leading whitespace
  if( (ident = get_unique_ident(_globalNames,"op_attrib")) == NULL )
    return;
  // Debugging Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Parsing Op_Attribute Form %s\n", ident);

  // Get default value of the instruction attribute
  skipws();                      // Skip whitespace
  if ((aexpr = get_paren_expr("attribute default expression string")) == NULL) {
    parse_err(SYNERR, "missing '(' in op_attrib definition\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Attribute Expression: %s\n", aexpr);

  // Check for terminator
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in op_attrib definition\n");
    return;
  }
  next_char();                    // Advance past the ';'

  // Construct the attribute, record global name, and store in ArchDesc
  attrib = new AttributeForm(ident, OP_ATTR, aexpr);
  _globalNames.Insert(ident, attrib);
  _AD.addForm(attrib);
}

//------------------------------definitions_parse-----------------------------------
void ADLParser::definitions_parse(void) {
  skipws();                       // Skip leading whitespace
  if (_curchar == '%' && *(_ptr+1) == '{') {
    next_char(); next_char();     // Skip "%{"
    skipws();
    while (_curchar != '%' && *(_ptr+1) != '}') {
      // Process each definition until finding closing string "%}"
      char *token = get_ident();
      if (token == NULL) {
        parse_err(SYNERR, "missing identifier inside definitions block.\n");
        return;
      }
      if (strcmp(token,"int_def")==0)     { int_def_parse(); }
      // if (strcmp(token,"str_def")==0)   { str_def_parse(); }
      skipws();
    }
  }
  else {
    parse_err(SYNERR, "Missing %%{ ... %%} block after definitions keyword.\n");
    return;
  }
}

//------------------------------int_def_parse----------------------------------
// Parse Example:
// int_def    MEMORY_REF_COST      (         200,  DEFAULT_COST * 2);
// <keyword>  <name>               ( <int_value>,   <description>  );
//
void ADLParser::int_def_parse(void) {
  char *name        = NULL;         // Name of definition
  char *value       = NULL;         // its value,
  int   int_value   = -1;           // positive values only
  char *description = NULL;         // textual description

  // Get definition name
  skipws();                      // Skip whitespace
  name = get_ident();
  if (name == NULL) {
    parse_err(SYNERR, "missing definition name after int_def\n");
    return;
  }

  // Check for value of int_def dname( integer_value [, string_expression ] )
  skipws();
  if (_curchar == '(') {

    // Parse the integer value.
    next_char();
    value = get_ident();
    if (value == NULL) {
      parse_err(SYNERR, "missing value in int_def\n");
      return;
    }
    if( !is_int_token(value, int_value) ) {
      parse_err(SYNERR, "value in int_def is not recognized as integer\n");
      return;
    }
    skipws();

    // Check for description
    if (_curchar == ',') {
      next_char();   // skip ','

      description = get_expr("int_def description", ")");
      if (description == NULL) {
        parse_err(SYNERR, "invalid or missing description in int_def\n");
        return;
      }
      trim(description);
    }

    if (_curchar != ')') {
      parse_err(SYNERR, "missing ')' in register definition statement\n");
      return;
    }
    next_char();
  }

  // Check for closing ';'
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' after int_def\n");
    return;
  }
  next_char();                   // move past ';'

  // Debug Stuff
  if (_AD._adl_debug > 1) {
    fprintf(stderr,"int_def: %s ( %s, %s )\n", name,
            (value), (description ? description : ""));
  }

  // Record new definition.
  Expr *expr     = new Expr(name, description, int_value, int_value);
  const Expr *old_expr = _AD.globalDefs().define(name, expr);
  if (old_expr != NULL) {
    parse_err(SYNERR, "Duplicate definition\n");
    return;
  }

  return;
}


//------------------------------source_parse-----------------------------------
void ADLParser::source_parse(void) {
  SourceForm *source;             // Encode class for instruction/operand
  char   *rule = NULL;            // String representation of encode rule

  skipws();                       // Skip leading whitespace
  if ( (rule = find_cpp_block("source block")) == NULL ) {
    parse_err(SYNERR, "incorrect or missing block for 'source'.\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Source Form: %s\n", rule);

  source = new SourceForm(rule);    // Build new Source object
  _AD.addForm(source);
  // skipws();
}

//------------------------------source_hpp_parse-------------------------------
// Parse a source_hpp %{ ... %} block.
// The code gets stuck into the ad_<arch>.hpp file.
// If the source_hpp block appears before the register block in the AD
// file, it goes up at the very top of the ad_<arch>.hpp file, so that
// it can be used by register encodings, etc.  Otherwise, it goes towards
// the bottom, where it's useful as a global definition to *.cpp files.
void ADLParser::source_hpp_parse(void) {
  char   *rule = NULL;            // String representation of encode rule

  skipws();                       // Skip leading whitespace
  if ( (rule = find_cpp_block("source_hpp block")) == NULL ) {
    parse_err(SYNERR, "incorrect or missing block for 'source_hpp'.\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Header Form: %s\n", rule);

  if (_AD.get_registers() == NULL) {
    // Very early in the file, before reg_defs, we collect pre-headers.
    PreHeaderForm* pre_header = new PreHeaderForm(rule);
    _AD.addForm(pre_header);
  } else {
    // Normally, we collect header info, placed at the bottom of the hpp file.
    HeaderForm* header = new HeaderForm(rule);
    _AD.addForm(header);
  }
}

//------------------------------reg_parse--------------------------------------
void ADLParser::reg_parse(void) {
  RegisterForm *regBlock = _AD.get_registers(); // Information about registers encoding
  if (regBlock == NULL) {
    // Create the RegisterForm for the architecture description.
    regBlock = new RegisterForm();    // Build new Source object
    _AD.addForm(regBlock);
  }

  skipws();                       // Skip leading whitespace
  if (_curchar == '%' && *(_ptr+1) == '{') {
    next_char(); next_char();     // Skip "%{"
    skipws();
    while (_curchar != '%' && *(_ptr+1) != '}') {
      char *token = get_ident();
      if (token == NULL) {
        parse_err(SYNERR, "missing identifier inside register block.\n");
        return;
      }
      if (strcmp(token,"reg_def")==0)          { reg_def_parse(); }
      else if (strcmp(token,"reg_class")==0)   { reg_class_parse(); }
      else if (strcmp(token, "reg_class_dynamic") == 0) { reg_class_dynamic_parse(); }
      else if (strcmp(token,"alloc_class")==0) { alloc_class_parse(); }
      else if (strcmp(token,"#define")==0)     { preproc_define(); }
      else { parse_err(SYNERR, "bad token %s inside register block.\n", token); break; }
      skipws();
    }
  }
  else {
    parse_err(SYNERR, "Missing %c{ ... %c} block after register keyword.\n",'%','%');
    return;
  }
}

//------------------------------encode_parse-----------------------------------
void ADLParser::encode_parse(void) {
  EncodeForm *encBlock;         // Information about instruction/operand encoding

  _AD.getForm(&encBlock);
  if ( encBlock == NULL) {
    // Create the EncodeForm for the architecture description.
    encBlock = new EncodeForm();    // Build new Source object
    _AD.addForm(encBlock);
  }

  skipws();                       // Skip leading whitespace
  if (_curchar == '%' && *(_ptr+1) == '{') {
    next_char(); next_char();     // Skip "%{"
    skipws();
    while (_curchar != '%' && *(_ptr+1) != '}') {
      char *token = get_ident();
      if (token == NULL) {
            parse_err(SYNERR, "missing identifier inside encoding block.\n");
            return;
      }
      if (strcmp(token,"enc_class")==0)   { enc_class_parse(); }
      skipws();
    }
  }
  else {
    parse_err(SYNERR, "Missing %c{ ... %c} block after encode keyword.\n",'%','%');
    return;
  }
}

//------------------------------enc_class_parse--------------------------------
void ADLParser::enc_class_parse(void) {
  char       *ec_name;           // Name of encoding class being defined

  // Get encoding class name
  skipws();                      // Skip whitespace
  ec_name = get_ident();
  if (ec_name == NULL) {
    parse_err(SYNERR, "missing encoding class name after encode.\n");
    return;
  }

  EncClass  *encoding = _AD._encode->add_EncClass(ec_name);
  encoding->_linenum = linenum();

  skipws();                      // Skip leading whitespace
  // Check for optional parameter list
  if (_curchar == '(') {
    do {
      char *pType = NULL;        // parameter type
      char *pName = NULL;        // parameter name

      next_char();               // skip open paren & comma characters
      skipws();
      if (_curchar == ')') break;

      // Get parameter type
      pType = get_ident();
      if (pType == NULL) {
        parse_err(SYNERR, "parameter type expected at %c\n", _curchar);
        return;
      }

      skipws();
      // Get parameter name
      pName = get_ident();
      if (pName == NULL) {
        parse_err(SYNERR, "parameter name expected at %c\n", _curchar);
        return;
      }

      // Record parameter type and name
      encoding->add_parameter( pType, pName );

      skipws();
    } while(_curchar == ',');

    if (_curchar != ')') parse_err(SYNERR, "missing ')'\n");
    else {
      next_char();                  // Skip ')'
    }
  } // Done with parameter list

  skipws();
  // Check for block starting delimiters
  if ((_curchar != '%') || (*(_ptr+1) != '{')) { // If not open block
    parse_err(SYNERR, "missing '%c{' in enc_class definition\n", '%');
    return;
  }
  next_char();                      // Skip '%'
  next_char();                      // Skip '{'

  enc_class_parse_block(encoding, ec_name);
}


void ADLParser::enc_class_parse_block(EncClass* encoding, char* ec_name) {
  skipws_no_preproc();              // Skip leading whitespace
  // Prepend location descriptor, for debugging; cf. ADLParser::find_cpp_block
  if (_AD._adlocation_debug) {
    encoding->add_code(get_line_string());
  }

  // Collect the parts of the encode description
  // (1) strings that are passed through to output
  // (2) replacement/substitution variable, preceeded by a '$'
  while ( (_curchar != '%') && (*(_ptr+1) != '}') ) {

    // (1)
    // Check if there is a string to pass through to output
    char *start = _ptr;       // Record start of the next string
    while ((_curchar != '$') && ((_curchar != '%') || (*(_ptr+1) != '}')) ) {
      // If at the start of a comment, skip past it
      if( (_curchar == '/') && ((*(_ptr+1) == '/') || (*(_ptr+1) == '*')) ) {
        skipws_no_preproc();
      } else {
        // ELSE advance to the next character, or start of the next line
        next_char_or_line();
      }
    }
    // If a string was found, terminate it and record in EncClass
    if ( start != _ptr ) {
      *_ptr  = '\0';          // Terminate the string
      encoding->add_code(start);
    }

    // (2)
    // If we are at a replacement variable,
    // copy it and record in EncClass
    if (_curchar == '$') {
      // Found replacement Variable
      char* rep_var = get_rep_var_ident_dup();
      // Add flag to _strings list indicating we should check _rep_vars
      encoding->add_rep_var(rep_var);
    }
  } // end while part of format description
  next_char();                      // Skip '%'
  next_char();                      // Skip '}'

  skipws();

  if (_AD._adlocation_debug) {
    encoding->add_code(end_line_marker());
  }

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"EncodingClass Form: %s\n", ec_name);
}

//------------------------------frame_parse-----------------------------------
void ADLParser::frame_parse(void) {
  FrameForm  *frame;              // Information about stack-frame layout
  char       *desc = NULL;        // String representation of frame

  skipws();                       // Skip leading whitespace

  frame = new FrameForm();        // Build new Frame object
  // Check for open block sequence
  skipws();                       // Skip leading whitespace
  if (_curchar == '%' && *(_ptr+1) == '{') {
    next_char(); next_char();     // Skip "%{"
    skipws();
    while (_curchar != '%' && *(_ptr+1) != '}') {
      char *token = get_ident();
      if (token == NULL) {
            parse_err(SYNERR, "missing identifier inside frame block.\n");
            return;
      }
      if (strcmp(token,"sync_stack_slots")==0) {
        sync_stack_slots_parse(frame);
      }
      if (strcmp(token,"frame_pointer")==0) {
        frame_pointer_parse(frame, false);
      }
      if (strcmp(token,"interpreter_frame_pointer")==0) {
        interpreter_frame_pointer_parse(frame, false);
      }
      if (strcmp(token,"inline_cache_reg")==0) {
        inline_cache_parse(frame, false);
      }
      if (strcmp(token,"compiler_method_oop_reg")==0) {
        parse_err(WARN, "Using obsolete Token, compiler_method_oop_reg");
        skipws();
      }
      if (strcmp(token,"interpreter_method_oop_reg")==0) {
        parse_err(WARN, "Using obsolete Token, interpreter_method_oop_reg");
        skipws();
      }
      if (strcmp(token,"interpreter_method_reg")==0) {
        parse_err(WARN, "Using obsolete Token, interpreter_method_reg");
        skipws();
      }
      if (strcmp(token,"cisc_spilling_operand_name")==0) {
        cisc_spilling_operand_name_parse(frame, false);
      }
      if (strcmp(token,"stack_alignment")==0) {
        stack_alignment_parse(frame);
      }
      if (strcmp(token,"return_addr")==0) {
        return_addr_parse(frame, false);
      }
      if (strcmp(token,"in_preserve_stack_slots")==0) {
        parse_err(WARN, "Using obsolete token, in_preserve_stack_slots");
        skipws();
      }
      if (strcmp(token,"out_preserve_stack_slots")==0) {
        parse_err(WARN, "Using obsolete token, out_preserve_stack_slots");
        skipws();
      }
      if (strcmp(token,"varargs_C_out_slots_killed")==0) {
        frame->_varargs_C_out_slots_killed = parse_one_arg("varargs C out slots killed");
      }
      if (strcmp(token,"calling_convention")==0) {
        parse_err(WARN, "Using obsolete token, calling_convention");
        skipws();
      }
      if (strcmp(token,"return_value")==0) {
        frame->_return_value = return_value_parse();
      }
      if (strcmp(token,"c_frame_pointer")==0) {
        frame_pointer_parse(frame, true);
      }
      if (strcmp(token,"c_return_addr")==0) {
        return_addr_parse(frame, true);
      }
      if (strcmp(token,"c_calling_convention")==0) {
        parse_err(WARN, "Using obsolete token, c_calling_convention");
        skipws();
      }
      if (strcmp(token,"c_return_value")==0) {
        frame->_c_return_value = return_value_parse();
      }

      skipws();
    }
  }
  else {
    parse_err(SYNERR, "Missing %c{ ... %c} block after encode keyword.\n",'%','%');
    return;
  }
  // All Java versions are required, native versions are optional
  if(frame->_frame_pointer == NULL) {
    parse_err(SYNERR, "missing frame pointer definition in frame section.\n");
    return;
  }
  // !!!!! !!!!!
  // if(frame->_interpreter_frame_ptr_reg == NULL) {
  //   parse_err(SYNERR, "missing interpreter frame pointer definition in frame section.\n");
  //   return;
  // }
  if(frame->_alignment == NULL) {
    parse_err(SYNERR, "missing alignment definition in frame section.\n");
    return;
  }
  if(frame->_return_addr == NULL) {
    parse_err(SYNERR, "missing return address location in frame section.\n");
    return;
  }
  if(frame->_varargs_C_out_slots_killed == NULL) {
    parse_err(SYNERR, "missing varargs C out slots killed definition in frame section.\n");
    return;
  }
  if(frame->_return_value == NULL) {
    parse_err(SYNERR, "missing return value definition in frame section.\n");
    return;
  }
  // Fill natives in identically with the Java versions if not present.
  if(frame->_c_frame_pointer == NULL) {
    frame->_c_frame_pointer = frame->_frame_pointer;
  }
  if(frame->_c_return_addr == NULL) {
    frame->_c_return_addr = frame->_return_addr;
    frame->_c_return_addr_loc = frame->_return_addr_loc;
  }
  if(frame->_c_return_value == NULL) {
    frame->_c_return_value = frame->_return_value;
  }

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Frame Form: %s\n", desc);

  // Create the EncodeForm for the architecture description.
  _AD.addForm(frame);
  // skipws();
}

//------------------------------sync_stack_slots_parse-------------------------
void ADLParser::sync_stack_slots_parse(FrameForm *frame) {
    // Assign value into frame form
    frame->_sync_stack_slots = parse_one_arg("sync stack slots entry");
}

//------------------------------frame_pointer_parse----------------------------
void ADLParser::frame_pointer_parse(FrameForm *frame, bool native) {
  char *frame_pointer = parse_one_arg("frame pointer entry");
  // Assign value into frame form
  if (native) { frame->_c_frame_pointer = frame_pointer; }
  else        { frame->_frame_pointer   = frame_pointer; }
}

//------------------------------interpreter_frame_pointer_parse----------------------------
void ADLParser::interpreter_frame_pointer_parse(FrameForm *frame, bool native) {
  frame->_interpreter_frame_pointer_reg = parse_one_arg("interpreter frame pointer entry");
}

//------------------------------inline_cache_parse-----------------------------
void ADLParser::inline_cache_parse(FrameForm *frame, bool native) {
  frame->_inline_cache_reg = parse_one_arg("inline cache reg entry");
}

//------------------------------cisc_spilling_operand_parse---------------------
void ADLParser::cisc_spilling_operand_name_parse(FrameForm *frame, bool native) {
  frame->_cisc_spilling_operand_name = parse_one_arg("cisc spilling operand name");
}

//------------------------------stack_alignment_parse--------------------------
void ADLParser::stack_alignment_parse(FrameForm *frame) {
  char *alignment = parse_one_arg("stack alignment entry");
  // Assign value into frame
  frame->_alignment   = alignment;
}

//------------------------------parse_one_arg-------------------------------
char *ADLParser::parse_one_arg(const char *description) {
  char *token = NULL;
  if(_curchar == '(') {
    next_char();
    skipws();
    token = get_expr(description, ")");
    if (token == NULL) {
      parse_err(SYNERR, "missing value inside %s.\n", description);
      return NULL;
    }
    next_char();           // skip the close paren
    if(_curchar != ';') {  // check for semi-colon
      parse_err(SYNERR, "missing %c in.\n", ';', description);
      return NULL;
    }
    next_char();           // skip the semi-colon
  }
  else {
    parse_err(SYNERR, "Missing %c in.\n", '(', description);
    return NULL;
  }

  trim(token);
  return token;
}

//------------------------------return_addr_parse------------------------------
void ADLParser::return_addr_parse(FrameForm *frame, bool native) {
  bool in_register  = true;
  if(_curchar == '(') {
    next_char();
    skipws();
    char *token = get_ident();
    if (token == NULL) {
      parse_err(SYNERR, "missing value inside return address entry.\n");
      return;
    }
    // check for valid values for stack/register
    if (strcmp(token, "REG") == 0) {
      in_register = true;
    }
    else if (strcmp(token, "STACK") == 0) {
      in_register = false;
    }
    else {
      parse_err(SYNERR, "invalid value inside return_address entry.\n");
      return;
    }
    if (native) { frame->_c_return_addr_loc = in_register; }
    else        { frame->_return_addr_loc   = in_register; }

    // Parse expression that specifies register or stack position
    skipws();
    char *token2 = get_expr("return address entry", ")");
    if (token2 == NULL) {
      parse_err(SYNERR, "missing value inside return address entry.\n");
      return;
    }
    next_char();           // skip the close paren
    if (native) { frame->_c_return_addr = token2; }
    else        { frame->_return_addr   = token2; }

    if(_curchar != ';') {  // check for semi-colon
      parse_err(SYNERR, "missing %c in return address entry.\n", ';');
      return;
    }
    next_char();           // skip the semi-colon
  }
  else {
    parse_err(SYNERR, "Missing %c in return_address entry.\n", '(');
  }
}

//------------------------------return_value_parse-----------------------------
char *ADLParser::return_value_parse() {
  char   *desc = NULL;          // String representation of return_value

  skipws();                     // Skip leading whitespace
  if ( (desc = find_cpp_block("return value block")) == NULL ) {
    parse_err(SYNERR, "incorrect or missing block for 'return_value'.\n");
  }
  return desc;
}

//------------------------------ins_pipe_parse---------------------------------
void ADLParser::ins_pipe_parse(InstructForm &instr) {
  char * ident;

  skipws();
  if ( _curchar != '(' ) {       // Check for delimiter
    parse_err(SYNERR, "missing \"(\" in ins_pipe definition\n");
    return;
  }

  next_char();
  ident = get_ident();           // Grab next identifier

  if (ident == NULL) {
    parse_err(SYNERR, "keyword identifier expected at %c\n", _curchar);
    return;
  }

  skipws();
  if ( _curchar != ')' ) {       // Check for delimiter
    parse_err(SYNERR, "missing \")\" in ins_pipe definition\n");
    return;
  }

  next_char();                   // skip the close paren
  if(_curchar != ';') {          // check for semi-colon
    parse_err(SYNERR, "missing %c in return value entry.\n", ';');
    return;
  }
  next_char();                   // skip the semi-colon

  // Check ident for validity
  if (_AD._pipeline && !_AD._pipeline->_classlist.search(ident)) {
    parse_err(SYNERR, "\"%s\" is not a valid pipeline class\n", ident);
    return;
  }

  // Add this instruction to the list in the pipeline class
  _AD._pipeline->_classdict[ident]->is_pipeclass()->_instructs.addName(instr._ident);

  // Set the name of the pipeline class in the instruction
  instr._ins_pipe = ident;
  return;
}

//------------------------------pipe_parse-------------------------------------
void ADLParser::pipe_parse(void) {
  PipelineForm *pipeline;         // Encode class for instruction/operand
  char * ident;

  pipeline = new PipelineForm();  // Build new Source object
  _AD.addForm(pipeline);

  skipws();                       // Skip leading whitespace
  // Check for block delimiter
  if ( (_curchar != '%')
       || ( next_char(),  (_curchar != '{')) ) {
    parse_err(SYNERR, "missing '%%{' in pipeline definition\n");
    return;
  }
  next_char();                     // Maintain the invariant
  do {
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at %c\n", _curchar);
      continue;
    }
    if      (!strcmp(ident, "resources" )) resource_parse(*pipeline);
    else if (!strcmp(ident, "pipe_desc" )) pipe_desc_parse(*pipeline);
    else if (!strcmp(ident, "pipe_class")) pipe_class_parse(*pipeline);
    else if (!strcmp(ident, "define")) {
      skipws();
      if ( (_curchar != '%')
           || ( next_char(),  (_curchar != '{')) ) {
        parse_err(SYNERR, "expected '%%{'\n");
        return;
      }
      next_char(); skipws();

      char *node_class = get_ident();
      if (node_class == NULL) {
        parse_err(SYNERR, "expected identifier, found \"%c\"\n", _curchar);
        return;
      }

      skipws();
      if (_curchar != ',' && _curchar != '=') {
        parse_err(SYNERR, "expected `=`, found '%c'\n", _curchar);
        break;
      }
      next_char(); skipws();

      char *pipe_class = get_ident();
      if (pipe_class == NULL) {
        parse_err(SYNERR, "expected identifier, found \"%c\"\n", _curchar);
        return;
      }
      if (_curchar != ';' ) {
        parse_err(SYNERR, "expected `;`, found '%c'\n", _curchar);
        break;
      }
      next_char();              // Skip over semi-colon

      skipws();
      if ( (_curchar != '%')
           || ( next_char(),  (_curchar != '}')) ) {
        parse_err(SYNERR, "expected '%%}', found \"%c\"\n", _curchar);
      }
      next_char();

      // Check ident for validity
      if (_AD._pipeline && !_AD._pipeline->_classlist.search(pipe_class)) {
        parse_err(SYNERR, "\"%s\" is not a valid pipeline class\n", pipe_class);
        return;
      }

      // Add this machine node to the list in the pipeline class
      _AD._pipeline->_classdict[pipe_class]->is_pipeclass()->_instructs.addName(node_class);

      MachNodeForm *machnode = new MachNodeForm(node_class); // Create new machnode form
      machnode->_machnode_pipe = pipe_class;

      _AD.addForm(machnode);
    }
    else if (!strcmp(ident, "attributes")) {
      bool vsi_seen = false;

      skipws();
      if ( (_curchar != '%')
           || ( next_char(),  (_curchar != '{')) ) {
        parse_err(SYNERR, "expected '%%{'\n");
        return;
      }
      next_char(); skipws();

      while (_curchar != '%') {
        ident = get_ident();
        if (ident == NULL)
          break;

        if (!strcmp(ident, "variable_size_instructions")) {
          skipws();
          if (_curchar == ';') {
            next_char(); skipws();
          }

          pipeline->_variableSizeInstrs = true;
          vsi_seen = true;
          continue;
        }

        if (!strcmp(ident, "fixed_size_instructions")) {
          skipws();
          if (_curchar == ';') {
            next_char(); skipws();
          }

          pipeline->_variableSizeInstrs = false;
          vsi_seen = true;
          continue;
        }

        if (!strcmp(ident, "branch_has_delay_slot")) {
          skipws();
          if (_curchar == ';') {
            next_char(); skipws();
          }

          pipeline->_branchHasDelaySlot = true;
          continue;
        }

        if (!strcmp(ident, "max_instructions_per_bundle")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`\n");
            break;
            }

          next_char(); skipws();
          pipeline->_maxInstrsPerBundle = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "max_bundles_per_cycle")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`\n");
            break;
            }

          next_char(); skipws();
          pipeline->_maxBundlesPerCycle = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "instruction_unit_size")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`, found '%c'\n", _curchar);
            break;
            }

          next_char(); skipws();
          pipeline->_instrUnitSize = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "bundle_unit_size")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`, found '%c'\n", _curchar);
            break;
            }

          next_char(); skipws();
          pipeline->_bundleUnitSize = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "instruction_fetch_unit_size")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`, found '%c'\n", _curchar);
            break;
            }

          next_char(); skipws();
          pipeline->_instrFetchUnitSize = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "instruction_fetch_units")) {
          skipws();
          if (_curchar != '=') {
            parse_err(SYNERR, "expected `=`, found '%c'\n", _curchar);
            break;
            }

          next_char(); skipws();
          pipeline->_instrFetchUnits = get_int();
          skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        if (!strcmp(ident, "nops")) {
          skipws();
          if (_curchar != '(') {
            parse_err(SYNERR, "expected `(`, found '%c'\n", _curchar);
            break;
            }

          next_char(); skipws();

          while (_curchar != ')') {
            ident = get_ident();
            if (ident == NULL) {
              parse_err(SYNERR, "expected identifier for nop instruction, found '%c'\n", _curchar);
              break;
            }

            pipeline->_noplist.addName(ident);
            pipeline->_nopcnt++;
            skipws();

            if (_curchar == ',') {
              next_char(); skipws();
            }
          }

          next_char(); skipws();

          if (_curchar == ';') {
            next_char(); skipws();
          }

          continue;
        }

        parse_err(SYNERR, "unknown specifier \"%s\"\n", ident);
      }

      if ( (_curchar != '%')
           || ( next_char(),  (_curchar != '}')) ) {
        parse_err(SYNERR, "expected '%%}', found \"%c\"\n", _curchar);
      }
      next_char(); skipws();

      if (pipeline->_maxInstrsPerBundle == 0)
        parse_err(SYNERR, "\"max_instructions_per_bundle\" unspecified\n");
      if (pipeline->_instrUnitSize == 0 && pipeline->_bundleUnitSize == 0)
        parse_err(SYNERR, "\"instruction_unit_size\" and \"bundle_unit_size\" unspecified\n");
      if (pipeline->_instrFetchUnitSize == 0)
        parse_err(SYNERR, "\"instruction_fetch_unit_size\" unspecified\n");
      if (pipeline->_instrFetchUnits == 0)
        parse_err(SYNERR, "\"instruction_fetch_units\" unspecified\n");
      if (!vsi_seen)
        parse_err(SYNERR, "\"variable_size_instruction\" or \"fixed_size_instruction\" unspecified\n");
    }
    else {  // Done with staticly defined parts of instruction definition
      parse_err(SYNERR, "expected one of \"resources\", \"pipe_desc\", \"pipe_class\", found \"%s\"\n", ident);
      return;
    }
    skipws();
    if (_curchar == ';')
      skipws();
  } while(_curchar != '%');

  next_char();
  if (_curchar != '}') {
    parse_err(SYNERR, "missing \"%%}\" in pipeline definition\n");
    return;
  }

  next_char();
}

//------------------------------resource_parse----------------------------
void ADLParser::resource_parse(PipelineForm &pipeline) {
  ResourceForm *resource;
  char * ident;
  char * expr;
  unsigned mask;
  pipeline._rescount = 0;

  skipws();                       // Skip leading whitespace

  if (_curchar != '(') {
    parse_err(SYNERR, "missing \"(\" in resource definition\n");
    return;
  }

  do {
    next_char();                   // Skip "(" or ","
    ident = get_ident();           // Grab next identifier

    if (_AD._adl_debug > 1) {
      if (ident != NULL) {
        fprintf(stderr, "resource_parse: identifier: %s\n", ident);
      }
    }

    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
      return;
    }
    skipws();

    if (_curchar != '=') {
      mask = (1 << pipeline._rescount++);
    }
    else {
      next_char(); skipws();
      expr = get_ident();          // Grab next identifier
      if (expr == NULL) {
        parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
        return;
      }
      resource = (ResourceForm *) pipeline._resdict[expr];
      if (resource == NULL) {
        parse_err(SYNERR, "resource \"%s\" is not defined\n", expr);
        return;
      }
      mask = resource->mask();

      skipws();
      while (_curchar == '|') {
        next_char(); skipws();

        expr = get_ident();          // Grab next identifier
        if (expr == NULL) {
          parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
          return;
        }

        resource = (ResourceForm *) pipeline._resdict[expr];   // Look up the value
        if (resource == NULL) {
          parse_err(SYNERR, "resource \"%s\" is not defined\n", expr);
          return;
        }

        mask |= resource->mask();
        skipws();
      }
    }

    resource = new ResourceForm(mask);

    pipeline._resdict.Insert(ident, resource);
    pipeline._reslist.addName(ident);
  } while (_curchar == ',');

  if (_curchar != ')') {
      parse_err(SYNERR, "\")\" expected at \"%c\"\n", _curchar);
      return;
  }

  next_char();                 // Skip ")"
  if (_curchar == ';')
    next_char();               // Skip ";"
}

//------------------------------resource_parse----------------------------
void ADLParser::pipe_desc_parse(PipelineForm &pipeline) {
  char * ident;

  skipws();                       // Skip leading whitespace

  if (_curchar != '(') {
    parse_err(SYNERR, "missing \"(\" in pipe_desc definition\n");
    return;
  }

  do {
    next_char();                   // Skip "(" or ","
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
      return;
    }

    // Add the name to the list
    pipeline._stages.addName(ident);
    pipeline._stagecnt++;

    skipws();
  } while (_curchar == ',');

  if (_curchar != ')') {
      parse_err(SYNERR, "\")\" expected at \"%c\"\n", _curchar);
      return;
  }

  next_char();                     // Skip ")"
  if (_curchar == ';')
    next_char();                   // Skip ";"
}

//------------------------------pipe_class_parse--------------------------
void ADLParser::pipe_class_parse(PipelineForm &pipeline) {
  PipeClassForm *pipe_class;
  char * ident;
  char * stage;
  char * read_or_write;
  int is_write;
  int is_read;
  OperandForm  *oper;

  skipws();                       // Skip leading whitespace

  ident = get_ident();            // Grab next identifier

  if (ident == NULL) {
    parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
    return;
  }

  // Create a record for the pipe_class
  pipe_class = new PipeClassForm(ident, ++pipeline._classcnt);
  pipeline._classdict.Insert(ident, pipe_class);
  pipeline._classlist.addName(ident);

  // Then get the operands
  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing \"(\" in pipe_class definition\n");
  }
  // Parse the operand list
  else get_oplist(pipe_class->_parameters, pipe_class->_localNames);
  skipws();                        // Skip leading whitespace
  // Check for block delimiter
  if ( (_curchar != '%')
       || ( next_char(),  (_curchar != '{')) ) {
    parse_err(SYNERR, "missing \"%%{\" in pipe_class definition\n");
    return;
  }
  next_char();

  do {
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "keyword identifier expected at \"%c\"\n", _curchar);
      continue;
    }
    skipws();

    if (!strcmp(ident, "fixed_latency")) {
      skipws();
      if (_curchar != '(') {
        parse_err(SYNERR, "missing \"(\" in latency definition\n");
        return;
      }
      next_char(); skipws();
      if( !isdigit(_curchar) ) {
        parse_err(SYNERR, "number expected for \"%c\" in latency definition\n", _curchar);
        return;
      }
      int fixed_latency = get_int();
      skipws();
      if (_curchar != ')') {
        parse_err(SYNERR, "missing \")\" in latency definition\n");
        return;
      }
      next_char(); skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" in latency definition\n");
        return;
      }

      pipe_class->setFixedLatency(fixed_latency);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "zero_instructions") ||
        !strcmp(ident, "no_instructions")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" in latency definition\n");
        return;
      }

      pipe_class->setInstructionCount(0);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "one_instruction_with_delay_slot") ||
        !strcmp(ident, "single_instruction_with_delay_slot")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" in latency definition\n");
        return;
      }

      pipe_class->setInstructionCount(1);
      pipe_class->setBranchDelay(true);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "one_instruction") ||
        !strcmp(ident, "single_instruction")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" in latency definition\n");
        return;
      }

      pipe_class->setInstructionCount(1);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "instructions_in_first_bundle") ||
        !strcmp(ident, "instruction_count")) {
      skipws();

      int number_of_instructions = 1;

      if (_curchar != '(') {
        parse_err(SYNERR, "\"(\" expected at \"%c\"\n", _curchar);
        continue;
      }

      next_char(); skipws();
      number_of_instructions = get_int();

      skipws();
      if (_curchar != ')') {
        parse_err(SYNERR, "\")\" expected at \"%c\"\n", _curchar);
        continue;
      }

      next_char(); skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" in latency definition\n");
        return;
      }

      pipe_class->setInstructionCount(number_of_instructions);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "multiple_bundles")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" after multiple bundles\n");
        return;
      }

      pipe_class->setMultipleBundles(true);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "has_delay_slot")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" after \"has_delay_slot\"\n");
        return;
      }

      pipe_class->setBranchDelay(true);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "force_serialization")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" after \"force_serialization\"\n");
        return;
      }

      pipe_class->setForceSerialization(true);
      next_char(); skipws();
      continue;
    }

    if (!strcmp(ident, "may_have_no_code")) {
      skipws();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing \";\" after \"may_have_no_code\"\n");
        return;
      }

      pipe_class->setMayHaveNoCode(true);
      next_char(); skipws();
      continue;
    }

    const Form *parm = pipe_class->_localNames[ident];
    if (parm != NULL) {
      oper = parm->is_operand();
      if (oper == NULL && !parm->is_opclass()) {
        parse_err(SYNERR, "operand name expected at %s\n", ident);
        continue;
      }

      if (_curchar != ':') {
        parse_err(SYNERR, "\":\" expected at \"%c\"\n", _curchar);
        continue;
      }
      next_char(); skipws();
      stage = get_ident();
      if (stage == NULL) {
        parse_err(SYNERR, "pipeline stage identifier expected at \"%c\"\n", _curchar);
        continue;
      }

      skipws();
      if (_curchar != '(') {
        parse_err(SYNERR, "\"(\" expected at \"%c\"\n", _curchar);
        continue;
      }

      next_char();
      read_or_write = get_ident();
      if (read_or_write == NULL) {
        parse_err(SYNERR, "\"read\" or \"write\" expected at \"%c\"\n", _curchar);
        continue;
      }

      is_read  = strcmp(read_or_write, "read")   == 0;
      is_write = strcmp(read_or_write, "write")  == 0;
      if (!is_read && !is_write) {
        parse_err(SYNERR, "\"read\" or \"write\" expected at \"%c\"\n", _curchar);
        continue;
      }

      skipws();
      if (_curchar != ')') {
        parse_err(SYNERR, "\")\" expected at \"%c\"\n", _curchar);
        continue;
      }

      next_char(); skipws();
      int more_instrs = 0;
      if (_curchar == '+') {
          next_char(); skipws();
          if (_curchar < '0' || _curchar > '9') {
            parse_err(SYNERR, "<number> expected at \"%c\"\n", _curchar);
            continue;
          }
          while (_curchar >= '0' && _curchar <= '9') {
            more_instrs *= 10;
            more_instrs += _curchar - '0';
            next_char();
          }
          skipws();
      }

      PipeClassOperandForm *pipe_operand = new PipeClassOperandForm(stage, is_write, more_instrs);
      pipe_class->_localUsage.Insert(ident, pipe_operand);

      if (_curchar == '%')
          continue;

      if (_curchar != ';') {
        parse_err(SYNERR, "\";\" expected at \"%c\"\n", _curchar);
        continue;
      }
      next_char(); skipws();
      continue;
    }

    // Scan for Resource Specifier
    const Form *res = pipeline._resdict[ident];
    if (res != NULL) {
      int cyclecnt = 1;
      if (_curchar != ':') {
        parse_err(SYNERR, "\":\" expected at \"%c\"\n", _curchar);
        continue;
      }
      next_char(); skipws();
      stage = get_ident();
      if (stage == NULL) {
        parse_err(SYNERR, "pipeline stage identifier expected at \"%c\"\n", _curchar);
        continue;
      }

      skipws();
      if (_curchar == '(') {
        next_char();
        cyclecnt = get_int();

        skipws();
        if (_curchar != ')') {
          parse_err(SYNERR, "\")\" expected at \"%c\"\n", _curchar);
          continue;
        }

        next_char(); skipws();
      }

      PipeClassResourceForm *resource = new PipeClassResourceForm(ident, stage, cyclecnt);
      int stagenum = pipeline._stages.index(stage);
      if (pipeline._maxcycleused < (stagenum+cyclecnt))
        pipeline._maxcycleused = (stagenum+cyclecnt);
      pipe_class->_resUsage.addForm(resource);

      if (_curchar == '%')
          continue;

      if (_curchar != ';') {
        parse_err(SYNERR, "\";\" expected at \"%c\"\n", _curchar);
        continue;
      }
      next_char(); skipws();
      continue;
    }

    parse_err(SYNERR, "resource expected at \"%s\"\n", ident);
    return;
  } while(_curchar != '%');

  next_char();
  if (_curchar != '}') {
    parse_err(SYNERR, "missing \"%%}\" in pipe_class definition\n");
    return;
  }

  next_char();
}

//------------------------------peep_parse-------------------------------------
void ADLParser::peep_parse(void) {
  Peephole  *peep;                // Pointer to current peephole rule form
  char      *desc = NULL;         // String representation of rule

  skipws();                       // Skip leading whitespace

  peep = new Peephole();          // Build new Peephole object
  // Check for open block sequence
  skipws();                       // Skip leading whitespace
  if (_curchar == '%' && *(_ptr+1) == '{') {
    next_char(); next_char();     // Skip "%{"
    skipws();
    while (_curchar != '%' && *(_ptr+1) != '}') {
      char *token = get_ident();
      if (token == NULL) {
        parse_err(SYNERR, "missing identifier inside peephole rule.\n");
        return;
      }
      // check for legal subsections of peephole rule
      if (strcmp(token,"peepmatch")==0) {
        peep_match_parse(*peep); }
      else if (strcmp(token,"peepconstraint")==0) {
        peep_constraint_parse(*peep); }
      else if (strcmp(token,"peepreplace")==0) {
        peep_replace_parse(*peep); }
      else {
        parse_err(SYNERR, "expected peepmatch, peepconstraint, or peepreplace for identifier %s.\n", token);
      }
      skipws();
    }
  }
  else {
    parse_err(SYNERR, "Missing %%{ ... %%} block after peephole keyword.\n");
    return;
  }
  next_char();                    // Skip past '%'
  next_char();                    // Skip past '}'
}

// ******************** Private Level 2 Parse Functions ********************
//------------------------------constraint_parse------------------------------
Constraint *ADLParser::constraint_parse(void) {
  char *func;
  char *arg;

  // Check for constraint expression
  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing constraint expression, (...)\n");
    return NULL;
  }
  next_char();                    // Skip past '('

  // Get constraint function
  skipws();
  func = get_ident();
  if (func == NULL) {
    parse_err(SYNERR, "missing function in constraint expression.\n");
    return NULL;
  }
  if (strcmp(func,"ALLOC_IN_RC")==0
      || strcmp(func,"IS_R_CLASS")==0) {
    // Check for '(' before argument
    skipws();
    if (_curchar != '(') {
      parse_err(SYNERR, "missing '(' for constraint function's argument.\n");
      return NULL;
    }
    next_char();

    // Get it's argument
    skipws();
    arg = get_ident();
    if (arg == NULL) {
      parse_err(SYNERR, "missing argument for constraint function %s\n",func);
      return NULL;
    }
    // Check for ')' after argument
    skipws();
    if (_curchar != ')') {
      parse_err(SYNERR, "missing ')' after constraint function argument %s\n",arg);
      return NULL;
    }
    next_char();
  } else {
    parse_err(SYNERR, "Invalid constraint function %s\n",func);
    return NULL;
  }

  // Check for closing paren and ';'
  skipws();
  if (_curchar != ')') {
    parse_err(SYNERR, "Missing ')' for constraint function %s\n",func);
    return NULL;
  }
  next_char();
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "Missing ';' after constraint.\n");
    return NULL;
  }
  next_char();

  // Create new "Constraint"
  Constraint *constraint = new Constraint(func,arg);
  return constraint;
}

//------------------------------constr_parse-----------------------------------
ConstructRule *ADLParser::construct_parse(void) {
  return NULL;
}


//------------------------------reg_def_parse----------------------------------
void ADLParser::reg_def_parse(void) {
  char *rname;                   // Name of register being defined

  // Get register name
  skipws();                      // Skip whitespace
  rname = get_ident();
  if (rname == NULL) {
    parse_err(SYNERR, "missing register name after reg_def\n");
    return;
  }

  // Check for definition of register calling convention (save on call, ...),
  // register save type, and register encoding value.
  skipws();
  char *callconv  = NULL;
  char *c_conv    = NULL;
  char *idealtype = NULL;
  char *encoding  = NULL;
  char *concrete = NULL;
  if (_curchar == '(') {
    next_char();
    callconv = get_ident();
    // Parse the internal calling convention, must be NS, SOC, SOE, or AS.
    if (callconv == NULL) {
      parse_err(SYNERR, "missing register calling convention value\n");
      return;
    }
    if(strcmp(callconv, "SOC") && strcmp(callconv,"SOE") &&
       strcmp(callconv, "NS") && strcmp(callconv, "AS")) {
      parse_err(SYNERR, "invalid value for register calling convention\n");
    }
    skipws();
    if (_curchar != ',') {
      parse_err(SYNERR, "missing comma in register definition statement\n");
      return;
    }
    next_char();

    // Parse the native calling convention, must be NS, SOC, SOE, AS
    c_conv = get_ident();
    if (c_conv == NULL) {
      parse_err(SYNERR, "missing register native calling convention value\n");
      return;
    }
    if(strcmp(c_conv, "SOC") && strcmp(c_conv,"SOE") &&
       strcmp(c_conv, "NS") && strcmp(c_conv, "AS")) {
      parse_err(SYNERR, "invalid value for register calling convention\n");
    }
    skipws();
    if (_curchar != ',') {
      parse_err(SYNERR, "missing comma in register definition statement\n");
      return;
    }
    next_char();
    skipws();

    // Parse the ideal save type
    idealtype = get_ident();
    if (idealtype == NULL) {
      parse_err(SYNERR, "missing register save type value\n");
      return;
    }
    skipws();
    if (_curchar != ',') {
      parse_err(SYNERR, "missing comma in register definition statement\n");
      return;
    }
    next_char();
    skipws();

    // Parse the encoding value
    encoding = get_expr("encoding", ",");
    if (encoding == NULL) {
      parse_err(SYNERR, "missing register encoding value\n");
      return;
    }
    trim(encoding);
    if (_curchar != ',') {
      parse_err(SYNERR, "missing comma in register definition statement\n");
      return;
    }
    next_char();
    skipws();
    // Parse the concrete name type
    // concrete = get_ident();
    concrete = get_expr("concrete", ")");
    if (concrete == NULL) {
      parse_err(SYNERR, "missing vm register name value\n");
      return;
    }

    if (_curchar != ')') {
      parse_err(SYNERR, "missing ')' in register definition statement\n");
      return;
    }
    next_char();
  }

  // Check for closing ';'
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' after reg_def\n");
    return;
  }
  next_char();                   // move past ';'

  // Debug Stuff
  if (_AD._adl_debug > 1) {
    fprintf(stderr,"Register Definition: %s ( %s, %s %s )\n", rname,
            (callconv ? callconv : ""), (c_conv ? c_conv : ""), concrete);
  }

  // Record new register definition.
  _AD._register->addRegDef(rname, callconv, c_conv, idealtype, encoding, concrete);
  return;
}

//------------------------------reg_class_parse--------------------------------
void ADLParser::reg_class_parse(void) {
  char *cname;                    // Name of register class being defined

  // Get register class name
  skipws();                       // Skip leading whitespace
  cname = get_ident();
  if (cname == NULL) {
    parse_err(SYNERR, "missing register class name after 'reg_class'\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug >1) fprintf(stderr,"Register Class: %s\n", cname);

  skipws();
  if (_curchar == '(') {
    // A register list is defined for the register class.
    // Collect registers into a generic RegClass register class.
    RegClass* reg_class = _AD._register->addRegClass<RegClass>(cname);

    next_char();                  // Skip '('
    skipws();
    while (_curchar != ')') {
      char *rname = get_ident();
      if (rname==NULL) {
        parse_err(SYNERR, "missing identifier inside reg_class list.\n");
        return;
      }
      RegDef *regDef = _AD._register->getRegDef(rname);
      if (!regDef) {
        parse_err(SEMERR, "unknown identifier %s inside reg_class list.\n", rname);
      } else {
        reg_class->addReg(regDef); // add regDef to regClass
      }

      // Check for ',' and position to next token.
      skipws();
      if (_curchar == ',') {
        next_char();              // Skip trailing ','
        skipws();
      }
    }
    next_char();                  // Skip closing ')'
  } else if (_curchar == '%') {
    // A code snippet is defined for the register class.
    // Collect the code snippet into a CodeSnippetRegClass register class.
    CodeSnippetRegClass* reg_class = _AD._register->addRegClass<CodeSnippetRegClass>(cname);
    char *code = find_cpp_block("reg class");
    if (code == NULL) {
      parse_err(SYNERR, "missing code declaration for reg class.\n");
      return;
    }
    reg_class->set_code_snippet(code);
    return;
  }

  // Check for terminating ';'
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' at end of reg_class definition.\n");
    return;
  }
  next_char();                    // Skip trailing ';'

  // Check RegClass size, must be <= 32 registers in class.

  return;
}

//------------------------------reg_class_dynamic_parse------------------------
void ADLParser::reg_class_dynamic_parse(void) {
  char *cname; // Name of dynamic register class being defined

  // Get register class name
  skipws();
  cname = get_ident();
  if (cname == NULL) {
    parse_err(SYNERR, "missing dynamic register class name after 'reg_class_dynamic'\n");
    return;
  }

  if (_AD._adl_debug > 1) {
    fprintf(stdout, "Dynamic Register Class: %s\n", cname);
  }

  skipws();
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' at the beginning of reg_class_dynamic definition\n");
    return;
  }
  next_char();
  skipws();

  // Collect two register classes and the C++ code representing the condition code used to
  // select between the two classes into a ConditionalRegClass register class.
  ConditionalRegClass* reg_class = _AD._register->addRegClass<ConditionalRegClass>(cname);
  int i;
  for (i = 0; i < 2; i++) {
    char* name = get_ident();
    if (name == NULL) {
      parse_err(SYNERR, "missing class identifier inside reg_class_dynamic list.\n");
      return;
    }
    RegClass* rc = _AD._register->getRegClass(name);
    if (rc == NULL) {
      parse_err(SEMERR, "unknown identifier %s inside reg_class_dynamic list.\n", name);
    } else {
      reg_class->set_rclass_at_index(i, rc);
    }

    skipws();
    if (_curchar == ',') {
      next_char();
      skipws();
    } else {
      parse_err(SYNERR, "missing separator ',' inside reg_class_dynamic list.\n");
    }
  }

  // Collect the condition code.
  skipws();
  if (_curchar == '%') {
    char* code = find_cpp_block("reg class dynamic");
    if (code == NULL) {
       parse_err(SYNERR, "missing code declaration for reg_class_dynamic.\n");
       return;
    }
    reg_class->set_condition_code(code);
  } else {
    parse_err(SYNERR, "missing %% at the beginning of code block in reg_class_dynamic definition\n");
    return;
  }

  skipws();
  if (_curchar != ')') {
    parse_err(SYNERR, "missing ')' at the end of reg_class_dynamic definition\n");
    return;
  }
  next_char();

  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' at the end of reg_class_dynamic definition.\n");
    return;
  }
  next_char();                    // Skip trailing ';'

  return;
}

//------------------------------alloc_class_parse------------------------------
void ADLParser::alloc_class_parse(void) {
  char *name;                     // Name of allocation class being defined

  // Get allocation class name
  skipws();                       // Skip leading whitespace
  name = get_ident();
  if (name == NULL) {
    parse_err(SYNERR, "missing allocation class name after 'reg_class'\n");
    return;
  }
  // Debug Stuff
  if (_AD._adl_debug >1) fprintf(stderr,"Allocation Class: %s\n", name);

  AllocClass *alloc_class = _AD._register->addAllocClass(name);

  // Collect registers in class
  skipws();
  if (_curchar == '(') {
    next_char();                  // Skip '('
    skipws();
    while (_curchar != ')') {
      char *rname = get_ident();
      if (rname==NULL) {
        parse_err(SYNERR, "missing identifier inside reg_class list.\n");
        return;
      }
      // Check if name is a RegDef
      RegDef *regDef = _AD._register->getRegDef(rname);
      if (regDef) {
        alloc_class->addReg(regDef);   // add regDef to allocClass
      } else {

        // name must be a RegDef or a RegClass
        parse_err(SYNERR, "name %s should be a previously defined reg_def.\n", rname);
        return;
      }

      // Check for ',' and position to next token.
      skipws();
      if (_curchar == ',') {
        next_char();              // Skip trailing ','
        skipws();
      }
    }
    next_char();                  // Skip closing ')'
  }

  // Check for terminating ';'
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' at end of reg_class definition.\n");
    return;
  }
  next_char();                    // Skip trailing ';'

  return;
}

//------------------------------peep_match_child_parse-------------------------
InstructForm *ADLParser::peep_match_child_parse(PeepMatch &match, int parent, int &position, int input){
  char      *token  = NULL;
  int        lparen = 0;          // keep track of parenthesis nesting depth
  int        rparen = 0;          // position of instruction at this depth
  InstructForm *inst_seen  = NULL;

  // Walk the match tree,
  // Record <parent, position, instruction name, input position>
  while ( lparen >= rparen ) {
    skipws();
    // Left paren signals start of an input, collect with recursive call
    if (_curchar == '(') {
      ++lparen;
      next_char();
      ( void ) peep_match_child_parse(match, parent, position, rparen);
    }
    // Right paren signals end of an input, may be more
    else if (_curchar == ')') {
      ++rparen;
      if( rparen == lparen ) { // IF rparen matches an lparen I've seen
        next_char();           //    move past ')'
      } else {                 // ELSE leave ')' for parent
        assert( rparen == lparen + 1, "Should only see one extra ')'");
        // if an instruction was not specified for this paren-pair
        if( ! inst_seen ) {   // record signal entry
          match.add_instruction( parent, position, NameList::_signal, input );
          ++position;
        }
        // ++input;   // TEMPORARY
        return inst_seen;
      }
    }
    // if no parens, then check for instruction name
    // This instruction is the parent of a sub-tree
    else if ((token = get_ident_dup()) != NULL) {
      const Form *form = _AD._globalNames[token];
      if (form) {
        InstructForm *inst = form->is_instruction();
        // Record the first instruction at this level
        if( inst_seen == NULL ) {
          inst_seen = inst;
        }
        if (inst) {
          match.add_instruction( parent, position, token, input );
          parent = position;
          ++position;
        } else {
          parse_err(SYNERR, "instruction name expected at identifier %s.\n",
                    token);
          return inst_seen;
        }
      }
      else {
        parse_err(SYNERR, "missing identifier in peepmatch rule.\n");
        return NULL;
      }
    }
    else {
      parse_err(SYNERR, "missing identifier in peepmatch rule.\n");
      return NULL;
    }

  } // end while

  assert( false, "ShouldNotReachHere();");
  return NULL;
}

//------------------------------peep_match_parse-------------------------------
// Syntax for a peepmatch rule
//
// peepmatch ( root_instr_name [(instruction subtree)] [,(instruction subtree)]* );
//
void ADLParser::peep_match_parse(Peephole &peep) {

  skipws();
  // Check the structure of the rule
  // Check for open paren
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' at start of peepmatch rule.\n");
    return;
  }
  next_char();   // skip '('

  // Construct PeepMatch and parse the peepmatch rule.
  PeepMatch *match = new PeepMatch(_ptr);
  int  parent   = -1;                   // parent of root
  int  position = 0;                    // zero-based positions
  int  input    = 0;                    // input position in parent's operands
  InstructForm *root= peep_match_child_parse( *match, parent, position, input);
  if( root == NULL ) {
    parse_err(SYNERR, "missing instruction-name at start of peepmatch.\n");
    return;
  }

  if( _curchar != ')' ) {
    parse_err(SYNERR, "missing ')' at end of peepmatch.\n");
    return;
  }
  next_char();   // skip ')'

  // Check for closing semicolon
  skipws();
  if( _curchar != ';' ) {
    parse_err(SYNERR, "missing ';' at end of peepmatch.\n");
    return;
  }
  next_char();   // skip ';'

  // Store match into peep, and store peep into instruction
  peep.add_match(match);
  root->append_peephole(&peep);
}

//------------------------------peep_constraint_parse--------------------------
// Syntax for a peepconstraint rule
// A parenthesized list of relations between operands in peepmatch subtree
//
// peepconstraint %{
// (instruction_number.operand_name
//     relational_op
//  instruction_number.operand_name OR register_name
//  [, ...] );
//
// // instruction numbers are zero-based using topological order in peepmatch
//
void ADLParser::peep_constraint_parse(Peephole &peep) {

  skipws();
  // Check the structure of the rule
  // Check for open paren
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' at start of peepconstraint rule.\n");
    return;
  }
  else {
    next_char();                  // Skip '('
  }

  // Check for a constraint
  skipws();
  while( _curchar != ')' ) {
    // Get information on the left instruction and its operand
    // left-instructions's number
    int left_inst = get_int();
    // Left-instruction's operand
    skipws();
    if( _curchar != '.' ) {
      parse_err(SYNERR, "missing '.' in peepconstraint after instruction number.\n");
      return;
    }
    next_char();                  // Skip '.'
    char *left_op = get_ident_dup();

    skipws();
    // Collect relational operator
    char *relation = get_relation_dup();

    skipws();
    // Get information on the right instruction and its operand
    int right_inst;        // Right-instructions's number
    if( isdigit(_curchar) ) {
      right_inst = get_int();
      // Right-instruction's operand
      skipws();
      if( _curchar != '.' ) {
        parse_err(SYNERR, "missing '.' in peepconstraint after instruction number.\n");
        return;
      }
      next_char();              // Skip '.'
    } else {
      right_inst = -1;          // Flag as being a register constraint
    }

    char *right_op = get_ident_dup();

    // Construct the next PeepConstraint
    PeepConstraint *constraint = new PeepConstraint( left_inst, left_op,
                                                     relation,
                                                     right_inst, right_op );
    // And append it to the list for this peephole rule
    peep.append_constraint( constraint );

    // Check for another constraint, or end of rule
    skipws();
    if( _curchar == ',' ) {
      next_char();                // Skip ','
      skipws();
    }
    else if( _curchar != ')' ) {
      parse_err(SYNERR, "expected ',' or ')' after peephole constraint.\n");
      return;
    }
  } // end while( processing constraints )
  next_char();                    // Skip ')'

  // Check for terminating ';'
  skipws();
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' at end of peepconstraint.\n");
    return;
  }
  next_char();                    // Skip trailing ';'
}


//------------------------------peep_replace_parse-----------------------------
// Syntax for a peepreplace rule
// root instruction name followed by a
// parenthesized list of whitespace separated instruction.operand specifiers
//
// peepreplace ( instr_name  ( [instruction_number.operand_name]* ) );
//
//
void ADLParser::peep_replace_parse(Peephole &peep) {
  int          lparen = 0;        // keep track of parenthesis nesting depth
  int          rparen = 0;        // keep track of parenthesis nesting depth
  int          icount = 0;        // count of instructions in rule for naming
  char        *str    = NULL;
  char        *token  = NULL;

  skipws();
  // Check for open paren
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' at start of peepreplace rule.\n");
    return;
  }
  else {
    lparen++;
    next_char();
  }

  // Check for root instruction
  char       *inst = get_ident_dup();
  const Form *form = _AD._globalNames[inst];
  if( form == NULL || form->is_instruction() == NULL ) {
    parse_err(SYNERR, "Instruction name expected at start of peepreplace.\n");
    return;
  }

  // Store string representation of rule into replace
  PeepReplace *replace = new PeepReplace(str);
  replace->add_instruction( inst );

  skipws();
  // Start of root's operand-list
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' at peepreplace root's operand-list.\n");
    return;
  }
  else {
    lparen++;
    next_char();
  }

  skipws();
  // Get the list of operands
  while( _curchar != ')' ) {
    // Get information on an instruction and its operand
    // instructions's number
    int   inst_num = get_int();
    // Left-instruction's operand
    skipws();
    if( _curchar != '.' ) {
      parse_err(SYNERR, "missing '.' in peepreplace after instruction number.\n");
      return;
    }
    next_char();                  // Skip '.'
    char *inst_op = get_ident_dup();
    if( inst_op == NULL ) {
      parse_err(SYNERR, "missing operand identifier in peepreplace.\n");
      return;
    }

    // Record this operand's position in peepmatch
    replace->add_operand( inst_num, inst_op );
    skipws();
  }

  // Check for the end of operands list
  skipws();
  assert( _curchar == ')', "While loop should have advanced to ')'.");
  next_char();  // Skip ')'

  skipws();
  // Check for end of peepreplace
  if( _curchar != ')' ) {
    parse_err(SYNERR, "missing ')' at end of peepmatch.\n");
    parse_err(SYNERR, "Support one replacement instruction.\n");
    return;
  }
  next_char(); // Skip ')'

  // Check for closing semicolon
  skipws();
  if( _curchar != ';' ) {
    parse_err(SYNERR, "missing ';' at end of peepreplace.\n");
    return;
  }
  next_char();   // skip ';'

  // Store replace into peep
  peep.add_replace( replace );
}

//------------------------------pred_parse-------------------------------------
Predicate *ADLParser::pred_parse(void) {
  Predicate *predicate;           // Predicate class for operand
  char      *rule = NULL;         // String representation of predicate

  skipws();                       // Skip leading whitespace
  int line = linenum();
  if ( (rule = get_paren_expr("pred expression", true)) == NULL ) {
    parse_err(SYNERR, "incorrect or missing expression for 'predicate'\n");
    return NULL;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Predicate: %s\n", rule);
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in predicate definition\n");
    return NULL;
  }
  next_char();                     // Point after the terminator

  predicate = new Predicate(rule); // Build new predicate object
  skipws();
  return predicate;
}


//------------------------------ins_encode_parse_block-------------------------
// Parse the block form of ins_encode.  See ins_encode_parse for more details
void ADLParser::ins_encode_parse_block(InstructForm& inst) {
  // Create a new encoding name based on the name of the instruction
  // definition, which should be unique.
  const char* prefix = "__ins_encode_";
  char* ec_name = (char*) AllocateHeap(strlen(inst._ident) + strlen(prefix) + 1);
  sprintf(ec_name, "%s%s", prefix, inst._ident);

  assert(_AD._encode->encClass(ec_name) == NULL, "shouldn't already exist");
  EncClass* encoding = _AD._encode->add_EncClass(ec_name);
  encoding->_linenum = linenum();

  // synthesize the arguments list for the enc_class from the
  // arguments to the instruct definition.
  const char* param = NULL;
  inst._parameters.reset();
  while ((param = inst._parameters.iter()) != NULL) {
    OpClassForm* opForm = inst._localNames[param]->is_opclass();
    assert(opForm != NULL, "sanity");
    encoding->add_parameter(opForm->_ident, param);
  }

  if (!inst._is_postalloc_expand) {
    // Define a MacroAssembler instance for use by the encoding.  The
    // name is chosen to match the __ idiom used for assembly in other
    // parts of hotspot and assumes the existence of the standard
    // #define __ _masm.
    encoding->add_code("    C2_MacroAssembler _masm(&cbuf);\n");
  }

  // Parse the following %{ }% block
  ins_encode_parse_block_impl(inst, encoding, ec_name);

  // Build an encoding rule which invokes the encoding rule we just
  // created, passing all arguments that we received.
  InsEncode*   encrule = new InsEncode(); // Encode class for instruction
  NameAndList* params  = encrule->add_encode(ec_name);
  inst._parameters.reset();
  while ((param = inst._parameters.iter()) != NULL) {
    params->add_entry(param);
  }

  // Check for duplicate ins_encode sections after parsing the block
  // so that parsing can continue and find any other errors.
  if (inst._insencode != NULL) {
    parse_err(SYNERR, "Multiple ins_encode sections defined\n");
    return;
  }

  // Set encode class of this instruction.
  inst._insencode = encrule;
}


void ADLParser::ins_encode_parse_block_impl(InstructForm& inst, EncClass* encoding, char* ec_name) {
  skipws_no_preproc();              // Skip leading whitespace
  // Prepend location descriptor, for debugging; cf. ADLParser::find_cpp_block
  if (_AD._adlocation_debug) {
    encoding->add_code(get_line_string());
  }

  // Collect the parts of the encode description
  // (1) strings that are passed through to output
  // (2) replacement/substitution variable, preceeded by a '$'
  while ((_curchar != '%') && (*(_ptr+1) != '}')) {

    // (1)
    // Check if there is a string to pass through to output
    char *start = _ptr;       // Record start of the next string
    while ((_curchar != '$') && ((_curchar != '%') || (*(_ptr+1) != '}')) ) {
      // If at the start of a comment, skip past it
      if( (_curchar == '/') && ((*(_ptr+1) == '/') || (*(_ptr+1) == '*')) ) {
        skipws_no_preproc();
      } else {
        // ELSE advance to the next character, or start of the next line
        next_char_or_line();
      }
    }
    // If a string was found, terminate it and record in EncClass
    if (start != _ptr) {
      *_ptr = '\0';          // Terminate the string
      encoding->add_code(start);
    }

    // (2)
    // If we are at a replacement variable,
    // copy it and record in EncClass
    if (_curchar == '$') {
      // Found replacement Variable
      char* rep_var = get_rep_var_ident_dup();

      // Add flag to _strings list indicating we should check _rep_vars
      encoding->add_rep_var(rep_var);

      skipws();

      // Check if this instruct is a MachConstantNode.
      if (strcmp(rep_var, "constanttablebase") == 0) {
        // This instruct is a MachConstantNode.
        inst.set_needs_constant_base(true);
        if (strncmp("MachCall", inst.mach_base_class(_globalNames), strlen("MachCall")) != 0 ) {
          inst.set_is_mach_constant(true);
        }

        if (_curchar == '(')  {
          parse_err(SYNERR, "constanttablebase in instruct %s cannot have an argument "
                            "(only constantaddress and constantoffset)", ec_name);
          return;
        }
      }
      else if ((strcmp(rep_var, "constantaddress")   == 0) ||
               (strcmp(rep_var, "constantoffset")    == 0)) {
        // This instruct is a MachConstantNode.
        inst.set_is_mach_constant(true);

        // If the constant keyword has an argument, parse it.
        if (_curchar == '(')  constant_parse(inst);
      }
    }
  } // end while part of format description
  next_char();                      // Skip '%'
  next_char();                      // Skip '}'

  skipws();

  if (_AD._adlocation_debug) {
    encoding->add_code(end_line_marker());
  }

  // Debug Stuff
  if (_AD._adl_debug > 1)  fprintf(stderr, "EncodingClass Form: %s\n", ec_name);
}


//------------------------------ins_encode_parse-------------------------------
// Encode rules have the form
//   ins_encode( encode_class_name(parameter_list), ... );
//
// The "encode_class_name" must be defined in the encode section
// The parameter list contains $names that are locals.
//
// Alternatively it can be written like this:
//
//   ins_encode %{
//      ... // body
//   %}
//
// which synthesizes a new encoding class taking the same arguments as
// the InstructForm, and automatically prefixes the definition with:
//
//    C2_MacroAssembler masm(&cbuf);\n");
//
//  making it more compact to take advantage of the C2_MacroAssembler and
//  placing the assembly closer to it's use by instructions.
void ADLParser::ins_encode_parse(InstructForm& inst) {

  // Parse encode class name
  skipws();                        // Skip whitespace
  if (_curchar != '(') {
    // Check for ins_encode %{ form
    if ((_curchar == '%') && (*(_ptr+1) == '{')) {
      next_char();                      // Skip '%'
      next_char();                      // Skip '{'

      // Parse the block form of ins_encode
      ins_encode_parse_block(inst);
      return;
    }

    parse_err(SYNERR, "missing '%%{' or '(' in ins_encode definition\n");
    return;
  }
  next_char();                     // move past '('
  skipws();

  InsEncode *encrule  = new InsEncode(); // Encode class for instruction
  encrule->_linenum = linenum();
  char      *ec_name  = NULL;      // String representation of encode rule
  // identifier is optional.
  while (_curchar != ')') {
    ec_name = get_ident();
    if (ec_name == NULL) {
      parse_err(SYNERR, "Invalid encode class name after 'ins_encode('.\n");
      return;
    }
    // Check that encoding is defined in the encode section
    EncClass *encode_class = _AD._encode->encClass(ec_name);
    if (encode_class == NULL) {
      // Like to defer checking these till later...
      // parse_err(WARN, "Using an undefined encode class '%s' in 'ins_encode'.\n", ec_name);
    }

    // Get list for encode method's parameters
    NameAndList *params = encrule->add_encode(ec_name);

    // Parse the parameters to this encode method.
    skipws();
    if ( _curchar == '(' ) {
      next_char();                 // move past '(' for parameters

      // Parse the encode method's parameters
      while (_curchar != ')') {
        char *param = get_ident_or_literal_constant("encoding operand");
        if ( param != NULL ) {

          // Check if this instruct is a MachConstantNode.
          if (strcmp(param, "constanttablebase") == 0) {
            // This instruct is a MachConstantNode.
            inst.set_needs_constant_base(true);
            if (strncmp("MachCall", inst.mach_base_class(_globalNames), strlen("MachCall")) != 0 ) {
              inst.set_is_mach_constant(true);
            }

            if (_curchar == '(')  {
              parse_err(SYNERR, "constanttablebase in instruct %s cannot have an argument "
                        "(only constantaddress and constantoffset)", ec_name);
              return;
            }
          } else {
            // Found a parameter:
            // Check it is a local name, add it to the list, then check for more
            // New: allow hex constants as parameters to an encode method.
            // New: allow parenthesized expressions as parameters.
            // New: allow "primary", "secondary", "tertiary" as parameters.
            // New: allow user-defined register name as parameter
            if ( (inst._localNames[param] == NULL) &&
                 !ADLParser::is_literal_constant(param) &&
                 (Opcode::as_opcode_type(param) == Opcode::NOT_AN_OPCODE) &&
                 ((_AD._register == NULL ) || (_AD._register->getRegDef(param) == NULL)) ) {
              parse_err(SYNERR, "Using non-locally defined parameter %s for encoding %s.\n", param, ec_name);
              return;
            }
          }
          params->add_entry(param);

          skipws();
          if (_curchar == ',' ) {
            // More parameters to come
            next_char();           // move past ',' between parameters
            skipws();              // Skip to next parameter
          }
          else if (_curchar == ')') {
            // Done with parameter list
          }
          else {
            // Only ',' or ')' are valid after a parameter name
            parse_err(SYNERR, "expected ',' or ')' after parameter %s.\n",
                      ec_name);
            return;
          }

        } else {
          skipws();
          // Did not find a parameter
          if (_curchar == ',') {
            parse_err(SYNERR, "Expected encode parameter before ',' in encoding %s.\n", ec_name);
            return;
          }
          if (_curchar != ')') {
            parse_err(SYNERR, "Expected ')' after encode parameters.\n");
            return;
          }
        }
      } // WHILE loop collecting parameters
      next_char();                   // move past ')' at end of parameters
    } // done with parameter list for encoding

    // Check for ',' or ')' after encoding
    skipws();                      // move to character after parameters
    if ( _curchar == ',' ) {
      // Found a ','
      next_char();                 // move past ',' between encode methods
      skipws();
    }
    else if ( _curchar != ')' ) {
      // If not a ',' then only a ')' is allowed
      parse_err(SYNERR, "Expected ')' after encoding %s.\n", ec_name);
      return;
    }

    // Check for ',' separating parameters
    // if ( _curchar != ',' && _curchar != ')' ) {
    //   parse_err(SYNERR, "expected ',' or ')' after encode method inside ins_encode.\n");
    //   return NULL;
    // }

  } // done parsing ins_encode methods and their parameters
  if (_curchar != ')') {
    parse_err(SYNERR, "Missing ')' at end of ins_encode description.\n");
    return;
  }
  next_char();                     // move past ')'
  skipws();                        // Skip leading whitespace

  if ( _curchar != ';' ) {
    parse_err(SYNERR, "Missing ';' at end of ins_encode.\n");
    return;
  }
  next_char();                     // move past ';'
  skipws();                        // be friendly to oper_parse()

  // Check for duplicate ins_encode sections after parsing the block
  // so that parsing can continue and find any other errors.
  if (inst._insencode != NULL) {
    parse_err(SYNERR, "Multiple ins_encode sections defined\n");
    return;
  }

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Instruction Encode: %s\n", ec_name);

  // Set encode class of this instruction.
  inst._insencode = encrule;
}

//------------------------------postalloc_expand_parse---------------------------
// Encode rules have the form
//   postalloc_expand( encode_class_name(parameter_list) );
//
// The "encode_class_name" must be defined in the encode section.
// The parameter list contains $names that are locals.
//
// This is just a copy of ins_encode_parse without the loop.
void ADLParser::postalloc_expand_parse(InstructForm& inst) {
  inst._is_postalloc_expand = true;

  // Parse encode class name.
  skipws();                        // Skip whitespace.
  if (_curchar != '(') {
    // Check for postalloc_expand %{ form
    if ((_curchar == '%') && (*(_ptr+1) == '{')) {
      next_char();                      // Skip '%'
      next_char();                      // Skip '{'

      // Parse the block form of postalloc_expand
      ins_encode_parse_block(inst);
      return;
    }

    parse_err(SYNERR, "missing '(' in postalloc_expand definition\n");
    return;
  }
  next_char();                     // Move past '('.
  skipws();

  InsEncode *encrule = new InsEncode(); // Encode class for instruction.
  encrule->_linenum = linenum();
  char      *ec_name = NULL;       // String representation of encode rule.
  // identifier is optional.
  if (_curchar != ')') {
    ec_name = get_ident();
    if (ec_name == NULL) {
      parse_err(SYNERR, "Invalid postalloc_expand class name after 'postalloc_expand('.\n");
      return;
    }
    // Check that encoding is defined in the encode section.
    EncClass *encode_class = _AD._encode->encClass(ec_name);

    // Get list for encode method's parameters
    NameAndList *params = encrule->add_encode(ec_name);

    // Parse the parameters to this encode method.
    skipws();
    if (_curchar == '(') {
      next_char();                 // Move past '(' for parameters.

      // Parse the encode method's parameters.
      while (_curchar != ')') {
        char *param = get_ident_or_literal_constant("encoding operand");
        if (param != NULL) {
          // Found a parameter:

          // First check for constant table support.

          // Check if this instruct is a MachConstantNode.
          if (strcmp(param, "constanttablebase") == 0) {
            // This instruct is a MachConstantNode.
            inst.set_needs_constant_base(true);
            if (strncmp("MachCall", inst.mach_base_class(_globalNames), strlen("MachCall")) != 0 ) {
              inst.set_is_mach_constant(true);
            }

            if (_curchar == '(') {
              parse_err(SYNERR, "constanttablebase in instruct %s cannot have an argument "
                        "(only constantaddress and constantoffset)", ec_name);
              return;
            }
          }
          else if ((strcmp(param, "constantaddress") == 0) ||
                   (strcmp(param, "constantoffset")  == 0))  {
            // This instruct is a MachConstantNode.
            inst.set_is_mach_constant(true);

            // If the constant keyword has an argument, parse it.
            if (_curchar == '(') constant_parse(inst);
          }

          // Else check it is a local name, add it to the list, then check for more.
          // New: allow hex constants as parameters to an encode method.
          // New: allow parenthesized expressions as parameters.
          // New: allow "primary", "secondary", "tertiary" as parameters.
          // New: allow user-defined register name as parameter.
          else if ((inst._localNames[param] == NULL) &&
                   !ADLParser::is_literal_constant(param) &&
                   (Opcode::as_opcode_type(param) == Opcode::NOT_AN_OPCODE) &&
                   ((_AD._register == NULL) || (_AD._register->getRegDef(param) == NULL))) {
            parse_err(SYNERR, "Using non-locally defined parameter %s for encoding %s.\n", param, ec_name);
            return;
          }
          params->add_entry(param);

          skipws();
          if (_curchar == ',') {
            // More parameters to come.
            next_char();           // Move past ',' between parameters.
            skipws();              // Skip to next parameter.
          } else if (_curchar == ')') {
            // Done with parameter list
          } else {
            // Only ',' or ')' are valid after a parameter name.
            parse_err(SYNERR, "expected ',' or ')' after parameter %s.\n", ec_name);
            return;
          }

        } else {
          skipws();
          // Did not find a parameter.
          if (_curchar == ',') {
            parse_err(SYNERR, "Expected encode parameter before ',' in postalloc_expand %s.\n", ec_name);
            return;
          }
          if (_curchar != ')') {
            parse_err(SYNERR, "Expected ')' after postalloc_expand parameters.\n");
            return;
          }
        }
      } // WHILE loop collecting parameters.
      next_char();                 // Move past ')' at end of parameters.
    } // Done with parameter list for encoding.

    // Check for ',' or ')' after encoding.
    skipws();                      // Move to character after parameters.
    if (_curchar != ')') {
      // Only a ')' is allowed.
      parse_err(SYNERR, "Expected ')' after postalloc_expand %s.\n", ec_name);
      return;
    }
  } // Done parsing postalloc_expand method and their parameters.
  if (_curchar != ')') {
    parse_err(SYNERR, "Missing ')' at end of postalloc_expand description.\n");
    return;
  }
  next_char();                     // Move past ')'.
  skipws();                        // Skip leading whitespace.

  if (_curchar != ';') {
    parse_err(SYNERR, "Missing ';' at end of postalloc_expand.\n");
    return;
  }
  next_char();                     // Move past ';'.
  skipws();                        // Be friendly to oper_parse().

  // Debug Stuff.
  if (_AD._adl_debug > 1) fprintf(stderr, "Instruction postalloc_expand: %s\n", ec_name);

  // Set encode class of this instruction.
  inst._insencode = encrule;
}


//------------------------------constant_parse---------------------------------
// Parse a constant expression.
void ADLParser::constant_parse(InstructForm& inst) {
  // Create a new encoding name based on the name of the instruction
  // definition, which should be unique.
  const char* prefix = "__constant_";
  char* ec_name = (char*) AllocateHeap(strlen(inst._ident) + strlen(prefix) + 1);
  sprintf(ec_name, "%s%s", prefix, inst._ident);

  assert(_AD._encode->encClass(ec_name) == NULL, "shouldn't already exist");
  EncClass* encoding = _AD._encode->add_EncClass(ec_name);
  encoding->_linenum = linenum();

  // synthesize the arguments list for the enc_class from the
  // arguments to the instruct definition.
  const char* param = NULL;
  inst._parameters.reset();
  while ((param = inst._parameters.iter()) != NULL) {
    OpClassForm* opForm = inst._localNames[param]->is_opclass();
    assert(opForm != NULL, "sanity");
    encoding->add_parameter(opForm->_ident, param);
  }

  // Parse the following ( ) expression.
  constant_parse_expression(encoding, ec_name);

  // Build an encoding rule which invokes the encoding rule we just
  // created, passing all arguments that we received.
  InsEncode*   encrule = new InsEncode(); // Encode class for instruction
  NameAndList* params  = encrule->add_encode(ec_name);
  inst._parameters.reset();
  while ((param = inst._parameters.iter()) != NULL) {
    params->add_entry(param);
  }

  // Set encode class of this instruction.
  inst._constant = encrule;
}


//------------------------------constant_parse_expression----------------------
void ADLParser::constant_parse_expression(EncClass* encoding, char* ec_name) {
  skipws();

  // Prepend location descriptor, for debugging; cf. ADLParser::find_cpp_block
  if (_AD._adlocation_debug) {
    encoding->add_code(get_line_string());
  }

  // Start code line.
  encoding->add_code("    _constant = C->output()->constant_table().add");

  // Parse everything in ( ) expression.
  encoding->add_code("(this, ");
  next_char();  // Skip '('
  int parens_depth = 1;

  // Collect the parts of the constant expression.
  // (1) strings that are passed through to output
  // (2) replacement/substitution variable, preceeded by a '$'
  while (parens_depth > 0) {
    if (_curchar == '(') {
      parens_depth++;
      encoding->add_code("(");
      next_char();
    }
    else if (_curchar == ')') {
      parens_depth--;
      if (parens_depth > 0)
        encoding->add_code(")");
      next_char();
    }
    else {
      // (1)
      // Check if there is a string to pass through to output
      char *start = _ptr;  // Record start of the next string
      while ((_curchar != '$') && (_curchar != '(') && (_curchar != ')')) {
        next_char();
      }
      // If a string was found, terminate it and record in EncClass
      if (start != _ptr) {
        *_ptr = '\0';  // Terminate the string
        encoding->add_code(start);
      }

      // (2)
      // If we are at a replacement variable, copy it and record in EncClass.
      if (_curchar == '$') {
        // Found replacement Variable
        char* rep_var = get_rep_var_ident_dup();
        encoding->add_rep_var(rep_var);
      }
    }
  }

  // Finish code line.
  encoding->add_code(");");

  if (_AD._adlocation_debug) {
    encoding->add_code(end_line_marker());
  }

  // Debug Stuff
  if (_AD._adl_debug > 1)  fprintf(stderr, "EncodingClass Form: %s\n", ec_name);
}


//------------------------------size_parse-----------------------------------
// Parse a 'size(<expr>)' attribute which specifies the size of the
// emitted instructions in bytes. <expr> can be a C++ expression,
// e.g. a constant.
char* ADLParser::size_parse(InstructForm *instr) {
  char* sizeOfInstr = NULL;

  // Get value of the instruction's size
  skipws();

  // Parse size
  sizeOfInstr = get_paren_expr("size expression");
  if (sizeOfInstr == NULL) {
     parse_err(SYNERR, "size of opcode expected at %c\n", _curchar);
     return NULL;
  }

  skipws();

  // Check for terminator
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in ins_attrib definition\n");
    return NULL;
  }
  next_char();                     // Advance past the ';'
  skipws();                        // necessary for instr_parse()

  // Debug Stuff
  if (_AD._adl_debug > 1) {
    if (sizeOfInstr != NULL) {
      fprintf(stderr,"size of opcode: %s\n", sizeOfInstr);
    }
  }

  return sizeOfInstr;
}


//------------------------------opcode_parse-----------------------------------
Opcode * ADLParser::opcode_parse(InstructForm *instr) {
  char *primary   = NULL;
  char *secondary = NULL;
  char *tertiary  = NULL;

  char   *val    = NULL;
  Opcode *opcode = NULL;

  // Get value of the instruction's opcode
  skipws();
  if (_curchar != '(') {         // Check for parenthesized operand list
    parse_err(SYNERR, "missing '(' in expand instruction declaration\n");
    return NULL;
  }
  next_char();                   // skip open paren
  skipws();
  if (_curchar != ')') {
    // Parse primary, secondary, and tertiary opcodes, if provided.
    if ( (primary = get_ident_or_literal_constant("primary opcode")) == NULL ) {
          parse_err(SYNERR, "primary hex opcode expected at %c\n", _curchar);
        return NULL;
    }
    skipws();
    if (_curchar == ',') {
      next_char();
      skipws();
      // Parse secondary opcode
      if ( (secondary = get_ident_or_literal_constant("secondary opcode")) == NULL ) {
        parse_err(SYNERR, "secondary hex opcode expected at %c\n", _curchar);
        return NULL;
      }
      skipws();
      if (_curchar == ',') {
        next_char();
        skipws();
        // Parse tertiary opcode
        if ( (tertiary = get_ident_or_literal_constant("tertiary opcode")) == NULL ) {
          parse_err(SYNERR,"tertiary hex opcode expected at %c\n", _curchar);
          return NULL;
        }
        skipws();
      }
    }
    skipws();
    if (_curchar != ')') {
      parse_err(SYNERR, "Missing ')' in opcode description\n");
      return NULL;
    }
  }
  next_char();                     // Skip ')'
  skipws();
  // Check for terminator
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in ins_attrib definition\n");
    return NULL;
  }
  next_char();                     // Advance past the ';'
  skipws();                        // necessary for instr_parse()

  // Debug Stuff
  if (_AD._adl_debug > 1) {
    if (primary   != NULL) fprintf(stderr,"primary   opcode: %s\n", primary);
    if (secondary != NULL) fprintf(stderr,"secondary opcode: %s\n", secondary);
    if (tertiary  != NULL) fprintf(stderr,"tertiary  opcode: %s\n", tertiary);
  }

  // Generate new object and return
  opcode = new Opcode(primary, secondary, tertiary);
  return opcode;
}


//------------------------------interface_parse--------------------------------
Interface *ADLParser::interface_parse(void) {
  char *iface_name  = NULL;      // Name of interface class being used
  char *iface_code  = NULL;      // Describe components of this class

  // Get interface class name
  skipws();                       // Skip whitespace
  if (_curchar != '(') {
    parse_err(SYNERR, "Missing '(' at start of interface description.\n");
    return NULL;
  }
  next_char();                    // move past '('
  skipws();
  iface_name = get_ident();
  if (iface_name == NULL) {
    parse_err(SYNERR, "missing interface name after 'interface'.\n");
    return NULL;
  }
  skipws();
  if (_curchar != ')') {
    parse_err(SYNERR, "Missing ')' after name of interface.\n");
    return NULL;
  }
  next_char();                    // move past ')'

  // Get details of the interface,
  // for the type of interface indicated by iface_name.
  Interface *inter = NULL;
  skipws();
  if ( _curchar != ';' ) {
    if ( strcmp(iface_name,"MEMORY_INTER") == 0 ) {
      inter = mem_interface_parse();
    }
    else if ( strcmp(iface_name,"COND_INTER") == 0 ) {
      inter = cond_interface_parse();
    }
    // The parse routines consume the "%}"

    // Check for probable extra ';' after defining block.
    if ( _curchar == ';' ) {
      parse_err(SYNERR, "Extra ';' after defining interface block.\n");
      next_char();                // Skip ';'
      return NULL;
    }
  } else {
    next_char();                  // move past ';'

    // Create appropriate interface object
    if ( strcmp(iface_name,"REG_INTER") == 0 ) {
      inter = new RegInterface();
    }
    else if ( strcmp(iface_name,"CONST_INTER") == 0 ) {
      inter = new ConstInterface();
    }
  }
  skipws();                       // be friendly to oper_parse()
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Interface Form: %s\n", iface_name);

  // Create appropriate interface object and return.
  return inter;
}


//------------------------------mem_interface_parse----------------------------
Interface *ADLParser::mem_interface_parse(void) {
  // Fields for MemInterface
  char *base        = NULL;
  char *index       = NULL;
  char *scale       = NULL;
  char *disp        = NULL;

  if (_curchar != '%') {
    parse_err(SYNERR, "Missing '%%{' for 'interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '%'
  if (_curchar != '{') {
    parse_err(SYNERR, "Missing '%%{' for 'interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '{'
  skipws();
  do {
    char *field = get_ident();
    if (field == NULL) {
      parse_err(SYNERR, "Expected keyword, base|index|scale|disp,  or '%%}' ending interface.\n");
      return NULL;
    }
    if ( strcmp(field,"base") == 0 ) {
      base  = interface_field_parse();
    }
    else if ( strcmp(field,"index") == 0 ) {
      index = interface_field_parse();
    }
    else if ( strcmp(field,"scale") == 0 ) {
      scale = interface_field_parse();
    }
    else if ( strcmp(field,"disp") == 0 ) {
      disp  = interface_field_parse();
    }
    else {
      parse_err(SYNERR, "Expected keyword, base|index|scale|disp,  or '%%}' ending interface.\n");
      return NULL;
    }
  } while( _curchar != '%' );
  next_char();                  // Skip '%'
  if ( _curchar != '}' ) {
    parse_err(SYNERR, "Missing '%%}' for 'interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '}'

  // Construct desired object and return
  Interface *inter = new MemInterface(base, index, scale, disp);
  return inter;
}


//------------------------------cond_interface_parse---------------------------
Interface *ADLParser::cond_interface_parse(void) {
  char *equal;
  char *not_equal;
  char *less;
  char *greater_equal;
  char *less_equal;
  char *greater;
  char *overflow;
  char *no_overflow;
  const char *equal_format = "eq";
  const char *not_equal_format = "ne";
  const char *less_format = "lt";
  const char *greater_equal_format = "ge";
  const char *less_equal_format = "le";
  const char *greater_format = "gt";
  const char *overflow_format = "o";
  const char *no_overflow_format = "no";

  if (_curchar != '%') {
    parse_err(SYNERR, "Missing '%%{' for 'cond_interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '%'
  if (_curchar != '{') {
    parse_err(SYNERR, "Missing '%%{' for 'cond_interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '{'
  skipws();
  do {
    char *field = get_ident();
    if (field == NULL) {
      parse_err(SYNERR, "Expected keyword, base|index|scale|disp,  or '%%}' ending interface.\n");
      return NULL;
    }
    if ( strcmp(field,"equal") == 0 ) {
      equal  = interface_field_parse(&equal_format);
    }
    else if ( strcmp(field,"not_equal") == 0 ) {
      not_equal = interface_field_parse(&not_equal_format);
    }
    else if ( strcmp(field,"less") == 0 ) {
      less = interface_field_parse(&less_format);
    }
    else if ( strcmp(field,"greater_equal") == 0 ) {
      greater_equal  = interface_field_parse(&greater_equal_format);
    }
    else if ( strcmp(field,"less_equal") == 0 ) {
      less_equal = interface_field_parse(&less_equal_format);
    }
    else if ( strcmp(field,"greater") == 0 ) {
      greater = interface_field_parse(&greater_format);
    }
    else if ( strcmp(field,"overflow") == 0 ) {
      overflow = interface_field_parse(&overflow_format);
    }
    else if ( strcmp(field,"no_overflow") == 0 ) {
      no_overflow = interface_field_parse(&no_overflow_format);
    }
    else {
      parse_err(SYNERR, "Expected keyword, base|index|scale|disp,  or '%%}' ending interface.\n");
      return NULL;
    }
  } while( _curchar != '%' );
  next_char();                  // Skip '%'
  if ( _curchar != '}' ) {
    parse_err(SYNERR, "Missing '%%}' for 'interface' block.\n");
    return NULL;
  }
  next_char();                  // Skip '}'

  // Construct desired object and return
  Interface *inter = new CondInterface(equal,         equal_format,
                                       not_equal,     not_equal_format,
                                       less,          less_format,
                                       greater_equal, greater_equal_format,
                                       less_equal,    less_equal_format,
                                       greater,       greater_format,
                                       overflow,      overflow_format,
                                       no_overflow,   no_overflow_format);
  return inter;
}


//------------------------------interface_field_parse--------------------------
char *ADLParser::interface_field_parse(const char ** format) {
  char *iface_field = NULL;

  // Get interface field
  skipws();                      // Skip whitespace
  if (_curchar != '(') {
    parse_err(SYNERR, "Missing '(' at start of interface field.\n");
    return NULL;
  }
  next_char();                   // move past '('
  skipws();
  if ( _curchar != '0' && _curchar != '$' ) {
    parse_err(SYNERR, "missing or invalid interface field contents.\n");
    return NULL;
  }
  iface_field = get_rep_var_ident();
  if (iface_field == NULL) {
    parse_err(SYNERR, "missing or invalid interface field contents.\n");
    return NULL;
  }
  skipws();
  if (format != NULL && _curchar == ',') {
    next_char();
    skipws();
    if (_curchar != '"') {
      parse_err(SYNERR, "Missing '\"' in field format .\n");
      return NULL;
    }
    next_char();
    char *start = _ptr;       // Record start of the next string
    while ((_curchar != '"') && (_curchar != '%') && (_curchar != '\n')) {
      if (_curchar == '\\')  next_char();  // superquote
      if (_curchar == '\n')  parse_err(SYNERR, "newline in string");  // unimplemented!
      next_char();
    }
    if (_curchar != '"') {
      parse_err(SYNERR, "Missing '\"' at end of field format .\n");
      return NULL;
    }
    // If a string was found, terminate it and record in FormatRule
    if ( start != _ptr ) {
      *_ptr  = '\0';          // Terminate the string
      *format = start;
    }
    next_char();
    skipws();
  }
  if (_curchar != ')') {
    parse_err(SYNERR, "Missing ')' after interface field.\n");
    return NULL;
  }
  next_char();                   // move past ')'
  skipws();
  if ( _curchar != ';' ) {
    parse_err(SYNERR, "Missing ';' at end of interface field.\n");
    return NULL;
  }
  next_char();                    // move past ';'
  skipws();                       // be friendly to interface_parse()

  return iface_field;
}


//------------------------------match_parse------------------------------------
MatchRule *ADLParser::match_parse(FormDict &operands) {
  MatchRule *match;               // Match Rule class for instruction/operand
  char      *cnstr = NULL;        // Code for constructor
  int        depth = 0;           // Counter for matching parentheses
  int        numleaves = 0;       // Counter for number of leaves in rule

  // Parse the match rule tree
  MatchNode *mnode = matchNode_parse(operands, depth, numleaves, true);

  // Either there is a block with a constructor, or a ';' here
  skipws();                       // Skip whitespace
  if ( _curchar == ';' ) {        // Semicolon is valid terminator
    cnstr = NULL;                 // no constructor for this form
    next_char();                  // Move past the ';', replaced with '\0'
  }
  else if ((cnstr = find_cpp_block("match constructor")) == NULL ) {
    parse_err(SYNERR, "invalid construction of match rule\n"
              "Missing ';' or invalid '%%{' and '%%}' constructor\n");
    return NULL;                  // No MatchRule to return
  }
  if (_AD._adl_debug > 1)
    if (cnstr) fprintf(stderr,"Match Constructor: %s\n", cnstr);
  // Build new MatchRule object
  match = new MatchRule(_AD, mnode, depth, cnstr, numleaves);
  skipws();                       // Skip any trailing whitespace
  return match;                   // Return MatchRule object
}

//------------------------------format_parse-----------------------------------
FormatRule* ADLParser::format_parse(void) {
  char       *desc   = NULL;
  FormatRule *format = (new FormatRule(desc));

  // Without expression form, MUST have a code block;
  skipws();                       // Skip whitespace
  if ( _curchar == ';' ) {        // Semicolon is valid terminator
    desc  = NULL;                 // no constructor for this form
    next_char();                  // Move past the ';', replaced with '\0'
  }
  else if ( _curchar == '%' && *(_ptr+1) == '{') {
    next_char();                  // Move past the '%'
    next_char();                  // Move past the '{'

    skipws();
    if (_curchar == '$') {
      char* ident = get_rep_var_ident();
      if (strcmp(ident, "$$template") == 0) return template_parse();
      parse_err(SYNERR, "Unknown \"%s\" directive in format", ident);
      return NULL;
    }
    // Check for the opening '"' inside the format description
    if ( _curchar == '"' ) {
      next_char();              // Move past the initial '"'
      if( _curchar == '"' ) {   // Handle empty format string case
        *_ptr = '\0';           // Terminate empty string
        format->_strings.addName(_ptr);
      }

      // Collect the parts of the format description
      // (1) strings that are passed through to tty->print
      // (2) replacement/substitution variable, preceeded by a '$'
      // (3) multi-token ANSIY C style strings
      while ( true ) {
        if ( _curchar == '%' || _curchar == '\n' ) {
          if ( _curchar != '"' ) {
            parse_err(SYNERR, "missing '\"' at end of format block");
            return NULL;
          }
        }

        // (1)
        // Check if there is a string to pass through to output
        char *start = _ptr;       // Record start of the next string
        while ((_curchar != '$') && (_curchar != '"') && (_curchar != '%') && (_curchar != '\n')) {
          if (_curchar == '\\') {
            next_char();  // superquote
            if ((_curchar == '$') || (_curchar == '%'))
              // hack to avoid % escapes and warnings about undefined \ escapes
              *(_ptr-1) = _curchar;
          }
          if (_curchar == '\n')  parse_err(SYNERR, "newline in string");  // unimplemented!
          next_char();
        }
        // If a string was found, terminate it and record in FormatRule
        if ( start != _ptr ) {
          *_ptr  = '\0';          // Terminate the string
          format->_strings.addName(start);
        }

        // (2)
        // If we are at a replacement variable,
        // copy it and record in FormatRule
        if ( _curchar == '$' ) {
          next_char();          // Move past the '$'
          char* rep_var = get_ident(); // Nil terminate the variable name
          rep_var = strdup(rep_var);// Copy the string
          *_ptr   = _curchar;     // and replace Nil with original character
          format->_rep_vars.addName(rep_var);
          // Add flag to _strings list indicating we should check _rep_vars
          format->_strings.addName(NameList::_signal);
        }

        // (3)
        // Allow very long strings to be broken up,
        // using the ANSI C syntax "foo\n" <newline> "bar"
        if ( _curchar == '"') {
          next_char();           // Move past the '"'
          skipws();              // Skip white space before next string token
          if ( _curchar != '"') {
            break;
          } else {
            // Found one.  Skip both " and the whitespace in between.
            next_char();
          }
        }
      } // end while part of format description

      // Check for closing '"' and '%}' in format description
      skipws();                   // Move to closing '%}'
      if ( _curchar != '%' ) {
        parse_err(SYNERR, "non-blank characters between closing '\"' and '%%' in format");
        return NULL;
      }
    } // Done with format description inside

    skipws();
    // Past format description, at '%'
    if ( _curchar != '%' || *(_ptr+1) != '}' ) {
      parse_err(SYNERR, "missing '%%}' at end of format block");
      return NULL;
    }
    next_char();                  // Move past the '%'
    next_char();                  // Move past the '}'
  }
  else {  // parameter list alone must terminate with a ';'
    parse_err(SYNERR, "missing ';' after Format expression");
    return NULL;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Format Rule: %s\n", desc);

  skipws();
  return format;
}


//------------------------------template_parse-----------------------------------
FormatRule* ADLParser::template_parse(void) {
  char       *desc   = NULL;
  FormatRule *format = (new FormatRule(desc));

  skipws();
  while ( (_curchar != '%') && (*(_ptr+1) != '}') ) {

    // (1)
    // Check if there is a string to pass through to output
    {
      char *start = _ptr;       // Record start of the next string
      while ((_curchar != '$') && ((_curchar != '%') || (*(_ptr+1) != '}')) ) {
        // If at the start of a comment, skip past it
        if( (_curchar == '/') && ((*(_ptr+1) == '/') || (*(_ptr+1) == '*')) ) {
          skipws_no_preproc();
        } else {
          // ELSE advance to the next character, or start of the next line
          next_char_or_line();
        }
      }
      // If a string was found, terminate it and record in EncClass
      if ( start != _ptr ) {
        *_ptr  = '\0';          // Terminate the string
        // Add flag to _strings list indicating we should check _rep_vars
        format->_strings.addName(NameList::_signal2);
        format->_strings.addName(start);
      }
    }

    // (2)
    // If we are at a replacement variable,
    // copy it and record in EncClass
    if ( _curchar == '$' ) {
      // Found replacement Variable
      char *rep_var = get_rep_var_ident_dup();
      if (strcmp(rep_var, "$emit") == 0) {
        // switch to normal format parsing
        next_char();
        next_char();
        skipws();
        // Check for the opening '"' inside the format description
        if ( _curchar == '"' ) {
          next_char();              // Move past the initial '"'
          if( _curchar == '"' ) {   // Handle empty format string case
            *_ptr = '\0';           // Terminate empty string
            format->_strings.addName(_ptr);
          }

          // Collect the parts of the format description
          // (1) strings that are passed through to tty->print
          // (2) replacement/substitution variable, preceeded by a '$'
          // (3) multi-token ANSIY C style strings
          while ( true ) {
            if ( _curchar == '%' || _curchar == '\n' ) {
              parse_err(SYNERR, "missing '\"' at end of format block");
              return NULL;
            }

            // (1)
            // Check if there is a string to pass through to output
            char *start = _ptr;       // Record start of the next string
            while ((_curchar != '$') && (_curchar != '"') && (_curchar != '%') && (_curchar != '\n')) {
              if (_curchar == '\\')  next_char();  // superquote
              if (_curchar == '\n')  parse_err(SYNERR, "newline in string");  // unimplemented!
              next_char();
            }
            // If a string was found, terminate it and record in FormatRule
            if ( start != _ptr ) {
              *_ptr  = '\0';          // Terminate the string
              format->_strings.addName(start);
            }

            // (2)
            // If we are at a replacement variable,
            // copy it and record in FormatRule
            if ( _curchar == '$' ) {
              next_char();          // Move past the '$'
              char* next_rep_var = get_ident(); // Nil terminate the variable name
              next_rep_var = strdup(next_rep_var);// Copy the string
              *_ptr   = _curchar;     // and replace Nil with original character
              format->_rep_vars.addName(next_rep_var);
              // Add flag to _strings list indicating we should check _rep_vars
              format->_strings.addName(NameList::_signal);
            }

            // (3)
            // Allow very long strings to be broken up,
            // using the ANSI C syntax "foo\n" <newline> "bar"
            if ( _curchar == '"') {
              next_char();           // Move past the '"'
              skipws();              // Skip white space before next string token
              if ( _curchar != '"') {
                break;
              } else {
                // Found one.  Skip both " and the whitespace in between.
                next_char();
              }
            }
          } // end while part of format description
        }
      } else {
        // Add flag to _strings list indicating we should check _rep_vars
        format->_rep_vars.addName(rep_var);
        // Add flag to _strings list indicating we should check _rep_vars
        format->_strings.addName(NameList::_signal3);
      }
    } // end while part of format description
  }

  skipws();
  // Past format description, at '%'
  if ( _curchar != '%' || *(_ptr+1) != '}' ) {
    parse_err(SYNERR, "missing '%%}' at end of format block");
    return NULL;
  }
  next_char();                  // Move past the '%'
  next_char();                  // Move past the '}'

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Format Rule: %s\n", desc);

  skipws();
  return format;
}


//------------------------------effect_parse-----------------------------------
void ADLParser::effect_parse(InstructForm *instr) {
  char* desc   = NULL;

  skipws();                      // Skip whitespace
  if (_curchar != '(') {
    parse_err(SYNERR, "missing '(' in effect definition\n");
    return;
  }
  // Get list of effect-operand pairs and insert into dictionary
  else get_effectlist(instr->_effects, instr->_localNames, instr->_has_call);

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Effect description: %s\n", desc);
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in Effect definition\n");
  }
  next_char();                  // Skip ';'

}

//------------------------------expand_parse-----------------------------------
ExpandRule* ADLParser::expand_parse(InstructForm *instr) {
  char         *ident, *ident2;
  NameAndList  *instr_and_operands = NULL;
  ExpandRule   *exp = new ExpandRule();

  // Expand is a block containing an ordered list of operands with initializers,
  // or instructions, each of which has an ordered list of operands.
  // Check for block delimiter
  skipws();                        // Skip leading whitespace
  if ((_curchar != '%')
      || (next_char(), (_curchar != '{')) ) { // If not open block
    parse_err(SYNERR, "missing '%%{' in expand definition\n");
    return(NULL);
  }
  next_char();                     // Maintain the invariant
  do {
    ident = get_ident();           // Grab next identifier
    if (ident == NULL) {
      parse_err(SYNERR, "identifier expected at %c\n", _curchar);
      continue;
    }

    // Check whether we should parse an instruction or operand.
    const Form *form = _globalNames[ident];
    bool parse_oper = false;
    bool parse_ins  = false;
    if (form == NULL) {
      skipws();
      // Check whether this looks like an instruction specification.  If so,
      // just parse the instruction.  The declaration of the instruction is
      // not needed here.
      if (_curchar == '(') parse_ins = true;
    } else if (form->is_instruction()) {
      parse_ins = true;
    } else if (form->is_operand()) {
      parse_oper = true;
    } else {
      parse_err(SYNERR, "instruction/operand name expected at %s\n", ident);
      continue;
    }

    if (parse_oper) {
      // This is a new operand
      OperandForm *oper = form->is_operand();
      if (oper == NULL) {
        parse_err(SYNERR, "instruction/operand name expected at %s\n", ident);
        continue;
      }
      // Throw the operand on the _newopers list
      skipws();
      ident = get_unique_ident(instr->_localNames,"Operand");
      if (ident == NULL) {
        parse_err(SYNERR, "identifier expected at %c\n", _curchar);
        continue;
      }
      exp->_newopers.addName(ident);
      // Add new operand to LocalNames
      instr->_localNames.Insert(ident, oper);
      // Grab any constructor code and save as a string
      char *c = NULL;
      skipws();
      if (_curchar == '%') { // Need a constructor for the operand
        c = find_cpp_block("Operand Constructor");
        if (c == NULL) {
          parse_err(SYNERR, "Invalid code block for operand constructor\n", _curchar);
          continue;
        }
        // Add constructor to _newopconst Dict
        exp->_newopconst.Insert(ident, c);
      }
      else if (_curchar != ';') { // If no constructor, need a ;
        parse_err(SYNERR, "Missing ; in expand rule operand declaration\n");
        continue;
      }
      else next_char(); // Skip the ;
      skipws();
    }
    else {
      assert(parse_ins, "sanity");
      // Add instruction to list
      instr_and_operands = new NameAndList(ident);
      // Grab operands, build nameList of them, and then put into dictionary
      skipws();
      if (_curchar != '(') {         // Check for parenthesized operand list
        parse_err(SYNERR, "missing '(' in expand instruction declaration\n");
        continue;
      }
      do {
        next_char();                 // skip open paren & comma characters
        skipws();
        if (_curchar == ')') break;
        ident2 = get_ident();
        skipws();
        if (ident2 == NULL) {
          parse_err(SYNERR, "identifier expected at %c\n", _curchar);
          continue;
        }                            // Check that you have a valid operand
        const Form *form2 = instr->_localNames[ident2];
        if (!form2) {
          parse_err(SYNERR, "operand name expected at %s\n", ident2);
          continue;
        }
        OperandForm *oper = form2->is_operand();
        if (oper == NULL && !form2->is_opclass()) {
          parse_err(SYNERR, "operand name expected at %s\n", ident2);
          continue;
        }                            // Add operand to list
        instr_and_operands->add_entry(ident2);
      } while(_curchar == ',');
      if (_curchar != ')') {
        parse_err(SYNERR, "missing ')'in expand instruction declaration\n");
        continue;
      }
      next_char();
      if (_curchar != ';') {
        parse_err(SYNERR, "missing ';'in expand instruction declaration\n");
        continue;
      }
      next_char();

      // Record both instruction name and its operand list
      exp->add_instruction(instr_and_operands);

      skipws();
    }

  } while(_curchar != '%');
  next_char();
  if (_curchar != '}') {
    parse_err(SYNERR, "missing '%%}' in expand rule definition\n");
    return(NULL);
  }
  next_char();

  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Expand Rule:\n");

  skipws();
  return (exp);
}

//------------------------------rewrite_parse----------------------------------
RewriteRule* ADLParser::rewrite_parse(void) {
  char* params = NULL;
  char* desc   = NULL;


  // This feature targeted for second generation description language.

  skipws();                      // Skip whitespace
  // Get parameters for rewrite
  if ((params = get_paren_expr("rewrite parameters")) == NULL) {
    parse_err(SYNERR, "missing '(' in rewrite rule\n");
    return NULL;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Rewrite parameters: %s\n", params);

  // For now, grab entire block;
  skipws();
  if ( (desc = find_cpp_block("rewrite block")) == NULL ) {
    parse_err(SYNERR, "incorrect or missing block for 'rewrite'.\n");
    return NULL;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Rewrite Rule: %s\n", desc);

  skipws();
  return (new RewriteRule(params,desc));
}

//------------------------------attr_parse-------------------------------------
Attribute *ADLParser::attr_parse(char* ident) {
  Attribute *attrib;              // Attribute class
  char      *cost = NULL;         // String representation of cost attribute

  skipws();                       // Skip leading whitespace
  if ( (cost = get_paren_expr("attribute")) == NULL ) {
    parse_err(SYNERR, "incorrect or missing expression for 'attribute'\n");
    return NULL;
  }
  // Debug Stuff
  if (_AD._adl_debug > 1) fprintf(stderr,"Attribute: %s\n", cost);
  if (_curchar != ';') {
    parse_err(SYNERR, "missing ';' in attribute definition\n");
    return NULL;
  }
  next_char();                   // Point after the terminator

  skipws();
  attrib = new Attribute(ident,cost,INS_ATTR); // Build new predicate object
  return attrib;
}


//------------------------------matchNode_parse--------------------------------
MatchNode *ADLParser::matchNode_parse(FormDict &operands, int &depth, int &numleaves, bool atroot) {
  // Count depth of parenthesis nesting for both left and right children
  int   lParens = depth;
  int   rParens = depth;

  // MatchNode objects for left, right, and root of subtree.
  MatchNode *lChild = NULL;
  MatchNode *rChild = NULL;
  char      *token;               // Identifier which may be opcode or operand

  // Match expression starts with a '('
  if (cur_char() != '(')
    return NULL;

  next_char();                    // advance past '('

  // Parse the opcode
  token = get_ident();            // Get identifier, opcode
  if (token == NULL) {
    parse_err(SYNERR, "missing opcode in match expression\n");
    return NULL;
  }

  // Take note if we see one of a few special operations - those that are
  // treated differently on different architectures in the sense that on
  // one architecture there is a match rule and on another there isn't (so
  // a call will eventually be generated).

  for (int i = _last_machine_leaf + 1; i < _last_opcode; i++) {
    if (strcmp(token, NodeClassNames[i]) == 0) {
      _AD.has_match_rule(i, true);
    }
  }

  // Lookup the root value in the operands dict to perform substitution
  const char  *result    = NULL;  // Result type will be filled in later
  const char  *name      = token; // local name associated with this node
  const char  *operation = token; // remember valid operation for later
  const Form  *form      = operands[token];
  OpClassForm *opcForm = form ? form->is_opclass() : NULL;
  if (opcForm != NULL) {
    // If this token is an entry in the local names table, record its type
    if (!opcForm->ideal_only()) {
      operation = opcForm->_ident;
      result = operation;         // Operands result in their own type
    }
    // Otherwise it is an ideal type, and so, has no local name
    else                        name = NULL;
  }

  // Parse the operands
  skipws();
  if (cur_char() != ')') {

    // Parse the left child
    if (strcmp(operation,"Set"))
      lChild = matchChild_parse(operands, lParens, numleaves, false);
    else
      lChild = matchChild_parse(operands, lParens, numleaves, true);

    skipws();
    if (cur_char() != ')' ) {
      if(strcmp(operation, "Set"))
        rChild = matchChild_parse(operands,rParens,numleaves,false);
      else
        rChild = matchChild_parse(operands,rParens,numleaves,true);
    }
  }

  // Check for required ')'
  skipws();
  if (cur_char() != ')') {
    parse_err(SYNERR, "missing ')' in match expression\n");
    return NULL;
  }
  next_char();                    // skip the ')'

  MatchNode* mroot = new MatchNode(_AD,result,name,operation,lChild,rChild);

  // If not the root, reduce this subtree to an internal operand
  if (!atroot) {
    mroot->build_internalop();
  }
  // depth is greater of left and right paths.
  depth = (lParens > rParens) ? lParens : rParens;

  return mroot;
}


//------------------------------matchChild_parse-------------------------------
MatchNode *ADLParser::matchChild_parse(FormDict &operands, int &parens, int &numleaves, bool atroot) {
  MatchNode  *child  = NULL;
  const char *result = NULL;
  const char *token  = NULL;
  const char *opType = NULL;

  if (cur_char() == '(') {         // child is an operation
    ++parens;
    child = matchNode_parse(operands, parens, numleaves, atroot);
  }
  else {                           // child is an operand
    token = get_ident();
    const Form  *form    = operands[token];
    OpClassForm *opcForm = form ? form->is_opclass() : NULL;
    if (opcForm != NULL) {
      opType = opcForm->_ident;
      result = opcForm->_ident;    // an operand's result matches its type
    } else {
      parse_err(SYNERR, "undefined operand %s in match rule\n", token);
      return NULL;
    }

    if (opType == NULL) {
      parse_err(SYNERR, "missing type for argument '%s'\n", token);
    }

    child = new MatchNode(_AD, result, token, opType);
    ++numleaves;
  }

  return child;
}



// ******************** Private Utility Functions *************************


char* ADLParser::find_cpp_block(const char* description) {
  char *next;                     // Pointer for finding block delimiters
  char* cppBlock = NULL;          // Beginning of C++ code block

  if (_curchar == '%') {          // Encoding is a C++ expression
    next_char();
    if (_curchar != '{') {
      parse_err(SYNERR, "missing '{' in %s \n", description);
      return NULL;
    }
    next_char();                  // Skip block delimiter
    skipws_no_preproc();          // Skip leading whitespace
    cppBlock = _ptr;              // Point to start of expression
    int line = linenum();
    next = _ptr + 1;
    while(((_curchar != '%') || (*next != '}')) && (_curchar != '\0')) {
      next_char_or_line();
      next = _ptr+1;              // Maintain the next pointer
    }                             // Grab string
    if (_curchar == '\0') {
      parse_err(SYNERR, "invalid termination of %s \n", description);
      return NULL;
    }
    *_ptr = '\0';                 // Terminate string
    _ptr += 2;                    // Skip block delimiter
    _curchar = *_ptr;             // Maintain invariant

    // Prepend location descriptor, for debugging.
    if (_AD._adlocation_debug) {
      char* location = get_line_string(line);
      char* end_loc  = end_line_marker();
      char* result = (char *)AllocateHeap(strlen(location) + strlen(cppBlock) + strlen(end_loc) + 1);
      strcpy(result, location);
      strcat(result, cppBlock);
      strcat(result, end_loc);
      cppBlock = result;
      free(location);
    }
  }

  return cppBlock;
}

// Move to the closing token of the expression we are currently at,
// as defined by stop_chars.  Match parens and quotes.
char* ADLParser::get_expr(const char *desc, const char *stop_chars) {
  char* expr = NULL;
  int   paren = 0;

  expr = _ptr;
  while (paren > 0 || !strchr(stop_chars, _curchar)) {
    if (_curchar == '(') {        // Down level of nesting
      paren++;                    // Bump the parenthesis counter
      next_char();                // maintain the invariant
    }
    else if (_curchar == ')') {   // Up one level of nesting
      if (paren == 0) {
        // Paren underflow:  We didn't encounter the required stop-char.
        parse_err(SYNERR, "too many )'s, did not find %s after %s\n",
                  stop_chars, desc);
        return NULL;
      }
      paren--;                    // Drop the parenthesis counter
      next_char();                // Maintain the invariant
    }
    else if (_curchar == '"' || _curchar == '\'') {
      int qchar = _curchar;
      while (true) {
        next_char();
        if (_curchar == qchar) { next_char(); break; }
        if (_curchar == '\\')  next_char();  // superquote
        if (_curchar == '\n' || _curchar == '\0') {
          parse_err(SYNERR, "newline in string in %s\n", desc);
          return NULL;
        }
      }
    }
    else if (_curchar == '%' && (_ptr[1] == '{' || _ptr[1] == '}')) {
      // Make sure we do not stray into the next ADLC-level form.
      parse_err(SYNERR, "unexpected %%%c in %s\n", _ptr[1], desc);
      return NULL;
    }
    else if (_curchar == '\0') {
      parse_err(SYNERR, "unexpected EOF in %s\n", desc);
      return NULL;
    }
    else {
      // Always walk over whitespace, comments, preprocessor directives, etc.
      char* pre_skip_ptr = _ptr;
      skipws();
      // If the parser declined to make progress on whitespace,
      // skip the next character, which is therefore NOT whitespace.
      if (pre_skip_ptr == _ptr) {
        next_char();
      } else if (pre_skip_ptr+strlen(pre_skip_ptr) != _ptr+strlen(_ptr)) {
        parse_err(SYNERR, "unimplemented: preprocessor must not elide subexpression in %s", desc);
      }
    }
  }

  assert(strchr(stop_chars, _curchar), "non-null return must be at stop-char");
  *_ptr = '\0';               // Replace ')' or other stop-char with '\0'
  return expr;
}

// Helper function around get_expr
// Sets _curchar to '(' so that get_paren_expr will search for a matching ')'
char *ADLParser::get_paren_expr(const char *description, bool include_location) {
  int line = linenum();
  if (_curchar != '(')            // Escape if not valid starting position
    return NULL;
  next_char();                    // Skip the required initial paren.
  char *token2 = get_expr(description, ")");
  if (_curchar == ')')
    next_char();                  // Skip required final paren.
  int junk = 0;
  if (include_location && _AD._adlocation_debug && !is_int_token(token2, junk)) {
    // Prepend location descriptor, for debugging.
    char* location = get_line_string(line);
    char* end_loc  = end_line_marker();
    char* result = (char *)AllocateHeap(strlen(location) + strlen(token2) + strlen(end_loc) + 1);
    strcpy(result, location);
    strcat(result, token2);
    strcat(result, end_loc);
    token2 = result;
    free(location);
  }
  return token2;
}

//------------------------------get_ident_common-------------------------------
// Looks for an identifier in the buffer, and turns it into a null terminated
// string(still inside the file buffer).  Returns a pointer to the string or
// NULL if some other token is found instead.
char *ADLParser::get_ident_common(bool do_preproc) {
  char c;
  char *start;                    // Pointer to start of token
  char *end;                      // Pointer to end of token

  if( _curline == NULL )          // Return NULL at EOF.
    return NULL;

  skipws_common(do_preproc);      // Skip whitespace before identifier
  start = end = _ptr;             // Start points at first character
  end--;                          // unwind end by one to prepare for loop
  do {
    end++;                        // Increment end pointer
    c = *end;                     // Grab character to test
  } while ( ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
            || ((c >= '0') && (c <= '9'))
            || ((c == '_')) || ((c == ':')) || ((c == '#')) );
  if (start == end) {             // We popped out on the first try
    // It can occur that `start' contains the rest of the input file.
    // In this case the output should be truncated.
    if (strlen(start) > 24) {
      char buf[32];
      strncpy(buf, start, 20);
      buf[20] = '\0';
      strcat(buf, "[...]");
      parse_err(SYNERR, "Identifier expected, but found '%s'.", buf);
    } else {
      parse_err(SYNERR, "Identifier expected, but found '%s'.", start);
    }
    start = NULL;
  }
  else {
    _curchar = c;                 // Save the first character of next token
    *end = '\0';                  // NULL terminate the string in place
  }
  _ptr = end;                     // Reset _ptr to point to next char after token

  // Make sure we do not try to use #defined identifiers.  If start is
  // NULL an error was already reported.
  if (do_preproc && start != NULL) {
    const char* def = _AD.get_preproc_def(start);
    if (def != NULL && strcmp(def, start)) {
      const char* def1 = def;
      const char* def2 = _AD.get_preproc_def(def1);
      // implement up to 2 levels of #define
      if (def2 != NULL && strcmp(def2, def1)) {
        def = def2;
        const char* def3 = _AD.get_preproc_def(def2);
        if (def3 != NULL && strcmp(def3, def2) && strcmp(def3, def1)) {
          parse_err(SYNERR, "unimplemented: using %s defined as %s => %s => %s",
                    start, def1, def2, def3);
        }
      }
      start = strdup(def);
    }
  }

  return start;                   // Pointer to token in filebuf
}

//------------------------------get_ident_dup----------------------------------
// Looks for an identifier in the buffer, and returns a duplicate
// or NULL if some other token is found instead.
char *ADLParser::get_ident_dup(void) {
  char *ident = get_ident();

  // Duplicate an identifier before returning and restore string.
  if( ident != NULL ) {
    ident = strdup(ident);  // Copy the string
    *_ptr   = _curchar;         // and replace Nil with original character
  }

  return ident;
}

//----------------------get_ident_or_literal_constant--------------------------
// Looks for an identifier in the buffer, or a parenthesized expression.
char *ADLParser::get_ident_or_literal_constant(const char* description) {
  char* param = NULL;
  skipws();
  if (_curchar == '(') {
    // Grab a constant expression.
    param = get_paren_expr(description);
    if (param[0] != '(') {
      char* buf = (char*) AllocateHeap(strlen(param) + 3);
      sprintf(buf, "(%s)", param);
      param = buf;
    }
    assert(is_literal_constant(param),
           "expr must be recognizable as a constant");
  } else {
    param = get_ident();
  }
  return param;
}

//------------------------------get_rep_var_ident-----------------------------
// Do NOT duplicate,
// Leave nil terminator in buffer
// Preserve initial '$'(s) in string
char *ADLParser::get_rep_var_ident(void) {
  // Remember starting point
  char *rep_var = _ptr;

  // Check for replacement variable indicator '$' and pass if present
  if ( _curchar == '$' ) {
    next_char();
  }
  // Check for a subfield indicator, a second '$', and pass if present
  if ( _curchar == '$' ) {
    next_char();
  }

  // Check for a control indicator, a third '$':
  if ( _curchar == '$' ) {
    next_char();
  }

  // Check for more than three '$'s in sequence, SYNERR
  if( _curchar == '$' ) {
    parse_err(SYNERR, "Replacement variables and field specifiers can not start with '$$$$'");
    next_char();
    return NULL;
  }

  // Nil terminate the variable name following the '$'
  char *rep_var_name = get_ident();
  assert( rep_var_name != NULL,
          "Missing identifier after replacement variable indicator '$'");

  return rep_var;
}



//------------------------------get_rep_var_ident_dup-------------------------
// Return the next replacement variable identifier, skipping first '$'
// given a pointer into a line of the buffer.
// Null terminates string, still inside the file buffer,
// Returns a pointer to a copy of the string, or NULL on failure
char *ADLParser::get_rep_var_ident_dup(void) {
  if( _curchar != '$' ) return NULL;

  next_char();                // Move past the '$'
  char *rep_var = _ptr;       // Remember starting point

  // Check for a subfield indicator, a second '$':
  if ( _curchar == '$' ) {
    next_char();
  }

  // Check for a control indicator, a third '$':
  if ( _curchar == '$' ) {
    next_char();
  }

  // Check for more than three '$'s in sequence, SYNERR
  if( _curchar == '$' ) {
    parse_err(SYNERR, "Replacement variables and field specifiers can not start with '$$$$'");
    next_char();
    return NULL;
  }

  // Nil terminate the variable name following the '$'
  char *rep_var_name = get_ident();
  assert( rep_var_name != NULL,
          "Missing identifier after replacement variable indicator '$'");
  rep_var = strdup(rep_var);  // Copy the string
  *_ptr   = _curchar;         // and replace Nil with original character

  return rep_var;
}


//------------------------------get_unique_ident------------------------------
// Looks for an identifier in the buffer, terminates it with a NULL,
// and checks that it is unique
char *ADLParser::get_unique_ident(FormDict& dict, const char* nameDescription){
  char* ident = get_ident();

  if (ident == NULL) {
    parse_err(SYNERR, "missing %s identifier at %c\n", nameDescription, _curchar);
  }
  else {
    if (dict[ident] != NULL) {
      parse_err(SYNERR, "duplicate name %s for %s\n", ident, nameDescription);
      ident = NULL;
    }
  }

  return ident;
}


//------------------------------get_int----------------------------------------
// Looks for a character string integer in the buffer, and turns it into an int
// invokes a parse_err if the next token is not an integer.
// This routine does not leave the integer null-terminated.
int ADLParser::get_int(void) {
  char          c;
  char         *start;            // Pointer to start of token
  char         *end;              // Pointer to end of token
  int           result;           // Storage for integer result

  if( _curline == NULL )          // Return NULL at EOF.
    return 0;

  skipws();                       // Skip whitespace before identifier
  start = end = _ptr;             // Start points at first character
  c = *end;                       // Grab character to test
  while ((c >= '0' && c <= '9') || (c == '-' && end == start)) {
    end++;                        // Increment end pointer
    c = *end;                     // Grab character to test
  }
  if (start == end) {             // We popped out on the first try
    parse_err(SYNERR, "integer expected at %c\n", c);
    result = 0;
  }
  else {
    _curchar = c;                 // Save the first character of next token
    *end = '\0';                  // NULL terminate the string in place
    result = atoi(start);         // Convert the string to an integer
    *end = _curchar;              // Restore buffer to original condition
  }

  // Reset _ptr to next char after token
  _ptr = end;

  return result;                   // integer
}


//------------------------------get_relation_dup------------------------------
// Looks for a relational operator in the buffer
// invokes a parse_err if the next token is not a relation
// This routine creates a duplicate of the string in the buffer.
char *ADLParser::get_relation_dup(void) {
  char         *result = NULL;    // relational operator being returned

  if( _curline == NULL )          // Return NULL at EOF.
    return  NULL;

  skipws();                       // Skip whitespace before relation
  char *start = _ptr;             // Store start of relational operator
  char first  = *_ptr;            // the first character
  if( (first == '=') || (first == '!') || (first == '<') || (first == '>') ) {
    next_char();
    char second = *_ptr;          // the second character
    if( second == '=' ) {
      next_char();
      char tmp  = *_ptr;
      *_ptr = '\0';               // NULL terminate
      result = strdup(start);     // Duplicate the string
      *_ptr = tmp;                // restore buffer
    } else {
      parse_err(SYNERR, "relational operator expected at %s\n", _ptr);
    }
  } else {
    parse_err(SYNERR, "relational operator expected at %s\n", _ptr);
  }

  return result;
}



//------------------------------get_oplist-------------------------------------
// Looks for identifier pairs where first must be the name of an operand, and
// second must be a name unique in the scope of this instruction.  Stores the
// names with a pointer to the OpClassForm of their type in a local name table.
void ADLParser::get_oplist(NameList &parameters, FormDict &operands) {
  OpClassForm *opclass = NULL;
  char        *ident   = NULL;

  do {
    next_char();             // skip open paren & comma characters
    skipws();
    if (_curchar == ')') break;

    // Get operand type, and check it against global name table
    ident = get_ident();
    if (ident == NULL) {
      parse_err(SYNERR, "optype identifier expected at %c\n", _curchar);
      return;
    }
    else {
      const Form  *form = _globalNames[ident];
      if( form == NULL ) {
        parse_err(SYNERR, "undefined operand type %s\n", ident);
        return;
      }

      // Check for valid operand type
      OpClassForm *opc  = form->is_opclass();
      OperandForm *oper = form->is_operand();
      if((oper == NULL) && (opc == NULL)) {
        parse_err(SYNERR, "identifier %s not operand type\n", ident);
        return;
      }
      opclass = opc;
    }
    // Debugging Stuff
    if (_AD._adl_debug > 1) fprintf(stderr, "\tOperand Type: %s\t", ident);

    // Get name of operand and add it to local name table
    if( (ident = get_unique_ident(operands, "operand")) == NULL) {
      return;
    }
    // Parameter names must not be global names.
    if( _globalNames[ident] != NULL ) {
         parse_err(SYNERR, "Reuse of global name %s as operand.\n",ident);
         return;
    }
    operands.Insert(ident, opclass);
    parameters.addName(ident);

    // Debugging Stuff
    if (_AD._adl_debug > 1) fprintf(stderr, "\tOperand Name: %s\n", ident);
    skipws();
  } while(_curchar == ',');

  if (_curchar != ')') parse_err(SYNERR, "missing ')'\n");
  else {
    next_char();  // set current character position past the close paren
  }
}


//------------------------------get_effectlist---------------------------------
// Looks for identifier pairs where first must be the name of a pre-defined,
// effect, and the second must be the name of an operand defined in the
// operand list of this instruction.  Stores the names with a pointer to the
// effect form in a local effects table.
void ADLParser::get_effectlist(FormDict &effects, FormDict &operands, bool& has_call) {
  OperandForm *opForm;
  Effect      *eForm;
  char        *ident;

  do {
    next_char();             // skip open paren & comma characters
    skipws();
    if (_curchar == ')') break;

    // Get effect type, and check it against global name table
    ident = get_ident();
    if (ident == NULL) {
      parse_err(SYNERR, "effect type identifier expected at %c\n", _curchar);
      return;
    }
    else {
      // Check for valid effect type
      const Form *form = _globalNames[ident];
      if( form == NULL ) {
        parse_err(SYNERR, "undefined effect type %s\n", ident);
        return;
      }
      else {
        if( (eForm = form->is_effect()) == NULL) {
          parse_err(SYNERR, "identifier %s not effect type\n", ident);
          return;
        }
      }
    }
      // Debugging Stuff
    if (_AD._adl_debug > 1) fprintf(stderr, "\tEffect Type: %s\t", ident);
    skipws();
    if (eForm->is(Component::CALL)) {
      if (_AD._adl_debug > 1) fprintf(stderr, "\n");
      has_call = true;
    } else {
      // Get name of operand and check that it is in the local name table
      if( (ident = get_unique_ident(effects, "effect")) == NULL) {
        parse_err(SYNERR, "missing operand identifier in effect list\n");
        return;
      }
      const Form *form = operands[ident];
      opForm = form ? form->is_operand() : NULL;
      if( opForm == NULL ) {
        if( form && form->is_opclass() ) {
          const char* cname = form->is_opclass()->_ident;
          parse_err(SYNERR, "operand classes are illegal in effect lists (found %s %s)\n", cname, ident);
        } else {
          parse_err(SYNERR, "undefined operand %s in effect list\n", ident);
        }
        return;
      }
      // Add the pair to the effects table
      effects.Insert(ident, eForm);
      // Debugging Stuff
      if (_AD._adl_debug > 1) fprintf(stderr, "\tOperand Name: %s\n", ident);
    }
    skipws();
  } while(_curchar == ',');

  if (_curchar != ')') parse_err(SYNERR, "missing ')'\n");
  else {
    next_char();  // set current character position past the close paren
  }
}


//-------------------------------preproc_line----------------------------------
// A "#line" keyword has been seen, so parse the rest of the line.
void ADLParser::preproc_line(void) {
  int line = get_int();
  skipws_no_preproc();
  const char* file = NULL;
  if (_curchar == '"') {
    next_char();              // Move past the initial '"'
    file = _ptr;
    while (true) {
      if (_curchar == '\n') {
        parse_err(SYNERR, "missing '\"' at end of #line directive");
        return;
      }
      if (_curchar == '"') {
        *_ptr  = '\0';          // Terminate the string
        next_char();
        skipws_no_preproc();
        break;
      }
      next_char();
    }
  }
  ensure_end_of_line();
  if (file != NULL)
    _AD._ADL_file._name = file;
  _buf.set_linenum(line);
}

//------------------------------preproc_define---------------------------------
// A "#define" keyword has been seen, so parse the rest of the line.
void ADLParser::preproc_define(void) {
  char* flag = get_ident_no_preproc();
  skipws_no_preproc();
  // only #define x y is supported for now
  char* def = get_ident_no_preproc();
  _AD.set_preproc_def(flag, def);
  skipws_no_preproc();
  if (_curchar != '\n') {
    parse_err(SYNERR, "non-identifier in preprocessor definition\n");
  }
}

//------------------------------preproc_undef----------------------------------
// An "#undef" keyword has been seen, so parse the rest of the line.
void ADLParser::preproc_undef(void) {
  char* flag = get_ident_no_preproc();
  skipws_no_preproc();
  ensure_end_of_line();
  _AD.set_preproc_def(flag, NULL);
}



//------------------------------parse_err--------------------------------------
// Issue a parser error message, and skip to the end of the current line
void ADLParser::parse_err(int flag, const char *fmt, ...) {
  va_list args;

  va_start(args, fmt);
  if (flag == 1)
    _AD._syntax_errs += _AD.emit_msg(0, flag, linenum(), fmt, args);
  else if (flag == 2)
    _AD._semantic_errs += _AD.emit_msg(0, flag, linenum(), fmt, args);
  else
    _AD._warnings += _AD.emit_msg(0, flag, linenum(), fmt, args);

  int error_char = _curchar;
  char* error_ptr = _ptr+1;
  for(;*_ptr != '\n'; _ptr++) ; // Skip to the end of the current line
  _curchar = '\n';
  va_end(args);
  _AD._no_output = 1;

  if (flag == 1) {
    char* error_tail = strchr(error_ptr, '\n');
    char tem = *error_ptr;
    error_ptr[-1] = '\0';
    char* error_head = error_ptr-1;
    while (error_head > _curline && *error_head)  --error_head;
    if (error_tail)  *error_tail = '\0';
    fprintf(stderr, "Error Context:  %s>>>%c<<<%s\n",
            error_head, error_char, error_ptr);
    if (error_tail)  *error_tail = '\n';
    error_ptr[-1] = tem;
  }
}

//---------------------------ensure_start_of_line------------------------------
// A preprocessor directive has been encountered.  Be sure it has fallen at
// the beginning of a line, or else report an error.
void ADLParser::ensure_start_of_line(void) {
  if (_curchar == '\n') { next_line(); return; }
  assert( _ptr >= _curline && _ptr < _curline+strlen(_curline),
          "Must be able to find which line we are in" );

  for (char *s = _curline; s < _ptr; s++) {
    if (*s > ' ') {
      parse_err(SYNERR, "'%c' must be at beginning of line\n", _curchar);
      break;
    }
  }
}

//---------------------------ensure_end_of_line--------------------------------
// A preprocessor directive has been parsed.  Be sure there is no trailing
// garbage at the end of this line.  Set the scan point to the beginning of
// the next line.
void ADLParser::ensure_end_of_line(void) {
  skipws_no_preproc();
  if (_curchar != '\n' && _curchar != '\0') {
    parse_err(SYNERR, "garbage char '%c' at end of line\n", _curchar);
  } else {
    next_char_or_line();
  }
}

//---------------------------handle_preproc------------------------------------
// The '#' character introducing a preprocessor directive has been found.
// Parse the whole directive name (e.g., #define, #endif) and take appropriate
// action.  If we are in an "untaken" span of text, simply keep track of
// #ifdef nesting structure, so we can find out when to start taking text
// again.  (In this state, we "sort of support" C's #if directives, enough
// to disregard their associated #else and #endif lines.)  If we are in a
// "taken" span of text, there are two cases:  "#define" and "#undef"
// directives are preserved and passed up to the caller, which eventually
// passes control to the top-level parser loop, which handles #define and
// #undef directly.  (This prevents these directives from occurring in
// arbitrary positions in the AD file--we require better structure than C.)
// In the other case, and #ifdef, #ifndef, #else, or #endif is silently
// processed as whitespace, with the "taken" state of the text correctly
// updated.  This routine returns "false" exactly in the case of a "taken"
// #define or #undef, which tells the caller that a preprocessor token
// has appeared which must be handled explicitly by the parse loop.
bool ADLParser::handle_preproc_token() {
  assert(*_ptr == '#', "must be at start of preproc");
  ensure_start_of_line();
  next_char();
  skipws_no_preproc();
  char* start_ident = _ptr;
  char* ident = (_curchar == '\n') ? NULL : get_ident_no_preproc();
  if (ident == NULL) {
    parse_err(SYNERR, "expected preprocessor command, got end of line\n");
  } else if (!strcmp(ident, "ifdef") ||
             !strcmp(ident, "ifndef")) {
    char* flag = get_ident_no_preproc();
    ensure_end_of_line();
    // Test the identifier only if we are already in taken code:
    bool flag_def  = preproc_taken() && (_AD.get_preproc_def(flag) != NULL);
    bool now_taken = !strcmp(ident, "ifdef") ? flag_def : !flag_def;
    begin_if_def(now_taken);
  } else if (!strcmp(ident, "if")) {
    if (preproc_taken())
      parse_err(SYNERR, "unimplemented: #%s %s", ident, _ptr+1);
    next_line();
    // Intelligently skip this nested C preprocessor directive:
    begin_if_def(true);
  } else if (!strcmp(ident, "else")) {
    ensure_end_of_line();
    invert_if_def();
  } else if (!strcmp(ident, "endif")) {
    ensure_end_of_line();
    end_if_def();
  } else if (preproc_taken()) {
    // pass this token up to the main parser as "#define" or "#undef"
    _ptr = start_ident;
    _curchar = *--_ptr;
    if( _curchar != '#' ) {
      parse_err(SYNERR, "no space allowed after # in #define or #undef");
      assert(_curchar == '#', "no space allowed after # in #define or #undef");
    }
    return false;
  }
  return true;
}

//---------------------------skipws_common-------------------------------------
// Skip whitespace, including comments and newlines, while keeping an accurate
// line count.
// Maybe handle certain preprocessor constructs: #ifdef, #ifndef, #else, #endif
void ADLParser::skipws_common(bool do_preproc) {
  char *start = _ptr;
  char *next = _ptr + 1;

  if (*_ptr == '\0') {
    // Check for string terminator
    if (_curchar > ' ')  return;
    if (_curchar == '\n') {
      if (!do_preproc)  return;            // let caller handle the newline
      next_line();
      _ptr = _curline; next = _ptr + 1;
    }
    else if (_curchar == '#' ||
        (_curchar == '/' && (*next == '/' || *next == '*'))) {
      parse_err(SYNERR, "unimplemented: comment token in a funny place");
    }
  }
  while(_curline != NULL) {                // Check for end of file
    if (*_ptr == '\n') {                   // keep proper track of new lines
      if (!do_preproc)  break;             // let caller handle the newline
      next_line();
      _ptr = _curline; next = _ptr + 1;
    }
    else if ((*_ptr == '/') && (*next == '/'))      // C++ comment
      do { _ptr++; next++; } while(*_ptr != '\n');  // So go to end of line
    else if ((*_ptr == '/') && (*next == '*')) {    // C comment
      _ptr++; next++;
      do {
        _ptr++; next++;
        if (*_ptr == '\n') {               // keep proper track of new lines
          next_line();                     // skip newlines within comments
          if (_curline == NULL) {          // check for end of file
            parse_err(SYNERR, "end-of-file detected inside comment\n");
            break;
          }
          _ptr = _curline; next = _ptr + 1;
        }
      } while(!((*_ptr == '*') && (*next == '/'))); // Go to end of comment
      _ptr = ++next; next++;               // increment _ptr past comment end
    }
    else if (do_preproc && *_ptr == '#') {
      // Note that this calls skipws_common(false) recursively!
      bool preproc_handled = handle_preproc_token();
      if (!preproc_handled) {
        if (preproc_taken()) {
          return;  // short circuit
        }
        ++_ptr;    // skip the preprocessor character
      }
      next = _ptr+1;
    } else if(*_ptr > ' ' && !(do_preproc && !preproc_taken())) {
      break;
    }
    else if (*_ptr == '"' || *_ptr == '\'') {
      assert(do_preproc, "only skip strings if doing preproc");
      // skip untaken quoted string
      int qchar = *_ptr;
      while (true) {
        ++_ptr;
        if (*_ptr == qchar) { ++_ptr; break; }
        if (*_ptr == '\\')  ++_ptr;
        if (*_ptr == '\n' || *_ptr == '\0') {
          parse_err(SYNERR, "newline in string");
          break;
        }
      }
      next = _ptr + 1;
    }
    else { ++_ptr; ++next; }
  }
  if( _curline != NULL )            // at end of file _curchar isn't valid
    _curchar = *_ptr;               // reset _curchar to maintain invariant
}

//---------------------------cur_char-----------------------------------------
char ADLParser::cur_char() {
  return (_curchar);
}

//---------------------------next_char-----------------------------------------
void ADLParser::next_char() {
  if (_curchar == '\n')  parse_err(WARN, "must call next_line!");
  _curchar = *++_ptr;
  // if ( _curchar == '\n' ) {
  //   next_line();
  // }
}

//---------------------------next_char_or_line---------------------------------
void ADLParser::next_char_or_line() {
  if ( _curchar != '\n' ) {
    _curchar = *++_ptr;
  } else {
    next_line();
    _ptr = _curline;
    _curchar = *_ptr;  // maintain invariant
  }
}

//---------------------------next_line-----------------------------------------
void ADLParser::next_line() {
  _curline = _buf.get_line();
  _curchar = ' ';
}

//------------------------get_line_string--------------------------------------
// Prepended location descriptor, for debugging.
// Must return a malloced string (that can be freed if desired).
char* ADLParser::get_line_string(int linenum) {
  const char* file = _AD._ADL_file._name;
  int         line = linenum ? linenum : this->linenum();
  char* location = (char *)AllocateHeap(strlen(file) + 100);
  sprintf(location, "\n#line %d \"%s\"\n", line, file);
  return location;
}

//-------------------------is_literal_constant---------------------------------
bool ADLParser::is_literal_constant(const char *param) {
  if (param[0] == 0)     return false;  // null string
  if (param[0] == '(')   return true;   // parenthesized expression
  if (param[0] == '0' && (param[1] == 'x' || param[1] == 'X')) {
    // Make sure it's a hex constant.
    int i = 2;
    do {
      if( !ADLParser::is_hex_digit(*(param+i)) )  return false;
      ++i;
    } while( *(param+i) != 0 );
    return true;
  }
  return false;
}

//---------------------------is_hex_digit--------------------------------------
bool ADLParser::is_hex_digit(char digit) {
  return ((digit >= '0') && (digit <= '9'))
       ||((digit >= 'a') && (digit <= 'f'))
       ||((digit >= 'A') && (digit <= 'F'));
}

//---------------------------is_int_token--------------------------------------
bool ADLParser::is_int_token(const char* token, int& intval) {
  const char* cp = token;
  while (*cp != '\0' && *cp <= ' ')  cp++;
  if (*cp == '-')  cp++;
  int ndigit = 0;
  while (*cp >= '0' && *cp <= '9')  { cp++; ndigit++; }
  while (*cp != '\0' && *cp <= ' ')  cp++;
  if (ndigit == 0 || *cp != '\0') {
    return false;
  }
  intval = atoi(token);
  return true;
}

static const char* skip_expr_ws(const char* str) {
  const char * cp = str;
  while (cp[0]) {
    if (cp[0] <= ' ') {
      ++cp;
    } else if (cp[0] == '#') {
      ++cp;
      while (cp[0] == ' ')  ++cp;
      assert(0 == strncmp(cp, "line", 4), "must be a #line directive");
      const char* eol = strchr(cp, '\n');
      assert(eol != NULL, "must find end of line");
      if (eol == NULL)  eol = cp + strlen(cp);
      cp = eol;
    } else {
      break;
    }
  }
  return cp;
}

//-----------------------equivalent_expressions--------------------------------
bool ADLParser::equivalent_expressions(const char* str1, const char* str2) {
  if (str1 == str2)
    return true;
  else if (str1 == NULL || str2 == NULL)
    return false;
  const char* cp1 = str1;
  const char* cp2 = str2;
  char in_quote = '\0';
  while (cp1[0] && cp2[0]) {
    if (!in_quote) {
      // skip spaces and/or cpp directives
      const char* cp1a = skip_expr_ws(cp1);
      const char* cp2a = skip_expr_ws(cp2);
      if (cp1a > cp1 && cp2a > cp2) {
        cp1 = cp1a; cp2 = cp2a;
        continue;
      }
      if (cp1a > cp1 || cp2a > cp2)  break; // fail
    }
    // match one non-space char
    if (cp1[0] != cp2[0])  break; // fail
    char ch = cp1[0];
    cp1++; cp2++;
    // watch for quotes
    if (in_quote && ch == '\\') {
      if (cp1[0] != cp2[0])  break; // fail
      if (!cp1[0])  break;
      cp1++; cp2++;
    }
    if (in_quote && ch == in_quote) {
      in_quote = '\0';
    } else if (!in_quote && (ch == '"' || ch == '\'')) {
      in_quote = ch;
    }
  }
  return (!cp1[0] && !cp2[0]);
}


//-------------------------------trim------------------------------------------
void ADLParser::trim(char* &token) {
  while (*token <= ' ')  token++;
  char* end = token + strlen(token);
  while (end > token && *(end-1) <= ' ')  --end;
  *end = '\0';
}
