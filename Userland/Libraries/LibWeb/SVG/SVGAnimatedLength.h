/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG11/types.html#InterfaceSVGAnimatedLength
class SVGAnimatedLength final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGAnimatedLength, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<SVGAnimatedLength> create(HTML::Window&, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val);
    virtual ~SVGAnimatedLength() override;

    JS::NonnullGCPtr<SVGLength> base_val() const { return m_base_val; }
    JS::NonnullGCPtr<SVGLength> anim_val() const { return m_anim_val; }

private:
    SVGAnimatedLength(HTML::Window&, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<SVGLength> m_base_val;
    JS::NonnullGCPtr<SVGLength> m_anim_val;
};

}
