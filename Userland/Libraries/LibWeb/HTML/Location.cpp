/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/URLParser.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/PropertyDescriptor.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibWeb/Bindings/LocationPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/CrossOrigin/AbstractOperations.h>
#include <LibWeb/HTML/Location.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/history.html#the-location-interface
Location::Location(JS::Realm& realm)
    : PlatformObject(realm)
{
}

Location::~Location() = default;

void Location::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& property : m_default_properties)
        visitor.visit(property);
}

JS::ThrowCompletionOr<void> Location::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Object::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::LocationPrototype>(realm, "Location"));

    // FIXME: Implement steps 2.-4.

    // 5. Set the value of the [[DefaultProperties]] internal slot of location to location.[[OwnPropertyKeys]]().
    // NOTE: In LibWeb this happens before the ESO is set up, so we must avoid location's custom [[OwnPropertyKeys]].
    m_default_properties.extend(MUST(Object::internal_own_property_keys()));

    return {};
}

// https://html.spec.whatwg.org/multipage/history.html#relevant-document
JS::GCPtr<DOM::Document> Location::relevant_document() const
{
    // A Location object has an associated relevant Document, which is this Location object's
    // relevant global object's browsing context's active document, if this Location object's
    // relevant global object's browsing context is non-null, and null otherwise.
    auto* browsing_context = verify_cast<HTML::Window>(HTML::relevant_global_object(*this)).browsing_context();
    return browsing_context ? browsing_context->active_document() : nullptr;
}

// https://html.spec.whatwg.org/multipage/history.html#concept-location-url
AK::URL Location::url() const
{
    // A Location object has an associated url, which is this Location object's relevant Document's URL,
    // if this Location object's relevant Document is non-null, and about:blank otherwise.
    auto const relevant_document = this->relevant_document();
    return relevant_document ? relevant_document->url() : "about:blank"sv;
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-href
WebIDL::ExceptionOr<String> Location::href() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 2. Return this's url, serialized.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(url().serialize()));
}

// https://html.spec.whatwg.org/multipage/history.html#the-location-interface:dom-location-href-2
WebIDL::ExceptionOr<void> Location::set_href(String const& new_href)
{
    auto& vm = this->vm();
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());

    // 1. If this's relevant Document is null, then return.
    auto const relevant_document = this->relevant_document();
    if (!relevant_document)
        return {};

    // 2. Parse the given value relative to the entry settings object. If that failed, throw a TypeError exception.
    auto href_url = window.associated_document().parse_url(new_href.to_deprecated_string());
    if (!href_url.is_valid())
        return vm.throw_completion<JS::URIError>(TRY_OR_THROW_OOM(vm, String::formatted("Invalid URL '{}'", new_href)));

    // 3. Location-object navigate given the resulting URL record.
    window.did_set_location_href({}, href_url);

    return {};
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-location-origin
WebIDL::ExceptionOr<String> Location::origin() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 2. Return the serialization of this's url's origin.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(url().serialize_origin()));
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-protocol
WebIDL::ExceptionOr<String> Location::protocol() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 2. Return this's url's scheme, followed by ":".
    return TRY_OR_THROW_OOM(vm, String::formatted("{}:", url().scheme()));
}

WebIDL::ExceptionOr<void> Location::set_protocol(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.protocol setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-host
WebIDL::ExceptionOr<String> Location::host() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 2. Let url be this's url.
    auto url = this->url();

    // 3. If url's host is null, return the empty string.
    if (url.host().has<Empty>())
        return String {};

    // 4. If url's port is null, return url's host, serialized.
    if (!url.port().has_value())
        return TRY_OR_THROW_OOM(vm, url.serialized_host());

    // 5. Return url's host, serialized, followed by ":" and url's port, serialized.
    return TRY_OR_THROW_OOM(vm, String::formatted("{}:{}", TRY_OR_THROW_OOM(vm, url.serialized_host()), *url.port()));
}

WebIDL::ExceptionOr<void> Location::set_host(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.host setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-hostname
WebIDL::ExceptionOr<String> Location::hostname() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    auto url = this->url();

    // 2. If this's url's host is null, return the empty string.
    if (url.host().has<Empty>())
        return String {};

    // 3. Return this's url's host, serialized.
    return TRY_OR_THROW_OOM(vm, url.serialized_host());
}

WebIDL::ExceptionOr<void> Location::set_hostname(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.hostname setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-port
WebIDL::ExceptionOr<String> Location::port() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    auto url = this->url();

    // 2. If this's url's port is null, return the empty string.
    if (!url.port().has_value())
        return String {};

    // 3. Return this's url's port, serialized.
    return TRY_OR_THROW_OOM(vm, String::number(*url.port()));
}

WebIDL::ExceptionOr<void> Location::set_port(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.port setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-pathname
WebIDL::ExceptionOr<String> Location::pathname() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 2. Return the result of URL path serializing this Location object's url.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(url().serialize_path()));
}

