/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibMain/Main.h>
#include <climits>
#include <stdio.h>

namespace Core {

class ArgsParser {
public:
    ArgsParser();

    enum class Required {
        Yes,
        No
    };

    enum class FailureBehavior {
        PrintUsageAndExit,
        PrintUsage,
        Exit,
        Ignore,
    };

    /// When an option is hidden.
    /// If the hide mode is not None, then it's always hidden from the usage/synopsis.
    enum class OptionHideMode {
        None,
        Markdown,
        CommandLineAndMarkdown,
    };

    struct Option {
        bool requires_argument { true };
        char const* help_string { nullptr };
        char const* long_name { nullptr };
        char short_name { 0 };
        char const* value_name { nullptr };
        Function<bool(char const*)> accept_value;
        OptionHideMode hide_mode { OptionHideMode::None };

        String name_for_display() const
        {
            if (long_name)
                return String::formatted("--{}", long_name);
            return String::formatted("-{:c}", short_name);
        }
    };

    struct Arg {
        char const* help_string { nullptr };
        char const* name { nullptr };
        int min_values { 0 };
        int max_values { 1 };
        Function<bool(char const*)> accept_value;
    };

    bool parse(int argc, char* const* argv, FailureBehavior failure_behavior = FailureBehavior::PrintUsageAndExit);
    bool parse(Main::Arguments const& arguments, FailureBehavior failure_behavior = FailureBehavior::PrintUsageAndExit)
    {
        return parse(arguments.argc, arguments.argv, failure_behavior);
    }

    // *Without* trailing newline!
    void set_general_help(char const* help_string) { m_general_help = help_string; };
    void set_stop_on_first_non_option(bool stop_on_first_non_option) { m_stop_on_first_non_option = stop_on_first_non_option; }
    void print_usage(FILE*, char const* argv0);
    void print_usage_terminal(FILE*, char const* argv0);
    void print_usage_markdown(FILE*, char const* argv0);
    void print_version(FILE*);

    void add_option(Option&&);
    void add_ignored(char const* long_name, char short_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(bool& value, char const* help_string, char const* long_name, char short_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(int& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(unsigned& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(double& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Optional<double>& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Optional<size_t>& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Vector<size_t>& values, char const* help_string, char const* long_name, char short_name, char const* value_name, char separator = ',', OptionHideMode hide_mode = OptionHideMode::None);

    template<typename T>
    requires(IsOneOf<T, char const*, String, StringView>) void add_option(T& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None)
    {
        add_option_helper(help_string, long_name, short_name, value_name, hide_mode,
            [&value](char const* s) {
                value = s;
                return true;
            });
    }

    void add_positional_argument(Arg&&);

    template<typename T>
    requires(IsAppendableContainer<T>) void add_positional_argument(T& values, char const* help_string, char const* name, Required required = Required::Yes)
    {
        add_positional_argument_helper<true>(help_string, name, required,
            [&values](char const* s) {
                values.append(s);
                return true;
            });
    }

    template<typename T>
    requires(IsOneOf<T, char const*, String, StringView>) void add_positional_argument(T& value, char const* help_string, char const* name, Required required = Required::Yes)
    {
        add_positional_argument_helper(help_string, name, required,
            [&value](char const* s) {
                value = s;
                return true;
            });
    }

    void add_positional_argument(int& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(unsigned& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(double& value, char const* help_string, char const* name, Required required = Required::Yes);

private:
    template<bool RequireArgument = true>
    void add_option_helper(char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode, Function<bool(char const*)> accept_value);

    template<bool IsContainer = false>
    void add_positional_argument_helper(char const* help_string, char const* name, Required required, Function<bool(char const*)> accept_value);

    void autocomplete(FILE*, StringView program_name, Span<char const* const> remaining_arguments);

    Vector<Option> m_options;
    Vector<Arg> m_positional_args;

    bool m_show_help { false };
    bool m_show_version { false };
    bool m_perform_autocomplete { false };
    char const* m_general_help { nullptr };
    bool m_stop_on_first_non_option { false };
};

}
