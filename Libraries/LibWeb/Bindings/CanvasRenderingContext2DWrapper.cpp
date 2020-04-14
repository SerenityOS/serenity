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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLImageElement.h>

namespace Web {
namespace Bindings {

CanvasRenderingContext2DWrapper* wrap(JS::Heap& heap, CanvasRenderingContext2D& impl)
{
    return static_cast<CanvasRenderingContext2DWrapper*>(wrap_impl(heap, impl));
}

CanvasRenderingContext2DWrapper::CanvasRenderingContext2DWrapper(CanvasRenderingContext2D& impl)
    : m_impl(impl)
{
    put_native_property("fillStyle", fill_style_getter, fill_style_setter);
    put_native_function("fillRect", fill_rect, 4);
    put_native_function("scale", scale, 2);
    put_native_function("translate", translate, 2);
    put_native_property("strokeStyle", stroke_style_getter, stroke_style_setter);
    put_native_function("strokeRect", stroke_rect, 4);
    put_native_function("drawImage", draw_image, 3);
}

CanvasRenderingContext2DWrapper::~CanvasRenderingContext2DWrapper()
{
}

static CanvasRenderingContext2D* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's a CanvasRenderingContext2DWrapper somehow!
    return &static_cast<CanvasRenderingContext2DWrapper*>(this_object)->impl();
}

JS::Value CanvasRenderingContext2DWrapper::fill_rect(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() >= 4)
        impl->fill_rect(arguments[0].to_double(), arguments[1].to_double(), arguments[2].to_double(), arguments[3].to_double());
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::stroke_rect(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() >= 4)
        impl->stroke_rect(arguments[0].to_double(), arguments[1].to_double(), arguments[2].to_double(), arguments[3].to_double());
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::draw_image(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() < 3)
        return interpreter.throw_exception<JS::TypeError>("drawImage() needs more arguments");

    auto* image_argument = arguments[0].to_object(interpreter.heap());
    if (!image_argument)
        return {};
    if (StringView(image_argument->class_name()) != "HTMLImageElementWrapper")
        return interpreter.throw_exception<JS::TypeError>(String::format("Image is not an HTMLImageElement, it's an %s", image_argument->class_name()));

    auto x = arguments[1].to_double();
    auto y = arguments[2].to_double();

    impl->draw_image(static_cast<const HTMLImageElementWrapper&>(*image_argument).node(), x, y);
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::scale(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() >= 2)
        impl->scale(arguments[0].to_double(), arguments[1].to_double());
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::translate(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto& arguments = interpreter.call_frame().arguments;
    if (arguments.size() >= 2)
        impl->translate(arguments[0].to_double(), arguments[1].to_double());
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::fill_style_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return JS::js_string(interpreter, impl->fill_style());
}

void CanvasRenderingContext2DWrapper::fill_style_setter(JS::Interpreter& interpreter, JS::Value value)
{
    if (auto* impl = impl_from(interpreter))
        impl->set_fill_style(value.to_string());
}

JS::Value CanvasRenderingContext2DWrapper::stroke_style_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return JS::js_string(interpreter, impl->stroke_style());
}

void CanvasRenderingContext2DWrapper::stroke_style_setter(JS::Interpreter& interpreter, JS::Value value)
{
    if (auto* impl = impl_from(interpreter))
        impl->set_stroke_style(value.to_string());
}

}
}
