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

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class WindowObject final : public JS::GlobalObject {
public:
    explicit WindowObject(Window&);
    virtual void initialize() override;
    virtual ~WindowObject() override;

    Window& impl() { return *m_impl; }
    const Window& impl() const { return *m_impl; }

    XMLHttpRequestPrototype* xhr_prototype() { return m_xhr_prototype; }
    XMLHttpRequestConstructor* xhr_constructor() { return m_xhr_constructor; }

private:
    virtual const char* class_name() const override { return "WindowObject"; }
    virtual void visit_children(Visitor&) override;

    static JS::Value document_getter(JS::Interpreter&);
    static void document_setter(JS::Interpreter&, JS::Value);

    static JS::Value alert(JS::Interpreter&);
    static JS::Value confirm(JS::Interpreter&);
    static JS::Value set_interval(JS::Interpreter&);
    static JS::Value set_timeout(JS::Interpreter&);
    static JS::Value request_animation_frame(JS::Interpreter&);
    static JS::Value cancel_animation_frame(JS::Interpreter&);

    NonnullRefPtr<Window> m_impl;

    XMLHttpRequestConstructor* m_xhr_constructor { nullptr };
    XMLHttpRequestPrototype* m_xhr_prototype { nullptr };
};

}
}
