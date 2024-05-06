/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/SVGTransformPrototype.h>
#include <LibWeb/SVG/SVGTransform.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGTransform);

JS::NonnullGCPtr<SVGTransform> SVGTransform::create(JS::Realm& realm)
{
    return realm.heap().allocate<SVGTransform>(realm, realm);
}

SVGTransform::SVGTransform(JS::Realm& realm)
    : PlatformObject(realm) {};

SVGTransform::~SVGTransform() = default;

void SVGTransform::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(SVGTransform);
}

// https://svgwg.org/svg2-draft/single-page.html#coords-__svg__SVGTransform__type
SVGTransform::Type SVGTransform::type()
{
    dbgln("FIXME: Implement SVGTransform::type()");
    return SVGTransform::Type::Unknown;
}

}
