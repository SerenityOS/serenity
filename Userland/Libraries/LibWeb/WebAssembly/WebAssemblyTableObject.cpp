/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyTablePrototype.h"
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/WebAssemblyTableObject.h>

namespace Web::Bindings {

WebAssemblyTableObject::WebAssemblyTableObject(JS::Realm& realm, Wasm::TableAddress address)
    : Object(Bindings::ensure_web_prototype<WebAssemblyTablePrototype>(realm, "WebAssemblyTablePrototype"))
    , m_address(address)
{
}

}
