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

#include <AK/JsonValue.h>
#include <AK/JsonObject.h>
#include <AK/LogStream.h>
#include <LibCore/File.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <sys/time.h>
#include <stdlib.h>

#define TOP_LEVEL_TEST_NAME "__$$TOP_LEVEL$$__"

// FIXME: Will eventually not be necessary when all tests are converted
Vector<String> tests_to_run = {
    "builtins/Boolean/Boolean.js",
    "builtins/Boolean/Boolean.prototype.js",
    "builtins/Boolean/Boolean.prototype.toString.js",
    "builtins/Boolean/Boolean.prototype.valueOf.js",
    "builtins/Date/Date.js",
    "builtins/Date/Date.now.js",
    "builtins/Date/Date.prototype.getDate.js",
    "builtins/Date/Date.prototype.getDay.js",
    "builtins/Date/Date.prototype.getFullYear.js",
    "builtins/Date/Date.prototype.getHours.js",
    "builtins/Date/Date.prototype.getMilliseconds.js",
    "builtins/Date/Date.prototype.getMinutes.js",
    "builtins/Date/Date.prototype.getMonth.js",
    "builtins/Date/Date.prototype.getSeconds.js",
    "builtins/Date/Date.prototype.getTime.js",
    "builtins/Error/Error.js",
    "builtins/Error/Error.prototype.name.js",
    "builtins/Error/Error.prototype.toString.js",
    "builtins/Function/Function.js",
    "builtins/Function/Function.prototype.apply.js",
    "builtins/Function/Function.prototype.bind.js",
    "builtins/Function/Function.prototype.call.js",
    "builtins/Function/Function.prototype.toString.js",
    "builtins/functions/isFinite.js",
    "builtins/functions/isNaN.js",
    "builtins/functions/parseFloat.js",
    "builtins/Infinity/Infinity.js",
    "builtins/JSON/JSON.parse.js",
    "builtins/JSON/JSON.parse-reviver.js",
    "builtins/JSON/JSON.stringify.js",
    "builtins/JSON/JSON.stringify-order.js",
    "builtins/JSON/JSON.stringify-proxy.js",
    "builtins/JSON/JSON.stringify-replacer.js",
    "builtins/JSON/JSON.stringify-space.js",
    "builtins/Math/Math-constants.js",
    "builtins/Math/Math.abs.js",
    "builtins/Math/Math.acosh.js",
    "builtins/Math/Math.asinh.js",
    "builtins/Math/Math.atanh.js",
    "builtins/Math/Math.cbrt.js",
    "builtins/Math/Math.ceil.js",
    "builtins/Math/Math.clz32.js",
    "builtins/Math/Math.cos.js",
    "builtins/Math/Math.exp.js",
    "builtins/Math/Math.expm1.js",
    "builtins/Math/Math.floor.js",
    "builtins/Math/Math.log1p.js",
    "builtins/Math/Math.max.js",
    "builtins/Math/Math.min.js",
    "builtins/Math/Math.pow.js",
    "builtins/Math/Math.sign.js",
    "builtins/Math/Math.sqrt.js",
    "builtins/Math/Math.tan.js",
    "builtins/Math/Math.trunc.js",
    "builtins/NaN/NaN.js",
    "builtins/Number/Number.js",
    "builtins/Number/Number-constants.js",
    "builtins/Number/Number.isFinite.js",
    "builtins/Number/Number.isInteger.js",
    "builtins/Number/Number.isNaN.js",
    "builtins/Number/Number.isSafeInteger.js",
    "builtins/Number/Number.parseFloat.js",
    "builtins/Number/Number.prototype.js",
    "builtins/Object/Object.js",
    "builtins/Object/Object.defineProperty.js",
    "builtins/Object/Object.entries.js",
    "builtins/Object/Object.getOwnPropertyDescriptor.js",
    "builtins/Object/Object.getOwnPropertyNames.js",
    "builtins/Object/Object.getPrototypeOf.js",
    "builtins/Object/Object.is.js",
    "builtins/Object/Object.isExtensible.js",
    "builtins/Object/Object.keys.js",
    "builtins/Object/Object.preventExtensions.js",
    "builtins/Object/Object.prototype.js",
    "builtins/Object/Object.prototype.constructor.js",
    "builtins/Object/Object.prototype.hasOwnProperty.js",
    "builtins/Object/Object.prototype.toLocaleString.js",
    "builtins/Object/Object.prototype.toString.js",
    "builtins/Object/Object.setPrototypeOf.js",
    "builtins/Object/Object.values.js",
    "builtins/Proxy/Proxy.js",
    "builtins/Proxy/Proxy.handler-apply.js",
    "builtins/Proxy/Proxy.handler-construct.js",
    "builtins/Proxy/Proxy.handler-defineProperty.js",
    "builtins/Proxy/Proxy.handler-deleteProperty.js",
    "builtins/Proxy/Proxy.handler-get.js",
    "builtins/Proxy/Proxy.handler-getOwnPropertyDescriptor.js",
    "builtins/Proxy/Proxy.handler-getPrototypeOf.js",
    "builtins/Proxy/Proxy.handler-has.js",
    "builtins/Proxy/Proxy.handler-isExtensible.js",
    "builtins/Proxy/Proxy.handler-preventExtensions.js",
    "builtins/Proxy/Proxy.handler-set.js",
    "builtins/Proxy/Proxy.handler-setPrototypeOf.js",\
    "builtins/Reflect/Reflect.apply.js",
    "builtins/Reflect/Reflect.construct.js",
    "builtins/Reflect/Reflect.defineProperty.js",
    "builtins/Reflect/Reflect.deleteProperty.js",
    "builtins/Reflect/Reflect.get.js",
    "builtins/Reflect/Reflect.getOwnPropertyDescriptor.js",
    "builtins/Reflect/Reflect.getPrototypeOf.js",
    "builtins/Reflect/Reflect.has.js",
    "builtins/Reflect/Reflect.isExtensible.js",
    "builtins/Reflect/Reflect.ownKeys.js",
    "builtins/Reflect/Reflect.preventExtensions.js",
    "builtins/Reflect/Reflect.set.js",
    "builtins/Reflect/Reflect.setPrototypeOf.js",
    "builtins/String/String.js",
    "builtins/String/String.fromCharCode.js",
    "builtins/String/String.prototype.js",
    "builtins/String/String.prototype-generic-functions.js",
    "builtins/String/String.prototype.charAt.js",
    "builtins/String/String.prototype.includes.js",
    "builtins/String/String.prototype.indexOf.js",
    "builtins/String/String.prototype.lastIndexOf.js",
    "builtins/String/String.prototype.padEnd.js",
    "builtins/String/String.prototype.padStart.js",
    "builtins/String/String.prototype.repeat.js",
    "builtins/String/String.prototype.slice.js",
    "builtins/String/String.prototype.startsWith.js",
    "builtins/String/String.prototype.substring.js",
    "builtins/String/String.prototype.toLowerCase.js",
    "builtins/String/String.prototype.toString.js",
    "builtins/String/String.prototype.toUpperCase.js",
    "builtins/String/String.prototype.trim.js",
    "builtins/String/String.prototype.valueOf.js",
    "builtins/String/String.raw.js",
    "add-values-to-primitive.js",
    "automatic-semicolon-insertion.js",
    "comments-basic.js",
    "debugger-statement.js",
    "empty-statements.js",
    "exception-ReferenceError.js",
    "exponentiation-basic.js",
    "indexed-access-string-object.js",
    "invalid-lhs-in-assignment.js",
    "let-scoping.js",
    "new-expression.js",
    "numeric-literals-basic.js",
    "object-getter-setter-shorthand.js",
    "object-method-shorthand.js",
    "object-spread.js",
    "tagged-template-literals.js",
    "test-common-tests.js",
    "switch-basic.js",
    "update-expression-on-member-expression.js",
};

