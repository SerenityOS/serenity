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
#include <LibJS/Script.h>
#include <LibJS/SourceTextModule.h>
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

#define TESTJS_RUN_FILE_FUNCTION(...)                                                                              \
    struct __TestJS_run_file {                                                                                     \
        __TestJS_run_file()                                                                                        \
        {                                                                                                          \
            ::Test::JS::g_run_file = hook;                                                                         \
        }                                                                                                          \
        static ::Test::JS::IntermediateRunFileResult hook(const String&, JS::Interpreter&, JS::ExecutionContext&); \
    } __testjs_common_run_file {};                                                                                 \
    ::Test::JS::IntermediateRunFileResult __TestJS_run_file::hook(__VA_ARGS__)

#define TESTJS_CREATE_INTERPRETER_HOOK(...)               \
    struct __TestJS_create_interpreter_hook {             \
        __TestJS_create_interpreter_hook()                \
        {                                                 \
            ::Test::JS::g_create_interpreter_hook = hook; \
        }                                                 \
        static NonnullOwnPtr<JS::Interpreter> hook();     \
    } __testjs_create_interpreter_hook {};                \
    NonnullOwnPtr<JS::Interpreter> __TestJS_create_interpreter_hook::hook(__VA_ARGS__)

namespace Test::JS {

namespace JS = ::JS;

template<typename... Args>
static consteval size_t __testjs_count(Args...) { return sizeof...(Args); }

template<auto... Values>
static consteval size_t __testjs_last()
{
    Array values { Values... };
    return values[values.size() - 1U];
}

static constexpr auto TOP_LEVEL_TEST_NAME = "__$$TOP_LEVEL$$__";
extern RefPtr<JS::VM> g_vm;
extern bool g_collect_on_every_allocation;
extern bool g_run_bytecode;
extern String g_currently_running_test;
struct FunctionWithLength {
    JS::ThrowCompletionOr<JS::Value> (*function)(JS::VM&, JS::GlobalObject&);
    size_t length { 0 };
};
extern HashMap<String, FunctionWithLength> s_exposed_global_functions;
extern String g_test_root_fragment;
extern String g_test_root;
extern int g_test_argc;
extern char** g_test_argv;
extern Function<void()> g_main_hook;
extern Function<NonnullOwnPtr<JS::Interpreter>()> g_create_interpreter_hook;
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
extern IntermediateRunFileResult (*g_run_file)(const String&, JS::Interpreter&, JS::ExecutionContext&);

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

inline ByteBuffer load_entire_file(StringView path)
{
    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open the following file: \"{}\"", path);
        cleanup_and_exit();
    }

    auto file = file_or_error.release_value();
    return file->read_all();
}

inline AK::Result<NonnullRefPtr<JS::Script>, ParserError> parse_script(StringView path, JS::Realm& realm)
{
    auto contents = load_entire_file(path);
    auto script_or_errors = JS::Script::parse(contents, realm, path);

    if (script_or_errors.is_error()) {
        auto errors = script_or_errors.release_error();
        return ParserError { errors[0], errors[0].source_location_hint(contents) };
    }

    return script_or_errors.release_value();
}

inline AK::Result<NonnullRefPtr<JS::SourceTextModule>, ParserError> parse_module(StringView path, JS::Realm& realm)
{
    auto contents = load_entire_file(path);
    auto script_or_errors = JS::SourceTextModule::parse(contents, realm, path);

    if (script_or_errors.is_error()) {
        auto errors = script_or_errors.release_error();
        return ParserError { errors[0], errors[0].source_location_hint(contents) };
    }

    return script_or_errors.release_value();
}

inline ErrorOr<JsonValue> get_test_results(JS::Interpreter& interpreter)
{
    auto results = MUST(interpreter.global_object().get("__TestResults__"));
    auto json_string = MUST(JS::JSONObject::stringify_impl(interpreter.global_object(), results, JS::js_undefined(), JS::js_undefined()));

    return JsonValue::from_string(json_string);
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

    // Since g_vm is reused for each new interpreter, Interpreter::create will end up pushing multiple
    // global execution contexts onto the VM's execution context stack. To prevent this, we immediately
    // pop the global execution context off the execution context stack and manually handle pushing
    // and popping it. Since the global execution context should be the only thing on the stack
    // at interpreter creation, let's assert there is only one.
    VERIFY(g_vm->execution_context_stack().size() == 1);
    auto& global_execution_context = *g_vm->execution_context_stack().take_first();

    // FIXME: This is a hack while we're refactoring Interpreter/VM stuff.
    JS::VM::InterpreterExecutionScope scope(*interpreter);

    interpreter->heap().set_should_collect_on_every_allocation(g_collect_on_every_allocation);

    if (g_run_file) {
        auto result = g_run_file(test_path, *interpreter, global_execution_context);
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

    // FIXME: Since a new interpreter is created every time with a new realm, we no longer cache the test-common.js file as scripts are parsed for the current realm only.
    //        Find a way to cache this.
    auto result = parse_script(m_common_path, interpreter->realm());
    if (result.is_error()) {
        warnln("Unable to parse test-common.js");
        warnln("{}", result.error().error.to_string());
        warnln("{}", result.error().hint);
        cleanup_and_exit();
    }
    auto test_script = result.release_value();

    if (g_run_bytecode) {
        auto executable = MUST(JS::Bytecode::Generator::generate(test_script->parse_node()));
        executable->name = test_path;
        if (JS::Bytecode::g_dump_bytecode)
            executable->dump();
        JS::Bytecode::Interpreter bytecode_interpreter(interpreter->global_object(), interpreter->realm());
        MUST(bytecode_interpreter.run(*executable));
    } else {
        g_vm->push_execution_context(global_execution_context, interpreter->global_object());
        MUST(interpreter->run(*test_script));
        g_vm->pop_execution_context();
    }

    auto file_script = parse_script(test_path, interpreter->realm());
    if (file_script.is_error())
        return { test_path, file_script.error() };
    if (g_run_bytecode) {
        auto executable_result = JS::Bytecode::Generator::generate(file_script.value()->parse_node());
        if (!executable_result.is_error()) {
            auto executable = executable_result.release_value();
            executable->name = test_path;
            if (JS::Bytecode::g_dump_bytecode)
                executable->dump();
            JS::Bytecode::Interpreter bytecode_interpreter(interpreter->global_object(), interpreter->realm());
            (void)bytecode_interpreter.run(*executable);
        }
    } else {
        g_vm->push_execution_context(global_execution_context, interpreter->global_object());
        (void)interpreter->run(file_script.value());
        g_vm->pop_execution_context();
    }

    auto test_json = get_test_results(*interpreter);
    if (test_json.is_error()) {
        warnln("Received malformed JSON from test \"{}\"", test_path);
        cleanup_and_exit();
    }

    JSFileResult file_result { test_path.substring(m_test_root.length() + 1, test_path.length() - m_test_root.length() - 1) };

    // Collect logged messages
    auto user_output = MUST(interpreter->global_object().get("__UserOutput__"));

    auto& arr = user_output.as_array();
    for (auto& entry : arr.indexed_properties()) {
        auto message = MUST(arr.get(entry.index()));
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
