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
    : FunctionObject({}, {}, prototype)
{
}

FunctionObject::FunctionObject(Value bound_this, Vector<Value> bound_arguments, Object& prototype)
    : Object(prototype)
    , m_bound_this(bound_this)
    , m_bound_arguments(move(bound_arguments))
{
}

FunctionObject::~FunctionObject()
{
}

BoundFunction* FunctionObject::bind(Value bound_this_value, Vector<Value> arguments)
{
    auto& vm = this->vm();
    FunctionObject& target_function = is<BoundFunction>(*this) ? static_cast<BoundFunction&>(*this).target_function() : *this;

    auto bound_this_object = [&vm, bound_this_value, this]() -> Value {
        if (!m_bound_this.is_empty())
            return m_bound_this;
        switch (bound_this_value.type()) {
        case Value::Type::Undefined:
        case Value::Type::Null:
            if (vm.in_strict_mode())
                return bound_this_value;
            return &global_object();
        default:
            return bound_this_value.to_object(global_object());
        }
    }();

    i32 computed_length = 0;
    auto length_property = get(vm.names.length);
    if (vm.exception())
        return nullptr;
    if (length_property.is_number())
        computed_length = max(0, length_property.as_i32() - static_cast<i32>(arguments.size()));

    Object* constructor_prototype = nullptr;
    auto prototype_property = target_function.get(vm.names.prototype);
    if (vm.exception())
        return nullptr;
    if (prototype_property.is_object())
        constructor_prototype = &prototype_property.as_object();

    auto all_bound_arguments = bound_arguments();
    all_bound_arguments.extend(move(arguments));

    return heap().allocate<BoundFunction>(global_object(), global_object(), target_function, bound_this_object, move(all_bound_arguments), computed_length, constructor_prototype);
}

void FunctionObject::add_field(StringOrSymbol property_key, FunctionObject* initializer)
{
    m_fields.empend(property_key, initializer);
}

// 7.3.31 DefineField ( receiver, fieldRecord ), https://tc39.es/ecma262/#sec-definefield
void FunctionObject::InstanceField::define_field(VM& vm, Object& receiver) const
{
    Value init_value = js_undefined();
    if (initializer) {
        init_value = vm.call(*initializer, receiver.value_of());
        if (vm.exception())
            return;
    }
    receiver.create_data_property_or_throw(name, init_value);
}

void FunctionObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(m_home_object);
    visitor.visit(m_bound_this);

    for (auto argument : m_bound_arguments)
        visitor.visit(argument);

    for (auto& field : m_fields) {
        field.name.visit_edges(visitor);
        visitor.visit(field.initializer);
    }
}

}
