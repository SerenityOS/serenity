/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PDFViewer.h"
#include "SidebarWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/Widget.h>

class PDFViewer;

class PDFViewerWidget final : public GUI::Widget {
    C_OBJECT(PDFViewerWidget)

public:
    ~PDFViewerWidget() override = default;

    void open_file(const String& path);
    void initialize_menubar(GUI::Menubar&);

private:
    PDFViewerWidget();

    RefPtr<GUI::Action> m_open_outline_action;
    RefPtr<PDFViewer> m_viewer;
    RefPtr<SidebarWidget> m_sidebar;
    bool m_sidebar_open { false };
    ByteBuffer m_buffer;
    RefPtr<GUI::Action> m_open_action;
};
