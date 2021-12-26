/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Bindings {

bool CSSStyleDeclarationWrapper::internal_has_property(JS::PropertyName const& name) const
{
    if (!name.is_string())
        return Base::internal_has_property(name);
    // FIXME: These should actually use camelCase versions of the property names!
    auto property_id = CSS::property_id_from_string(name.to_string());
    return property_id != CSS::PropertyID::Invalid;
}

JS::Value CSSStyleDeclarationWrapper::internal_get(JS::PropertyName const& name, JS::Value receiver) const
{
    if (!name.is_string())
        return Base::internal_get(name, receiver);
    // FIXME: These should actually use camelCase versions of the property names!
    auto property_id = CSS::property_id_from_string(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_get(name, receiver);
    if (auto maybe_property = impl().property(property_id); maybe_property.has_value())
        return js_string(vm(), maybe_property->value->to_string());
    return js_string(vm(), String::empty());
}

bool CSSStyleDeclarationWrapper::internal_set(JS::PropertyName const& name, JS::Value value, JS::Value receiver)
{
    if (!name.is_string())
        return Base::internal_set(name, value, receiver);
    // FIXME: These should actually use camelCase versions of the property names!
    auto property_id = CSS::property_id_from_string(name.to_string());
    if (property_id == CSS::PropertyID::Invalid)
        return Base::internal_set(name, value, receiver);

    auto css_text = value.to_string(global_object());
    if (vm().exception())
        return false;

    impl().set_property(property_id, css_text);
    return true;
}

}
