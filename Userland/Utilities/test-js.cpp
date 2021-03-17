/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibTest/Results.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

#define TOP_LEVEL_TEST_NAME "__$$TOP_LEVEL$$__"

RefPtr<JS::VM> vm;

static bool collect_on_every_allocation = false;
static String currently_running_test;

struct ParserError {
    JS::Parser::Error error;
    String hint;
};

struct JSFileResult {
    String name;
    Optional<ParserError> error {};
    double time_taken { 0 };
    // A failed test takes precedence over a skipped test, which both have
    // precedence over a passed test
    Test::Result most_severe_test_result { Test::Result::Pass };
    Vector<Test::Suite> suites {};
    Vector<String> logged_messages {};
};

struct JSTestRunnerCounts {
    int tests_failed { 0 };
    int tests_passed { 0 };
    int tests_skipped { 0 };
    int suites_failed { 0 };
    int suites_passed { 0 };
    int files_total { 0 };
};

class TestRunnerGlobalObject final : public JS::GlobalObject {
    JS_OBJECT(TestRunnerGlobalObject, JS::GlobalObject);

public:
    TestRunnerGlobalObject();
    virtual ~TestRunnerGlobalObject() override;

    virtual void initialize_global_object() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(is_strict_mode);
    JS_DECLARE_NATIVE_FUNCTION(can_parse_source);
};

class TestRunner {
public:
    static TestRunner* the()
    {
        return s_the;
    }

    TestRunner(String test_root, bool print_times, bool print_progress)
        : m_test_root(move(test_root))
        , m_print_times(print_times)
        , m_print_progress(print_progress)
    {
        VERIFY(!s_the);
        s_the = this;
    }

    void run();

    const JSTestRunnerCounts& counts() const { return m_counts; }

    bool is_printing_progress() const { return m_print_progress; }

protected:
    static TestRunner* s_the;

    virtual Vector<String> get_test_paths() const;
    virtual JSFileResult run_file_test(const String& test_path);
    void print_file_result(const JSFileResult& file_result) const;
    void print_test_results() const;

    String m_test_root;
    bool m_print_times;
    bool m_print_progress;

    double m_total_elapsed_time_in_ms { 0 };
    JSTestRunnerCounts m_counts;

    RefPtr<JS::Program> m_test_program;
};

TestRunner* TestRunner::s_the = nullptr;

TestRunnerGlobalObject::TestRunnerGlobalObject()
{
}

TestRunnerGlobalObject::~TestRunnerGlobalObject()
{
}

void TestRunnerGlobalObject::initialize_global_object()
{
    Base::initialize_global_object();
    static FlyString global_property_name { "global" };
    static FlyString is_strict_mode_property_name { "isStrictMode" };
    static FlyString can_parse_source_property_name { "canParseSource" };
    define_property(global_property_name, this, JS::Attribute::Enumerable);
    define_native_function(is_strict_mode_property_name, is_strict_mode);
    define_native_function(can_parse_source_property_name, can_parse_source);
}

JS_DEFINE_NATIVE_FUNCTION(TestRunnerGlobalObject::is_strict_mode)
{
    return JS::Value(vm.in_strict_mode());
}

JS_DEFINE_NATIVE_FUNCTION(TestRunnerGlobalObject::can_parse_source)
{
    auto source = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto parser = JS::Parser(JS::Lexer(source));
    parser.parse_program();
    return JS::Value(!parser.has_errors());
}

static void cleanup_and_exit()
{
    // Clear the taskbar progress.
    if (TestRunner::the() && TestRunner::the()->is_printing_progress())
        warn("\033]9;-1;\033\\");
    exit(1);
}

static void handle_sigabrt(int)
{
    dbgln("test-js: SIGABRT received, cleaning up.");
    cleanup_and_exit();
}

