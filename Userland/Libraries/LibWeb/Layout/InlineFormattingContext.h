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
    InlineFormattingContext(FormattingState&, BlockContainer const& containing_block, BlockFormattingContext& parent);
    ~InlineFormattingContext();

    BlockFormattingContext& parent();
    BlockFormattingContext const& parent() const;

    BlockContainer const& containing_block() const { return static_cast<BlockContainer const&>(context_box()); }

    virtual void run(Box const&, LayoutMode) override;

    void dimension_box_on_line(Box const&, LayoutMode);

    struct AvailableSpaceForLineInfo {
        float left { 0 };
        float right { 0 };
    };

    AvailableSpaceForLineInfo available_space_for_line(float y) const;

private:
    void generate_line_boxes(LayoutMode);
};

}
