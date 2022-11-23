/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/AvailableSpace.h>
#include <LibWeb/Layout/LayoutState.h>

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

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) = 0;

    // This function returns the automatic content height of the context's root box.
    virtual CSSPixels automatic_content_width() const { return 0; }

    // This function returns the automatic content height of the context's root box.
    virtual CSSPixels automatic_content_height() const = 0;

    Box const& context_box() const { return m_context_box; }

    FormattingContext* parent() { return m_parent; }
    FormattingContext const* parent() const { return m_parent; }

    Type type() const { return m_type; }
    bool is_block_formatting_context() const { return type() == Type::Block; }

    virtual bool inhibits_floating() const { return false; }

    static bool creates_block_formatting_context(Box const&);

    static CSSPixels compute_width_for_replaced_element(LayoutState const&, ReplacedBox const&, AvailableSpace const&);
    static CSSPixels compute_height_for_replaced_element(LayoutState const&, ReplacedBox const&, AvailableSpace const&);

    OwnPtr<FormattingContext> create_independent_formatting_context_if_needed(LayoutState&, Box const& child_box);

    virtual void parent_context_did_dimension_child_root_box() { }

    CSSPixels calculate_min_content_width(Layout::Box const&) const;
    CSSPixels calculate_max_content_width(Layout::Box const&) const;
    CSSPixels calculate_min_content_height(Layout::Box const&, AvailableSize const& available_width) const;
    CSSPixels calculate_max_content_height(Layout::Box const&, AvailableSize const& available_width) const;

    CSSPixels calculate_fit_content_height(Layout::Box const&, AvailableSpace const&) const;
    CSSPixels calculate_fit_content_width(Layout::Box const&, AvailableSpace const&) const;

    CSS::Length calculate_inner_width(Layout::Box const&, AvailableSize const&, CSS::Size const& width) const;
    CSS::Length calculate_inner_height(Layout::Box const&, AvailableSize const&, CSS::Size const& height) const;

    virtual CSSPixels greatest_child_width(Box const&);

    CSSPixels containing_block_width_for(Box const& box) const { return containing_block_width_for(box, m_state); }
    CSSPixels containing_block_height_for(Box const& box) const { return containing_block_height_for(box, m_state); }

    static CSSPixels containing_block_width_for(Box const&, LayoutState const&);
    static CSSPixels containing_block_height_for(Box const&, LayoutState const&);

    [[nodiscard]] CSSPixels calculate_stretch_fit_width(Box const&, AvailableSize const&) const;
    [[nodiscard]] CSSPixels calculate_stretch_fit_height(Box const&, AvailableSize const&) const;

    virtual bool can_determine_size_of_child() const { return false; }
    virtual void determine_width_of_child(Box const&, AvailableSpace const&) { }
    virtual void determine_height_of_child(Box const&, AvailableSpace const&) { }

    virtual CSSPixelPoint calculate_static_position(Box const&) const;
    bool can_skip_is_anonymous_text_run(Box&);

protected:
    FormattingContext(Type, LayoutState&, Box const&, FormattingContext* parent = nullptr);

    static bool should_treat_width_as_auto(Box const&, AvailableSpace const&);
    static bool should_treat_height_as_auto(Box const&, AvailableSpace const&);

    OwnPtr<FormattingContext> layout_inside(Box const&, LayoutMode, AvailableSpace const&);
    void compute_inset(Box const& box);

    struct SpaceUsedByFloats {
        CSSPixels left { 0 };
        CSSPixels right { 0 };
    };

    struct ShrinkToFitResult {
        CSSPixels preferred_width { 0 };
        CSSPixels preferred_minimum_width { 0 };
    };

    static CSSPixels tentative_width_for_replaced_element(LayoutState const&, ReplacedBox const&, CSS::Size const& computed_width, AvailableSpace const&);
    static CSSPixels tentative_height_for_replaced_element(LayoutState const&, ReplacedBox const&, CSS::Size const& computed_height, AvailableSpace const&);
    CSSPixels compute_auto_height_for_block_formatting_context_root(BlockContainer const&) const;

    ShrinkToFitResult calculate_shrink_to_fit_widths(Box const&);

    void layout_absolutely_positioned_element(Box const&, AvailableSpace const&);
    void compute_width_for_absolutely_positioned_element(Box const&, AvailableSpace const&);
    void compute_width_for_absolutely_positioned_non_replaced_element(Box const&, AvailableSpace const&);
    void compute_width_for_absolutely_positioned_replaced_element(ReplacedBox const&, AvailableSpace const&);
    void compute_height_for_absolutely_positioned_element(Box const&, AvailableSpace const&);
    void compute_height_for_absolutely_positioned_non_replaced_element(Box const&, AvailableSpace const&);
    void compute_height_for_absolutely_positioned_replaced_element(ReplacedBox const&, AvailableSpace const&);

    Type m_type {};

    FormattingContext* m_parent { nullptr };
    Box const& m_context_box;

    LayoutState& m_state;
};

}
