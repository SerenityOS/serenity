/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/PromiseResolvingElementFunctions.h>

namespace JS {

PromiseResolvingElementFunction::PromiseResolvingElementFunction(size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : NativeFunction(prototype)
    , m_index(index)
    , m_values(values)
    , m_capability(move(capability))
    , m_remaining_elements(remaining_elements)
{
}

void PromiseResolvingElementFunction::initialize(GlobalObject& global_object)
{
    Base::initialize(global_object);
    define_direct_property(vm().names.length, Value(1), Attribute::Configurable);
}

ThrowCompletionOr<Value> PromiseResolvingElementFunction::call()
{
    if (m_already_called)
        return js_undefined();
    m_already_called = true;

    return resolve_element();
}

void PromiseResolvingElementFunction::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_values);
    visitor.visit(m_capability.promise);
    visitor.visit(m_capability.resolve);
    visitor.visit(m_capability.reject);
    visitor.visit(&m_remaining_elements);
}

PromiseAllResolveElementFunction* PromiseAllResolveElementFunction::create(GlobalObject& global_object, size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements)
{
    return global_object.heap().allocate<PromiseAllResolveElementFunction>(global_object, index, values, capability, remaining_elements, *global_object.function_prototype());
}

PromiseAllResolveElementFunction::PromiseAllResolveElementFunction(size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : PromiseResolvingElementFunction(index, values, move(capability), remaining_elements, prototype)
{
}

Value PromiseAllResolveElementFunction::resolve_element()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    m_values.values()[m_index] = vm.argument(0);

    if (--m_remaining_elements.value == 0) {
        auto values_array = Array::create_from(global_object, m_values.values());
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    return js_undefined();
}

PromiseAllSettledResolveElementFunction* PromiseAllSettledResolveElementFunction::create(GlobalObject& global_object, size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements)
{
    return global_object.heap().allocate<PromiseAllSettledResolveElementFunction>(global_object, index, values, capability, remaining_elements, *global_object.function_prototype());
}

PromiseAllSettledResolveElementFunction::PromiseAllSettledResolveElementFunction(size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : PromiseResolvingElementFunction(index, values, move(capability), remaining_elements, prototype)
{
}

Value PromiseAllSettledResolveElementFunction::resolve_element()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* object = Object::create(global_object, global_object.object_prototype());
    MUST(object->create_data_property_or_throw(vm.names.status, js_string(vm, "fulfilled"sv)));
    MUST(object->create_data_property_or_throw(vm.names.value, vm.argument(0)));

    m_values.values()[m_index] = object;

    if (--m_remaining_elements.value == 0) {
        auto values_array = Array::create_from(global_object, m_values.values());
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    return js_undefined();
}

PromiseAllSettledRejectElementFunction* PromiseAllSettledRejectElementFunction::create(GlobalObject& global_object, size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements)
{
    return global_object.heap().allocate<PromiseAllSettledRejectElementFunction>(global_object, index, values, capability, remaining_elements, *global_object.function_prototype());
}

PromiseAllSettledRejectElementFunction::PromiseAllSettledRejectElementFunction(size_t index, PromiseValueList& values, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : PromiseResolvingElementFunction(index, values, move(capability), remaining_elements, prototype)
{
}

Value PromiseAllSettledRejectElementFunction::resolve_element()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* object = Object::create(global_object, global_object.object_prototype());
    MUST(object->create_data_property_or_throw(vm.names.status, js_string(vm, "rejected"sv)));
    MUST(object->create_data_property_or_throw(vm.names.reason, vm.argument(0)));

    m_values.values()[m_index] = object;

    if (--m_remaining_elements.value == 0) {
        auto values_array = Array::create_from(global_object, m_values.values());
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    return js_undefined();
}

PromiseAnyRejectElementFunction* PromiseAnyRejectElementFunction::create(GlobalObject& global_object, size_t index, PromiseValueList& errors, PromiseCapability capability, RemainingElements& remaining_elements)
{
    return global_object.heap().allocate<PromiseAnyRejectElementFunction>(global_object, index, errors, capability, remaining_elements, *global_object.function_prototype());
}

PromiseAnyRejectElementFunction::PromiseAnyRejectElementFunction(size_t index, PromiseValueList& errors, PromiseCapability capability, RemainingElements& remaining_elements, Object& prototype)
    : PromiseResolvingElementFunction(index, errors, move(capability), remaining_elements, prototype)
{
}

Value PromiseAnyRejectElementFunction::resolve_element()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    m_values.values()[m_index] = vm.argument(0);

    if (--m_remaining_elements.value == 0) {
        auto errors_array = Array::create_from(global_object, m_values.values());

        auto* error = AggregateError::create(global_object);
        MUST(error->define_property_or_throw(vm.names.errors, { .value = errors_array, .writable = true, .enumerable = false, .configurable = true }));

        return TRY_OR_DISCARD(vm.call(*m_capability.reject, js_undefined(), error));
    }

    return js_undefined();
}

}
