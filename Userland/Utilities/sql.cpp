/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibLine/Editor.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Token.h>
#include <LibSQL/SQLClient.h>
#include <unistd.h>

class SQLRepl {
public:
    explicit SQLRepl(String const& database_name)
        : m_loop()
    {
        m_editor = Line::Editor::construct();
        m_editor->load_history(m_history_path);

        m_editor->on_display_refresh = [this](Line::Editor& editor) {
            editor.strip_styles();

            int open_indents = m_repl_line_level;

            auto line = editor.line();
            SQL::AST::Lexer lexer(line);

            bool indenters_starting_line = true;
            for (SQL::AST::Token token = lexer.next(); token.type() != SQL::AST::TokenType::Eof; token = lexer.next()) {
                auto start = token.start_position().column - 1;
                auto end = token.end_position().column - 1;

                if (indenters_starting_line) {
                    if (token.type() != SQL::AST::TokenType::ParenClose)
                        indenters_starting_line = false;
                    else
                        --open_indents;
                }

                switch (token.category()) {
                case SQL::AST::TokenCategory::Invalid:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Underline });
                    break;
                case SQL::AST::TokenCategory::Number:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta) });
                    break;
                case SQL::AST::TokenCategory::String:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Green), Line::Style::Bold });
                    break;
                case SQL::AST::TokenCategory::Blob:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta), Line::Style::Bold });
                    break;
                case SQL::AST::TokenCategory::Keyword:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Bold });
                    break;
                case SQL::AST::TokenCategory::Identifier:
                    editor.stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::White), Line::Style::Bold });
                    break;
                default:
                    break;
                }
            }

            m_editor->set_prompt(prompt_for_level(open_indents));
        };

        m_sql_client = SQL::SQLClient::construct();

        m_sql_client->on_connected = [this](int connection_id, String const& connected_to_database) {
            outln("Connected to \033[33;1m{}\033[0m", connected_to_database);
            m_current_database = connected_to_database;
            m_pending_database = "";
            m_connection_id = connection_id;
            read_sql();
        };

        m_sql_client->on_execution_success = [this](int, bool has_results, int updated, int created, int deleted) {
            if (updated != 0 || created != 0 || deleted != 0) {
                outln("{} row(s) updated, {} created, {} deleted", updated, created, deleted);
            }
            if (!has_results) {
                read_sql();
            }
        };

        m_sql_client->on_next_result = [](int, Vector<String> const& row) {
            StringBuilder builder;
            builder.join(", ", row);
            outln("{}", builder.build());
        };

        m_sql_client->on_results_exhausted = [this](int, int total_rows) {
            outln("{} row(s)", total_rows);
            read_sql();
        };

        m_sql_client->on_connection_error = [this](int, int code, String const& message) {
            outln("\033[33;1mConnection error:\033[0m {}", message);
            m_loop.quit(code);
        };

        m_sql_client->on_execution_error = [this](int, int, String const& message) {
            outln("\033[33;1mExecution error:\033[0m {}", message);
            read_sql();
        };

        m_sql_client->on_disconnected = [this](int) {
            if (m_pending_database.is_empty()) {
                outln("Disconnected from \033[33;1m{}\033[0m and terminating", m_current_database);
                m_loop.quit(0);
            } else {
                outln("Disconnected from \033[33;1m{}\033[0m", m_current_database);
                m_current_database = "";
                m_sql_client->connect(m_pending_database);
            }
        };

        if (!database_name.is_empty())
            connect(database_name);
    }

    ~SQLRepl()
    {
        m_editor->save_history(m_history_path);
    }

    void connect(String const& database_name)
    {
        if (m_current_database.is_empty()) {
            m_sql_client->connect(database_name);
        } else {
            m_pending_database = database_name;
            m_sql_client->async_disconnect(m_connection_id);
        }
    }

    void source_file(String file_name)
    {
        m_input_file_chain.append(move(file_name));
        m_quit_when_files_read = false;
    }

    void read_file(String file_name)
    {
        m_input_file_chain.append(move(file_name));
        m_quit_when_files_read = true;
    }

    auto run()
    {
        return m_loop.exec();
    }

