/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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
                return String::format("--%s", long_name);
            return String::format("-%c", short_name);
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
    void print_usage(FILE*, const char* argv0);

    void add_option(Option&&);
    void add_option(bool& value, const char* help_string, const char* long_name, char short_name);
    void add_option(const char*& value, const char* help_string, const char* long_name, char short_name, const char* value_name);
    void add_option(int& value, const char* help_string, const char* long_name, char short_name, const char* value_name);

    void add_positional_argument(Arg&&);
    void add_positional_argument(const char*& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(int& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(double& value, const char* help_string, const char* name, Required required = Required::Yes);
    void add_positional_argument(Vector<const char*>& value, const char* help_string, const char* name, Required required = Required::Yes);

private:
    Vector<Option> m_options;
    Vector<Arg> m_positional_args;

    bool m_show_help { false };
};

}
