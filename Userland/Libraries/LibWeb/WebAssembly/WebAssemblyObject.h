/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebAssembly/WebAssemblyObjectPrototype.h>

namespace Web::Bindings {

class WebAssemblyObject final : public JS::Object {
    JS_OBJECT(WebAssemblyObject, JS::Object);

public:
    explicit WebAssemblyObject(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyObject() override = default;

    struct CompiledWebAssemblyModule {
        explicit CompiledWebAssemblyModule(Wasm::Module&& module)
            : module(move(module))
        {
        }

        Wasm::Module module;
    };

    // FIXME: These should just be members of the module (instance) object,
    //        but the module needs to stick around while its instance is alive
    //        so ideally this would be a refcounted object, shared between
    //        WebAssemblyModuleObject's and WebAssemblyInstantiatedModuleObject's.
    static NonnullOwnPtrVector<CompiledWebAssemblyModule> s_compiled_modules;
    static NonnullOwnPtrVector<Wasm::ModuleInstance> s_instantiated_modules;

    static Wasm::AbstractMachine s_abstract_machine;

private:
    JS_DECLARE_NATIVE_FUNCTION(validate);
    JS_DECLARE_NATIVE_FUNCTION(compile);
    JS_DECLARE_NATIVE_FUNCTION(instantiate);
};

class WebAssemblyModuleObject final : public JS::Object {
    JS_OBJECT(WebAssemblyModuleObject, JS::Object);

public:
    explicit WebAssemblyModuleObject(JS::GlobalObject&, size_t index);
    virtual ~WebAssemblyModuleObject() override = default;

    size_t index() const { return m_index; }
    const Wasm::Module& module() const { return WebAssemblyObject::s_compiled_modules.at(m_index).module; }

private:
    size_t m_index { 0 };
};

class WebAssemblyInstanceObject final : public JS::Object {
    JS_OBJECT(WebAssemblyInstanceObject, JS::Object);

public:
    explicit WebAssemblyInstanceObject(JS::GlobalObject&, size_t index);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyInstanceObject() override = default;

    size_t index() const { return m_index; }
    Wasm::ModuleInstance& instance() const { return WebAssemblyObject::s_instantiated_modules.at(m_index); }

    void visit_edges(Cell::Visitor&) override;

    friend class WebAssemblyInstancePrototype;

private:
    size_t m_index { 0 };
    JS::Object* m_exports_object { nullptr };
};

class WebAssemblyMemoryObject final : public JS::Object {
    JS_OBJECT(WebAssemblyModuleObject, JS::Object);

public:
    explicit WebAssemblyMemoryObject(JS::GlobalObject&, Wasm::MemoryAddress);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~WebAssemblyMemoryObject() override = default;

    auto address() const { return m_address; }

private:
    JS_DECLARE_NATIVE_FUNCTION(grow);
    JS_DECLARE_NATIVE_GETTER(buffer);

    Wasm::MemoryAddress m_address;
};

}
