/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/SVGAnimatedBooleanPrototype.h>
#include <LibWeb/SVG/SVGAnimatedBoolean.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGAnimatedBoolean);

JS::NonnullGCPtr<SVGAnimatedBoolean> SVGAnimatedBoolean::create(JS::Realm& realm, bool base_val, bool anim_val)
{
    return realm.heap().allocate<SVGAnimatedBoolean>(realm, realm, base_val, anim_val);
}

SVGAnimatedBoolean::SVGAnimatedBoolean(JS::Realm& realm, bool base_val, bool anim_val)
    : PlatformObject(realm)
    , m_base_val(base_val)
    , m_anim_val(anim_val)
{
}

SVGAnimatedBoolean::~SVGAnimatedBoolean() = default;

void SVGAnimatedBoolean::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(SVGAnimatedBoolean);
}

}
