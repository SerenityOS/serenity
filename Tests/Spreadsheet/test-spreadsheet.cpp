/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Map.h>
#include <LibTest/JavaScriptTestRunner.h>

TEST_ROOT("Userland/Applications/Spreadsheet/Tests");

#ifdef AK_OS_SERENITY
static constexpr auto s_spreadsheet_runtime_path = "/res/js/Spreadsheet/runtime.js"sv;
#else
static constexpr auto s_spreadsheet_runtime_path = "../../../../Base/res/js/Spreadsheet/runtime.js"sv;
#endif

TESTJS_RUN_FILE_FUNCTION(ByteString const&, JS::Realm& realm, JS::ExecutionContext& global_execution_context)
{
    auto run_file = [&](StringView name) {
        auto result = Test::JS::parse_script(name, realm);
        if (result.is_error()) {
            warnln("Unable to parse {}", name);
            warnln("{}", result.error().error.to_byte_string());
            warnln("{}", result.error().hint);
            Test::cleanup_and_exit();
        }
        auto script = result.release_value();

        realm.vm().push_execution_context(global_execution_context);
        MUST(realm.vm().bytecode_interpreter().run(*script));
        realm.vm().pop_execution_context();
    };

#ifdef AK_OS_SERENITY
    run_file(s_spreadsheet_runtime_path);
#else
    run_file(LexicalPath::join(Test::JS::g_test_root, s_spreadsheet_runtime_path).string());
#endif

    run_file("mock.test-common.js"sv);

    return Test::JS::RunFileHookResult::RunAsNormal;
}