enum class TestResult {
    Pass,
    Fail,
};

struct JSTest {
    String name;
    TestResult result;
};

struct JSSuite {
    String name;
    bool has_failed_tests { false };
    Vector<JSTest> tests {};
};

struct ParserError {
    JS::Parser::Error error;
    String hint;
};

struct JSFileResult {
    String name;
    Optional<ParserError> error {};
    bool has_failed_tests { false };
    Vector<JSSuite> suites {};
};

struct JSTestRunnerCounts {
    int tests_failed { 0 };
    int tests_passed { 0 };
    int suites_failed { 0 };
    int suites_passed { 0 };
    int files_total { 0 };
};

using JSTestRunnerResult = Vector<JSFileResult>;

double get_time()
{
    struct timeval tv1;
    struct timezone tz1;
    auto return_code = gettimeofday(&tv1, &tz1);
    ASSERT(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) + static_cast<double>(tv1.tv_usec) / 1'000'000;
}

class TestRunner {
public:
    TestRunner(String test_root)
        : m_test_root(move(test_root))
    {
    }

    void run();

private:
    JSFileResult run_file_test(const String& test_path);
    static void print_file_result(const JSFileResult& file_result);
    void print_test_results() const;

    String m_test_root;

    double m_start_time;
    double m_end_time;
    JSTestRunnerCounts m_counts;
};

