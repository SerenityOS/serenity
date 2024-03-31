/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::SVG {

// FIXME: This class is just a stub.
// https://svgwg.org/svg2-draft/single-page.html#coords-InterfaceSVGTransform
class SVGTransform final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGTransform, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SVGTransform);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SVGTransform> create(JS::Realm& realm);
    virtual ~SVGTransform() override;

private:
    SVGTransform(JS::Realm& realm);

    virtual void initialize(JS::Realm& realm) override;
};

}
