/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

// DFA.CPP - Method definitions for outputting the matcher DFA from ADLC
#include "adlc.hpp"

//---------------------------Switches for debugging output---------------------
static bool debug_output   = false;
static bool debug_output1  = false;    // top level chain rules

//---------------------------Production State----------------------------------
static const char *knownInvalid = "knownInvalid";    // The result does NOT have a rule defined
static const char *knownValid   = "knownValid";      // The result must be produced by a rule
static const char *unknownValid = "unknownValid";    // Unknown (probably due to a child or predicate constraint)

static const char *noConstraint  = "noConstraint";   // No constraints seen so far
static const char *hasConstraint = "hasConstraint";  // Within the first constraint


//------------------------------Production------------------------------------
// Track the status of productions for a particular result
class Production {
public:
  const char *_result;
  const char *_constraint;
  const char *_valid;
  Expr       *_cost_lb;            // Cost lower bound for this production
  Expr       *_cost_ub;            // Cost upper bound for this production

public:
  Production(const char *result, const char *constraint, const char *valid);
  ~Production() {};

  void        initialize();        // reset to be an empty container

  const char   *valid()  const { return _valid; }
  Expr       *cost_lb()  const { return (Expr *)_cost_lb;  }
  Expr       *cost_ub()  const { return (Expr *)_cost_ub;  }

  void print();
};


//------------------------------ProductionState--------------------------------
// Track the status of all production rule results
// Reset for each root opcode (e.g., Op_RegI, Op_AddI, ...)
class ProductionState {
private:
  Dict _production;    // map result of production, char*, to information or NULL
  const char *_constraint;

public:
  // cmpstr does string comparisions.  hashstr computes a key.
  ProductionState(Arena *arena) : _production(cmpstr, hashstr, arena) { initialize(); };
  ~ProductionState() { };

  void        initialize();                // reset local and dictionary state

  const char *constraint();
  void    set_constraint(const char *constraint); // currently working inside of constraints

  const char *valid(const char *result);   // unknownValid, or status for this production
  void    set_valid(const char *result);   // if not constrained, set status to knownValid

  Expr           *cost_lb(const char *result);
  Expr           *cost_ub(const char *result);
  void    set_cost_bounds(const char *result, const Expr *cost, bool has_state_check, bool has_cost_check);

  // Return the Production associated with the result,
  // or create a new Production and insert it into the dictionary.
  Production *getProduction(const char *result);

  void print();

private:
    // Disable public use of constructor, copy-ctor,  ...
  ProductionState( )                         : _production(cmpstr, hashstr, Form::arena) {  assert( false, "NotImplemented");  };
  ProductionState( const ProductionState & ) : _production(cmpstr, hashstr, Form::arena) {  assert( false, "NotImplemented");  }; // Deep-copy
};


//---------------------------Helper Functions----------------------------------
// cost_check template:
// 1)      if (STATE__NOT_YET_VALID(EBXREGI) || _cost[EBXREGI] > c) {
// 2)        DFA_PRODUCTION(EBXREGI, cmovI_memu_rule, c)
// 3)      }
//
static void cost_check(FILE *fp, const char *spaces,
                       const char *arrayIdx, const Expr *cost, const char *rule, ProductionState &status) {
  bool state_check               = false;  // true if this production needs to check validity
  bool cost_check                = false;  // true if this production needs to check cost
  bool cost_is_above_upper_bound = false;  // true if this production is unnecessary due to high cost
  bool cost_is_below_lower_bound = false;  // true if this production replaces a higher cost production

  // Get information about this production
  const Expr *previous_ub = status.cost_ub(arrayIdx);
  if( !previous_ub->is_unknown() ) {
    if( previous_ub->less_than_or_equal(cost) ) {
      cost_is_above_upper_bound = true;
      if( debug_output ) { fprintf(fp, "// Previous rule with lower cost than: %s === %s_rule costs %s\n", arrayIdx, rule, cost->as_string()); }
    }
  }

  const Expr *previous_lb = status.cost_lb(arrayIdx);
  if( !previous_lb->is_unknown() ) {
    if( cost->less_than_or_equal(previous_lb) ) {
      cost_is_below_lower_bound = true;
      if( debug_output ) { fprintf(fp, "// Previous rule with higher cost\n"); }
    }
  }

  // line 1)
  // Check for validity and compare to other match costs
  const char *validity_check = status.valid(arrayIdx);
  if( validity_check == unknownValid ) {
    fprintf(fp, "%sif (STATE__NOT_YET_VALID(%s) || _cost[%s] > %s) {\n",  spaces, arrayIdx, arrayIdx, cost->as_string());
    state_check = true;
    cost_check  = true;
  }
  else if( validity_check == knownInvalid ) {
    if( debug_output ) { fprintf(fp, "%s// %s KNOWN_INVALID \n",  spaces, arrayIdx); }
  }
  else if( validity_check == knownValid ) {
    if( cost_is_above_upper_bound ) {
      // production cost is known to be too high.
      return;
    } else if( cost_is_below_lower_bound ) {
      // production will unconditionally overwrite a previous production that had higher cost
    } else {
      fprintf(fp, "%sif ( /* %s KNOWN_VALID || */ _cost[%s] > %s) {\n",  spaces, arrayIdx, arrayIdx, cost->as_string());
      cost_check  = true;
    }
  }

  // line 2)
  fprintf(fp, "%s  DFA_PRODUCTION(%s, %s_rule, %s)", spaces, arrayIdx, rule, cost->as_string() );
  if (validity_check == knownValid) {
    if (cost_is_below_lower_bound) {
      fprintf(fp, "\t  // overwrites higher cost rule");
    }
  }
  fprintf(fp, "\n");

  // line 3)
  if( cost_check || state_check ) {
    fprintf(fp, "%s}\n", spaces);
  }

  status.set_cost_bounds(arrayIdx, cost, state_check, cost_check);

  // Update ProductionState
  if( validity_check != knownValid ) {
    // set State vector if not previously known
    status.set_valid(arrayIdx);
  }
}


