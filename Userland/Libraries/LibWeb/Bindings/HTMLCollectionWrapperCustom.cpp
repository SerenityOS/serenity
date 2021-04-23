/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibWeb/Bindings/HTMLCollectionWrapper.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/Element.h>

namespace Web::Bindings {

JS::Value HTMLCollectionWrapper::get(JS::PropertyName const& name, JS::Value receiver, bool without_side_effects) const
{
    if (!name.is_string())
        return Base::get(name, receiver, without_side_effects);
    auto* item = const_cast<DOM::HTMLCollection&>(impl()).named_item(name.to_string());
    if (!item)
        return Base::get(name, receiver, without_side_effects);
    return JS::Value { wrap(global_object(), *item) };
}

JS::Value HTMLCollectionWrapper::get_by_index(u32 property_index) const
{
    auto* item = const_cast<DOM::HTMLCollection&>(impl()).item(property_index);
    if (!item)
        return Base::get_by_index(property_index);
    return wrap(global_object(), *item);
}

}
