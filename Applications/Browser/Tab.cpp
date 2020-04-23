/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Tab.h"
#include "BookmarksBarWidget.h"
#include "History.h"
#include "InspectorWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Window.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOMTreeModel.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Frame.h>
#include <LibWeb/HtmlView.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Parser/CSSParser.h>
#include <LibWeb/Parser/HTMLParser.h>
#include <LibWeb/ResourceLoader.h>

namespace Browser {

static const char* home_url = "file:///home/anon/www/welcome.html";
static const char* bookmarks_filename = "/home/anon/bookmarks.json";

Tab::Tab()
{
    auto& widget = *this;
    auto& layout = set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 1, 1, 1, 1 });

    bool bookmarksbar_enabled = true;

    auto& toolbar_container = widget.add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();
    m_bookmarks_bar = toolbar_container.add<BookmarksBarWidget>(bookmarks_filename, bookmarksbar_enabled);
    m_html_widget = widget.add<Web::HtmlView>();

    m_bookmarks_bar->on_bookmark_click = [this](auto&, auto& url) {
        m_html_widget->load(url);
    };

    m_go_back_action = GUI::CommonActions::make_go_back_action([this](auto&) {
        m_history.go_back();
        update_actions();
        TemporaryChange<bool> change(m_should_push_loads_to_history, false);
        m_html_widget->load(m_history.current());
    });

    m_go_forward_action = GUI::CommonActions::make_go_forward_action([this](auto&) {
        m_history.go_forward();
        update_actions();
        TemporaryChange<bool> change(m_should_push_loads_to_history, false);
        m_html_widget->load(m_history.current());
    });

    toolbar.add_action(*m_go_back_action);
    toolbar.add_action(*m_go_forward_action);

    toolbar.add_action(GUI::CommonActions::make_go_home_action([this](auto&) {
        m_html_widget->load(home_url);
    }));

    toolbar.add_action(GUI::CommonActions::make_reload_action([this](auto&) {
        TemporaryChange<bool> change(m_should_push_loads_to_history, false);
        m_html_widget->reload();
    }));

    m_location_box = toolbar.add<GUI::TextBox>();

    m_location_box->on_return_pressed = [this] {
        m_html_widget->load(m_location_box->text());
    };

    m_bookmark_button = toolbar.add<GUI::Button>();
    m_bookmark_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_bookmark_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/star-black.png"));
    m_bookmark_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_bookmark_button->set_preferred_size(22, 22);

    m_bookmark_button->on_click = [this] {
        auto url = m_html_widget->main_frame().document()->url().to_string();
        if (m_bookmarks_bar->contains_bookmark(url)) {
            m_bookmarks_bar->remove_bookmark(url);
        } else {
            m_bookmarks_bar->add_bookmark(url, m_title);
        }
        update_bookmark_button(url);
    };

    m_html_widget->on_load_start = [this](auto& url) {
        m_location_box->set_text(url.to_string());
        if (m_should_push_loads_to_history)
            m_history.push(url);
        update_actions();
        update_bookmark_button(url.to_string());
    };

    m_html_widget->on_link_click = [this](auto& url) {
        if (url.starts_with("#")) {
            m_html_widget->scroll_to_anchor(url.substring_view(1, url.length() - 1));
        } else {
            m_html_widget->load(m_html_widget->document()->complete_url(url));
        }
    };

    m_html_widget->on_title_change = [this](auto& title) {
        if (title.is_null()) {
            m_title = m_html_widget->main_frame().document()->url().to_string();
        } else {
            m_title = title;
        }
        if (on_title_change)
            on_title_change(m_title);
    };

    auto focus_location_box_action = GUI::Action::create("Focus location box", { Mod_Ctrl, Key_L }, [this](auto&) {
        m_location_box->select_all();
        m_location_box->set_focus(true);
    });

    m_statusbar = widget.add<GUI::StatusBar>();

    m_html_widget->on_link_hover = [this](auto& href) {
        m_statusbar->set_text(href);
    };

    m_bookmarks_bar->on_bookmark_hover = [this](auto&, auto& url) {
        m_statusbar->set_text(url);
    };

    Web::ResourceLoader::the().on_load_counter_change = [this] {
        if (Web::ResourceLoader::the().pending_loads() == 0) {
            m_statusbar->set_text("");
            return;
        }
        m_statusbar->set_text(String::format("Loading (%d pending resources...)", Web::ResourceLoader::the().pending_loads()));
    };

    m_menubar = GUI::MenuBar::construct();

    auto& app_menu = m_menubar->add_menu("Browser");
    app_menu.add_action(GUI::Action::create("Reload", { Mod_None, Key_F5 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"), [this](auto&) {
        TemporaryChange<bool> change(m_should_push_loads_to_history, false);
        m_html_widget->reload();
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit();
    }));

    auto& inspect_menu = m_menubar->add_menu("Inspect");
    inspect_menu.add_action(GUI::Action::create("View source", { Mod_Ctrl, Key_U }, [this](auto&) {
        String filename_to_open;
        char tmp_filename[] = "/tmp/view-source.XXXXXX";
        ASSERT(m_html_widget->document());
        if (m_html_widget->document()->url().protocol() == "file") {
            filename_to_open = m_html_widget->document()->url().path();
        } else {
            int fd = mkstemp(tmp_filename);
            ASSERT(fd >= 0);
            auto source = m_html_widget->document()->source();
            write(fd, source.characters(), source.length());
            close(fd);
            filename_to_open = tmp_filename;
        }
        if (fork() == 0) {
            execl("/bin/TextEditor", "TextEditor", filename_to_open.characters(), nullptr);
            ASSERT_NOT_REACHED();
        }
    }));
    inspect_menu.add_action(GUI::Action::create("Inspect DOM tree", { Mod_None, Key_F12 }, [this](auto&) {
        if (!m_dom_inspector_window) {
            m_dom_inspector_window = GUI::Window::construct();
            m_dom_inspector_window->set_rect(100, 100, 300, 500);
            m_dom_inspector_window->set_title("DOM inspector");
            m_dom_inspector_window->set_main_widget<InspectorWidget>();
        }
        auto* inspector_widget = static_cast<InspectorWidget*>(m_dom_inspector_window->main_widget());
        inspector_widget->set_document(m_html_widget->document());
        m_dom_inspector_window->show();
        m_dom_inspector_window->move_to_front();
    }));

    auto& debug_menu = m_menubar->add_menu("Debug");
    debug_menu.add_action(GUI::Action::create("Dump DOM tree", [this](auto&) {
        Web::dump_tree(*m_html_widget->document());
    }));
    debug_menu.add_action(GUI::Action::create("Dump Layout tree", [this](auto&) {
        Web::dump_tree(*m_html_widget->document()->layout_node());
    }));
    debug_menu.add_action(GUI::Action::create("Dump Style sheets", [this](auto&) {
        for (auto& sheet : m_html_widget->document()->stylesheets()) {
            dump_sheet(sheet);
        }
    }));
    debug_menu.add_separator();
    auto line_box_borders_action = GUI::Action::create_checkable("Line box borders", [this](auto& action) {
        m_html_widget->set_should_show_line_box_borders(action.is_checked());
        m_html_widget->update();
    });
    line_box_borders_action->set_checked(false);
    debug_menu.add_action(line_box_borders_action);

    auto& bookmarks_menu = m_menubar->add_menu("Bookmarks");
    auto show_bookmarksbar_action = GUI::Action::create_checkable("Show bookmarks bar", [this](auto& action) {
        m_bookmarks_bar->set_visible(action.is_checked());
        m_bookmarks_bar->update();
    });
    show_bookmarksbar_action->set_checked(bookmarksbar_enabled);
    bookmarks_menu.add_action(show_bookmarksbar_action);

    auto& help_menu = m_menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [this](const GUI::Action&) {
        GUI::AboutDialog::show("Browser", Gfx::Bitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window());
    }));
}

Tab::~Tab()
{
}

void Tab::load(const URL& url)
{
    m_html_widget->load(url);
}

void Tab::update_actions()
{
    m_go_back_action->set_enabled(m_history.can_go_back());
    m_go_forward_action->set_enabled(m_history.can_go_forward());
}

void Tab::update_bookmark_button(const String& url)
{
    if (m_bookmarks_bar->contains_bookmark(url)) {
        m_bookmark_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/star-yellow.png"));
    } else {
        m_bookmark_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/star-contour.png"));
    }
}

void Tab::did_become_active()
{
    GUI::Application::the().set_menubar(m_menubar);
}

}