static double get_time_in_ms()
{
    struct timeval tv1;
    auto return_code = gettimeofday(&tv1, nullptr);
    VERIFY(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

template<typename Callback>
static void iterate_directory_recursively(const String& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto file_path = String::formatted("{}/{}", directory_path, directory_iterator.next_path());
        if (Core::File::is_directory(file_path)) {
            iterate_directory_recursively(file_path, callback);
        } else {
            callback(move(file_path));
        }
    }
}

Vector<String> TestRunner::get_test_paths() const
{
    Vector<String> paths;
    iterate_directory_recursively(m_test_root, [&](const String& file_path) {
        if (!file_path.ends_with("test-common.js"))
            paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

void TestRunner::run()
{
    size_t progress_counter = 0;
    auto test_paths = get_test_paths();
    for (auto& path : test_paths) {
        ++progress_counter;
        print_file_result(run_file_test(path));
        if (m_print_progress)
            warn("\033]9;{};{};\033\\", progress_counter, test_paths.size());
    }

    if (m_print_progress)
        warn("\033]9;-1;\033\\");

    print_test_results();
}

static Result<NonnullRefPtr<JS::Program>, ParserError> parse_file(const String& file_path)
{
    auto file = Core::File::construct(file_path);
    auto result = file->open(Core::IODevice::ReadOnly);
    if (!result) {
        warnln("Failed to open the following file: \"{}\"", file_path);
        cleanup_and_exit();
    }

    auto contents = file->read_all();
    String test_file_string(reinterpret_cast<const char*>(contents.data()), contents.size());
    file->close();

    auto parser = JS::Parser(JS::Lexer(test_file_string));
    auto program = parser.parse_program();

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        return Result<NonnullRefPtr<JS::Program>, ParserError>(ParserError { error, error.source_location_hint(test_file_string) });
    }

    return Result<NonnullRefPtr<JS::Program>, ParserError>(program);
}

static Optional<JsonValue> get_test_results(JS::Interpreter& interpreter)
{
    auto result = vm->get_variable("__TestResults__", interpreter.global_object());
    auto json_string = JS::JSONObject::stringify_impl(interpreter.global_object(), result, JS::js_undefined(), JS::js_undefined());

    auto json = JsonValue::from_string(json_string);
    if (!json.has_value())
        return {};

    return json.value();
}

JSFileResult TestRunner::run_file_test(const String& test_path)
{
    currently_running_test = test_path;

    double start_time = get_time_in_ms();
    auto interpreter = JS::Interpreter::create<TestRunnerGlobalObject>(*vm);

    // FIXME: This is a hack while we're refactoring Interpreter/VM stuff.
    JS::VM::InterpreterExecutionScope scope(*interpreter);

    interpreter->heap().set_should_collect_on_every_allocation(collect_on_every_allocation);

    if (!m_test_program) {
        auto result = parse_file(String::formatted("{}/test-common.js", m_test_root));
        if (result.is_error()) {
            warnln("Unable to parse test-common.js");
            warnln("{}", result.error().error.to_string());
            warnln("{}", result.error().hint);
            cleanup_and_exit();
        }
        m_test_program = result.value();
    }

    interpreter->run(interpreter->global_object(), *m_test_program);

    auto file_program = parse_file(test_path);
    if (file_program.is_error())
        return { test_path, file_program.error() };
    interpreter->run(interpreter->global_object(), *file_program.value());

    auto test_json = get_test_results(*interpreter);
    if (!test_json.has_value()) {
        warnln("Received malformed JSON from test \"{}\"", test_path);
        cleanup_and_exit();
    }

    JSFileResult file_result { test_path.substring(m_test_root.length() + 1, test_path.length() - m_test_root.length() - 1) };

    // Collect logged messages
    auto& arr = interpreter->vm().get_variable("__UserOutput__", interpreter->global_object()).as_array();
    for (auto& entry : arr.indexed_properties()) {
        auto message = entry.value_and_attributes(&interpreter->global_object()).value;
        file_result.logged_messages.append(message.to_string_without_side_effects());
    }

    test_json.value().as_object().for_each_member([&](const String& suite_name, const JsonValue& suite_value) {
        Test::Suite suite { suite_name };

        VERIFY(suite_value.is_object());

        suite_value.as_object().for_each_member([&](const String& test_name, const JsonValue& test_value) {
            Test::Case test { test_name, Test::Result::Fail, "" };

            VERIFY(test_value.is_object());
            VERIFY(test_value.as_object().has("result"));

            auto result = test_value.as_object().get("result");
            VERIFY(result.is_string());
            auto result_string = result.as_string();
            if (result_string == "pass") {
                test.result = Test::Result::Pass;
                m_counts.tests_passed++;
            } else if (result_string == "fail") {
                test.result = Test::Result::Fail;
                m_counts.tests_failed++;
                suite.most_severe_test_result = Test::Result::Fail;
                VERIFY(test_value.as_object().has("details"));
                auto details = test_value.as_object().get("details");
                VERIFY(result.is_string());
                test.details = details.as_string();
            } else {
                test.result = Test::Result::Skip;
                if (suite.most_severe_test_result == Test::Result::Pass)
                    suite.most_severe_test_result = Test::Result::Skip;
                m_counts.tests_skipped++;
            }

            suite.tests.append(test);
        });

        if (suite.most_severe_test_result == Test::Result::Fail) {
            m_counts.suites_failed++;
            file_result.most_severe_test_result = Test::Result::Fail;
        } else {
            if (suite.most_severe_test_result == Test::Result::Skip && file_result.most_severe_test_result == Test::Result::Pass)
                file_result.most_severe_test_result = Test::Result::Skip;
            m_counts.suites_passed++;
        }

        file_result.suites.append(suite);
    });

    m_counts.files_total++;

    file_result.time_taken = get_time_in_ms() - start_time;
    m_total_elapsed_time_in_ms += file_result.time_taken;

    return file_result;
}

enum Modifier {
    BG_RED,
    BG_GREEN,
    FG_RED,
    FG_GREEN,
    FG_ORANGE,
    FG_GRAY,
    FG_BLACK,
    FG_BOLD,
    ITALIC,
    CLEAR,
};

static void print_modifiers(Vector<Modifier> modifiers)
{
    for (auto& modifier : modifiers) {
        auto code = [&] {
            switch (modifier) {
            case BG_RED:
                return "\033[48;2;255;0;102m";
            case BG_GREEN:
                return "\033[48;2;102;255;0m";
            case FG_RED:
                return "\033[38;2;255;0;102m";
            case FG_GREEN:
                return "\033[38;2;102;255;0m";
            case FG_ORANGE:
                return "\033[38;2;255;102;0m";
            case FG_GRAY:
                return "\033[38;2;135;139;148m";
            case FG_BLACK:
                return "\033[30m";
            case FG_BOLD:
                return "\033[1m";
            case ITALIC:
                return "\033[3m";
            case CLEAR:
                return "\033[0m";
            }
            VERIFY_NOT_REACHED();
        }();
        out("{}", code);
    }
}

void TestRunner::print_file_result(const JSFileResult& file_result) const
{
    if (file_result.most_severe_test_result == Test::Result::Fail || file_result.error.has_value()) {
        print_modifiers({ BG_RED, FG_BLACK, FG_BOLD });
        out(" FAIL ");
        print_modifiers({ CLEAR });
    } else {
        if (m_print_times || file_result.most_severe_test_result != Test::Result::Pass) {
            print_modifiers({ BG_GREEN, FG_BLACK, FG_BOLD });
            out(" PASS ");
            print_modifiers({ CLEAR });
        } else {
            return;
        }
    }

    out(" {}", file_result.name);

    if (m_print_times) {
        print_modifiers({ CLEAR, ITALIC, FG_GRAY });
        if (file_result.time_taken < 1000) {
            outln(" ({}ms)", static_cast<int>(file_result.time_taken));
        } else {
            outln(" ({:3}s)", file_result.time_taken / 1000.0);
        }
        print_modifiers({ CLEAR });
    } else {
        outln();
    }

    if (!file_result.logged_messages.is_empty()) {
        print_modifiers({ FG_GRAY, FG_BOLD });
#ifdef __serenity__
        outln("     ℹ Console output:");
#else
        // This emoji has a second invisible byte after it. The one above does not
        outln("    ℹ️  Console output:");
#endif
        print_modifiers({ CLEAR, FG_GRAY });
        for (auto& message : file_result.logged_messages)
            outln("         {}", message);
    }

    if (file_result.error.has_value()) {
        auto test_error = file_result.error.value();

        print_modifiers({ FG_RED });
#ifdef __serenity__
        outln("     ❌ The file failed to parse");
#else
        // No invisible byte here, but the spacing still needs to be altered on the host
        outln("    ❌ The file failed to parse");
#endif
        outln();
        print_modifiers({ FG_GRAY });
        for (auto& message : test_error.hint.split('\n', true)) {
            outln("         {}", message);
        }
        print_modifiers({ FG_RED });
        outln("         {}", test_error.error.to_string());
        outln();
        return;
    }

    if (file_result.most_severe_test_result != Test::Result::Pass) {
        for (auto& suite : file_result.suites) {
            if (suite.most_severe_test_result == Test::Result::Pass)
                continue;

            bool failed = suite.most_severe_test_result == Test::Result::Fail;

            print_modifiers({ FG_GRAY, FG_BOLD });

            if (failed) {
#ifdef __serenity__
                out("     ❌ Suite:  ");
#else
                // No invisible byte here, but the spacing still needs to be altered on the host
                out("    ❌ Suite:  ");
#endif
            } else {
#ifdef __serenity__
                out("     ⚠ Suite:  ");
#else
                // This emoji has a second invisible byte after it. The one above does not
                out("    ⚠️  Suite:  ");
#endif
            }

            print_modifiers({ CLEAR, FG_GRAY });

            if (suite.name == TOP_LEVEL_TEST_NAME) {
                outln("<top-level>");
            } else {
                outln("{}", suite.name);
            }
            print_modifiers({ CLEAR });

            for (auto& test : suite.tests) {
                if (test.result == Test::Result::Pass)
                    continue;

                print_modifiers({ FG_GRAY, FG_BOLD });
                out("         Test:   ");
                if (test.result == Test::Result::Fail) {
                    print_modifiers({ CLEAR, FG_RED });
                    outln("{} (failed):", test.name);
                    outln("                 {}", test.details);
                } else {
                    print_modifiers({ CLEAR, FG_ORANGE });
                    outln("{} (skipped)", test.name);
                }
                print_modifiers({ CLEAR });
            }
        }
    }
}

void TestRunner::print_test_results() const
{
    out("\nTest Suites: ");
    if (m_counts.suites_failed) {
        print_modifiers({ FG_RED });
        out("{} failed, ", m_counts.suites_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.suites_passed) {
        print_modifiers({ FG_GREEN });
        out("{} passed, ", m_counts.suites_passed);
        print_modifiers({ CLEAR });
    }
    outln("{} total", m_counts.suites_failed + m_counts.suites_passed);

    out("Tests:       ");
    if (m_counts.tests_failed) {
        print_modifiers({ FG_RED });
        out("{} failed, ", m_counts.tests_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_skipped) {
        print_modifiers({ FG_ORANGE });
        out("{} skipped, ", m_counts.tests_skipped);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_passed) {
        print_modifiers({ FG_GREEN });
        out("{} passed, ", m_counts.tests_passed);
        print_modifiers({ CLEAR });
    }
    outln("{} total", m_counts.tests_failed + m_counts.tests_skipped + m_counts.tests_passed);

    outln("Files:       {} total", m_counts.files_total);

    out("Time:        ");
    if (m_total_elapsed_time_in_ms < 1000.0) {
        outln("{}ms", static_cast<int>(m_total_elapsed_time_in_ms));
    } else {
        outln("{:>.3}s", m_total_elapsed_time_in_ms / 1000.0);
    }
    outln();
}

class Test262ParserTestRunner final : public TestRunner {
public:
    using TestRunner::TestRunner;

private:
    virtual Vector<String> get_test_paths() const override;
    virtual JSFileResult run_file_test(const String& test_path) override;
};

Vector<String> Test262ParserTestRunner::get_test_paths() const
{
    Vector<String> paths;
    iterate_directory_recursively(m_test_root, [&](const String& file_path) {
        auto dirname = LexicalPath(file_path).dirname();
        if (dirname.ends_with("early") || dirname.ends_with("fail") || dirname.ends_with("pass") || dirname.ends_with("pass-explicit"))
            paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

JSFileResult Test262ParserTestRunner::run_file_test(const String& test_path)
{
    currently_running_test = test_path;

    auto dirname = LexicalPath(test_path).dirname();
    bool expecting_file_to_parse;
    if (dirname.ends_with("early") || dirname.ends_with("fail")) {
        expecting_file_to_parse = false;
    } else if (dirname.ends_with("pass") || dirname.ends_with("pass-explicit")) {
        expecting_file_to_parse = true;
    } else {
        VERIFY_NOT_REACHED();
    }

    auto start_time = get_time_in_ms();
    String details = "";
    Test::Result test_result;
    if (test_path.ends_with(".module.js")) {
        test_result = Test::Result::Skip;
        m_counts.tests_skipped++;
        m_counts.suites_passed++;
    } else {
        auto parse_result = parse_file(test_path);
        if (expecting_file_to_parse) {
            if (!parse_result.is_error()) {
                test_result = Test::Result::Pass;
            } else {
                test_result = Test::Result::Fail;
                details = parse_result.error().error.to_string();
            }
        } else {
            if (parse_result.is_error()) {
                test_result = Test::Result::Pass;
            } else {
                test_result = Test::Result::Fail;
                details = "File was expected to produce a parser error but didn't";
            }
        }
    }

    // test262-parser-tests doesn't have "suites" and "tests" in the usual sense, it just has files
    // and an expectation whether they should parse or not. We add one suite with one test nonetheless:
    //
    // - This makes interpreting skipped test easier as their file is shown as "PASS"
    // - That way we can show additional information such as "file parsed but shouldn't have" or
    //   parser errors for files that should parse respectively

    Test::Case test { expecting_file_to_parse ? "file should parse" : "file should not parse", test_result, details };
    Test::Suite suite { "Parse file", test_result, { test } };
    JSFileResult file_result {
        test_path.substring(m_test_root.length() + 1, test_path.length() - m_test_root.length() - 1),
        {},
        get_time_in_ms() - start_time,
        test_result,
        { suite }
    };

    if (test_result == Test::Result::Fail) {
        m_counts.tests_failed++;
        m_counts.suites_failed++;
    } else {
        m_counts.tests_passed++;
        m_counts.suites_passed++;
    }
    m_counts.files_total++;
    m_total_elapsed_time_in_ms += file_result.time_taken;

    return file_result;
}

int main(int argc, char** argv)
{
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
        int len = snprintf(buffer, sizeof(buffer), "Pass: %d, Fail: %d, Skip: %d\nCurrent test: %s\n", counts.tests_passed, counts.tests_failed, counts.tests_skipped, currently_running_test.characters());
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
    bool test262_parser_tests = false;
    const char* specified_test_root = nullptr;

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
    args_parser.add_option(collect_on_every_allocation, "Collect garbage after every allocation", "collect-often", 'g');
    args_parser.add_option(test262_parser_tests, "Run test262 parser tests", "test262-parser-tests", 0);
    args_parser.add_positional_argument(specified_test_root, "Tests root directory", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (test262_parser_tests) {
        if (collect_on_every_allocation) {
            warnln("--collect-often and --test262-parser-tests options must not be used together");
            return 1;
        }
        if (!specified_test_root) {
            warnln("Test root is required with --test262-parser-tests");
            return 1;
        }
    }

    if (getenv("DISABLE_DBG_OUTPUT")) {
        AK::set_debug_enabled(false);
    }

    String test_root;

    if (specified_test_root) {
        test_root = String { specified_test_root };
    } else {
#ifdef __serenity__
        test_root = "/home/anon/js-tests";
#else
        char* serenity_root = getenv("SERENITY_ROOT");
        if (!serenity_root) {
            warnln("No test root given, test-js requires the SERENITY_ROOT environment variable to be set");
            return 1;
        }
        test_root = String::formatted("{}/Userland/Libraries/LibJS/Tests", serenity_root);
#endif
    }
    if (!Core::File::is_directory(test_root)) {
        warnln("Test root is not a directory: {}", test_root);
        return 1;
    }

    vm = JS::VM::create();

    if (test262_parser_tests)
        Test262ParserTestRunner(test_root, print_times, print_progress).run();
    else
        TestRunner(test_root, print_times, print_progress).run();

    vm = nullptr;

    return TestRunner::the()->counts().tests_failed > 0 ? 1 : 0;
}