//---------------------------child_test----------------------------------------
// Example:
//   STATE__VALID_CHILD(_kids[0], FOO) &&  STATE__VALID_CHILD(_kids[1], BAR)
// Macro equivalent to: _kids[0]->valid(FOO) && _kids[1]->valid(BAR)
//
static void child_test(FILE *fp, MatchList &mList) {
  if (mList._lchild) { // If left child, check it
    const char* lchild_to_upper = ArchDesc::getMachOperEnum(mList._lchild);
    fprintf(fp, "STATE__VALID_CHILD(_kids[0], %s)", lchild_to_upper);
    delete[] lchild_to_upper;
  }
  if (mList._lchild && mList._rchild) { // If both, add the "&&"
    fprintf(fp, " && ");
  }
  if (mList._rchild) { // If right child, check it
    const char* rchild_to_upper = ArchDesc::getMachOperEnum(mList._rchild);
    fprintf(fp, "STATE__VALID_CHILD(_kids[1], %s)", rchild_to_upper);
    delete[] rchild_to_upper;
  }
}

//---------------------------calc_cost-----------------------------------------
// Example:
//           unsigned int c = _kids[0]->_cost[FOO] + _kids[1]->_cost[BAR] + 5;
//
Expr *ArchDesc::calc_cost(FILE *fp, const char *spaces, MatchList &mList, ProductionState &status) {
  fprintf(fp, "%sunsigned int c = ", spaces);
  Expr *c = new Expr("0");
  if (mList._lchild) { // If left child, add it in
    const char* lchild_to_upper = ArchDesc::getMachOperEnum(mList._lchild);
    sprintf(Expr::buffer(), "_kids[0]->_cost[%s]", lchild_to_upper);
    c->add(Expr::buffer());
    delete[] lchild_to_upper;
}
  if (mList._rchild) { // If right child, add it in
    const char* rchild_to_upper = ArchDesc::getMachOperEnum(mList._rchild);
    sprintf(Expr::buffer(), "_kids[1]->_cost[%s]", rchild_to_upper);
    c->add(Expr::buffer());
    delete[] rchild_to_upper;
  }
  // Add in cost of this rule
  const char *mList_cost = mList.get_cost();
  c->add(mList_cost, *this);

  fprintf(fp, "%s;\n", c->as_string());
  c->set_external_name("c");
  return c;
}


//---------------------------gen_match-----------------------------------------
void ArchDesc::gen_match(FILE *fp, MatchList &mList, ProductionState &status, Dict &operands_chained_from) {
  const char *spaces4 = "    ";
  const char *spaces6 = "      ";

  fprintf(fp, "%s", spaces4);
  // Only generate child tests if this is not a leaf node
  bool has_child_constraints = mList._lchild || mList._rchild;
  const char *predicate_test = mList.get_pred();
  if (has_child_constraints || predicate_test) {
    // Open the child-and-predicate-test braces
    fprintf(fp, "if( ");
    status.set_constraint(hasConstraint);
    child_test(fp, mList);
    // Only generate predicate test if one exists for this match
    if (predicate_test) {
      if (has_child_constraints) {
        fprintf(fp," &&\n");
      }
      fprintf(fp, "%s  %s", spaces6, predicate_test);
    }
    // End of outer tests
    fprintf(fp," ) ");
  } else {
    // No child or predicate test needed
    status.set_constraint(noConstraint);
  }

  // End of outer tests
  fprintf(fp,"{\n");

  // Calculate cost of this match
  const Expr *cost = calc_cost(fp, spaces6, mList, status);
  // Check against other match costs, and update cost & rule vectors
  cost_check(fp, spaces6, ArchDesc::getMachOperEnum(mList._resultStr), cost, mList._opcode, status);

  // If this is a member of an operand class, update the class cost & rule
  expand_opclass( fp, spaces6, cost, mList._resultStr, status);

  // Check if this rule should be used to generate the chains as well.
  const char *rule = /* set rule to "Invalid" for internal operands */
    strcmp(mList._opcode, mList._resultStr) ? mList._opcode : "Invalid";

  // If this rule produces an operand which has associated chain rules,
  // update the operands with the chain rule + this rule cost & this rule.
  chain_rule(fp, spaces6, mList._resultStr, cost, rule, operands_chained_from, status);

  // Close the child-and-predicate-test braces
  fprintf(fp, "    }\n");

}


