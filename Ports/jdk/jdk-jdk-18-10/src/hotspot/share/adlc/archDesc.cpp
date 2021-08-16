//
// Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//
//


// archDesc.cpp - Internal format for architecture definition
#include "adlc.hpp"

static FILE *errfile = stderr;

//--------------------------- utility functions -----------------------------
inline char toUpper(char lower) {
  return (('a' <= lower && lower <= 'z') ? ((char) (lower + ('A'-'a'))) : lower);
}
char *toUpper(const char *str) {
  char *upper  = new char[strlen(str)+1];
  char *result = upper;
  const char *end    = str + strlen(str);
  for (; str < end; ++str, ++upper) {
    *upper = toUpper(*str);
  }
  *upper = '\0';
  return result;
}

//---------------------------ChainList Methods-------------------------------
ChainList::ChainList() {
}

void ChainList::insert(const char *name, const char *cost, const char *rule) {
  _name.addName(name);
  _cost.addName(cost);
  _rule.addName(rule);
}

bool ChainList::search(const char *name) {
  return _name.search(name);
}

void ChainList::reset() {
  _name.reset();
  _cost.reset();
  _rule.reset();
}

bool ChainList::iter(const char * &name, const char * &cost, const char * &rule) {
  bool        notDone = false;
  const char *n       = _name.iter();
  const char *c       = _cost.iter();
  const char *r       = _rule.iter();

  if (n && c && r) {
    notDone = true;
    name = n;
    cost = c;
    rule = r;
  }

  return notDone;
}

void ChainList::dump() {
  output(stderr);
}

void ChainList::output(FILE *fp) {
  fprintf(fp, "\nChain Rules: output resets iterator\n");
  const char   *cost  = NULL;
  const char   *name  = NULL;
  const char   *rule  = NULL;
  bool   chains_exist = false;
  for(reset(); (iter(name,cost,rule)) == true; ) {
    fprintf(fp, "Chain to <%s> at cost #%s using %s_rule\n",name, cost ? cost : "0", rule);
    //  // Check for transitive chain rules
    //  Form *form = (Form *)_globalNames[rule];
    //  if (form->is_instruction()) {
    //    // chain_rule(fp, indent, name, cost, rule);
    //    chain_rule(fp, indent, name, cost, rule);
    //  }
  }
  reset();
  if( ! chains_exist ) {
    fprintf(fp, "No entries in this ChainList\n");
  }
}


//---------------------------MatchList Methods-------------------------------
bool MatchList::search(const char *opc, const char *res, const char *lch,
                       const char *rch, Predicate *pr) {
  bool tmp = false;
  if ((res == _resultStr) || (res && _resultStr && !strcmp(res, _resultStr))) {
    if ((lch == _lchild) || (lch && _lchild && !strcmp(lch, _lchild))) {
      if ((rch == _rchild) || (rch && _rchild && !strcmp(rch, _rchild))) {
        char * predStr = get_pred();
        char * prStr = pr?pr->_pred:NULL;
        if (ADLParser::equivalent_expressions(prStr, predStr)) {
          return true;
        }
      }
    }
  }
  if (_next) {
    tmp = _next->search(opc, res, lch, rch, pr);
  }
  return tmp;
}


void MatchList::dump() {
  output(stderr);
}

void MatchList::output(FILE *fp) {
  fprintf(fp, "\nMatchList output is Unimplemented();\n");
}


//---------------------------ArchDesc Constructor and Destructor-------------

ArchDesc::ArchDesc()
  : _globalNames(cmpstr,hashstr, Form::arena),
    _globalDefs(cmpstr,hashstr, Form::arena),
    _preproc_table(cmpstr,hashstr, Form::arena),
    _idealIndex(cmpstr,hashstr, Form::arena),
    _internalOps(cmpstr,hashstr, Form::arena),
    _internalMatch(cmpstr,hashstr, Form::arena),
    _chainRules(cmpstr,hashstr, Form::arena),
    _cisc_spill_operand(NULL),
    _needs_deep_clone_jvms(false) {

      // Initialize the opcode to MatchList table with NULLs
      for( int i=0; i<_last_opcode; ++i ) {
        _mlistab[i] = NULL;
      }

      // Set-up the global tables
      initKeywords(_globalNames);    // Initialize the Name Table with keywords

      // Prime user-defined types with predefined types: Set, RegI, RegF, ...
      initBaseOpTypes();

      // Initialize flags & counters
      _TotalLines        = 0;
      _no_output         = 0;
      _quiet_mode        = 0;
      _disable_warnings  = 0;
      _dfa_debug         = 0;
      _dfa_small         = 0;
      _adl_debug         = 0;
      _adlocation_debug  = 0;
      _internalOpCounter = 0;
      _cisc_spill_debug  = false;
      _short_branch_debug = false;

      // Initialize match rule flags
      for (int i = 0; i < _last_opcode; i++) {
        _has_match_rule[i] = false;
      }

      // Error/Warning Counts
      _syntax_errs       = 0;
      _semantic_errs     = 0;
      _warnings          = 0;
      _internal_errs     = 0;

      // Initialize I/O Files
      _ADL_file._name = NULL; _ADL_file._fp = NULL;
      // Machine dependent output files
      _DFA_file._name    = NULL;  _DFA_file._fp = NULL;
      _HPP_file._name    = NULL;  _HPP_file._fp = NULL;
      _CPP_file._name    = NULL;  _CPP_file._fp = NULL;
      _bug_file._name    = "bugs.out";      _bug_file._fp = NULL;

      // Initialize Register & Pipeline Form Pointers
      _register = NULL;
      _encode = NULL;
      _pipeline = NULL;
      _frame = NULL;
}

