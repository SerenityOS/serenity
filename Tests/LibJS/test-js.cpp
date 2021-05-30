/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/JavaScriptTestRunner.h>

TEST_ROOT("Userland/Libraries/LibJS/Tests");

TESTJS_PROGRAM_FLAG(test262_parser_tests, "Run test262 parser tests", "test262-parser-tests", 0);

TESTJS_GLOBAL_FUNCTION(is_strict_mode, isStrictMode, 0)
{
    return JS::Value(vm.in_strict_mode());
}

TESTJS_GLOBAL_FUNCTION(can_parse_source, canParseSource)
{
    auto source = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto parser = JS::Parser(JS::Lexer(source));
    parser.parse_program();
    return JS::Value(!parser.has_errors());
}

TESTJS_GLOBAL_FUNCTION(run_queued_promise_jobs, runQueuedPromiseJobs)
{
    vm.run_queued_promise_jobs();
    return JS::js_undefined();
}

TESTJS_RUN_FILE_FUNCTION(const String& test_file, JS::Interpreter&)
{
    if (!test262_parser_tests)
        return Test::JS::RunFileHookResult::RunAsNormal;

    auto start_time = Test::JS::get_time_in_ms();

    LexicalPath path(test_file);
    auto& dirname = path.dirname();
    enum {
        Early,
        Fail,
        Pass,
        ExplicitPass,
    } expectation { Pass };

    if (dirname.ends_with("early"))
        expectation = Early;
    else if (dirname.ends_with("fail"))
        expectation = Fail;
    else if (dirname.ends_with("pass-explicit"))
        expectation = ExplicitPass;
    else if (dirname.ends_with("pass"))
        expectation = Pass;
    else
        return Test::JS::RunFileHookResult::SkipFile;

    auto parse_result = Test::JS::parse_file(test_file);
    bool test_passed = true;
    String message;
    String expectation_string;

    switch (expectation) {
    case Early:
    case Fail:
        expectation_string = "File should not parse";
        test_passed = parse_result.is_error();
        if (!test_passed)
            message = "Expected the file to fail parsing, but it did not";
        break;
    case Pass:
    case ExplicitPass:
        expectation_string = "File should parse";
        test_passed = !parse_result.is_error();
        if (!test_passed)
            message = "Expected the file to parse, but it did not";
        break;
    }

    auto test_result = test_passed ? Test::Result::Pass : Test::Result::Fail;

    return Test::JS::JSFileResult {
        LexicalPath::relative_path(test_file, Test::JS::g_test_root),
        {},
        Test::JS::get_time_in_ms() - start_time,
        test_result,
        { Test::Suite { "Parse file", test_result, { { expectation_string, test_result, message } } } }
    };
}
