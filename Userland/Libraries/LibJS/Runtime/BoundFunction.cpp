/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BoundFunction::BoundFunction(GlobalObject& global_object, FunctionObject& bound_target_function, Value bound_this, Vector<Value> bound_arguments, i32 length, Object* constructor_prototype)
    : FunctionObject(*global_object.function_prototype())
    , m_bound_target_function(&bound_target_function)
    , m_bound_this(bound_this)
    , m_bound_arguments(move(bound_arguments))
    , m_constructor_prototype(constructor_prototype)
    , m_name(String::formatted("bound {}", bound_target_function.name()))
    , m_length(length)
{
}

void BoundFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Base::initialize(global_object);
    define_direct_property(vm.names.length, Value(m_length), Attribute::Configurable);
}

BoundFunction::~BoundFunction()
{
}

Value BoundFunction::call()
{
    return m_bound_target_function->call();
}

Value BoundFunction::construct(FunctionObject& new_target)
{
    if (auto this_value = vm().this_value(global_object()); m_constructor_prototype && this_value.is_object())
        TRY_OR_DISCARD(this_value.as_object().internal_set_prototype_of(m_constructor_prototype));
    return m_bound_target_function->construct(new_target);
}

FunctionEnvironment* BoundFunction::new_function_environment(Object* new_target)
{
    return m_bound_target_function->new_function_environment(new_target);
}

void BoundFunction::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(m_bound_target_function);
    visitor.visit(m_bound_this);
    for (auto argument : m_bound_arguments)
        visitor.visit(argument);

    visitor.visit(m_constructor_prototype);
}

}
