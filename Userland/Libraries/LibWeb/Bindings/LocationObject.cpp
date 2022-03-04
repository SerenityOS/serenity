/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <AK/StringBuilder.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>

namespace Web::Bindings {

// https://html.spec.whatwg.org/multipage/history.html#the-location-interface
LocationObject::LocationObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void LocationObject::initialize(JS::GlobalObject& global_object)
{
    auto& vm = global_object.vm();

    Object::initialize(global_object);
    u8 attr = JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_accessor("href", href_getter, href_setter, attr);
    define_native_accessor("host", host_getter, {}, attr);
    define_native_accessor("hostname", hostname_getter, {}, attr);
    define_native_accessor("pathname", pathname_getter, {}, attr);
    define_native_accessor("hash", hash_getter, {}, attr);
    define_native_accessor("search", search_getter, {}, attr);
    define_native_accessor("protocol", protocol_getter, {}, attr);
    define_native_accessor("port", port_getter, {}, attr);

    define_native_function("reload", reload, 0, JS::Attribute::Enumerable);
    define_native_function("replace", replace, 1, JS::Attribute::Enumerable);

    define_native_function(vm.names.toString, href_getter, 0, JS::Attribute::Enumerable);
}

LocationObject::~LocationObject()
{
}

// https://html.spec.whatwg.org/multipage/history.html#relevant-document
DOM::Document const* LocationObject::relevant_document() const
{
    // A Location object has an associated relevant Document, which is this Location object's
    // relevant global object's browsing context's active document, if this Location object's
    // relevant global object's browsing context is non-null, and null otherwise.
    auto* browsing_context = static_cast<WindowObject&>(global_object()).impl().browsing_context();
    return browsing_context ? browsing_context->active_document() : nullptr;
}

// https://html.spec.whatwg.org/multipage/history.html#concept-location-url
AK::URL LocationObject::url() const
{
    // A Location object has an associated url, which is this Location object's relevant Document's URL,
    // if this Location object's relevant Document is non-null, and about:blank otherwise.
    auto const* relevant_document = this->relevant_document();
    return relevant_document ? relevant_document->url() : "about:blank"sv;
}

static JS::ThrowCompletionOr<LocationObject*> typed_this_value(JS::GlobalObject& global_object)
{
    auto& vm = global_object.vm();
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !is<LocationObject>(this_value.as_object()))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Location");
    return static_cast<LocationObject*>(&this_value.as_object());
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-href
JS_DEFINE_NATIVE_FUNCTION(LocationObject::href_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. Return this's url, serialized.
    return JS::js_string(vm, location_object->url().to_string());
}

// https://html.spec.whatwg.org/multipage/history.html#the-location-interface:dom-location-href-2
JS_DEFINE_NATIVE_FUNCTION(LocationObject::href_setter)
{
    auto& window = static_cast<WindowObject&>(global_object);

    // FIXME: 1. If this's relevant Document is null, then return.

    // 2. Parse the given value relative to the entry settings object. If that failed, throw a TypeError exception.
    auto new_href = TRY(vm.argument(0).to_string(global_object));
    auto href_url = window.impl().associated_document().parse_url(new_href);
    if (!href_url.is_valid())
        return vm.throw_completion<JS::URIError>(global_object, String::formatted("Invalid URL '{}'", new_href));

    // 3. Location-object navigate given the resulting URL record.
    window.impl().did_set_location_href({}, href_url);

    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-pathname
JS_DEFINE_NATIVE_FUNCTION(LocationObject::pathname_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. Return the result of URL path serializing this Location object's url.
    return JS::js_string(vm, location_object->url().path());
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-hostname
JS_DEFINE_NATIVE_FUNCTION(LocationObject::hostname_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. If this's url's host is null, return the empty string.
    if (location_object->url().host().is_null())
        return JS::js_string(vm, String::empty());

    // 3. Return this's url's host, serialized.
    return JS::js_string(vm, location_object->url().host());
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-host
JS_DEFINE_NATIVE_FUNCTION(LocationObject::host_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. Let url be this's url.
    auto url = location_object->url();

    // 3. If url's host is null, return the empty string.
    if (url.host().is_null())
        return JS::js_string(vm, String::empty());

    // 4. If url's port is null, return url's host, serialized.
    if (!url.port().has_value())
        return JS::js_string(vm, url.host());

    // 5. Return url's host, serialized, followed by ":" and url's port, serialized.
    return JS::js_string(vm, String::formatted("{}:{}", url.host(), *url.port()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-hash
JS_DEFINE_NATIVE_FUNCTION(LocationObject::hash_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. If this's url's fragment is either null or the empty string, return the empty string.
    if (location_object->url().fragment().is_empty())
        return JS::js_string(vm, String::empty());

    // 3. Return "#", followed by this's url's fragment.
    return JS::js_string(vm, String::formatted("#{}", location_object->url().fragment()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-search
JS_DEFINE_NATIVE_FUNCTION(LocationObject::search_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. If this's url's query is either null or the empty string, return the empty string.
    if (location_object->url().query().is_empty())
        return JS::js_string(vm, String::empty());

    // 3. Return "?", followed by this's url's query.
    return JS::js_string(vm, String::formatted("?{}", location_object->url().query()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-protocol
JS_DEFINE_NATIVE_FUNCTION(LocationObject::protocol_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. Return this's url's scheme, followed by ":".
    return JS::js_string(vm, String::formatted("{}:", location_object->url().protocol()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-port
JS_DEFINE_NATIVE_FUNCTION(LocationObject::port_getter)
{
    auto* location_object = TRY(typed_this_value(global_object));

    // FIXME: 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.

    // 2. If this's url's port is null, return the empty string.
    if (!location_object->url().port().has_value())
        return JS::js_string(vm, String::empty());

    // 3. Return this's url's port, serialized.
    return JS::js_string(vm, String::number(*location_object->url().port()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-reload
JS_DEFINE_NATIVE_FUNCTION(LocationObject::reload)
{
    auto& window = static_cast<WindowObject&>(global_object);
    window.impl().did_call_location_reload({});
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-replace
JS_DEFINE_NATIVE_FUNCTION(LocationObject::replace)
{
    auto& window = static_cast<WindowObject&>(global_object);
    auto url = TRY(vm.argument(0).to_string(global_object));
    // FIXME: This needs spec compliance work.
    window.impl().did_call_location_replace({}, move(url));
    return JS::js_undefined();
}

// https://html.spec.whatwg.org/multipage/history.html#location-setprototypeof
JS::ThrowCompletionOr<bool> LocationObject::internal_set_prototype_of(Object* prototype)
{
    // 1. Return ! SetImmutablePrototype(this, V).
    return MUST(set_immutable_prototype(prototype));
}

// https://html.spec.whatwg.org/multipage/history.html#location-isextensible
JS::ThrowCompletionOr<bool> LocationObject::internal_is_extensible() const
{
    // 1. Return true.
    return true;
}

// https://html.spec.whatwg.org/multipage/history.html#location-preventextensions
JS::ThrowCompletionOr<bool> LocationObject::internal_prevent_extensions()
{
    // 1. Return false.
    return false;
}

}
