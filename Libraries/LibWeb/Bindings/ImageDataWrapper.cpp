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
#include <LibJS/Runtime/Uint8ClampedArray.h>
#include <LibWeb/Bindings/ImageDataWrapper.h>
#include <LibWeb/DOM/ImageData.h>

namespace Web {
namespace Bindings {

ImageDataWrapper* wrap(JS::Heap& heap, ImageData& event)
{
    return static_cast<ImageDataWrapper*>(wrap_impl(heap, event));
}

ImageDataWrapper::ImageDataWrapper(JS::GlobalObject& global_object, ImageData& impl)
    : Wrapper(*global_object.object_prototype())
    , m_impl(impl)
{
}

void ImageDataWrapper::initialize(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    Wrapper::initialize(interpreter, global_object);
    define_native_property("width", width_getter, nullptr);
    define_native_property("height", height_getter, nullptr);
    define_native_property("data", data_getter, nullptr);
}

ImageDataWrapper::~ImageDataWrapper()
{
}

static ImageData* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);
    if (!this_object) {
        ASSERT_NOT_REACHED();
        return nullptr;
    }
    if (StringView("ImageDataWrapper") != this_object->class_name()) {
        interpreter.throw_exception<JS::TypeError>(JS::ErrorType::NotAn, "ImageDataWrapper");
        return nullptr;
    }
    return &static_cast<ImageDataWrapper*>(this_object)->impl();
}

JS_DEFINE_NATIVE_GETTER(ImageDataWrapper::width_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return JS::Value(impl->width());
}

JS_DEFINE_NATIVE_GETTER(ImageDataWrapper::height_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return JS::Value(impl->height());
}

JS_DEFINE_NATIVE_GETTER(ImageDataWrapper::data_getter)
{
    auto* impl = impl_from(interpreter, global_object);
    if (!impl)
        return {};
    return impl->data();
}

}
}
