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
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>

PDFViewerWidget::PDFViewerWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    create_toolbar();

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
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = menubar.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("PDF Viewer", GUI::Icon::default_icon("app-pdf-viewer"), window()));
}

void PDFViewerWidget::create_toolbar()
{
    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    auto open_outline_action = GUI::Action::create(
        "Open &Sidebar", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/sidebar.png"), [&](auto& action) {
            m_sidebar_open = !m_sidebar_open;
            m_sidebar->set_fixed_width(m_sidebar_open ? 0 : 200);
            action.set_text(m_sidebar_open ? "Open &Sidebar" : "Close &Sidebar");
        },
        nullptr);
    open_outline_action->set_enabled(false);
    m_toggle_sidebar_action = open_outline_action;

    toolbar.add_action(*open_outline_action);
    toolbar.add_separator();

    m_go_to_prev_page_action = GUI::Action::create("Go to &Previous Page", Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"), [&](auto&) {
        VERIFY(m_viewer->current_page() > 0);
        m_page_text_box->set_current_number(m_viewer->current_page());
    });
    m_go_to_prev_page_action->set_enabled(false);

    m_go_to_next_page_action = GUI::Action::create("Go to &Next Page", Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [&](auto&) {
        VERIFY(m_viewer->current_page() < m_viewer->document()->get_page_count() - 1);
        m_page_text_box->set_current_number(m_viewer->current_page() + 2);
    });
    m_go_to_next_page_action->set_enabled(false);

    toolbar.add_action(*m_go_to_prev_page_action);
    toolbar.add_action(*m_go_to_next_page_action);

    m_page_text_box = toolbar.add<NumericInput>();
    m_page_text_box->set_enabled(false);
    m_page_text_box->set_fixed_width(30);
    m_page_text_box->set_min_number(1);

    m_page_text_box->on_number_changed = [&](i32 number) {
        auto page_count = m_viewer->document()->get_page_count();
        auto new_page_number = static_cast<u32>(number);
        VERIFY(new_page_number >= 1 && new_page_number <= page_count);
        m_viewer->set_current_page(new_page_number - 1);
        m_viewer->update();
        m_go_to_prev_page_action->set_enabled(new_page_number > 1);
        m_go_to_next_page_action->set_enabled(new_page_number < page_count);
    };

    m_total_page_label = toolbar.add<GUI::Label>();
}

void PDFViewerWidget::open_file(const String& path)
{
    window()->set_title(String::formatted("{} - PDF Viewer", path));
    auto file_result = Core::File::open(path, Core::OpenMode::ReadOnly);
    VERIFY(!file_result.is_error());
    m_buffer = file_result.value()->read_all();
    auto document = adopt_ref(*new PDF::Document(m_buffer));
    m_viewer->set_document(document);
    m_total_page_label->set_text(String::formatted("of {}", document->get_page_count()));
    m_total_page_label->set_fixed_width(30);

    m_page_text_box->set_enabled(true);
    m_page_text_box->set_current_number(1, false);
    m_page_text_box->set_max_number(document->get_page_count());
    m_go_to_prev_page_action->set_enabled(false);
    m_go_to_next_page_action->set_enabled(document->get_page_count() > 1);
    m_toggle_sidebar_action->set_enabled(true);

    if (document->outline()) {
        auto outline = document->outline();
        m_sidebar->set_outline(outline.release_nonnull());
        m_sidebar->set_fixed_width(200);
        m_sidebar_open = true;
    } else {
        m_sidebar->set_outline({});
        m_sidebar->set_fixed_width(0);
        m_sidebar_open = false;
    }
}
