/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LogStream.h>
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
#include <signal.h>
#include <stdlib.h>
#include <sys/time.h>

#define TOP_LEVEL_TEST_NAME "__$$TOP_LEVEL$$__"

RefPtr<JS::VM> vm;

static bool collect_on_every_allocation = false;
static String currently_running_test;

enum class TestResult {
    Pass,
    Fail,
    Skip,
};

struct JSTest {
    String name;
    TestResult result;
    String details;
};

struct JSSuite {
    String name;
    // A failed test takes precedence over a skipped test, which both have
    // precedence over a passed test
    TestResult most_severe_test_result { TestResult::Pass };
    Vector<JSTest> tests {};
};

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
    TestResult most_severe_test_result { TestResult::Pass };
    Vector<JSSuite> suites {};
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

class TestRunnerGlobalObject : public JS::GlobalObject {
public:
    TestRunnerGlobalObject();
    virtual ~TestRunnerGlobalObject() override;

    virtual void initialize() override;

private:
    virtual const char* class_name() const override { return "TestRunnerGlobalObject"; }

    JS_DECLARE_NATIVE_FUNCTION(is_strict_mode);
};

class TestRunner {
public:
    static TestRunner* the()
    {
        return s_the;
    }

    TestRunner(String test_root, bool print_times)
        : m_test_root(move(test_root))
        , m_print_times(print_times)
    {
        ASSERT(!s_the);
        s_the = this;
    }

    void run();

    const JSTestRunnerCounts& counts() const { return m_counts; }

private:
    static TestRunner* s_the;

    JSFileResult run_file_test(const String& test_path);
    void print_file_result(const JSFileResult& file_result) const;
    void print_test_results() const;

