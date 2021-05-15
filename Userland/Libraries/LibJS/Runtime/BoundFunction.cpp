/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BoundFunction::BoundFunction(GlobalObject& global_object, Function& target_function, Value bound_this, Vector<Value> arguments, i32 length, Object* constructor_prototype)
    : Function::Function(*global_object.function_prototype(), bound_this, move(arguments))
    , m_target_function(&target_function)
    , m_constructor_prototype(constructor_prototype)
    , m_name(String::formatted("bound {}", target_function.name()))
    , m_length(length)
{
}

void BoundFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Function::initialize(global_object);
    define_property(vm.names.length, Value(m_length), Attribute::Configurable);
}

BoundFunction::~BoundFunction()
{
}

Value BoundFunction::call()
{
    return m_target_function->call();
}

Value BoundFunction::construct(Function& new_target)
{
    if (auto this_value = vm().this_value(global_object()); m_constructor_prototype && this_value.is_object()) {
        this_value.as_object().set_prototype(m_constructor_prototype);
        if (vm().exception())
            return {};
    }
    return m_target_function->construct(new_target);
}

LexicalEnvironment* BoundFunction::create_environment()
{
    return m_target_function->create_environment();
}

void BoundFunction::visit_edges(Visitor& visitor)
{
    Function::visit_edges(visitor);
    visitor.visit(m_target_function);
    visitor.visit(m_constructor_prototype);
}

}
