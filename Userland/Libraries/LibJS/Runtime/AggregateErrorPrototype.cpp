/*
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateErrorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>

namespace JS {

AggregateErrorPrototype::AggregateErrorPrototype(Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().error_prototype())
{
}

ThrowCompletionOr<void> AggregateErrorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.name, MUST_OR_THROW_OOM(PrimitiveString::create(vm, "AggregateError"sv)), attr);
    define_direct_property(vm.names.message, PrimitiveString::create(vm, String {}), attr);

    return {};
}

}
