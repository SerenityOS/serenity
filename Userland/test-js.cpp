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
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <stdlib.h>
#include <sys/time.h>

#define TOP_LEVEL_TEST_NAME "__$$TOP_LEVEL$$__"

// FIXME: Will eventually not be necessary when all tests are converted
Vector<String> tests_to_run = {
    "builtins/Array/Array.js",
    "builtins/Array/array-basic.js",
    "builtins/Array/array-length-setter.js",
    "builtins/Array/array-shrink-during-find-crash.js",
    "builtins/Array/array-spread.js",
    "builtins/Array/Array.isArray.js",
    "builtins/Array/Array.of.js",
    "builtins/Array/Array.prototype-generic-functions.js",
    "builtins/Array/Array.prototype.concat.js",
    "builtins/Array/Array.prototype.every.js",
    "builtins/Array/Array.prototype.fill.js",
    "builtins/Array/Array.prototype.filter.js",
    "builtins/Array/Array.prototype.find.js",
    "builtins/Array/Array.prototype.findIndex.js",
    "builtins/Array/Array.prototype.forEach.js",
    "builtins/Array/Array.prototype.includes.js",
    "builtins/Array/Array.prototype.indexOf.js",
    "builtins/Array/Array.prototype.join.js",
    "builtins/Array/Array.prototype.lastIndexOf.js",
    "builtins/Array/Array.prototype.map.js",
    "builtins/Array/Array.prototype.pop.js",
    "builtins/Array/Array.prototype.push.js",
    "builtins/Array/Array.prototype.reduce.js",
    "builtins/Array/Array.prototype.reduceRight.js",
    "builtins/Array/Array.prototype.reverse.js",
    "builtins/Array/Array.prototype.shift.js",
    "builtins/Array/Array.prototype.slice.js",
    "builtins/Array/Array.prototype.some.js",
    "builtins/Array/Array.prototype.splice.js",
    "builtins/Array/Array.prototype.toLocaleString.js",
    "builtins/Array/Array.prototype.toString.js",
    "builtins/Array/Array.prototype.unshift.js",
    "builtins/BigInt/BigInt.js",
    "builtins/BigInt/bigint-basic.js",
    "builtins/BigInt/bigint-number-mix-errors.js",
    "builtins/BigInt/BigInt.prototype.toLocaleString.js",
    "builtins/BigInt/BigInt.prototype.toString.js",
    "builtins/BigInt/BigInt.prototype.valueOf.js",
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
    "builtins/Proxy/Proxy.handler-setPrototypeOf.js",
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
    "builtins/Symbol/Symbol.js",
    "builtins/Symbol/Symbol.for.js",
    "builtins/Symbol/Symbol.keyFor.js",
    "builtins/Symbol/Symbol.prototype.toString.js",
    "builtins/Symbol/Symbol.prototype.valueOf.js",
    "classes/class-advanced-extends.js",
    "classes/class-basic.js",
    "classes/class-constructor.js",
    "classes/class-errors.js",
    "classes/class-expressions.js",
    "classes/class-getters.js",
    "classes/class-inheritance.js",
    "classes/class-methods.js",
    "classes/class-setters.js",
    "classes/class-static.js",
    "classes/class-static-getters.js",
    "classes/class-static-setters.js",
    "classes/class-strict-mode.js",
    "functions/arrow-functions.js",
    "functions/constructor-basic.js",
    "functions/function-default-parameters.js",
    "functions/function-hoisting.js",
    "functions/function-length.js",
    "functions/function-missing-arg.js",
    "functions/function-name.js",
    "functions/function-rest-params.js",
    "functions/function-spread.js",
    "functions/function-strict-mode.js",
    "functions/function-this-in-arguments.js",
    "functions/function-TypeError.js",
    "loops/continue-basic.js",
    "loops/do-while-basic.js",
    "loops/for-basic.js",
    "loops/for-head-errors.js",
    "loops/for-in-basic.js",
    "loops/for-no-curlies.js",
    "loops/for-of-basic.js",
    "loops/for-scopes.js",
    "loops/while-basic.js",
    "operators/assignment-operators.js",
    "operators/binary-bitwise-left-shift.js",
    "operators/binary-bitwise-or.js",
    "operators/binary-bitwise-right-shift.js",
    "operators/binary-bitwise-unsigned-right-shift.js",
    "operators/binary-relational.js",
    "operators/comma-operator.js",
    "operators/delete-basic.js",
    "operators/delete-global-variable.js",
    "operators/delete-globalThis-property-crash.js",
    "operators/in-operator-basic.js",
    "operators/instanceof-basic.js",
    "operators/logical-and.js",
    "operators/logical-expressions-short-circuit.js",
    "operators/logical-nullish-coalescing.js",
    "operators/logical-or.js",
    "operators/modulo-basic.js",
    "operators/ternary-basic.js",
    "operators/typeof-basic.js",
    "operators/void-basic.js",
    "add-values-to-primitive.js",
    "automatic-semicolon-insertion.js",
    "comments-basic.js",
    "const-reassignment.js",
    "debugger-statement.js",
    "empty-statements.js",
    "exception-ReferenceError.js",
    "exponentiation-basic.js",
    "indexed-access-string-object.js",
    "invalid-lhs-in-assignment.js",
    "let-scoping.js",
    "new-expression.js",
    "numeric-literals-basic.js",
    "object-basic.js",
    "object-getter-setter-shorthand.js",
    "object-method-shorthand.js",
    "object-spread.js",
    "parser-unary-associativity.js",
    "program-strict-mode.js",
    "strict-mode-errors.js",
    "string-escapes.js",
    "string-spread.js",
    "switch-basic.js",
    "switch-break.js",
    "tagged-template-literals.js",
    "template-literals.js",
    "test-common-tests.js",
    "throw-basic.js",
    "to-number-basic.js",
    "to-number-exception.js",
    "update-expression-on-member-expression.js",
    "update-expressions-basic.js",
    "var-multiple-declarator.js",
    "var-scoping.js",
    "variable-undefined.js",
};

