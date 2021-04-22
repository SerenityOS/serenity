/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class CursorTool;
class FormEditorWidget;

class FormWidget final : public GUI::Widget {
    C_OBJECT(FormWidget)
public:
    virtual ~FormWidget() override;

    FormEditorWidget& editor();
    const FormEditorWidget& editor() const;

    // FIXME: This should be an app-wide preference instead.
    int grid_size() const { return m_grid_size; }

private:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    FormWidget();

    int m_grid_size { 5 };
};

}
