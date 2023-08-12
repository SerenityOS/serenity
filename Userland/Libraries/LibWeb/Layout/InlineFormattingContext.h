/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class InlineFormattingContext final : public FormattingContext {
public:
    InlineFormattingContext(LayoutState&, BlockContainer const& containing_block, BlockFormattingContext& parent);
    ~InlineFormattingContext();

    BlockFormattingContext& parent();
    BlockFormattingContext const& parent() const;

    BlockContainer const& containing_block() const { return static_cast<BlockContainer const&>(context_box()); }

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual CSSPixels automatic_content_height() const override;
    virtual CSSPixels automatic_content_width() const override;

    void dimension_box_on_line(Box const&, LayoutMode);

    CSSPixels leftmost_x_offset_at(CSSPixels y) const;
    AvailableSize available_space_for_line(CSSPixels y) const;
    bool any_floats_intrude_at_y(CSSPixels y) const;
    bool can_fit_new_line_at_y(CSSPixels y) const;

    virtual bool can_determine_size_of_child() const override;
    virtual void determine_width_of_child(Box const&, AvailableSpace const&) override;
    virtual void determine_height_of_child(Box const&, AvailableSpace const&) override;

    CSSPixels vertical_float_clearance() const;
    void set_vertical_float_clearance(CSSPixels);

private:
    void generate_line_boxes(LayoutMode);
    void apply_justification_to_fragments(CSS::TextJustify, LineBox&, bool is_last_line);

    LayoutState::UsedValues const& m_containing_block_state;

    Optional<AvailableSpace> m_available_space;

    CSSPixels m_automatic_content_width { 0 };
    CSSPixels m_automatic_content_height { 0 };

    CSSPixels m_vertical_float_clearance { 0 };
};

}
