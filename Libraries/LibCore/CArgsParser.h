/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/HashMap.h>
#include <AK/Vector.h>

/*
  The class ArgsParser provides a way to parse arguments by using a given list that describes the possible
  types of arguments (name, description, required or not, must be followed by a value...).
  Call the add_arg() functions to describe your arguments.

  The class ArgsParserResult is used to manipulate the arguments (checking if an arg has been provided,
  retrieve its value...). In case of error (missing required argument) an empty structure is returned as result.
*/

class CArgsParserResult {
public:
    bool is_present(const String& arg_name) const;
    String get(const String& arg_name) const;
    const Vector<String>& get_single_values() const;

private:
    HashMap<String, String> m_args;
    Vector<String> m_single_values;

    friend class CArgsParser;
};

class CArgsParser {
public:
    CArgsParser(const String& program_name);

    CArgsParserResult parse(int argc, char** argv);

    void add_required_arg(const String& name, const String& description);
    void add_required_arg(const String& name, const String& value_name, const String& description);
    void add_arg(const String& name, const String& description);
    void add_arg(const String& name, const String& value_name, const String& description);
    void add_single_value(const String& name);
    void add_required_single_value(const String& name);
    String get_usage() const;
    void print_usage() const;

private:
    struct Arg {
        inline Arg() {}
        Arg(const String& name, const String& description, bool required);
        Arg(const String& name, const String& value_name, const String& description, bool required);

        String name;
        String description;
        String value_name;
        bool required;
    };

    int parse_next_param(int index, char** argv, const int params_left, CArgsParserResult& res);
    bool is_param_valid(const String& param_name);
    bool check_required_args(const CArgsParserResult& res);

    String m_program_name;
    String m_prefix;

    struct SingleArg {
        String name;
        bool required;
    };
    Vector<SingleArg> m_single_args;
    HashMap<String, Arg> m_args;
};
