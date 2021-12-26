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
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/WeakMap.h>
#include <LibJS/Runtime/WeakSet.h>
#include <LibTest/Results.h>
#include <LibTest/TestRunner.h>
#include <fcntl.h>
#include <sys/time.h>
#include <unistd.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

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
extern bool g_run_bytecode;
extern bool g_dump_bytecode;
extern String g_currently_running_test;
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

class TestRunner : public ::Test::TestRunner {
public:
    TestRunner(String test_root, String common_path, bool print_times, bool print_progress, bool print_json)
        : ::Test::TestRunner(move(test_root), print_times, print_progress, print_json)
        , m_common_path(move(common_path))
    {
        g_test_root = m_test_root;
    }

    virtual ~TestRunner() = default;

protected:
    virtual void do_run_single_test(const String& test_path, size_t, size_t) override;
    virtual Vector<String> get_test_paths() const override;
    virtual JSFileResult run_file_test(const String& test_path);
    void print_file_result(const JSFileResult& file_result) const;

    String m_common_path;
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
    define_direct_property("global", this, JS::Attribute::Enumerable);
    for (auto& entry : s_exposed_global_functions) {
        define_native_function(
            entry.key, [fn = entry.value.function](auto& vm, auto& global_object) {
                return fn(vm, global_object);
            },
            entry.value.length, JS::default_attributes);
    }
}

inline AK::Result<NonnullRefPtr<JS::Program>, ParserError> parse_file(const String& file_path, JS::Program::Type program_type = JS::Program::Type::Script)
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

    auto parser = JS::Parser(JS::Lexer(test_file_string), program_type);
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

inline void TestRunner::do_run_single_test(const String& test_path, size_t, size_t)
{
    auto file_result = run_file_test(test_path);
    if (!m_print_json)
        print_file_result(file_result);
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

inline JSFileResult TestRunner::run_file_test(const String& test_path)
{
    g_currently_running_test = test_path;

#ifdef __serenity__
    auto string_id = perf_register_string(test_path.characters(), test_path.length());
    perf_event(PERF_EVENT_SIGNPOST, string_id, 0);
#endif

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

    if (g_run_bytecode) {
        auto unit = JS::Bytecode::Generator::generate(*m_test_program);
        if (g_dump_bytecode) {
            for (auto& block : unit.basic_blocks)
                block.dump(unit);
            if (!unit.string_table->is_empty()) {
                outln();
                unit.string_table->dump();
            }
        }

        JS::Bytecode::Interpreter bytecode_interpreter(interpreter->global_object());
        bytecode_interpreter.run(unit);
    } else {
        interpreter->run(interpreter->global_object(), *m_test_program);
    }

    VERIFY(!g_vm->exception());

    auto file_program = parse_file(test_path);
    if (file_program.is_error())
        return { test_path, file_program.error() };
    if (g_run_bytecode) {
        auto unit = JS::Bytecode::Generator::generate(*file_program.value());
        if (g_dump_bytecode) {
            for (auto& block : unit.basic_blocks)
                block.dump(unit);
            if (!unit.string_table->is_empty()) {
                outln();
                unit.string_table->dump();
            }
        }

        JS::Bytecode::Interpreter bytecode_interpreter(interpreter->global_object());
        bytecode_interpreter.run(unit);
    } else {
        interpreter->run(interpreter->global_object(), *file_program.value());
    }

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
        auto message = arr.get(entry.index());
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

}
