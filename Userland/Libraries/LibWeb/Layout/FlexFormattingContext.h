/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

struct FlexItem;

class FlexFormattingContext final : public FormattingContext {
public:
    FlexFormattingContext(Box& flex_container, FormattingContext* parent);
    ~FlexFormattingContext();

    virtual bool inhibits_floating() const override { return true; }

    virtual void run(Box&, LayoutMode) override;

private:
    void generate_anonymous_flex_items(Box& flex_container, Vector<FlexItem>&);

    bool is_row_layout() const { return m_flex_direction == CSS::FlexDirection::Row || m_flex_direction == CSS::FlexDirection::RowReverse; }

    CSS::FlexDirection m_flex_direction {};
};

}
