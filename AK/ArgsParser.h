#pragma once

#include "AKString.h"
#include "HashMap.h"
#include "Vector.h"

namespace AK {
  class ArgsParserResult {
  public:
    bool is_present(const String& arg_name) const;
    String get(const String& arg_name) const;
    const Vector<String>& get_single_values() const;

  private:
    HashMap<String, String> m_args;
    Vector<String> m_single_values;

    friend class ArgsParser;
  };

  class ArgsParser {
  public:
    ArgsParser(const String& program_name, const String& prefix);

    ArgsParserResult parse(const int argc, const char** argv);

    void add_arg(const String& name, const String& description, bool required);
    void add_arg(const String& name, const String& value_name, const String& description, bool required);
    String get_usage() const;

  private:
    struct Arg {
      Arg();
      Arg(const String& name, const String& description, bool required);
      Arg(const String& name, const String& value_name, const String& description, bool required);

      String name;
      String description;
      String value_name;
      bool has_value_name;
      bool required;
    };

    int parse_next_param(const int index, const char** argv, const int nbParamLeft, ArgsParserResult& res);
    bool is_param_valid(const String& paramName);
    bool check_required_args(const ArgsParserResult& res);

    String m_program_name;
    String m_prefix;
    HashMap<String, Arg> m_args;
  };
}
