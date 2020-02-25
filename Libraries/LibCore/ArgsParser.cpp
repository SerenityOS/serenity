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

#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <getopt.h>
#include <limits.h>
#include <stdio.h>

namespace Core {

ArgsParser::ArgsParser()
{
    add_option(m_show_help, "Display this message", "help", 0);
}

void ArgsParser::parse(int argc, char** argv)
{
    auto print_usage_and_exit = [this, argv] {
        print_usage(stderr, argv[0]);
        exit(1);
    };
    Vector<option> long_options;
    StringBuilder short_options_builder;

    int index_of_found_long_option = -1;

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
            print_usage_and_exit();
        }

        // Let's see what option we just found.
        Option* found_option = nullptr;
        if (c == 0) {
            // It was a long option.
            ASSERT(index_of_found_long_option >= 0);
            found_option = &m_options[index_of_found_long_option];
            index_of_found_long_option = -1;
        } else {
            // It was a short option, look it up.
            auto it = m_options.find([c](auto& opt) { return c == opt.short_name; });
            ASSERT(!it.is_end());
            found_option = &*it;
        }
        ASSERT(found_option);

        const char* arg = found_option->requires_argument ? optarg : nullptr;
        if (!found_option->accept_value(arg)) {
            fprintf(stderr, "Invalid value for option %s\n", found_option->name_for_display().characters());
            print_usage_and_exit();
        }
    }

    // We're done processing options, now let's parse positional arguments.

    int values_left = argc - optind;
    int num_values_for_arg[m_positional_args.size()];
    int total_values_required = 0;
    for (size_t i = 0; i < m_positional_args.size(); i++) {
        auto& arg = m_positional_args[i];
        num_values_for_arg[i] = arg.min_values;
        total_values_required += arg.min_values;
    }

    if (total_values_required > values_left)
        print_usage_and_exit();
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
        print_usage_and_exit();
    }

    for (size_t i = 0; i < m_positional_args.size(); i++) {
        auto& arg = m_positional_args[i];
        for (int j = 0; j < num_values_for_arg[i]; j++) {
            const char* value = argv[optind++];
            if (!arg.accept_value(value)) {
                fprintf(stderr, "Invalid value for argument %s\n", arg.name);
                print_usage_and_exit();
            }
        }
    }

    // We're done parsing! :)
    // Now let's show help if requested.
    if (m_show_help) {
        print_usage(stdout, argv[0]);
        exit(0);
    }
}

void ArgsParser::print_usage(FILE* file, const char* argv0)
{
    fprintf(file, "Usage:\n\t%s", argv0);

    for (auto& opt : m_options) {
        if (opt.long_name && !strcmp(opt.long_name, "help"))
            continue;
        if (opt.requires_argument)
            fprintf(file, " [%s %s]", opt.name_for_display().characters(), opt.value_name);
        else
            fprintf(file, " [%s]", opt.name_for_display().characters());
    }
    for (auto& arg : m_positional_args) {
        bool required = arg.min_values > 0;
        bool repeated = arg.max_values > 1;

        if (required && repeated)
            fprintf(file, " <%s...>", arg.name);
        else if (required && !repeated)
            fprintf(file, " <%s>", arg.name);
        else if (!required && repeated)
            fprintf(file, " [%s...]", arg.name);
        else if (!required && !repeated)
            fprintf(file, " [%s]", arg.name);
    }

    if (!m_options.is_empty())
        fprintf(file, "\nOptions:\n");

    for (auto& opt : m_options) {
        auto print_argument = [&]() {
            if (opt.value_name) {
                if (opt.requires_argument)
                    fprintf(file, " %s", opt.value_name);
                else
                    fprintf(file, " [%s]", opt.value_name);
            }
        };
        fprintf(file, "\t");
        if (opt.short_name) {
            fprintf(file, "-%c", opt.short_name);
            print_argument();
        }
        if (opt.short_name && opt.long_name)
            fprintf(file, ", ");
        if (opt.long_name) {
            fprintf(file, "--%s", opt.long_name);
            print_argument();
        }

        if (opt.help_string)
            fprintf(file, "\t%s", opt.help_string);
        fprintf(file, "\n");
    }

    if (!m_positional_args.is_empty())
        fprintf(file, "\nArguments:\n");

    for (auto& arg : m_positional_args) {
        fprintf(file, "\t%s", arg.name);
        if (arg.help_string)
            fprintf(file, "\t%s", arg.help_string);
        fprintf(file, "\n");
    }
}

void ArgsParser::add_option(Option&& option)
{
    m_options.append(move(option));
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
            ASSERT(s == nullptr);
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

void ArgsParser::add_option(int& value, const char* help_string, const char* long_name, char short_name, const char* value_name)
{
    Option option {
        true,
        help_string,
        long_name,
        short_name,
        value_name,
        [&value](const char* s) {
            bool ok;
            value = StringView(s).to_int(ok);
            return ok;
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

void ArgsParser::add_positional_argument(int& value, const char* help_string, const char* name, Required required)
{
    Arg arg {
        help_string,
        name,
        required == Required::Yes ? 1 : 0,
        1,
        [&value](const char* s) {
            bool ok;
            value = StringView(s).to_int(ok);
            return ok;
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

}
