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

#pragma once

#include <AK/Weakable.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class WindowObject final
    : public JS::GlobalObject
    , public Weakable<WindowObject> {
public:
    explicit WindowObject(DOM::Window&);
    virtual void initialize() override;
    virtual ~WindowObject() override;

    DOM::Window& impl() { return *m_impl; }
    const DOM::Window& impl() const { return *m_impl; }

    Origin origin() const;

    XMLHttpRequestPrototype* xhr_prototype() { return m_xhr_prototype; }
    XMLHttpRequestConstructor* xhr_constructor() { return m_xhr_constructor; }

private:
    virtual const char* class_name() const override { return "WindowObject"; }
    virtual void visit_children(Visitor&) override;

    JS_DECLARE_NATIVE_GETTER(document_getter);
    JS_DECLARE_NATIVE_SETTER(document_setter);

    JS_DECLARE_NATIVE_GETTER(performance_getter);

    JS_DECLARE_NATIVE_FUNCTION(alert);
    JS_DECLARE_NATIVE_FUNCTION(confirm);
    JS_DECLARE_NATIVE_FUNCTION(set_interval);
    JS_DECLARE_NATIVE_FUNCTION(set_timeout);
    JS_DECLARE_NATIVE_FUNCTION(clear_interval);
    JS_DECLARE_NATIVE_FUNCTION(clear_timeout);
    JS_DECLARE_NATIVE_FUNCTION(request_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(cancel_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(atob);
    JS_DECLARE_NATIVE_FUNCTION(btoa);

    NonnullRefPtr<DOM::Window> m_impl;

    XMLHttpRequestConstructor* m_xhr_constructor { nullptr };
    XMLHttpRequestPrototype* m_xhr_prototype { nullptr };
};

}
}