void TestRunner::run()
{
    m_start_time = get_time();

    // FIXME: The way this currently works, the time it takes to print is
    // counted in the total test duration. In order to change this, we'd have to
    // loop over the paths and collect the results, record then time, and then
    // print. However, doing it this way provides no feedback to the user at
    // first, and then all the feedback at once. Both ways have their pros and
    // cons, but which one we prefer still needs to be decided.
    for (auto& test_path : tests_to_run)
        print_file_result(run_file_test(test_path));

    m_end_time = get_time();

    print_test_results();
}

Optional<ParserError> parse_and_run_file(JS::Interpreter& interpreter, const String& path)
{
    auto file = Core::File::construct(path);
    auto result = file->open(Core::IODevice::ReadOnly);
    if (!result) {
        printf("Failed to open the following file: \"%s\"\n", path.characters());
        exit(1);
    }

    auto contents = file->read_all();
    String test_file_string(reinterpret_cast<const char*>(contents.data()), contents.size());
    file->close();

    auto parser = JS::Parser(JS::Lexer(test_file_string));
    auto program = parser.parse_program();

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        return ParserError { error, error.source_location_hint(test_file_string) };
    } else {
        interpreter.run(interpreter.global_object(), *program);
    }

    return {};
}

Optional<JsonValue> get_test_results(JS::Interpreter& interpreter)
{
    auto result = interpreter.get_variable("__TestResults__", interpreter.global_object());
    auto json_string = JS::JSONObject::stringify_impl(interpreter, interpreter.global_object(), result, JS::js_undefined(), JS::js_undefined());

    auto json = JsonValue::from_string(json_string);
    if (!json.has_value())
        return {};

    return json.value();
}

JSFileResult TestRunner::run_file_test(const String& test_path)
{
    auto interpreter = JS::Interpreter::create<JS::GlobalObject>();

    if (parse_and_run_file(*interpreter, String::format("%s/test-common.js", m_test_root.characters())).has_value()) {
        dbg() << "test-common.js failed to parse";
        exit(1);
    }

    auto source_file_result = parse_and_run_file(*interpreter, String::format("%s/%s", m_test_root.characters(), test_path.characters()));
    if (source_file_result.has_value())
        return { test_path, source_file_result };

    // Print any output
    // FIXME: Should be printed to stdout in a nice format
    auto& arr = interpreter->get_variable("__UserOutput__", interpreter->global_object()).as_array();
    for (auto& entry : arr.indexed_properties()) {
        dbg() << test_path << ": " << entry.value_and_attributes(&interpreter->global_object()).value.to_string_without_side_effects();
    }

    auto test_json = get_test_results(*interpreter);
    if (!test_json.has_value()) {
        printf("Received malformed JSON from test \"%s\"\n", test_path.characters());
        exit(1);
    }

    JSFileResult file_result { test_path };

    test_json.value().as_object().for_each_member([&](const String& suite_name, const JsonValue& suite_value) {
        JSSuite suite { suite_name };

        if (!suite_value.is_object()) {
            printf("Test JSON has a suite which is not an object (\"%s\")\n", test_path.characters());
            exit(1);
        }

        suite_value.as_object().for_each_member([&](const String& test_name, const JsonValue& test_value) {
            JSTest test { test_name, TestResult::Fail };

            ASSERT(test_value.is_object());
            ASSERT(test_value.as_object().has("result"));

            auto result = test_value.as_object().get("result");
            ASSERT(result.is_string());
            auto result_string = result.as_string();
            if (result_string == "pass") {
                test.result = TestResult::Pass;
                m_counts.tests_passed++;
            } else {
                test.result = TestResult::Fail;
                m_counts.tests_failed++;
                suite.has_failed_tests = true;
            }

            suite.tests.append(test);
        });

        if (suite.has_failed_tests) {
            m_counts.suites_failed++;
            file_result.has_failed_tests = true;
        } else {
            m_counts.suites_passed++;
        }

        file_result.suites.append(suite);
    });

    m_counts.files_total++;

    return file_result;
}

