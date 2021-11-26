/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

class WebAssemblyModuleObject final : public JS::Object {
    JS_OBJECT(WebAssemblyModuleObject, Object);

public:
    explicit WebAssemblyModuleObject(JS::GlobalObject&, size_t index);
    virtual ~WebAssemblyModuleObject() override = default;

    size_t index() const { return m_index; }
    const Wasm::Module& module() const { return WebAssemblyObject::s_compiled_modules.at(m_index).module; }

private:
    size_t m_index { 0 };
};

}