ArchDesc::~ArchDesc() {
  // Clean-up and quit

}

//---------------------------ArchDesc methods: Public ----------------------
// Store forms according to type
void ArchDesc::addForm(PreHeaderForm *ptr) { _pre_header.addForm(ptr); };
void ArchDesc::addForm(HeaderForm    *ptr) { _header.addForm(ptr); };
void ArchDesc::addForm(SourceForm    *ptr) { _source.addForm(ptr); };
void ArchDesc::addForm(EncodeForm    *ptr) { _encode = ptr; };
void ArchDesc::addForm(InstructForm  *ptr) { _instructions.addForm(ptr); };
void ArchDesc::addForm(MachNodeForm  *ptr) { _machnodes.addForm(ptr); };
void ArchDesc::addForm(OperandForm   *ptr) { _operands.addForm(ptr); };
void ArchDesc::addForm(OpClassForm   *ptr) { _opclass.addForm(ptr); };
void ArchDesc::addForm(AttributeForm *ptr) { _attributes.addForm(ptr); };
void ArchDesc::addForm(RegisterForm  *ptr) { _register = ptr; };
void ArchDesc::addForm(FrameForm     *ptr) { _frame = ptr; };
void ArchDesc::addForm(PipelineForm  *ptr) { _pipeline = ptr; };

// Build MatchList array and construct MatchLists
void ArchDesc::generateMatchLists() {
  // Call inspection routines to populate array
  inspectOperands();
  inspectInstructions();
}

// Build MatchList structures for operands
void ArchDesc::inspectOperands() {

  // Iterate through all operands
  _operands.reset();
  OperandForm *op;
  for( ; (op = (OperandForm*)_operands.iter()) != NULL;) {
    // Construct list of top-level operands (components)
    op->build_components();

    // Ensure that match field is defined.
    if ( op->_matrule == NULL )  continue;

    // Type check match rules
    check_optype(op->_matrule);

    // Construct chain rules
    build_chain_rule(op);

    MatchRule *mrule = op->_matrule;
    Predicate *pred  = op->_predicate;

    // Grab the machine type of the operand
    const char  *rootOp    = op->_ident;
    mrule->_machType  = rootOp;

    // Check for special cases
    if (strcmp(rootOp,"Universe")==0) continue;
    if (strcmp(rootOp,"label")==0) continue;
    // !!!!! !!!!!
    assert( strcmp(rootOp,"sReg") != 0, "Disable untyped 'sReg'");
    if (strcmp(rootOp,"sRegI")==0) continue;
    if (strcmp(rootOp,"sRegP")==0) continue;
    if (strcmp(rootOp,"sRegF")==0) continue;
    if (strcmp(rootOp,"sRegD")==0) continue;
    if (strcmp(rootOp,"sRegL")==0) continue;

    // Cost for this match
    const char *costStr     = op->cost();
    const char *defaultCost =
      ((AttributeForm*)_globalNames[AttributeForm::_op_cost])->_attrdef;
    const char *cost        =  costStr? costStr : defaultCost;

    // Find result type for match.
    const char *result      = op->reduce_result();

    // Construct a MatchList for this entry.
    // Iterate over the list to enumerate all match cases for operands with multiple match rules.
    for (; mrule != NULL; mrule = mrule->_next) {
      mrule->_machType = rootOp;
      buildMatchList(mrule, result, rootOp, pred, cost);
    }
  }
}

// Build MatchList structures for instructions
void ArchDesc::inspectInstructions() {

  // Iterate through all instructions
  _instructions.reset();
  InstructForm *instr;
  for( ; (instr = (InstructForm*)_instructions.iter()) != NULL; ) {
    // Construct list of top-level operands (components)
    instr->build_components();

    // Ensure that match field is defined.
    if ( instr->_matrule == NULL )  continue;

    MatchRule &mrule = *instr->_matrule;
    Predicate *pred  =  instr->build_predicate();

    // Grab the machine type of the operand
    const char  *rootOp    = instr->_ident;
    mrule._machType  = rootOp;

    // Cost for this match
    const char *costStr = instr->cost();
    const char *defaultCost =
      ((AttributeForm*)_globalNames[AttributeForm::_ins_cost])->_attrdef;
    const char *cost    =  costStr? costStr : defaultCost;

    // Find result type for match
    const char *result  = instr->reduce_result();

    if (( instr->is_ideal_branch() && instr->label_position() == -1) ||
        (!instr->is_ideal_branch() && instr->label_position() != -1)) {
      syntax_err(instr->_linenum, "%s: Only branches to a label are supported\n", rootOp);
    }

    Attribute *attr = instr->_attribs;
    while (attr != NULL) {
      if (strcmp(attr->_ident,"ins_short_branch") == 0 &&
          attr->int_val(*this) != 0) {
        if (!instr->is_ideal_branch() || instr->label_position() == -1) {
          syntax_err(instr->_linenum, "%s: Only short branch to a label is supported\n", rootOp);
        }
        instr->set_short_branch(true);
      } else if (strcmp(attr->_ident,"ins_alignment") == 0 &&
          attr->int_val(*this) != 0) {
        instr->set_alignment(attr->int_val(*this));
      }
      attr = (Attribute *)attr->_next;
    }

    if (!instr->is_short_branch()) {
      buildMatchList(instr->_matrule, result, mrule._machType, pred, cost);
    }
  }
}

