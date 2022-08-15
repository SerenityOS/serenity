/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/CrossOriginAbstractOperations.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class LocationObject final : public JS::Object {
    JS_OBJECT(LocationObject, JS::Object);

public:
    explicit LocationObject(JS::Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~LocationObject() override = default;

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

    CrossOriginPropertyDescriptorMap const& cross_origin_property_descriptor_map() const { return m_cross_origin_property_descriptor_map; }
    CrossOriginPropertyDescriptorMap& cross_origin_property_descriptor_map() { return m_cross_origin_property_descriptor_map; }

private:
    DOM::Document const* relevant_document() const;
    AK::URL url() const;

    JS_DECLARE_NATIVE_FUNCTION(reload);
    JS_DECLARE_NATIVE_FUNCTION(replace);

    JS_DECLARE_NATIVE_FUNCTION(href_getter);
    JS_DECLARE_NATIVE_FUNCTION(href_setter);

    JS_DECLARE_NATIVE_FUNCTION(host_getter);
    JS_DECLARE_NATIVE_FUNCTION(hostname_getter);
    JS_DECLARE_NATIVE_FUNCTION(pathname_getter);
    JS_DECLARE_NATIVE_FUNCTION(hash_getter);
    JS_DECLARE_NATIVE_FUNCTION(search_getter);
    JS_DECLARE_NATIVE_FUNCTION(protocol_getter);
    JS_DECLARE_NATIVE_FUNCTION(port_getter);

    // [[CrossOriginPropertyDescriptorMap]], https://html.spec.whatwg.org/multipage/browsers.html#crossoriginpropertydescriptormap
    CrossOriginPropertyDescriptorMap m_cross_origin_property_descriptor_map;

    // [[DefaultProperties]], https://html.spec.whatwg.org/multipage/history.html#defaultproperties
    JS::MarkedVector<JS::Value> m_default_properties;
};

}
}