WebIDL::ExceptionOr<void> Location::set_pathname(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.pathname setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-search
WebIDL::ExceptionOr<String> Location::search() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    auto url = this->url();

    // 2. If this's url's query is either null or the empty string, return the empty string.
    if (url.query().is_empty())
        return String {};

    // 3. Return "?", followed by this's url's query.
    return TRY_OR_THROW_OOM(vm, String::formatted("?{}", url.query()));
}

WebIDL::ExceptionOr<void> Location::set_search(String const&)
{
    auto& vm = this->vm();
    return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotImplemented, "Location.search setter");
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-hash
WebIDL::ExceptionOr<String> Location::hash() const
{
    auto& vm = this->vm();

    // 1. If this's relevant Document is non-null and its origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    auto const relevant_document = this->relevant_document();
    if (relevant_document && !relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    auto url = this->url();

    // 2. If this's url's fragment is either null or the empty string, return the empty string.
    if (url.fragment().is_empty())
        return String {};

    // 3. Return "#", followed by this's url's fragment.
    return TRY_OR_THROW_OOM(vm, String::formatted("#{}", url.fragment()));
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-location-hash
WebIDL::ExceptionOr<void> Location::set_hash(String const& value)
{
    // The hash setter steps are:
    auto const relevant_document = this->relevant_document();

    // 1. If this's relevant Document is null, then return.
    if (!relevant_document)
        return {};

    // 2. If this's relevant Document's origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    if (!relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 3. Let copyURL be a copy of this's url.
    auto copy_url = this->url();

    // 4. Let input be the given value with a single leading "#" removed, if any.
    auto input = value.bytes_as_string_view().trim("#"sv, TrimMode::Left);

    // 5. Set copyURL's fragment to the empty string.
    copy_url.set_fragment("");

    // 6. Basic URL parse input, with copyURL as url and fragment state as state override.
    auto result_url = URLParser::basic_parse(input, {}, copy_url, URLParser::State::Fragment);

    // 7. If copyURL's fragment is this's url's fragment, then return.
    if (copy_url.fragment() == this->url().fragment())
        return {};

    // 8. Location-object navigate this to copyURL.
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    window.did_set_location_href({}, copy_url);

    return {};
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-reload
void Location::reload() const
{
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    window.did_call_location_reload({});
}

// https://html.spec.whatwg.org/multipage/history.html#dom-location-replace
void Location::replace(String const& url) const
{
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    // FIXME: This needs spec compliance work.
    window.did_call_location_replace({}, url.to_deprecated_string());
}

// https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-location-assign
WebIDL::ExceptionOr<void> Location::assign(String const& url) const
{
    // 1. If this's relevant Document is null, then return.
    auto const relevant_document = this->relevant_document();
    if (!relevant_document)
        return {};

    // 2. If this's relevant Document's origin is not same origin-domain with the entry settings object's origin, then throw a "SecurityError" DOMException.
    if (!relevant_document->origin().is_same_origin_domain(entry_settings_object().origin()))
        return WebIDL::SecurityError::create(realm(), "Location's relevant document is not same origin-domain with the entry settings object's origin"sv);

    // 3. Parse url relative to the entry settings object. If that failed, throw a "SyntaxError" DOMException.
    auto assign_url = entry_settings_object().parse_url(url);
    if (!assign_url.is_valid())
        return WebIDL::SyntaxError::create(realm(), DeprecatedString::formatted("Invalid URL '{}'", url));

    // 4. Location-object navigate this to the resulting URL record.
    auto& window = verify_cast<HTML::Window>(HTML::current_global_object());
    window.did_set_location_href({}, assign_url);
    return {};
}

// 7.10.5.1 [[GetPrototypeOf]] ( ), https://html.spec.whatwg.org/multipage/history.html#location-getprototypeof
JS::ThrowCompletionOr<JS::Object*> Location::internal_get_prototype_of() const
{
    // 1. If IsPlatformObjectSameOrigin(this) is true, then return ! OrdinaryGetPrototypeOf(this).
    if (HTML::is_platform_object_same_origin(*this))
        return MUST(JS::Object::internal_get_prototype_of());

    // 2. Return null.
    return nullptr;
}

// 7.10.5.2 [[SetPrototypeOf]] ( V ), https://html.spec.whatwg.org/multipage/history.html#location-setprototypeof
JS::ThrowCompletionOr<bool> Location::internal_set_prototype_of(Object* prototype)
{
    // 1. Return ! SetImmutablePrototype(this, V).
    return MUST(set_immutable_prototype(prototype));
}

// 7.10.5.3 [[IsExtensible]] ( ), https://html.spec.whatwg.org/multipage/history.html#location-isextensible
JS::ThrowCompletionOr<bool> Location::internal_is_extensible() const
{
    // 1. Return true.
    return true;
}

// 7.10.5.4 [[PreventExtensions]] ( ), https://html.spec.whatwg.org/multipage/history.html#location-preventextensions
JS::ThrowCompletionOr<bool> Location::internal_prevent_extensions()
{
    // 1. Return false.
    return false;
}

// 7.10.5.5 [[GetOwnProperty]] ( P ), https://html.spec.whatwg.org/multipage/history.html#location-getownproperty
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> Location::internal_get_own_property(JS::PropertyKey const& property_key) const
{
    auto& vm = this->vm();

    // 1. If IsPlatformObjectSameOrigin(this) is true, then:
    if (HTML::is_platform_object_same_origin(*this)) {
        // 1. Let desc be OrdinaryGetOwnProperty(this, P).
        auto descriptor = MUST(Object::internal_get_own_property(property_key));

        // 2. If the value of the [[DefaultProperties]] internal slot of this contains P, then set desc.[[Configurable]] to true.
        auto property_key_value = property_key.is_symbol()
            ? JS::Value { property_key.as_symbol() }
            : JS::PrimitiveString::create(vm, property_key.to_string());
        if (m_default_properties.contains_slow(property_key_value))
            descriptor->configurable = true;

        // 3. Return desc.
        return descriptor;
    }

    // 2. Let property be CrossOriginGetOwnPropertyHelper(this, P).
    auto property = HTML::cross_origin_get_own_property_helper(const_cast<Location*>(this), property_key);

    // 3. If property is not undefined, then return property.
    if (property.has_value())
        return property;

    // 4. Return ? CrossOriginPropertyFallback(P).
    return TRY(HTML::cross_origin_property_fallback(vm, property_key));
}

// 7.10.5.6 [[DefineOwnProperty]] ( P, Desc ), https://html.spec.whatwg.org/multipage/history.html#location-defineownproperty
JS::ThrowCompletionOr<bool> Location::internal_define_own_property(JS::PropertyKey const& property_key, JS::PropertyDescriptor const& descriptor)
{
    // 1. If IsPlatformObjectSameOrigin(this) is true, then:
    if (HTML::is_platform_object_same_origin(*this)) {
        // 1. If the value of the [[DefaultProperties]] internal slot of this contains P, then return false.
        // 2. Return ? OrdinaryDefineOwnProperty(this, P, Desc).
        return JS::Object::internal_define_own_property(property_key, descriptor);
    }

    // 2. Throw a "SecurityError" DOMException.
    return throw_completion(WebIDL::SecurityError::create(realm(), DeprecatedString::formatted("Can't define property '{}' on cross-origin object", property_key)));
}

// 7.10.5.7 [[Get]] ( P, Receiver ), https://html.spec.whatwg.org/multipage/history.html#location-get
JS::ThrowCompletionOr<JS::Value> Location::internal_get(JS::PropertyKey const& property_key, JS::Value receiver, JS::CacheablePropertyMetadata* cacheable_metadata) const
{
    auto& vm = this->vm();

    // 1. If IsPlatformObjectSameOrigin(this) is true, then return ? OrdinaryGet(this, P, Receiver).
    if (HTML::is_platform_object_same_origin(*this))
        return JS::Object::internal_get(property_key, receiver, cacheable_metadata);

    // 2. Return ? CrossOriginGet(this, P, Receiver).
    return HTML::cross_origin_get(vm, static_cast<JS::Object const&>(*this), property_key, receiver);
}

// 7.10.5.8 [[Set]] ( P, V, Receiver ), https://html.spec.whatwg.org/multipage/history.html#location-set
JS::ThrowCompletionOr<bool> Location::internal_set(JS::PropertyKey const& property_key, JS::Value value, JS::Value receiver)
{
    auto& vm = this->vm();

    // 1. If IsPlatformObjectSameOrigin(this) is true, then return ? OrdinarySet(this, P, V, Receiver).
    if (HTML::is_platform_object_same_origin(*this))
        return JS::Object::internal_set(property_key, value, receiver);

    // 2. Return ? CrossOriginSet(this, P, V, Receiver).
    return HTML::cross_origin_set(vm, static_cast<JS::Object&>(*this), property_key, value, receiver);
}

// 7.10.5.9 [[Delete]] ( P ), https://html.spec.whatwg.org/multipage/history.html#location-delete
JS::ThrowCompletionOr<bool> Location::internal_delete(JS::PropertyKey const& property_key)
{
    // 1. If IsPlatformObjectSameOrigin(this) is true, then return ? OrdinaryDelete(this, P).
    if (HTML::is_platform_object_same_origin(*this))
        return JS::Object::internal_delete(property_key);

    // 2. Throw a "SecurityError" DOMException.
    return throw_completion(WebIDL::SecurityError::create(realm(), DeprecatedString::formatted("Can't delete property '{}' on cross-origin object", property_key)));
}

// 7.10.5.10 [[OwnPropertyKeys]] ( ), https://html.spec.whatwg.org/multipage/history.html#location-ownpropertykeys
JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> Location::internal_own_property_keys() const
{
    // 1. If IsPlatformObjectSameOrigin(this) is true, then return OrdinaryOwnPropertyKeys(this).
    if (HTML::is_platform_object_same_origin(*this))
        return JS::Object::internal_own_property_keys();

    // 2. Return CrossOriginOwnPropertyKeys(this).
    return HTML::cross_origin_own_property_keys(this);
}

}