static int setsResult(MatchRule &mrule) {
  if (strcmp(mrule._name,"Set") == 0) return 1;
  return 0;
}

const char *ArchDesc::getMatchListIndex(MatchRule &mrule) {
  if (setsResult(mrule)) {
    // right child
    return mrule._rChild->_opType;
  } else {
    // first entry
    return mrule._opType;
  }
}


//------------------------------result of reduction----------------------------


//------------------------------left reduction---------------------------------
// Return the left reduction associated with an internal name
const char *ArchDesc::reduceLeft(char         *internalName) {
  const char *left  = NULL;
  MatchNode *mnode = (MatchNode*)_internalMatch[internalName];
  if (mnode->_lChild) {
    mnode = mnode->_lChild;
    left = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  return left;
}


//------------------------------right reduction--------------------------------
const char *ArchDesc::reduceRight(char  *internalName) {
  const char *right  = NULL;
  MatchNode *mnode = (MatchNode*)_internalMatch[internalName];
  if (mnode->_rChild) {
    mnode = mnode->_rChild;
    right = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  return right;
}


//------------------------------check_optype-----------------------------------
void ArchDesc::check_optype(MatchRule *mrule) {
  MatchRule *rule = mrule;

  //   !!!!!
  //   // Cycle through the list of match rules
  //   while(mrule) {
  //     // Check for a filled in type field
  //     if (mrule->_opType == NULL) {
  //     const Form  *form    = operands[_result];
  //     OpClassForm *opcForm = form ? form->is_opclass() : NULL;
  //     assert(opcForm != NULL, "Match Rule contains invalid operand name.");
  //     }
  //     char *opType = opcForm->_ident;
  //   }
}

//------------------------------add_chain_rule_entry--------------------------
void ArchDesc::add_chain_rule_entry(const char *src, const char *cost,
                                    const char *result) {
  // Look-up the operation in chain rule table
  ChainList *lst = (ChainList *)_chainRules[src];
  if (lst == NULL) {
    lst = new ChainList();
    _chainRules.Insert(src, lst);
  }
  if (!lst->search(result)) {
    if (cost == NULL) {
      cost = ((AttributeForm*)_globalNames[AttributeForm::_op_cost])->_attrdef;
    }
    lst->insert(result, cost, result);
  }
}

//------------------------------build_chain_rule-------------------------------
void ArchDesc::build_chain_rule(OperandForm *oper) {
  MatchRule     *rule;

  // Check for chain rules here
  // If this is only a chain rule
  if ((oper->_matrule) && (oper->_matrule->_lChild == NULL) &&
      (oper->_matrule->_rChild == NULL)) {

    {
      const Form *form = _globalNames[oper->_matrule->_opType];
      if ((form) && form->is_operand() &&
          (form->ideal_only() == false)) {
        add_chain_rule_entry(oper->_matrule->_opType, oper->cost(), oper->_ident);
      }
    }
    // Check for additional chain rules
    if (oper->_matrule->_next) {
      rule = oper->_matrule;
      do {
        rule = rule->_next;
        // Any extra match rules after the first must be chain rules
        const Form *form = _globalNames[rule->_opType];
        if ((form) && form->is_operand() &&
            (form->ideal_only() == false)) {
          add_chain_rule_entry(rule->_opType, oper->cost(), oper->_ident);
        }
      } while(rule->_next != NULL);
    }
  }
  else if ((oper->_matrule) && (oper->_matrule->_next)) {
    // Regardles of whether the first matchrule is a chain rule, check the list
    rule = oper->_matrule;
    do {
      rule = rule->_next;
      // Any extra match rules after the first must be chain rules
      const Form *form = _globalNames[rule->_opType];
      if ((form) && form->is_operand() &&
          (form->ideal_only() == false)) {
        assert( oper->cost(), "This case expects NULL cost, not default cost");
        add_chain_rule_entry(rule->_opType, oper->cost(), oper->_ident);
      }
    } while(rule->_next != NULL);
  }

}

//------------------------------buildMatchList---------------------------------
// operands and instructions provide the result
void ArchDesc::buildMatchList(MatchRule *mrule, const char *resultStr,
                              const char *rootOp, Predicate *pred,
                              const char *cost) {
  const char *leftstr, *rightstr;
  MatchNode  *mnode;

  leftstr = rightstr = NULL;
  // Check for chain rule, and do not generate a match list for it
  if ( mrule->is_chain_rule(_globalNames) ) {
    return;
  }

  // Identify index position among ideal operands
  intptr_t    index     = _last_opcode;
  const char  *indexStr  = getMatchListIndex(*mrule);
  index  = (intptr_t)_idealIndex[indexStr];
  if (index == 0) {
    fprintf(stderr, "Ideal node missing: %s\n", indexStr);
    assert(index != 0, "Failed lookup of ideal node\n");
  }

  // Check that this will be placed appropriately in the DFA
  if (index >= _last_opcode) {
    fprintf(stderr, "Invalid match rule %s <-- ( %s )\n",
            resultStr ? resultStr : " ",
            rootOp    ? rootOp    : " ");
    assert(index < _last_opcode, "Matching item not in ideal graph\n");
    return;
  }


  // Walk the MatchRule, generating MatchList entries for each level
  // of the rule (each nesting of parentheses)
  // Check for "Set"
  if (!strcmp(mrule->_opType, "Set")) {
    mnode = mrule->_rChild;
    buildMList(mnode, rootOp, resultStr, pred, cost);
    return;
  }
  // Build MatchLists for children
  // Check each child for an internal operand name, and use that name
  // for the parent's matchlist entry if it exists
  mnode = mrule->_lChild;
  if (mnode) {
    buildMList(mnode, NULL, NULL, NULL, NULL);
    leftstr = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  mnode = mrule->_rChild;
  if (mnode) {
    buildMList(mnode, NULL, NULL, NULL, NULL);
    rightstr = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  // Search for an identical matchlist entry already on the list
  if ((_mlistab[index] == NULL) ||
      (_mlistab[index] &&
       !_mlistab[index]->search(rootOp, resultStr, leftstr, rightstr, pred))) {
    // Place this match rule at front of list
    MatchList *mList =
      new MatchList(_mlistab[index], pred, cost,
                    rootOp, resultStr, leftstr, rightstr);
    _mlistab[index] = mList;
  }
}

// Recursive call for construction of match lists
void ArchDesc::buildMList(MatchNode *node, const char *rootOp,
                          const char *resultOp, Predicate *pred,
                          const char *cost) {
  const char *leftstr, *rightstr;
  const char *resultop;
  const char *opcode;
  MatchNode  *mnode;
  Form       *form;

  leftstr = rightstr = NULL;
  // Do not process leaves of the Match Tree if they are not ideal
  if ((node) && (node->_lChild == NULL) && (node->_rChild == NULL) &&
      ((form = (Form *)_globalNames[node->_opType]) != NULL) &&
      (!form->ideal_only())) {
    return;
  }

  // Identify index position among ideal operands
  intptr_t index = _last_opcode;
  const char *indexStr = node ? node->_opType : (char *) " ";
  index = (intptr_t)_idealIndex[indexStr];
  if (index == 0) {
    fprintf(stderr, "error: operand \"%s\" not found\n", indexStr);
    assert(0, "fatal error");
  }

  if (node == NULL) {
    fprintf(stderr, "error: node is NULL\n");
    assert(0, "fatal error");
  }
  // Build MatchLists for children
  // Check each child for an internal operand name, and use that name
  // for the parent's matchlist entry if it exists
  mnode = node->_lChild;
  if (mnode) {
    buildMList(mnode, NULL, NULL, NULL, NULL);
    leftstr = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  mnode = node->_rChild;
  if (mnode) {
    buildMList(mnode, NULL, NULL, NULL, NULL);
    rightstr = mnode->_internalop ? mnode->_internalop : mnode->_opType;
  }
  // Grab the string for the opcode of this list entry
  if (rootOp == NULL) {
    opcode = (node->_internalop) ? node->_internalop : node->_opType;
  } else {
    opcode = rootOp;
  }
  // Grab the string for the result of this list entry
  if (resultOp == NULL) {
    resultop = (node->_internalop) ? node->_internalop : node->_opType;
  }
  else resultop = resultOp;
  // Search for an identical matchlist entry already on the list
  if ((_mlistab[index] == NULL) || (_mlistab[index] &&
                                    !_mlistab[index]->search(opcode, resultop, leftstr, rightstr, pred))) {
    // Place this match rule at front of list
    MatchList *mList =
      new MatchList(_mlistab[index],pred,cost,
                    opcode, resultop, leftstr, rightstr);
    _mlistab[index] = mList;
  }
}

// Count number of OperandForms defined
int  ArchDesc::operandFormCount() {
  // Only interested in ones with non-NULL match rule
  int  count = 0; _operands.reset();
  OperandForm *cur;
  for( ; (cur = (OperandForm*)_operands.iter()) != NULL; ) {
    if (cur->_matrule != NULL) ++count;
  };
  return count;
}

// Count number of OpClassForms defined
int  ArchDesc::opclassFormCount() {
  // Only interested in ones with non-NULL match rule
  int  count = 0; _operands.reset();
  OpClassForm *cur;
  for( ; (cur = (OpClassForm*)_opclass.iter()) != NULL; ) {
    ++count;
  };
  return count;
}

// Count number of InstructForms defined
int  ArchDesc::instructFormCount() {
  // Only interested in ones with non-NULL match rule
  int  count = 0; _instructions.reset();
  InstructForm *cur;
  for( ; (cur = (InstructForm*)_instructions.iter()) != NULL; ) {
    if (cur->_matrule != NULL) ++count;
  };
  return count;
}


//------------------------------get_preproc_def--------------------------------
// Return the textual binding for a given CPP flag name.
// Return NULL if there is no binding, or it has been #undef-ed.
char* ArchDesc::get_preproc_def(const char* flag) {
  // In case of syntax errors, flag may take the value NULL.
  SourceForm* deff = NULL;
  if (flag != NULL)
    deff = (SourceForm*) _preproc_table[flag];
  return (deff == NULL) ? NULL : deff->_code;
}


//------------------------------set_preproc_def--------------------------------
// Change or create a textual binding for a given CPP flag name.
// Giving NULL means the flag name is to be #undef-ed.
// In any case, _preproc_list collects all names either #defined or #undef-ed.
void ArchDesc::set_preproc_def(const char* flag, const char* def) {
  SourceForm* deff = (SourceForm*) _preproc_table[flag];
  if (deff == NULL) {
    deff = new SourceForm(NULL);
    _preproc_table.Insert(flag, deff);
    _preproc_list.addName(flag);   // this supports iteration
  }
  deff->_code = (char*) def;
}


bool ArchDesc::verify() {

  if (_register)
    assert( _register->verify(), "Register declarations failed verification");
  if (!_quiet_mode)
    fprintf(stderr,"\n");
  // fprintf(stderr,"---------------------------- Verify Operands ---------------\n");
  // _operands.verify();
  // fprintf(stderr,"\n");
  // fprintf(stderr,"---------------------------- Verify Operand Classes --------\n");
  // _opclass.verify();
  // fprintf(stderr,"\n");
  // fprintf(stderr,"---------------------------- Verify Attributes  ------------\n");
  // _attributes.verify();
  // fprintf(stderr,"\n");
  if (!_quiet_mode)
    fprintf(stderr,"---------------------------- Verify Instructions ----------------------------\n");
  _instructions.verify();
  if (!_quiet_mode)
    fprintf(stderr,"\n");
  // if ( _encode ) {
  //   fprintf(stderr,"---------------------------- Verify Encodings --------------\n");
  //   _encode->verify();
  // }

  //if (_pipeline) _pipeline->verify();

  return true;
}


void ArchDesc::dump() {
  _pre_header.dump();
  _header.dump();
  _source.dump();
  if (_register) _register->dump();
  fprintf(stderr,"\n");
  fprintf(stderr,"------------------ Dump Operands ---------------------\n");
  _operands.dump();
  fprintf(stderr,"\n");
  fprintf(stderr,"------------------ Dump Operand Classes --------------\n");
  _opclass.dump();
  fprintf(stderr,"\n");
  fprintf(stderr,"------------------ Dump Attributes  ------------------\n");
  _attributes.dump();
  fprintf(stderr,"\n");
  fprintf(stderr,"------------------ Dump Instructions -----------------\n");
  _instructions.dump();
  if ( _encode ) {
    fprintf(stderr,"------------------ Dump Encodings --------------------\n");
    _encode->dump();
  }
  if (_pipeline) _pipeline->dump();
}


//------------------------------init_keywords----------------------------------
// Load the kewords into the global name table
void ArchDesc::initKeywords(FormDict& names) {
  // Insert keyword strings into Global Name Table.  Keywords have a NULL value
  // field for quick easy identification when checking identifiers.
  names.Insert("instruct", NULL);
  names.Insert("operand", NULL);
  names.Insert("attribute", NULL);
  names.Insert("source", NULL);
  names.Insert("register", NULL);
  names.Insert("pipeline", NULL);
  names.Insert("constraint", NULL);
  names.Insert("predicate", NULL);
  names.Insert("encode", NULL);
  names.Insert("enc_class", NULL);
  names.Insert("interface", NULL);
  names.Insert("opcode", NULL);
  names.Insert("ins_encode", NULL);
  names.Insert("match", NULL);
  names.Insert("effect", NULL);
  names.Insert("expand", NULL);
  names.Insert("rewrite", NULL);
  names.Insert("reg_def", NULL);
  names.Insert("reg_class", NULL);
  names.Insert("alloc_class", NULL);
  names.Insert("resource", NULL);
  names.Insert("pipe_class", NULL);
  names.Insert("pipe_desc", NULL);
}


//------------------------------internal_err----------------------------------
// Issue a parser error message, and skip to the end of the current line
void ArchDesc::internal_err(const char *fmt, ...) {
  va_list args;

  va_start(args, fmt);
  _internal_errs += emit_msg(0, INTERNAL_ERR, 0, fmt, args);
  va_end(args);

  _no_output = 1;
}

//------------------------------syntax_err----------------------------------
// Issue a parser error message, and skip to the end of the current line
void ArchDesc::syntax_err(int lineno, const char *fmt, ...) {
  va_list args;

  va_start(args, fmt);
  _internal_errs += emit_msg(0, SYNERR, lineno, fmt, args);
  va_end(args);

  _no_output = 1;
}

//------------------------------emit_msg---------------------------------------
// Emit a user message, typically a warning or error
int ArchDesc::emit_msg(int quiet, int flag, int line, const char *fmt,
    va_list args) {
  static int  last_lineno = -1;
  int         i;
  const char *pref;

  switch(flag) {
  case 0: pref = "Warning: "; break;
  case 1: pref = "Syntax Error: "; break;
  case 2: pref = "Semantic Error: "; break;
  case 3: pref = "Internal Error: "; break;
  default: assert(0, ""); break;
  }

  if (line == last_lineno) return 0;
  last_lineno = line;

  if (!quiet) {                        /* no output if in quiet mode         */
    i = fprintf(errfile, "%s(%d) ", _ADL_file._name, line);
    while (i++ <= 15)  fputc(' ', errfile);
    fprintf(errfile, "%-8s:", pref);
    vfprintf(errfile, fmt, args);
    fprintf(errfile, "\n");
    fflush(errfile);
  }
  return 1;
}


// ---------------------------------------------------------------------------
//--------Utilities to build mappings for machine registers ------------------
// ---------------------------------------------------------------------------

// Construct the name of the register mask.
static const char *getRegMask(const char *reg_class_name) {
  if( reg_class_name == NULL ) return "RegMask::Empty";

  if (strcmp(reg_class_name,"Universe")==0) {
    return "RegMask::Empty";
  } else if (strcmp(reg_class_name,"stack_slots")==0) {
    return "(Compile::current()->FIRST_STACK_mask())";
  } else if (strcmp(reg_class_name, "dynamic")==0) {
    return "*_opnds[0]->in_RegMask(0)";
  } else {
    char       *rc_name = toUpper(reg_class_name);
    const char *mask    = "_mask";
    int         length  = (int)strlen(rc_name) + (int)strlen(mask) + 5;
    char       *regMask = new char[length];
    sprintf(regMask,"%s%s()", rc_name, mask);
    delete[] rc_name;
    return regMask;
  }
}

// Convert a register class name to its register mask.
const char *ArchDesc::reg_class_to_reg_mask(const char *rc_name) {
  const char *reg_mask = "RegMask::Empty";

  if( _register ) {
    RegClass *reg_class  = _register->getRegClass(rc_name);
    if (reg_class == NULL) {
      syntax_err(0, "Use of an undefined register class %s", rc_name);
      return reg_mask;
    }

    // Construct the name of the register mask.
    reg_mask = getRegMask(rc_name);
  }

  return reg_mask;
}


// Obtain the name of the RegMask for an OperandForm
const char *ArchDesc::reg_mask(OperandForm  &opForm) {
  const char *regMask      = "RegMask::Empty";

  // Check constraints on result's register class
  const char *result_class = opForm.constrained_reg_class();
  if (result_class == NULL) {
    opForm.dump();
    syntax_err(opForm._linenum,
               "Use of an undefined result class for operand: %s",
               opForm._ident);
    abort();
  }

  regMask = reg_class_to_reg_mask( result_class );

  return regMask;
}

// Obtain the name of the RegMask for an InstructForm
const char *ArchDesc::reg_mask(InstructForm &inForm) {
  const char *result = inForm.reduce_result();

  if (result == NULL) {
    syntax_err(inForm._linenum,
               "Did not find result operand or RegMask"
               " for this instruction: %s",
               inForm._ident);
    abort();
  }

  // Instructions producing 'Universe' use RegMask::Empty
  if (strcmp(result,"Universe") == 0) {
    return "RegMask::Empty";
  }

  // Lookup this result operand and get its register class
  Form *form = (Form*)_globalNames[result];
  if (form == NULL) {
    syntax_err(inForm._linenum,
               "Did not find result operand for result: %s", result);
    abort();
  }
  OperandForm *oper = form->is_operand();
  if (oper == NULL) {
    syntax_err(inForm._linenum, "Form is not an OperandForm:");
    form->dump();
    abort();
  }
  return reg_mask( *oper );
}


// Obtain the STACK_OR_reg_mask name for an OperandForm
char *ArchDesc::stack_or_reg_mask(OperandForm  &opForm) {
  // name of cisc_spillable version
  const char *reg_mask_name = reg_mask(opForm);

  if (reg_mask_name == NULL) {
     syntax_err(opForm._linenum,
                "Did not find reg_mask for opForm: %s",
                opForm._ident);
     abort();
  }

  const char *stack_or = "STACK_OR_";
  int   length         = (int)strlen(stack_or) + (int)strlen(reg_mask_name) + 1;
  char *result         = new char[length];
  sprintf(result,"%s%s", stack_or, reg_mask_name);

  return result;
}

// Record that the register class must generate a stack_or_reg_mask
void ArchDesc::set_stack_or_reg(const char *reg_class_name) {
  if( _register ) {
    RegClass *reg_class  = _register->getRegClass(reg_class_name);
    reg_class->set_stack_version(true);
  }
}


// Return the type signature for the ideal operation
const char *ArchDesc::getIdealType(const char *idealOp) {
  // Find last character in idealOp, it specifies the type
  char  last_char = 0;
  const char *ptr = idealOp;
  for (; *ptr != '\0'; ++ptr) {
    last_char = *ptr;
  }

  // Match Vector types.
  if (strncmp(idealOp, "Vec",3)==0) {
    switch(last_char) {
    case 'A':  return "TypeVect::VECTA";
    case 'S':  return "TypeVect::VECTS";
    case 'D':  return "TypeVect::VECTD";
    case 'X':  return "TypeVect::VECTX";
    case 'Y':  return "TypeVect::VECTY";
    case 'Z':  return "TypeVect::VECTZ";
    default:
      internal_err("Vector type %s with unrecognized type\n",idealOp);
    }
  }

  if (strncmp(idealOp, "RegVectMask", 8) == 0) {
    return "TypeVect::VECTMASK";
  }

  // !!!!!
  switch(last_char) {
  case 'I':    return "TypeInt::INT";
  case 'P':    return "TypePtr::BOTTOM";
  case 'N':    return "TypeNarrowOop::BOTTOM";
  case 'F':    return "Type::FLOAT";
  case 'D':    return "Type::DOUBLE";
  case 'L':    return "TypeLong::LONG";
  case 's':    return "TypeInt::CC /*flags*/";
  default:
    return NULL;
    // !!!!!
    // internal_err("Ideal type %s with unrecognized type\n",idealOp);
    break;
  }

  return NULL;
}



OperandForm *ArchDesc::constructOperand(const char *ident,
                                        bool  ideal_only) {
  OperandForm *opForm = new OperandForm(ident, ideal_only);
  _globalNames.Insert(ident, opForm);
  addForm(opForm);

  return opForm;
}


// Import predefined base types: Set = 1, RegI, RegP, ...
void ArchDesc::initBaseOpTypes() {
  // Create OperandForm and assign type for each opcode.
  for (int i = 1; i < _last_machine_leaf; ++i) {
    char *ident = (char *)NodeClassNames[i];
    constructOperand(ident, true);
  }
  // Create InstructForm and assign type for each ideal instruction.
  for (int j = _last_machine_leaf+1; j < _last_opcode; ++j) {
    char *ident = (char *)NodeClassNames[j];
    if (!strcmp(ident, "ConI") || !strcmp(ident, "ConP") ||
        !strcmp(ident, "ConN") || !strcmp(ident, "ConNKlass") ||
        !strcmp(ident, "ConF") || !strcmp(ident, "ConD") ||
        !strcmp(ident, "ConL") || !strcmp(ident, "Con" ) ||
        !strcmp(ident, "Bool")) {
      constructOperand(ident, true);
    } else {
      InstructForm *insForm = new InstructForm(ident, true);
      // insForm->_opcode = nextUserOpType(ident);
      _globalNames.Insert(ident, insForm);
      addForm(insForm);
    }
  }

  { OperandForm *opForm;
  // Create operand type "Universe" for return instructions.
  const char *ident = "Universe";
  opForm = constructOperand(ident, false);

  // Create operand type "label" for branch targets
  ident = "label";
  opForm = constructOperand(ident, false);

  // !!!!! Update - when adding a new sReg/stackSlot type
  // Create operand types "sReg[IPFDL]" for stack slot registers
  opForm = constructOperand("sRegI", false);
  opForm->_constraint = new Constraint("ALLOC_IN_RC", "stack_slots");
  opForm = constructOperand("sRegP", false);
  opForm->_constraint = new Constraint("ALLOC_IN_RC", "stack_slots");
  opForm = constructOperand("sRegF", false);
  opForm->_constraint = new Constraint("ALLOC_IN_RC", "stack_slots");
  opForm = constructOperand("sRegD", false);
  opForm->_constraint = new Constraint("ALLOC_IN_RC", "stack_slots");
  opForm = constructOperand("sRegL", false);
  opForm->_constraint = new Constraint("ALLOC_IN_RC", "stack_slots");

  // Create operand type "method" for call targets
  ident = "method";
  opForm = constructOperand(ident, false);
  }

  // Create Effect Forms for each of the legal effects
  // USE, DEF, USE_DEF, KILL, USE_KILL
  {
    const char *ident = "USE";
    Effect     *eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "DEF";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "USE_DEF";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "KILL";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "USE_KILL";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "TEMP";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "TEMP_DEF";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
    ident = "CALL";
    eForm = new Effect(ident);
    _globalNames.Insert(ident, eForm);
  }

  //
  // Build mapping from ideal names to ideal indices
  int idealIndex = 0;
  for (idealIndex = 1; idealIndex < _last_machine_leaf; ++idealIndex) {
    const char *idealName = NodeClassNames[idealIndex];
    _idealIndex.Insert((void*) idealName, (void*) (intptr_t) idealIndex);
  }
  for (idealIndex = _last_machine_leaf+1;
       idealIndex < _last_opcode; ++idealIndex) {
    const char *idealName = NodeClassNames[idealIndex];
    _idealIndex.Insert((void*) idealName, (void*) (intptr_t) idealIndex);
  }

}


//---------------------------addSUNcopyright-------------------------------
// output SUN copyright info
void ArchDesc::addSunCopyright(char* legal, int size, FILE *fp) {
  size_t count = fwrite(legal, 1, size, fp);
  assert(count == (size_t) size, "copyright info truncated");
  fprintf(fp,"\n");
  fprintf(fp,"// Machine Generated File.  Do Not Edit!\n");
  fprintf(fp,"\n");
}


//---------------------------addIncludeGuardStart--------------------------
// output the start of an include guard.
void ArchDesc::addIncludeGuardStart(ADLFILE &adlfile, const char* guardString) {
  // Build #include lines
  fprintf(adlfile._fp, "\n");
  fprintf(adlfile._fp, "#ifndef %s\n", guardString);
  fprintf(adlfile._fp, "#define %s\n", guardString);
  fprintf(adlfile._fp, "\n");

}

//---------------------------addIncludeGuardEnd--------------------------
// output the end of an include guard.
void ArchDesc::addIncludeGuardEnd(ADLFILE &adlfile, const char* guardString) {
  // Build #include lines
  fprintf(adlfile._fp, "\n");
  fprintf(adlfile._fp, "#endif // %s\n", guardString);

}

//---------------------------addInclude--------------------------
// output the #include line for this file.
void ArchDesc::addInclude(ADLFILE &adlfile, const char* fileName) {
  fprintf(adlfile._fp, "#include \"%s\"\n", fileName);

}

void ArchDesc::addInclude(ADLFILE &adlfile, const char* includeDir, const char* fileName) {
  fprintf(adlfile._fp, "#include \"%s/%s\"\n", includeDir, fileName);

}

//---------------------------addPreprocessorChecks-----------------------------
// Output C preprocessor code to verify the backend compilation environment.
// The idea is to force code produced by "adlc -DHS64" to be compiled by a
// command of the form "CC ... -DHS64 ...", so that any #ifdefs in the source
// blocks select C code that is consistent with adlc's selections of AD code.
void ArchDesc::addPreprocessorChecks(FILE *fp) {
  const char* flag;
  _preproc_list.reset();
  if (_preproc_list.count() > 0 && !_preproc_list.current_is_signal()) {
    fprintf(fp, "// Check consistency of C++ compilation with ADLC options:\n");
  }
  for (_preproc_list.reset(); (flag = _preproc_list.iter()) != NULL; ) {
    if (_preproc_list.current_is_signal())  break;
    char* def = get_preproc_def(flag);
    fprintf(fp, "// Check adlc ");
    if (def)
          fprintf(fp, "-D%s=%s\n", flag, def);
    else  fprintf(fp, "-U%s\n", flag);
    fprintf(fp, "#%s %s\n",
            def ? "ifndef" : "ifdef", flag);
    fprintf(fp, "#  error \"%s %s be defined\"\n",
            flag, def ? "must" : "must not");
    fprintf(fp, "#endif // %s\n", flag);
  }
}


// Convert operand name into enum name
const char *ArchDesc::machOperEnum(const char *opName) {
  return ArchDesc::getMachOperEnum(opName);
}

// Convert operand name into enum name
const char *ArchDesc::getMachOperEnum(const char *opName) {
  return (opName ? toUpper(opName) : opName);
}

//---------------------------buildMustCloneMap-----------------------------
// Flag cases when machine needs cloned values or instructions
void ArchDesc::buildMustCloneMap(FILE *fp_hpp, FILE *fp_cpp) {
  // Build external declarations for mappings
  fprintf(fp_hpp, "// Mapping from machine-independent opcode to boolean\n");
  fprintf(fp_hpp, "// Flag cases where machine needs cloned values or instructions\n");
  fprintf(fp_hpp, "extern const char must_clone[];\n");
  fprintf(fp_hpp, "\n");

  // Build mapping from ideal names to ideal indices
  fprintf(fp_cpp, "\n");
  fprintf(fp_cpp, "// Mapping from machine-independent opcode to boolean\n");
  fprintf(fp_cpp, "const        char must_clone[] = {\n");
  for (int idealIndex = 0; idealIndex < _last_opcode; ++idealIndex) {
    int         must_clone = 0;
    const char *idealName = NodeClassNames[idealIndex];
    // Previously selected constants for cloning
    // !!!!!
    // These are the current machine-dependent clones
    if ( strcmp(idealName,"CmpI") == 0
         || strcmp(idealName,"CmpU") == 0
         || strcmp(idealName,"CmpP") == 0
         || strcmp(idealName,"CmpN") == 0
         || strcmp(idealName,"CmpL") == 0
         || strcmp(idealName,"CmpUL") == 0
         || strcmp(idealName,"CmpD") == 0
         || strcmp(idealName,"CmpF") == 0
         || strcmp(idealName,"FastLock") == 0
         || strcmp(idealName,"FastUnlock") == 0
         || strcmp(idealName,"OverflowAddI") == 0
         || strcmp(idealName,"OverflowAddL") == 0
         || strcmp(idealName,"OverflowSubI") == 0
         || strcmp(idealName,"OverflowSubL") == 0
         || strcmp(idealName,"OverflowMulI") == 0
         || strcmp(idealName,"OverflowMulL") == 0
         || strcmp(idealName,"Bool") == 0
         || strcmp(idealName,"Binary") == 0 ) {
      // Removed ConI from the must_clone list.  CPUs that cannot use
      // large constants as immediates manifest the constant as an
      // instruction.  The must_clone flag prevents the constant from
      // floating up out of loops.
      must_clone = 1;
    }
    fprintf(fp_cpp, "  %d%s // %s: %d\n", must_clone,
      (idealIndex != (_last_opcode - 1)) ? "," : " // no trailing comma",
      idealName, idealIndex);
  }
  // Finish defining table
  fprintf(fp_cpp, "};\n");
}
