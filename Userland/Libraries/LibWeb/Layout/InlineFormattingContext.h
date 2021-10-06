/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    InlineFormattingContext(BlockContainer& containing_block, FormattingContext* parent);
    ~InlineFormattingContext();

    BlockContainer& containing_block() { return static_cast<BlockContainer&>(context_box()); }
    BlockContainer const& containing_block() const { return static_cast<BlockContainer const&>(context_box()); }

    virtual void run(Box&, LayoutMode) override;

    float available_width_at_line(size_t line_index) const;

    void dimension_box_on_line(Box&, LayoutMode);
};

}