//---------------------------expand_opclass------------------------------------
// Chain from one result_type to all other members of its operand class
void ArchDesc::expand_opclass(FILE *fp, const char *indent, const Expr *cost,
                              const char *result_type, ProductionState &status) {
  const Form *form = _globalNames[result_type];
  OperandForm *op = form ? form->is_operand() : NULL;
  if( op && op->_classes.count() > 0 ) {
    if( debug_output ) { fprintf(fp, "// expand operand classes for operand: %s \n", (char *)op->_ident  ); } // %%%%% Explanation
    // Iterate through all operand classes which include this operand
    op->_classes.reset();
    const char *oclass;
    // Expr *cCost = new Expr(cost);
    while( (oclass = op->_classes.iter()) != NULL )
      // Check against other match costs, and update cost & rule vectors
      cost_check(fp, indent, ArchDesc::getMachOperEnum(oclass), cost, result_type, status);
  }
}

//---------------------------chain_rule----------------------------------------
// Starting at 'operand', check if we know how to automatically generate other results
void ArchDesc::chain_rule(FILE *fp, const char *indent, const char *operand,
     const Expr *icost, const char *irule, Dict &operands_chained_from,  ProductionState &status) {

  // Check if we have already generated chains from this starting point
  if( operands_chained_from[operand] != NULL ) {
    return;
  } else {
    operands_chained_from.Insert( operand, operand);
  }
  if( debug_output ) { fprintf(fp, "// chain rules starting from: %s  and  %s \n", (char *)operand, (char *)irule); } // %%%%% Explanation

  ChainList *lst = (ChainList *)_chainRules[operand];
  if (lst) {
    // printf("\nChain from <%s> at cost #%s\n",operand, icost ? icost : "_");
    const char *result, *cost, *rule;
    for(lst->reset(); (lst->iter(result,cost,rule)) == true; ) {
      // Do not generate operands that are already available
      if( operands_chained_from[result] != NULL ) {
        continue;
      } else {
        // Compute the cost for previous match + chain_rule_cost
        // total_cost = icost + cost;
        Expr *total_cost = icost->clone();  // icost + cost
        total_cost->add(cost, *this);

        // Check for transitive chain rules
        Form *form = (Form *)_globalNames[rule];
        if ( ! form->is_instruction()) {
          // printf("   result=%s cost=%s rule=%s\n", result, total_cost, rule);
          // Check against other match costs, and update cost & rule vectors
          const char *reduce_rule = strcmp(irule,"Invalid") ? irule : rule;
          cost_check(fp, indent, ArchDesc::getMachOperEnum(result), total_cost, reduce_rule, status);
          chain_rule(fp, indent, result, total_cost, irule, operands_chained_from, status);
        } else {
          // printf("   result=%s cost=%s rule=%s\n", result, total_cost, rule);
          // Check against other match costs, and update cost & rule vectors
          cost_check(fp, indent, ArchDesc::getMachOperEnum(result), total_cost, rule, status);
          chain_rule(fp, indent, result, total_cost, rule, operands_chained_from, status);
        }

        // If this is a member of an operand class, update class cost & rule
        expand_opclass( fp, indent, total_cost, result, status );
      }
    }
  }
}

//---------------------------prune_matchlist-----------------------------------
// Check for duplicate entries in a matchlist, and prune out the higher cost
// entry.
void ArchDesc::prune_matchlist(Dict &minimize, MatchList &mlist) {

}

