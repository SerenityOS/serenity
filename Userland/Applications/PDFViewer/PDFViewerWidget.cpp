/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <stdio.h>
#include <string.h>

PDFViewerWidget::PDFViewerWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    m_viewer = add<PDFViewer>();
}

PDFViewerWidget::~PDFViewerWidget()
{
}

void PDFViewerWidget::initialize_menubar(GUI::Menubar& menubar)
{
    auto& file_menu = menubar.add_menu("File");
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window());
        if (open_path.has_value())
            open_file(open_path.value());
    }));
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));
}

void PDFViewerWidget::open_file(const String& path)
{
    window()->set_title(String::formatted("{} - PDFViewer", path));
    auto file_result = Core::File::open(path, Core::IODevice::OpenMode::ReadOnly);
    VERIFY(!file_result.is_error());
    m_buffer = file_result.value()->read_all();
    m_viewer->set_document(PDF::Document::from(m_buffer));
}
