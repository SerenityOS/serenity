/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "AST.h"
#include <AK/Function.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

namespace Shell {

class Parser {
public:
    Parser(StringView input, bool interactive = false)
        : m_input(move(input))
        , m_in_interactive_mode(interactive)
    {
    }

    RefPtr<AST::Node> parse();
    /// Parse the given string *as* an expression
    /// that is to forefully enclose it in double-quotes.
    RefPtr<AST::Node> parse_as_single_expression();
    NonnullRefPtrVector<AST::Node> parse_as_multiple_expressions();

    struct SavedOffset {
        size_t offset;
        AST::Position::Line line;
    };
    SavedOffset save_offset() const;

private:
    enum class ShouldReadMoreSequences {
        Yes,
        No,
    };
    struct SequenceParseResult {
        NonnullRefPtrVector<AST::Node> entries;
        Vector<AST::Position, 1> separator_positions;
        ShouldReadMoreSequences decision;
    };

    constexpr static size_t max_allowed_nested_rule_depth = 2048;
    RefPtr<AST::Node> parse_toplevel();
    SequenceParseResult parse_sequence();
    RefPtr<AST::Node> parse_function_decl();
    RefPtr<AST::Node> parse_and_logical_sequence();
    RefPtr<AST::Node> parse_or_logical_sequence();
    RefPtr<AST::Node> parse_variable_decls();
    RefPtr<AST::Node> parse_pipe_sequence();
    RefPtr<AST::Node> parse_command();
    RefPtr<AST::Node> parse_control_structure();
    RefPtr<AST::Node> parse_continuation_control();
    RefPtr<AST::Node> parse_for_loop();
    RefPtr<AST::Node> parse_loop_loop();
    RefPtr<AST::Node> parse_if_expr();
    RefPtr<AST::Node> parse_subshell();
    RefPtr<AST::Node> parse_match_expr();
    AST::MatchEntry parse_match_entry();
    RefPtr<AST::Node> parse_match_pattern();
    RefPtr<AST::Node> parse_redirection();
    RefPtr<AST::Node> parse_list_expression();
    RefPtr<AST::Node> parse_expression();
    RefPtr<AST::Node> parse_string_composite();
    RefPtr<AST::Node> parse_string();
    RefPtr<AST::Node> parse_doublequoted_string_inner();
    RefPtr<AST::Node> parse_variable();
    RefPtr<AST::Node> parse_evaluate();
    RefPtr<AST::Node> parse_history_designator();
    RefPtr<AST::Node> parse_comment();
    RefPtr<AST::Node> parse_bareword();
    RefPtr<AST::Node> parse_glob();
    RefPtr<AST::Node> parse_brace_expansion();
    RefPtr<AST::Node> parse_brace_expansion_spec();
    RefPtr<AST::Node> parse_immediate_expression();

    template<typename A, typename... Args>
    NonnullRefPtr<A> create(Args... args);

    bool at_end() const { return m_input.length() <= m_offset; }
    char peek();
    char consume();
    bool expect(char);
    bool expect(const StringView&);
    bool next_is(const StringView&);

    void restore_to(size_t offset, AST::Position::Line line)
    {
        m_offset = offset;
        m_line = move(line);
    }

    AST::Position::Line line() const { return m_line; }

    StringView consume_while(Function<bool(char)>);

    struct Offset {
        size_t offset;
        AST::Position::Line line;
    };
    struct ScopedOffset {
        ScopedOffset(Vector<size_t>& offsets, Vector<AST::Position::Line>& lines, size_t offset, size_t lineno, size_t linecol)
            : offsets(offsets)
            , lines(lines)
            , offset(offset)
            , line({ lineno, linecol })
        {
            offsets.append(offset);
            lines.append(line);
        }
        ~ScopedOffset()
        {
            auto last = offsets.take_last();
            VERIFY(last == offset);

            auto last_line = lines.take_last();
            VERIFY(last_line == line);
        }

        Vector<size_t>& offsets;
        Vector<AST::Position::Line>& lines;
        size_t offset;
        AST::Position::Line line;
    };

    void restore_to(const ScopedOffset& offset) { restore_to(offset.offset, offset.line); }

    OwnPtr<ScopedOffset> push_start();
    Offset current_position();

    StringView m_input;
    size_t m_offset { 0 };

    AST::Position::Line m_line { 0, 0 };

