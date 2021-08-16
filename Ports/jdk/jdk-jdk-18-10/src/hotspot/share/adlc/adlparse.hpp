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

#ifndef SHARE_ADLC_ADLPARSE_HPP
#define SHARE_ADLC_ADLPARSE_HPP

// ADLPARSE.HPP - Definitions for Architecture Description Language Parser
// Authors: Chris Vick and Mike Paleczny

// Class List
class Form;
// ***** Top Level, 1, classes  *****
class InstructForm;
class OperandForm;
class OpClassForm;
class AttributeForm;
class RegisterForm;
class PipelineForm;
class SourceForm;
class Peephole;
// ***** Level 2 classes *****
class Component;
class Predicate;
class MatchRule;
class Encode;
class Attribute;
class Effect;
class ExpandRule;
class RewriteRule;
class Constraint;
class ConstructRule;
// ***** Register Section *****
class RegDef;
class RegClass;
class CodeSnippetRegClass;
class ConditionalRegClass;
class AllocClass;
class ResourceForm;
// ***** Pipeline Section *****
class PipeDesc;
class PipeClass;
class RegList;
// ***** Peephole Section *****
class PeepMatch;
class PeepConstraint;
class PeepReplace;

extern char *toUpper(const char *str);

//---------------------------ADLParser-----------------------------------------
class ADLParser {
protected:
  char     *_curline;           // Start of current line
  char     *_ptr;               // Pointer into current location in File Buffer
  char      _curchar;           // Current character from buffer
  FormDict &_globalNames;       // Global names

  enum { _preproc_limit = 20 };
  int       _preproc_depth;                 // How deep are we into ifdefs?
  int       _preproc_not_taken;             // How deep in not-taken ifdefs?
  bool      _preproc_taken[_preproc_limit]; // Are we taking this ifdef level?
  bool      _preproc_else[_preproc_limit];  // Did this level have an else yet?

  // ***** Level 1 Parse functions *****
  void instr_parse(void);       // Parse instruction definitions
  void oper_parse(void);        // Parse operand definitions
  void opclass_parse(void);     // Parse operand class definitions
  void ins_attr_parse(void);    // Parse instruction attrubute definitions
  void op_attr_parse(void);     // Parse operand attrubute definitions
  void source_parse(void);      // Parse source section
  void source_hpp_parse(void);  // Parse source_hpp section
  void reg_parse(void);         // Parse register section
  void encode_parse(void);      // Parse encoding section
  void frame_parse(void);       // Parse frame section
  void pipe_parse(void);        // Parse pipeline section
  void definitions_parse(void); // Parse definitions section
  void peep_parse(void);        // Parse peephole rule definitions
  void preproc_line(void);      // Parse a #line statement
  void preproc_define(void);    // Parse a #define statement
  void preproc_undef(void);     // Parse an #undef statement

  // Helper functions for instr_parse().
  void adjust_set_rule(InstructForm *instr);
  void matchrule_clone_and_swap(MatchRule *rule, const char* instr_ident, int& match_rules_cnt);

  // ***** Level 2 Parse functions *****
  // Parse the components of the encode section
  void enc_class_parse(void);   // Parse encoding class definition
  void enc_class_parse_block(EncClass* encoding, char* ec_name);

  // Parse the components of the frame section
  void sync_stack_slots_parse(FrameForm *frame);
  void frame_pointer_parse(FrameForm *frame, bool native);
  void interpreter_frame_pointer_parse(FrameForm *frame, bool native);
  void inline_cache_parse(FrameForm *frame, bool native);
  void interpreter_arg_ptr_parse(FrameForm *frame, bool native);
  void interpreter_method_parse(FrameForm *frame, bool native);
  void cisc_spilling_operand_name_parse(FrameForm *frame, bool native);
  void stack_alignment_parse(FrameForm *frame);
  void return_addr_parse(FrameForm *frame, bool native);
  char *return_value_parse();

