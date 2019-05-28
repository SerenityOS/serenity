#pragma once

#include <AK/AKString.h>
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

    CArgsParserResult parse(const int argc, const char** argv);

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

    int parse_next_param(const int index, const char** argv, const int params_left, CArgsParserResult& res);
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
