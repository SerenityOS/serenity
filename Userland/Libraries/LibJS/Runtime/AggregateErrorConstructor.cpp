/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/AggregateErrorConstructor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

AggregateErrorConstructor::AggregateErrorConstructor(Realm& realm)
    : NativeFunction(static_cast<Object&>(*realm.intrinsics().error_constructor()))
{
}

void AggregateErrorConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 20.5.7.2.1 AggregateError.prototype, https://tc39.es/ecma262/#sec-aggregate-error.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().aggregate_error_prototype(), 0);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 20.5.7.1.1 AggregateError ( errors, message ), https://tc39.es/ecma262/#sec-aggregate-error
ThrowCompletionOr<Value> AggregateErrorConstructor::call()
{
    return TRY(construct(*this));
}

// 20.5.7.1.1 AggregateError ( errors, message ), https://tc39.es/ecma262/#sec-aggregate-error
ThrowCompletionOr<Object*> AggregateErrorConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    auto* aggregate_error = TRY(ordinary_create_from_constructor<AggregateError>(vm, new_target, &Intrinsics::aggregate_error_prototype));

    if (!vm.argument(1).is_undefined()) {
        auto message = TRY(vm.argument(1).to_string(vm));
        aggregate_error->create_non_enumerable_data_property_or_throw(vm.names.message, js_string(vm, message));
    }

    TRY(aggregate_error->install_error_cause(vm.argument(2)));

    auto errors_list = TRY(iterable_to_list(vm, vm.argument(0)));

    MUST(aggregate_error->define_property_or_throw(vm.names.errors, { .value = Array::create_from(realm, errors_list), .writable = true, .enumerable = false, .configurable = true }));

    return aggregate_error;
}

}