    Vector<size_t> m_rule_start_offsets;
    Vector<AST::Position::Line> m_rule_start_lines;

    Vector<char> m_extra_chars_not_allowed_in_barewords;
    bool m_is_in_brace_expansion_spec { false };
    bool m_continuation_controls_allowed { false };
    bool m_in_interactive_mode { false };
};

#if 0
constexpr auto the_grammar = R"(
toplevel :: sequence?

sequence :: variable_decls? or_logical_sequence terminator sequence
          | variable_decls? or_logical_sequence '&' sequence
          | variable_decls? or_logical_sequence
          | variable_decls? function_decl (terminator sequence)?
          | variable_decls? terminator sequence

function_decl :: identifier '(' (ws* identifier)* ')' ws* '{' [!c] toplevel '}'

or_logical_sequence :: and_logical_sequence '|' '|' and_logical_sequence
                     | and_logical_sequence

and_logical_sequence :: pipe_sequence '&' '&' and_logical_sequence
                      | pipe_sequence

terminator :: ';'
            | '\n'

variable_decls :: identifier '=' expression (' '+ variable_decls)? ' '*
                | identifier '=' '(' pipe_sequence ')' (' '+ variable_decls)? ' '*

pipe_sequence :: command '|' pipe_sequence
               | command
               | control_structure '|' pipe_sequence
               | control_structure

control_structure[c] :: for_expr
                      | loop_expr
                      | if_expr
                      | subshell
                      | match_expr
                      | ?c: continuation_control

continuation_control :: 'break'
                      | 'continue'

for_expr :: 'for' ws+ (identifier ' '+ 'in' ws*)? expression ws+ '{' [c] toplevel '}'

loop_expr :: 'loop' ws* '{' [c] toplevel '}'

if_expr :: 'if' ws+ or_logical_sequence ws+ '{' toplevel '}' else_clause?

else_clause :: else '{' toplevel '}'
             | else if_expr

subshell :: '{' toplevel '}'

match_expr :: 'match' ws+ expression ws* ('as' ws+ identifier)? '{' match_entry* '}'

match_entry :: match_pattern ws* (as identifier_list)? '{' toplevel '}'

identifier_list :: '(' (identifier ws*)* ')'

match_pattern :: expression (ws* '|' ws* expression)*

command :: redirection command
         | list_expression command?

redirection :: number? '>'{1,2} ' '* string_composite
             | number? '<' ' '* string_composite
             | number? '>' '&' number
             | number? '>' '&' '-'

list_expression :: ' '* expression (' '+ list_expression)?

expression :: evaluate expression?
            | string_composite expression?
            | comment expression?
            | immediate_expression expression?
            | history_designator expression?
            | '(' list_expression ')' expression?

evaluate :: '$' '(' pipe_sequence ')'
          | '$' expression          {eval / dynamic resolve}

string_composite :: string string_composite?
                  | variable string_composite?
                  | bareword string_composite?
                  | glob string_composite?
                  | brace_expansion string_composite?

string :: '"' dquoted_string_inner '"'
        | "'" [^']* "'"

dquoted_string_inner :: '\' . dquoted_string_inner?       {concat}
                      | variable dquoted_string_inner?    {compose}
                      | . dquoted_string_inner?
                      | '\' 'x' digit digit dquoted_string_inner?
                      | '\' [abefrn] dquoted_string_inner?

variable :: '$' identifier
          | '$' '$'
          | '$' '?'
          | '$' '*'
          | '$' '#'
          | ...

comment :: '#' [^\n]*

immediate_expression :: '$' '{' immediate_function expression* '}'

immediate_function :: identifier       { predetermined list of names, see Shell.h:ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS }

history_designator :: '!' event_selector (':' word_selector_composite)?

event_selector :: '!'                  {== '-0'}
                | '?' bareword '?'
                | bareword             {number: index, otherwise: lookup}

word_selector_composite :: word_selector ('-' word_selector)?

word_selector :: number
               | '^'                   {== 0}
               | '$'                   {== end}

bareword :: [^"'*$&#|()[\]{} ?;<>] bareword?
          | '\' [^"'*$&#|()[\]{} ?;<>] bareword?

bareword_with_tilde_expansion :: '~' bareword?

glob :: [*?] bareword?
      | bareword [*?]

brace_expansion :: '{' brace_expansion_spec '}'

brace_expansion_spec :: expression? (',' expression?)*
                      | expression '..' expression
)";
#endif

}
