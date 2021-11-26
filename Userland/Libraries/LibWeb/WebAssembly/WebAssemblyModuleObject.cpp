/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyModulePrototype.h"
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>

namespace Web::Bindings {

WebAssemblyModuleObject::WebAssemblyModuleObject(JS::GlobalObject& global_object, size_t index)
    : Object(static_cast<WindowObject&>(global_object).ensure_web_prototype<WebAssemblyModulePrototype>("WebAssemblyModulePrototype"))
    , m_index(index)
{
}

}
