/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/Shape.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/CSSNamespace.h>
#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/Bindings/CryptoWrapper.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/ElementWrapper.h>
#include <LibWeb/Bindings/EventTargetConstructor.h>
#include <LibWeb/Bindings/EventTargetPrototype.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HistoryWrapper.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/MediaQueryListWrapper.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/PerformanceWrapper.h>
#include <LibWeb/Bindings/Replaceable.h>
#include <LibWeb/Bindings/ScreenWrapper.h>
#include <LibWeb/Bindings/SelectionWrapper.h>
#include <LibWeb/Bindings/StorageWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/WindowObjectHelper.h>
#include <LibWeb/Bindings/WindowPrototype.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Storage.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WindowObject::WindowObject(JS::Realm& realm, HTML::Window& impl)
    : GlobalObject(realm)
    , m_impl(impl)
{
    impl.set_wrapper({}, *this);
}

void WindowObject::initialize_global_object()
{
    Base::initialize_global_object();

    Object::set_prototype(&ensure_web_prototype<WindowPrototype>("Window"));

    auto& realm = *associated_realm();

    // FIXME: These should be native accessors, not properties
    define_direct_property("window", this, JS::Attribute::Enumerable);
    define_direct_property("frames", this, JS::Attribute::Enumerable);
    define_direct_property("self", this, JS::Attribute::Enumerable);
    define_native_accessor("top", top_getter, nullptr, JS::Attribute::Enumerable);
    define_native_accessor("parent", parent_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("document", document_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("name", name_getter, name_setter, JS::Attribute::Enumerable);
    define_native_accessor("history", history_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("performance", performance_getter, performance_setter, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_accessor("crypto", crypto_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("screen", screen_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("innerWidth", inner_width_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("innerHeight", inner_height_getter, {}, JS::Attribute::Enumerable);
    define_native_accessor("devicePixelRatio", device_pixel_ratio_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable;
    define_native_function("alert", alert, 0, attr);
    define_native_function("confirm", confirm, 0, attr);
    define_native_function("prompt", prompt, 0, attr);
    define_native_function("setInterval", set_interval, 1, attr);
    define_native_function("setTimeout", set_timeout, 1, attr);
    define_native_function("clearInterval", clear_interval, 1, attr);
    define_native_function("clearTimeout", clear_timeout, 1, attr);
    define_native_function("requestAnimationFrame", request_animation_frame, 1, attr);
    define_native_function("cancelAnimationFrame", cancel_animation_frame, 1, attr);
    define_native_function("atob", atob, 1, attr);
    define_native_function("btoa", btoa, 1, attr);

    define_native_function("queueMicrotask", queue_microtask, 1, attr);

    define_native_function("requestIdleCallback", request_idle_callback, 1, attr);
    define_native_function("cancelIdleCallback", cancel_idle_callback, 1, attr);

    define_native_function("getComputedStyle", get_computed_style, 1, attr);
    define_native_function("matchMedia", match_media, 1, attr);
    define_native_function("getSelection", get_selection, 0, attr);

    define_native_function("postMessage", post_message, 1, attr);

    // FIXME: These properties should be [Replaceable] according to the spec, but [Writable+Configurable] is the closest we have.
    define_native_accessor("scrollX", scroll_x_getter, {}, attr);
    define_native_accessor("pageXOffset", scroll_x_getter, {}, attr);
    define_native_accessor("scrollY", scroll_y_getter, {}, attr);
    define_native_accessor("pageYOffset", scroll_y_getter, {}, attr);

    define_native_function("scroll", scroll, 2, attr);
    define_native_function("scrollTo", scroll, 2, attr);
    define_native_function("scrollBy", scroll_by, 2, attr);

    define_native_accessor("screenX", screen_x_getter, {}, attr);
    define_native_accessor("screenY", screen_y_getter, {}, attr);
    define_native_accessor("screenLeft", screen_left_getter, {}, attr);
    define_native_accessor("screenTop", screen_top_getter, {}, attr);

    define_direct_property("CSS", heap().allocate<CSSNamespace>(*this, realm), 0);

    define_native_accessor("localStorage", local_storage_getter, {}, attr);
    define_native_accessor("sessionStorage", session_storage_getter, {}, attr);
    define_native_accessor("origin", origin_getter, {}, attr);

    // Legacy
    define_native_accessor("event", event_getter, event_setter, JS::Attribute::Enumerable);

    m_location_object = heap().allocate<LocationObject>(*this, realm);

    auto* m_navigator_object = heap().allocate<NavigatorObject>(*this, realm);
    define_direct_property("navigator", m_navigator_object, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_direct_property("clientInformation", m_navigator_object, JS::Attribute::Enumerable | JS::Attribute::Configurable);

    // NOTE: location is marked as [LegacyUnforgeable], meaning it isn't configurable.
    define_native_accessor("location", location_getter, location_setter, JS::Attribute::Enumerable);

    // WebAssembly "namespace"
    define_direct_property("WebAssembly", heap().allocate<WebAssemblyObject>(*this, realm), JS::Attribute::Enumerable | JS::Attribute::Configurable);

    // HTML::GlobalEventHandlers and HTML::WindowEventHandlers
#define __ENUMERATE(attribute, event_name) \
    define_native_accessor(#attribute, attribute##_getter, attribute##_setter, attr);
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE);
    ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE);
#undef __ENUMERATE

    ADD_WINDOW_OBJECT_INTERFACES;
}

void WindowObject::visit_edges(Visitor& visitor)
{
    GlobalObject::visit_edges(visitor);
    visitor.visit(m_location_object);
    for (auto& it : m_prototypes)
        visitor.visit(it.value);
    for (auto& it : m_constructors)
        visitor.visit(it.value);
}

HTML::Origin WindowObject::origin() const
{
    return impl().associated_document().origin();
}

// https://webidl.spec.whatwg.org/#platform-object-setprototypeof
JS::ThrowCompletionOr<bool> WindowObject::internal_set_prototype_of(JS::Object* prototype)
{
    // 1. Return ? SetImmutablePrototype(O, V).
    return set_immutable_prototype(prototype);
}

static JS::ThrowCompletionOr<HTML::Window*> impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    // Since this is a non built-in function we must treat it as non-strict mode
    // this means that a nullish this_value should be converted to the
    // global_object. Generally this does not matter as we try to convert the
    // this_value to a specific object type in the bindings. But since window is
    // the global object we make an exception here.
    // This allows calls like `setTimeout(f, 10)` to work.
    auto this_value = vm.this_value(global_object);
    if (this_value.is_nullish())
        this_value = &global_object;

    auto* this_object = MUST(this_value.to_object(global_object));

    if (!is<WindowObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "WindowObject");
    return &static_cast<WindowObject*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::alert)
{
    // https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#simple-dialogs
    // Note: This method is defined using two overloads, instead of using an optional argument,
    //       for historical reasons. The practical impact of this is that alert(undefined) is
    //       treated as alert("undefined"), but alert() is treated as alert("").
    auto* impl = TRY(impl_from(vm, global_object));
    String message = "";
    if (vm.argument_count())
        message = TRY(vm.argument(0).to_string(global_object));
    impl->alert(message);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::confirm)
{
    auto* impl = TRY(impl_from(vm, global_object));
    String message = "";
    if (!vm.argument(0).is_undefined())
        message = TRY(vm.argument(0).to_string(global_object));
    return JS::Value(impl->confirm(message));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::prompt)
{
    auto* impl = TRY(impl_from(vm, global_object));
    String message = "";
    String default_ = "";
    if (!vm.argument(0).is_undefined())
        message = TRY(vm.argument(0).to_string(global_object));
    if (!vm.argument(1).is_undefined())
        default_ = TRY(vm.argument(1).to_string(global_object));
    auto response = impl->prompt(message, default_);
    if (response.is_null())
        return JS::js_null();
    return JS::js_string(vm, response);
}

static JS::ThrowCompletionOr<TimerHandler> make_timer_handler(JS::GlobalObject& global_object, JS::Value handler)
{
    if (handler.is_function())
        return Bindings::CallbackType(JS::make_handle<JS::Object>(handler.as_function()), HTML::incumbent_settings_object());
    return TRY(handler.to_string(global_object));
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-settimeout
JS_DEFINE_NATIVE_FUNCTION(WindowObject::set_timeout)
{
    auto* impl = TRY(impl_from(vm, global_object));

    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "setTimeout");

    auto handler = TRY(make_timer_handler(global_object, vm.argument(0)));

    i32 timeout = 0;
    if (vm.argument_count() >= 2)
        timeout = TRY(vm.argument(1).to_i32(global_object));

    JS::MarkedVector<JS::Value> arguments { vm.heap() };
    for (size_t i = 2; i < vm.argument_count(); ++i)
        arguments.append(vm.argument(i));

    auto id = impl->set_timeout(move(handler), timeout, move(arguments));
    return JS::Value(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-setinterval
JS_DEFINE_NATIVE_FUNCTION(WindowObject::set_interval)
{
    auto* impl = TRY(impl_from(vm, global_object));

    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "setInterval");

    auto handler = TRY(make_timer_handler(global_object, vm.argument(0)));

    i32 timeout = 0;
    if (vm.argument_count() >= 2)
        timeout = TRY(vm.argument(1).to_i32(global_object));

    JS::MarkedVector<JS::Value> arguments { vm.heap() };
    for (size_t i = 2; i < vm.argument_count(); ++i)
        arguments.append(vm.argument(i));

    auto id = impl->set_interval(move(handler), timeout, move(arguments));
    return JS::Value(id);
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-cleartimeout
JS_DEFINE_NATIVE_FUNCTION(WindowObject::clear_timeout)
{
    auto* impl = TRY(impl_from(vm, global_object));

    i32 id = 0;
    if (vm.argument_count())
        id = TRY(vm.argument(0).to_i32(global_object));

    impl->clear_timeout(id);
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#dom-clearinterval
JS_DEFINE_NATIVE_FUNCTION(WindowObject::clear_interval)
{
    auto* impl = TRY(impl_from(vm, global_object));

    i32 id = 0;
    if (vm.argument_count())
        id = TRY(vm.argument(0).to_i32(global_object));

    impl->clear_interval(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::request_animation_frame)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "requestAnimationFrame");
    auto* callback_object = TRY(vm.argument(0).to_object(global_object));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);
    NonnullOwnPtr<Bindings::CallbackType> callback = adopt_own(*new Bindings::CallbackType(JS::make_handle(callback_object), HTML::incumbent_settings_object()));
    return JS::Value(impl->request_animation_frame(move(callback)));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::cancel_animation_frame)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "cancelAnimationFrame");
    auto id = TRY(vm.argument(0).to_i32(global_object));
    impl->cancel_animation_frame(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::queue_microtask)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "queueMicrotask");
    auto* callback_object = TRY(vm.argument(0).to_object(global_object));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);

    auto callback = adopt_own(*new Bindings::CallbackType(JS::make_handle(callback_object), HTML::incumbent_settings_object()));

    impl->queue_microtask(move(callback));
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::request_idle_callback)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountAtLeastOne, "requestIdleCallback");
    auto* callback_object = TRY(vm.argument(0).to_object(global_object));
    if (!callback_object->is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAFunctionNoParam);
    // FIXME: accept options object

    auto callback = adopt_own(*new Bindings::CallbackType(JS::make_handle(callback_object), HTML::incumbent_settings_object()));

    return JS::Value(impl->request_idle_callback(move(callback)));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::cancel_idle_callback)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "cancelIdleCallback");
    auto id = TRY(vm.argument(0).to_u32(global_object));
    impl->cancel_idle_callback(id);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::atob)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "atob");
    auto string = TRY(vm.argument(0).to_string(global_object));
    auto decoded = decode_base64(StringView(string));
    if (decoded.is_error())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::InvalidFormat, "Base64");

    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    auto decoder = TextCodec::decoder_for("windows-1252");
    VERIFY(decoder);
    return JS::js_string(vm, decoder->to_utf8(decoded.value()));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::btoa)
{
    if (!vm.argument_count())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "btoa");
    auto string = TRY(vm.argument(0).to_string(global_object));

    Vector<u8> byte_string;
    byte_string.ensure_capacity(string.length());
    for (u32 code_point : Utf8View(string)) {
        if (code_point > 0xff)
            return vm.throw_completion<JS::InvalidCharacterError>(global_object, JS::ErrorType::NotAByteString, "btoa");
        byte_string.append(code_point);
    }

    auto encoded = encode_base64(byte_string.span());
    return JS::js_string(vm, move(encoded));
}

