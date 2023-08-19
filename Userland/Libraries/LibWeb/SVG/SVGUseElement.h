/*
 * Copyright (c) 2023, Preston Taylor <95388976+PrestonLTaylor@users.noreply.github.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Path.h>
#include <LibWeb/DOM/DocumentObserver.h>
#include <LibWeb/SVG/SVGAnimatedLength.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

class SVGUseElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGUseElement, SVGGraphicsElement);

public:
    virtual ~SVGUseElement() override = default;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    virtual void inserted() override;

    void svg_element_changed(SVGElement&);
    void svg_element_removed(SVGElement&);

    JS::NonnullGCPtr<SVGAnimatedLength> x() const;
    JS::NonnullGCPtr<SVGAnimatedLength> y() const;
    JS::NonnullGCPtr<SVGAnimatedLength> width() const;
    JS::NonnullGCPtr<SVGAnimatedLength> height() const;

    JS::GCPtr<SVGElement> instance_root() const;
    JS::GCPtr<SVGElement> animated_instance_root() const;

private:
    SVGUseElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual bool is_svg_use_element() const override { return true; }

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    Optional<StringView> parse_id_from_href(DeprecatedString const& href);

    JS::GCPtr<DOM::Element> referenced_element();

    void clone_element_tree_as_our_shadow_tree(Element* to_clone) const;
    bool is_valid_reference_element(Element* reference_element) const;

    Optional<float> m_x;
    Optional<float> m_y;

    Optional<StringView> m_referenced_id;

    JS::GCPtr<DOM::DocumentObserver> m_document_observer;
};

}

namespace Web::DOM {

template<>
inline bool Node::fast_is<SVG::SVGUseElement>() const { return is_svg_use_element(); }

}
