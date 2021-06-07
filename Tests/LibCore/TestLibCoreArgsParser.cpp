/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibTest/TestCase.h>
#include <string.h>

static char** build_argv(Vector<String> arguments)
{
    auto argv = new char*[arguments.size() + 1];
    size_t idx = 0;
    for (auto& argument : arguments) {
        auto char_argument = new char[argument.length() + 1];
        memcpy(char_argument, argument.characters(), argument.length());
        char_argument[argument.length()] = '\0';
        argv[idx++] = char_argument;
    }
    argv[idx++] = nullptr;
    return argv;
}

static void delete_argv(char** argv, size_t number_of_arguments)
{
    for (size_t idx = 0; idx < number_of_arguments; ++idx)
        delete[] argv[idx];
    delete[] argv;
}

static bool run_parser(Vector<String> arguments, Function<void(Core::ArgsParser&)> parser_initialization = {})
{
    Core::ArgsParser parser;
    if (parser_initialization)
        parser_initialization(parser);

    auto argv = build_argv(arguments);
    auto result = parser.parse(arguments.size(), argv, Core::ArgsParser::FailureBehavior::Ignore);
    delete_argv(argv, arguments.size());

    return result;
}

TEST_CASE(no_arguments)
{
    auto parser_result = run_parser({ "app" });
    EXPECT_EQ(parser_result, true);
}

TEST_CASE(bool_option)
{
    // Short option
    bool force = false;
    auto parser_result = run_parser({ "app", "-f" }, [&](auto& parser) {
        parser.add_option(force, "force", nullptr, 'f');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, true);

    // Short option, not given
    force = false;
    parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_option(force, "force", nullptr, 'f');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, false);

    // Long option
    force = false;
    parser_result = run_parser({ "app", "--force" }, [&](auto& parser) {
        parser.add_option(force, "force", "force", '\0');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, true);

    // Long option, not given
    force = false;
    parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_option(force, "force", "force", '\0');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, false);

    // Allow both short and long option, provide short
    force = false;
    parser_result = run_parser({ "app", "-f" }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, true);

    // Allow both short and long option, provide long
    force = false;
    parser_result = run_parser({ "app", "--force" }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, true);

    // Allow both short and long option, provide both
    force = false;
    parser_result = run_parser({ "app", "--force", "-f" }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(force, true);
}

TEST_CASE(positional_string_argument)
{
    // Single required string argument
    String name = "";
    auto parser_result = run_parser({ "app", "buggie" }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(name, "buggie");

    // Single required string argument, not given
    name = "";
    parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result, false);
    EXPECT_EQ(name, "");

    // Single optional string argument
    name = "";
    parser_result = run_parser({ "app", "buggie" }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(name, "buggie");

    // Single optional string argument, not given
    name = "";
    parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(name, "");
}

TEST_CASE(positional_vector_string_argument)
{
    // Zero or more positional arguments, zero given
    Vector<String> values = {};
    auto parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(values.size(), 0u);

    // Zero or more positional arguments, one given
    values = {};
    parser_result = run_parser({ "app", "one" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(values.size(), 1u);
    if (values.size() == 1u)
        EXPECT_EQ(values[0], "one");

    // Zero or more positional arguments, two given
    values = {};
    parser_result = run_parser({ "app", "one", "two" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(values.size(), 2u);
    if (values.size() == 2u) {
        EXPECT_EQ(values[0], "one");
        EXPECT_EQ(values[1], "two");
    }

    // One or more positional arguments, zero given
    values = {};
    parser_result = run_parser({ "app" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result, false);
    EXPECT_EQ(values.size(), 0u);

    // One or more positional arguments, one given
    values = {};
    parser_result = run_parser({ "app", "one" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(values.size(), 1u);
    if (values.size() == 1u)
        EXPECT_EQ(values[0], "one");

    // One or more positional arguments, two given
    values = {};
    parser_result = run_parser({ "app", "one", "two" }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result, true);
    EXPECT_EQ(values.size(), 2u);
    if (values.size() == 2u) {
        EXPECT_EQ(values[0], "one");
        EXPECT_EQ(values[1], "two");
    }
}
}
