/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "BookmarksBarWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Event.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace Browser {

static BookmarksBarWidget* s_the;

BookmarksBarWidget& BookmarksBarWidget::the()
{
    return *s_the;
}

BookmarksBarWidget::BookmarksBarWidget(const String& bookmarks_file, bool enabled)
{
    s_the = this;
    set_layout<GUI::HorizontalBoxLayout>();
    layout()->set_spacing(0);

    set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    set_preferred_size(0, 20);

    if (!enabled)
        set_visible(false);

    m_additional = GUI::Button::construct();
    m_additional->set_button_style(Gfx::ButtonStyle::CoolBar);
    m_additional->set_text(">");
    m_additional->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_additional->set_preferred_size(14, 20);
    m_additional->on_click = [this](auto) {
        if (m_additional_menu) {
            m_additional_menu->popup(m_additional->relative_position().translated(relative_position().translated(m_additional->window()->position())));
        }
    };

    m_separator = GUI::Widget::construct();

    m_context_menu = GUI::Menu::construct();
    auto default_action = GUI::Action::create("Open", [this](auto&) {
        if (on_bookmark_click)
            on_bookmark_click(m_context_menu_url, Mod_None);
    });
    m_context_menu_default_action = default_action;
    m_context_menu->add_action(default_action);
    m_context_menu->add_action(GUI::Action::create("Open in new tab", [this](auto&) {
        if (on_bookmark_click)
            on_bookmark_click(m_context_menu_url, Mod_Ctrl);
    }));
    m_context_menu->add_action(GUI::Action::create("Delete", [this](auto&) {
        remove_bookmark(m_context_menu_url);
    }));

    Vector<GUI::JsonArrayModel::FieldSpec> fields;
    fields.empend("title", "Title", Gfx::TextAlignment::CenterLeft);
    fields.empend("url", "Url", Gfx::TextAlignment::CenterRight);
    set_model(GUI::JsonArrayModel::create(bookmarks_file, move(fields)));
    model()->update();
}

BookmarksBarWidget::~BookmarksBarWidget()
{
    if (m_model)
        m_model->unregister_client(*this);
}

void BookmarksBarWidget::set_model(RefPtr<GUI::Model> model)
{
    if (model == m_model)
        return;
    if (m_model)
        m_model->unregister_client(*this);
    m_model = move(model);
    m_model->register_client(*this);
}

void BookmarksBarWidget::resize_event(GUI::ResizeEvent& event)
{
    Widget::resize_event(event);
    update_content_size();
}

void BookmarksBarWidget::model_did_update(unsigned)
{
    for (auto* child : child_widgets()) {
        child->remove_from_parent();
    }

    m_bookmarks.clear();

    int width = 0;
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto title = model()->index(item_index, 0).data().to_string();
        auto url = model()->index(item_index, 1).data().to_string();

        Gfx::IntRect rect { width, 0, font().width(title) + 32, height() };

        auto& button = add<GUI::Button>();
        m_bookmarks.append(button);

        button.set_button_style(Gfx::ButtonStyle::CoolBar);
        button.set_text(title);
        button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));
        button.set_preferred_size(font().width(title) + 32, 20);
        button.set_relative_rect(rect);

        button.on_click = [title, url, this](auto modifiers) {
            if (on_bookmark_click)
                on_bookmark_click(url, modifiers);
        };

        button.on_context_menu_request = [this, url](auto& context_menu_event) {
            m_context_menu_url = url;
            m_context_menu->popup(context_menu_event.screen_position(), m_context_menu_default_action);
        };

        width += rect.width();
    }

    add_child(*m_separator);
    add_child(*m_additional);

    update_content_size();
    update();
}

void BookmarksBarWidget::update_content_size()
{
    int x_position = 0;
    m_last_visible_index = -1;

    for (size_t i = 0; i < m_bookmarks.size(); ++i) {
        auto& bookmark = m_bookmarks.at(i);
        if (x_position + bookmark.width() > width()) {
            m_last_visible_index = i;
            break;
        }
        bookmark.set_x(x_position);
        bookmark.set_visible(true);
        x_position += bookmark.width();
    }

    if (m_last_visible_index < 0) {
        m_additional->set_visible(false);
    } else {
        // hide all items > m_last_visible_index and create new bookmarks menu for them
        m_additional->set_visible(true);
        m_additional_menu = GUI::Menu::construct("Additional Bookmarks");
        for (size_t i = m_last_visible_index; i < m_bookmarks.size(); ++i) {
            auto& bookmark = m_bookmarks.at(i);
            bookmark.set_visible(false);
            m_additional_menu->add_action(GUI::Action::create(bookmark.text(),
                Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"),
                [&](auto&) {
                    bookmark.on_click(0);
                }));
        }
    }
}

bool BookmarksBarWidget::contains_bookmark(const String& url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto item_title = model()->index(item_index, 0).data().to_string();
        auto item_url = model()->index(item_index, 1).data().to_string();
        if (item_url == url) {
            return true;
        }
    }
    return false;
}

bool BookmarksBarWidget::remove_bookmark(const String& url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto item_title = model()->index(item_index, 0).data().to_string();
        auto item_url = model()->index(item_index, 1).data().to_string();
        if (item_url == url) {
            auto& json_model = *static_cast<GUI::JsonArrayModel*>(model());

            const auto item_removed = json_model.remove(item_index);
            if (item_removed)
                json_model.store();

            return item_removed;
        }
    }

    return false;
}
bool BookmarksBarWidget::add_bookmark(const String& url, const String& title)
{
    Vector<JsonValue> values;
    values.append(title);
    values.append(url);

    auto& json_model = *static_cast<GUI::JsonArrayModel*>(model());
    if (json_model.add(move(values))) {
        json_model.store();
        return true;
    }
    return false;
}

}
