/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyTablePrototype.h"
#include <LibWeb/WebAssembly/WebAssemblyTableObject.h>

namespace Web::Bindings {

WebAssemblyTableObject::WebAssemblyTableObject(JS::GlobalObject& global_object, Wasm::TableAddress address)
    : Object(static_cast<WindowObject&>(global_object).ensure_web_prototype<WebAssemblyTablePrototype>("WebAssemblyTablePrototype"))
    , m_address(address)
{
}

}
