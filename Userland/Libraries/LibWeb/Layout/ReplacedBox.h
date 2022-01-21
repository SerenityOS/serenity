/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class ReplacedBox : public Box {
public:
    ReplacedBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~ReplacedBox() override;

    const DOM::Element& dom_node() const { return verify_cast<DOM::Element>(*Node::dom_node()); }
    DOM::Element& dom_node() { return verify_cast<DOM::Element>(*Node::dom_node()); }

    virtual Optional<float> intrinsic_width() const final { return m_intrinsic_width; }
    virtual Optional<float> intrinsic_height() const final { return m_intrinsic_height; }
    virtual Optional<float> intrinsic_aspect_ratio() const final { return m_intrinsic_aspect_ratio; }

    void set_intrinsic_width(Optional<float> width) { m_intrinsic_width = width; }
    void set_intrinsic_height(Optional<float> height) { m_intrinsic_height = height; }
    void set_intrinsic_aspect_ratio(Optional<float> ratio) { m_intrinsic_aspect_ratio = ratio; }

    virtual void prepare_for_replaced_layout() { }

    virtual bool can_have_children() const override { return false; }

private:
    Optional<float> m_intrinsic_width;
    Optional<float> m_intrinsic_height;
    Optional<float> m_intrinsic_aspect_ratio;
};

}
