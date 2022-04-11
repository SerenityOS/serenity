/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PDFViewerWidget.h"
#include <LibCore/File.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>

PDFViewerWidget::PDFViewerWidget()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    auto& toolbar_container = add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    auto& splitter = add<GUI::HorizontalSplitter>();

    m_sidebar = splitter.add<SidebarWidget>();
    m_sidebar->set_fixed_width(0);

    m_viewer = splitter.add<PDFViewer>();
    m_viewer->on_page_change = [&](auto new_page) {
        m_page_text_box->set_current_number(new_page + 1, GUI::AllowCallback::No);
    };

    initialize_toolbar(toolbar);
}

void PDFViewerWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_open_file(&window);
        if (!response.is_error())
            open_file(*response.value());
    }));
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& view_menu = window.add_menu("&View");
    view_menu.add_action(*m_toggle_sidebar_action);
    view_menu.add_separator();
    auto& view_mode_menu = view_menu.add_submenu("View &Mode");
    view_mode_menu.add_action(*m_page_view_mode_single);
    view_mode_menu.add_action(*m_page_view_mode_multiple);
    view_menu.add_separator();
    view_menu.add_action(*m_zoom_in_action);
    view_menu.add_action(*m_zoom_out_action);
    view_menu.add_action(*m_reset_zoom_action);

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("PDF Viewer", GUI::Icon::default_icon("app-pdf-viewer"), &window));
}

void PDFViewerWidget::initialize_toolbar(GUI::Toolbar& toolbar)
{
    auto open_outline_action = GUI::Action::create(
        "Toggle &Sidebar", { Mod_Ctrl, Key_S }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/sidebar.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
            m_sidebar_open = !m_sidebar_open;
            m_sidebar->set_fixed_width(m_sidebar_open ? 200 : 0);
        },
        nullptr);
    open_outline_action->set_enabled(false);
    m_toggle_sidebar_action = open_outline_action;

    toolbar.add_action(*open_outline_action);
    toolbar.add_separator();

    m_go_to_prev_page_action = GUI::Action::create("Go to &Previous Page", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-up.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        VERIFY(m_viewer->current_page() > 0);
        m_page_text_box->set_current_number(m_viewer->current_page());
    });
    m_go_to_prev_page_action->set_enabled(false);

    m_go_to_next_page_action = GUI::Action::create("Go to &Next Page", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-down.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
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
        m_go_to_prev_page_action->set_enabled(new_page_number > 1);
        m_go_to_next_page_action->set_enabled(new_page_number < page_count);
    };

    m_total_page_label = toolbar.add<GUI::Label>();
    m_total_page_label->set_autosize(true, 5);
    toolbar.add_separator();

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action([&](auto&) {
        m_viewer->zoom_in();
    });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action([&](auto&) {
        m_viewer->zoom_out();
    });

    m_reset_zoom_action = GUI::CommonActions::make_reset_zoom_action([&](auto&) {
        m_viewer->reset_zoom();
    });

    m_rotate_counterclockwise_action = GUI::CommonActions::make_rotate_counterclockwise_action([&](auto&) {
        m_viewer->rotate(-90);
    });

    m_rotate_clockwise_action = GUI::CommonActions::make_rotate_clockwise_action([&](auto&) {
        m_viewer->rotate(90);
    });

    m_zoom_in_action->set_enabled(false);
    m_zoom_out_action->set_enabled(false);
    m_reset_zoom_action->set_enabled(false);
    m_rotate_counterclockwise_action->set_enabled(false);
    m_rotate_clockwise_action->set_enabled(false);

    m_page_view_mode_single = GUI::Action::create_checkable("Single", [&](auto&) {
        m_viewer->set_page_view_mode(PDFViewer::PageViewMode::Single);
    });

    m_page_view_mode_multiple = GUI::Action::create_checkable("Multiple", [&](auto&) {
        m_viewer->set_page_view_mode(PDFViewer::PageViewMode::Multiple);
    });

    if (m_viewer->page_view_mode() == PDFViewer::PageViewMode::Single) {
        m_page_view_mode_single->set_checked(true);
    } else {
        m_page_view_mode_multiple->set_checked(true);
    }

    m_page_view_action_group.add_action(*m_page_view_mode_single);
    m_page_view_action_group.add_action(*m_page_view_mode_multiple);
    m_page_view_action_group.set_exclusive(true);

    toolbar.add_action(*m_zoom_in_action);
    toolbar.add_action(*m_zoom_out_action);
    toolbar.add_action(*m_reset_zoom_action);
    toolbar.add_action(*m_rotate_counterclockwise_action);
    toolbar.add_action(*m_rotate_clockwise_action);
}

void PDFViewerWidget::open_file(Core::File& file)
{
    window()->set_title(String::formatted("{} - PDF Viewer", file.filename()));

    auto handle_error = [&]<typename T>(PDF::PDFErrorOr<T> maybe_error) {
        if (maybe_error.is_error()) {
            auto error = maybe_error.release_error();
            GUI::MessageBox::show_error(nullptr, String::formatted("Couldn't load PDF {}:\n{}", file.filename(), error.message()));
            return true;
        }
        return false;
    };

    m_buffer = file.read_all();
    auto maybe_document = PDF::Document::create(m_buffer);
    if (handle_error(maybe_document))
        return;

    auto document = maybe_document.release_value();

    if (auto sh = document->security_handler(); sh && !sh->has_user_password()) {
        // FIXME: Prompt the user for a password
        VERIFY_NOT_REACHED();
    }

    if (handle_error(document->initialize()))
        return;

    if (handle_error(m_viewer->set_document(document)))
        return;

    m_total_page_label->set_text(String::formatted("of {}", document->get_page_count()));

    m_page_text_box->set_enabled(true);
    m_page_text_box->set_current_number(1, GUI::AllowCallback::No);
    m_page_text_box->set_max_number(document->get_page_count());
    m_go_to_prev_page_action->set_enabled(false);
    m_go_to_next_page_action->set_enabled(document->get_page_count() > 1);
    m_toggle_sidebar_action->set_enabled(true);
    m_zoom_in_action->set_enabled(true);
    m_zoom_out_action->set_enabled(true);
    m_reset_zoom_action->set_enabled(true);
    m_rotate_counterclockwise_action->set_enabled(true);
    m_rotate_clockwise_action->set_enabled(true);

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
