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

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>

namespace Web {
namespace Bindings {

HTMLCanvasElementWrapper::HTMLCanvasElementWrapper(HTMLCanvasElement& element)
    : ElementWrapper(element)
{
    put_native_function("getContext", get_context, 1);

    put_native_property("width", width_getter, nullptr);
    put_native_property("height", height_getter, nullptr);
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

static HTMLCanvasElement* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's a HTMLCanvasElementWrapper somehow!
    return &static_cast<HTMLCanvasElementWrapper*>(this_object)->node();
}

JS::Value HTMLCanvasElementWrapper::get_context(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() >= 1) {
        auto string = arguments[0].to_string(interpreter);
        if (interpreter.exception())
            return {};
        auto* context = impl->get_context(string);
        return wrap(interpreter.heap(), *context);
    }
    return JS::js_undefined();
}

JS::Value HTMLCanvasElementWrapper::width_getter(JS::Interpreter& interpreter)
{
    if (auto* impl = impl_from(interpreter))
        return JS::Value(impl->requested_width());
    return {};
}

JS::Value HTMLCanvasElementWrapper::height_getter(JS::Interpreter& interpreter)
{
    if (auto* impl = impl_from(interpreter))
        return JS::Value(impl->requested_height());
    return {};
}

}
}
