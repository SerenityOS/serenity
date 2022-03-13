/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Traits.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

struct CrossOriginProperty {
    String property;
    Optional<bool> needs_get {};
    Optional<bool> needs_set {};
};

struct CrossOriginKey {
    FlatPtr current_settings_object;
    FlatPtr relevant_settings_object;
    JS::PropertyKey property_key;
};

using CrossOriginPropertyDescriptorMap = HashMap<CrossOriginKey, JS::PropertyDescriptor>;

Vector<CrossOriginProperty> cross_origin_properties(Variant<LocationObject const*, WindowObject const*> const&);
bool is_cross_origin_accessible_window_property_name(JS::PropertyKey const&);
JS::ThrowCompletionOr<JS::PropertyDescriptor> cross_origin_property_fallback(JS::GlobalObject&, JS::PropertyKey const&);
bool is_platform_object_same_origin(JS::Object const&);
Optional<JS::PropertyDescriptor> cross_origin_get_own_property_helper(Variant<LocationObject*, WindowObject*> const&, JS::PropertyKey const&);
JS::ThrowCompletionOr<JS::Value> cross_origin_get(JS::GlobalObject&, JS::Object const&, JS::PropertyKey const&, JS::Value receiver);
JS::ThrowCompletionOr<bool> cross_origin_set(JS::GlobalObject&, JS::Object&, JS::PropertyKey const&, JS::Value, JS::Value receiver);
JS::MarkedVector<JS::Value> cross_origin_own_property_keys(Variant<LocationObject const*, WindowObject const*> const&);

}

namespace AK {

template<>
struct Traits<Web::Bindings::CrossOriginKey> : public GenericTraits<Web::Bindings::CrossOriginKey> {
    static unsigned hash(Web::Bindings::CrossOriginKey const& key)
    {
        return pair_int_hash(
            Traits<JS::PropertyKey>::hash(key.property_key),
            pair_int_hash(ptr_hash(key.current_settings_object), ptr_hash(key.relevant_settings_object)));
    }

    static bool equals(Web::Bindings::CrossOriginKey const& a, Web::Bindings::CrossOriginKey const& b)
    {
        return a.current_settings_object == b.current_settings_object
            && a.relevant_settings_object == b.relevant_settings_object
            && Traits<JS::PropertyKey>::equals(a.property_key, b.property_key);
    }
};

}
