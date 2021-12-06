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
    auto source = TRY(vm.argument(0).to_string(global_object));
    auto parser = JS::Parser(JS::Lexer(source));
    (void)parser.parse_program();
    return JS::Value(!parser.has_errors());
}

TESTJS_GLOBAL_FUNCTION(run_queued_promise_jobs, runQueuedPromiseJobs)
{
    vm.run_queued_promise_jobs();
    return JS::js_undefined();
}

TESTJS_GLOBAL_FUNCTION(get_weak_set_size, getWeakSetSize)
{
    auto* object = TRY(vm.argument(0).to_object(global_object));
    if (!is<JS::WeakSet>(object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "WeakSet");
    auto* weak_set = static_cast<JS::WeakSet*>(object);
    return JS::Value(weak_set->values().size());
}

TESTJS_GLOBAL_FUNCTION(get_weak_map_size, getWeakMapSize)
{
    auto* object = TRY(vm.argument(0).to_object(global_object));
    if (!is<JS::WeakMap>(object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "WeakMap");
    auto* weak_map = static_cast<JS::WeakMap*>(object);
    return JS::Value(weak_map->values().size());
}

TESTJS_GLOBAL_FUNCTION(mark_as_garbage, markAsGarbage)
{
    auto argument = vm.argument(0);
    if (!argument.is_string())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAString, argument.to_string_without_side_effects());

    auto& variable_name = argument.as_string();

    // In native functions we don't have a lexical environment so get the outer via the execution stack.
    auto outer_environment = vm.execution_context_stack().last_matching([&](auto& execution_context) {
        return execution_context->lexical_environment != nullptr;
    });
    if (!outer_environment.has_value())
        return vm.throw_completion<JS::ReferenceError>(global_object, JS::ErrorType::UnknownIdentifier, variable_name.string());

    auto reference = vm.resolve_binding(variable_name.string(), outer_environment.value()->lexical_environment);

    auto value = TRY(reference.get_value(global_object));

    if (!value.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObject, String::formatted("Variable with name {}", variable_name.string()));

    vm.heap().uproot_cell(&value.as_object());
    TRY(reference.delete_(global_object));

    return JS::js_undefined();
}

TESTJS_RUN_FILE_FUNCTION(String const& test_file, JS::Interpreter& interpreter)
{
    if (!test262_parser_tests)
        return Test::JS::RunFileHookResult::RunAsNormal;

    auto start_time = Test::get_time_in_ms();

    LexicalPath path(test_file);
    auto dirname = path.dirname();
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

    auto program_type = path.basename().ends_with(".module.js") ? JS::Program::Type::Module : JS::Program::Type::Script;
    bool parse_succeeded = false;
    if (program_type == JS::Program::Type::Module)
        parse_succeeded = !Test::JS::parse_module(test_file, interpreter.realm()).is_error();
    else
        parse_succeeded = !Test::JS::parse_script(test_file, interpreter.realm()).is_error();

    bool test_passed = true;
    String message;
    String expectation_string;

    switch (expectation) {
    case Early:
    case Fail:
        expectation_string = "File should not parse";
        test_passed = !parse_succeeded;
        if (!test_passed)
            message = "Expected the file to fail parsing, but it did not";
        break;
    case Pass:
    case ExplicitPass:
        expectation_string = "File should parse";
        test_passed = parse_succeeded;
        if (!test_passed)
            message = "Expected the file to parse, but it did not";
        break;
    }

    auto test_result = test_passed ? Test::Result::Pass : Test::Result::Fail;

    return Test::JS::JSFileResult {
        LexicalPath::relative_path(test_file, Test::JS::g_test_root),
        {},
        Test::get_time_in_ms() - start_time,
        test_result,
        { Test::Suite { "Parse file", test_result, { { expectation_string, test_result, message } } } }
    };
}
