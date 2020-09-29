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

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Shape.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/XMLHttpRequestConstructor.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/XMLHttpRequest.h>

namespace Web::Bindings {

XMLHttpRequestConstructor::XMLHttpRequestConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

void XMLHttpRequestConstructor::initialize(JS::GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("length", JS::Value(1), JS::Attribute::Configurable);

    define_property("UNSENT", JS::Value((i32)XMLHttpRequest::ReadyState::Unsent), JS::Attribute::Enumerable);
    define_property("OPENED", JS::Value((i32)XMLHttpRequest::ReadyState::Opened), JS::Attribute::Enumerable);
    define_property("HEADERS_RECEIVED", JS::Value((i32)XMLHttpRequest::ReadyState::HeadersReceived), JS::Attribute::Enumerable);
    define_property("LOADING", JS::Value((i32)XMLHttpRequest::ReadyState::Loading), JS::Attribute::Enumerable);
    define_property("DONE", JS::Value((i32)XMLHttpRequest::ReadyState::Done), JS::Attribute::Enumerable);
}

XMLHttpRequestConstructor::~XMLHttpRequestConstructor()
{
}

JS::Value XMLHttpRequestConstructor::call()
{
    return construct(*this);
}

JS::Value XMLHttpRequestConstructor::construct(Function&)
{
    auto& window = static_cast<WindowObject&>(global_object());
    return heap().allocate<XMLHttpRequestWrapper>(window, window, XMLHttpRequest::create(window.impl()));
}

}
