/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class GroupBox : public Widget {
    C_OBJECT(GroupBox)
public:
    virtual ~GroupBox() override;

    String title() const { return m_title; }
    void set_title(StringView const&);

protected:
    explicit GroupBox(StringView const& title = {});

    virtual void paint_event(PaintEvent&) override;

private:
    String m_title;
};

}
