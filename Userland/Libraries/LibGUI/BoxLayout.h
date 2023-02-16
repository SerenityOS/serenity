/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Forward.h>
#include <LibGUI/Layout.h>
#include <LibGfx/Orientation.h>

namespace GUI {

class BoxLayout : public Layout {
    C_OBJECT(BoxLayout);

public:
    virtual ~BoxLayout() override = default;

    Gfx::Orientation orientation() const { return m_orientation; }

    virtual void run(Widget&) override;
    virtual UISize preferred_size() const override;
    virtual UISize min_size() const override;

protected:
    explicit BoxLayout(Gfx::Orientation, Margins = {}, int spacing = Layout::default_spacing);

private:
    Gfx::Orientation m_orientation;
};

class VerticalBoxLayout final : public BoxLayout {
    C_OBJECT(VerticalBoxLayout);

private:
    explicit VerticalBoxLayout(Margins margins = {}, int spacing = Layout::default_spacing)
        : BoxLayout(Gfx::Orientation::Vertical, margins, spacing)
    {
    }
    virtual ~VerticalBoxLayout() override = default;
};

class HorizontalBoxLayout final : public BoxLayout {
    C_OBJECT(HorizontalBoxLayout);

private:
    explicit HorizontalBoxLayout(Margins margins = {}, int spacing = Layout::default_spacing)
        : BoxLayout(Gfx::Orientation::Horizontal, margins, spacing)
    {
    }
    virtual ~HorizontalBoxLayout() override = default;
};

}
