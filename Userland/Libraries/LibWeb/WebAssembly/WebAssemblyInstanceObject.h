/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

class WebAssemblyInstanceObject final : public JS::Object {
    JS_OBJECT(WebAssemblyInstanceObject, Object);

public:
    explicit WebAssemblyInstanceObject(JS::GlobalObject&, size_t index);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyInstanceObject() override = default;

    size_t index() const { return m_index; }
    Wasm::ModuleInstance& instance() const { return WebAssemblyObject::s_instantiated_modules.at(m_index); }
    auto& cache() { return WebAssemblyObject::s_module_caches.at(m_index); }

    void visit_edges(Visitor&) override;

    friend class WebAssemblyInstancePrototype;

private:
    size_t m_index { 0 };
    Object* m_exports_object { nullptr };
};

}
