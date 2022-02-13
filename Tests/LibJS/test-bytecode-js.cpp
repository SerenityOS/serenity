/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Script.h>
#include <LibTest/TestCase.h>

#define SETUP_AND_PARSE(source)                                                 \
    auto vm = JS::VM::create();                                                 \
    auto ast_interpreter = JS::Interpreter::create<JS::GlobalObject>(*vm);      \
                                                                                \
    auto script_or_error = JS::Script::parse(source, ast_interpreter->realm()); \
    EXPECT(!script_or_error.is_error());                                        \
                                                                                \
    auto script = script_or_error.release_value();                              \
    auto const& program = script->parse_node();                                 \
    JS::Bytecode::Interpreter bytecode_interpreter(ast_interpreter->global_object(), ast_interpreter->realm());

#define EXPECT_NO_EXCEPTION(executable)                                 \
    auto executable = MUST(JS::Bytecode::Generator::generate(program)); \
    auto result = bytecode_interpreter.run(*executable);                \
    EXPECT(!result.is_error());                                         \
    if (result.is_error())                                              \
        dbgln("Error: {}", MUST(result.throw_completion().value()->to_string(bytecode_interpreter.global_object())));

#define EXPECT_NO_EXCEPTION_WITH_OPTIMIZATIONS(executable)                  \
    auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();      \
    passes.perform(*executable);                                            \
                                                                            \
    auto result_with_optimizations = bytecode_interpreter.run(*executable); \
                                                                            \
    EXPECT(!result_with_optimizations.is_error());                          \
    if (result_with_optimizations.is_error())                               \
        dbgln("Error: {}", MUST(result_with_optimizations.throw_completion().value()->to_string(bytecode_interpreter.global_object())));

#define EXPECT_NO_EXCEPTION_ALL(source)           \
    SETUP_AND_PARSE("(() => {\n" source "\n})()") \
    EXPECT_NO_EXCEPTION(executable)               \
    EXPECT_NO_EXCEPTION_WITH_OPTIMIZATIONS(executable)

TEST_CASE(empty_program)
{
    EXPECT_NO_EXCEPTION_ALL("");
}

TEST_CASE(if_statement_pass)
{
    EXPECT_NO_EXCEPTION_ALL("if (false) throw new Exception('failed');");
}

TEST_CASE(if_statement_fail)
{
    SETUP_AND_PARSE("if (true) throw new Exception('failed');");

    auto executable = MUST(JS::Bytecode::Generator::generate(program));
    auto result = bytecode_interpreter.run(*executable);
    EXPECT(result.is_error());
}

TEST_CASE(trivial_program)
{
    EXPECT_NO_EXCEPTION_ALL("if (1 + 1 !== 2) throw new Exception('failed');");
}

TEST_CASE(variables)
{
    EXPECT_NO_EXCEPTION_ALL("var a = 1; \n"
                            "if (a + 1 !== 2) throw new Exception('failed'); ");
}

TEST_CASE(function_call)
{
    EXPECT_NO_EXCEPTION_ALL("if (!isNaN(NaN)) throw new Exception('failed'); ");
}

TEST_CASE(function_delcaration_and_call)
{
    EXPECT_NO_EXCEPTION_ALL("var passed = false; \n"
                            "function f() { passed = true; return 1; }\n"
                            "if (f() !== 1) throw new Exception('failed');\n"
                            // The passed !== true is needed as otherwise UBSAN
                            // complains about unaligned access, until that
                            // is fixed or ignored care must be taken to prevent such cases in tests.
                            "if (passed !== true) throw new Exception('failed');");
}

TEST_CASE(generator_function_call)
{
    EXPECT_NO_EXCEPTION_ALL("function *g() { yield 2; }\n"
                            "var gen = g();\n"
                            "var result = gen.next();\n"
                            "if (result.value !== 2) throw new Exception('failed');");
}

TEST_CASE(loading_multiple_files)
{
    // This is a testcase which is very much like test-js and test262
    // which load some common files first and only then the actual test file.

    SETUP_AND_PARSE("function f() { return 'hello'; }");

    {
        EXPECT_NO_EXCEPTION(common_file_executable);
    }

    {
        auto test_file_script_or_error = JS::Script::parse("if (f() !== 'hello') throw new Exception('failed'); ", ast_interpreter->realm());
        EXPECT(!test_file_script_or_error.is_error());

        auto test_file_script = test_file_script_or_error.release_value();
        auto const& test_file_program = test_file_script->parse_node();

        auto executable = MUST(JS::Bytecode::Generator::generate(test_file_program));
        auto result = bytecode_interpreter.run(*executable);
        EXPECT(!result.is_error());
    }
}

TEST_CASE(catch_exception)
{
    // FIXME: Currently it seems that try/catch with finally is broken so we test both at once.
    EXPECT_NO_EXCEPTION_ALL("var hitCatch = false;\n"
                            "var hitFinally = false;\n"
                            "try {\n"
                            "   a();\n"
                            "} catch (e) {\n"
                            "    hitCatch = e instanceof ReferenceError;\n"
                            "    !1\n" // This is here to fix the alignment issue until that is actually resolved.
                            "} finally {\n"
                            "    hitFinally = true;\n"
                            "}\n"
                            "if (hitCatch !== true) throw new Exception('failed');\n"
                            "if (hitFinally !== true) throw new Exception('failed');");
}