private:
    String m_history_path { String::formatted("{}/.sql-history", Core::StandardPaths::home_directory()) };
    RefPtr<Line::Editor> m_editor { nullptr };
    int m_repl_line_level { 0 };
    bool m_keep_running { true };
    String m_pending_database {};
    String m_current_database {};
    AK::RefPtr<SQL::SQLClient> m_sql_client { nullptr };
    int m_connection_id { 0 };
    Core::EventLoop m_loop;
    RefPtr<Core::File> m_input_file { nullptr };
    bool m_quit_when_files_read { false };
    Vector<String> m_input_file_chain {};

    Optional<String> get_line()
    {
        if (!m_input_file && !m_input_file_chain.is_empty()) {
            auto file_name = m_input_file_chain.take_first();
            auto file_or_error = Core::File::open(file_name, Core::OpenMode::ReadOnly);
            if (file_or_error.is_error()) {
                warnln("Input file {} could not be opened: {}", file_name, file_or_error.error());
                return {};
            }
            m_input_file = file_or_error.value();
        }
        if (m_input_file) {
            auto line = m_input_file->read_line();
            if (m_input_file->eof()) {
                m_input_file->close();
                m_input_file = nullptr;
                if (m_quit_when_files_read && m_input_file_chain.is_empty())
                    return {};
            }
            return line;
            // If the last file is exhausted but m_quit_when_files_read is false
            // we fall through to the standard reading from the editor behaviour
        }
        auto line_result = m_editor->get_line(prompt_for_level(m_repl_line_level));
        if (line_result.is_error())
            return {};
        return line_result.value();
    }

    String read_next_piece()
    {
        StringBuilder piece;

        do {
            if (!piece.is_empty())
                piece.append('\n');

            auto line_maybe = get_line();

            if (!line_maybe.has_value()) {
                m_keep_running = false;
                return {};
            }

            auto& line = line_maybe.value();
            auto lexer = SQL::AST::Lexer(line);

            m_editor->add_to_history(line);
            piece.append(line);

            bool is_first_token = true;
            bool is_command = false;
            bool last_token_ended_statement = false;

            for (SQL::AST::Token token = lexer.next(); token.type() != SQL::AST::TokenType::Eof; token = lexer.next()) {
                switch (token.type()) {
                case SQL::AST::TokenType::ParenOpen:
                    ++m_repl_line_level;
                    break;
                case SQL::AST::TokenType::ParenClose:
                    --m_repl_line_level;
                    break;
                case SQL::AST::TokenType::SemiColon:
                    last_token_ended_statement = true;
                    break;
                case SQL::AST::TokenType::Period:
                    if (is_first_token)
                        is_command = true;
                    break;
                default:
                    last_token_ended_statement = is_command;
                    break;
                }

                is_first_token = false;
            }

            m_repl_line_level = last_token_ended_statement ? 0 : (m_repl_line_level > 0 ? m_repl_line_level : 1);
        } while ((m_repl_line_level > 0) || piece.is_empty());

        return piece.to_string();
    }

    void read_sql()
    {
        String piece = read_next_piece();

        // m_keep_running can be set to false when the file we are reading
        // from is exhausted...
        if (!m_keep_running) {
            m_sql_client->async_disconnect(m_connection_id);
            return;
        }

        if (piece.starts_with('.')) {
            handle_command(piece);
        } else {
            auto statement_id = m_sql_client->sql_statement(m_connection_id, piece);
            m_sql_client->async_statement_execute(statement_id);
        }

        // ...But m_keep_running can also be set to false by a command handler.
        if (!m_keep_running) {
            m_sql_client->async_disconnect(m_connection_id);
            return;
        }
    };

    static String prompt_for_level(int level)
    {
        static StringBuilder prompt_builder;
        prompt_builder.clear();
        prompt_builder.append("> ");

        for (auto i = 0; i < level; ++i)
            prompt_builder.append("    ");

        return prompt_builder.build();
    }

    void handle_command(StringView command)
    {
        if (command == ".exit" || command == ".quit") {
            m_keep_running = false;
        } else if (command.starts_with(".connect ")) {
            auto parts = command.split_view(' ');
            if (parts.size() == 2)
                connect(parts[1]);
            else
                outln("\033[33;1mUsage: .connect <database name>\033[0m");
        } else if (command.starts_with(".read ")) {
            if (!m_input_file) {
                auto parts = command.split_view(' ');
                if (parts.size() == 2) {
                    source_file(parts[1]);
                } else {
                    outln("\033[33;1mUsage: .read <sql file>\033[0m");
                }
            } else {
                outln("\033[33;1mCannot recursively read sql files\033[0m");
            }
            m_loop.deferred_invoke([this]() {
                read_sql();
            });
        } else {
            outln("\033[33;1mUnrecognized command:\033[0m {}", command);
        }
    }
};

int main(int argc, char** argv)
{
    String database_name(getlogin());
    String file_to_source;
    String file_to_read;
    bool suppress_sqlrc = false;
    auto sqlrc_path = String::formatted("{}/.sqlrc", Core::StandardPaths::home_directory());

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This is a client for the SerenitySQL database server.");
    args_parser.add_option(database_name, "Database to connect to", "database", 'd', "database");
    args_parser.add_option(file_to_read, "File to read", "read", 'r', "file");
    args_parser.add_option(file_to_source, "File to source", "source", 's', "file");
    args_parser.add_option(suppress_sqlrc, "Don't read ~/.sqlrc", "no-sqlrc", 'n');
    args_parser.parse(argc, argv);

    SQLRepl repl(database_name);

    if (!suppress_sqlrc && Core::File::exists(sqlrc_path))
        repl.source_file(sqlrc_path);
    if (!file_to_source.is_empty())
        repl.source_file(file_to_source);
    if (!file_to_read.is_empty())
        repl.read_file(file_to_read);
    return repl.run();
}
