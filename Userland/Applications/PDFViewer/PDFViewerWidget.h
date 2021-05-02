/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PDFViewer.h"
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class PDFViewer;

class PDFViewerWidget final : public GUI::Widget {
    C_OBJECT(PDFViewerWidget)
public:
    ~PDFViewerWidget() override;
    void open_file(const String& path);
    void initialize_menubar(GUI::Menubar&);

private:
    PDFViewerWidget();

    RefPtr<PDFViewer> m_viewer;
    ByteBuffer m_buffer;
    RefPtr<GUI::Action> m_open_action;
};
