/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <stdio.h>

namespace Core {

class ArgsParser {
public:
    ArgsParser();

    enum class Required {
        Yes,
        No
    };

    struct Option {
        bool requires_argument { true };
        const char* help_string { nullptr };
        const char* long_name { nullptr };
        char short_name { 0 };
        const char* value_name { nullptr };
        Function<bool(const char*)> accept_value;

        String name_for_display() const
        {
            if (long_name)
                return String::formatted("--{}", long_name);
            return String::formatted("-{:c}", short_name);
        }
    };

    struct Arg {
        const char* help_string { nullptr };
        const char* name { nullptr };
        int min_values { 0 };
        int max_values { 1 };
        Function<bool(const char*)> accept_value;
    };

    bool parse(int argc, char** argv, bool exit_on_failure = true);
    // *Without* trailing newline!
    void set_general_help(const char* help_string) { m_general_help = help_string; };
    void print_usage(FILE*, const char* argv0);

    void add_option(Option&&);
    void add_option(bool& value, const char* help_string, const char* long_name, char short_name);
    void add_option(const char*& value, const char* help_string, const char* long_name, char short_name, const char* value_name);
    void add_option(String& value, const char* help_string, const char* long_name, char short_name, const char* value_name);
    void add_option(int& value, const char* help_string, const char* long_name, char short_name, const char* value_name);
    void add_option(double& value, const char* help_string, const char* long_name, char short_name, const char* value_name);

    void add_positional_argument(Arg&&);
    void add_positional_argument(const char*& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(String& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(int& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(double& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(Vector<const char*>& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(Vector<String>& value, const char* help_string, const char* name, Required required = Required::Yes);

private:
    Vector<Option> m_options;
    Vector<Arg> m_positional_args;

    bool m_show_help { false };
    const char* m_general_help { nullptr };
};

}