// https://html.spec.whatwg.org/multipage/browsers.html#dom-top
JS_DEFINE_NATIVE_FUNCTION(WindowObject::top_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));

    auto* this_browsing_context = impl->associated_document().browsing_context();
    if (!this_browsing_context)
        return JS::js_null();

    VERIFY(this_browsing_context->top_level_browsing_context().active_document());
    auto& top_window = this_browsing_context->top_level_browsing_context().active_document()->window();
    return top_window.wrapper();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::parent_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    auto* parent = impl->parent();
    if (!parent)
        return JS::js_null();
    return parent->wrapper();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::document_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return wrap(global_object, impl->associated_document());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::performance_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return wrap(global_object, impl->performance());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::performance_setter)
{
    // https://webidl.spec.whatwg.org/#dfn-attribute-setter
    // 4.1. If no arguments were passed, then throw a TypeError.
    if (vm.argument_count() == 0)
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::BadArgCountOne, "set performance");

    auto* impl = TRY(impl_from(vm, global_object));

    // 5. If attribute is declared with the [Replaceable] extended attribute, then:
    // 1. Perform ? CreateDataProperty(esValue, id, V).
    VERIFY(impl->wrapper());
    TRY(impl->wrapper()->create_data_property("performance", vm.argument(0)));

    // 2. Return undefined.
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::screen_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return wrap(global_object, impl->screen());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::event_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!impl->current_event())
        return JS::js_undefined();
    return wrap(global_object, const_cast<DOM::Event&>(*impl->current_event()));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::event_setter)
{
    REPLACEABLE_PROPERTY_SETTER(WindowObject, event);
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::location_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    VERIFY(impl->wrapper());
    return impl->wrapper()->m_location_object;
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::location_setter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    VERIFY(impl->wrapper());
    TRY(impl->wrapper()->m_location_object->set(JS::PropertyKey("href"), vm.argument(0), JS::Object::ShouldThrowExceptions::Yes));
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::crypto_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return wrap(global_object, impl->crypto());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::inner_width_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->inner_width());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::inner_height_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->inner_height());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::device_pixel_ratio_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->device_pixel_ratio());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::get_computed_style)
{
    auto* impl = TRY(impl_from(vm, global_object));
    auto* object = TRY(vm.argument(0).to_object(global_object));
    if (!is<ElementWrapper>(object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "DOM element");

    return wrap(global_object, impl->get_computed_style(static_cast<ElementWrapper*>(object)->impl()));
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::get_selection)
{
    auto* impl = TRY(impl_from(vm, global_object));
    auto* selection = impl->get_selection();
    if (!selection)
        return JS::js_null();
    return wrap(global_object, *selection);
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::match_media)
{
    auto* impl = TRY(impl_from(vm, global_object));
    auto media = TRY(vm.argument(0).to_string(global_object));
    return wrap(global_object, impl->match_media(move(media)));
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrollx
JS_DEFINE_NATIVE_FUNCTION(WindowObject::scroll_x_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->scroll_x());
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrolly
JS_DEFINE_NATIVE_FUNCTION(WindowObject::scroll_y_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->scroll_y());
}

enum class ScrollBehavior {
    Auto,
    Smooth
};

// https://www.w3.org/TR/cssom-view/#perform-a-scroll
static void perform_a_scroll(Page& page, double x, double y, ScrollBehavior)
{
    // FIXME: Stop any existing smooth-scrolls
    // FIXME: Implement smooth-scroll
    page.client().page_did_request_scroll_to({ x, y });
}

// https://www.w3.org/TR/cssom-view/#dom-window-scroll
JS_DEFINE_NATIVE_FUNCTION(WindowObject::scroll)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!impl->page())
        return JS::js_undefined();
    auto& page = *impl->page();

    auto viewport_rect = page.top_level_browsing_context().viewport_rect();
    auto x_value = JS::Value(viewport_rect.x());
    auto y_value = JS::Value(viewport_rect.y());
    String behavior_string = "auto";

    if (vm.argument_count() == 1) {
        auto* options = TRY(vm.argument(0).to_object(global_object));
        auto left = TRY(options->get("left"));
        if (!left.is_undefined())
            x_value = left;

        auto top = TRY(options->get("top"));
        if (!top.is_undefined())
            y_value = top;

        auto behavior_string_value = TRY(options->get("behavior"));
        if (!behavior_string_value.is_undefined())
            behavior_string = TRY(behavior_string_value.to_string(global_object));
        if (behavior_string != "smooth" && behavior_string != "auto")
            return vm.throw_completion<JS::TypeError>(global_object, "Behavior is not one of 'smooth' or 'auto'");

    } else if (vm.argument_count() >= 2) {
        // We ignore arguments 2+ in line with behavior of Chrome and Firefox
        x_value = vm.argument(0);
        y_value = vm.argument(1);
    }

    ScrollBehavior behavior = (behavior_string == "smooth") ? ScrollBehavior::Smooth : ScrollBehavior::Auto;

    double x = TRY(x_value.to_double(global_object));
    x = JS::Value(x).is_finite_number() ? x : 0.0;

    double y = TRY(y_value.to_double(global_object));
    y = JS::Value(y).is_finite_number() ? y : 0.0;

    // FIXME: Are we calculating the viewport in the way this function expects?
    // FIXME: Handle overflow-directions other than top-left to bottom-right

    perform_a_scroll(page, x, y, behavior);
    return JS::js_undefined();
}

