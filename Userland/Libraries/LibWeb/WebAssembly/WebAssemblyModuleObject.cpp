/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyModulePrototype.h"
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>

namespace Web::Bindings {

WebAssemblyModuleObject::WebAssemblyModuleObject(JS::Realm& realm, size_t index)
    : Object(Bindings::ensure_web_prototype<WebAssemblyModulePrototype>(realm, "WebAssemblyModulePrototype"))
    , m_index(index)
{
}

}
