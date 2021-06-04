/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/Result.h>
#include <AK/Tuple.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibTest/Results.h>
#include <fcntl.h>
#include <sys/time.h>
#include <unistd.h>

#define STRCAT(x, y) __STRCAT(x, y)
#define STRSTRCAT(x, y) __STRSTRCAT(x, y)
#define __STRCAT(x, y) x #y
#define __STRSTRCAT(x, y) x y

// Note: This is a little weird, so here's an explanation:
//       If the vararg isn't given, the tuple initializer will simply expand to `fn, ::Test::JS::__testjs_last<1>()`
//       and if it _is_ given (say as `A`), the tuple initializer will expand to `fn, ::Test::JS::__testjs_last<1, A>()`, which will end up being evaluated as `A`
//       and if multiple args are given, the static_assert will be sad.
#define __TESTJS_REGISTER_GLOBAL_FUNCTION(name, fn, ...)                                                                                        \
    struct __TestJS_register_##fn {                                                                                                             \
        static_assert(                                                                                                                          \
            ::Test::JS::__testjs_count(__VA_ARGS__) <= 1,                                                                                       \
            STRCAT(STRSTRCAT(STRCAT("Expected at most three arguments to TESTJS_GLOBAL_FUNCTION at line", __LINE__), ", in file "), __FILE__)); \
        __TestJS_register_##fn() noexcept                                                                                                       \
        {                                                                                                                                       \
            ::Test::JS::s_exposed_global_functions.set(                                                                                         \
                name,                                                                                                                           \
                { fn, ::Test::JS::__testjs_last<1, ##__VA_ARGS__>() });                                                                         \
        }                                                                                                                                       \
    } __testjs_register_##fn {};

#define TESTJS_GLOBAL_FUNCTION(function, exposed_name, ...)                    \
    JS_DECLARE_NATIVE_FUNCTION(function);                                      \
    __TESTJS_REGISTER_GLOBAL_FUNCTION(#exposed_name, function, ##__VA_ARGS__); \
    JS_DEFINE_NATIVE_FUNCTION(function)

#define TESTJS_MAIN_HOOK()                  \
    struct __TestJS_main_hook {             \
        __TestJS_main_hook()                \
        {                                   \
            ::Test::JS::g_main_hook = hook; \
        }                                   \
        static void hook();                 \
    } __testjs_common_register_##name {};   \
    void __TestJS_main_hook::hook()

#define TESTJS_PROGRAM_FLAG(flag, help_string, long_name, short_name)                      \
    bool flag { false };                                                                   \
    struct __TestJS_flag_hook_##flag {                                                     \
        __TestJS_flag_hook_##flag()                                                        \
        {                                                                                  \
            ::Test::JS::g_extra_args.set(&(flag), { help_string, long_name, short_name }); \
        };                                                                                 \
    } __testjs_flag_hook_##flag;

#define TEST_ROOT(path) \
    String Test::JS::g_test_root_fragment = path

#define TESTJS_RUN_FILE_FUNCTION(...)                                                       \
    struct __TestJS_run_file {                                                              \
        __TestJS_run_file()                                                                 \
        {                                                                                   \
            ::Test::JS::g_run_file = hook;                                                  \
        }                                                                                   \
        static ::Test::JS::IntermediateRunFileResult hook(const String&, JS::Interpreter&); \
    } __testjs_common_run_file {};                                                          \
    ::Test::JS::IntermediateRunFileResult __TestJS_run_file::hook(__VA_ARGS__)

namespace Test::JS {

namespace JS = ::JS;

template<typename... Args>
static consteval size_t __testjs_count(Args...) { return sizeof...(Args); }

template<auto... Values>
static consteval size_t __testjs_last() { return (AK::Detail::IntegralConstant<size_t, Values> {}, ...).value; }

static constexpr auto TOP_LEVEL_TEST_NAME = "__$$TOP_LEVEL$$__";
extern RefPtr<JS::VM> g_vm;
extern bool g_collect_on_every_allocation;
extern String g_currently_running_test;
extern String g_test_glob;
struct FunctionWithLength {
    JS::Value (*function)(JS::VM&, JS::GlobalObject&);
    size_t length { 0 };
};
extern HashMap<String, FunctionWithLength> s_exposed_global_functions;
extern String g_test_root_fragment;
extern String g_test_root;
extern int g_test_argc;
extern char** g_test_argv;
extern Function<void()> g_main_hook;
extern HashMap<bool*, Tuple<String, String, char>> g_extra_args;

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

enum class RunFileHookResult {
    RunAsNormal,
    SkipFile,
};

using IntermediateRunFileResult = AK::Result<JSFileResult, RunFileHookResult>;
extern IntermediateRunFileResult (*g_run_file)(const String&, JS::Interpreter&);

class TestRunner {
public:
    static TestRunner* the()
    {
        return s_the;
    }

    TestRunner(String test_root, String common_path, bool print_times, bool print_progress, bool print_json)
        : m_common_path(move(common_path))
        , m_test_root(move(test_root))
        , m_print_times(print_times)
        , m_print_progress(print_progress)
        , m_print_json(print_json)
    {
        VERIFY(!s_the);
        s_the = this;
        g_test_root = m_test_root;
    }

    virtual ~TestRunner() = default;

    void run();

    const Test::Counts& counts() const { return m_counts; }

    bool is_printing_progress() const { return m_print_progress; }

protected:
    static TestRunner* s_the;

    virtual Vector<String> get_test_paths() const;
    virtual JSFileResult run_file_test(const String& test_path);
    void print_file_result(const JSFileResult& file_result) const;
    void print_test_results() const;
    void print_test_results_as_json() const;

    String m_common_path;
    String m_test_root;
    bool m_print_times;
    bool m_print_progress;
    bool m_print_json;

    double m_total_elapsed_time_in_ms { 0 };
    Test::Counts m_counts;

    RefPtr<JS::Program> m_test_program;
};

class TestRunnerGlobalObject final : public JS::GlobalObject {
    JS_OBJECT(TestRunnerGlobalObject, JS::GlobalObject);

public:
    TestRunnerGlobalObject() = default;
    virtual ~TestRunnerGlobalObject() override = default;

    virtual void initialize_global_object() override;
};

inline void TestRunnerGlobalObject::initialize_global_object()
{
    Base::initialize_global_object();
    define_property("global", this, JS::Attribute::Enumerable);
    for (auto& entry : s_exposed_global_functions) {
        define_native_function(
            entry.key, [fn = entry.value.function](auto& vm, auto& global_object) {
                return fn(vm, global_object);
            },
            entry.value.length);
    }
}

inline void cleanup()
{
    // Clear the taskbar progress.
    if (TestRunner::the() && TestRunner::the()->is_printing_progress())
        warn("\033]9;-1;\033\\");
}

inline void cleanup_and_exit()
{
    cleanup();
    exit(1);
}

inline double get_time_in_ms()
{
    struct timeval tv1;
    auto return_code = gettimeofday(&tv1, nullptr);
    VERIFY(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

template<typename Callback>
inline void iterate_directory_recursively(const String& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto name = directory_iterator.next_path();
        struct stat st = {};
        if (fstatat(directory_iterator.fd(), name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0)
            continue;
        bool is_directory = S_ISDIR(st.st_mode);
        auto full_path = String::formatted("{}/{}", directory_path, name);
        if (is_directory && name != "/Fixtures"sv) {
            iterate_directory_recursively(full_path, callback);
        } else if (!is_directory) {
            callback(full_path);
        }
    }
}

inline Vector<String> TestRunner::get_test_paths() const
{
    Vector<String> paths;
    iterate_directory_recursively(m_test_root, [&](const String& file_path) {
        if (!file_path.ends_with(".js"))
            return;
        if (!file_path.ends_with("test-common.js"))
            paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

inline void TestRunner::run()
{
    size_t progress_counter = 0;
    auto test_paths = get_test_paths();
    for (auto& path : test_paths) {
        if (!path.matches(g_test_glob))
            continue;
        ++progress_counter;
        auto file_result = run_file_test(path);
        if (!m_print_json)
            print_file_result(file_result);
        if (m_print_progress)
            warn("\033]9;{};{};\033\\", progress_counter, test_paths.size());
    }

    if (m_print_progress)
        warn("\033]9;-1;\033\\");

    if (!m_print_json)
        print_test_results();
    else
        print_test_results_as_json();
}

inline AK::Result<NonnullRefPtr<JS::Program>, ParserError> parse_file(const String& file_path)
{
    auto file = Core::File::construct(file_path);
    auto result = file->open(Core::OpenMode::ReadOnly);
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
        return AK::Result<NonnullRefPtr<JS::Program>, ParserError>(ParserError { error, error.source_location_hint(test_file_string) });
    }

    return AK::Result<NonnullRefPtr<JS::Program>, ParserError>(program);
}

inline Optional<JsonValue> get_test_results(JS::Interpreter& interpreter)
{
    auto result = g_vm->get_variable("__TestResults__", interpreter.global_object());
    auto json_string = JS::JSONObject::stringify_impl(interpreter.global_object(), result, JS::js_undefined(), JS::js_undefined());

    auto json = JsonValue::from_string(json_string);
    if (!json.has_value())
        return {};

    return json.value();
}

inline JSFileResult TestRunner::run_file_test(const String& test_path)
{
    g_currently_running_test = test_path;

    double start_time = get_time_in_ms();
    auto interpreter = JS::Interpreter::create<TestRunnerGlobalObject>(*g_vm);

    // FIXME: This is a hack while we're refactoring Interpreter/VM stuff.
    JS::VM::InterpreterExecutionScope scope(*interpreter);

    interpreter->heap().set_should_collect_on_every_allocation(g_collect_on_every_allocation);

    if (g_run_file) {
        auto result = g_run_file(test_path, *interpreter);
        if (result.is_error() && result.error() == RunFileHookResult::SkipFile) {
            return {
                test_path,
                {},
                0,
                Test::Result::Skip,
                {},
                {}
            };
        }
        if (!result.is_error()) {
            auto value = result.release_value();
            for (auto& suite : value.suites) {
                if (suite.most_severe_test_result == Result::Pass)
                    m_counts.suites_passed++;
                else if (suite.most_severe_test_result == Result::Fail)
                    m_counts.suites_failed++;
                for (auto& test : suite.tests) {
                    if (test.result == Result::Pass)
                        m_counts.tests_passed++;
                    else if (test.result == Result::Fail)
                        m_counts.tests_failed++;
                    else if (test.result == Result::Skip)
                        m_counts.tests_skipped++;
                }
            }
            ++m_counts.files_total;
            m_total_elapsed_time_in_ms += value.time_taken;

            return value;
        }
    }

    if (!m_test_program) {
        auto result = parse_file(m_common_path);
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

    if (g_vm->exception())
        g_vm->clear_exception();

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

inline void print_modifiers(Vector<Modifier> modifiers)
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

inline void TestRunner::print_file_result(const JSFileResult& file_result) const
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

inline void TestRunner::print_test_results() const
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

inline void TestRunner::print_test_results_as_json() const
{
    JsonObject suites;
    suites.set("failed", m_counts.suites_failed);
    suites.set("passed", m_counts.suites_passed);
    suites.set("total", m_counts.suites_failed + m_counts.suites_passed);

    JsonObject tests;
    tests.set("failed", m_counts.tests_failed);
    tests.set("passed", m_counts.tests_passed);
    tests.set("skipped", m_counts.tests_skipped);
    tests.set("total", m_counts.tests_failed + m_counts.tests_passed + m_counts.tests_skipped);

    JsonObject results;
    results.set("suites", suites);
    results.set("tests", tests);

    JsonObject root;
    root.set("results", results);
    root.set("files_total", m_counts.files_total);
    root.set("duration", m_total_elapsed_time_in_ms / 1000.0);

    outln("{}", root.to_string());
}

}
