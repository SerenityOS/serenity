/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyInstanceObject.h"
#include <AK/TypeCasts.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>

namespace Web::Bindings {

JS::ThrowCompletionOr<void> WebAssemblyInstancePrototype::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Object::initialize(realm));
    define_native_accessor(realm, "exports", exports_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);

    return {};
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyInstancePrototype::exports_getter)
{
    auto this_value = vm.this_value();
    auto* this_object = TRY(this_value.to_object(vm));
    if (!is<WebAssemblyInstanceObject>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Instance");
    auto object = static_cast<WebAssemblyInstanceObject*>(this_object);
    return object->m_exports_object;
}

}
