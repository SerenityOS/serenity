/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/Shape.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/EventTargetConstructor.h>
#include <LibWeb/Bindings/EventTargetPrototype.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/PerformanceWrapper.h>
#include <LibWeb/Bindings/ScreenWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/BrowsingContext.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

#include <LibWeb/Bindings/WindowObjectHelper.h>

namespace Web::Bindings {

WindowObject::WindowObject(DOM::Window& impl)
    : m_impl(impl)
{
    impl.set_wrapper({}, *this);
}

void WindowObject::initialize_global_object()
{
    Base::initialize_global_object();

    set_prototype(&ensure_web_prototype<EventTargetPrototype>("EventTarget"));

    define_property("window", this, JS::Attribute::Enumerable);
    define_property("frames", this, JS::Attribute::Enumerable);
    define_property("self", this, JS::Attribute::Enumerable);
    define_native_property("top", top_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("parent", parent_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("document", document_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("performance", performance_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("screen", screen_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("innerWidth", inner_width_getter, nullptr, JS::Attribute::Enumerable);
    define_native_property("innerHeight", inner_height_getter, nullptr, JS::Attribute::Enumerable);
    define_native_function("alert", alert);
    define_native_function("confirm", confirm);
    define_native_function("prompt", prompt);
    define_native_function("setInterval", set_interval, 1);
    define_native_function("setTimeout", set_timeout, 1);
    define_native_function("clearInterval", clear_interval, 1);
    define_native_function("clearTimeout", clear_timeout, 1);
    define_native_function("requestAnimationFrame", request_animation_frame, 1);
    define_native_function("cancelAnimationFrame", cancel_animation_frame, 1);
    define_native_function("atob", atob, 1);
    define_native_function("btoa", btoa, 1);

    // Legacy
    define_native_property("event", event_getter, nullptr, JS::Attribute::Enumerable);

    define_property("navigator", heap().allocate<NavigatorObject>(*this, *this), JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_property("location", heap().allocate<LocationObject>(*this, *this), JS::Attribute::Enumerable | JS::Attribute::Configurable);

    // WebAssembly "namespace"
    define_property("WebAssembly", heap().allocate<WebAssemblyObject>(*this, *this), JS::Attribute::Enumerable | JS::Attribute::Configurable);

    ADD_WINDOW_OBJECT_INTERFACES;
}

WindowObject::~WindowObject()
{
}

void WindowObject::visit_edges(Visitor& visitor)
{
    GlobalObject::visit_edges(visitor);
    for (auto& it : m_prototypes)
        visitor.visit(it.value);
    for (auto& it : m_constructors)
        visitor.visit(it.value);
}

Origin WindowObject::origin() const
{
    return impl().document().origin();
}

static DOM::Window* impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object) {
        VERIFY_NOT_REACHED();
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
    // https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#simple-dialogs
    // Note: This method is defined using two overloads, instead of using an optional argument,
    //       for historical reasons. The practical impact of this is that alert(undefined) is
    //       treated as alert("undefined"), but alert() is treated as alert("").
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
    if (!vm.argument(0).is_undefined()) {
        message = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    return JS::Value(impl->confirm(message));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::prompt)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    String message = "";
    String default_ = "";
    if (!vm.argument(0).is_undefined()) {
        message = vm.argument(0).to_string(global_object);
        if (vm.exception())
            return {};
    }
    if (!vm.argument(1).is_undefined()) {
        default_ = vm.argument(1).to_string(global_object);
        if (vm.exception())
            return {};
    }
    auto response = impl->prompt(message, default_);
    if (response.is_null())
        return JS::js_null();
    return JS::js_string(vm, response);
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
    auto decoder = TextCodec::decoder_for("windows-1252");
    VERIFY(decoder);
    return JS::js_string(vm, decoder->to_utf8(decoded));
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

// https://html.spec.whatwg.org/multipage/browsers.html#dom-top
JS_DEFINE_NATIVE_GETTER(WindowObject::top_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    auto* this_browsing_context = impl->document().browsing_context();
    if (!this_browsing_context)
        return JS::js_null();

    VERIFY(this_browsing_context->top_level_browsing_context().document());
    auto& top_window = this_browsing_context->top_level_browsing_context().document()->window();
    return top_window.wrapper();
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-parent
JS_DEFINE_NATIVE_GETTER(WindowObject::parent_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    auto* this_browsing_context = impl->document().browsing_context();
    if (!this_browsing_context)
        return JS::js_null();

    if (this_browsing_context->parent()) {
        VERIFY(this_browsing_context->parent()->document());
        auto& parent_window = this_browsing_context->parent()->document()->window();
        return parent_window.wrapper();
    }
    VERIFY(this_browsing_context == &this_browsing_context->top_level_browsing_context());
    return impl->wrapper();
}

JS_DEFINE_NATIVE_GETTER(WindowObject::document_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return wrap(global_object, impl->document());
}

JS_DEFINE_NATIVE_GETTER(WindowObject::performance_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return wrap(global_object, impl->performance());
}

JS_DEFINE_NATIVE_GETTER(WindowObject::screen_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return wrap(global_object, impl->screen());
}

JS_DEFINE_NATIVE_GETTER(WindowObject::event_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    if (!impl->current_event())
        return JS::js_undefined();
    return wrap(global_object, const_cast<DOM::Event&>(*impl->current_event()));
}

JS_DEFINE_NATIVE_GETTER(WindowObject::inner_width_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return JS::Value(impl->inner_width());
}

JS_DEFINE_NATIVE_GETTER(WindowObject::inner_height_getter)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
    return JS::Value(impl->inner_height());
}

}
