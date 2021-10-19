/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyInstanceObject.h"
#include <AK/TypeCasts.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>

namespace Web::Bindings {

void WebAssemblyInstancePrototype::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_accessor("exports", exports_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);
}

JS_DEFINE_OLD_NATIVE_FUNCTION(WebAssemblyInstancePrototype::exports_getter)
{
    auto this_value = vm.this_value(global_object);
    auto* this_object = TRY_OR_DISCARD(this_value.to_object(global_object));
    if (!is<WebAssemblyInstanceObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "WebAssembly.Instance");
        return {};
    }
    auto object = static_cast<WebAssemblyInstanceObject*>(this_object);
    return object->m_exports_object;
}

}
