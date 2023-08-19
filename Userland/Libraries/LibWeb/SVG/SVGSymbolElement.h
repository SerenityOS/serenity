/*
 * Copyright (c) 2023, Preston Taylor <95388976+PrestonLTaylor@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGSymbolElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGSymbolElement, SVGGraphicsElement);

public:
    virtual ~SVGSymbolElement() override = default;

    void apply_presentational_hints(CSS::StyleProperties& style) const override;

    Optional<ViewBox> view_box() const { return m_view_box; }

private:
    SVGSymbolElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    bool is_direct_child_of_use_shadow_tree() const;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    Optional<ViewBox> m_view_box;
};

}
