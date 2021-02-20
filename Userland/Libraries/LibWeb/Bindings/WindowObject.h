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

#include <AK/TypeCasts.h>
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

    JS::Object* web_prototype(const String& class_name) { return m_prototypes.get(class_name).value_or(nullptr); }
    JS::NativeFunction* web_constructor(const String& class_name) { return m_constructors.get(class_name).value_or(nullptr); }

    template<typename T>
    JS::Object& ensure_web_prototype(const String& class_name)
    {
        auto it = m_prototypes.find(class_name);
        if (it != m_prototypes.end())
            return *it->value;
        auto* prototype = heap().allocate<T>(*this, *this);
        m_prototypes.set(class_name, prototype);
        return *prototype;
    }

    template<typename T>
    JS::NativeFunction& ensure_web_constructor(const String& class_name)
    {
        auto it = m_constructors.find(class_name);
        if (it != m_constructors.end())
            return *it->value;
        auto* constructor = heap().allocate<T>(*this, *this);
        m_constructors.set(class_name, constructor);
        define_property(class_name, JS::Value(constructor), JS::Attribute::Writable | JS::Attribute::Configurable);
        return *constructor;
    }

private:
    virtual const char* class_name() const override { return "WindowObject"; }
    virtual void visit_edges(Visitor&) override;

    JS_DECLARE_NATIVE_GETTER(document_getter);
    JS_DECLARE_NATIVE_SETTER(document_setter);

    JS_DECLARE_NATIVE_GETTER(performance_getter);

    JS_DECLARE_NATIVE_GETTER(event_getter);

    JS_DECLARE_NATIVE_FUNCTION(alert);
    JS_DECLARE_NATIVE_FUNCTION(confirm);
    JS_DECLARE_NATIVE_FUNCTION(prompt);
    JS_DECLARE_NATIVE_FUNCTION(set_interval);
    JS_DECLARE_NATIVE_FUNCTION(set_timeout);
    JS_DECLARE_NATIVE_FUNCTION(clear_interval);
    JS_DECLARE_NATIVE_FUNCTION(clear_timeout);
    JS_DECLARE_NATIVE_FUNCTION(request_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(cancel_animation_frame);
    JS_DECLARE_NATIVE_FUNCTION(atob);
    JS_DECLARE_NATIVE_FUNCTION(btoa);

    NonnullRefPtr<DOM::Window> m_impl;

    HashMap<String, JS::Object*> m_prototypes;
    HashMap<String, JS::NativeFunction*> m_constructors;
};

}
}
