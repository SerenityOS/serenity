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
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/XMLHttpRequestConstructor.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/XMLHttpRequest.h>

namespace Web {
namespace Bindings {

XMLHttpRequestConstructor::XMLHttpRequestConstructor()
    : NativeFunction(*interpreter().global_object().function_prototype())
{
    put("length", JS::Value(1));

    put("UNSENT", JS::Value((i32)XMLHttpRequest::ReadyState::Unsent));
    put("OPENED", JS::Value((i32)XMLHttpRequest::ReadyState::Opened));
    put("HEADERS_RECEIVED", JS::Value((i32)XMLHttpRequest::ReadyState::HeadersReceived));
    put("LOADING", JS::Value((i32)XMLHttpRequest::ReadyState::Loading));
    put("DONE", JS::Value((i32)XMLHttpRequest::ReadyState::Done));
}

XMLHttpRequestConstructor::~XMLHttpRequestConstructor()
{
}

JS::Value XMLHttpRequestConstructor::call(JS::Interpreter& interpreter)
{
    return construct(interpreter);
}

JS::Value XMLHttpRequestConstructor::construct(JS::Interpreter& interpreter)
{
    auto& window = static_cast<WindowObject&>(interpreter.global_object());
    return interpreter.heap().allocate<XMLHttpRequestWrapper>(XMLHttpRequest::create(window.impl()));
}

}
}
