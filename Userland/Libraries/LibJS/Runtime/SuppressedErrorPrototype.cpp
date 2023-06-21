/*
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/SuppressedErrorPrototype.h>

namespace JS {

SuppressedErrorPrototype::SuppressedErrorPrototype(Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().error_prototype())
{
}

ThrowCompletionOr<void> SuppressedErrorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_direct_property(vm.names.name, MUST_OR_THROW_OOM(PrimitiveString::create(vm, "SuppressedError"sv)), attr);
    define_direct_property(vm.names.message, PrimitiveString::create(vm, String {}), attr);

    return {};
}

}
