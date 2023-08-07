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
#include <LibWeb/WebAssembly/WebAssembly.h>

namespace Web::WebAssembly {

WebIDL::ExceptionOr<JS::NonnullGCPtr<Memory>> Memory::construct_impl(JS::Realm& realm, MemoryDescriptor& descriptor)
{
    auto& vm = realm.vm();

    Wasm::Limits limits { descriptor.initial, move(descriptor.maximum) };
    Wasm::MemoryType memory_type { move(limits) };

    auto address = Detail::s_abstract_machine.store().allocate(memory_type);
    if (!address.has_value())
        return vm.throw_completion<JS::TypeError>("Wasm Memory allocation failed"sv);

    auto memory_object = MUST_OR_THROW_OOM(vm.heap().allocate<Memory>(realm, realm, *address));
    Detail::s_abstract_machine.store().get(*address)->successful_grow_hook = [memory_object] {
        MUST(memory_object->reset_the_memory_buffer());
    };

    return memory_object;
}

Memory::Memory(JS::Realm& realm, Wasm::MemoryAddress address)
    : Bindings::PlatformObject(realm)
    , m_address(address)
{
}

void Memory::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MemoryPrototype>(realm, "WebAssembly.Memory"sv));
}

// https://webassembly.github.io/spec/js-api/#dom-memory-grow
WebIDL::ExceptionOr<u32> Memory::grow(u32 delta)
{
    auto& vm = this->vm();

    auto* memory = Detail::s_abstract_machine.store().get(address());
    if (!memory)
        return vm.throw_completion<JS::RangeError>("Could not find the memory instance to grow"sv);

    auto previous_size = memory->size() / Wasm::Constants::page_size;
    if (!memory->grow(delta * Wasm::Constants::page_size, Wasm::MemoryInstance::InhibitGrowCallback::Yes))
        return vm.throw_completion<JS::RangeError>("Memory.grow() grows past the stated limit of the memory instance"sv);

    TRY(reset_the_memory_buffer());

    return previous_size;
}

// https://webassembly.github.io/spec/js-api/#reset-the-memory-buffer
WebIDL::ExceptionOr<void> Memory::reset_the_memory_buffer()
{
    if (!m_buffer)
        return {};

    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    MUST(JS::detach_array_buffer(vm, *m_buffer, MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm, "WebAssembly.Memory"sv))));

    auto buffer = TRY(create_a_memory_buffer(vm, realm, m_address));
    m_buffer = buffer;

    return {};
}

// https://webassembly.github.io/spec/js-api/#dom-memory-buffer
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> Memory::buffer() const
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    if (!m_buffer)
        m_buffer = TRY(create_a_memory_buffer(vm, realm, m_address));

    return JS::NonnullGCPtr(*m_buffer);
}

// https://webassembly.github.io/spec/js-api/#create-a-memory-buffer
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> Memory::create_a_memory_buffer(JS::VM& vm, JS::Realm& realm, Wasm::MemoryAddress address)
{
    auto* memory = Detail::s_abstract_machine.store().get(address);
    if (!memory)
        return vm.throw_completion<JS::RangeError>("Could not find the memory instance"sv);

    auto array_buffer = JS::ArrayBuffer::create(realm, &memory->data());
    array_buffer->set_detach_key(MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm, "WebAssembly.Memory"sv)));

    return JS::NonnullGCPtr(*array_buffer);
}

}
