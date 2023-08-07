/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

class SVGPolylineElement final : public SVGGeometryElement {
    WEB_PLATFORM_OBJECT(SVGPolylineElement, SVGGeometryElement);

public:
    virtual ~SVGPolylineElement() override = default;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual Gfx::Path& get_path() override;

private:
    SVGPolylineElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    Optional<Gfx::Path> m_path;

    Vector<Gfx::FloatPoint> m_points;
};

}
