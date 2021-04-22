/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/StandardPaths.h>
#include <LibLine/Editor.h>
#include <LibSQL/Lexer.h>
#include <LibSQL/Parser.h>
#include <LibSQL/Token.h>

namespace {

String s_history_path = String::formatted("{}/.sql-history", Core::StandardPaths::home_directory());
RefPtr<Line::Editor> s_editor;
int s_repl_line_level = 0;
bool s_keep_running = true;

String prompt_for_level(int level)
{
    static StringBuilder prompt_builder;
    prompt_builder.clear();
    prompt_builder.append("> ");

    for (auto i = 0; i < level; ++i)
        prompt_builder.append("    ");

    return prompt_builder.build();
}

String read_next_piece()
{
    StringBuilder piece;

    do {
        if (!piece.is_empty())
            piece.append('\n');

        auto line_result = s_editor->get_line(prompt_for_level(s_repl_line_level));

        if (line_result.is_error()) {
            s_keep_running = false;
            return {};
        }

        auto& line = line_result.value();
        auto lexer = SQL::Lexer(line);

        s_editor->add_to_history(line);
        piece.append(line);

        bool is_first_token = true;
        bool is_command = false;
        bool last_token_ended_statement = false;

        for (SQL::Token token = lexer.next(); token.type() != SQL::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case SQL::TokenType::ParenOpen:
                ++s_repl_line_level;
                break;
            case SQL::TokenType::ParenClose:
                --s_repl_line_level;
                break;
            case SQL::TokenType::SemiColon:
                last_token_ended_statement = true;
                break;
            case SQL::TokenType::Period:
                if (is_first_token)
                    is_command = true;
                break;
            default:
                last_token_ended_statement = is_command;
                break;
            }

            is_first_token = false;
        }

        s_repl_line_level = last_token_ended_statement ? 0 : (s_repl_line_level > 0 ? s_repl_line_level : 1);
    } while (s_repl_line_level > 0);

    return piece.to_string();
}

void handle_command(StringView command)
{
    if (command == ".exit")
        s_keep_running = false;
    else
        outln("\033[33;1mUnrecognized command:\033[0m {}", command);
}

void handle_statement(StringView statement_string)
{
    auto parser = SQL::Parser(SQL::Lexer(statement_string));
    [[maybe_unused]] auto statement = parser.next_statement();

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        outln("\033[33;1mInvalid statement:\033[0m {}", error.to_string());
    }
}

void repl()
{
    while (s_keep_running) {
        String piece = read_next_piece();
        if (piece.is_empty())
            continue;

        if (piece.starts_with('.'))
            handle_command(piece);
        else
            handle_statement(piece);
    }
}

}

int main()
{
    s_editor = Line::Editor::construct();
    s_editor->load_history(s_history_path);

    s_editor->on_display_refresh = [](Line::Editor& editor) {
        editor.strip_styles();

        size_t open_indents = s_repl_line_level;

        auto line = editor.line();
        SQL::Lexer lexer(line);

        bool indenters_starting_line = true;
        for (SQL::Token token = lexer.next(); token.type() != SQL::TokenType::Eof; token = lexer.next()) {
            auto length = token.value().length();
            auto start = token.line_column() - 1;
            auto end = start + length;

            if (indenters_starting_line) {
                if (token.type() != SQL::TokenType::ParenClose)
                    indenters_starting_line = false;
                else
                    --open_indents;
            }

            switch (token.category()) {
            case SQL::TokenCategory::Invalid:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Underline });
                break;
            case SQL::TokenCategory::Number:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta) });
                break;
            case SQL::TokenCategory::String:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Green), Line::Style::Bold });
                break;
            case SQL::TokenCategory::Blob:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta), Line::Style::Bold });
                break;
            case SQL::TokenCategory::Keyword:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Bold });
                break;
            case SQL::TokenCategory::Identifier:
                editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::White), Line::Style::Bold });
            default:
                break;
            }
        }

        editor.set_prompt(prompt_for_level(open_indents));
    };

    repl();
    s_editor->save_history(s_history_path);

    return 0;
}
