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

#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/Shape.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/PerformanceWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/XMLHttpRequestConstructor.h>
#include <LibWeb/Bindings/XMLHttpRequestPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Origin.h>

namespace Web {
namespace Bindings {

WindowObject::WindowObject(DOM::Window& impl)
    : m_impl(impl)
{
    impl.set_wrapper({}, *this);
}

void WindowObject::initialize()
{
    GlobalObject::initialize();

    define_property("window", this, JS::Attribute::Enumerable);
    define_property("frames", this, JS::Attribute::Enumerable);
    define_property("self", this, JS::Attribute::Enumerable);
    define_native_property("document", document_getter, document_setter, JS::Attribute::Enumerable);
    define_native_property("performance", performance_getter, nullptr, JS::Attribute::Enumerable);
    define_native_function("alert", alert);
    define_native_function("confirm", confirm);
    define_native_function("setInterval", set_interval, 1);
    define_native_function("setTimeout", set_timeout, 1);
    define_native_function("clearInterval", clear_interval, 1);
    define_native_function("clearTimeout", clear_timeout, 1);
    define_native_function("requestAnimationFrame", request_animation_frame, 1);
    define_native_function("cancelAnimationFrame", cancel_animation_frame, 1);
    define_native_function("atob", atob, 1);
    define_native_function("btoa", btoa, 1);

    define_property("navigator", heap().allocate<NavigatorObject>(*this, *this), JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_property("location", heap().allocate<LocationObject>(*this, *this), JS::Attribute::Enumerable | JS::Attribute::Configurable);

    m_xhr_prototype = heap().allocate<XMLHttpRequestPrototype>(*this, *this);
    m_xhr_constructor = heap().allocate<XMLHttpRequestConstructor>(*this, *this);
    m_xhr_constructor->define_property("prototype", m_xhr_prototype, 0);
    add_constructor("XMLHttpRequest", m_xhr_constructor, *m_xhr_prototype);
}

WindowObject::~WindowObject()
{
}

void WindowObject::visit_children(Visitor& visitor)
{
    GlobalObject::visit_children(visitor);
    visitor.visit(m_xhr_constructor);
    visitor.visit(m_xhr_prototype);
}

Origin WindowObject::origin() const
{
    return impl().document().origin();
}

static DOM::Window* impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object) {
        ASSERT_NOT_REACHED();
        return nullptr;
    }
    if (StringView("WindowObject") != this_object->class_name()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "WindowObject");
        return nullptr;
    }
    return &static_cast<WindowObject*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::alert)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    String message = "";
    if (vm.argument_count()) {
        message = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    impl->alert(message);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::confirm)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    String message = "";
    if (vm.argument_count()) {
        message = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    return JS::Value(impl->confirm(message));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::set_interval)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "setInterval");
        return {};
    }
    auto* callback_object = vm.argument(0).to_object(global_object);
    if (!callback_object)
        return {};
    if (!callback_object->is_function()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);
        return {};
    }
    i32 interval = 0;
    if (vm.argument_count() >= 2) {
        interval = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};
        if (interval < 0)
            interval = 0;
    }

    auto timer_id = impl->set_interval(*static_cast<JS::Function*>(callback_object), interval);
    return JS::Value(timer_id);
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::set_timeout)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "setTimeout");
        return {};
    }
    auto* callback_object = vm.argument(0).to_object(global_object);
    if (!callback_object)
        return {};
    if (!callback_object->is_function()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);
        return {};
    }
    i32 interval = 0;
    if (vm.argument_count() >= 2) {
        interval = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};
        if (interval < 0)
            interval = 0;
    }

    auto timer_id = impl->set_timeout(*static_cast<JS::Function*>(callback_object), interval);
    return JS::Value(timer_id);
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::clear_timeout)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "clearTimeout");
        return {};
    }
    i32 timer_id = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};
    impl->clear_timeout(timer_id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::clear_interval)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "clearInterval");
        return {};
    }
    i32 timer_id = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};
    impl->clear_timeout(timer_id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::request_animation_frame)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "requestAnimationFrame");
        return {};
    }
    auto* callback_object = vm.argument(0).to_object(global_object);
    if (!callback_object)
        return {};
    if (!callback_object->is_function()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);
        return {};
    }
    return JS::Value(impl->request_animation_frame(*static_cast<JS::Function*>(callback_object)));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::cancel_animation_frame)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "cancelAnimationFrame");
        return {};
    }
    auto id = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};
    impl->cancel_animation_frame(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::atob)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "atob");
        return {};
    }
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto decoded = decode_base64(StringView(string));

    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    return JS::js_string(vm, TextCodec::decoder_for("iso-8859-1")->to_utf8(decoded));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::btoa)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "btoa");
        return {};
    }
    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    Vector<u8> byte_string;
    byte_string.ensure_capacity(string.length());
    for (u32 code_point : Utf8View(string)) {
        if (code_point > 0xff) {
            vm.throw_exception<JS::InvalidCharacterError>(global_object, JS::ErrorType::NotAByteString, "btoa");
            return {};
        }
        byte_string.append(code_point);
    }

    auto encoded = encode_base64(byte_string.span());
    return JS::js_string(vm, move(encoded));
}

JS_DEFINE_NATIVE_GETTER(WindowObject::document_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return wrap(global_object, impl->document());
}

JS_DEFINE_NATIVE_SETTER(WindowObject::document_setter)
{
    // FIXME: Figure out what we should do here. Just ignore attempts to set window.document for now.
    UNUSED_PARAM(value);
}

JS_DEFINE_NATIVE_GETTER(WindowObject::performance_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return wrap(global_object, impl->performance());
}

}
}
