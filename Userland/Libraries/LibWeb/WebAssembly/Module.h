/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Heap/Handle.h>
#include <LibWasm/Types.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::WebAssembly {

class Module : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Module, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Module>> construct_impl(JS::Realm&, JS::Handle<JS::Object>& bytes);

    size_t index() const { return m_index; }
    Wasm::Module const& module() const;

private:
    Module(JS::Realm&, size_t index);

    virtual void initialize(JS::Realm&) override;

    size_t m_index { 0 };
};

}
