/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PromiseAllResolveElementFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

PromiseAllResolveElementFunction* PromiseAllResolveElementFunction::create(GlobalObject& global_object, size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements)
{
    return global_object.heap().allocate<PromiseAllResolveElementFunction>(global_object, index, values, capability, remaining_elements, *global_object.function_prototype());
}

PromiseAllResolveElementFunction::PromiseAllResolveElementFunction(size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : NativeFunction(prototype)
    , m_index(index)
    , m_values(values)
    , m_capability(move(capability))
    , m_remaining_elements(remaining_elements)
{
}

void PromiseAllResolveElementFunction::initialize(GlobalObject& global_object)
{
    Base::initialize(global_object);
    define_direct_property(vm().names.length, Value(1), Attribute::Configurable);
}

Value PromiseAllResolveElementFunction::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    if (m_already_called)
        return js_undefined();
    m_already_called = true;

    m_values.values[m_index] = vm.argument(0);

    if (--m_remaining_elements.value == 0) {
        auto values_array = Array::create_from(global_object, m_values.values);
        return vm.call(*m_capability.resolve, js_undefined(), values_array);
    }

    return js_undefined();
}

void PromiseAllResolveElementFunction::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_values);
    visitor.visit(m_capability.promise);
    visitor.visit(m_capability.resolve);
    visitor.visit(m_capability.reject);
    visitor.visit(&m_remaining_elements);
}

}
