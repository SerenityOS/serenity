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
    void set_title(StringView);
    virtual Margins content_margins() const override;

protected:
    explicit GroupBox(StringView title = {});

    virtual void paint_event(PaintEvent&) override;
    virtual void fonts_change_event(FontsChangeEvent&) override;

private:
    String m_title;
};

}
