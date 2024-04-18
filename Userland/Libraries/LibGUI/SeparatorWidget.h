/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class SeparatorWidget : public Widget {
    C_OBJECT(SeparatorWidget);

public:
    virtual ~SeparatorWidget() override = default;

protected:
    explicit SeparatorWidget(Gfx::Orientation);

private:
    virtual void paint_event(PaintEvent&) override;
    virtual Optional<UISize> calculated_preferred_size() const override;
    virtual Optional<UISize> calculated_min_size() const override;

    Gfx::Orientation const m_orientation;
};

class VerticalSeparator final : public SeparatorWidget {
    C_OBJECT(VerticalSeparator)
public:
    virtual ~VerticalSeparator() override = default;

private:
    VerticalSeparator()
        : SeparatorWidget(Gfx::Orientation::Vertical)
    {
    }
};

class HorizontalSeparator final : public SeparatorWidget {
    C_OBJECT(HorizontalSeparator)
public:
    virtual ~HorizontalSeparator() override = default;

private:
    HorizontalSeparator()
        : SeparatorWidget(Gfx::Orientation::Horizontal)
    {
    }
};

}
