/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Splitter.h>

PDFViewerWidget::PDFViewerWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    auto& splitter = add<GUI::HorizontalSplitter>();

    m_sidebar = splitter.add<SidebarWidget>();
    m_sidebar->set_fixed_width(0);

    m_viewer = splitter.add<PDFViewer>();
}

void PDFViewerWidget::initialize_menubar(GUI::Menubar& menubar)
{
    auto& file_menu = menubar.add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window());
        if (open_path.has_value())
            open_file(open_path.value());
    }));
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& view_menu = menubar.add_menu("&View");

    auto open_sidebar_action = GUI::Action::create(
        "Open &Sidebar", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/sidebar.png"), [&](auto& action) {
            m_sidebar_open = !m_sidebar_open;
            m_sidebar->set_fixed_width(m_sidebar_open ? 0 : 200);
            action.set_text(m_sidebar_open ? "Open &Sidebar" : "Close &Sidebar");
        },
        nullptr);
    open_sidebar_action->set_enabled(false);

    view_menu.add_action(open_sidebar_action);

    m_open_outline_action = open_sidebar_action;
}

void PDFViewerWidget::open_file(const String& path)
{
    window()->set_title(String::formatted("{} - PDFViewer", path));
    auto file_result = Core::File::open(path, Core::OpenMode::ReadOnly);
    VERIFY(!file_result.is_error());
    m_buffer = file_result.value()->read_all();
    auto document = adopt_ref(*new PDF::Document(m_buffer));
    m_viewer->set_document(document);

    if (document->outline()) {
        auto outline = document->outline();
        m_sidebar->set_outline(outline.release_nonnull());
        m_sidebar->set_fixed_width(200);
        m_sidebar_open = true;
        m_open_outline_action->set_enabled(true);
    } else {
        m_sidebar->set_outline({});
        m_sidebar->set_fixed_width(0);
        m_sidebar_open = false;
        m_open_outline_action->set_enabled(false);
    }
}
