/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/StringUtils.h>
#include <LibShell/Parser.h>
#include <LibShell/PosixParser.h>

#define TRY_OR_THROW_PARSE_ERROR_AT(expr, position) ({                                                     \
    /* Ignore -Wshadow to allow nesting the macro. */                                                      \
    AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                                       \
                         auto&& _value_or_error = expr;)                                                   \
    if (_value_or_error.is_error()) {                                                                      \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                                   \
                             auto _error = _value_or_error.release_error();)                               \
        if (_error.is_errno() && _error.code() == ENOMEM)                                                  \
            return make_ref_counted<AST::SyntaxError>(position, "OOM"_string);                             \
        return make_ref_counted<AST::SyntaxError>(position, MUST(String::formatted("Error: {}", _error))); \
    }                                                                                                      \
    _value_or_error.release_value();                                                                       \
})

static RefPtr<Shell::AST::Node> strip_execute(RefPtr<Shell::AST::Node> node)
{
    while (node && node->is_execute())
        node = static_ptr_cast<Shell::AST::Execute>(node)->command();
    return node;
}

static Shell::AST::Position empty_position()
{
    return { 0, 0, { 0, 0 }, { 0, 0 } };
}

template<typename T, typename... Ts>
static inline bool is_one_of(T const& value, Ts const&... values)
{
    return ((value == values) || ...);
}

static inline bool is_io_operator(Shell::Posix::Token const& token)
{
    using namespace Shell::Posix;
    return is_one_of(token.type,
        Token::Type::Less, Token::Type::Great,
        Token::Type::LessAnd, Token::Type::GreatAnd,
        Token::Type::DoubleLess, Token::Type::DoubleGreat,
        Token::Type::DoubleLessDash, Token::Type::LessGreat,
        Token::Type::Clobber);
}

static inline bool is_separator(Shell::Posix::Token const& token)
{
    using namespace Shell::Posix;
    return is_one_of(token.type,
        Token::Type::Semicolon, Token::Type::Newline,
        Token::Type::AndIf, Token::Type::OrIf,
        Token::Type::Pipe,
        Token::Type::And);
}

static inline bool is_a_reserved_word_position(Shell::Posix::Token const& token, Optional<Shell::Posix::Token> const& previous_token, Optional<Shell::Posix::Token> const& previous_previous_token)
{
    using namespace Shell::Posix;

    auto is_start_of_command = !previous_token.has_value()
        || previous_token->value.is_empty()
        || is_separator(*previous_token)
        || is_one_of(previous_token->type,
            Token::Type::OpenParen, Token::Type::CloseParen, Token::Type::Newline, Token::Type::DoubleSemicolon,
            Token::Type::Semicolon, Token::Type::Pipe, Token::Type::OrIf, Token::Type::AndIf);
    if (is_start_of_command)
        return true;

    if (!previous_token.has_value())
        return false;

    auto previous_is_reserved_word = is_one_of(previous_token->value,
        "for"sv, "in"sv, "case"sv, "if"sv, "then"sv, "else"sv,
        "elif"sv, "while"sv, "until"sv, "do"sv, "done"sv, "esac"sv,
        "fi"sv, "!"sv, "{"sv, "}"sv);

    if (previous_is_reserved_word)
        return true;

    if (!previous_previous_token.has_value())
        return false;

    auto is_third_in_case = previous_previous_token->value == "case"sv
        && token.type == Token::Type::Token && token.value == "in"sv;

    if (is_third_in_case)
        return true;

    auto is_third_in_for = previous_previous_token->value == "for"sv
        && token.type == Token::Type::Token && is_one_of(token.value, "in"sv, "do"sv);

    return is_third_in_for;
}

static inline bool is_reserved(Shell::Posix::Token const& token)
{
    using namespace Shell::Posix;
    return is_one_of(token.type,
        Token::Type::If, Token::Type::Then, Token::Type::Else,
        Token::Type::Elif, Token::Type::Fi, Token::Type::Do,
        Token::Type::Done, Token::Type::Case, Token::Type::Esac,
        Token::Type::While, Token::Type::Until, Token::Type::For,
        Token::Type::In, Token::Type::OpenBrace, Token::Type::CloseBrace,
        Token::Type::Bang);
}

static inline bool is_valid_name(StringView word)
{
    // Dr.POSIX: a word consisting solely of underscores, digits, and alphabetics from the portable character set. The first character of a name is not a digit.
    return !word.is_empty()
        && !is_ascii_digit(word[0])
        && all_of(word, [](auto ch) { return is_ascii_alphanumeric(ch) || ch == '_'; });
}

