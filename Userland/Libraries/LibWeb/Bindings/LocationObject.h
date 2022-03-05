/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Bindings/CrossOriginAbstractOperations.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class LocationObject final : public JS::Object {
    JS_OBJECT(LocationObject, JS::Object);

public:
    explicit LocationObject(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~LocationObject() override;

    virtual JS::ThrowCompletionOr<bool> internal_set_prototype_of(Object* prototype) override;
    virtual JS::ThrowCompletionOr<bool> internal_is_extensible() const override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;

    // FIXME: There should also be a custom [[GetPrototypeOf]], [[GetOwnProperty]], [[DefineOwnProperty]], [[Get]], [[Set]], [[Delete]] and [[OwnPropertyKeys]],
    //        but we don't have the infrastructure in place to implement them yet.

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
};

}
}
