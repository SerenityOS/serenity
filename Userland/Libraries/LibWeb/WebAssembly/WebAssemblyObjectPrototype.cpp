/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/WebAssembly/WebAssemblyObject.h>
#include <LibWeb/WebAssembly/WebAssemblyObjectPrototype.h>

namespace Web::Bindings {

void WebAssemblyInstancePrototype::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_property("exports", exports_getter, nullptr);
}

JS_DEFINE_NATIVE_GETTER(WebAssemblyInstancePrototype::exports_getter)
{
    auto this_value = vm.this_value(global_object);
    auto this_object = this_value.to_object(global_object);
    if (vm.exception())
        return {};
    if (!is<WebAssemblyInstanceObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAn, "WebAssemblyInstance");
        return {};
    }
    auto object = static_cast<WebAssemblyInstanceObject*>(this_object);
    return object->m_exports_object;
}

}