enum Modifier {
    BG_RED,
    BG_GREEN,
    FG_RED,
    FG_GREEN,
    FG_GRAY,
    FG_BLACK,
    FG_BOLD,
    CLEAR,
};

void print_modifiers(Vector<Modifier> modifiers)
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
            case FG_GRAY:
                return "\033[38;2;135;139;148m";
            case FG_BLACK:
                return "\033[30m";
            case FG_BOLD:
                return "\033[1m";
            case CLEAR:
                return "\033[0m";
            }
            ASSERT_NOT_REACHED();
        };
        printf("%s", code().characters());
    }
}

void TestRunner::print_file_result(const JSFileResult& file_result)
{
    if (file_result.has_failed_tests || file_result.error.has_value()) {
        print_modifiers({ BG_RED, FG_BLACK, FG_BOLD });
        printf(" FAIL ");
        print_modifiers({ CLEAR });
    } else {
        print_modifiers({ BG_GREEN, FG_BLACK, FG_BOLD });
        printf(" PASS ");
        print_modifiers({ CLEAR });
    }

    printf(" %s\n", file_result.name.characters());

    if (file_result.error.has_value()) {
        auto test_error = file_result.error.value();

        print_modifiers({ FG_RED });
        printf("       ❌ The file failed to parse\n\n");
        print_modifiers({ FG_GRAY });
        for (auto& message : test_error.hint.split('\n', true)) {
            printf("            %s\n", message.characters());
        }
        print_modifiers({ FG_RED });
        printf("            %s\n\n", test_error.error.to_string().characters());

        return;
    }

    if (file_result.has_failed_tests) {
        for (auto& suite : file_result.suites) {
            if (!suite.has_failed_tests)
                continue;

            print_modifiers({ FG_GRAY, FG_BOLD });
            printf("       ❌ Suite:  ");
            if (suite.name == TOP_LEVEL_TEST_NAME) {
                print_modifiers({ CLEAR, FG_GRAY });
                printf("<top-level>\n");
            } else {
                print_modifiers({ CLEAR, FG_RED });
                printf("%s\n", suite.name.characters());
            }
            print_modifiers({ CLEAR });

            for (auto& test : suite.tests) {
                if (test.result == TestResult::Pass)
                    continue;

                print_modifiers({ FG_GRAY, FG_BOLD });
                printf("            Test:   ");
                print_modifiers({ CLEAR, FG_RED });
                printf("%s\n", test.name.characters());
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
    if (m_counts.tests_passed) {
        print_modifiers({ FG_GREEN });
        printf("%d passed, ", m_counts.tests_passed);
        print_modifiers({ CLEAR });
    }
    printf("%d total\n", m_counts.tests_failed + m_counts.tests_passed);

    printf("Files:       %d total\n", m_counts.files_total);
    printf("Time:        %-.3fs\n\n", m_end_time - m_start_time);
}

int main(int, char**)
{
#ifdef __serenity__
    TestRunner("/home/anon/js-tests").run();
#else
    char* serenity_root = getenv("SERENITY_ROOT");
    if (!serenity_root) {
        printf("test-js requires the SERENITY_ROOT environment variable to be set");
        return 1;
    }
    TestRunner(String::format("%s/Libraries/LibJS/Tests", serenity_root)).run();
#endif

    return 0;
}