namespace Shell::Posix {

template<typename... Args>
static NonnullRefPtr<AST::Node> immediate(String name, AST::Position position, Args&&... args)
{
    return make_ref_counted<AST::ImmediateExpression>(
        position,
        AST::NameWithPosition {
            move(name),
            position,
        },
        Vector<NonnullRefPtr<AST::Node>> { forward<Args>(args)... },
        empty_position());
}

template<typename... Args>
static NonnullRefPtr<AST::Node> reexpand(AST::Position position, Args&&... args)
{
    return immediate("reexpand"_string, position, forward<Args>(args)...);
}

ErrorOr<void> Parser::fill_token_buffer(Optional<Reduction> starting_reduction)
{
    for (;;) {
        auto token = TRY(next_expanded_token(starting_reduction));
        if (!token.has_value())
            break;
#if SHELL_POSIX_PARSER_DEBUG
        ByteString position = "(~)";
        if (token->position.has_value())
            position = ByteString::formatted("{}:{}", token->position->start_offset, token->position->end_offset);
        ByteString expansions = "";
        for (auto& exp : token->resolved_expansions)
            exp.visit(
                [&](ResolvedParameterExpansion& x) { expansions = ByteString::formatted("{}param({}),", expansions, x.to_byte_string()); },
                [&](ResolvedCommandExpansion& x) { expansions = ByteString::formatted("{}command({:p})", expansions, x.command.ptr()); },
                [&](ResolvedArithmeticExpansion& x) { expansions = ByteString::formatted("{}arith({})", expansions, x.source_expression); });
        ByteString rexpansions = "";
        for (auto& exp : token->expansions)
            exp.visit(
                [&](ParameterExpansion& x) { rexpansions = ByteString::formatted("{}param({}) from {} to {},", rexpansions, x.parameter.string_view(), x.range.start, x.range.length); },
                [&](auto&) { rexpansions = ByteString::formatted("{}...,", rexpansions); });
        dbgln("Token @ {}: '{}' (type {}) - parsed expansions: {} - raw expansions: {}", position, token->value.replace("\n"sv, "\\n"sv, ReplaceMode::All), token->type_name(), expansions, rexpansions);
#endif
    }
    m_token_index = 0;

    // Detect Assignment words, bash-like lists extension
    for (size_t i = 1; i < m_token_buffer.size(); ++i) {
        // Treat 'ASSIGNMENT_WORD OPEN_PAREN' where ASSIGNMENT_WORD is `word=' and OPEN_PAREN has no preceding trivia as a bash-like list assignment.
        auto& token = m_token_buffer[i - 1];
        auto& next_token = m_token_buffer[i];

        if (token.type != Token::Type::AssignmentWord)
            continue;

        if (!token.value.ends_with('='))
            continue;

        if (next_token.type != Token::Type::OpenParen)
            continue;

        if (token.position.map([](auto& x) { return x.end_offset + 1; }) != next_token.position.map([](auto& x) { return x.start_offset; }))
            continue;

        token.type = Token::Type::ListAssignmentWord;
    }

    return {};
}

RefPtr<AST::Node> Parser::parse()
{
    auto start_position = peek().position.value_or(empty_position());
    return TRY_OR_THROW_PARSE_ERROR_AT(parse_complete_command(), start_position);
}

void Parser::handle_heredoc_contents()
{
    while (!eof() && m_token_buffer[m_token_index].type == Token::Type::HeredocContents) {
        auto& token = m_token_buffer[m_token_index++];
        auto entry = m_unprocessed_heredoc_entries.get(token.relevant_heredoc_key.value());
        if (!entry.has_value()) {
            error(token, "Discarding unexpected heredoc contents for key '{}'", *token.relevant_heredoc_key);
            continue;
        }

        auto& heredoc = **entry;

        auto contents_or_error = [&]() -> ErrorOr<RefPtr<AST::Node>> {
            if (heredoc.allow_interpolation()) {
                Parser parser { token.value, m_in_interactive_mode, Reduction::HeredocContents };
                return parser.parse_word();
            }

            return make_ref_counted<AST::StringLiteral>(
                token.position.value_or(empty_position()),
                token.value,
                AST::StringLiteral::EnclosureType::None);
        }();

        if (contents_or_error.is_error()) {
            warnln("Shell: Failed to parse heredoc contents: {}", contents_or_error.error());
            continue;
        }

        if (auto contents = contents_or_error.release_value())
            heredoc.set_contents(contents);

        m_unprocessed_heredoc_entries.remove(*token.relevant_heredoc_key);
    }
}

ErrorOr<Optional<Token>> Parser::next_expanded_token(Optional<Reduction> starting_reduction)
{
    while (m_token_buffer.is_empty() || m_token_buffer.last().type != Token::Type::Eof) {
        auto tokens = TRY(m_lexer.batch_next(starting_reduction));
        auto expanded = perform_expansions(move(tokens));
        m_token_buffer.extend(expanded.span());
    }

    if (m_token_buffer.size() == m_token_index)
        return OptionalNone {};

    return m_token_buffer[m_token_index++];
}

Vector<Token> Parser::perform_expansions(Vector<Token> tokens)
{
    if (tokens.is_empty())
        return {};

    Vector<Token> expanded_tokens;
    auto previous_token = Optional<Token>();
    auto previous_previous_token = Optional<Token>();
    auto tokens_taken_from_buffer = 0;

    expanded_tokens.ensure_capacity(tokens.size());

    auto swap_expansions = [&] {
        if (previous_previous_token.has_value())
            expanded_tokens.append(previous_previous_token.release_value());

        if (previous_token.has_value())
            expanded_tokens.append(previous_token.release_value());

        for (; tokens_taken_from_buffer > 0; tokens_taken_from_buffer--)
            m_token_buffer.append(expanded_tokens.take_first());

        swap(tokens, expanded_tokens);
        expanded_tokens.clear_with_capacity();
    };

    // (1) join all consecutive newlines (this works around a grammar ambiguity)
    auto previous_was_newline = !m_token_buffer.is_empty() && m_token_buffer.last().type == Token::Type::Newline;
    for (auto& token : tokens) {
        if (token.type == Token::Type::Newline) {
            if (previous_was_newline)
                continue;
            previous_was_newline = true;
        } else {
            previous_was_newline = false;
        }
        expanded_tokens.append(move(token));
    }

    swap_expansions();

    // (2) Detect reserved words
    if (m_token_buffer.size() >= 1) {
        previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }
    if (m_token_buffer.size() >= 1) {
        previous_previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }

    auto check_reserved_word = [&](auto& token) {
        if (is_a_reserved_word_position(token, previous_token, previous_previous_token)) {
            if (token.value == "if"sv)
                token.type = Token::Type::If;
            else if (token.value == "then"sv)
                token.type = Token::Type::Then;
            else if (token.value == "else"sv)
                token.type = Token::Type::Else;
            else if (token.value == "elif"sv)
                token.type = Token::Type::Elif;
            else if (token.value == "fi"sv)
                token.type = Token::Type::Fi;
            else if (token.value == "while"sv)
                token.type = Token::Type::While;
            else if (token.value == "until"sv)
                token.type = Token::Type::Until;
            else if (token.value == "do"sv)
                token.type = Token::Type::Do;
            else if (token.value == "done"sv)
                token.type = Token::Type::Done;
            else if (token.value == "case"sv)
                token.type = Token::Type::Case;
            else if (token.value == "esac"sv)
                token.type = Token::Type::Esac;
            else if (token.value == "for"sv)
                token.type = Token::Type::For;
            else if (token.value == "in"sv)
                token.type = Token::Type::In;
            else if (token.value == "!"sv)
                token.type = Token::Type::Bang;
            else if (token.value == "{"sv)
                token.type = Token::Type::OpenBrace;
            else if (token.value == "}"sv)
                token.type = Token::Type::CloseBrace;
            else if (token.type == Token::Type::Token)
                token.type = Token::Type::Word;
        } else if (token.type == Token::Type::Token) {
            token.type = Token::Type::Word;
        }
    };

    for (auto& token : tokens) {
        if (!previous_token.has_value()) {
            check_reserved_word(token);
            previous_token = token;
            continue;
        }
        if (!previous_previous_token.has_value()) {
            check_reserved_word(token);
            previous_previous_token = move(previous_token);
            previous_token = token;
            continue;
        }

        check_reserved_word(token);
        expanded_tokens.append(exchange(*previous_previous_token, exchange(*previous_token, move(token))));
    }

    swap_expansions();

    // (3) Detect io_number tokens
    previous_token = Optional<Token>();
    tokens_taken_from_buffer = 0;
    if (m_token_buffer.size() >= 1) {
        previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }

    for (auto& token : tokens) {
        if (!previous_token.has_value()) {
            previous_token = token;
            continue;
        }

        if (is_io_operator(token) && previous_token->type == Token::Type::Word && all_of(previous_token->value.bytes_as_string_view(), is_ascii_digit)) {
            previous_token->type = Token::Type::IoNumber;
        }

        expanded_tokens.append(exchange(*previous_token, move(token)));
    }

    swap_expansions();

    // (4) Try to identify simple commands
    previous_token = Optional<Token>();
    tokens_taken_from_buffer = 0;

    if (m_token_buffer.size() >= 1) {
        previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }

    for (auto& token : tokens) {
        if (!previous_token.has_value()) {
            token.could_be_start_of_a_simple_command = true;
            previous_token = token;
            continue;
        }

        token.could_be_start_of_a_simple_command = is_one_of(previous_token->type, Token::Type::OpenParen, Token::Type::CloseParen, Token::Type::Newline)
            || is_separator(*previous_token)
            || (!is_reserved(*previous_token) && is_reserved(token));

        expanded_tokens.append(exchange(*previous_token, move(token)));
    }

    swap_expansions();

    // (5) Detect assignment words
    for (auto& token : tokens) {
        if (token.could_be_start_of_a_simple_command)
            m_disallow_command_prefix = false;

        // Check if we're in a command prefix (could be an assignment)
        if (!m_disallow_command_prefix && token.type == Token::Type::Word && token.value.contains('=')) {
            // If the word before '=' is a valid name, this is an assignment
            auto equal_offset = *token.value.find_byte_offset('=');
            if (is_valid_name(token.value.bytes_as_string_view().substring_view(0, equal_offset)))
                token.type = Token::Type::AssignmentWord;
            else
                m_disallow_command_prefix = true;
        } else {
            m_disallow_command_prefix = true;
        }

        expanded_tokens.append(move(token));
    }

    swap_expansions();

    // (6) Parse expansions
    for (auto& token : tokens) {
        if (!is_one_of(token.type, Token::Type::Word, Token::Type::AssignmentWord)) {
            expanded_tokens.append(move(token));
            continue;
        }

        Vector<ResolvedExpansion> resolved_expansions;
        for (auto& expansion : token.expansions) {
            auto resolved = expansion.visit(
                [&](ParameterExpansion const& expansion) -> ResolvedExpansion {
                    auto text = expansion.parameter.string_view();
                    // ${NUMBER}
                    if (all_of(text, is_ascii_digit)) {
                        return ResolvedParameterExpansion {
                            .parameter = expansion.parameter.to_string().release_value_but_fixme_should_propagate_errors(),
                            .argument = {},
                            .range = expansion.range,
                            .op = ResolvedParameterExpansion::Op::GetPositionalParameter,
                            .expand = ResolvedParameterExpansion::Expand::Word,
                        };
                    }

                    if (text.length() == 1) {
                        ResolvedParameterExpansion::Op op;
                        switch (text[0]) {
                        case '!':
                            op = ResolvedParameterExpansion::Op::GetLastBackgroundPid;
                            break;
                        case '@':
                            op = ResolvedParameterExpansion::Op::GetPositionalParameterList;
                            break;
                        case '-':
                            op = ResolvedParameterExpansion::Op::GetCurrentOptionFlags;
                            break;
                        case '#':
                            op = ResolvedParameterExpansion::Op::GetPositionalParameterCount;
                            break;
                        case '?':
                            op = ResolvedParameterExpansion::Op::GetLastExitStatus;
                            break;
                        case '*':
                            op = ResolvedParameterExpansion::Op::GetPositionalParameterListAsString;
                            break;
                        case '$':
                            op = ResolvedParameterExpansion::Op::GetShellProcessId;
                            break;
                        default:
                            if (is_valid_name(text)) {
                                op = ResolvedParameterExpansion::Op::GetVariable;
                            } else {
                                error(token, "Unknown parameter expansion: {}", text);
                                return ResolvedParameterExpansion {
                                    .parameter = expansion.parameter.to_string().release_value_but_fixme_should_propagate_errors(),
                                    .argument = {},
                                    .range = expansion.range,
                                    .op = ResolvedParameterExpansion::Op::StringLength,
                                };
                            }
                        }

                        return ResolvedParameterExpansion {
                            .parameter = String::from_code_point(text[0]),
                            .argument = {},
                            .range = expansion.range,
                            .op = op,
                            .expand = ResolvedParameterExpansion::Expand::Word,
                        };
                    }

                    if (text.starts_with('#')) {
                        return ResolvedParameterExpansion {
                            .parameter = String::from_utf8(text.substring_view(1)).release_value_but_fixme_should_propagate_errors(),
                            .argument = {},
                            .range = expansion.range,
                            .op = ResolvedParameterExpansion::Op::StringLength,
                        };
                    }

                    GenericLexer lexer { text };
                    auto parameter = lexer.consume_while([first = true](char c) mutable {
                        if (first) {
                            first = false;
                            return is_ascii_alpha(c) || c == '_';
                        }
                        return is_ascii_alphanumeric(c) || c == '_';
                    });

                    StringView argument;
                    ResolvedParameterExpansion::Op op;
                    switch (lexer.peek()) {
                    case ':':
                        lexer.ignore();
                        switch (lexer.is_eof() ? 0 : lexer.consume()) {
                        case '-':
                            argument = lexer.consume_all();
                            op = ResolvedParameterExpansion::Op::UseDefaultValue;
                            break;
                        case '=':
                            argument = lexer.consume_all();
                            op = ResolvedParameterExpansion::Op::AssignDefaultValue;
                            break;
                        case '?':
                            argument = lexer.consume_all();
                            op = ResolvedParameterExpansion::Op::IndicateErrorIfEmpty;
                            break;
                        case '+':
                            argument = lexer.consume_all();
                            op = ResolvedParameterExpansion::Op::UseAlternativeValue;
                            break;
                        default:
                            error(token, "Unknown parameter expansion: {}", text);
                            return ResolvedParameterExpansion {
                                .parameter = String::from_utf8(parameter).release_value_but_fixme_should_propagate_errors(),
                                .argument = {},
                                .range = expansion.range,
                                .op = ResolvedParameterExpansion::Op::StringLength,
                            };
                        }
                        break;
                    case '-':
                        lexer.ignore();
                        argument = lexer.consume_all();
                        op = ResolvedParameterExpansion::Op::UseDefaultValueIfUnset;
                        break;
                    case '=':
                        lexer.ignore();
                        argument = lexer.consume_all();
                        op = ResolvedParameterExpansion::Op::AssignDefaultValueIfUnset;
                        break;
                    case '?':
                        lexer.ignore();
                        argument = lexer.consume_all();
                        op = ResolvedParameterExpansion::Op::IndicateErrorIfUnset;
                        break;
                    case '+':
                        lexer.ignore();
                        argument = lexer.consume_all();
                        op = ResolvedParameterExpansion::Op::UseAlternativeValueIfUnset;
                        break;
                    case '%':
                        if (lexer.consume_specific('%'))
                            op = ResolvedParameterExpansion::Op::RemoveLargestSuffixByPattern;
                        else
                            op = ResolvedParameterExpansion::Op::RemoveSmallestSuffixByPattern;
                        argument = lexer.consume_all();
                        break;
                    case '#':
                        if (lexer.consume_specific('#'))
                            op = ResolvedParameterExpansion::Op::RemoveLargestPrefixByPattern;
                        else
                            op = ResolvedParameterExpansion::Op::RemoveSmallestPrefixByPattern;
                        argument = lexer.consume_all();
                        break;
                    default:
                        if (is_valid_name(text)) {
                            op = ResolvedParameterExpansion::Op::GetVariable;
                        } else {
                            error(token, "Unknown parameter expansion: {}", text);
                            return ResolvedParameterExpansion {
                                .parameter = String::from_utf8(parameter).release_value_but_fixme_should_propagate_errors(),
                                .argument = {},
                                .range = expansion.range,
                                .op = ResolvedParameterExpansion::Op::StringLength,
                            };
                        }
                    }
                    VERIFY(lexer.is_eof());

                    return ResolvedParameterExpansion {
                        .parameter = String::from_utf8(parameter).release_value_but_fixme_should_propagate_errors(),
                        .argument = String::from_utf8(argument).release_value_but_fixme_should_propagate_errors(),
                        .range = expansion.range,
                        .op = op,
                        .expand = ResolvedParameterExpansion::Expand::Word,
                    };
                },
                [&](ArithmeticExpansion const& expansion) -> ResolvedExpansion {
                    return ResolvedArithmeticExpansion { expansion.expression, expansion.range };
                },
                [&](CommandExpansion const& expansion) -> ResolvedExpansion {
                    Parser parser { expansion.command.string_view() };
                    auto node = parser.parse();
                    m_errors.extend(move(parser.m_errors));
                    return ResolvedCommandExpansion {
                        move(node),
                        expansion.range,
                    };
                });

            resolved_expansions.append(move(resolved));
        }

        token.resolved_expansions = move(resolved_expansions);
        expanded_tokens.append(move(token));
    }

    swap_expansions();

    // (7) Loop variables
    previous_token = {};
    tokens_taken_from_buffer = 0;
    if (m_token_buffer.size() >= 1) {
        previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }

    for (auto& token : tokens) {
        if (!previous_token.has_value()) {
            previous_token = token;
            continue;
        }

        if (previous_token->type == Token::Type::For && token.type == Token::Type::Word && is_valid_name(token.value)) {
            token.type = Token::Type::VariableName;
        }

        expanded_tokens.append(exchange(*previous_token, token));
    }

    swap_expansions();

    // (8) Function names
    previous_token = {};
    previous_previous_token = {};
    tokens_taken_from_buffer = 0;
    if (m_token_buffer.size() >= 1) {
        previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }
    if (m_token_buffer.size() >= 1) {
        previous_previous_token = m_token_buffer.take_last();
        tokens_taken_from_buffer++;
    }

    for (auto& token : tokens) {
        if (!previous_token.has_value()) {
            previous_token = token;
            continue;
        }
        if (!previous_previous_token.has_value()) {
            previous_previous_token = move(previous_token);
            previous_token = token;
            continue;
        }

        // NAME ( )
        if (previous_previous_token->could_be_start_of_a_simple_command
            && previous_previous_token->type == Token::Type::Word
            && previous_token->type == Token::Type::OpenParen
            && token.type == Token::Type::CloseParen) {

            previous_previous_token->type = Token::Type::VariableName;
        }

        expanded_tokens.append(exchange(*previous_previous_token, exchange(*previous_token, token)));
    }

    swap_expansions();

    return tokens;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_complete_command()
{
    auto list = TRY([&]() -> ErrorOr<RefPtr<AST::Node>> {
        // separator...
        while (is_separator(peek()))
            skip();

        // list EOF
        auto list = TRY(parse_list());
        if (eof())
            return list;

        // list separator EOF
        while (is_separator(peek()))
            skip();

        if (eof())
            return list;

        auto position = peek().position;
        auto syntax_error = make_ref_counted<AST::SyntaxError>(
            position.value_or(empty_position()),
            "Extra tokens after complete command"_string);

        if (list)
            list->set_is_syntax_error(*syntax_error);
        else
            list = syntax_error;

        return list;
    }());

    if (!list)
        return nullptr;

    return make_ref_counted<AST::Execute>(list->position(), *list);
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_list()
{
    Vector<NonnullRefPtr<AST::Node>> nodes;
    Vector<AST::Position> positions;

    auto start_position = peek().position.value_or(empty_position());

    for (;;) {
        auto new_node = TRY(parse_and_or());
        if (!new_node)
            break;

        if (peek().type == Token::Type::And) {
            new_node = make_ref_counted<AST::Background>(
                new_node->position(),
                *new_node);
        }

        nodes.append(new_node.release_nonnull());

        if (!is_separator(peek()) || eof())
            break;

        auto position = consume().position;
        if (position.has_value())
            positions.append(position.release_value());
    }

    auto end_position = peek().position.value_or(empty_position());

    return make_ref_counted<AST::Sequence>(
        AST::Position {
            start_position.start_offset,
            end_position.end_offset,
            start_position.start_line,
            end_position.end_line,
        },
        move(nodes),
        move(positions));
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_and_or()
{
    while (peek().type == Token::Type::Newline)
        skip();

    auto node = TRY(parse_pipeline());
    if (!node)
        return RefPtr<AST::Node> {};

    for (;;) {
        if (peek().type == Token::Type::AndIf) {
            auto and_token = consume();
            while (peek().type == Token::Type::Newline)
                skip();

            auto rhs = TRY(parse_pipeline());
            if (!rhs)
                return RefPtr<AST::Node> {};
            node = make_ref_counted<AST::And>(
                node->position(),
                *node,
                rhs.release_nonnull(),
                and_token.position.value_or(empty_position()));
            continue;
        }
        if (peek().type == Token::Type::OrIf) {
            auto or_token = consume();
            while (peek().type == Token::Type::Newline)
                skip();

            auto rhs = TRY(parse_pipeline());
            if (!rhs)
                return RefPtr<AST::Node> {};
            node = make_ref_counted<AST::Or>(
                node->position(),
                *node,
                rhs.release_nonnull(),
                or_token.position.value_or(empty_position()));
            continue;
        }
        break;
    }

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_pipeline()
{
    while (peek().type == Token::Type::Newline)
        skip();

    auto is_negated = false;
    if (peek().type == Token::Type::Bang) {
        is_negated = true;
        skip();
    }

    return parse_pipe_sequence(is_negated);
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_pipe_sequence(bool is_negated)
{
    auto node = TRY(parse_command());
    if (!node)
        return RefPtr<AST::Node> {};

    if (is_negated) {
        if (is<AST::CastToCommand>(node.ptr())) {
            node = make_ref_counted<AST::CastToCommand>(
                node->position(),
                make_ref_counted<AST::ListConcatenate>(
                    node->position(),
                    Vector<NonnullRefPtr<AST::Node>> {
                        make_ref_counted<AST::BarewordLiteral>(
                            node->position(),
                            "not"_string),
                        *static_cast<AST::CastToCommand&>(*node).inner() }));
        }
    }

    for (;;) {
        if (peek().type != Token::Type::Pipe)
            break;

        consume();
        while (peek().type == Token::Type::Newline)
            skip();

        auto rhs = TRY(parse_command());
        if (!rhs)
            return RefPtr<AST::Node> {};
        node = make_ref_counted<AST::Pipe>(
            node->position(),
            *node,
            rhs.release_nonnull());
    }

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_command()
{
    auto node = TRY([this]() -> ErrorOr<RefPtr<AST::Node>> {
        if (auto node = TRY(parse_function_definition()))
            return node;

        if (auto node = TRY(parse_simple_command()))
            return make_ref_counted<AST::CastToCommand>(node->position(), *node);

        auto node = TRY(parse_compound_command());
        if (!node)
            return node;

        if (auto list = TRY(parse_redirect_list())) {
            auto position = list->position();
            node = make_ref_counted<AST::Join>(
                node->position().with_end(position),
                *node,
                list.release_nonnull());
        }

        return node;
    }());

    if (!node)
        return nullptr;

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_function_definition()
{
    // NAME OPEN_PAREN CLOSE_PAREN newline* function_body

    auto start_index = m_token_index;
    ArmedScopeGuard reset = [&] {
        m_token_index = start_index;
    };

    if (peek().type != Token::Type::VariableName) {
        return nullptr;
    }

    auto name = consume();

    if (consume().type != Token::Type::OpenParen)
        return nullptr;

    if (consume().type != Token::Type::CloseParen)
        return nullptr;

    while (peek().type == Token::Type::Newline)
        skip();

    auto body = TRY(parse_function_body());

    if (!body)
        return nullptr;

    reset.disarm();

    return make_ref_counted<AST::FunctionDeclaration>(
        name.position.value_or(empty_position()).with_end(peek().position.value_or(empty_position())),
        AST::NameWithPosition { name.value, name.position.value_or(empty_position()) },
        Vector<AST::NameWithPosition> {},
        body.release_nonnull());
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_function_body()
{
    // compound_command redirect_list?

    auto node = TRY(parse_compound_command());
    if (!node)
        return nullptr;

    if (auto list = TRY(parse_redirect_list())) {
        auto position = list->position();
        node = make_ref_counted<AST::Join>(
            node->position().with_end(position),
            *node,
            list.release_nonnull());
    }

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_redirect_list()
{
    // io_redirect*

    RefPtr<AST::Node> node;

    for (;;) {
        auto new_node = TRY(parse_io_redirect());
        if (!new_node)
            break;

        if (node) {
            node = make_ref_counted<AST::Join>(
                node->position().with_end(new_node->position()),
                *node,
                new_node.release_nonnull());
        } else {
            node = new_node;
        }
    }

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_compound_command()
{
    if (auto node = TRY(parse_brace_group()))
        return node;

    if (auto node = TRY(parse_subshell()))
        return node;

    if (auto node = TRY(parse_if_clause()))
        return node;

    if (auto node = TRY(parse_for_clause()))
        return node;

    if (auto node = TRY(parse_case_clause()))
        return node;

    if (auto node = TRY(parse_while_clause()))
        return node;

    if (auto node = TRY(parse_until_clause()))
        return node;

    return nullptr;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_while_clause()
{
    if (peek().type != Token::Type::While)
        return nullptr;

    auto start_position = consume().position.value_or(empty_position());
    auto condition = TRY(parse_compound_list());
    if (!condition)
        condition = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            "Expected condition after 'while'"_string);

    auto do_group = TRY(parse_do_group());
    if (!do_group)
        do_group = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            "Expected 'do' after 'while'"_string);

    // while foo; bar -> loop { if foo { bar } else { break } }
    auto position = start_position.with_end(peek().position.value_or(empty_position()));
    return make_ref_counted<AST::ForLoop>(
        position,
        Optional<AST::NameWithPosition> {},
        Optional<AST::NameWithPosition> {},
        nullptr,
        make_ref_counted<AST::Execute>(position,
            make_ref_counted<AST::IfCond>(
                position,
                Optional<AST::Position> {},
                condition.release_nonnull(),
                do_group.release_nonnull(),
                make_ref_counted<AST::ContinuationControl>(
                    start_position,
                    AST::ContinuationControl::ContinuationKind::Break))));
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_until_clause()
{
    if (peek().type != Token::Type::Until)
        return nullptr;

    auto start_position = consume().position.value_or(empty_position());
    auto condition = TRY(parse_compound_list());
    if (!condition)
        condition = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            "Expected condition after 'until'"_string);

    auto do_group = TRY(parse_do_group());
    if (!do_group)
        do_group = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            "Expected 'do' after 'until'"_string);

    // until foo; bar -> loop { if foo { break } else { bar } }
    auto position = start_position.with_end(peek().position.value_or(empty_position()));
    return make_ref_counted<AST::ForLoop>(
        position,
        Optional<AST::NameWithPosition> {},
        Optional<AST::NameWithPosition> {},
        nullptr,
        make_ref_counted<AST::Execute>(position,
            make_ref_counted<AST::IfCond>(
                position,
                Optional<AST::Position> {},
                condition.release_nonnull(),
                make_ref_counted<AST::ContinuationControl>(
                    start_position,
                    AST::ContinuationControl::ContinuationKind::Break),
                do_group.release_nonnull())));
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_brace_group()
{
    if (peek().type != Token::Type::OpenBrace)
        return nullptr;

    consume();

    auto list = TRY(parse_compound_list());

    RefPtr<AST::SyntaxError> error;
    if (peek().type != Token::Type::CloseBrace) {
        error = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected '}}', not {}", peek().type_name())));
    } else {
        consume();
    }

    if (error) {
        if (list)
            list->set_is_syntax_error(*error);
        else
            list = error;
    }

    return make_ref_counted<AST::Execute>(list->position(), *list);
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_case_clause()
{
    auto start_position = peek().position.value_or(empty_position());
    if (peek().type != Token::Type::Case)
        return nullptr;

    skip();

    RefPtr<AST::SyntaxError> syntax_error;
    auto expr = TRY(parse_word());
    if (!expr)
        expr = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected a word, not {}", peek().type_name())));

    if (peek().type != Token::Type::In) {
        syntax_error = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected 'in', not {}", peek().type_name())));
    } else {
        skip();
    }

    while (peek().type == Token::Type::Newline)
        skip();

    Vector<AST::MatchEntry> entries;

    for (;;) {
        if (eof() || peek().type == Token::Type::Esac)
            break;

        if (peek().type == Token::Type::Newline) {
            skip();
            continue;
        }

        // Parse a pattern list
        auto needs_dsemi = true;
        if (peek().type == Token::Type::OpenParen) {
            skip();
            needs_dsemi = false;
        }

        auto result = TRY(parse_case_list());

        if (peek().type == Token::Type::CloseParen) {
            skip();
        } else {
            if (!syntax_error)
                syntax_error = make_ref_counted<AST::SyntaxError>(
                    peek().position.value_or(empty_position()),
                    TRY(String::formatted("Expected ')', not {}", peek().type_name())));
            break;
        }

        while (peek().type == Token::Type::Newline)
            skip();

        auto compound_list = TRY(parse_compound_list());

        if (peek().type == Token::Type::DoubleSemicolon) {
            skip();
        } else if (needs_dsemi) {
            if (!syntax_error)
                syntax_error = make_ref_counted<AST::SyntaxError>(
                    peek().position.value_or(empty_position()),
                    TRY(String::formatted("Expected ';;', not {}", peek().type_name())));
        }

        if (syntax_error) {
            if (compound_list)
                compound_list->set_is_syntax_error(*syntax_error);
            else
                compound_list = syntax_error;
            syntax_error = nullptr;
        }

        auto position = compound_list->position();
        entries.append(AST::MatchEntry {
            .options = move(result.nodes),
            .match_names = {},
            .match_as_position = {},
            .pipe_positions = move(result.pipe_positions),
            .body = make_ref_counted<AST::Execute>(position, compound_list.release_nonnull()),
        });
    }

    if (peek().type != Token::Type::Esac) {
        syntax_error = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected 'esac', not {}", peek().type_name())));
    } else {
        skip();
    }

    auto node = make_ref_counted<AST::MatchExpr>(
        start_position.with_end(peek().position.value_or(empty_position())),
        expr.release_nonnull(),
        String {},
        Optional<AST::Position> {},
        move(entries));

    if (syntax_error)
        node->set_is_syntax_error(*syntax_error);

    return node;
}

ErrorOr<Parser::CaseItemsResult> Parser::parse_case_list()
{
    // Just a list of words split by '|', delimited by ')'
    Vector<NonnullRefPtr<AST::Node>> nodes;
    Vector<AST::Position> pipes;

    for (;;) {
        if (eof() || peek().type == Token::Type::CloseParen)
            break;

        if (peek().type != Token::Type::Word)
            break;

        auto node = TRY(parse_word());
        if (!node)
            node = make_ref_counted<AST::SyntaxError>(
                peek().position.value_or(empty_position()),
                TRY(String::formatted("Expected a word, not {}", peek().type_name())));

        nodes.append(node.release_nonnull());

        if (peek().type == Token::Type::Pipe) {
            pipes.append(peek().position.value_or(empty_position()));
            skip();
        } else {
            break;
        }
    }

    if (nodes.is_empty())
        nodes.append(make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected a word, not {}", peek().type_name()))));

    return CaseItemsResult { move(pipes), move(nodes) };
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_if_clause()
{
    // If compound_list Then compound_list {Elif compound_list Then compound_list (Fi|Else)?} [(?=Else) compound_list] (?!=Fi) Fi
    auto start_position = peek().position.value_or(empty_position());
    if (peek().type != Token::Type::If)
        return nullptr;

    skip();
    auto main_condition = TRY(parse_compound_list());
    if (!main_condition)
        main_condition = make_ref_counted<AST::SyntaxError>(empty_position(), "Expected compound list after 'if'"_string);

    RefPtr<AST::SyntaxError> syntax_error;
    if (peek().type != Token::Type::Then) {
        syntax_error = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected 'then', not {}", peek().type_name())));
    } else {
        skip();
    }

    auto main_consequence = TRY(parse_compound_list());
    if (!main_consequence)
        main_consequence = make_ref_counted<AST::SyntaxError>(empty_position(), "Expected compound list after 'then'"_string);

    auto node = make_ref_counted<AST::IfCond>(start_position, Optional<AST::Position>(), main_condition.release_nonnull(), main_consequence.release_nonnull(), nullptr);
    auto active_node = node;

    while (peek().type == Token::Type::Elif) {
        skip();
        auto condition = TRY(parse_compound_list());
        if (!condition)
            condition = make_ref_counted<AST::SyntaxError>(empty_position(), "Expected compound list after 'elif'"_string);

        if (peek().type != Token::Type::Then) {
            if (!syntax_error)
                syntax_error = make_ref_counted<AST::SyntaxError>(
                    peek().position.value_or(empty_position()),
                    TRY(String::formatted("Expected 'then', not {}", peek().type_name())));
        } else {
            skip();
        }

        auto consequence = TRY(parse_compound_list());
        if (!consequence)
            consequence = make_ref_counted<AST::SyntaxError>(empty_position(), "Expected compound list after 'then'"_string);

        auto new_node = make_ref_counted<AST::IfCond>(start_position, Optional<AST::Position>(), condition.release_nonnull(), consequence.release_nonnull(), nullptr);

        active_node->false_branch() = new_node;
        active_node = move(new_node);
    }

    auto needs_fi = true;
    switch (peek().type) {
    case Token::Type::Else:
        skip();
        active_node->false_branch() = TRY(parse_compound_list());
        if (!active_node->false_branch())
            active_node->false_branch() = make_ref_counted<AST::SyntaxError>(empty_position(), "Expected compound list after 'else'"_string);
        break;
    case Token::Type::Fi:
        skip();
        needs_fi = false;
        break;
    default:
        if (!syntax_error)
            syntax_error = make_ref_counted<AST::SyntaxError>(
                peek().position.value_or(empty_position()),
                TRY(String::formatted("Expected 'else' or 'fi', not {}", peek().type_name())));
        break;
    }

    if (needs_fi) {
        if (peek().type != Token::Type::Fi) {
            if (!syntax_error)
                syntax_error = make_ref_counted<AST::SyntaxError>(
                    peek().position.value_or(empty_position()),
                    TRY(String::formatted("Expected 'fi', not {}", peek().type_name())));
        } else {
            skip();
        }
    }

    if (syntax_error)
        node->set_is_syntax_error(*syntax_error);

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_subshell()
{
    auto start_position = peek().position.value_or(empty_position());
    if (peek().type != Token::Type::OpenParen)
        return nullptr;

    skip();
    RefPtr<AST::SyntaxError> error;

    auto list = TRY(parse_compound_list());
    if (!list)
        error = make_ref_counted<AST::SyntaxError>(peek().position.value_or(empty_position()), "Expected compound list after ("_string);

    if (peek().type != Token::Type::CloseParen)
        error = make_ref_counted<AST::SyntaxError>(peek().position.value_or(empty_position()), "Expected ) after compound list"_string);
    else
        skip();

    if (!list)
        return error;

    return make_ref_counted<AST::Subshell>(
        start_position.with_end(peek().position.value_or(empty_position())),
        list.release_nonnull());
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_compound_list()
{
    while (peek().type == Token::Type::Newline)
        skip();

    auto term = TRY(parse_term());
    if (!term)
        return term;

    if (is_separator(peek())) {
        if (consume().type == Token::Type::And) {
            term = make_ref_counted<AST::Background>(
                term->position().with_end(peek().position.value_or(empty_position())),
                *term);
        }
    }

    return term;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_term()
{
    Vector<NonnullRefPtr<AST::Node>> nodes;
    Vector<AST::Position> positions;

    auto start_position = peek().position.value_or(empty_position());

    for (;;) {
        auto new_node = TRY(parse_and_or());
        if (!new_node)
            break;

        nodes.append(new_node.release_nonnull());

        if (!is_separator(peek()))
            break;

        auto position = consume().position;
        if (position.has_value())
            positions.append(position.release_value());
    }

    auto end_position = peek().position.value_or(empty_position());

    return make_ref_counted<AST::Sequence>(
        start_position.with_end(end_position),
        move(nodes),
        move(positions));
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_for_clause()
{
    // FOR NAME newline+ do_group //-> FOR NAME IN "$@" newline+ do_group
    // FOR NAME newline+ IN separator do_group
    // FOR NAME IN separator do_group
    // FOR NAME IN wordlist separator do_group

    if (peek().type != Token::Type::For)
        return nullptr;

    auto start_position = consume().position.value_or(empty_position());

    String name;
    Optional<AST::Position> name_position;
    if (peek().type == Token::Type::VariableName) {
        name_position = peek().position;
        name = consume().value;
    } else {
        name = "it"_string;
        error(peek(), "Expected a variable name, not {}", peek().type_name());
    }

    auto saw_newline = false;
    while (peek().type == Token::Type::Newline) {
        saw_newline = true;
        skip();
    }

    auto saw_in = false;
    Optional<AST::Position> in_kw_position;
    RefPtr<AST::Node> iterated_expression;

    if (peek().type == Token::Type::In) {
        saw_in = true;
        in_kw_position = peek().position;
        skip();
    } else if (!saw_newline) {
        error(peek(), "Expected 'in' or a newline, not {}", peek().type_name());
    } else {
        // FOR NAME newline+ do_group //-> FOR NAME IN "$@" newline+ do_group
        iterated_expression = TRY(Parser { "\"$@\""_string }.parse_word());
    }

    if (saw_in && !saw_newline) {
        if (auto list = parse_word_list())
            iterated_expression = reexpand(peek().position.value_or(empty_position()), list.release_nonnull());
    }

    if (saw_in) {
        if (peek().type == Token::Type::Semicolon || peek().type == Token::Type::Newline)
            skip();
        else
            error(peek(), "Expected a semicolon, not {}", peek().type_name());
    }

    while (peek().type == Token::Type::Newline)
        skip();

    auto body = TRY(parse_do_group());
    return AST::make_ref_counted<AST::ForLoop>(
        start_position.with_end(peek().position.value_or(empty_position())),
        AST::NameWithPosition { name, name_position.value_or(empty_position()) },
        Optional<AST::NameWithPosition> {},
        move(iterated_expression),
        move(body),
        move(in_kw_position),
        Optional<AST::Position> {});
}

RefPtr<AST::Node> Parser::parse_word_list(AllowNewlines allow_newlines)
{
    Vector<NonnullRefPtr<AST::Node>> nodes;

    auto start_position = peek().position.value_or(empty_position());

    if (allow_newlines == AllowNewlines::Yes) {
        while (peek().type == Token::Type::Newline)
            skip();
    }

    for (; peek().type == Token::Type::Word;) {
        auto word = TRY_OR_THROW_PARSE_ERROR_AT(parse_word(), start_position);
        nodes.append(word.release_nonnull());
        if (allow_newlines == AllowNewlines::Yes) {
            while (peek().type == Token::Type::Newline)
                skip();
        }
    }

    return make_ref_counted<AST::ListConcatenate>(
        start_position.with_end(peek().position.value_or(empty_position())),
        move(nodes));
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_word()
{
    if (peek().type != Token::Type::Word)
        return nullptr;

    auto token = consume();
    RefPtr<AST::Node> word;

    enum class Quote {
        None,
        Single,
        Double,
    } in_quote { Quote::None };

    auto append_bareword = [&](StringView string) -> ErrorOr<void> {
        if (!word && string.starts_with('~')) {
            GenericLexer lexer { string };
            lexer.ignore();
            auto user = lexer.consume_while(is_ascii_alphanumeric);
            string = lexer.remaining();

            word = make_ref_counted<AST::Tilde>(token.position.value_or(empty_position()), TRY(String::from_utf8(user)));
        }

        if (string.is_empty())
            return {};

        auto node = make_ref_counted<AST::BarewordLiteral>(
            token.position.value_or(empty_position()),
            TRY(String::from_utf8(string)));

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position().with_end(token.position.value_or(empty_position())),
                *word,
                move(node),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(node);
        }

        return {};
    };

    auto append_string_literal = [&](StringView string) -> ErrorOr<void> {
        auto node = make_ref_counted<AST::StringLiteral>(
            token.position.value_or(empty_position()),
            TRY(String::from_utf8(string)),
            AST::StringLiteral::EnclosureType::SingleQuotes);

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position().with_end(token.position.value_or(empty_position())),
                *word,
                move(node),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(node);
        }

        return {};
    };

    auto append_string_part = [&](StringView string) -> ErrorOr<void> {
        auto node = make_ref_counted<AST::StringLiteral>(
            token.position.value_or(empty_position()),
            TRY(String::from_utf8(string)),
            AST::StringLiteral::EnclosureType::DoubleQuotes);

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position().with_end(token.position.value_or(empty_position())),
                *word,
                move(node),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(node);
        }

        return {};
    };

    auto append_arithmetic_expansion = [&](ResolvedArithmeticExpansion const& x) -> ErrorOr<void> {
        auto node = immediate(
            "math"_string,
            token.position.value_or(empty_position()),
            reexpand(
                token.position.value_or(empty_position()),
                make_ref_counted<AST::StringLiteral>(
                    token.position.value_or(empty_position()),
                    x.source_expression,
                    AST::StringLiteral::EnclosureType::DoubleQuotes)));

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position().with_end(token.position.value_or(empty_position())),
                *word,
                move(node),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(node);
        }

        return {};
    };

    auto append_parameter_expansion = [&](ResolvedParameterExpansion const& x) -> ErrorOr<void> {
        StringView immediate_function_name;
        RefPtr<AST::Node> node;
        switch (x.op) {
        case ResolvedParameterExpansion::Op::UseDefaultValue:
            immediate_function_name = "value_or_default"sv;
            break;
        case ResolvedParameterExpansion::Op::AssignDefaultValue:
            immediate_function_name = "assign_default"sv;
            break;
        case ResolvedParameterExpansion::Op::IndicateErrorIfEmpty:
            immediate_function_name = "error_if_empty"sv;
            break;
        case ResolvedParameterExpansion::Op::UseAlternativeValue:
            immediate_function_name = "null_or_alternative"sv;
            break;
        case ResolvedParameterExpansion::Op::UseDefaultValueIfUnset:
            immediate_function_name = "defined_value_or_default"sv;
            break;
        case ResolvedParameterExpansion::Op::AssignDefaultValueIfUnset:
            immediate_function_name = "assign_defined_default"sv;
            break;
        case ResolvedParameterExpansion::Op::IndicateErrorIfUnset:
            immediate_function_name = "error_if_unset"sv;
            break;
        case ResolvedParameterExpansion::Op::UseAlternativeValueIfUnset:
            immediate_function_name = "null_if_unset_or_alternative"sv;
            break;
        case ResolvedParameterExpansion::Op::RemoveLargestSuffixByPattern:
            // FIXME: Implement this
        case ResolvedParameterExpansion::Op::RemoveSmallestSuffixByPattern:
            immediate_function_name = "remove_suffix"sv;
            break;
        case ResolvedParameterExpansion::Op::RemoveLargestPrefixByPattern:
            // FIXME: Implement this
        case ResolvedParameterExpansion::Op::RemoveSmallestPrefixByPattern:
            immediate_function_name = "remove_prefix"sv;
            break;
        case ResolvedParameterExpansion::Op::StringLength:
            immediate_function_name = "length_of_variable"sv;
            break;
        case ResolvedParameterExpansion::Op::GetPositionalParameter:
        case ResolvedParameterExpansion::Op::GetVariable:
            node = make_ref_counted<AST::SimpleVariable>(
                token.position.value_or(empty_position()),
                x.parameter);
            break;
        case ResolvedParameterExpansion::Op::GetLastBackgroundPid:
            node = make_ref_counted<AST::SyntaxError>(
                token.position.value_or(empty_position()),
                "$! not implemented"_string);
            break;
        case ResolvedParameterExpansion::Op::GetPositionalParameterList:
            node = make_ref_counted<AST::SpecialVariable>(
                token.position.value_or(empty_position()),
                '*');
            break;
        case ResolvedParameterExpansion::Op::GetCurrentOptionFlags:
            node = make_ref_counted<AST::SyntaxError>(
                token.position.value_or(empty_position()),
                "The current option flags are not available in parameter expansions"_string);
            break;
        case ResolvedParameterExpansion::Op::GetPositionalParameterCount:
            node = make_ref_counted<AST::SpecialVariable>(
                token.position.value_or(empty_position()),
                '#');
            break;
        case ResolvedParameterExpansion::Op::GetLastExitStatus:
            node = make_ref_counted<AST::SpecialVariable>(
                token.position.value_or(empty_position()),
                '?');
            break;
        case ResolvedParameterExpansion::Op::GetPositionalParameterListAsString:
            node = strip_execute(::Shell::Parser { "${join \"${defined_value_or_default IFS ' '}\" $*}"sv }.parse());
            break;
        case ResolvedParameterExpansion::Op::GetShellProcessId:
            node = make_ref_counted<AST::SpecialVariable>(
                token.position.value_or(empty_position()),
                '$');
            break;
        }

        if (!node) {
            Vector<NonnullRefPtr<AST::Node>> arguments;
            arguments.append(make_ref_counted<AST::BarewordLiteral>(
                token.position.value_or(empty_position()),
                x.parameter));

            if (!x.argument.is_empty()) {
                // dbgln("Will parse {}", x.argument);
                arguments.append(*TRY(Parser { x.argument }.parse_word()));
            }

            node = immediate(TRY(String::from_utf8(immediate_function_name)), token.position.value_or(empty_position()), move(arguments));
        }

        if (x.expand == ResolvedParameterExpansion::Expand::Word)
            node = reexpand(token.position.value_or(empty_position()), node.release_nonnull());

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position().with_end(token.position.value_or(empty_position())),
                *word,
                node.release_nonnull(),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(node);
        }

        return {};
    };

    auto append_command_expansion = [&](ResolvedCommandExpansion const& x) -> ErrorOr<void> {
        if (!x.command)
            return {};

        RefPtr<AST::Execute> execute_node;

        if (x.command->is_execute()) {
            execute_node = const_cast<AST::Execute&>(static_cast<AST::Execute const&>(*x.command));
            execute_node->capture_stdout();
        } else {
            execute_node = make_ref_counted<AST::Execute>(
                word ? word->position() : empty_position(),
                *x.command,
                true);
        }

        if (word) {
            word = make_ref_counted<AST::Juxtaposition>(
                word->position(),
                *word,
                execute_node.release_nonnull(),
                AST::Juxtaposition::Mode::StringExpand);
        } else {
            word = move(execute_node);
        }

        return {};
    };

    auto append_string = [&](StringView string) -> ErrorOr<void> {
        if (string.is_empty())
            return {};

        Optional<size_t> run_start;
        auto escape = false;
        for (size_t i = 0; i < string.length(); ++i) {
            auto ch = string[i];
            switch (ch) {
            case '\\':
                if (!escape && i + 1 < string.length()) {
                    if (run_start.has_value())
                        TRY(append_string_literal(string.substring_view(*run_start, i - *run_start)));
                    run_start = i + 1;

                    if (is_one_of(string[i + 1], '"', '\'', '$', '`', '\\')) {
                        escape = in_quote != Quote::Single;
                        continue;
                    }
                }
                break;
            case '\'':
                if (in_quote == Quote::Single) {
                    in_quote = Quote::None;
                    if (run_start.has_value())
                        TRY(append_string_literal(string.substring_view(*run_start, i - *run_start)));
                    run_start = i + 1;
                    continue;
                }
                if (in_quote == Quote::Double) {
                    escape = false;
                    continue;
                }
                [[fallthrough]];
            case '"':
                if (ch == '\'' && in_quote == Quote::Single) {
                    escape = false;
                    continue;
                }
                if (!escape) {
                    if (ch == '"' && in_quote == Quote::Double) {
                        in_quote = Quote::None;
                        if (run_start.has_value())
                            TRY(append_string_part(string.substring_view(*run_start, i - *run_start)));
                        run_start = i + 1;
                        continue;
                    }
                    if (run_start.has_value())
                        TRY(append_bareword(string.substring_view(*run_start, i - *run_start)));
                    in_quote = ch == '\'' ? Quote::Single : Quote::Double;
                    run_start = i + 1;
                }
                escape = false;
                [[fallthrough]];
            default:
                if (!run_start.has_value())
                    run_start = i;
                escape = false;
                continue;
            }
        }

        if (run_start.has_value())
            TRY(append_bareword(string.substring_view(*run_start, string.length() - *run_start)));

        return {};
    };

    if (!token.resolved_expansions.is_empty())
        dbgln_if(SHELL_POSIX_PARSER_DEBUG, "Expanding '{}' with {} expansion entries", token.value, token.resolved_expansions.size());

    size_t current_offset = 0;
    auto value_bytes = token.value.bytes_as_string_view();
    for (auto& expansion : token.resolved_expansions) {
        TRY(expansion.visit(
            [&](ResolvedParameterExpansion const& x) -> ErrorOr<void> {
                dbgln_if(SHELL_POSIX_PARSER_DEBUG, "    Expanding '{}' ({}+{})", x.to_byte_string(), x.range.start, x.range.length);
                if (x.range.start >= value_bytes.length()) {
                    dbgln("Parameter expansion range {}-{} is out of bounds for '{}'", x.range.start, x.range.length, value_bytes);
                    return {};
                }

                if (x.range.start != current_offset) {
                    TRY(append_string(value_bytes.substring_view(current_offset, x.range.start - current_offset)));
                    current_offset = x.range.start;
                }
                current_offset += x.range.length;
                return append_parameter_expansion(x);
            },
            [&](ResolvedArithmeticExpansion const& x) -> ErrorOr<void> {
                if (x.range.start >= value_bytes.length()) {
                    dbgln("Parameter expansion range {}-{} is out of bounds for '{}'", x.range.start, x.range.length, value_bytes);
                    return {};
                }

                if (x.range.start != current_offset) {
                    TRY(append_string(value_bytes.substring_view(current_offset, x.range.start - current_offset)));
                    current_offset = x.range.start;
                }
                current_offset += x.range.length;
                return append_arithmetic_expansion(x);
            },
            [&](ResolvedCommandExpansion const& x) -> ErrorOr<void> {
                if (x.range.start >= value_bytes.length()) {
                    dbgln("Parameter expansion range {}-{} is out of bounds for '{}'", x.range.start, x.range.length, value_bytes);
                    return {};
                }

                if (x.range.start != current_offset) {
                    TRY(append_string(value_bytes.substring_view(current_offset, x.range.start - current_offset)));
                    current_offset = x.range.start;
                }
                current_offset += x.range.length;
                return append_command_expansion(x);
            }));
    }

    if (current_offset > value_bytes.length()) {
        dbgln("Parameter expansion range {}- is out of bounds for '{}'", current_offset, value_bytes);
        return word;
    }

    if (current_offset != value_bytes.length())
        TRY(append_string(value_bytes.substring_view(current_offset)));

    return word;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_bash_like_list()
{
    if (peek().type != Token::Type::OpenParen)
        return nullptr;

    auto start_position = peek().position.value_or(empty_position());
    consume();

    auto list = parse_word_list(AllowNewlines::Yes);

    if (peek().type != Token::Type::CloseParen) {
        return make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected ')', not {}", peek().type_name())));
    }

    consume();

    if (list)
        list->position() = start_position.with_end(peek().position.value_or(empty_position()));
    else
        list = make_ref_counted<AST::ListConcatenate>(start_position.with_end(peek().position.value_or(empty_position())), Vector<NonnullRefPtr<AST::Node>> {});

    return list;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_do_group()
{
    if (peek().type != Token::Type::Do) {
        return make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected 'do', not {}", peek().type_name())));
    }

    consume();

    auto list = TRY(parse_compound_list());

    RefPtr<AST::SyntaxError> error;
    if (peek().type != Token::Type::Done) {
        error = make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            TRY(String::formatted("Expected 'done', not {}", peek().type_name())));
    } else {
        consume();
    }

    if (error) {
        if (list)
            list->set_is_syntax_error(*error);
        else
            list = error;
    }

    return make_ref_counted<AST::Execute>(list->position(), *list);
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_simple_command()
{
    auto start_position = peek().position.value_or(empty_position());

    Vector<String> definitions;
    HashMap<String, NonnullRefPtr<AST::Node>> list_assignments;
    Vector<NonnullRefPtr<AST::Node>> nodes;

    for (;;) {
        if (auto io_redirect = TRY(parse_io_redirect()))
            nodes.append(*io_redirect);
        else
            break;
    }

    while (is_one_of(peek().type, Token::Type::ListAssignmentWord, Token::Type::AssignmentWord)) {
        if (peek().type == Token::Type::ListAssignmentWord) {
            auto token = consume();
            auto value = TRY(parse_bash_like_list());
            if (!value)
                return make_ref_counted<AST::SyntaxError>(
                    token.position.value_or(empty_position()),
                    TRY(String::formatted("Expected a list literal after '{}', not {}", token.value, peek().type_name())));

            list_assignments.set(token.value, value.release_nonnull());
            continue;
        }

        definitions.append(peek().value);

        if (nodes.is_empty()) {
            // run_with_env -e*(assignments) -- (command)
            nodes.append(make_ref_counted<AST::BarewordLiteral>(
                empty_position(),
                "run_with_env"_string));
        }

        auto position = peek().position.value_or(empty_position());
        nodes.append(reexpand(position, make_ref_counted<AST::StringLiteral>(position, TRY(String::formatted("-e{}", consume().value)), AST::StringLiteral::EnclosureType::DoubleQuotes)));
    }

    if (!definitions.is_empty()) {
        nodes.append(make_ref_counted<AST::BarewordLiteral>(
            empty_position(),
            "--"_string));
    }

    // WORD or io_redirect: IO_NUMBER or io_file
    if (!is_one_of(peek().type,
            Token::Type::Word, Token::Type::IoNumber,
            Token::Type::Less, Token::Type::LessAnd, Token::Type::Great, Token::Type::GreatAnd,
            Token::Type::DoubleGreat, Token::Type::LessGreat, Token::Type::Clobber)) {
        if (!definitions.is_empty() || !list_assignments.is_empty()) {
            Vector<AST::VariableDeclarations::Variable> variables;
            for (auto& definition : definitions) {
                auto equal_offset = definition.find_byte_offset('=');
                auto split_offset = equal_offset.value_or(definition.bytes().size());
                auto name = make_ref_counted<AST::BarewordLiteral>(
                    empty_position(),
                    TRY(definition.substring_from_byte_offset_with_shared_superstring(0, split_offset)));

                auto position = peek().position.value_or(empty_position());
                auto expanded_value = reexpand(position, make_ref_counted<AST::StringLiteral>(position, TRY(definition.substring_from_byte_offset_with_shared_superstring(split_offset + 1)), AST::StringLiteral::EnclosureType::DoubleQuotes));

                variables.append({ move(name), move(expanded_value) });
            }
            for (auto& [key, value] : list_assignments) {
                auto equal_offset = key.find_byte_offset('=');
                auto split_offset = equal_offset.value_or(key.bytes().size());
                auto name = make_ref_counted<AST::BarewordLiteral>(
                    empty_position(),
                    TRY(key.substring_from_byte_offset_with_shared_superstring(0, split_offset)));

                variables.append({ move(name), move(value) });
            }

            return make_ref_counted<AST::VariableDeclarations>(empty_position(), move(variables));
        }
        return nullptr;
    }

    if (!list_assignments.is_empty()) {
        return make_ref_counted<AST::SyntaxError>(
            peek().position.value_or(empty_position()),
            "List assignments are not allowed as a command prefix"_string);
    }

    // auto first = true;
    for (;;) {
        if (peek().type == Token::Type::Word) {
            auto new_word = TRY(parse_word());
            if (!new_word)
                break;

            nodes.append(new_word.release_nonnull());
        } else if (auto io_redirect = TRY(parse_io_redirect())) {
            nodes.append(io_redirect.release_nonnull());
        } else {
            break;
        }
    }

    auto node = make_ref_counted<AST::ListConcatenate>(
        start_position.with_end(peek().position.value_or(empty_position())),
        move(nodes));

    return node;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_io_redirect()
{
    auto start_position = peek().position.value_or(empty_position());
    auto start_index = m_token_index;

    // io_redirect: IO_NUMBER? io_file | IO_NUMBER? io_here
    Optional<int> io_number;

    if (peek().type == Token::Type::IoNumber)
        io_number = consume().value.to_number<int>();

    if (auto io_file = TRY(parse_io_file(start_position, io_number)))
        return io_file;

    if (auto io_here = TRY(parse_io_here(start_position, io_number)))
        return io_here;

    m_token_index = start_index;
    return nullptr;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_io_here(AST::Position start_position, Optional<int> fd)
{
    // io_here: IO_NUMBER? (DLESS | DLESSDASH) WORD
    auto io_operator = peek().type;
    if (!is_one_of(io_operator, Token::Type::DoubleLess, Token::Type::DoubleLessDash))
        return nullptr;

    auto io_operator_token = consume();

    auto redirection_fd = fd.value_or(0);

    auto end_keyword = consume();
    if (!is_one_of(end_keyword.type, Token::Type::Word, Token::Type::Token))
        return make_ref_counted<AST::SyntaxError>(io_operator_token.position.value_or(start_position), "Expected a heredoc keyword"_string, true);

    auto [end_keyword_text, allow_interpolation] = Lexer::process_heredoc_key(end_keyword);
    RefPtr<AST::SyntaxError> error;

    auto position = start_position.with_end(peek().position.value_or(empty_position()));
    auto result = make_ref_counted<AST::Heredoc>(
        position,
        end_keyword_text,
        allow_interpolation,
        io_operator == Token::Type::DoubleLessDash,
        Optional<int> { redirection_fd });

    m_unprocessed_heredoc_entries.set(end_keyword_text, result);

    if (error)
        result->set_is_syntax_error(*error);

    return result;
}

ErrorOr<RefPtr<AST::Node>> Parser::parse_io_file(AST::Position start_position, Optional<int> fd)
{
    auto start_index = m_token_index;

    // io_file = (LESS | LESSAND | GREAT | GREATAND | DGREAT | LESSGREAT | CLOBBER) WORD
    auto io_operator = peek().type;
    if (!is_one_of(io_operator,
            Token::Type::Less, Token::Type::LessAnd, Token::Type::Great, Token::Type::GreatAnd,
            Token::Type::DoubleGreat, Token::Type::LessGreat, Token::Type::Clobber))
        return nullptr;

    auto io_operator_token = consume();

    RefPtr<AST::Node> word;
    if (peek().type == Token::Type::IoNumber) {
        auto token = consume();
        word = make_ref_counted<AST::BarewordLiteral>(
            token.position.value_or(empty_position()),
            token.value);
    } else {
        word = TRY(parse_word());
    }

    if (!word) {
        m_token_index = start_index;
        return nullptr;
    }

    auto position = start_position.with_end(peek().position.value_or(empty_position()));
    switch (io_operator) {
    case Token::Type::Less:
        return make_ref_counted<AST::ReadRedirection>(
            position,
            fd.value_or(0),
            word.release_nonnull());
    case Token::Type::Clobber:
        // FIXME: Add support for clobber (and 'noclobber')
    case Token::Type::Great:
        return make_ref_counted<AST::WriteRedirection>(
            position,
            fd.value_or(1),
            word.release_nonnull());
    case Token::Type::DoubleGreat:
        return make_ref_counted<AST::WriteAppendRedirection>(
            position,
            fd.value_or(1),
            word.release_nonnull());
    case Token::Type::LessGreat:
        return make_ref_counted<AST::ReadWriteRedirection>(
            position,
            fd.value_or(0),
            word.release_nonnull());
    case Token::Type::LessAnd:
    case Token::Type::GreatAnd: {
        auto is_less = io_operator == Token::Type::LessAnd;
        auto source_fd = fd.value_or(is_less ? 0 : 1);
        if (word->is_bareword()) {
            auto text = static_ptr_cast<AST::BarewordLiteral>(word)->text();
            if (!is_less && text == "-"sv) {
                return make_ref_counted<AST::CloseFdRedirection>(
                    position,
                    source_fd);
            }

            auto maybe_target_fd = text.to_number<int>();
            if (maybe_target_fd.has_value()) {
                auto target_fd = maybe_target_fd.release_value();
                if (is_less)
                    swap(source_fd, target_fd);

                return make_ref_counted<AST::Fd2FdRedirection>(
                    position,
                    source_fd,
                    target_fd);
            }
        }
        if (is_less) {
            return make_ref_counted<AST::ReadRedirection>(
                position,
                source_fd,
                word.release_nonnull());
        }

        return make_ref_counted<AST::WriteRedirection>(
            position,
            source_fd,
            word.release_nonnull());
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

}
