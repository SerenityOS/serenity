/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWasm/Types.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/Memory.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::WebAssembly {

WebIDL::ExceptionOr<JS::NonnullGCPtr<Memory>> Memory::construct_impl(JS::Realm& realm, MemoryDescriptor& descriptor)
{
    auto& vm = realm.vm();

    Wasm::Limits limits { descriptor.initial, move(descriptor.maximum) };
    Wasm::MemoryType memory_type { move(limits) };

    auto address = Bindings::WebAssemblyObject::s_abstract_machine.store().allocate(memory_type);
    if (!address.has_value())
        return vm.throw_completion<JS::TypeError>("Wasm Memory allocation failed"sv);

    return MUST_OR_THROW_OOM(vm.heap().allocate<Memory>(realm, realm, *address));
}

Memory::Memory(JS::Realm& realm, Wasm::MemoryAddress address)
    : Bindings::PlatformObject(realm)
    , m_address(address)
{
}

JS::ThrowCompletionOr<void> Memory::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MemoryPrototype>(realm, "WebAssembly.Memory"sv));

    return {};
}

// https://webassembly.github.io/spec/js-api/#dom-memory-grow
WebIDL::ExceptionOr<u32> Memory::grow(u32 delta)
{
    auto& vm = this->vm();

    auto* memory = Bindings::WebAssemblyObject::s_abstract_machine.store().get(address());
    if (!memory)
        return vm.throw_completion<JS::RangeError>("Could not find the memory instance to grow"sv);

    auto previous_size = memory->size() / Wasm::Constants::page_size;
    if (!memory->grow(delta * Wasm::Constants::page_size))
        return vm.throw_completion<JS::RangeError>("Memory.grow() grows past the stated limit of the memory instance"sv);

    return previous_size;
}

// https://webassembly.github.io/spec/js-api/#dom-memory-buffer
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> Memory::buffer() const
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    auto* memory = Bindings::WebAssemblyObject::s_abstract_machine.store().get(address());
    if (!memory)
        return vm.throw_completion<JS::RangeError>("Could not find the memory instance"sv);

    auto array_buffer = JS::ArrayBuffer::create(realm, &memory->data());
    array_buffer->set_detach_key(MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm, "WebAssembly.Memory"sv)));

    return array_buffer;
}

}
