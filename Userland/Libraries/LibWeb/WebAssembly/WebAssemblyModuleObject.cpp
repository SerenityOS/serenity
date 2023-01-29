/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyModulePrototype.h"
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>

namespace Web::Bindings {

WebAssemblyModuleObject::WebAssemblyModuleObject(JS::Realm& realm, size_t index)
    : Object(ConstructWithPrototypeTag::Tag, Bindings::ensure_web_prototype<WebAssemblyModulePrototype>(realm, "WebAssembly.Module"))
    , m_index(index)
{
}

}