    String m_test_root;
    bool m_print_times;

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

void TestRunnerGlobalObject::initialize()
{
    JS::GlobalObject::initialize();
    define_property("global", this, JS::Attribute::Enumerable);
    define_native_function("isStrictMode", is_strict_mode);
}

JS_DEFINE_NATIVE_FUNCTION(TestRunnerGlobalObject::is_strict_mode)
{
    return JS::Value(vm.in_strict_mode());
}

static void cleanup_and_exit()
{
    // Clear the taskbar progress.
#ifdef __serenity__
    fprintf(stderr, "\033]9;-1;\033\\");
#endif
    exit(1);
}

static void handle_sigabrt(int)
{
    dbg() << "test-js: SIGABRT received, cleaning up.";
    cleanup_and_exit();
}

static double get_time_in_ms()
{
    struct timeval tv1;
    auto return_code = gettimeofday(&tv1, nullptr);
    ASSERT(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

template<typename Callback>
static void iterate_directory_recursively(const String& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto file_path = String::format("%s/%s", directory_path.characters(), directory_iterator.next_path().characters());
        if (Core::File::is_directory(file_path)) {
            iterate_directory_recursively(file_path, callback);
        } else {
            callback(move(file_path));
        }
    }
}

static Vector<String> get_test_paths(const String& test_root)
{
    Vector<String> paths;

    iterate_directory_recursively(test_root, [&](const String& file_path) {
        if (!file_path.ends_with("test-common.js"))
            paths.append(file_path);
    });

    quick_sort(paths);

    return paths;
}

void TestRunner::run()
{
    size_t progress_counter = 0;
    auto test_paths = get_test_paths(m_test_root);
    for (auto& path : test_paths) {
        ++progress_counter;
        print_file_result(run_file_test(path));
#ifdef __serenity__
        fprintf(stderr, "\033]9;%zu;%zu;\033\\", progress_counter, test_paths.size());
#endif
    }

#ifdef __serenity__
    fprintf(stderr, "\033]9;-1;\033\\");
#endif

    print_test_results();
}

static Result<NonnullRefPtr<JS::Program>, ParserError> parse_file(const String& file_path)
{
    auto file = Core::File::construct(file_path);
    auto result = file->open(Core::IODevice::ReadOnly);
    if (!result) {
        printf("Failed to open the following file: \"%s\"\n", file_path.characters());
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
        auto result = parse_file(String::format("%s/test-common.js", m_test_root.characters()));
        if (result.is_error()) {
            printf("Unable to parse test-common.js\n");
            printf("%s\n", result.error().error.to_string().characters());
            printf("%s\n", result.error().hint.characters());
            cleanup_and_exit();
            ;
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
        printf("Received malformed JSON from test \"%s\"\n", test_path.characters());
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
        JSSuite suite { suite_name };

        ASSERT(suite_value.is_object());

        suite_value.as_object().for_each_member([&](const String& test_name, const JsonValue& test_value) {
            JSTest test { test_name, TestResult::Fail, "" };

            ASSERT(test_value.is_object());
            ASSERT(test_value.as_object().has("result"));

            auto result = test_value.as_object().get("result");
            ASSERT(result.is_string());
            auto result_string = result.as_string();
            if (result_string == "pass") {
                test.result = TestResult::Pass;
                m_counts.tests_passed++;
            } else if (result_string == "fail") {
                test.result = TestResult::Fail;
                m_counts.tests_failed++;
                suite.most_severe_test_result = TestResult::Fail;
                ASSERT(test_value.as_object().has("details"));
                auto details = test_value.as_object().get("details");
                ASSERT(result.is_string());
                test.details = details.as_string();
            } else {
                test.result = TestResult::Skip;
                if (suite.most_severe_test_result == TestResult::Pass)
                    suite.most_severe_test_result = TestResult::Skip;
                m_counts.tests_skipped++;
            }

            suite.tests.append(test);
        });

        if (suite.most_severe_test_result == TestResult::Fail) {
            m_counts.suites_failed++;
            file_result.most_severe_test_result = TestResult::Fail;
        } else {
            if (suite.most_severe_test_result == TestResult::Skip && file_result.most_severe_test_result == TestResult::Pass)
                file_result.most_severe_test_result = TestResult::Skip;
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
        auto code = [&]() -> String {
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
            ASSERT_NOT_REACHED();
        };
        printf("%s", code().characters());
    }
}

void TestRunner::print_file_result(const JSFileResult& file_result) const
{
    if (file_result.most_severe_test_result == TestResult::Fail || file_result.error.has_value()) {
        print_modifiers({ BG_RED, FG_BLACK, FG_BOLD });
        printf(" FAIL ");
        print_modifiers({ CLEAR });
    } else {
        if (m_print_times || file_result.most_severe_test_result != TestResult::Pass) {
            print_modifiers({ BG_GREEN, FG_BLACK, FG_BOLD });
            printf(" PASS ");
            print_modifiers({ CLEAR });
        } else {
            return;
        }
    }

    printf(" %s", file_result.name.characters());

    if (m_print_times) {
        print_modifiers({ CLEAR, ITALIC, FG_GRAY });
        if (file_result.time_taken < 1000) {
            printf(" (%dms)\n", static_cast<int>(file_result.time_taken));
        } else {
            printf(" (%.3fs)\n", file_result.time_taken / 1000.0);
        }
        print_modifiers({ CLEAR });
    } else {
        printf("\n");
    }

    if (!file_result.logged_messages.is_empty()) {
        print_modifiers({ FG_GRAY, FG_BOLD });
#ifdef __serenity__
        printf("     ℹ Console output:\n");
#else
        // This emoji has a second invisible byte after it. The one above does not
        printf("    ℹ️  Console output:\n");
#endif
        print_modifiers({ CLEAR, FG_GRAY });
        for (auto& message : file_result.logged_messages)
            printf("         %s\n", message.characters());
    }

    if (file_result.error.has_value()) {
        auto test_error = file_result.error.value();

        print_modifiers({ FG_RED });
#ifdef __serenity__
        printf("     ❌ The file failed to parse\n\n");
#else
        // No invisible byte here, but the spacing still needs to be altered on the host
        printf("    ❌ The file failed to parse\n\n");
#endif
        print_modifiers({ FG_GRAY });
        for (auto& message : test_error.hint.split('\n', true)) {
            printf("         %s\n", message.characters());
        }
        print_modifiers({ FG_RED });
        printf("         %s\n\n", test_error.error.to_string().characters());

        return;
    }

    if (file_result.most_severe_test_result != TestResult::Pass) {
        for (auto& suite : file_result.suites) {
            if (suite.most_severe_test_result == TestResult::Pass)
                continue;

            bool failed = suite.most_severe_test_result == TestResult::Fail;

            print_modifiers({ FG_GRAY, FG_BOLD });

            if (failed) {
#ifdef __serenity__
                printf("     ❌ Suite:  ");
#else
                // No invisible byte here, but the spacing still needs to be altered on the host
                printf("    ❌ Suite:  ");
#endif
            } else {
#ifdef __serenity__
                printf("     ⚠ Suite:  ");
#else
                // This emoji has a second invisible byte after it. The one above does not
                printf("    ⚠️  Suite:  ");
#endif
            }

            print_modifiers({ CLEAR, FG_GRAY });

            if (suite.name == TOP_LEVEL_TEST_NAME) {
                printf("<top-level>\n");
            } else {
                printf("%s\n", suite.name.characters());
            }
            print_modifiers({ CLEAR });

            for (auto& test : suite.tests) {
                if (test.result == TestResult::Pass)
                    continue;

                print_modifiers({ FG_GRAY, FG_BOLD });
                printf("         Test:   ");
                if (test.result == TestResult::Fail) {
                    print_modifiers({ CLEAR, FG_RED });
                    printf("%s (failed):\n", test.name.characters());
                    printf("                 %s\n", test.details.characters());
                } else {
                    print_modifiers({ CLEAR, FG_ORANGE });
                    printf("%s (skipped)\n", test.name.characters());
                }
                print_modifiers({ CLEAR });
            }
        }
    }
}

void TestRunner::print_test_results() const
{
    printf("\nTest Suites: ");
    if (m_counts.suites_failed) {
        print_modifiers({ FG_RED });
        printf("%d failed, ", m_counts.suites_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.suites_passed) {
        print_modifiers({ FG_GREEN });
        printf("%d passed, ", m_counts.suites_passed);
        print_modifiers({ CLEAR });
    }
    printf("%d total\n", m_counts.suites_failed + m_counts.suites_passed);

    printf("Tests:       ");
    if (m_counts.tests_failed) {
        print_modifiers({ FG_RED });
        printf("%d failed, ", m_counts.tests_failed);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_skipped) {
        print_modifiers({ FG_ORANGE });
        printf("%d skipped, ", m_counts.tests_skipped);
        print_modifiers({ CLEAR });
    }
    if (m_counts.tests_passed) {
        print_modifiers({ FG_GREEN });
        printf("%d passed, ", m_counts.tests_passed);
        print_modifiers({ CLEAR });
    }
    printf("%d total\n", m_counts.tests_failed + m_counts.tests_passed);

    printf("Files:       %d total\n", m_counts.files_total);

    printf("Time:        ");
    if (m_total_elapsed_time_in_ms < 1000.0) {
        printf("%dms\n\n", static_cast<int>(m_total_elapsed_time_in_ms));
    } else {
        printf("%-.3fs\n\n", m_total_elapsed_time_in_ms / 1000.0);
    }
}

int main(int argc, char** argv)
{
    bool print_times = false;
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

    Core::ArgsParser args_parser;
    args_parser.add_option(print_times, "Show duration of each test", "show-time", 't');
    args_parser.add_option(collect_on_every_allocation, "Collect garbage after every allocation", "collect-often", 'g');
    args_parser.parse(argc, argv);

    if (getenv("DISABLE_DBG_OUTPUT")) {
        DebugLogStream::set_enabled(false);
    }

    vm = JS::VM::create();

#ifdef __serenity__
    TestRunner("/home/anon/js-tests", print_times).run();
#else
    char* serenity_root = getenv("SERENITY_ROOT");
    if (!serenity_root) {
        printf("test-js requires the SERENITY_ROOT environment variable to be set");
        return 1;
    }
    TestRunner(String::format("%s/Libraries/LibJS/Tests", serenity_root), print_times).run();
#endif

    vm = nullptr;

    return 0;
}
