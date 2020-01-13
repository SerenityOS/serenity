#include "CArgsParser.h"
#include <AK/StringBuilder.h>

#include <stdio.h>

bool CArgsParserResult::is_present(const String& arg_name) const
{
    return m_args.contains(arg_name);
}

String CArgsParserResult::get(const String& arg_name) const
{
    return m_args.get(arg_name).value_or({});
}

const Vector<String>& CArgsParserResult::get_single_values() const
{
    return m_single_values;
}

CArgsParser::Arg::Arg(const String& name, const String& description, bool required)
    : name(name)
    , description(description)
    , required(required)
{
}

CArgsParser::Arg::Arg(const String& name, const String& value_name, const String& description, bool required)
    : name(name)
    , description(description)
    , value_name(value_name)
    , required(required)
{
}

CArgsParser::CArgsParser(const String& program_name)
    : m_program_name(program_name)
    , m_prefix("-")
{
}

CArgsParserResult CArgsParser::parse(int argc, char** argv)
{
    CArgsParserResult res;

    // We should have at least one parameter
    if (argc < 2)
        return {};

    // We parse the first parameter at the index 1
    if (parse_next_param(1, argv, argc - 1, res) != 0)
        return {};

    if (!check_required_args(res))
        return {};

    return res;
}

int CArgsParser::parse_next_param(int index, char** argv, const int params_left, CArgsParserResult& res)
{
    ASSERT(params_left >= 0);
    if (params_left == 0)
        return 0;

    String param = argv[index];

    // We check if the prefix is found at the beginning of the param name
    if (is_param_valid(param)) {
        auto prefix_length = m_prefix.length();
        String param_name = param.substring(prefix_length, param.length() - prefix_length);

        auto arg = m_args.find(param_name);
        if (arg == m_args.end()) {
            printf("Unknown arg \"");
            if (!param_name.is_null())
                printf("%s", param_name.characters());
            printf("\"\n");
            return -1;
        }

        // If this parameter must be followed by a value, we look for it
        if (!arg->value.value_name.is_null()) {
            if (params_left < 2) {
                printf("Missing value for argument %s\n", arg->value.name.characters());
                return -1;
            }

            String next = String(argv[index + 1]);

            if (is_param_valid(next)) {
                printf("Missing value for argument %s\n", arg->value.name.characters());
                return -1;
            }

            res.m_args.set(arg->value.name, next);
            return parse_next_param(index + 2, argv, params_left - 2, res);
        }

        // Single argument, not followed by a value
        res.m_args.set(arg->value.name, "");
        return parse_next_param(index + 1, argv, params_left - 1, res);
    }

    // Else, it's a value alone, a file name parameter for example
    res.m_single_values.append(param);
    return parse_next_param(index + 1, argv, params_left - 1, res);
}

bool CArgsParser::is_param_valid(const String& param_name)
{
    return param_name.length() >= m_prefix.length() &&
        param_name.substring(0, m_prefix.length()) == m_prefix;
}

bool CArgsParser::check_required_args(const CArgsParserResult& res)
{
    for (auto& it : m_args) {
        if (it.value.required) {
            if (!res.is_present(it.value.name))
                return false;
        }
    }

    int required_arguments = 0;
    for (const auto& a : m_single_args) {
        if (a.required) {
            required_arguments++;
        }
    }

    if (required_arguments != 0) {
        if (res.m_single_values.size() < required_arguments)
            return false;
    }

    return true;
}

void CArgsParser::add_required_arg(const String& name, const String& description)
{
    m_args.set(name, Arg(name, description, true));
}

void CArgsParser::add_required_arg(const String& name, const String& value_name, const String& description)
{
    m_args.set(name, Arg(name, value_name, description, true));
}

void CArgsParser::add_arg(const String& name, const String& description)
{
    m_args.set(name, Arg(name, description, false));
}

void CArgsParser::add_arg(const String& name, const String& value_name, const String& description)
{
    m_args.set(name, Arg(name, value_name, description, false));
}

void CArgsParser::add_single_value(const String& name)
{
    m_single_args.append(SingleArg { name, false });
}

void CArgsParser::add_required_single_value(const String& name)
{
    if (m_single_args.size() != 0) {
        // adding required arguments after non-required arguments would be nonsensical
        ASSERT(m_single_args.last().required);
    }
    m_single_args.append(SingleArg { name, true });
}

String CArgsParser::get_usage() const
{
    StringBuilder sb;

    sb.append("usage : ");
    sb.append(m_program_name);
    sb.append(" ");

    for (auto& it : m_args) {
        if (it.value.required)
            sb.append("<");
        else
            sb.append("[");

        sb.append(m_prefix);
        sb.append(it.value.name);

        if (!it.value.value_name.is_null()) {
            sb.append(" ");
            sb.append(it.value.value_name);
        }

        if (it.value.required)
            sb.append("> ");
        else
            sb.append("] ");
    }

    for (auto& arg : m_single_args) {
        if (arg.required)
            sb.append("<");
        else
            sb.append("[");

        sb.append(arg.name);

        if (arg.required)
            sb.append("> ");
        else
            sb.append("] ");
    }

    sb.append("\n");

    for (auto& it : m_args) {
        sb.append("    ");
        sb.append(m_prefix);
        sb.append(it.value.name);

        if (!it.value.value_name.is_null()) {
            sb.append(" ");
            sb.append(it.value.value_name);
        }

        sb.append(" : ");
        sb.append(it.value.description);
        sb.append("\n");
    }

    return sb.to_string();
}

void CArgsParser::print_usage() const
{
    printf("%s\n", get_usage().characters());
}
