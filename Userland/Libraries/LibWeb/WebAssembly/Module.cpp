/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/ModulePrototype.h>
#include <LibWeb/WebAssembly/Module.h>
#include <LibWeb/WebAssembly/WebAssembly.h>

namespace Web::WebAssembly {

WebIDL::ExceptionOr<JS::NonnullGCPtr<Module>> Module::construct_impl(JS::Realm& realm, JS::Handle<JS::Object>& bytes)
{
    auto& vm = realm.vm();

    auto index = TRY(Detail::parse_module(vm, bytes.cell()));
    return vm.heap().allocate<Module>(realm, realm, index);
}

Module::Module(JS::Realm& realm, size_t index)
    : Bindings::PlatformObject(realm)
    , m_index(index)
{
}

void Module::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ModulePrototype>(realm, "WebAssembly.Module"sv));
}

Wasm::Module const& Module::module() const
{
    return Detail::s_compiled_modules.at(index())->module;
}

}
