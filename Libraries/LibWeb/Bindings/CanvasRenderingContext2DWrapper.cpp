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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/ImageDataWrapper.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/DOM/ImageData.h>

namespace Web {
namespace Bindings {

CanvasRenderingContext2DWrapper* wrap(JS::Heap& heap, CanvasRenderingContext2D& impl)
{
    return static_cast<CanvasRenderingContext2DWrapper*>(wrap_impl(heap, impl));
}

CanvasRenderingContext2DWrapper::CanvasRenderingContext2DWrapper(CanvasRenderingContext2D& impl)
    : Wrapper(*interpreter().global_object().object_prototype())
    , m_impl(impl)
{
    put_native_property("fillStyle", fill_style_getter, fill_style_setter);
    put_native_function("fillRect", fill_rect, 4);
    put_native_function("scale", scale, 2);
    put_native_function("translate", translate, 2);
    put_native_property("strokeStyle", stroke_style_getter, stroke_style_setter);
    put_native_function("strokeRect", stroke_rect, 4);
    put_native_function("drawImage", draw_image, 3);

    put_native_function("beginPath", begin_path, 0);
    put_native_function("closePath", close_path, 0);
    put_native_function("stroke", stroke, 0);
    put_native_function("fill", fill, 0);
    put_native_function("moveTo", move_to, 2);
    put_native_function("lineTo", line_to, 2);
    put_native_function("quadraticCurveTo", quadratic_curve_to, 4);

    put_native_function("createImageData", create_image_data, 1);
    put_native_function("putImageData", put_image_data, 3);

    put_native_property("lineWidth", line_width_getter, line_width_setter);
}

CanvasRenderingContext2DWrapper::~CanvasRenderingContext2DWrapper()
{
}

static CanvasRenderingContext2D* impl_from(JS::Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter);
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

    auto* image_argument = arguments[0].to_object(interpreter);
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
    if (auto* impl = impl_from(interpreter)) {
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_fill_style(string);
    }
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
    if (auto* impl = impl_from(interpreter)){
        auto string = value.to_string(interpreter);
        if (interpreter.exception())
            return;
        impl->set_stroke_style(string);
    }
}

JS::Value CanvasRenderingContext2DWrapper::line_width_getter(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    return JS::Value(impl->line_width());
}

void CanvasRenderingContext2DWrapper::line_width_setter(JS::Interpreter& interpreter, JS::Value value)
{
    if (auto* impl = impl_from(interpreter))
        impl->set_line_width(value.to_double());
}

JS::Value CanvasRenderingContext2DWrapper::begin_path(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    impl->begin_path();
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::close_path(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    impl->close_path();
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::stroke(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    impl->stroke();
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::fill(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    auto winding = Gfx::Painter::WindingRule::Nonzero;

    if (interpreter.argument_count() == 1) {
        auto arg0 = interpreter.argument(0);
        if (arg0.is_string()) {
            const auto& winding_name = arg0.as_string().string();
            if (winding_name == "evenodd") {
                winding = Gfx::Painter::WindingRule::EvenOdd;
            } else if (winding_name != "nonzero") {
                return interpreter.throw_exception<JS::TypeError>("fill winding rule must be either 'nonzero' or 'evenodd'");
            }
        } else {
            return interpreter.throw_exception<JS::TypeError>("fill called with non-string");
        }
    } else {
        // FIXME: Path2D object
        return JS::js_undefined();
    }
    impl->fill(winding);
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::move_to(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    double x = interpreter.argument(0).to_double();
    double y = interpreter.argument(1).to_double();
    impl->move_to(x, y);
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::line_to(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    double x = interpreter.argument(0).to_double();
    double y = interpreter.argument(1).to_double();
    impl->line_to(x, y);
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::quadratic_curve_to(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    double cx = interpreter.argument(0).to_double();
    double cy = interpreter.argument(1).to_double();
    double x = interpreter.argument(2).to_double();
    double y = interpreter.argument(3).to_double();
    impl->quadratic_curve_to(cx, cy, x, y);
    return JS::js_undefined();
}

JS::Value CanvasRenderingContext2DWrapper::create_image_data(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};
    i32 width = interpreter.argument(0).to_i32();
    i32 height = interpreter.argument(1).to_i32();
    auto image_data = impl->create_image_data(interpreter.global_object(), width, height);
    return wrap(interpreter.heap(), *image_data);
}

JS::Value CanvasRenderingContext2DWrapper::put_image_data(JS::Interpreter& interpreter)
{
    auto* impl = impl_from(interpreter);
    if (!impl)
        return {};

    auto* image_data_object = interpreter.argument(0).to_object(interpreter);
    if (!image_data_object)
        return {};

    if (StringView(image_data_object->class_name()) != "ImageDataWrapper") {
        return interpreter.throw_exception<JS::TypeError>("putImageData called with non-ImageData");
    }

    auto& image_data = static_cast<ImageDataWrapper*>(image_data_object)->impl();
    auto x = interpreter.argument(1).to_double();
    auto y = interpreter.argument(2).to_double();
    impl->put_image_data(image_data, x, y);
    return JS::js_undefined();
}

}
}
