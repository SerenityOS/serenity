/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGMaskElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGMaskElement, SVGGraphicsElement);

public:
    virtual ~SVGMaskElement() override;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    CSSPixelRect resolve_masking_area(CSSPixelRect const& mask_target) const;

    MaskContentUnits mask_content_units() const;
    MaskUnits mask_units() const;

private:
    SVGMaskElement(DOM::Document&, DOM::QualifiedName);
    virtual void initialize(JS::Realm&) override;

    Optional<MaskContentUnits> m_mask_content_units = {};
    Optional<MaskUnits> m_mask_units = {};
};

}
