/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/XHR/XMLHttpRequestEventTarget.h>

namespace Web::XHR {

class XMLHttpRequest final
    : public RefCounted<XMLHttpRequest>
    , public Weakable<XMLHttpRequest>
    , public XMLHttpRequestEventTarget {
public:
    enum class ReadyState : u16 {
        Unsent = 0,
        Opened = 1,
        HeadersReceived = 2,
        Loading = 3,
        Done = 4,
    };

    using WrapperType = Bindings::XMLHttpRequestWrapper;

    static NonnullRefPtr<XMLHttpRequest> create(DOM::Window& window)
    {
        return adopt(*new XMLHttpRequest(window));
    }
    static NonnullRefPtr<XMLHttpRequest> create_with_global_object(Bindings::WindowObject& window)
    {
        return XMLHttpRequest::create(window.impl());
    }

    virtual ~XMLHttpRequest() override;

    using RefCounted::ref;
    using RefCounted::unref;

    ReadyState ready_state() const { return m_ready_state; };
    String response_text() const;

    DOM::ExceptionOr<void> open(const String& method, const String& url);
    void send();

    DOM::ExceptionOr<void> set_request_header(const String& header, const String& value);

private:
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual bool dispatch_event(NonnullRefPtr<DOM::Event>) override;
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void set_ready_state(ReadyState);
    void fire_progress_event(const String&, u64, u64);

    explicit XMLHttpRequest(DOM::Window&);

    NonnullRefPtr<DOM::Window> m_window;

    ReadyState m_ready_state { ReadyState::Unsent };
    bool m_send { false };

    String m_method;
    URL m_url;

    HashMap<String, String, CaseInsensitiveStringTraits> m_request_headers;

    bool m_synchronous { false };
    bool m_upload_complete { false };
    bool m_upload_listener { false };
    bool m_timed_out { false };

    ByteBuffer m_response_object;
};

}
