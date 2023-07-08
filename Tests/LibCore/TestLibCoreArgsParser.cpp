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

class ParserResult {
public:
    ParserResult(Vector<StringView> arguments)
        : arguments(move(arguments))
    {
    }

    ParserResult(ParserResult&& other)
    {
        arguments = move(other.arguments);
        result = other.result;
    }

    ParserResult& operator=(ParserResult&& other)
    {
        if (this != &other) {
            arguments = move(other.arguments);
            result = other.result;
        }
        return *this;
    }

    Vector<StringView> arguments;
    bool result { false };
};

static ParserResult run_parser(Vector<StringView> arguments, Function<void(Core::ArgsParser&)> parser_initialization = {})
{
    Core::ArgsParser parser;
    if (parser_initialization)
        parser_initialization(parser);

    auto parse_result = ParserResult { move(arguments) };
    parse_result.result = parser.parse(parse_result.arguments, Core::ArgsParser::FailureBehavior::Ignore);
    return parse_result;
}

TEST_CASE(no_arguments)
{
    auto parser_result = run_parser({ "app"sv });
    EXPECT_EQ(parser_result.result, true);
}

TEST_CASE(bool_option)
{
    // Short option
    bool force = false;
    auto parser_result = run_parser({ "app"sv, "-f"sv }, [&](auto& parser) {
        parser.add_option(force, "force", nullptr, 'f');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, true);

    // Short option, not given
    force = false;
    parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_option(force, "force", nullptr, 'f');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, false);

    // Long option
    force = false;
    parser_result = run_parser({ "app"sv, "--force"sv }, [&](auto& parser) {
        parser.add_option(force, "force", "force", '\0');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, true);

    // Long option, not given
    force = false;
    parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_option(force, "force", "force", '\0');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, false);

    // Allow both short and long option, provide short
    force = false;
    parser_result = run_parser({ "app"sv, "-f"sv }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, true);

    // Allow both short and long option, provide long
    force = false;
    parser_result = run_parser({ "app"sv, "--force"sv }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, true);

    // Allow both short and long option, provide both
    force = false;
    parser_result = run_parser({ "app"sv, "--force"sv, "-f"sv }, [&](auto& parser) {
        parser.add_option(force, "force", "force", 'f');
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(force, true);
}

TEST_CASE(positional_string_argument)
{
    // Single required string argument
    DeprecatedString name = "";
    auto parser_result = run_parser({ "app"sv, "buggie"sv }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(name, "buggie");

    // Single required string argument, not given
    name = "";
    parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, false);
    EXPECT_EQ(name, "");

    // Single optional string argument
    name = "";
    parser_result = run_parser({ "app"sv, "buggie"sv }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(name, "buggie");

    // Single optional string argument, not given
    name = "";
    parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_positional_argument(name, "name", "name", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(name, "");
}

TEST_CASE(positional_vector_string_argument)
{
    Vector<StringView> values;

    // Zero or more positional arguments, zero given
    auto parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(values.size(), 0u);

    // Zero or more positional arguments, one given
    values = {};
    parser_result = run_parser({ "app"sv, "one"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(values.size(), 1u);
    if (values.size() == 1u)
        EXPECT_EQ(values[0], "one");

    // Zero or more positional arguments, two given
    values = {};
    parser_result = run_parser({ "app"sv, "one"sv, "two"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(values.size(), 2u);
    if (values.size() == 2u) {
        EXPECT_EQ(values[0], "one");
        EXPECT_EQ(values[1], "two");
    }

    // One or more positional arguments, zero given
    values = {};
    parser_result = run_parser({ "app"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, false);
    EXPECT_EQ(values.size(), 0u);

    // One or more positional arguments, one given
    values = {};
    parser_result = run_parser({ "app"sv, "one"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(values.size(), 1u);
    if (values.size() == 1u)
        EXPECT_EQ(values[0], "one");

    // One or more positional arguments, two given
    values = {};
    parser_result = run_parser({ "app"sv, "one"sv, "two"sv }, [&](auto& parser) {
        parser.add_positional_argument(values, "values", "values", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(values.size(), 2u);
    if (values.size() == 2u) {
        EXPECT_EQ(values[0], "one");
        EXPECT_EQ(values[1], "two");
    }
}

TEST_CASE(combination_of_bool_options_with_positional_vector_string)
{
    Vector<StringView> positionals;
    // Bool options (given) and positional arguments (given)
    // Expected: all arguments fill as given
    bool bool_opt1 = false;
    bool bool_opt2 = false;
    auto parser_result = run_parser({ "app"sv, "-b"sv, "-c"sv, "one"sv, "two"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, true);
    EXPECT_EQ(positionals.size(), 2u);
    if (positionals.size() == 2u) {
        EXPECT_EQ(positionals[0], "one");
        EXPECT_EQ(positionals[1], "two");
    }

    // Bool options (missing) and positional arguments (given)
    // Expected: only the positional arguments are filled
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "one"sv, "two"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, false);
    EXPECT_EQ(bool_opt2, false);
    EXPECT_EQ(positionals.size(), 2u);
    if (positionals.size() == 2u) {
        EXPECT_EQ(positionals[0], "one");
        EXPECT_EQ(positionals[1], "two");
    }

    // Bool options (given) and positional arguments (missing)
    // Expected: only the bool options are filled
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "-c"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, true);
    EXPECT_EQ(positionals.size(), 0u);

    // Bool options (missing) and positional arguments (given) using double dash
    // Expected: the bool options are interpreted as positional arguments
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "--"sv, "-b"sv, "-c"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, false);
    EXPECT_EQ(bool_opt2, false);
    EXPECT_EQ(positionals.size(), 2u);
    if (positionals.size() == 2u) {
        EXPECT_EQ(positionals[0], "-b");
        EXPECT_EQ(positionals[1], "-c");
    }

    // Bool options (one given) and positional arguments (one given) using double dash
    // Expected: bool_opt1 is set, one positional is added
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "--"sv, "-c"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, false);
    EXPECT_EQ(positionals.size(), 1u);
    if (positionals.size() == 1u) {
        EXPECT_EQ(positionals[0], "-c");
    }

    // Bool options (three given, one incorrect) and positional arguments (missing)
    // Expected: parser fails
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "-d"sv, "-c"sv }, [&](auto& parser) {
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::No);
    });
    EXPECT_EQ(parser_result.result, false);
}

TEST_CASE(stop_on_first_non_option)
{
    // Do not stop on first non-option; arguments in correct order
    // Expected: bool options are set and one positional argument is filled
    bool bool_opt1 = false;
    bool bool_opt2 = false;
    Vector<StringView> positionals;
    auto parser_result = run_parser({ "app"sv, "-b"sv, "-c"sv, "one"sv }, [&](auto& parser) {
        parser.set_stop_on_first_non_option(false);
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, true);
    EXPECT_EQ(positionals.size(), 1u);
    if (positionals.size() == 1u)
        EXPECT_EQ(positionals[0], "one");

    // Do not stop on first non-option; arguments in wrong order
    // Expected: bool options are set and one positional argument is filled
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "one"sv, "-c"sv }, [&](auto& parser) {
        parser.set_stop_on_first_non_option(false);
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, true);
    EXPECT_EQ(positionals.size(), 1u);
    if (positionals.size() == 1u)
        EXPECT_EQ(positionals[0], "one");

    // Stop on first non-option; arguments in correct order
    // Expected: bool options are set and one positional argument is filled
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "-c"sv, "one"sv }, [&](auto& parser) {
        parser.set_stop_on_first_non_option(true);
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, true);
    EXPECT_EQ(positionals.size(), 1u);
    if (positionals.size() == 1u)
        EXPECT_EQ(positionals[0], "one");

    // Stop on first non-option; arguments in wrong order
    // Expected: bool_opt1 is set, other arguments are filled as positional arguments
    bool_opt1 = false;
    bool_opt2 = false;
    positionals = {};
    parser_result = run_parser({ "app"sv, "-b"sv, "one"sv, "-c"sv }, [&](auto& parser) {
        parser.set_stop_on_first_non_option(true);
        parser.add_option(bool_opt1, "bool_opt1", nullptr, 'b');
        parser.add_option(bool_opt2, "bool_opt2", nullptr, 'c');
        parser.add_positional_argument(positionals, "pos", "pos", Core::ArgsParser::Required::Yes);
    });
    EXPECT_EQ(parser_result.result, true);
    EXPECT_EQ(bool_opt1, true);
    EXPECT_EQ(bool_opt2, false);
    EXPECT_EQ(positionals.size(), 2u);
    if (positionals.size() == 2u) {
        EXPECT_EQ(positionals[0], "one");
        EXPECT_EQ(positionals[1], "-c");
    }
}