//---------------------------buildDFA------------------------------------------
// DFA is a large switch with case statements for each ideal opcode encountered
// in any match rule in the ad file.  Each case has a series of if's to handle
// the match or fail decisions.  The matches test the cost function of that
// rule, and prune any cases which are higher cost for the same reduction.
// In order to generate the DFA we walk the table of ideal opcode/MatchList
// pairs generated by the ADLC front end to build the contents of the case
// statements (a series of if statements).
void ArchDesc::buildDFA(FILE* fp) {
  int i;
  // Remember operands that are the starting points for chain rules.
  // Prevent cycles by checking if we have already generated chain.
  Dict operands_chained_from(cmpstr, hashstr, Form::arena);

  // Hash inputs to match rules so that final DFA contains only one entry for
  // each match pattern which is the low cost entry.
  Dict minimize(cmpstr, hashstr, Form::arena);

  // Track status of dfa for each resulting production
  // reset for each ideal root.
  ProductionState status(Form::arena);

  // Output the start of the DFA method into the output file

  fprintf(fp, "\n");
  fprintf(fp, "//------------------------- Source -----------------------------------------\n");
  // Do not put random source code into the DFA.
  // If there are constants which need sharing, put them in "source_hpp" forms.
  // _source.output(fp);
  fprintf(fp, "\n");
  fprintf(fp, "//------------------------- Attributes -------------------------------------\n");
  _attributes.output(fp);
  fprintf(fp, "\n");
  fprintf(fp, "//------------------------- Macros -----------------------------------------\n");
  fprintf(fp, "#define DFA_PRODUCTION(result, rule, cost)\\\n");
  fprintf(fp, "  assert(rule < (1 << 15), \"too many rules\"); _cost[ (result) ] = cost; _rule[ (result) ] = (rule << 1) | 0x1;\n");
  fprintf(fp, "\n");

  fprintf(fp, "//------------------------- DFA --------------------------------------------\n");

  fprintf(fp,
"// DFA is a large switch with case statements for each ideal opcode encountered\n"
"// in any match rule in the ad file.  Each case has a series of if's to handle\n"
"// the match or fail decisions.  The matches test the cost function of that\n"
"// rule, and prune any cases which are higher cost for the same reduction.\n"
"// In order to generate the DFA we walk the table of ideal opcode/MatchList\n"
"// pairs generated by the ADLC front end to build the contents of the case\n"
"// statements (a series of if statements).\n"
);
  fprintf(fp, "\n");
  fprintf(fp, "\n");
  if (_dfa_small) {
    // Now build the individual routines just like the switch entries in large version
    // Iterate over the table of MatchLists, start at first valid opcode of 1
    for (i = 1; i < _last_opcode; i++) {
      if (_mlistab[i] == NULL) continue;
      // Generate the routine header statement for this opcode
      fprintf(fp, "void  State::_sub_Op_%s(const Node *n){\n", NodeClassNames[i]);
      // Generate body. Shared for both inline and out-of-line version
      gen_dfa_state_body(fp, minimize, status, operands_chained_from, i);
      // End of routine
      fprintf(fp, "}\n");
    }
  }
  fprintf(fp, "bool State::DFA");
  fprintf(fp, "(int opcode, const Node *n) {\n");
  fprintf(fp, "  switch(opcode) {\n");

  // Iterate over the table of MatchLists, start at first valid opcode of 1
  for (i = 1; i < _last_opcode; i++) {
    if (_mlistab[i] == NULL) continue;
    // Generate the case statement for this opcode
    if (_dfa_small) {
      fprintf(fp, "  case Op_%s: { _sub_Op_%s(n);\n", NodeClassNames[i], NodeClassNames[i]);
    } else {
      fprintf(fp, "  case Op_%s: {\n", NodeClassNames[i]);
      // Walk the list, compacting it
      gen_dfa_state_body(fp, minimize, status, operands_chained_from, i);
    }
    // Print the "break"
    fprintf(fp, "    break;\n");
    fprintf(fp, "  }\n");
  }

  // Generate the default case for switch(opcode)
  fprintf(fp, "  \n");
  fprintf(fp, "  default:\n");
  fprintf(fp, "    tty->print(\"Default case invoked for: \\n\");\n");
  fprintf(fp, "    tty->print(\"   opcode  = %cd, \\\"%cs\\\"\\n\", opcode, NodeClassNames[opcode]);\n", '%', '%');
  fprintf(fp, "    return false;\n");
  fprintf(fp, "  }\n");

  // Return status, indicating a successful match.
  fprintf(fp, "  return true;\n");
  // Generate the closing brace for method Matcher::DFA
  fprintf(fp, "}\n");
  Expr::check_buffers();
}


class dfa_shared_preds {
  enum { count = 3 IA32_ONLY( + 1 ) };

  static bool        _found[count];
  static const char* _type [count];
  static const char* _var  [count];
  static const char* _pred [count];

  static void check_index(int index) { assert( 0 <= index && index < count, "Invalid index"); }

