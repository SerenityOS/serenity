/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGAnimatedLength.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGLineElement final : public SVGGeometryElement {
public:
    using WrapperType = Bindings::SVGLineElementWrapper;

    SVGLineElement(DOM::Document&, DOM::QualifiedName);
    virtual ~SVGLineElement() override = default;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual Gfx::Path& get_path() override;

    NonnullRefPtr<SVGAnimatedLength> x1() const;
    NonnullRefPtr<SVGAnimatedLength> y1() const;
    NonnullRefPtr<SVGAnimatedLength> x2() const;
    NonnullRefPtr<SVGAnimatedLength> y2() const;

private:
    Optional<Gfx::Path> m_path;

    Optional<float> m_x1;
    Optional<float> m_y1;
    Optional<float> m_x2;
    Optional<float> m_y2;
};

}