  // Parse components of the register section
  void reg_def_parse(void);              // Parse register definition
  void reg_class_parse(void);            // Parse register class definition
  void reg_class_dynamic_parse(void);    // Parse dynamic register class definition
  void alloc_class_parse(void);          // Parse allocation class definition

  // Parse components of the definition section
  void int_def_parse(void);              // Parse an integer definition

  // Parse components of a pipeline rule
  void resource_parse(PipelineForm &pipe);   // Parse resource definition
  void pipe_desc_parse(PipelineForm &pipe);  // Parse pipeline description definition
  void pipe_class_parse(PipelineForm &pipe); // Parse pipeline class definition

  // Parse components of a peephole rule
  void peep_match_parse(Peephole &peep);     // Parse the peephole match rule
  void peep_constraint_parse(Peephole &peep);// Parse the peephole constraints
  void peep_replace_parse(Peephole &peep);   // Parse peephole replacement rule

  // Parse the peep match rule tree
  InstructForm *peep_match_child_parse(PeepMatch &match, int parent, int &position, int input);

  // Parse components of an operand and/or instruction form
  Predicate     *pred_parse(void);       // Parse predicate rule
  // Parse match rule, and internal nodes
  MatchRule     *match_parse(FormDict &operands);
  MatchNode     *matchNode_parse(FormDict &operands, int &depth,
                                 int &numleaves, bool atroot);
  MatchNode     *matchChild_parse(FormDict &operands, int &depth,
                                  int &numleaves, bool atroot);

  Attribute     *attr_parse(char *ident);// Parse instr/operand attribute rule
  // Parse instruction encode rule
  void           ins_encode_parse(InstructForm &inst);
  void           ins_encode_parse_block(InstructForm &inst);
  void           ins_encode_parse_block_impl(InstructForm& inst, EncClass* encoding, char* ec_name);
  // Parse instruction postalloc expand rule.
  void           postalloc_expand_parse(InstructForm &inst);

  void           constant_parse(InstructForm& inst);
  void           constant_parse_expression(EncClass* encoding, char* ec_name);

  Opcode        *opcode_parse(InstructForm *insr); // Parse instruction opcode
  char          *size_parse(InstructForm *insr); // Parse instruction size
  Interface     *interface_parse();      // Parse operand interface rule
  Interface     *mem_interface_parse();  // Parse memory interface rule
  Interface     *cond_interface_parse(); // Parse conditional interface rule
  char          *interface_field_parse(const char** format = NULL);// Parse field contents

  FormatRule    *format_parse(void);     // Parse format rule
  FormatRule    *template_parse(void);     // Parse format rule
  void           effect_parse(InstructForm *instr); // Parse effect rule
  ExpandRule    *expand_parse(InstructForm *instr); // Parse expand rule
  RewriteRule   *rewrite_parse(void);    // Parse rewrite rule
  Constraint    *constraint_parse(void); // Parse constraint rule
  ConstructRule *construct_parse(void);  // Parse construct rule
  void           ins_pipe_parse(InstructForm &instr); // Parse ins_pipe rule

  // ***** Preprocessor functions *****
  void begin_if_def(bool taken) {
    assert(_preproc_depth < _preproc_limit, "#ifdef nesting limit");
    int ppn = _preproc_depth++;
    _preproc_taken[ppn] = taken;
    // Invariant:  _preproc_not_taken = SUM !_preproc_taken[0.._preproc_depth)
    if (!_preproc_taken[ppn])  _preproc_not_taken += 1;
    _preproc_else[ppn] = false;
  }
  void invert_if_def() {
    assert(_preproc_depth > 0, "#ifdef matching");
    int ppn = _preproc_depth - 1;
    assert(!_preproc_else[ppn], "multiple #else lines");
    _preproc_else[ppn] = true;
    if (!_preproc_taken[ppn])  _preproc_not_taken -= 1;
    _preproc_taken[ppn] = !_preproc_taken[ppn];
    if (!_preproc_taken[ppn])  _preproc_not_taken += 1;
  }
  void end_if_def() {
    assert(_preproc_depth > 0, "#ifdef matching");
    int ppn = --_preproc_depth;
    if (!_preproc_taken[ppn])  _preproc_not_taken -= 1;
  }
  bool preproc_taken() {
    // Return true only if there is no directive hiding this text position.
    return _preproc_not_taken == 0;
  }
  // Handle a '#' token.  Return true if it disappeared.
  bool handle_preproc_token();