  // Confirm that this is a separate sub-expression.
  // Only need to catch common cases like " ... && shared ..."
  // and avoid hazardous ones like "...->shared"
  static bool valid_loc(char *pred, char *shared) {
    // start of predicate is valid
    if( shared == pred ) return true;

    // Check previous character and recurse if needed
    char *prev = shared - 1;
    char c  = *prev;
    switch( c ) {
    case ' ':
    case '\n':
      return dfa_shared_preds::valid_loc(pred, prev);
    case '!':
    case '(':
    case '<':
    case '=':
      return true;
    case '"':  // such as: #line 10 "myfile.ad"\n mypredicate
      return true;
    case '|':
      if (prev != pred && *(prev-1) == '|') return true;
      break;
    case '&':
      if (prev != pred && *(prev-1) == '&') return true;
      break;
    default:
      return false;
    }

    return false;
  }

public:

  static bool        found(int index){ check_index(index); return _found[index]; }
  static void    set_found(int index, bool val) { check_index(index); _found[index] = val; }
  static void  reset_found() {
    for( int i = 0; i < count; ++i ) { _found[i] = false; }
  };

  static const char* type(int index) { check_index(index); return _type[index]; }
  static const char* var (int index) { check_index(index); return _var [index];  }
  static const char* pred(int index) { check_index(index); return _pred[index]; }

  // Check each predicate in the MatchList for common sub-expressions
  static void cse_matchlist(MatchList *matchList) {
    for( MatchList *mList = matchList; mList != NULL; mList = mList->get_next() ) {
      Predicate* predicate = mList->get_pred_obj();
      char*      pred      = mList->get_pred();
      if( pred != NULL ) {
        for(int index = 0; index < count; ++index ) {
          const char *shared_pred      = dfa_shared_preds::pred(index);
          const char *shared_pred_var  = dfa_shared_preds::var(index);
          bool result = dfa_shared_preds::cse_predicate(predicate, shared_pred, shared_pred_var);
          if( result ) dfa_shared_preds::set_found(index, true);
        }
      }
    }
  }

  // If the Predicate contains a common sub-expression, replace the Predicate's
  // string with one that uses the variable name.
  static bool cse_predicate(Predicate* predicate, const char *shared_pred, const char *shared_pred_var) {
    bool result = false;
    char *pred = predicate->_pred;
    if( pred != NULL ) {
      char *new_pred = pred;
      for( char *shared_pred_loc = strstr(new_pred, shared_pred);
      shared_pred_loc != NULL && dfa_shared_preds::valid_loc(new_pred,shared_pred_loc);
      shared_pred_loc = strstr(new_pred, shared_pred) ) {
        // Do not modify the original predicate string, it is shared
        if( new_pred == pred ) {
          new_pred = strdup(pred);
          shared_pred_loc = strstr(new_pred, shared_pred);
        }
        // Replace shared_pred with variable name
        strncpy(shared_pred_loc, shared_pred_var, strlen(shared_pred_var));
      }
      // Install new predicate
      if( new_pred != pred ) {
        predicate->_pred = new_pred;
        result = true;
      }
    }
    return result;
  }

  // Output the hoisted common sub-expression if we found it in predicates
  static void generate_cse(FILE *fp) {
    for(int j = 0; j < count; ++j ) {
      if( dfa_shared_preds::found(j) ) {
        const char *shared_pred_type = dfa_shared_preds::type(j);
        const char *shared_pred_var  = dfa_shared_preds::var(j);
        const char *shared_pred      = dfa_shared_preds::pred(j);
        fprintf(fp, "    %s %s = %s;\n", shared_pred_type, shared_pred_var, shared_pred);
      }
    }
  }
};
// shared predicates, _var and _pred entry should be the same length
bool         dfa_shared_preds::_found[dfa_shared_preds::count] = { false,          false,           false               IA32_ONLY(COMMA false)  };
const char*  dfa_shared_preds::_type [dfa_shared_preds::count] = { "int",          "jlong",         "intptr_t"          IA32_ONLY(COMMA "bool") };
const char*  dfa_shared_preds::_var  [dfa_shared_preds::count] = { "_n_get_int__", "_n_get_long__", "_n_get_intptr_t__" IA32_ONLY(COMMA "Compile__current____select_24_bit_instr__") };
const char*  dfa_shared_preds::_pred [dfa_shared_preds::count] = { "n->get_int()", "n->get_long()", "n->get_intptr_t()" IA32_ONLY(COMMA "Compile::current()->select_24_bit_instr()") };

