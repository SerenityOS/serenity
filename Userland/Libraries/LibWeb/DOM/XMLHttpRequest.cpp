/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/DOM/XMLHttpRequest.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Origin.h>

namespace Web {

XMLHttpRequest::XMLHttpRequest(DOM::Window& window)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.document()))
    , m_window(window)
{
}

XMLHttpRequest::~XMLHttpRequest()
{
}

void XMLHttpRequest::set_ready_state(ReadyState ready_state)
{
    // FIXME: call onreadystatechange once we have that
    m_ready_state = ready_state;
}

String XMLHttpRequest::response_text() const
{
    if (m_response.is_null())
        return {};
    return String::copy(m_response);
}

void XMLHttpRequest::set_request_header(const String& header, const String& value)
{
    m_request_headers.set(header, value);
}

void XMLHttpRequest::open(const String& method, const String& url)
{
    m_method = method;
    m_url = url;
    m_request_headers.clear();
    set_ready_state(ReadyState::Opened);
}

void XMLHttpRequest::send()
{
    URL request_url = m_window->document().complete_url(m_url);
    dbgln("XHR send from {} to {}", m_window->document().url(), request_url);

    // TODO: Add support for preflight requests to support CORS requests
    Origin request_url_origin = Origin(request_url.protocol(), request_url.host(), request_url.port());

    if (!m_window->document().origin().is_same(request_url_origin)) {
        dbgln("XHR failed to load: Same-Origin Policy violation: {} may not load {}", m_window->document().url(), request_url);
        auto weak_this = make_weak_ptr();
        if (!weak_this)
            return;
        const_cast<XMLHttpRequest&>(*weak_this).set_ready_state(ReadyState::Done);
        const_cast<XMLHttpRequest&>(*weak_this).dispatch_event(DOM::Event::create(HTML::EventNames::error));
        return;
    }

    LoadRequest request;
    request.set_url(m_window->document().complete_url(m_url));
    for (auto& it : m_request_headers)
        request.set_header(it.key, it.value);

    // FIXME: in order to properly set ReadyState::HeadersReceived and ReadyState::Loading,
    // we need to make ResourceLoader give us more detailed updates than just "done" and "error".
    ResourceLoader::the().load(
        request,
        [weak_this = make_weak_ptr()](auto data, auto&) {
            if (!weak_this)
                return;
            const_cast<XMLHttpRequest&>(*weak_this).m_response = ByteBuffer::copy(data);
            const_cast<XMLHttpRequest&>(*weak_this).set_ready_state(ReadyState::Done);
            const_cast<XMLHttpRequest&>(*weak_this).dispatch_event(DOM::Event::create(HTML::EventNames::load));
        },
        [weak_this = make_weak_ptr()](auto& error) {
            if (!weak_this)
                return;
            dbgln("XHR failed to load: {}", error);
            const_cast<XMLHttpRequest&>(*weak_this).set_ready_state(ReadyState::Done);
            const_cast<XMLHttpRequest&>(*weak_this).dispatch_event(DOM::Event::create(HTML::EventNames::error));
        });
}

bool XMLHttpRequest::dispatch_event(NonnullRefPtr<DOM::Event> event)
{
    return DOM::EventDispatcher::dispatch(*this, move(event));
}

JS::Object* XMLHttpRequest::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

}
