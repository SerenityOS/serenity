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

    virtual void run(Box const&, LayoutMode) override;
    virtual float automatic_content_height() const override;

    void dimension_box_on_line(Box const&, LayoutMode);

    float leftmost_x_offset_at(float y) const;
    float available_space_for_line(float y) const;
    bool any_floats_intrude_at_y(float y) const;
    bool can_fit_new_line_at_y(float y) const;

    float effective_containing_block_width() const { return m_effective_containing_block_width; }

private:
    void generate_line_boxes(LayoutMode);
    void apply_justification_to_fragments(CSS::TextJustify, LineBox&, bool is_last_line);

    LayoutState::UsedValues const& m_containing_block_state;
    float m_effective_containing_block_width { 0 };
};

}
