/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/JavaScriptTestRunner.h>

TEST_ROOT("Userland/Libraries/LibJS/Tests");

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
