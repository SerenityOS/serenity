/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <signal.h>
#include <stdio.h>

namespace Test {

TestRunner* ::Test::TestRunner::s_the = nullptr;

namespace JS {

RefPtr<::JS::VM> g_vm;
bool g_collect_on_every_allocation = false;
bool g_run_bytecode = false;
String g_currently_running_test;
HashMap<String, FunctionWithLength> s_exposed_global_functions;
Function<void()> g_main_hook;
HashMap<bool*, Tuple<String, String, char>> g_extra_args;
IntermediateRunFileResult (*g_run_file)(const String&, JS::Interpreter&) = nullptr;
String g_test_root;
int g_test_argc;
char** g_test_argv;

} // namespace JS
} // namespace Test

using namespace Test::JS;

static StringView g_program_name { "test-js"sv };

static void handle_sigabrt(int)
{
    dbgln("{}: SIGABRT received, cleaning up.", g_program_name);
    Test::cleanup();
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_DFL;
    int rc = sigaction(SIGABRT, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        exit(1);
    }
    abort();
}

int main(int argc, char** argv)
{
    g_test_argc = argc;
    g_test_argv = argv;
    auto program_name = LexicalPath::basename(argv[0]);
    g_program_name = program_name;

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = handle_sigabrt;
    int rc = sigaction(SIGABRT, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

#ifdef SIGINFO
    signal(SIGINFO, [](int) {
        static char buffer[4096];
        auto& counts = ::Test::TestRunner::the()->counts();
        int len = snprintf(buffer, sizeof(buffer), "Pass: %d, Fail: %d, Skip: %d\nCurrent test: %s\n", counts.tests_passed, counts.tests_failed, counts.tests_skipped, g_currently_running_test.characters());
        write(STDOUT_FILENO, buffer, len);
    });
#endif

    bool print_times = false;
    bool print_progress =
#ifdef __serenity__
        true; // Use OSC 9 to print progress
#else
        false;
#endif
    bool print_json = false;
    const char* specified_test_root = nullptr;
    String common_path;
    String test_glob;

    Core::ArgsParser args_parser;
    args_parser.add_option(print_times, "Show duration of each test", "show-time", 't');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Show progress with OSC 9 (true, false)",
        .long_name = "show-progress",
        .short_name = 'p',
        .accept_value = [&](auto* str) {
            if ("true"sv == str)
                print_progress = true;
            else if ("false"sv == str)
                print_progress = false;
            else
                return false;
            return true;
        },
    });
    args_parser.add_option(print_json, "Show results as JSON", "json", 'j');
    args_parser.add_option(g_collect_on_every_allocation, "Collect garbage after every allocation", "collect-often", 'g');
    args_parser.add_option(g_run_bytecode, "Use the bytecode interpreter", "run-bytecode", 'b');
    args_parser.add_option(JS::Bytecode::g_dump_bytecode, "Dump the bytecode", "dump-bytecode", 'd');
    args_parser.add_option(test_glob, "Only run tests matching the given glob", "filter", 'f', "glob");
    for (auto& entry : g_extra_args)
        args_parser.add_option(*entry.key, entry.value.get<0>().characters(), entry.value.get<1>().characters(), entry.value.get<2>());
    args_parser.add_positional_argument(specified_test_root, "Tests root directory", "path", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(common_path, "Path to tests-common.js", "common-path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    test_glob = String::formatted("*{}*", test_glob);

    if (getenv("DISABLE_DBG_OUTPUT")) {
        AK::set_debug_enabled(false);
    }

    if (JS::Bytecode::g_dump_bytecode && !g_run_bytecode) {
        warnln("--dump-bytecode can only be used when --run-bytecode is specified.");
        return 1;
    }

    String test_root;

    if (specified_test_root) {
        test_root = String { specified_test_root };
    } else {
#ifdef __serenity__
        test_root = LexicalPath::join("/home/anon", String::formatted("{}-tests", program_name.split_view('-').last())).string();
#else
        char* serenity_source_dir = getenv("SERENITY_SOURCE_DIR");
        if (!serenity_source_dir) {
            warnln("No test root given, {} requires the SERENITY_SOURCE_DIR environment variable to be set", g_program_name);
            return 1;
        }
        test_root = String::formatted("{}/{}", serenity_source_dir, g_test_root_fragment);
        common_path = String::formatted("{}/Userland/Libraries/LibJS/Tests/test-common.js", serenity_source_dir);
#endif
    }
    if (!Core::File::is_directory(test_root)) {
        warnln("Test root is not a directory: {}", test_root);
        return 1;
    }

    if (common_path.is_empty()) {
#ifdef __serenity__
        common_path = "/home/anon/js-tests/test-common.js";
#else
        char* serenity_source_dir = getenv("SERENITY_SOURCE_DIR");
        if (!serenity_source_dir) {
            warnln("No test root given, {} requires the SERENITY_SOURCE_DIR environment variable to be set", g_program_name);
            return 1;
        }
        common_path = String::formatted("{}/Userland/Libraries/LibJS/Tests/test-common.js", serenity_source_dir);
#endif
    }

    test_root = Core::File::real_path_for(test_root);
    common_path = Core::File::real_path_for(common_path);

    if (chdir(test_root.characters()) < 0) {
        auto saved_errno = errno;
        warnln("chdir failed: {}", strerror(saved_errno));
        return 1;
    }

    if (g_main_hook)
        g_main_hook();

    if (!g_vm) {
        g_vm = JS::VM::create();
        g_vm->enable_default_host_import_module_dynamically_hook();
    }

    Test::JS::TestRunner test_runner(test_root, common_path, print_times, print_progress, print_json);
    test_runner.run(test_glob);

    g_vm = nullptr;

    return test_runner.counts().tests_failed > 0 ? 1 : 0;
}
