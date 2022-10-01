/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/BlockFormattingContext.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class GridFormattingContext final : public BlockFormattingContext {
public:
    explicit GridFormattingContext(LayoutState&, BlockContainer const&, FormattingContext* parent);
    ~GridFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const& available_width, AvailableSpace const& available_height) override;
    virtual float automatic_content_height() const override;

private:
    float m_automatic_content_height { 0 };
    bool is_auto_positioned_row(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_column(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
    bool is_auto_positioned_track(CSS::GridTrackPlacement const&, CSS::GridTrackPlacement const&) const;
};

}
