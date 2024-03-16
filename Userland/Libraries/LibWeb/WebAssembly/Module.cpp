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
#include <LibWeb/WebIDL/Buffers.h>

namespace Web::WebAssembly {

JS_DEFINE_ALLOCATOR(Module);

WebIDL::ExceptionOr<JS::NonnullGCPtr<Module>> Module::construct_impl(JS::Realm& realm, JS::Handle<WebIDL::BufferSource>& bytes)
{
    auto& vm = realm.vm();

    auto index = TRY(Detail::parse_module(vm, bytes->raw_object()));
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
    WEB_SET_PROTOTYPE_FOR_INTERFACE_WITH_CUSTOM_NAME(Module, WebAssembly.Module);
}

Wasm::Module const& Module::module() const
{
    return Detail::s_compiled_modules.at(index())->module;
}

}
