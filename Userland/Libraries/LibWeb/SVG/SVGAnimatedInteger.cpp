/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/SVGAnimatedIntegerPrototype.h>
#include <LibWeb/SVG/SVGAnimatedInteger.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGAnimatedInteger);

JS::NonnullGCPtr<SVGAnimatedInteger> SVGAnimatedInteger::create(JS::Realm& realm, u32 base_val, u32 anim_val)
{
    return realm.heap().allocate<SVGAnimatedInteger>(realm, realm, base_val, anim_val);
}

SVGAnimatedInteger::SVGAnimatedInteger(JS::Realm& realm, u32 base_val, u32 anim_val)
    : PlatformObject(realm)
    , m_base_val(base_val)
    , m_anim_val(anim_val)
{
}

SVGAnimatedInteger::~SVGAnimatedInteger() = default;

void SVGAnimatedInteger::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(SVGAnimatedInteger);
}

}
