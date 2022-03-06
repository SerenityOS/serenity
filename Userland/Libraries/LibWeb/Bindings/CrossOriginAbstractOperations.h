/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Traits.h>
#include <LibJS/Forward.h>

namespace Web::Bindings {

struct CrossOriginKey {
    FlatPtr current_settings_object;
    FlatPtr relevant_settings_object;
    JS::PropertyKey property_key;
};

using CrossOriginPropertyDescriptorMap = HashMap<CrossOriginKey, JS::PropertyDescriptor>;

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
