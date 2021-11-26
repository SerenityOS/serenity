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

class WebAssemblyTableObject final : public JS::Object {
    JS_OBJECT(WebAssemblyTableObject, Object);

public:
    explicit WebAssemblyTableObject(JS::GlobalObject&, Wasm::TableAddress);
    virtual ~WebAssemblyTableObject() override = default;

    Wasm::TableAddress address() const { return m_address; }

private:
    Wasm::TableAddress m_address;
};

}
