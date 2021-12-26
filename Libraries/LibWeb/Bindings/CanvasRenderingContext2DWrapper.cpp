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
#include <LibWeb/DOM/HTMLCanvasElement.h>
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
    define_native_function("fillRect", fill_rect, 4);
    define_native_function("scale", scale, 2);
    define_native_function("translate", translate, 2);
    define_native_function("strokeRect", stroke_rect, 4);
    define_native_function("drawImage", draw_image, 3);
    define_native_function("beginPath", begin_path, 0);
    define_native_function("closePath", close_path, 0);
    define_native_function("stroke", stroke, 0);
    define_native_function("fill", fill, 0);
    define_native_function("moveTo", move_to, 2);
    define_native_function("lineTo", line_to, 2);
    define_native_function("quadraticCurveTo", quadratic_curve_to, 4);
    define_native_function("createImageData", create_image_data, 1);
    define_native_function("putImageData", put_image_data, 3);

    define_native_property("fillStyle", fill_style_getter, fill_style_setter);
    define_native_property("strokeStyle", stroke_style_getter, stroke_style_setter);
    define_native_property("lineWidth", line_width_getter, line_width_setter);
    define_native_property("canvas", canvas_getter, nullptr);
}

CanvasRenderingContext2DWrapper::~CanvasRenderingContext2DWrapper()
{
}

static CanvasRenderingContext2D* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's a CanvasRenderingContext2DWrapper somehow!
    return &static_cast<CanvasRenderingContext2DWrapper*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::fill_rect)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    if (interpreter.argument_count() >= 4) {
        auto x = interpreter.argument(0).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto y = interpreter.argument(1).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto width = interpreter.argument(2).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto height = interpreter.argument(3).to_double(interpreter);
        if (interpreter.exception())
            return {};
        impl->fill_rect(x, y, width, height);
    }
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::stroke_rect)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    if (interpreter.argument_count() >= 4) {
        auto x = interpreter.argument(0).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto y = interpreter.argument(1).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto width = interpreter.argument(2).to_double(interpreter);
        if (interpreter.exception())
            return {};
        auto height = interpreter.argument(3).to_double(interpreter);
        if (interpreter.exception())
            return {};
        impl->stroke_rect(x, y, width, height);
    }
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::draw_image)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    if (interpreter.argument_count() < 3)
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::DrawImageArgumentCount);
    auto* image_argument = interpreter.argument(0).to_object(interpreter, global_object);
    if (!image_argument)
        return {};
    if (StringView(image_argument->class_name()) != "HTMLImageElementWrapper")
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::ImageIsAn, image_argument->class_name());

    auto x = interpreter.argument(1).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto y = interpreter.argument(2).to_double(interpreter);
    if (interpreter.exception())
        return {};
    impl->draw_image(static_cast<const HTMLImageElementWrapper&>(*image_argument).node(), x, y);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::scale)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    if (interpreter.argument_count() >= 2) {
        auto sx = interpreter.argument(0).to_number(interpreter);
        if (interpreter.exception())
            return {};
        auto sy = interpreter.argument(1).to_number(interpreter);
        if (interpreter.exception())
            return {};
        if (sx.is_finite_number() && sy.is_finite_number())
            impl->scale(sx.as_double(), sy.as_double());
    }
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::translate)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    if (interpreter.argument_count() >= 2) {
        auto tx = interpreter.argument(0).to_number(interpreter);
        if (interpreter.exception())
            return {};
        auto ty = interpreter.argument(1).to_number(interpreter);
        if (interpreter.exception())
            return {};
        if (tx.is_finite_number() && ty.is_finite_number())
            impl->translate(tx.as_double(), ty.as_double());
    }
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_GETTER(CanvasRenderingContext2DWrapper::fill_style_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return JS::js_string(interpreter, impl->fill_style());
}

JS_DEFINE_NATIVE_SETTER(CanvasRenderingContext2DWrapper::fill_style_setter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return;
    auto string = value.to_string(interpreter);
    if (interpreter.exception())
        return;
    impl->set_fill_style(string);
}

JS_DEFINE_NATIVE_GETTER(CanvasRenderingContext2DWrapper::stroke_style_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return JS::js_string(interpreter, impl->stroke_style());
}

JS_DEFINE_NATIVE_SETTER(CanvasRenderingContext2DWrapper::stroke_style_setter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return;
    auto string = value.to_string(interpreter);
    if (interpreter.exception())
        return;
    impl->set_stroke_style(string);
}

JS_DEFINE_NATIVE_GETTER(CanvasRenderingContext2DWrapper::line_width_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return JS::Value(impl->line_width());
}

JS_DEFINE_NATIVE_SETTER(CanvasRenderingContext2DWrapper::line_width_setter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return;
    auto line_width = value.to_double(interpreter);
    if (interpreter.exception())
        return;
    impl->set_line_width(line_width);
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::begin_path)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    impl->begin_path();
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::close_path)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    impl->close_path();
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::stroke)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    impl->stroke();
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::fill)
{
    auto* impl = impl_from(interpreter, global_object);
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
                return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::FillBadWindingRule);
            }
        } else {
            return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::FillNonString);
        }
    } else {
        // FIXME: Path2D object
        return JS::js_undefined();
    }
    impl->fill(winding);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::move_to)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    auto x = interpreter.argument(0).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto y = interpreter.argument(1).to_double(interpreter);
    if (interpreter.exception())
        return {};
    impl->move_to(x, y);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::line_to)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    auto x = interpreter.argument(0).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto y = interpreter.argument(1).to_double(interpreter);
    if (interpreter.exception())
        return {};
    impl->line_to(x, y);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::quadratic_curve_to)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    auto cx = interpreter.argument(0).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto cy = interpreter.argument(1).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto x = interpreter.argument(2).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto y = interpreter.argument(3).to_double(interpreter);
    if (interpreter.exception())
        return {};
    impl->quadratic_curve_to(cx, cy, x, y);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::create_image_data)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    auto width = interpreter.argument(0).to_i32(interpreter);
    if (interpreter.exception())
        return {};
    auto height = interpreter.argument(1).to_i32(interpreter);
    if (interpreter.exception())
        return {};
    auto image_data = impl->create_image_data(global_object, width, height);
    return wrap(interpreter.heap(), *image_data);
}

JS_DEFINE_NATIVE_FUNCTION(CanvasRenderingContext2DWrapper::put_image_data)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};

    auto* image_data_object = interpreter.argument(0).to_object(interpreter, global_object);
    if (!image_data_object)
        return {};

    if (StringView(image_data_object->class_name()) != "ImageDataWrapper") {
        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::PutImageDataBadCall);
    }

    auto& image_data = static_cast<ImageDataWrapper*>(image_data_object)->impl();
    auto x = interpreter.argument(1).to_double(interpreter);
    if (interpreter.exception())
        return {};
    auto y = interpreter.argument(2).to_double(interpreter);
    if (interpreter.exception())
        return {};
    impl->put_image_data(image_data, x, y);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_GETTER(CanvasRenderingContext2DWrapper::canvas_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    auto* element = impl->element();
    if (!element)
        return JS::js_null();
    return wrap(interpreter.heap(), *element);
}

}
}