void ArchDesc::gen_dfa_state_body(FILE* fp, Dict &minimize, ProductionState &status, Dict &operands_chained_from, int i) {
  // Start the body of each Op_XXX sub-dfa with a clean state.
  status.initialize();

  // Walk the list, compacting it
  MatchList* mList = _mlistab[i];
  do {
    // Hash each entry using inputs as key and pointer as data.
    // If there is already an entry, keep the one with lower cost, and
    // remove the other one from the list.
    prune_matchlist(minimize, *mList);
    // Iterate
    mList = mList->get_next();
  } while(mList != NULL);

  // Hoist previously specified common sub-expressions out of predicates
  dfa_shared_preds::reset_found();
  dfa_shared_preds::cse_matchlist(_mlistab[i]);
  dfa_shared_preds::generate_cse(fp);

  mList = _mlistab[i];

  // Walk the list again, generating code
  do {
    // Each match can generate its own chains
    operands_chained_from.Clear();
    gen_match(fp, *mList, status, operands_chained_from);
    mList = mList->get_next();
  } while(mList != NULL);
  // Fill in any chain rules which add instructions
  // These can generate their own chains as well.
  operands_chained_from.Clear();  //
  if( debug_output1 ) { fprintf(fp, "// top level chain rules for: %s \n", (char *)NodeClassNames[i]); } // %%%%% Explanation
  const Expr *zeroCost = new Expr("0");
  chain_rule(fp, "   ", (char *)NodeClassNames[i], zeroCost, "Invalid",
             operands_chained_from, status);
}



//------------------------------Expr------------------------------------------
Expr *Expr::_unknown_expr = NULL;
char  Expr::string_buffer[STRING_BUFFER_LENGTH];
char  Expr::external_buffer[STRING_BUFFER_LENGTH];
bool  Expr::_init_buffers = Expr::init_buffers();

Expr::Expr() {
  _external_name = NULL;
  _expr          = "Invalid_Expr";
  _min_value     = Expr::Max;
  _max_value     = Expr::Zero;
}
Expr::Expr(const char *cost) {
  _external_name = NULL;

  int intval = 0;
  if( cost == NULL ) {
    _expr = "0";
    _min_value = Expr::Zero;
    _max_value = Expr::Zero;
  }
  else if( ADLParser::is_int_token(cost, intval) ) {
    _expr = cost;
    _min_value = intval;
    _max_value = intval;
  }
  else {
    assert( strcmp(cost,"0") != 0, "Recognize string zero as an int");
    _expr = cost;
    _min_value = Expr::Zero;
    _max_value = Expr::Max;
  }
}

Expr::Expr(const char *name, const char *expression, int min_value, int max_value) {
  _external_name = name;
  _expr          = expression ? expression : name;
  _min_value     = min_value;
  _max_value     = max_value;
  assert(_min_value >= 0 && _min_value <= Expr::Max, "value out of range");
  assert(_max_value >= 0 && _max_value <= Expr::Max, "value out of range");
}

Expr *Expr::clone() const {
  Expr *cost = new Expr();
  cost->_external_name = _external_name;
  cost->_expr          = _expr;
  cost->_min_value     = _min_value;
  cost->_max_value     = _max_value;

  return cost;
}

void Expr::add(const Expr *c) {
  // Do not update fields until all computation is complete
  const char *external  = compute_external(this, c);
  const char *expr      = compute_expr(this, c);
  int         min_value = compute_min (this, c);
  int         max_value = compute_max (this, c);

  _external_name = external;
  _expr      = expr;
  _min_value = min_value;
  _max_value = max_value;
}

void Expr::add(const char *c) {
  Expr *cost = new Expr(c);
  add(cost);
}

void Expr::add(const char *c, ArchDesc &AD) {
  const Expr *e = AD.globalDefs()[c];
  if( e != NULL ) {
    // use the value of 'c' defined in <arch>.ad
    add(e);
  } else {
    Expr *cost = new Expr(c);
    add(cost);
  }
}

const char *Expr::compute_external(const Expr *c1, const Expr *c2) {
  const char * result = NULL;

  // Preserve use of external name which has a zero value
  if( c1->_external_name != NULL ) {
    if( c2->is_zero() ) {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s", c1->as_string());
    } else {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s+%s", c1->as_string(), c2->as_string());
    }
    string_buffer[STRING_BUFFER_LENGTH - 1] = '\0';
    result = strdup(string_buffer);
  }
  else if( c2->_external_name != NULL ) {
    if( c1->is_zero() ) {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s", c2->_external_name);
    } else {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s + %s", c1->as_string(), c2->as_string());
    }
    string_buffer[STRING_BUFFER_LENGTH - 1] = '\0';
    result = strdup(string_buffer);
  }
  return result;
}

const char *Expr::compute_expr(const Expr *c1, const Expr *c2) {
  if( !c1->is_zero() ) {
    if( c2->is_zero() ) {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s", c1->_expr);
    } else {
      snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s+%s", c1->_expr, c2->_expr);
    }
  }
  else if( !c2->is_zero() ) {
    snprintf(string_buffer, STRING_BUFFER_LENGTH, "%s", c2->_expr);
  }
  else {
    sprintf( string_buffer, "0");
  }
  string_buffer[STRING_BUFFER_LENGTH - 1] = '\0';
  char *cost = strdup(string_buffer);

  return cost;
}

