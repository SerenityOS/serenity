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

    // 8. Set values[index] to x.
    m_values.values()[m_index] = vm.argument(0);

    // 9. Set remainingElementsCount.[[Value]] to remainingElementsCount.[[Value]] - 1.
    // 10. If remainingElementsCount.[[Value]] is 0, then
    if (--m_remaining_elements.value == 0) {
        // a. Let valuesArray be ! CreateArrayFromList(values).
        auto* values_array = Array::create_from(global_object, m_values.values());

        // b. Return ? Call(promiseCapability.[[Resolve]], undefined, « valuesArray »).
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    // 11. Return undefined.
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

    // 9. Let obj be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* object = Object::create(global_object, global_object.object_prototype());

    // 10. Perform ! CreateDataPropertyOrThrow(obj, "status", "fulfilled").
    MUST(object->create_data_property_or_throw(vm.names.status, js_string(vm, "fulfilled"sv)));

    // 11. Perform ! CreateDataPropertyOrThrow(obj, "value", x).
    MUST(object->create_data_property_or_throw(vm.names.value, vm.argument(0)));

    // 12. Set values[index] to obj.
    m_values.values()[m_index] = object;

    // 13. Set remainingElementsCount.[[Value]] to remainingElementsCount.[[Value]] - 1.
    // 14. If remainingElementsCount.[[Value]] is 0, then
    if (--m_remaining_elements.value == 0) {
        // a. Let valuesArray be ! CreateArrayFromList(values).
        auto* values_array = Array::create_from(global_object, m_values.values());

        // b. Return ? Call(promiseCapability.[[Resolve]], undefined, « valuesArray »).
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    // 15. Return undefined.
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

    // 9. Let obj be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* object = Object::create(global_object, global_object.object_prototype());

    // 10. Perform ! CreateDataPropertyOrThrow(obj, "status", "rejected").
    MUST(object->create_data_property_or_throw(vm.names.status, js_string(vm, "rejected"sv)));

    // 11. Perform ! CreateDataPropertyOrThrow(obj, "reason", x).
    MUST(object->create_data_property_or_throw(vm.names.reason, vm.argument(0)));

    // 12. Set values[index] to obj.
    m_values.values()[m_index] = object;

    // 13. Set remainingElementsCount.[[Value]] to remainingElementsCount.[[Value]] - 1.
    // 14. If remainingElementsCount.[[Value]] is 0, then
    if (--m_remaining_elements.value == 0) {
        // a. Let valuesArray be ! CreateArrayFromList(values).
        auto values_array = Array::create_from(global_object, m_values.values());

        // b. Return ? Call(promiseCapability.[[Resolve]], undefined, « valuesArray »).
        return TRY_OR_DISCARD(vm.call(*m_capability.resolve, js_undefined(), values_array));
    }

    // 15. Return undefined.
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

    // 8. Set errors[index] to x.
    m_values.values()[m_index] = vm.argument(0);

    // 9. Set remainingElementsCount.[[Value]] to remainingElementsCount.[[Value]] - 1.
    // 10. If remainingElementsCount.[[Value]] is 0, then
    if (--m_remaining_elements.value == 0) {
        // a. Let error be a newly created AggregateError object.
        auto* error = AggregateError::create(global_object);

        // b. Perform ! DefinePropertyOrThrow(error, "errors", PropertyDescriptor { [[Configurable]]: true, [[Enumerable]]: false, [[Writable]]: true, [[Value]]: ! CreateArrayFromList(errors) }).
        auto errors_array = Array::create_from(global_object, m_values.values());
        MUST(error->define_property_or_throw(vm.names.errors, { .value = errors_array, .writable = true, .enumerable = false, .configurable = true }));

        // c. Return ? Call(promiseCapability.[[Reject]], undefined, « error »).
        return TRY_OR_DISCARD(vm.call(*m_capability.reject, js_undefined(), error));
    }

    return js_undefined();
}

}
