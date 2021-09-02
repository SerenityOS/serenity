/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "NumericInput.h"
#include "PDFViewer.h"
#include "SidebarWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

class PDFViewer;

class PDFViewerWidget final : public GUI::Widget {
    C_OBJECT(PDFViewerWidget)

public:
    ~PDFViewerWidget() override = default;

    void initialize_menubar(GUI::Window&);
    void create_toolbar();
    void open_file(int fd, const String& path);

private:
    PDFViewerWidget();

    RefPtr<PDFViewer> m_viewer;
    RefPtr<SidebarWidget> m_sidebar;
    RefPtr<NumericInput> m_page_text_box;
    RefPtr<GUI::Label> m_total_page_label;
    RefPtr<GUI::Action> m_go_to_prev_page_action;
    RefPtr<GUI::Action> m_go_to_next_page_action;
    RefPtr<GUI::Action> m_toggle_sidebar_action;
    bool m_sidebar_open { false };
    ByteBuffer m_buffer;
};
