/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingState.h>

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

    virtual void run(Box const&, LayoutMode) = 0;

    Box const& context_box() const { return m_context_box; }

    FormattingContext* parent() { return m_parent; }
    FormattingContext const* parent() const { return m_parent; }

    Type type() const { return m_type; }
    bool is_block_formatting_context() const { return type() == Type::Block; }

    virtual bool inhibits_floating() const { return false; }

    static bool creates_block_formatting_context(Box const&);

    static float compute_width_for_replaced_element(FormattingState const&, ReplacedBox const&);
    static float compute_height_for_replaced_element(FormattingState const&, ReplacedBox const&);

    OwnPtr<FormattingContext> create_independent_formatting_context_if_needed(FormattingState&, Box const& child_box);

    virtual void parent_context_did_dimension_child_root_box() { }

    struct MinAndMaxContentSize {
        float min_content_size { 0 };
        float max_content_size { 0 };
    };

    MinAndMaxContentSize calculate_min_and_max_content_width(Layout::Box const&) const;
    MinAndMaxContentSize calculate_min_and_max_content_height(Layout::Box const&) const;

    float calculate_fit_content_height(Layout::Box const&, Optional<float> available_height) const;
    float calculate_fit_content_width(Layout::Box const&, Optional<float> available_width) const;

    virtual float greatest_child_width(Box const&);

protected:
    FormattingContext(Type, FormattingState&, Box const&, FormattingContext* parent = nullptr);

    float calculate_fit_content_size(float min_content_size, float max_content_size, Optional<float> available_space) const;
    FormattingState::IntrinsicSizes calculate_intrinsic_sizes(Layout::Box const&) const;

    OwnPtr<FormattingContext> layout_inside(Box const&, LayoutMode);
    void compute_inset(Box const& box);

    struct SpaceUsedByFloats {
        float left { 0 };
        float right { 0 };
    };

    struct ShrinkToFitResult {
        float preferred_width { 0 };
        float preferred_minimum_width { 0 };
    };

    static float tentative_width_for_replaced_element(FormattingState const&, ReplacedBox const&, CSS::Length const& width);
    static float tentative_height_for_replaced_element(FormattingState const&, ReplacedBox const&, CSS::Length const& height);
    static float compute_auto_height_for_block_formatting_context_root(FormattingState const&, BlockContainer const&);
    static float compute_auto_height_for_block_level_element(FormattingState const&, Box const&);
    static float calculate_auto_height(FormattingState const& state, Box const& box);

    ShrinkToFitResult calculate_shrink_to_fit_widths(Box const&);

    void layout_absolutely_positioned_element(Box const&);
    void compute_width_for_absolutely_positioned_element(Box const&);
    void compute_width_for_absolutely_positioned_non_replaced_element(Box const&);
    void compute_width_for_absolutely_positioned_replaced_element(ReplacedBox const&);
    void compute_height_for_absolutely_positioned_element(Box const&);
    void compute_height_for_absolutely_positioned_non_replaced_element(Box const&);
    void compute_height_for_absolutely_positioned_replaced_element(ReplacedBox const&);

    Type m_type {};

    FormattingContext* m_parent { nullptr };
    Box const& m_context_box;

    FormattingState& m_state;
};

}
