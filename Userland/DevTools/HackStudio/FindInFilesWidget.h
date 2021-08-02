/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class FindInFilesWidget final : public GUI::Widget {
    C_OBJECT(FindInFilesWidget)
public:
    virtual ~FindInFilesWidget() override { }

    void focus_textbox_and_select_all();

    void reset();

private:
    explicit FindInFilesWidget();

    RefPtr<GUI::TextBox> m_textbox;
    RefPtr<GUI::Button> m_button;
    RefPtr<GUI::TableView> m_result_view;
};

}
