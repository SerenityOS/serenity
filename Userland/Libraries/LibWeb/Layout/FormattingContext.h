/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibWeb/Forward.h>

namespace Web::Layout {

class FormattingContext {
public:
    virtual ~FormattingContext();

    enum class Type {
        Block,
        Inline,
        Flex,
        Table,
        SVG,
    };

    virtual void run(Box&, LayoutMode) = 0;

    Box& context_box() { return m_context_box; }
    const Box& context_box() const { return m_context_box; }

    FormattingContext* parent() { return m_parent; }
    const FormattingContext* parent() const { return m_parent; }

    Type type() const { return m_type; }
    bool is_block_formatting_context() const { return type() == Type::Block; }

    virtual bool inhibits_floating() const { return false; }

    static bool creates_block_formatting_context(const Box&);

    static float compute_width_for_replaced_element(const ReplacedBox&);
    static float compute_height_for_replaced_element(const ReplacedBox&);

    OwnPtr<FormattingContext> create_independent_formatting_context_if_needed(Box& child_box);

    virtual void parent_context_did_dimension_child_root_box() { }

protected:
    FormattingContext(Type, Box&, FormattingContext* parent = nullptr);

    OwnPtr<FormattingContext> layout_inside(Box&, LayoutMode);

    struct ShrinkToFitResult {
        float preferred_width { 0 };
        float preferred_minimum_width { 0 };
    };

    static float tentative_width_for_replaced_element(const ReplacedBox&, const CSS::Length& width);
    static float tentative_height_for_replaced_element(const ReplacedBox&, const CSS::Length& height);
    enum ConsiderFloats {
        Yes,
        No,
    };
    static float compute_auto_height_for_block_level_element(Box const&, ConsiderFloats consider_floats = ConsiderFloats::Yes);

    ShrinkToFitResult calculate_shrink_to_fit_widths(Box&);

    void layout_absolutely_positioned_element(Box&);
    void compute_width_for_absolutely_positioned_element(Box&);
    void compute_width_for_absolutely_positioned_non_replaced_element(Box&);
    void compute_width_for_absolutely_positioned_replaced_element(ReplacedBox&);
    void compute_height_for_absolutely_positioned_element(Box&);
    void compute_height_for_absolutely_positioned_non_replaced_element(Box&);
    void compute_height_for_absolutely_positioned_replaced_element(ReplacedBox&);

    Type m_type {};

    FormattingContext* m_parent { nullptr };
    Box& m_context_box;
};

}
