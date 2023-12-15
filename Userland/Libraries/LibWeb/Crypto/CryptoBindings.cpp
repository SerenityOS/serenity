/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Crypto/CryptoBindings.h>

namespace Web::Bindings {

JS_DEFINE_ALLOCATOR(KeyAlgorithm);

JS::NonnullGCPtr<KeyAlgorithm> KeyAlgorithm::create(JS::Realm& realm)
{
    return realm.heap().allocate<KeyAlgorithm>(realm, realm);
}

KeyAlgorithm::KeyAlgorithm(JS::Realm& realm)
    : Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().object_prototype())
{
}

void KeyAlgorithm::initialize(JS::Realm& realm)
{
    define_native_accessor(realm, "name", name_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    Base::initialize(realm);
}

static JS::ThrowCompletionOr<KeyAlgorithm*> impl_from(JS::VM& vm)
{
    auto this_value = vm.this_value();
    JS::Object* this_object = nullptr;
    if (this_value.is_nullish())
        this_object = &vm.current_realm()->global_object();
    else
        this_object = TRY(this_value.to_object(vm));

    if (!is<KeyAlgorithm>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "KeyAlgorithm");
    return static_cast<KeyAlgorithm*>(this_object);
}

JS_DEFINE_NATIVE_FUNCTION(KeyAlgorithm::name_getter)
{
    auto* impl = TRY(impl_from(vm));
    auto name = TRY(throw_dom_exception_if_needed(vm, [&] { return impl->name(); }));
    return JS::PrimitiveString::create(vm, name);
}

}
