/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BookmarksBarWidget.h"
#include <Applications/Browser/EditBookmarkGML.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace Browser {

namespace {

class BookmarkEditor final : public GUI::Dialog {
    C_OBJECT(BookmarkEditor)

public:
    static Vector<JsonValue>
    edit_bookmark(Window* parent_window, StringView title, StringView url)
    {
        auto editor = BookmarkEditor::construct(parent_window, title, url);
        editor->set_title("Edit Bookmark");

        if (editor->exec() == Dialog::ExecOK) {
            return Vector<JsonValue> { editor->title(), editor->url() };
        }

        return {};
    }

private:
    BookmarkEditor(Window* parent_window, StringView title, StringView url)
        : Dialog(parent_window)
    {
        auto& widget = set_main_widget<GUI::Widget>();
        if (!widget.load_from_gml(edit_bookmark_gml))
            VERIFY_NOT_REACHED();

        set_resizable(false);
        resize(260, 85);

        m_title_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("title_textbox");
        m_title_textbox->set_text(title);
        m_title_textbox->set_focus(true);
        m_title_textbox->select_all();
        m_title_textbox->on_return_pressed = [this] {
            done(Dialog::ExecOK);
        };

        m_url_textbox = *widget.find_descendant_of_type_named<GUI::TextBox>("url_textbox");
        m_url_textbox->set_text(url);
        m_url_textbox->on_return_pressed = [this] {
            done(Dialog::ExecOK);
        };

        auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
        ok_button.on_click = [this](auto) {
            done(Dialog::ExecOK);
        };

        auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
        cancel_button.on_click = [this](auto) {
            done(Dialog::ExecCancel);
        };
    }

    String title() const
    {
        return m_title_textbox->text();
    }

    String url() const
    {
        return m_url_textbox->text();
    }

    RefPtr<GUI::TextBox> m_title_textbox;
    RefPtr<GUI::TextBox> m_url_textbox;
};

}

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

    set_fixed_height(20);

    if (!enabled)
        set_visible(false);

    m_additional = GUI::Button::construct();
    m_additional->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_additional->set_text(">");
    m_additional->set_fixed_size(14, 20);
    m_additional->set_focus_policy(GUI::FocusPolicy::TabFocus);
    m_additional->on_click = [this](auto) {
        if (m_additional_menu) {
            m_additional_menu->popup(m_additional->relative_position().translated(relative_position().translated(m_additional->window()->position())));
        }
    };

    m_separator = GUI::Widget::construct();

    m_context_menu = GUI::Menu::construct();
    auto default_action = GUI::Action::create(
        "&Open", [this](auto&) {
            if (on_bookmark_click)
                on_bookmark_click(m_context_menu_url, Mod_None);
        },
        this);
    m_context_menu_default_action = default_action;
    m_context_menu->add_action(default_action);
    m_context_menu->add_action(GUI::Action::create(
        "Open in New &Tab", [this](auto&) {
            if (on_bookmark_click)
                on_bookmark_click(m_context_menu_url, Mod_Ctrl);
        },
        this));
    m_context_menu->add_separator();
    m_context_menu->add_action(GUI::Action::create(
        "&Edit...", [this](auto&) {
            edit_bookmark(m_context_menu_url);
        },
        this));
    m_context_menu->add_action(GUI::CommonActions::make_delete_action(
        [this](auto&) {
            remove_bookmark(m_context_menu_url);
        },
        this));

    Vector<GUI::JsonArrayModel::FieldSpec> fields;
    fields.empend("title", "Title", Gfx::TextAlignment::CenterLeft);
    fields.empend("url", "Url", Gfx::TextAlignment::CenterRight);
    set_model(GUI::JsonArrayModel::create(bookmarks_file, move(fields)));
    model()->invalidate();
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
    remove_all_children();

    m_bookmarks.clear();

    int width = 0;
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto title = model()->index(item_index, 0).data().to_string();
        auto url = model()->index(item_index, 1).data().to_string();

        Gfx::IntRect rect { width, 0, font().width(title) + 32, height() };

        auto& button = add<GUI::Button>();
        m_bookmarks.append(button);

        button.set_button_style(Gfx::ButtonStyle::Coolbar);
        button.set_text(title);
        button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png").release_value_but_fixme_should_propagate_errors());
        button.set_fixed_size(font().width(title) + 32, 20);
        button.set_relative_rect(rect);
        button.set_focus_policy(GUI::FocusPolicy::TabFocus);
        button.set_tooltip(url);

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
                Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png").release_value_but_fixme_should_propagate_errors(),
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

bool BookmarksBarWidget::edit_bookmark(const String& url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {
        auto item_title = model()->index(item_index, 0).data().to_string();
        auto item_url = model()->index(item_index, 1).data().to_string();

        if (item_url == url) {
            auto values = BookmarkEditor::edit_bookmark(window(), item_title, item_url);
            bool item_replaced = false;

            if (!values.is_empty()) {
                auto& json_model = *static_cast<GUI::JsonArrayModel*>(model());
                item_replaced = json_model.set(item_index, move(values));

                if (item_replaced)
                    json_model.store();
            }

            return item_replaced;
        }
    }

    return false;
}

}
