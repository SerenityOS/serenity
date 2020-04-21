/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Function.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GeneratorFunction.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GeneratorFunction* GeneratorFunction::create(GlobalObject& global_object, const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment, LexicalEnvironment* own_environment)
{
    return global_object.heap().allocate<GeneratorFunction>(name, body, move(parameters), parent_environment, *global_object.function_prototype(), own_environment);
}

GeneratorFunction::GeneratorFunction(const FlyString& name, const Statement& body, Vector<FlyString> parameters, LexicalEnvironment* parent_environment, Object& prototype, LexicalEnvironment* own_environment)
    : ScriptFunction(name, move(body), move(parameters), parent_environment, prototype)
    , m_own_environment(own_environment)
{
}

GeneratorFunction::~GeneratorFunction()
{
}

void GeneratorFunction::visit_children(Visitor& visitor)
{
    ScriptFunction::visit_children(visitor);
    visitor.visit(m_own_environment);
}

LexicalEnvironment* GeneratorFunction::create_environment()
{
    if (m_own_environment)
        return m_own_environment;

    return m_own_environment = ScriptFunction::create_environment();
}

Value GeneratorFunction::call(Interpreter& interpreter)
{
    auto& argument_values = interpreter.call_frame().arguments;
    ArgumentVector arguments;
    for (size_t i = 0; i < m_parameters.size(); ++i) {
        auto name = parameters()[i];
        auto value = js_undefined();
        if (i < argument_values.size())
            value = argument_values[i];
        arguments.append({ name, value });
        interpreter.current_environment()->set(name, { value, DeclarationKind::Var });
    }

    auto new_body = adopt(*new BlockStatement);
    auto* block = static_cast<ScopeNode*>(new_body);

    auto& my_block = static_cast<const ScopeNode&>(body());

    for (auto& node : my_block.children()) {
        block->append(node);
    }
    HashMap<FlyString, Variable> values;
    auto new_lexical_environment = interpreter.heap().allocate<LexicalEnvironment>(move(values), create_environment());
    auto iterator = heap().allocate<Iterator>(
        *heap().allocate<GeneratorFunction>(name(), *new_body, parameters(), m_parent_environment, *prototype(), new_lexical_environment),
        [&interpreter, argument_values, arguments](Object& generator_object, const Vector<Value>& next_arguments) -> Iterator::IteratorResult {
            auto& generator = static_cast<GeneratorFunction&>(generator_object);
            if (generator.m_done)
                return { true, js_undefined() };

            auto& call_frame = interpreter.push_call_frame();
            call_frame.environment = generator.create_environment();
            call_frame.arguments = next_arguments;
            call_frame.function_name = generator.name();

            auto result = interpreter.destructive_run(generator.m_body, arguments, ScopeType::Function);

            interpreter.pop_call_frame();

            if (interpreter.has_returned() || static_cast<ScopeNode&>(*generator.m_body).children().size() == 0)
                generator.m_done = true;
            return { generator.m_done, result };
        });

    return Value(iterator);
}

Value GeneratorFunction::construct(Interpreter& interpreter)
{
    return interpreter.throw_exception<TypeError>("Not a constructor");
}

}
