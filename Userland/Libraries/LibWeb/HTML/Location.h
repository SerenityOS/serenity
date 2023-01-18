/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/CrossOrigin/CrossOriginPropertyDescriptorMap.h>

namespace Web::HTML {

class Location final : public Bindings::PlatformObject {
    JS_OBJECT(Location, Bindings::PlatformObject);

public:
    virtual ~Location() override;

    DeprecatedString href() const;
    JS::ThrowCompletionOr<void> set_href(DeprecatedString const&);

    DeprecatedString origin() const;

    DeprecatedString protocol() const;
    JS::ThrowCompletionOr<void> set_protocol(DeprecatedString const&);

    DeprecatedString host() const;
    JS::ThrowCompletionOr<void> set_host(DeprecatedString const&);

    DeprecatedString hostname() const;
    JS::ThrowCompletionOr<void> set_hostname(DeprecatedString const&);

    DeprecatedString port() const;
    JS::ThrowCompletionOr<void> set_port(DeprecatedString const&);

    DeprecatedString pathname() const;
    JS::ThrowCompletionOr<void> set_pathname(DeprecatedString const&);

    DeprecatedString search() const;
    JS::ThrowCompletionOr<void> set_search(DeprecatedString const&);

    DeprecatedString hash() const;
    JS::ThrowCompletionOr<void> set_hash(DeprecatedString const&);

    void replace(DeprecatedString url) const;
    void reload() const;

    virtual JS::ThrowCompletionOr<JS::Object*> internal_get_prototype_of() const override;
    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype) override;
    virtual JS::ThrowCompletionOr<bool> internal_is_extensible() const override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const&) const override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const&, JS::PropertyDescriptor const&) override;
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value receiver) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value value, JS::Value receiver) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const&) override;
    virtual JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> internal_own_property_keys() const override;

    HTML::CrossOriginPropertyDescriptorMap const& cross_origin_property_descriptor_map() const { return m_cross_origin_property_descriptor_map; }
    HTML::CrossOriginPropertyDescriptorMap& cross_origin_property_descriptor_map() { return m_cross_origin_property_descriptor_map; }

private:
    explicit Location(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    DOM::Document const* relevant_document() const;
    AK::URL url() const;

    // [[CrossOriginPropertyDescriptorMap]], https://html.spec.whatwg.org/multipage/browsers.html#crossoriginpropertydescriptormap
    HTML::CrossOriginPropertyDescriptorMap m_cross_origin_property_descriptor_map;

    // [[DefaultProperties]], https://html.spec.whatwg.org/multipage/history.html#defaultproperties
    Vector<JS::Value> m_default_properties;
};

}
