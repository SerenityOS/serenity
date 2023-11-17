/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <stdlib.h>
#include <time.h>

TEST_ROOT("Userland/Libraries/LibJS/Tests");

TESTJS_PROGRAM_FLAG(test262_parser_tests, "Run test262 parser tests", "test262-parser-tests", 0);

TESTJS_GLOBAL_FUNCTION(is_strict_mode, isStrictMode, 0)
{
    return JS::Value(vm.in_strict_mode());
}

TESTJS_GLOBAL_FUNCTION(can_parse_source, canParseSource)
{
    auto source = TRY(vm.argument(0).to_deprecated_string(vm));
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
    auto object = TRY(vm.argument(0).to_object(vm));
    if (!is<JS::WeakSet>(*object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WeakSet");
    auto& weak_set = static_cast<JS::WeakSet&>(*object);
    return JS::Value(weak_set.values().size());
}

TESTJS_GLOBAL_FUNCTION(get_weak_map_size, getWeakMapSize)
{
    auto object = TRY(vm.argument(0).to_object(vm));
    if (!is<JS::WeakMap>(*object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WeakMap");
    auto& weak_map = static_cast<JS::WeakMap&>(*object);
    return JS::Value(weak_map.values().size());
}

TESTJS_GLOBAL_FUNCTION(mark_as_garbage, markAsGarbage)
{
    auto argument = vm.argument(0);
    if (!argument.is_string())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAString, argument.to_string_without_side_effects());

    auto& variable_name = argument.as_string();

    // In native functions we don't have a lexical environment so get the outer via the execution stack.
    auto outer_environment = vm.execution_context_stack().last_matching([&](auto& execution_context) {
        return execution_context->lexical_environment != nullptr;
    });
    if (!outer_environment.has_value())
        return vm.throw_completion<JS::ReferenceError>(JS::ErrorType::UnknownIdentifier, variable_name.deprecated_string());

    auto reference = TRY(vm.resolve_binding(variable_name.deprecated_string(), outer_environment.value()->lexical_environment));

    auto value = TRY(reference.get_value(vm));

    if (!can_be_held_weakly(value))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::CannotBeHeldWeakly, DeprecatedString::formatted("Variable with name {}", variable_name.deprecated_string()));

    vm.heap().uproot_cell(&value.as_cell());
    TRY(reference.delete_(vm));

    return JS::js_undefined();
}

TESTJS_GLOBAL_FUNCTION(detach_array_buffer, detachArrayBuffer)
{
    auto array_buffer = vm.argument(0);
    if (!array_buffer.is_object() || !is<JS::ArrayBuffer>(array_buffer.as_object()))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "ArrayBuffer");

    auto& array_buffer_object = static_cast<JS::ArrayBuffer&>(array_buffer.as_object());
    TRY(JS::detach_array_buffer(vm, array_buffer_object, vm.argument(1)));
    return JS::js_null();
}

TESTJS_GLOBAL_FUNCTION(set_time_zone, setTimeZone)
{
    auto current_time_zone = JS::js_null();

    if (auto const* time_zone = getenv("TZ"))
        current_time_zone = JS::PrimitiveString::create(vm, StringView { time_zone, strlen(time_zone) });

    if (auto time_zone = vm.argument(0); time_zone.is_null()) {
        if (auto result = Core::System::unsetenv("TZ"sv); result.is_error())
            return vm.throw_completion<JS::InternalError>(MUST(String::formatted("Could not unset time zone: {}", result.error())));
    } else {
        if (auto result = Core::System::setenv("TZ"sv, TRY(time_zone.to_string(vm)), true); result.is_error())
            return vm.throw_completion<JS::InternalError>(MUST(String::formatted("Could not set time zone: {}", result.error())));
    }

    tzset();

    return current_time_zone;
}

TESTJS_RUN_FILE_FUNCTION(DeprecatedString const& test_file, JS::Realm& realm, JS::ExecutionContext&)
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

    if (dirname.ends_with("early"sv))
        expectation = Early;
    else if (dirname.ends_with("fail"sv))
        expectation = Fail;
    else if (dirname.ends_with("pass-explicit"sv))
        expectation = ExplicitPass;
    else if (dirname.ends_with("pass"sv))
        expectation = Pass;
    else
        return Test::JS::RunFileHookResult::SkipFile;

    auto program_type = path.basename().ends_with(".module.js"sv) ? JS::Program::Type::Module : JS::Program::Type::Script;
    bool parse_succeeded = false;
    if (program_type == JS::Program::Type::Module)
        parse_succeeded = !Test::JS::parse_module(test_file, realm).is_error();
    else
        parse_succeeded = !Test::JS::parse_script(test_file, realm).is_error();

    bool test_passed = true;
    DeprecatedString message;
    DeprecatedString expectation_string;

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
    auto test_path = LexicalPath::relative_path(test_file, Test::JS::g_test_root);
    auto duration_ms = Test::get_time_in_ms() - start_time;
    return Test::JS::JSFileResult {
        test_path,
        {},
        duration_ms,
        test_result,
        { Test::Suite { test_path, "Parse file", test_result, { { expectation_string, test_result, message, static_cast<u64>(duration_ms) * 1000u } } } }
    };
}
