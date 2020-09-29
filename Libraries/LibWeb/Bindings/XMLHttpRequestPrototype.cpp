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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/XMLHttpRequestPrototype.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/XMLHttpRequest.h>

namespace Web::Bindings {

XMLHttpRequestPrototype::XMLHttpRequestPrototype(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void XMLHttpRequestPrototype::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_function("open", open, 2);
    define_native_function("send", send, 0);
    define_native_property("readyState", ready_state_getter, nullptr, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_property("responseText", response_text_getter, nullptr, JS::Attribute::Enumerable | JS::Attribute::Configurable);

    define_property("UNSENT", JS::Value((i32)XMLHttpRequest::ReadyState::Unsent), JS::Attribute::Enumerable);
    define_property("OPENED", JS::Value((i32)XMLHttpRequest::ReadyState::Opened), JS::Attribute::Enumerable);
    define_property("HEADERS_RECEIVED", JS::Value((i32)XMLHttpRequest::ReadyState::HeadersReceived), JS::Attribute::Enumerable);
    define_property("LOADING", JS::Value((i32)XMLHttpRequest::ReadyState::Loading), JS::Attribute::Enumerable);
    define_property("DONE", JS::Value((i32)XMLHttpRequest::ReadyState::Done), JS::Attribute::Enumerable);
}

XMLHttpRequestPrototype::~XMLHttpRequestPrototype()
{
}

static XMLHttpRequest* impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (StringView("XMLHttpRequestWrapper") != this_object->class_name()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "XMLHttpRequest");
        return nullptr;
    }
    return &static_cast<XMLHttpRequestWrapper*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(XMLHttpRequestPrototype::open)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    auto arg0 = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto arg1 = vm.argument(1).to_string(global_object);
    if (vm.exception())
        return {};
    impl->open(arg0, arg1);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(XMLHttpRequestPrototype::send)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    impl->send();
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_GETTER(XMLHttpRequestPrototype::ready_state_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return JS::Value((i32)impl->ready_state());
}

JS_DEFINE_NATIVE_GETTER(XMLHttpRequestPrototype::response_text_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return JS::js_string(vm, impl->response_text());
}

}