// https://www.w3.org/TR/cssom-view/#dom-window-scrollby
JS_DEFINE_NATIVE_FUNCTION(WindowObject::scroll_by)
{
    auto* impl = TRY(impl_from(vm, global_object));
    if (!impl->page())
        return JS::js_undefined();
    auto& page = *impl->page();

    JS::Object* options = nullptr;

    if (vm.argument_count() == 0) {
        options = JS::Object::create(global_object, nullptr);
    } else if (vm.argument_count() == 1) {
        options = TRY(vm.argument(0).to_object(global_object));
    } else if (vm.argument_count() >= 2) {
        // We ignore arguments 2+ in line with behavior of Chrome and Firefox
        options = JS::Object::create(global_object, nullptr);
        MUST(options->set("left", vm.argument(0), ShouldThrowExceptions::No));
        MUST(options->set("top", vm.argument(1), ShouldThrowExceptions::No));
        MUST(options->set("behavior", JS::js_string(vm, "auto"), ShouldThrowExceptions::No));
    }

    auto left_value = TRY(options->get("left"));
    auto left = TRY(left_value.to_double(global_object));

    auto top_value = TRY(options->get("top"));
    auto top = TRY(top_value.to_double(global_object));

    left = JS::Value(left).is_finite_number() ? left : 0.0;
    top = JS::Value(top).is_finite_number() ? top : 0.0;

    auto current_scroll_position = page.top_level_browsing_context().viewport_scroll_offset();
    left = left + current_scroll_position.x();
    top = top + current_scroll_position.y();

    auto behavior_string_value = TRY(options->get("behavior"));
    auto behavior_string = behavior_string_value.is_undefined() ? "auto" : TRY(behavior_string_value.to_string(global_object));
    if (behavior_string != "smooth" && behavior_string != "auto")
        return vm.throw_completion<JS::TypeError>(global_object, "Behavior is not one of 'smooth' or 'auto'");
    ScrollBehavior behavior = (behavior_string == "smooth") ? ScrollBehavior::Smooth : ScrollBehavior::Auto;

    // FIXME: Spec wants us to call scroll(options) here.
    //        The only difference is that would invoke the viewport calculations that scroll()
    //        is not actually doing yet, so this is the same for now.
    perform_a_scroll(page, left, top, behavior);
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::history_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return wrap(global_object, impl->associated_document().history());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::screen_left_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->screen_x());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::screen_top_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->screen_y());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::screen_x_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->screen_x());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::screen_y_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::Value(impl->screen_y());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::post_message)
{
    auto* impl = TRY(impl_from(vm, global_object));
    auto target_origin = TRY(vm.argument(1).to_string(global_object));
    impl->post_message(vm.argument(0), target_origin);
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-origin
JS_DEFINE_NATIVE_FUNCTION(WindowObject::origin_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::js_string(vm, impl->associated_document().origin().serialize());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::local_storage_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    // FIXME: localStorage may throw. We have to deal with that here.
    return wrap(global_object, *impl->local_storage());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::session_storage_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    // FIXME: sessionStorage may throw. We have to deal with that here.
    return wrap(global_object, *impl->session_storage());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::name_getter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return JS::js_string(vm, impl->name());
}

JS_DEFINE_NATIVE_FUNCTION(WindowObject::name_setter)
{
    auto* impl = TRY(impl_from(vm, global_object));
    impl->set_name(TRY(vm.argument(0).to_string(global_object)));
    return JS::js_undefined();
}

#define __ENUMERATE(attribute, event_name)                                                                                 \
    JS_DEFINE_NATIVE_FUNCTION(WindowObject::attribute##_getter)                                                            \
    {                                                                                                                      \
        auto* impl = TRY(impl_from(vm, global_object));                                                                    \
        auto retval = impl->attribute();                                                                                   \
        if (!retval)                                                                                                       \
            return JS::js_null();                                                                                          \
        return retval->callback.cell();                                                                                    \
    }                                                                                                                      \
    JS_DEFINE_NATIVE_FUNCTION(WindowObject::attribute##_setter)                                                            \
    {                                                                                                                      \
        auto* impl = TRY(impl_from(vm, global_object));                                                                    \
        auto value = vm.argument(0);                                                                                       \
        Optional<Bindings::CallbackType> cpp_value;                                                                        \
        if (value.is_object()) {                                                                                           \
            cpp_value = Bindings::CallbackType { JS::make_handle(&value.as_object()), HTML::incumbent_settings_object() }; \
        }                                                                                                                  \
        impl->set_##attribute(cpp_value);                                                                                  \
        return JS::js_undefined();                                                                                         \
    }
ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
ENUMERATE_WINDOW_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
