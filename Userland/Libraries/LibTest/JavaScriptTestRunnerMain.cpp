/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/JavaScriptTestRunner.h>
#include <signal.h>
#include <stdio.h>

namespace Test::JS {

RefPtr<::JS::VM> g_vm;
bool g_collect_on_every_allocation = false;
String g_currently_running_test;
String g_test_glob;
HashMap<String, FunctionWithLength> s_exposed_global_functions;
Function<void()> g_main_hook;
HashMap<bool*, Tuple<String, String, char>> g_extra_args;
IntermediateRunFileResult (*g_run_file)(const String&, JS::Interpreter&) = nullptr;
TestRunner* TestRunner::s_the = nullptr;
String g_test_root;
int g_test_argc;
char** g_test_argv;

}

using namespace Test::JS;

static StringView g_program_name { "test-js"sv };

static void handle_sigabrt(int)
{
    dbgln("{}: SIGABRT received, cleaning up.", g_program_name);
    cleanup();
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
    auto program_name = LexicalPath { argv[0] }.basename();
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
        auto& counts = TestRunner::the()->counts();
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

    Core::ArgsParser args_parser;
    args_parser.add_option(print_times, "Show duration of each test", "show-time", 't');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Show progress with OSC 9 (true, false)",
        .long_name = "show-progress",
        .short_name = 'p',
        .accept_value = [&](auto* str) {
            if (StringView { "true" } == str)
                print_progress = true;
            else if (StringView { "false" } == str)
                print_progress = false;
            else
                return false;
            return true;
        },
    });
    args_parser.add_option(print_json, "Show results as JSON", "json", 'j');
    args_parser.add_option(g_collect_on_every_allocation, "Collect garbage after every allocation", "collect-often", 'g');
    args_parser.add_option(g_test_glob, "Only run tests matching the given glob", "filter", 'f', "glob");
    for (auto& entry : g_extra_args)
        args_parser.add_option(*entry.key, entry.value.get<0>().characters(), entry.value.get<1>().characters(), entry.value.get<2>());
    args_parser.add_positional_argument(specified_test_root, "Tests root directory", "path", Core::ArgsParser::Required::No);
    args_parser.add_positional_argument(common_path, "Path to tests-common.js", "common-path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    g_test_glob = String::formatted("*{}*", g_test_glob);

    if (getenv("DISABLE_DBG_OUTPUT")) {
        AK::set_debug_enabled(false);
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

    if (!g_vm)
        g_vm = JS::VM::create();

    TestRunner test_runner(test_root, common_path, print_times, print_progress, print_json);
    test_runner.run();

    g_vm = nullptr;

    return test_runner.counts().tests_failed > 0 ? 1 : 0;
}
