/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Matthias Zimmerman <matthias291999@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Version.h>
#include <getopt.h>
#include <limits.h>
#include <math.h>
#include <stdio.h>
#include <string.h>

static Optional<double> convert_to_double(const char* s)
{
    char* p;
    double v = strtod(s, &p);
    if (isnan(v) || p == s)
        return {};
    return v;
}

namespace Core {

ArgsParser::ArgsParser()
{
    add_option(m_show_help, "Display help message and exit", "help", 0);
    add_option(m_show_version, "Print version", "version", 0);
}

bool ArgsParser::parse(int argc, char* const* argv, FailureBehavior failure_behavior)
{
    auto fail = [this, argv, failure_behavior] {
        if (failure_behavior == FailureBehavior::PrintUsage || failure_behavior == FailureBehavior::PrintUsageAndExit)
            print_usage(stderr, argv[0]);
        if (failure_behavior == FailureBehavior::Exit || failure_behavior == FailureBehavior::PrintUsageAndExit)
            exit(1);
    };

    Vector<option> long_options;
    StringBuilder short_options_builder;

    if (m_stop_on_first_non_option)
        short_options_builder.append('+');

    int index_of_found_long_option = -1;

    // Tell getopt() to reset its internal state, and start scanning from optind = 1.
    // We could also set optreset = 1, but the host platform may not support that.
    optind = 0;

    for (size_t i = 0; i < m_options.size(); i++) {
        auto& opt = m_options[i];
        if (opt.long_name) {
            option long_opt {
                opt.long_name,
                opt.requires_argument ? required_argument : no_argument,
                &index_of_found_long_option,
                static_cast<int>(i)
            };
            long_options.append(long_opt);
        }
        if (opt.short_name) {
            short_options_builder.append(opt.short_name);
            if (opt.requires_argument)
                short_options_builder.append(':');
        }
    }
    long_options.append({ 0, 0, 0, 0 });

    String short_options = short_options_builder.build();

    while (true) {
        int c = getopt_long(argc, argv, short_options.characters(), long_options.data(), nullptr);
        if (c == -1) {
            // We have reached the end.
            break;
        } else if (c == '?') {
            // There was an error, and getopt() has already
            // printed its error message.
            fail();
            return false;
        }

        // Let's see what option we just found.
        Option* found_option = nullptr;
        if (c == 0) {
            // It was a long option.
            VERIFY(index_of_found_long_option >= 0);
            found_option = &m_options[index_of_found_long_option];
            index_of_found_long_option = -1;
        } else {
            // It was a short option, look it up.
            auto it = m_options.find_if([c](auto& opt) { return c == opt.short_name; });
            VERIFY(!it.is_end());
            found_option = &*it;
        }
        VERIFY(found_option);

        const char* arg = found_option->requires_argument ? optarg : nullptr;
        if (!found_option->accept_value(arg)) {
            warnln("\033[31mInvalid value for option \033[1m{}\033[22m\033[0m", found_option->name_for_display());
            fail();
            return false;
        }
    }

    // We're done processing options.
    // Now let's show version or help if requested.

    if (m_show_version) {
        print_version(stdout);
        if (failure_behavior == FailureBehavior::Exit || failure_behavior == FailureBehavior::PrintUsageAndExit)
            exit(0);
        return false;
    }
    if (m_show_help) {
        print_usage(stdout, argv[0]);
        if (failure_behavior == FailureBehavior::Exit || failure_behavior == FailureBehavior::PrintUsageAndExit)
            exit(0);
        return false;
    }

    // Now let's parse positional arguments.

    int values_left = argc - optind;
    Vector<int, 16> num_values_for_arg;
    num_values_for_arg.resize(m_positional_args.size(), true);
    int total_values_required = 0;
    for (size_t i = 0; i < m_positional_args.size(); i++) {
        auto& arg = m_positional_args[i];
        num_values_for_arg[i] = arg.min_values;
        total_values_required += arg.min_values;
    }

    if (total_values_required > values_left) {
        fail();
        return false;
    }
    int extra_values_to_distribute = values_left - total_values_required;

    for (size_t i = 0; i < m_positional_args.size(); i++) {
        auto& arg = m_positional_args[i];
        int extra_values_to_this_arg = min(arg.max_values - arg.min_values, extra_values_to_distribute);
        num_values_for_arg[i] += extra_values_to_this_arg;
        extra_values_to_distribute -= extra_values_to_this_arg;
        if (extra_values_to_distribute == 0)
            break;
    }

    if (extra_values_to_distribute > 0) {
        // We still have too many values :(
        fail();
        return false;
    }

    for (size_t i = 0; i < m_positional_args.size(); i++) {
        auto& arg = m_positional_args[i];
        for (int j = 0; j < num_values_for_arg[i]; j++) {
            const char* value = argv[optind++];
            if (!arg.accept_value(value)) {
                warnln("Invalid value for argument {}", arg.name);
                fail();
                return false;
            }
        }
    }

    return true;
}

void ArgsParser::print_usage(FILE* file, const char* argv0)
{
    char const* env_preference = getenv("ARGSPARSER_EMIT_MARKDOWN");
    if (env_preference != nullptr && env_preference[0] == '1' && env_preference[1] == 0) {
        print_usage_markdown(file, argv0);
    } else {
        print_usage_terminal(file, argv0);
    }
}

void ArgsParser::print_usage_terminal(FILE* file, const char* argv0)
{
    out(file, "Usage:\n\t\033[1m{}\033[0m", argv0);

    for (auto& opt : m_options) {
        if (opt.long_name && !strcmp(opt.long_name, "help"))
            continue;
        if (opt.requires_argument)
            out(file, " [{} {}]", opt.name_for_display(), opt.value_name);
        else
            out(file, " [{}]", opt.name_for_display());
    }
    for (auto& arg : m_positional_args) {
        bool required = arg.min_values > 0;
        bool repeated = arg.max_values > 1;

        if (required && repeated)
            out(file, " <{}...>", arg.name);
        else if (required && !repeated)
            out(file, " <{}>", arg.name);
        else if (!required && repeated)
            out(file, " [{}...]", arg.name);
        else if (!required && !repeated)
            out(file, " [{}]", arg.name);
    }
    outln(file);

    if (m_general_help != nullptr && m_general_help[0] != '\0') {
        outln(file, "\nDescription:");
        outln(file, "{}", m_general_help);
    }

    if (!m_options.is_empty())
        outln(file, "\nOptions:");
    for (auto& opt : m_options) {
        auto print_argument = [&]() {
            if (opt.value_name) {
                if (opt.requires_argument)
                    out(file, " {}", opt.value_name);
                else
                    out(file, " [{}]", opt.value_name);
            }
        };
        out(file, "\t");
        if (opt.short_name) {
            out(file, "\033[1m-{}\033[0m", opt.short_name);
            print_argument();
        }
        if (opt.short_name && opt.long_name)
            out(file, ", ");
        if (opt.long_name) {
            out(file, "\033[1m--{}\033[0m", opt.long_name);
            print_argument();
        }

        if (opt.help_string)
            out(file, "\t{}", opt.help_string);
        outln(file);
    }

    if (!m_positional_args.is_empty())
        outln(file, "\nArguments:");

    for (auto& arg : m_positional_args) {
        out(file, "\t\033[1m{}\033[0m", arg.name);
        if (arg.help_string)
            out(file, "\t{}", arg.help_string);
        outln(file);
    }
}

void ArgsParser::print_usage_markdown(FILE* file, const char* argv0)
{
    outln(file, "## Name\n\n{}", argv0);

    out(file, "\n## Synopsis\n\n```sh\n$ {}", argv0);
    for (auto& opt : m_options) {
        if (opt.long_name != nullptr && (!strcmp(opt.long_name, "help") || !strcmp(opt.long_name, "version")))
            continue;
        if (opt.requires_argument)
            out(file, " [{} {}]", opt.name_for_display(), opt.value_name);
        else
            out(file, " [{}]", opt.name_for_display());
    }
    for (auto& arg : m_positional_args) {
        bool required = arg.min_values > 0;
        bool repeated = arg.max_values > 1;

        if (required && repeated)
            out(file, " <{}...>", arg.name);
        else if (required && !repeated)
            out(file, " <{}>", arg.name);
        else if (!required && repeated)
            out(file, " [{}...]", arg.name);
        else if (!required && !repeated)
            out(file, " [{}]", arg.name);
    }
    outln(file, "\n```");

    if (m_general_help != nullptr && m_general_help[0] != '\0') {
        outln(file, "\n## Description\n\n{}", m_general_help);
    }

    if (!m_options.is_empty())
        outln(file, "\n## Options:\n");
    for (auto& opt : m_options) {
        auto print_argument = [&]() {
            if (opt.value_name != nullptr) {
                if (opt.requires_argument)
                    out(file, " {}", opt.value_name);
                else
                    out(file, " [{}]", opt.value_name);
            }
        };
        out(file, "* ");
        if (opt.short_name != '\0') {
            out(file, "`-{}", opt.short_name);
            print_argument();
            out(file, "`");
        }
        if (opt.short_name != '\0' && opt.long_name != nullptr)
            out(file, ", ");
        if (opt.long_name != nullptr) {
            out(file, "`--{}", opt.long_name);
            print_argument();
            out(file, "`");
        }

        if (opt.help_string != nullptr)
            out(file, ": {}", opt.help_string);
        outln(file);
    }

    if (!m_positional_args.is_empty())
        outln(file, "\n## Arguments:\n");

    for (auto& arg : m_positional_args) {
        out(file, "* `{}`", arg.name);
        if (arg.help_string != nullptr)
            out(file, ": {}", arg.help_string);
        outln(file);
    }
}

void ArgsParser::print_version(FILE* file)
{
    outln(file, Core::Version::SERENITY_VERSION);
}

void ArgsParser::add_option(Option&& option)
{
    m_options.append(move(option));
}

void ArgsParser::add_ignored(const char* long_name, char short_name)
{
    Option option {
        false,
        "Ignored",
        long_name,
        short_name,
        nullptr,
        [](const char*) {
            return true;
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(bool& value, const char* help_string, const char* long_name, char short_name)
{
    Option option {
        false,
        help_string,
        long_name,
        short_name,
        nullptr,
        [&value](const char* s) {
            VERIFY(s == nullptr);
            value = true;
            return true;
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(const char*& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(String& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(StringView& value, char const* help_string, char const* long_name, char short_name, char const* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(int& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            auto opt = StringView(s).to_int();
            value = opt.value_or(0);
            return opt.has_value();
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(unsigned& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            auto opt = StringView(s).to_uint();
            value = opt.value_or(0);
            return opt.has_value();
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(double& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            auto opt = convert_to_double(s);
            value = opt.value_or(0.0);
            return opt.has_value();
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(Optional<double>& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            value = convert_to_double(s);
            return value.has_value();
        }
    };
    add_option(move(option));
}

void ArgsParser::add_option(Optional<size_t>& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            value = AK::StringUtils::convert_to_uint<size_t>(s);
            return value.has_value();
        }
    };
    add_option(move(option));
}

void ArgsParser::add_positional_argument(Arg&& arg)
{
    m_positional_args.append(move(arg));
}

void ArgsParser::add_positional_argument(const char*& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(String& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(StringView& value, char const* help_string, char const* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            value = s;
            return true;
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(int& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            auto opt = StringView(s).to_int();
            value = opt.value_or(0);
            return opt.has_value();
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(unsigned& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            auto opt = StringView(s).to_uint();
            value = opt.value_or(0);
            return opt.has_value();
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(double& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            auto opt = convert_to_double(s);
            value = opt.value_or(0.0);
            return opt.has_value();
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(Vector<const char*>& values, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        INT_MAX,
        [&values](const char* s) {
            values.append(s);
            return true;
        }
    };
    add_positional_argument(move(arg));
}

void ArgsParser::add_positional_argument(Vector<StringView>& values, char const* help_string, char const* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        INT_MAX,
        [&values](char const* s) {
            values.append(s);
            return true;
        }
    };
    add_positional_argument(move(arg));
}

}
