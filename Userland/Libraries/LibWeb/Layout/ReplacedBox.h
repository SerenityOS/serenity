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

    const DOM::Element& dom_node() const { return downcast<DOM::Element>(*Node::dom_node()); }
    DOM::Element& dom_node() { return downcast<DOM::Element>(*Node::dom_node()); }

    bool has_intrinsic_width() const { return m_has_intrinsic_width; }
    bool has_intrinsic_height() const { return m_has_intrinsic_height; }
    bool has_intrinsic_ratio() const { return m_has_intrinsic_ratio; }

    float intrinsic_width() const { return m_intrinsic_width; }
    float intrinsic_height() const { return m_intrinsic_height; }
    float intrinsic_ratio() const { return m_intrinsic_ratio; }

    void set_has_intrinsic_width(bool has) { m_has_intrinsic_width = has; }
    void set_has_intrinsic_height(bool has) { m_has_intrinsic_height = has; }
    void set_has_intrinsic_ratio(bool has) { m_has_intrinsic_ratio = has; }

    void set_intrinsic_width(float width) { m_intrinsic_width = width; }
    void set_intrinsic_height(float height) { m_intrinsic_height = height; }
    void set_intrinsic_ratio(float ratio) { m_intrinsic_ratio = ratio; }

    virtual void prepare_for_replaced_layout() { }

    virtual bool can_have_children() const override { return false; }

protected:
    virtual void split_into_lines(InlineFormattingContext&, LayoutMode) override;

private:
    bool m_has_intrinsic_width { false };
    bool m_has_intrinsic_height { false };
    bool m_has_intrinsic_ratio { false };
    float m_intrinsic_width { 0 };
    float m_intrinsic_height { 0 };
    float m_intrinsic_ratio { 0 };
};

}