  // ***** Utility Functions for ADL Parser ******

  // Parse one string argument inside parens:  '(' string ')' ';'
  char *parse_one_arg(const char *description);

  // Return the next identifier given a pointer into a line of the buffer.
  char *get_ident()            { return get_ident_common(true); }
  char *get_ident_no_preproc() { return get_ident_common(false); }
  char *get_ident_common(bool do_preproc);      // Grab it from the file buffer
  char *get_ident_dup(void);    // Grab a duplicate of the identifier
  char *get_ident_or_literal_constant(const char* description);
  // Grab unique identifier from file buffer
  char *get_unique_ident(FormDict &dict, const char *nameDescription);
  // Return the next replacement variable identifier
  char *get_rep_var_ident(void);
  // Skip first '$' and make a duplicate of the string
  char *get_rep_var_ident_dup(void);
  // Return the next token given as a signed integer.
  int   get_int(void);
  // Return the next token, a relational operator { ==, !=, <=, >= }
  char *get_relation_dup(void);

  void  get_oplist(NameList &parameters, FormDict &operands);// Parse type-operand pairs
  void  get_effectlist(FormDict &effects, FormDict &operands, bool& has_call); // Parse effect-operand pairs
  // Return the contents of a parenthesized expression.
  // Requires initial '(' and consumes final ')', which is replaced by '\0'.
  char *get_paren_expr(const char *description, bool include_location = false);
  // Return expression up to next stop-char, which terminator replaces.
  // Does not require initial '('.  Does not consume final stop-char.
  // Final stop-char is left in _curchar, but is also is replaced by '\0'.
  char *get_expr(const char *description, const char *stop_chars);
  char *find_cpp_block(const char *description); // Parse a C++ code block
  // Issue parser error message & go to EOL
  void parse_err(int flag, const char *fmt, ...);
  // Create a location marker for this file and line.
  char *get_line_string(int linenum = 0);
  // Return a location marker which tells the C preprocessor to
  // forget the previous location marker.  (Requires awk postprocessing.)
  char *end_line_marker() { return (char*)"\n#line 999999\n"; }

  // Return pointer to current character
  inline char  cur_char(void);
  // Advance to next character, assign this to _curchar
  inline void  next_char(void);
  inline void  next_char_or_line(void);
  // Advance File Buffer to next line, updating _curline
  inline void  next_line(void);
  // Issue an error if we are not at the beginning of a line (exc. whitespace).
  void ensure_start_of_line(void);
  // Issue an error if we are not at the end of a line (exc. whitespace).
  void ensure_end_of_line(void);
  // Skip whitespace, leaving ptr pointing to first non-whitespace character
  // Also handle preprocessor constructs like "#ifdef".
  void skipws()                { skipws_common(true); }
  // Skip comments and spaces but not newlines or preprocessor constructs.
  void skipws_no_preproc()     { skipws_common(false); }
  void skipws_common(bool do_preproc);

  FileBuff &_buf;               // File buffer to be parsed
  ArchDesc &_AD;                // Architecture Description being built

public:

  ADLParser(FileBuff &buf, ArchDesc &archDesc); // Create new ADLParser object
  ~ADLParser();                 // Destroy ADLParser object

  void parse(void);             // Do the parsing & build forms lists

  int linenum() { return _buf.linenum(); }

  static bool is_literal_constant(const char *hex_string);
  static bool is_hex_digit(char digit);
  static bool is_int_token(const char* token, int& intval);
  static bool equivalent_expressions(const char* str1, const char* str2);
  static void trim(char* &token);  // trim leading & trailing spaces
};

#endif // SHARE_ADLC_ADLPARSE_HPP
