/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Fetch/Fetching/PendingResponse.h>
#include <LibWeb/Platform/EventLoopPlugin.h>

namespace Web::Fetch::Fetching {

JS::NonnullGCPtr<PendingResponse> PendingResponse::create(JS::VM& vm)
{
    return { *vm.heap().allocate_without_realm<PendingResponse>() };
}

JS::NonnullGCPtr<PendingResponse> PendingResponse::create(JS::VM& vm, JS::NonnullGCPtr<Infrastructure::Response> response)
{
    return { *vm.heap().allocate_without_realm<PendingResponse>(response) };
}

PendingResponse::PendingResponse(JS::NonnullGCPtr<Infrastructure::Response> response)
    : m_response(response)
{
}

void PendingResponse::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_response);
}

void PendingResponse::when_loaded(Callback callback)
{
    VERIFY(!m_callback);
    m_callback = move(callback);
    if (m_response)
        run_callback();
}

void PendingResponse::resolve(JS::NonnullGCPtr<Infrastructure::Response> response)
{
    VERIFY(!m_response);
    m_response = response;
    if (m_callback)
        run_callback();
}

void PendingResponse::run_callback() const
{
    VERIFY(m_callback);
    VERIFY(m_response);
    Platform::EventLoopPlugin::the().deferred_invoke([strong_this = JS::make_handle(const_cast<PendingResponse&>(*this))]() {
        strong_this->m_callback(*strong_this->m_response);
    });
}

}
