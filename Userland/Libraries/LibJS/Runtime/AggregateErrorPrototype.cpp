/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateErrorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>

namespace JS {

AggregateErrorPrototype::AggregateErrorPrototype(Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, *realm.intrinsics().error_prototype())
{
}

void AggregateErrorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.name, PrimitiveString::create(vm, "AggregateError"), attr);
    define_direct_property(vm.names.message, PrimitiveString::create(vm, ""), attr);
}

}
