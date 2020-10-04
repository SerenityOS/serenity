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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Function::Function(Object& prototype)
    : Function(prototype, {}, {})
{
}

Function::Function(Object& prototype, Value bound_this, Vector<Value> bound_arguments)
    : Object(prototype)
    , m_bound_this(bound_this)
    , m_bound_arguments(move(bound_arguments))
{
}

Function::~Function()
{
}

BoundFunction* Function::bind(Value bound_this_value, Vector<Value> arguments)
{
    Function& target_function = is_bound_function() ? static_cast<BoundFunction&>(*this).target_function() : *this;

    auto bound_this_object = [bound_this_value, this]() -> Value {
        if (!m_bound_this.is_empty())
            return m_bound_this;
        switch (bound_this_value.type()) {
        case Value::Type::Undefined:
        case Value::Type::Null:
            if (vm().in_strict_mode())
                return bound_this_value;
            return &global_object();
        default:
            return bound_this_value.to_object(global_object());
        }
    }();

    i32 computed_length = 0;
    auto length_property = get("length");
    if (vm().exception())
        return nullptr;
    if (length_property.is_number())
        computed_length = max(0, length_property.as_i32() - static_cast<i32>(arguments.size()));

    Object* constructor_prototype = nullptr;
    auto prototype_property = target_function.get("prototype");
    if (vm().exception())
        return nullptr;
    if (prototype_property.is_object())
        constructor_prototype = &prototype_property.as_object();

    auto all_bound_arguments = bound_arguments();
    all_bound_arguments.append(move(arguments));

    return heap().allocate<BoundFunction>(global_object(), global_object(), target_function, bound_this_object, move(all_bound_arguments), computed_length, constructor_prototype);
}

void Function::visit_children(Visitor& visitor)
{
    Object::visit_children(visitor);

    visitor.visit(m_bound_this);

    for (auto argument : m_bound_arguments)
        visitor.visit(argument);
}

}
