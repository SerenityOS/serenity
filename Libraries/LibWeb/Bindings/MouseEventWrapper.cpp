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
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/MouseEventWrapper.h>
#include <LibWeb/DOM/MouseEvent.h>

namespace Web {
namespace Bindings {

MouseEventWrapper::MouseEventWrapper(JS::GlobalObject& global_object, MouseEvent& event)
    : EventWrapper(global_object, event)
{
}

void MouseEventWrapper::initialize(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    EventWrapper::initialize(interpreter, global_object);
    define_native_property("offsetX", offset_x_getter, nullptr);
    define_native_property("offsetY", offset_y_getter, nullptr);
}

MouseEventWrapper::~MouseEventWrapper()
{
}

const MouseEvent& MouseEventWrapper::event() const
{
    return static_cast<const MouseEvent&>(EventWrapper::event());
}

MouseEvent& MouseEventWrapper::event()
{
    return static_cast<MouseEvent&>(EventWrapper::event());
}

static MouseEvent* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);
    if (!this_object)
        return nullptr;
    // FIXME: Verify that it's a CanvasRenderingContext2DWrapper somehow!
    return &static_cast<MouseEventWrapper*>(this_object)->event();
}

JS_DEFINE_NATIVE_GETTER(MouseEventWrapper::offset_x_getter)
{
    if (auto* impl = impl_from(interpreter, global_object))
        return JS::Value(impl->offset_x());
    return {};
}

JS_DEFINE_NATIVE_GETTER(MouseEventWrapper::offset_y_getter)
{
    if (auto* impl = impl_from(interpreter, global_object))
        return JS::Value(impl->offset_y());
    return {};
}

}
}
