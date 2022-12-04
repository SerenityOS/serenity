/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibMain/Main.h>
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

    enum class OptionArgumentMode {
        None,
        Optional,
        Required,
    };

    /// When an option is hidden.
    /// If the hide mode is not None, then it's always hidden from the usage/synopsis.
    enum class OptionHideMode {
        None,
        Markdown,
        CommandLineAndMarkdown,
    };

    struct Option {
        OptionArgumentMode argument_mode { OptionArgumentMode::Required };
        char const* help_string { nullptr };
        char const* long_name { nullptr };
        char short_name { 0 };
        char const* value_name { nullptr };
        Function<bool(char const*)> accept_value;
        OptionHideMode hide_mode { OptionHideMode::None };

        DeprecatedString name_for_display() const
        {
            if (long_name)
                return DeprecatedString::formatted("--{}", long_name);
            return DeprecatedString::formatted("-{:c}", short_name);
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
    void add_option(char const*& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(DeprecatedString& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(StringView& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    template<typename Integral>
    void add_option(Integral& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None)
    requires(IsIntegral<Integral>);
    void add_option(double& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Optional<double>& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Optional<size_t>& value, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);
    void add_option(Vector<size_t>& values, char const* help_string, char const* long_name, char short_name, char const* value_name, char separator = ',', OptionHideMode hide_mode = OptionHideMode::None);
    // Note: This option is being used when we expect the user to use the same option
    // multiple times (e.g. "program --option=example --option=anotherexample ...").
    void add_option(Vector<DeprecatedString>& values, char const* help_string, char const* long_name, char short_name, char const* value_name, OptionHideMode hide_mode = OptionHideMode::None);

    void add_positional_argument(Arg&&);
    void add_positional_argument(char const*& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(DeprecatedString& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(StringView& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(int& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(unsigned& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(double& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(Vector<char const*>& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(Vector<DeprecatedString>& value, char const* help_string, char const* name, Required required = Required::Yes);
    void add_positional_argument(Vector<StringView>& value, char const* help_string, char const* name, Required required = Required::Yes);

private:
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