int Expr::compute_min(const Expr *c1, const Expr *c2) {
  int v1 = c1->_min_value;
  int v2 = c2->_min_value;
  assert(0 <= v2 && v2 <= Expr::Max, "sanity");
  assert(v1 <= Expr::Max - v2, "Invalid cost computation");

  return v1 + v2;
}


int Expr::compute_max(const Expr *c1, const Expr *c2) {
  int v1 = c1->_max_value;
  int v2 = c2->_max_value;

  // Check for overflow without producing UB. If v2 is positive
  // and not larger than Max, the subtraction cannot underflow.
  assert(0 <= v2 && v2 <= Expr::Max, "sanity");
  if (v1 > Expr::Max - v2) {
    return Expr::Max;
  }

  return v1 + v2;
}

void Expr::print() const {
  if( _external_name != NULL ) {
    printf("  %s == (%s) === [%d, %d]\n", _external_name, _expr, _min_value, _max_value);
  } else {
    printf("  %s === [%d, %d]\n", _expr, _min_value, _max_value);
  }
}

void Expr::print_define(FILE *fp) const {
  assert( _external_name != NULL, "definition does not have a name");
  assert( _min_value == _max_value, "Expect user definitions to have constant value");
  fprintf(fp, "#define  %s  (%s)  \n", _external_name, _expr);
  fprintf(fp, "// value == %d \n", _min_value);
}

void Expr::print_assert(FILE *fp) const {
  assert( _external_name != NULL, "definition does not have a name");
  assert( _min_value == _max_value, "Expect user definitions to have constant value");
  fprintf(fp, "  assert( %s == %d, \"Expect (%s) to equal %d\");\n", _external_name, _min_value, _expr, _min_value);
}

Expr *Expr::get_unknown() {
  if( Expr::_unknown_expr == NULL ) {
    Expr::_unknown_expr = new Expr();
  }

  return Expr::_unknown_expr;
}

bool Expr::init_buffers() {
  // Fill buffers with 0
  for( int i = 0; i < STRING_BUFFER_LENGTH; ++i ) {
    external_buffer[i] = '\0';
    string_buffer[i]   = '\0';
  }

  return true;
}

bool Expr::check_buffers() {
  // returns 'true' if buffer use may have overflowed
  bool ok = true;
  for( int i = STRING_BUFFER_LENGTH - 100; i < STRING_BUFFER_LENGTH; ++i) {
    if( external_buffer[i] != '\0' || string_buffer[i]   != '\0' ) {
      ok = false;
      assert( false, "Expr:: Buffer overflow");
    }
  }

  return ok;
}


//------------------------------ExprDict---------------------------------------
// Constructor
ExprDict::ExprDict( CmpKey cmp, Hash hash, Arena *arena )
  : _expr(cmp, hash, arena), _defines()  {
}
ExprDict::~ExprDict() {
}

// Return # of name-Expr pairs in dict
int ExprDict::Size(void) const {
  return _expr.Size();
}

// define inserts the given key-value pair into the dictionary,
// and records the name in order for later output, ...
const Expr  *ExprDict::define(const char *name, Expr *expr) {
  const Expr *old_expr = (*this)[name];
  assert(old_expr == NULL, "Implementation does not support redefinition");

  _expr.Insert(name, expr);
  _defines.addName(name);

  return old_expr;
}

// Insert inserts the given key-value pair into the dictionary.  The prior
// value of the key is returned; NULL if the key was not previously defined.
const Expr  *ExprDict::Insert(const char *name, Expr *expr) {
  return (Expr*)_expr.Insert((void*)name, (void*)expr);
}

// Finds the value of a given key; or NULL if not found.
// The dictionary is NOT changed.
const Expr  *ExprDict::operator [](const char *name) const {
  return (Expr*)_expr[name];
}

void ExprDict::print_defines(FILE *fp) {
  fprintf(fp, "\n");
  const char *name = NULL;
  for( _defines.reset(); (name = _defines.iter()) != NULL; ) {
    const Expr *expr = (const Expr*)_expr[name];
    assert( expr != NULL, "name in ExprDict without matching Expr in dictionary");
    expr->print_define(fp);
  }
}
void ExprDict::print_asserts(FILE *fp) {
  fprintf(fp, "\n");
  fprintf(fp, "  // Following assertions generated from definition section\n");
  const char *name = NULL;
  for( _defines.reset(); (name = _defines.iter()) != NULL; ) {
    const Expr *expr = (const Expr*)_expr[name];
    assert( expr != NULL, "name in ExprDict without matching Expr in dictionary");
    expr->print_assert(fp);
  }
}

// Print out the dictionary contents as key-value pairs
static void dumpekey(const void* key)  { fprintf(stdout, "%s", (char*) key); }
static void dumpexpr(const void* expr) { fflush(stdout); ((Expr*)expr)->print(); }

