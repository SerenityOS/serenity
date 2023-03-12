/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OptionParser.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <getopt.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int opterr = 1;
int optopt = 0;
int optind = 1;
int optreset = 0;
char* optarg = nullptr;

// POSIX says, "When an element of argv[] contains multiple option characters,
// it is unspecified how getopt() determines which options have already been
// processed". Well, this is how we do it.
namespace {
Vector<StringView> s_args;
OptionParser s_parser;
}

int getopt(int argc, char* const* argv, char const* short_options)
{
    s_args.clear_with_capacity();
    s_args.ensure_capacity(argc);
    for (auto i = 1; i < argc; ++i)
        s_args.append({ argv[i], strlen(argv[i]) });

    if (optind == 1 || optreset == 1) {
        s_parser.reset_state();
        optind = 1;
        optreset = 0;
    }

    auto result = s_parser.getopt(s_args.span(), { short_options, strlen(short_options) }, {}, {});

    optind += result.consumed_args;
    optarg = result.optarg_value.map([](auto x) { return const_cast<char*>(x.characters_without_null_termination()); }).value_or(optarg);
    optopt = result.optopt_value.value_or(optopt);
    return result.result;
}

int getopt_long(int argc, char* const* argv, char const* short_options, const struct option* long_options, int* out_long_option_index)
{
    s_args.clear_with_capacity();
    s_args.ensure_capacity(argc);
    for (auto i = 1; i < argc; ++i)
        s_args.append({ argv[i], strlen(argv[i]) });

    size_t long_option_count = 0;
    for (auto option = long_options; option && option->name; option += 1)
        long_option_count++;

    Vector<OptionParser::Option> translated_long_options;
    translated_long_options.ensure_capacity(long_option_count);
    for (size_t i = 0; i < long_option_count; ++i) {
        auto option = &long_options[i];

        translated_long_options.append(OptionParser::Option {
            .name = { option->name, strlen(option->name) },
            .requirement = option->has_arg == no_argument
                ? AK::OptionParser::ArgumentRequirement::NoArgument
                : option->has_arg == optional_argument
                ? AK::OptionParser::ArgumentRequirement::HasOptionalArgument
                : AK::OptionParser::ArgumentRequirement::HasRequiredArgument,
            .flag = option->flag,
            .val = option->val,
        });
    }

    if (optind == 1 || optreset == 1) {
        s_parser.reset_state();
        optind = 1;
        optreset = 0;
    }

    auto result = s_parser.getopt(
        s_args.span(),
        { short_options, strlen(short_options) },
        translated_long_options.span(),
        out_long_option_index ? *out_long_option_index : Optional<int&>());

    optind += result.consumed_args;
    optarg = result.optarg_value.map([](auto x) { return const_cast<char*>(x.characters_without_null_termination()); }).value_or(optarg);
    optopt = result.optopt_value.value_or(optopt);
    return result.result;
}
