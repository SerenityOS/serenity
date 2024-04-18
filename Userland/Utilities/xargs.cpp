/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

bool run_command(Vector<char*>&& child_argv, bool verbose, bool is_stdin, int devnull_fd);

enum Decision {
    Unget,
    Continue,
    Stop,
};
bool read_items(FILE* fp, char entry_separator, Function<Decision(StringView)>);

class ParsedInitialArguments {
public:
    ParsedInitialArguments(Vector<ByteString>&, StringView placeholder);

    void for_each_joined_argument(StringView, Function<void(ByteString const&)>) const;

    size_t size() const { return m_all_parts.size(); }

private:
    Vector<Vector<StringView>> m_all_parts;
};

ErrorOr<int> serenity_main(Main::Arguments main_arguments)
{
    TRY(Core::System::pledge("stdio rpath proc exec"));

    StringView placeholder;
    bool split_with_nulls = false;
    ByteString specified_delimiter = "\n"sv;
    Vector<ByteString> arguments;
    bool verbose = false;
    ByteString file_to_read = "-"sv;
    int max_lines_for_one_command = 0;
    int max_bytes_for_one_command = ARG_MAX;

    Core::ArgsParser args_parser;
    args_parser.set_stop_on_first_non_option(true);
    args_parser.set_general_help("Read arguments from stdin and interpret them as command-line arguments for another program. See also: 'man xargs'.");
    args_parser.add_option(placeholder, "Placeholder string to be replaced in arguments", "replace", 'I', "placeholder");
    args_parser.add_option(split_with_nulls, "Split input items with the null character instead of newline", "null", '0');
    args_parser.add_option(specified_delimiter, "Split the input items with the specified character", "delimiter", 'd', "delim");
    args_parser.add_option(verbose, "Display each command before executing it", "verbose", 'v');
    args_parser.add_option(file_to_read, "Read arguments from the specified file instead of stdin", "arg-file", 'a', "file");
    args_parser.add_option(max_lines_for_one_command, "Use at most max-lines lines to create a command", "line-limit", 'L', "max-lines");
    args_parser.add_option(max_bytes_for_one_command, "Use at most max-chars characters to create a command", "char-limit", 's', "max-chars");
    args_parser.add_positional_argument(arguments, "Command and any initial arguments for it", "command", Core::ArgsParser::Required::No);
    args_parser.parse(main_arguments);

    size_t max_bytes = min(ARG_MAX, max_bytes_for_one_command);
    size_t max_lines = max(max_lines_for_one_command, 0);

    if (!split_with_nulls && specified_delimiter.length() > 1) {
        warnln("xargs: the delimiter must be a single byte");
        return 1;
    }

    char entry_separator = split_with_nulls ? '\0' : specified_delimiter[0];

    if (!placeholder.is_empty())
        max_lines = 1;

    if (arguments.is_empty())
        arguments.append("echo");

    ParsedInitialArguments initial_arguments(arguments, placeholder);

    FILE* fp = stdin;
    bool is_stdin = true;

    if ("-"sv != file_to_read) {
        // A file was specified, try to open it.
        fp = fopen(file_to_read.characters(), "re");
        if (!fp) {
            perror("fopen");
            return 1;
        }
        is_stdin = false;
    }

    StringBuilder builder;
    Vector<char*> child_argv;

    int devnull_fd = 0;

    if (is_stdin) {
        devnull_fd = TRY(Core::System::open("/dev/null"sv, O_RDONLY | O_CLOEXEC));
    }

    size_t total_command_length = 0;
    size_t items_used_for_this_command = 0;

    auto fail = read_items(fp, entry_separator, [&](StringView item) {
        if (item.ends_with('\n'))
            item = item.substring_view(0, item.length() - 1);

        if (item.is_empty())
            return Continue;

        // The first item is processed differently, as all the initial-arguments are processed _with_ that item
        // as their substitution target (assuming that substitution is enabled).
        // Note that if substitution is not enabled, we manually insert a substitution target at the end of initial-arguments,
        // so this item has a place to go.
        if (items_used_for_this_command == 0) {
            child_argv.ensure_capacity(initial_arguments.size());

            initial_arguments.for_each_joined_argument(item, [&](ByteString const& string) {
                total_command_length += string.length();
                child_argv.append(strdup(string.characters()));
            });

            ++items_used_for_this_command;
        } else {
            if ((max_lines > 0 && items_used_for_this_command + 1 > max_lines) || total_command_length + item.length() + 1 >= max_bytes) {
                // Note: This `move' does not actually move-construct a new Vector at the callsite, it only allows perfect-forwarding
                //       and does not invalidate `child_argv' in this scope.
                //       The same applies for the one below.
                if (!run_command(move(child_argv), verbose, is_stdin, devnull_fd))
                    return Stop;
                items_used_for_this_command = 0;
                total_command_length = 0;
                return Unget;
            } else {
                child_argv.append(strndup(item.characters_without_null_termination(), item.length()));
                total_command_length += item.length();
                ++items_used_for_this_command;
            }
        }

        return Continue;
    });

    if (!fail && !child_argv.is_empty())
        fail = !run_command(move(child_argv), verbose, is_stdin, devnull_fd);

    if (!is_stdin)
        fclose(fp);

    return fail ? 1 : 0;
}

