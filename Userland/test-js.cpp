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
#include <LibJS/Runtime/MarkedValueList.h>
#include <sys/time.h>
#include <stdlib.h>
#include <string.h>

#define TOP_LEVEL_TEST_NAME "__$$TOP_LEVEL$$__"

// FIXME: Will eventually not be necessary when all tests are converted
Vector<String> tests_to_run = {
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

struct FileTest {
    String name;
    bool passed;
};

struct FileSuite {
    String name;
    int passed { 0 };
    int failed { 0 };
    Vector<FileTest> tests {};
};

struct TestError {
    JS::Parser::Error error;
    String hint;
};

struct FileResults {
    String file;
    Optional<TestError> error {};
    int passed { 0 };
    int failed { 0 };
    Vector<FileSuite> suites {};
};

struct Results {
    Vector<FileResults> file_results {};
};

Optional<TestError> parse_and_run_file(JS::Interpreter& interpreter, const String& path)
{
    auto file = Core::File::construct(path);
    auto result = file->open(Core::IODevice::ReadOnly);
    ASSERT(result);

    auto contents = file->read_all();
    String test_file_string(reinterpret_cast<const char*>(contents.data()), contents.size());
    file->close();

    auto parser = JS::Parser(JS::Lexer(test_file_string));
    auto program = parser.parse_program();

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        return TestError { error, error.source_location_hint(test_file_string) };
    } else {
        interpreter.run(interpreter.global_object(), *program);
    }

    return {};
}

FileResults run_test(const String& path, const String& test_root)
{
    auto interpreter = JS::Interpreter::create<JS::GlobalObject>();

    if (parse_and_run_file(*interpreter, String::format("%s/test-common.js", test_root.characters())).has_value()) {
        dbg() << "test-common.js failed to parse";
        exit(1);
    }

    auto source_file_result = parse_and_run_file(*interpreter, String::format("%s/%s", test_root.characters(), path.characters()));
    if (source_file_result.has_value())
        return { path, source_file_result };

    // Print any output
    // FIXME: Should be printed to stdout in a nice format
    auto& arr = interpreter->get_variable("__UserOutput__", interpreter->global_object()).as_array();
    for (auto& entry : arr.indexed_properties()) {
        dbg() << "OUTPUT: " << entry.value_and_attributes(&interpreter->global_object()).value.to_string_without_side_effects();
    }

    // FIXME: This is _so_ scuffed
    auto result = interpreter->get_variable("__TestResults__", interpreter->global_object());
    auto json_object = interpreter->get_variable("JSON", interpreter->global_object());
    auto stringify = json_object.as_object().get("stringify");
    JS::MarkedValueList arguments(interpreter->heap());
    arguments.append(result);
    auto json_string = interpreter->call(stringify.as_function(), interpreter->this_value(interpreter->global_object()), move(arguments)).to_string(*interpreter);

    auto json_result = JsonValue::from_string(json_string);

    if (!json_result.has_value()) {
        dbg() << "BAD JSON:";
        dbg() << json_string;
        return {};
    }

    auto json = json_result.value();

    FileResults results { path };

    json.as_object().for_each_member([&](const String& property, const JsonValue& value) {
        FileSuite suite { property };

        value.as_object().for_each_member([&](const String& property1, const JsonValue& value1) {
            FileTest test { property1, false };

            if (value1.is_object()) {
                auto obj = value1.as_object();
                if (obj.has("passed")) {
                    auto passed = obj.get("passed");
                    test.passed = passed.is_bool() && passed.as_bool();
                }
            }

            if (test.passed) {
                suite.passed++;
            } else {
                suite.failed++;
            }

            suite.tests.append(test);
        });

        if (suite.failed) {
            results.failed++;
        } else {
            results.passed++;
        }

        results.suites.append(suite);
    });

    return results;
}

bool skip_test(char* test_name)
{
    return !strcmp(test_name, "test-common.js") || !strcmp(test_name, "run_tests.sh");
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

void print_file_results(const FileResults& results)
{
    if (results.failed || results.error.has_value()) {
        print_modifiers({ BG_RED, FG_BLACK, FG_BOLD });
        printf(" FAIL ");
        print_modifiers({ CLEAR });
    } else {
        print_modifiers({ BG_GREEN, FG_BLACK, FG_BOLD });
        printf(" PASS ");
        print_modifiers({ CLEAR });
    }

    printf(" %s\n", results.file.characters());

    if (results.error.has_value()) {
        auto test_error = results.error.value();

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

    if (results.failed) {
        for (auto& suite : results.suites) {
            if (!suite.failed)
                continue;

            bool top_level = suite.name == TOP_LEVEL_TEST_NAME;

            if (!top_level) {
                print_modifiers({ FG_GRAY, FG_BOLD });
                printf("       ❌ Suite:  ");
                print_modifiers({ CLEAR, FG_RED });
                printf("%s\n", suite.name.characters());
                print_modifiers({ CLEAR });
            }

            for (auto& test : suite.tests) {
                if (test.passed)
                    continue;

                if (!top_level) {
                    print_modifiers({ FG_GRAY, FG_BOLD });
                    printf("            Test: ");
                    print_modifiers({ CLEAR, FG_RED });
                    printf("%s\n", test.name.characters());
                    print_modifiers({ CLEAR });
                } else {
                    print_modifiers({ FG_GRAY, FG_BOLD });
                    printf("       ❌ Test:   ");
                    print_modifiers({ CLEAR, FG_RED });
                    printf("%s\n", test.name.characters());
                    print_modifiers({ CLEAR });
                }
            }
        }
    }
}

void print_results(const Results& results, double time_elapsed)
{
    for (auto& result : results.file_results)
        print_file_results(result);

    int suites_passed = 0;
    int suites_failed = 0;
    int tests_passed = 0;
    int tests_failed = 0;

    for (auto& file_result : results.file_results) {
        for (auto& suite : file_result.suites) {
            tests_passed += suite.passed;
            tests_failed += suite.failed;

            if (suite.failed) {
                suites_failed++;
            } else {
                suites_passed++;
            }
        }
    }


    printf("\nTest Suites: ");
    if (suites_failed) {
        print_modifiers({ FG_RED });
        printf("%d failed, ", suites_failed);
        print_modifiers({ CLEAR });
    }
    if (suites_passed) {
        print_modifiers({ FG_GREEN });
        printf("%d passed, ", suites_passed);
        print_modifiers({ CLEAR });
    }
    printf("%d total\n", suites_failed + suites_passed);

    printf("Tests:       ");
    if (tests_failed) {
        print_modifiers({ FG_RED });
        printf("%d failed, ", tests_failed);
        print_modifiers({ CLEAR });
    }
    if (tests_passed) {
        print_modifiers({ FG_GREEN });
        printf("%d passed, ", tests_passed);
        print_modifiers({ CLEAR });
    }
    printf("%d total\n", tests_failed + tests_passed);

    printf("Time:        %-.3fs\n\n", time_elapsed);
}

double get_time()
{
    struct timeval tv1;
    struct timezone tz1;
    auto return_code = gettimeofday(&tv1, &tz1);
    ASSERT(return_code >= 0);
    return static_cast<double>(tv1.tv_sec) + static_cast<double>(tv1.tv_usec) / 1'000'000;
}

int main(int, char** argv)
{
    String test_root = argv[1];
    Results results;

    double start_time = get_time();

    for (auto& test : tests_to_run)
        results.file_results.append(run_test(test, test_root));

    print_results(results, get_time() - start_time);

    return 0;
}

