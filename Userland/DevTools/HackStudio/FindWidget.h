/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Editor.h"
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

namespace HackStudio {

class Editor;
class EditorWrapper;

class FindWidget final : public GUI::Widget {
    C_OBJECT(FindWidget)
public:
    ~FindWidget() = default;

    void show();
    void hide();
    bool visible() const { return m_visible; }

private:
    FindWidget(NonnullRefPtr<Editor>);

    void find_next(GUI::TextEditor::SearchDirection);

    static constexpr auto widget_height = 25;

    NonnullRefPtr<Editor> m_editor;
    RefPtr<GUI::TextBox> m_input_field;
    RefPtr<GUI::Label> m_index_label;
    RefPtr<GUI::Button> m_next;
    RefPtr<GUI::Button> m_previous;
    bool m_visible { false };
};

}
