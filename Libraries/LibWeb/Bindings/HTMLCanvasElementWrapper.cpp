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
#include <AK/FlyString.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>

namespace Web {
namespace Bindings {

HTMLCanvasElementWrapper::HTMLCanvasElementWrapper(HTMLCanvasElement& element)
    : NodeWrapper(element)
{
    put_native_function("getContext", [this](JS::Object*, const Vector<JS::Value>& arguments) -> JS::Value {
        if (arguments.size() >= 1) {
            auto* context = node().get_context(arguments[0].to_string());
            return wrap(heap(), *context);
        }
        return JS::js_undefined();
    });
    put_native_property(
        "width",
        [this](JS::Object*) {
            return JS::Value(node().preferred_width());
        },
        nullptr);
    put_native_property(
        "height",
        [this](JS::Object*) {
            return JS::Value(node().preferred_height());
        },
        nullptr);
}

HTMLCanvasElementWrapper::~HTMLCanvasElementWrapper()
{
}

HTMLCanvasElement& HTMLCanvasElementWrapper::node()
{
    return static_cast<HTMLCanvasElement&>(NodeWrapper::node());
}

const HTMLCanvasElement& HTMLCanvasElementWrapper::node() const
{
    return static_cast<const HTMLCanvasElement&>(NodeWrapper::node());
}

}
}