enum class TestResult {
    Pass,
    Fail,
    Skip,
};

struct JSTest {
    String name;
    TestResult result;
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

using JSTestRunnerResult = Vector<JSFileResult>;

class TestRunner {
public:
    TestRunner(String test_root, bool print_times)
        : m_test_root(move(test_root))
        , m_print_times(print_times)
    {
    }

    void run();

private:
    JSFileResult run_file_test(const String& test_path);
    void print_file_result(const JSFileResult& file_result) const;
    void print_test_results() const;

    String m_test_root;
    bool m_print_times;

    double m_total_elapsed_time_in_ms { 0 };
    JSTestRunnerCounts m_counts;

    RefPtr<JS::Program> m_test_program;
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
    return JS::Value(interpreter.in_strict_mode());
}

double get_time()
{
    struct timeval tv1;
    struct timezone tz1;
    auto return_code = gettimeofday(&tv1, &tz1);
        ASSERT(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) * 1000.0 + static_cast<double>(tv1.tv_usec) / 1000.0;
}

void TestRunner::run()
{
    for (auto& test_path : tests_to_run)
        print_file_result(run_file_test(test_path));

    print_test_results();
}

Result<NonnullRefPtr<JS::Program>, ParserError> parse_file(const String& file_path)
{
    auto file = Core::File::construct(file_path);
    auto result = file->open(Core::IODevice::ReadOnly);
    if (!result) {
        printf("Failed to open the following file: \"%s\"\n", file_path.characters());
        exit(1);
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
    double start_time = get_time();
    auto interpreter = JS::Interpreter::create<TestRunnerGlobalObject>();

    if (!m_test_program) {
        auto result = parse_file(String::format("%s/test-common.js", m_test_root.characters()));
        if (result.is_error()) {
            printf("Unable to parse test-common.js");
            exit(1);
        }
        m_test_program = result.value();
    }

    interpreter->run(interpreter->global_object(), *m_test_program);

    auto file_program = parse_file(String::format("%s/%s", m_test_root.characters(), test_path.characters()));
    if (file_program.is_error())
        return { test_path, file_program.error() };
    interpreter->run(interpreter->global_object(), *file_program.value());

    auto test_json = get_test_results(*interpreter);
    if (!test_json.has_value()) {
        printf("Received malformed JSON from test \"%s\"\n", test_path.characters());
        exit(1);
    }

    JSFileResult file_result { test_path };

    // Collect logged messages
    auto& arr = interpreter->get_variable("__UserOutput__", interpreter->global_object()).as_array();
    for (auto& entry : arr.indexed_properties()) {
        auto message = entry.value_and_attributes(&interpreter->global_object()).value;
        file_result.logged_messages.append(message.to_string_without_side_effects());
    }

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
            } else if (result_string == "fail") {
                test.result = TestResult::Fail;
                m_counts.tests_failed++;
                suite.most_severe_test_result = TestResult::Fail;
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

    file_result.time_taken = get_time() - start_time;
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
                    printf("%s (failed)\n", test.name.characters());
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

    Core::ArgsParser args_parser;
    args_parser.add_option(print_times, "Show duration of each test", "show-time", 't');
    args_parser.parse(argc, argv);

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

    return 0;
}
