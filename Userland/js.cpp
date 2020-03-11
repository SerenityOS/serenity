/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/NonnullOwnPtr.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Object.h>
#include <LibJS/Parser.h>
#include <LibJS/PrimitiveString.h>
#include <LibJS/Value.h>
#include <stdio.h>

#define PROGRAM 6

static NonnullOwnPtr<JS::Program> build_program(JS::Heap&);

int main()
{
    JS::Interpreter interpreter;

    auto program = build_program(interpreter.heap());
    program->dump(0);

    auto result = interpreter.run(*program);
    dbg() << "Interpreter returned " << result;

    printf("%s\n", result.to_string().characters());

    dbg() << "Collecting garbage on exit...";
    interpreter.heap().collect_garbage();
    return 0;
}

#if PROGRAM == 1
NonnullOwnPtr<JS::Program> build_program(JS::Heap&)
{
    // function foo() { return (1 + 2) + 3; }
    // foo();

    auto block = make<JS::BlockStatement>();
    block->append<JS::ReturnStatement>(
        make<JS::BinaryExpression>(
            JS::BinaryOp::Plus,
            make<JS::BinaryExpression>(
                JS::BinaryOp::Plus,
                make<JS::Literal>(JS::Value(1)),
                make<JS::Literal>(JS::Value(2))),
            make<JS::Literal>(JS::Value(3))));

    auto program = make<JS::Program>();
    program->append<JS::FunctionDeclaration>("foo", move(block));
    program->append<JS::ExpressionStatement>(make<JS::CallExpression>("foo"));
    return program;
}
#elif PROGRAM == 2
NonnullOwnPtr<JS::Program> build_program(JS::Heap&)
{
    // c = 1;
    // function foo() {
    //   var a = 5;
    //   var b = 7;
    //   return a + b + c;
    // }
    // foo();

    auto program = make<JS::Program>();
    program->append<JS::ExpressionStatement>(make<JS::AssignmentExpression>(
        JS::AssignmentOp::Assign,
        make<JS::Identifier>("c"),
        make<JS::Literal>(JS::Value(1))));

    auto block = make<JS::BlockStatement>();
    block->append<JS::VariableDeclaration>(
        make<JS::Identifier>("a"),
        make<JS::Literal>(JS::Value(5)),
        JS::DeclarationType::Var);
    block->append<JS::VariableDeclaration>(
        make<JS::Identifier>("b"),
        make<JS::Literal>(JS::Value(7)),
        JS::DeclarationType::Var);

    block->append<JS::ReturnStatement>(
        make<JS::BinaryExpression>(
            JS::BinaryOp::Plus,
            make<JS::BinaryExpression>(
                JS::BinaryOp::Plus,
                make<JS::Identifier>("a"),
                make<JS::Identifier>("b")),
            make<JS::Identifier>("c")));

    program->append<JS::FunctionDeclaration>("foo", move(block));
    program->append<JS::ExpressionStatement>(make<JS::CallExpression>("foo"));

    return program;
}
#elif PROGRAM == 3
NonnullOwnPtr<JS::Program> build_program(JS::Heap&)
{
    // function foo() {
    //   var x = {};
    //   $gc();
    // }
    // foo();

    auto block = make<JS::BlockStatement>();
    block->append<JS::VariableDeclaration>(
        make<JS::Identifier>("x"),
        make<JS::ObjectExpression>(),
        JS::DeclarationType::Var);
    block->append<JS::ExpressionStatement>(make<JS::CallExpression>("$gc"));

    auto program = make<JS::Program>();
    program->append<JS::FunctionDeclaration>("foo", move(block));
    program->append<JS::ExpressionStatement>(make<JS::CallExpression>("foo"));
    return program;
}
#elif PROGRAM == 4
NonnullOwnPtr<JS::Program> build_program(JS::Heap&)
{
    // function foo() {
    //   function bar() {
    //      var y = 6;
    //   }
    //
    //   bar()
    //   return y;
    // }
    // foo(); //I should return `undefined` because y is bound to the inner-most enclosing function, i.e the nested one (bar()), therefore, it's undefined in the scope of foo()

    auto block_bar = make<JS::BlockStatement>();
    block_bar->append<JS::VariableDeclaration>(make<JS::Identifier>("y"), make<JS::Literal>(JS::Value(6)), JS::DeclarationType::Var);

    auto block_foo = make<JS::BlockStatement>();
    block_foo->append<JS::FunctionDeclaration>("bar", move(block_bar));
    block_foo->append<JS::ExpressionStatement>(make<JS::CallExpression>("bar"));
    block_foo->append<JS::ReturnStatement>(make<JS::Identifier>("y"));

    auto program = make<JS::Program>();
    program->append<JS::FunctionDeclaration>("foo", move(block_foo));
    program->append<JS::ExpressionStatement>(make<JS::CallExpression>("foo"));
    return program;
}
#elif PROGRAM == 5
NonnullOwnPtr<JS::Program> build_program(JS::Heap& heap)
{
    // "hello friends".length

    auto program = make<JS::Program>();
    program->append<JS::ExpressionStatement>(make<JS::MemberExpression>(
        make<JS::Literal>(JS::Value(js_string(heap, "hello friends"))),
        make<JS::Identifier>("length")));

    return program;
}
#elif PROGRAM == 6
NonnullOwnPtr<JS::Program> build_program(JS::Heap&)
{
    const char* source = "var foo = 1;\n"
                         "function bar() {\n"
                         "    return 38;\n"
                         "}\n"
                         "foo = {};\n"
                         "foo = bar() + 4;\n"
                         "foo;\n";

    auto parser = JS::Parser(JS::Lexer(source));
    return parser.parse_program();
}
#endif