bool read_items(FILE* fp, char entry_separator, Function<Decision(StringView)> callback)
{
    bool fail = false;

    for (;;) {
        char* item = nullptr;
        size_t buffer_size = 0;

        auto item_size = getdelim(&item, &buffer_size, entry_separator, fp);

        if (item_size < 0) {
            // getdelim() will return -1 and set errno to 0 on EOF.
            if (errno != 0) {
                perror("getdelim");
                fail = true;
            }
            free(item);
            break;
        }

        Decision decision;
        do {
            decision = callback({ item, strlen(item) });
            if (decision == Stop) {
                free(item);
                return true;
            }
        } while (decision == Unget);

        free(item);
    }

    return fail;
}

bool run_command(Vector<char*>&& child_argv, bool verbose, bool is_stdin, int devnull_fd)
{
    child_argv.append(nullptr);

    if (verbose) {
        StringBuilder builder;
        builder.join(' ', child_argv);
        warnln("xargs: {}", builder.to_byte_string());
    }

    auto pid = fork();
    if (pid < 0) {
        perror("fork");
        return false;
    }

    if (pid == 0) {
        if (is_stdin)
            dup2(devnull_fd, STDIN_FILENO);

        execvp(child_argv[0], child_argv.data());
        exit(1);
    }

    for (auto* ptr : child_argv)
        free(ptr);

    child_argv.clear_with_capacity();

    int wstatus = 0;
    if (waitpid(pid, &wstatus, 0) < 0) {
        perror("waitpid");
        return false;
    }

    if (WIFEXITED(wstatus)) {
        if (WEXITSTATUS(wstatus) != 0)
            return false;
    } else {
        return false;
    }

    return true;
}

ParsedInitialArguments::ParsedInitialArguments(Vector<ByteString>& arguments, StringView placeholder)
{
    m_all_parts.ensure_capacity(arguments.size());
    bool some_argument_has_placeholder = false;

    for (auto arg : arguments) {
        if (placeholder.is_empty()) {
            m_all_parts.append({ arg });
        } else {
            auto parts = arg.view().split_view(placeholder, SplitBehavior::KeepEmpty);
            some_argument_has_placeholder = some_argument_has_placeholder || parts.size() > 1;
            m_all_parts.append(move(parts));
        }
    }

    // Append an implicit placeholder at the end if no argument has any placeholders.
    if (!some_argument_has_placeholder) {
        Vector<StringView> parts;
        parts.append(""sv);
        parts.append(""sv);
        m_all_parts.append(move(parts));
    }
}

void ParsedInitialArguments::for_each_joined_argument(StringView separator, Function<void(ByteString const&)> callback) const
{
    StringBuilder builder;
    for (auto& parts : m_all_parts) {
        builder.clear();
        builder.join(separator, parts);
        callback(builder.to_byte_string());
    }
}
