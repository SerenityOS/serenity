/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/SVGAnimatedEnumerationPrototype.h>
#include <LibWeb/SVG/SVGAnimatedEnumeration.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGAnimatedEnumeration);

JS::NonnullGCPtr<SVGAnimatedEnumeration> SVGAnimatedEnumeration::create(JS::Realm& realm, WebIDL::UnsignedShort base_val, WebIDL::UnsignedShort anim_val)
{
    return realm.heap().allocate<SVGAnimatedEnumeration>(realm, realm, base_val, anim_val);
}

SVGAnimatedEnumeration::SVGAnimatedEnumeration(JS::Realm& realm, WebIDL::UnsignedShort base_val, WebIDL::UnsignedShort anim_val)
    : PlatformObject(realm)
    , m_base_val(base_val)
    , m_anim_val(anim_val)
{
}

SVGAnimatedEnumeration::~SVGAnimatedEnumeration() = default;

void SVGAnimatedEnumeration::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(SVGAnimatedEnumeration);
}

WebIDL::ExceptionOr<void> SVGAnimatedEnumeration::set_base_val(WebIDL::UnsignedShort base_val)
{
    // 1. Let value be the value being assigned to baseVal.
    auto value = base_val;

    // 2. If value is 0 or is not the numeric type value for any value of the reflected attribute, then throw a TypeError.
    // FIXME: is not the numeric type value for any value of the reflected attribute
    if (value == 0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Value is 0 or is not the numeric type value for any value of the reflected attribute,"sv };

    // FIXME: 3. Otherwise, if the reflecting IDL attribute is orientType and value is SVG_MARKER_ORIENT_ANGLE, then set the reflected attribute to the string "0".

    // 4. Otherwise, value is the numeric type value for a specific, single keyword value for the reflected attribute. Set the reflected attribute to that value.
    m_base_val = value;

    return {};
}

}
