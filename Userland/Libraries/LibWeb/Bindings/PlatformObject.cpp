/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

PlatformObject::PlatformObject(JS::Realm& realm, MayInterfereWithIndexedPropertyAccess may_interfere_with_indexed_property_access)
    : JS::Object(realm, nullptr, may_interfere_with_indexed_property_access)
{
}

PlatformObject::PlatformObject(JS::Object& prototype, MayInterfereWithIndexedPropertyAccess may_interfere_with_indexed_property_access)
    : JS::Object(ConstructWithPrototypeTag::Tag, prototype, may_interfere_with_indexed_property_access)
{
}

PlatformObject::~PlatformObject() = default;

JS::Realm& PlatformObject::realm() const
{
    return shape().realm();
}

HTML::Window& PlatformObject::global_object() const
{
    return verify_cast<HTML::Window>(realm().global_object());
}

}
