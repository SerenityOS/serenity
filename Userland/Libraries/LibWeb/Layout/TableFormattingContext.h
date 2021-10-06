/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibWeb/Layout/BlockFormattingContext.h>

namespace Web::Layout {

class TableFormattingContext final : public BlockFormattingContext {
public:
    explicit TableFormattingContext(BlockContainer&, FormattingContext* parent);
    ~TableFormattingContext();

    virtual void run(Box&, LayoutMode) override;

private:
    void calculate_column_widths(Box& row, Vector<float>& column_widths);
    void layout_row(Box& row, Vector<float>& column_widths);
};

}
