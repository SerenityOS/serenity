/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Intrinsics.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/IteratorConstructor.h>
#include <LibJS/Runtime/IteratorPrototype.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 3.1.1.1 The Iterator Constructor, https://tc39.es/proposal-iterator-helpers/#sec-iterator-constructor
IteratorConstructor::IteratorConstructor(Realm& realm)
    : Base(realm.vm().names.Iterator.as_string(), realm.intrinsics().function_prototype())
{
}

ThrowCompletionOr<void> IteratorConstructor::initialize(Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));

    auto& vm = this->vm();

    // 3.1.1.2.1 Iterator.prototype, https://tc39.es/proposal-iterator-helpers/#sec-iterator.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().iterator_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    return {};
}

// 3.1.1.1.1 Iterator ( ), https://tc39.es/proposal-iterator-helpers/#sec-iterator
ThrowCompletionOr<Value> IteratorConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined or the active function object, throw a TypeError exception.
    return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, "Iterator");
}

// 3.1.1.1.1 Iterator ( ), https://tc39.es/proposal-iterator-helpers/#sec-iterator
ThrowCompletionOr<NonnullGCPtr<Object>> IteratorConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined or the active function object, throw a TypeError exception.
    if (&new_target == this)
        return vm.throw_completion<TypeError>(ErrorType::ClassIsAbstract, "Iterator");

    // 2. Return ? OrdinaryCreateFromConstructor(NewTarget, "%Iterator.prototype%").
    return TRY(ordinary_create_from_constructor<Iterator>(vm, new_target, &Intrinsics::iterator_prototype));
}

}
