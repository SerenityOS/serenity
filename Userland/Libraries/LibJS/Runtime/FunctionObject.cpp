/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

FunctionObject::FunctionObject(Object& prototype)
    : Object(prototype)
{
}

FunctionObject::~FunctionObject()
{
}

BoundFunction* FunctionObject::bind(Value bound_this_value, Vector<Value> arguments)
{
    auto& vm = this->vm();
    FunctionObject& target_function = is<BoundFunction>(*this) ? static_cast<BoundFunction&>(*this).bound_target_function() : *this;

    auto get_bound_this_object = [&vm, bound_this_value, this]() -> ThrowCompletionOr<Value> {
        if (is<BoundFunction>(*this) && !static_cast<BoundFunction&>(*this).bound_this().is_empty())
            return static_cast<BoundFunction&>(*this).bound_this();
        switch (bound_this_value.type()) {
        case Value::Type::Undefined:
        case Value::Type::Null:
            if (vm.in_strict_mode())
                return bound_this_value;
            return &global_object();
        default:
            return TRY(bound_this_value.to_object(global_object()));
        }
    };
    auto bound_this_object = TRY_OR_DISCARD(get_bound_this_object());

    i32 computed_length = 0;
    auto length_property = TRY_OR_DISCARD(get(vm.names.length));
    if (length_property.is_number())
        computed_length = max(0, length_property.as_i32() - static_cast<i32>(arguments.size()));

    Object* constructor_prototype = nullptr;
    auto prototype_property = TRY_OR_DISCARD(target_function.get(vm.names.prototype));
    if (prototype_property.is_object())
        constructor_prototype = &prototype_property.as_object();

    Vector<Value> all_bound_arguments;
    if (is<BoundFunction>(*this))
        all_bound_arguments.extend(static_cast<BoundFunction&>(*this).bound_arguments());
    all_bound_arguments.extend(move(arguments));

    return heap().allocate<BoundFunction>(global_object(), global_object(), target_function, bound_this_object, move(all_bound_arguments), computed_length, constructor_prototype);
}

}