void ExprDict::dump() {
  _expr.print(dumpekey, dumpexpr);
}


//------------------------------ExprDict::private------------------------------
// Disable public use of constructor, copy-ctor, operator =, operator ==
ExprDict::ExprDict( ) : _expr(cmpkey,hashkey), _defines()  {
  assert( false, "NotImplemented");
}
ExprDict::ExprDict( const ExprDict & ) : _expr(cmpkey,hashkey), _defines() {
  assert( false, "NotImplemented");
}
ExprDict &ExprDict::operator =( const ExprDict &rhs) {
  assert( false, "NotImplemented");
  _expr = rhs._expr;
  return *this;
}
// == compares two dictionaries; they must have the same keys (their keys
// must match using CmpKey) and they must have the same values (pointer
// comparison).  If so 1 is returned, if not 0 is returned.
bool ExprDict::operator ==(const ExprDict &d) const {
  assert( false, "NotImplemented");
  return false;
}


//------------------------------Production-------------------------------------
Production::Production(const char *result, const char *constraint, const char *valid) {
  initialize();
  _result     = result;
  _constraint = constraint;
  _valid      = valid;
}

void Production::initialize() {
  _result     = NULL;
  _constraint = NULL;
  _valid      = knownInvalid;
  _cost_lb    = Expr::get_unknown();
  _cost_ub    = Expr::get_unknown();
}

void Production::print() {
  printf("%s", (_result     == NULL ? "NULL" : _result ) );
  printf("%s", (_constraint == NULL ? "NULL" : _constraint ) );
  printf("%s", (_valid      == NULL ? "NULL" : _valid ) );
  _cost_lb->print();
  _cost_ub->print();
}


//------------------------------ProductionState--------------------------------
void ProductionState::initialize() {
  _constraint = noConstraint;

  // reset each Production currently in the dictionary
  DictI iter( &_production );
  const void *x, *y = NULL;
  for( ; iter.test(); ++iter) {
    x = iter._key;
    y = iter._value;
    Production *p = (Production*)y;
    if( p != NULL ) {
      p->initialize();
    }
  }
}

Production *ProductionState::getProduction(const char *result) {
  Production *p = (Production *)_production[result];
  if( p == NULL ) {
    p = new Production(result, _constraint, knownInvalid);
    _production.Insert(result, p);
  }

  return p;
}

void ProductionState::set_constraint(const char *constraint) {
  _constraint = constraint;
}

const char *ProductionState::valid(const char *result) {
  return getProduction(result)->valid();
}

void ProductionState::set_valid(const char *result) {
  Production *p = getProduction(result);

  // Update valid as allowed by current constraints
  if( _constraint == noConstraint ) {
    p->_valid = knownValid;
  } else {
    if( p->_valid != knownValid ) {
      p->_valid = unknownValid;
    }
  }
}

Expr *ProductionState::cost_lb(const char *result) {
  return getProduction(result)->cost_lb();
}

Expr *ProductionState::cost_ub(const char *result) {
  return getProduction(result)->cost_ub();
}

void ProductionState::set_cost_bounds(const char *result, const Expr *cost, bool has_state_check, bool has_cost_check) {
  Production *p = getProduction(result);

  if( p->_valid == knownInvalid ) {
    // Our cost bounds are not unknown, just not defined.
    p->_cost_lb = cost->clone();
    p->_cost_ub = cost->clone();
  } else if (has_state_check || _constraint != noConstraint) {
    // The production is protected by a condition, so
    // the cost bounds may expand.
    // _cost_lb = min(cost, _cost_lb)
    if( cost->less_than_or_equal(p->_cost_lb) ) {
      p->_cost_lb = cost->clone();
    }
    // _cost_ub = max(cost, _cost_ub)
    if( p->_cost_ub->less_than_or_equal(cost) ) {
      p->_cost_ub = cost->clone();
    }
  } else if (has_cost_check) {
    // The production has no condition check, but does
    // have a cost check that could reduce the upper
    // and/or lower bound.
    // _cost_lb = min(cost, _cost_lb)
    if( cost->less_than_or_equal(p->_cost_lb) ) {
      p->_cost_lb = cost->clone();
    }
    // _cost_ub = min(cost, _cost_ub)
    if( cost->less_than_or_equal(p->_cost_ub) ) {
      p->_cost_ub = cost->clone();
    }
  } else {
    // The costs are unconditionally set.
    p->_cost_lb = cost->clone();
    p->_cost_ub = cost->clone();
  }

}

// Print out the dictionary contents as key-value pairs
static void print_key (const void* key)              { fprintf(stdout, "%s", (char*) key); }
static void print_production(const void* production) { fflush(stdout); ((Production*)production)->print(); }

void ProductionState::print() {
  _production.print(print_key, print_production);
}
