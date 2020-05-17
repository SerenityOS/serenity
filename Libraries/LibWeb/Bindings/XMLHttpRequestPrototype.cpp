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

#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/XMLHttpRequestPrototype.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/XMLHttpRequest.h>

namespace Web {
namespace Bindings {

XMLHttpRequestPrototype::XMLHttpRequestPrototype()
    : Object(interpreter().global_object().object_prototype())
{
    put_native_function("open", open, 2);
    put_native_function("send", send, 0);
    put_native_property("readyState", ready_state_getter, nullptr, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    put_native_property("responseText", response_text_getter, nullptr, JS::Attribute::Enumerable | JS::Attribute::Configurable);

    put("UNSENT", JS::Value((i32)XMLHttpRequest::ReadyState::Unsent), JS::Attribute::Enumerable);
    put("OPENED", JS::Value((i32)XMLHttpRequest::ReadyState::Opened), JS::Attribute::Enumerable);
    put("HEADERS_RECEIVED", JS::Value((i32)XMLHttpRequest::ReadyState::HeadersReceived), JS::Attribute::Enumerable);
    put("LOADING", JS::Value((i32)XMLHttpRequest::ReadyState::Loading), JS::Attribute::Enumerable);
    put("DONE", JS::Value((i32)XMLHttpRequest::ReadyState::Done), JS::Attribute::Enumerable);
}

XMLHttpRequestPrototype::~XMLHttpRequestPrototype()
{
}

static XMLHttpRequest* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
    if (!this_object)
        return nullptr;
    if (StringView("XMLHttpRequestWrapper") != this_object->class_name()) {
        interpreter.throw_exception<JS::TypeError>("This is not an XMLHttpRequest object");
        return nullptr;
    }
    return &static_cast<XMLHttpRequestWrapper*>(this_object)->impl();
}

JS::Value XMLHttpRequestPrototype::open(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto arg0 = interpreter.argument(0).to_string(interpreter);
    if (interpreter.exception())
        return {};
    auto arg1 = interpreter.argument(1).to_string(interpreter);
    if (interpreter.exception())
        return {};
    impl->open(arg0, arg1);
    return JS::js_undefined();
}

JS::Value XMLHttpRequestPrototype::send(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    impl->send();
    return JS::js_undefined();
}

JS::Value XMLHttpRequestPrototype::ready_state_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return JS::Value((i32)impl->ready_state());
}

JS::Value XMLHttpRequestPrototype::response_text_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return JS::js_string(interpreter, impl->response_text());
}

}
}
