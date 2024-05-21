/*
 * Copyright (c) 2021-2024, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Alex Major
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/StandardPaths.h>
#include <LibFileSystem/FileSystem.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Token.h>
#include <LibSQL/SQLClient.h>
#include <unistd.h>

#if !defined(AK_OS_SERENITY)
#    include <LibCore/Process.h>
#endif

class SQLRepl {
public:
    explicit SQLRepl(Core::EventLoop& loop, ByteString const& database_name, NonnullRefPtr<SQL::SQLClient> sql_client)
        : m_history_path(ByteString::formatted("{}/.sql-history", Core::StandardPaths::home_directory()))
        , m_sql_client(move(sql_client))
        , m_loop(loop)
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

        m_sql_client->on_execution_success = [this](auto result) {
            if (result.rows_updated != 0 || result.rows_created != 0 || result.rows_deleted != 0)
                outln("{} row(s) created, {} updated, {} deleted", result.rows_created, result.rows_updated, result.rows_deleted);
            if (!result.has_results)
                read_sql();
        };

        m_sql_client->on_next_result = [](auto result) {
            StringBuilder builder;
            builder.join(", "sv, result.values);
            outln("{}", builder.to_byte_string());
        };

        m_sql_client->on_results_exhausted = [this](auto result) {
            outln("{} row(s)", result.total_rows);
            read_sql();
        };

        m_sql_client->on_execution_error = [this](auto result) {
            outln("\033[33;1mExecution error:\033[0m {}", result.error_message);
            read_sql();
        };

        if (!database_name.is_empty())
            connect(database_name);
    }

    ~SQLRepl()
    {
        m_editor->save_history(m_history_path);
    }

    void connect(ByteString const& database_name)
    {
        if (!m_database_name.is_empty()) {
            m_sql_client->disconnect(m_connection_id);
            m_database_name = {};
        }

        if (auto connection_id = m_sql_client->connect(database_name); connection_id.has_value()) {
            outln("Connected to \033[33;1m{}\033[0m", database_name);
            m_database_name = database_name;
            m_connection_id = *connection_id;
        } else {
            warnln("\033[33;1mCould not connect to:\033[0m {}", database_name);
            m_loop.quit(1);
        }
    }

    void source_file(ByteString file_name)
    {
        m_input_file_chain.append(move(file_name));
        m_quit_when_files_read = false;
    }

    void read_file(ByteString file_name)
    {
        m_input_file_chain.append(move(file_name));
        m_quit_when_files_read = true;
    }

    auto run()
    {
        read_sql();
        return m_loop.exec();
    }

private:
    ByteString m_history_path;
    RefPtr<Line::Editor> m_editor { nullptr };
    int m_repl_line_level { 0 };
    bool m_keep_running { true };
    ByteString m_database_name {};
    NonnullRefPtr<SQL::SQLClient> m_sql_client;
    SQL::ConnectionID m_connection_id { 0 };
    Core::EventLoop& m_loop;
    OwnPtr<Core::InputBufferedFile> m_input_file { nullptr };
    bool m_quit_when_files_read { false };
    Vector<ByteString> m_input_file_chain {};
    Array<u8, 4096> m_buffer {};

    Optional<ByteString> get_line()
    {
        if (!m_input_file && !m_input_file_chain.is_empty()) {
            auto file_name = m_input_file_chain.take_first();
            auto file_or_error = Core::File::open(file_name, Core::File::OpenMode::Read);
            if (file_or_error.is_error()) {
                warnln("Input file {} could not be opened: {}", file_name, file_or_error.error());
                return {};
            }

            auto buffered_file_or_error = Core::InputBufferedFile::create(file_or_error.release_value());
            if (buffered_file_or_error.is_error()) {
                warnln("Input file {} could not be buffered: {}", file_name, buffered_file_or_error.error());
                return {};
            }

            m_input_file = buffered_file_or_error.release_value();
        }
        if (m_input_file) {
            auto line = m_input_file->read_line(m_buffer);
            if (line.is_error()) {
                warnln("Failed to read line: {}", line.error());
                return {};
            }
            if (m_input_file->is_eof()) {
                m_input_file->close();
                m_input_file = nullptr;
                if (m_quit_when_files_read && m_input_file_chain.is_empty())
                    return {};
            }
            return line.release_value();
            // If the last file is exhausted but m_quit_when_files_read is false
            // we fall through to the standard reading from the editor behavior
        }
        auto line_result = m_editor->get_line(prompt_for_level(m_repl_line_level));
        if (line_result.is_error())
            return {};
        return line_result.value();
    }

    ByteString read_next_piece()
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
            bool tokens_found = false;

            for (SQL::AST::Token token = lexer.next(); token.type() != SQL::AST::TokenType::Eof; token = lexer.next()) {
                tokens_found = true;
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

            if (tokens_found)
                m_repl_line_level = last_token_ended_statement ? 0 : (m_repl_line_level > 0 ? m_repl_line_level : 1);
        } while ((m_repl_line_level > 0) || piece.is_empty());

        return piece.to_byte_string();
    }

    void read_sql()
    {
        ByteString piece = read_next_piece();

        // m_keep_running can be set to false when the file we are reading
        // from is exhausted...
        if (!m_keep_running) {
            m_sql_client->disconnect(m_connection_id);
            m_loop.quit(0);
            return;
        }

        if (piece.starts_with('.')) {
            bool ready_for_input = handle_command(piece);
            if (ready_for_input)
                m_loop.deferred_invoke([this]() {
                    read_sql();
                });
        } else if (auto statement_id = m_sql_client->prepare_statement(m_connection_id, piece); statement_id.has_value()) {
            m_sql_client->async_execute_statement(*statement_id, {});
        } else {
            warnln("\033[33;1mError parsing SQL statement\033[0m: {}", piece);
            m_loop.deferred_invoke([this]() {
                read_sql();
            });
        }

        // ...But m_keep_running can also be set to false by a command handler.
        if (!m_keep_running) {
            m_sql_client->disconnect(m_connection_id);
            m_loop.quit(0);
            return;
        }
    }

    static ByteString prompt_for_level(int level)
    {
        static StringBuilder prompt_builder;
        prompt_builder.clear();
        prompt_builder.append("> "sv);

        for (auto i = 0; i < level; ++i)
            prompt_builder.append("    "sv);

        return prompt_builder.to_byte_string();
    }

    bool handle_command(StringView command)
    {
        bool ready_for_input = true;
        if (command == ".exit" || command == ".quit") {
            m_keep_running = false;
            ready_for_input = false;
        } else if (command.starts_with(".connect "sv)) {
            auto parts = command.split_view(' ');
            if (parts.size() == 2) {
                connect(parts[1]);
                ready_for_input = false;
            } else {
                outln("\033[33;1mUsage: .connect <database name>\033[0m");
            }
        } else if (command.starts_with(".read "sv)) {
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
        } else {
            outln("\033[33;1mUnrecognized command:\033[0m {}", command);
        }
        return ready_for_input;
    }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    ByteString database_name(getlogin());
    ByteString file_to_source;
    ByteString file_to_read;
    bool suppress_sqlrc = false;
    auto sqlrc_path = ByteString::formatted("{}/.sqlrc", Core::StandardPaths::home_directory());
#if !defined(AK_OS_SERENITY)
    StringView sql_server_path;
#endif

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This is a client for the SerenitySQL database server.");
    args_parser.add_option(database_name, "Database to connect to", "database", 'd', "database");
    args_parser.add_option(file_to_read, "File to read", "read", 'r', "file");
    args_parser.add_option(file_to_source, "File to source", "source", 's', "file");
    args_parser.add_option(suppress_sqlrc, "Don't read ~/.sqlrc", "no-sqlrc", 'n');
#if !defined(AK_OS_SERENITY)
    args_parser.add_option(sql_server_path, "Path to SQLServer to launch if needed", "sql-server-path", 'p', "path");
#endif
    args_parser.parse(arguments);

    Core::EventLoop loop;

#if defined(AK_OS_SERENITY)
    auto sql_client = TRY(SQL::SQLClient::try_create());
#else
    VERIFY(!sql_server_path.is_empty());

    auto [_, sql_client] = TRY(Core::IPCProcess::spawn_singleton<SQL::SQLClient>({
        .name = "SQLServer"sv,
        .executable = sql_server_path,
    }));
#endif

    SQLRepl repl(loop, database_name, move(sql_client));

    if (!suppress_sqlrc && FileSystem::exists(sqlrc_path))
        repl.source_file(sqlrc_path);
    if (!file_to_source.is_empty())
        repl.source_file(file_to_source);
    if (!file_to_read.is_empty())
        repl.read_file(file_to_read);
    return repl.run();
}
