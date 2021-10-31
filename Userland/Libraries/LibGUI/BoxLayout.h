/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~BoxLayout() override { }

    Gfx::Orientation orientation() const { return m_orientation; }

    virtual void run(Widget&) override;
    virtual Gfx::IntSize preferred_size() const override;

protected:
    explicit BoxLayout(Gfx::Orientation);

private:
    int preferred_primary_size() const;
    int preferred_secondary_size() const;

    Gfx::Orientation m_orientation;
};

class VerticalBoxLayout final : public BoxLayout {
    C_OBJECT(VerticalBoxLayout);

private:
    explicit VerticalBoxLayout()
        : BoxLayout(Gfx::Orientation::Vertical)
    {
    }
    virtual ~VerticalBoxLayout() override { }
};

class HorizontalBoxLayout final : public BoxLayout {
    C_OBJECT(HorizontalBoxLayout);

private:
    explicit HorizontalBoxLayout()
        : BoxLayout(Gfx::Orientation::Horizontal)
    {
    }
    virtual ~HorizontalBoxLayout() override { }
};

}
