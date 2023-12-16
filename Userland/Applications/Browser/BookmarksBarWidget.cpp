/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/BookmarksBarWidget.h>
#include <Applications/Browser/Browser.h>
#include <Applications/Browser/EditBookmarkGML.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace Browser {

namespace {

enum class PerformEditOn {
    NewBookmark,
    ExistingBookmark
};

class BookmarkEditor final : public GUI::Dialog {
    C_OBJECT(BookmarkEditor)

public:
    static Vector<JsonValue>
    edit_bookmark(Window* parent_window, StringView title, StringView url, PerformEditOn perform_edit_on)
    {
        auto editor = BookmarkEditor::construct(parent_window, title, url);
        if (perform_edit_on == PerformEditOn::NewBookmark) {
            editor->set_title("Add Bookmark");
        } else {
            editor->set_title("Edit Bookmark");
        }
        editor->set_icon(g_icon_bag.bookmark_filled);

        if (editor->exec() == ExecResult::OK) {
            return Vector<JsonValue> { editor->title(), editor->url() };
        }

        return {};
    }

private:
    BookmarkEditor(Window* parent_window, StringView title, StringView url)
        : Dialog(parent_window)
    {
        auto widget = set_main_widget<GUI::Widget>();
        widget->load_from_gml(edit_bookmark_gml).release_value_but_fixme_should_propagate_errors();

        set_resizable(false);
        resize(260, 85);

        m_title_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("title_textbox");
        m_title_textbox->set_text(title);
        m_title_textbox->set_focus(true);
        m_title_textbox->select_all();

        auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
        ok_button.on_click = [this](auto) {
            done(ExecResult::OK);
        };
        ok_button.set_default(true);

        m_url_textbox = *widget->find_descendant_of_type_named<GUI::TextBox>("url_textbox");
        m_url_textbox->set_text(url);
        m_url_textbox->on_change = [this, &ok_button]() {
            auto has_url = !m_url_textbox->text().is_empty();
            ok_button.set_enabled(has_url);
        };

        auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
        cancel_button.on_click = [this](auto) {
            done(ExecResult::Cancel);
        };
    }

    ByteString title() const
    {
        return m_title_textbox->text();
    }

    ByteString url() const
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

BookmarksBarWidget::BookmarksBarWidget(ByteString const& bookmarks_file, bool enabled)
{
    s_the = this;
    set_layout<GUI::HorizontalBoxLayout>(2, 0);

    set_fixed_height(20);

    if (!enabled)
        set_visible(false);

    m_additional = GUI::Button::construct();
    m_additional->set_tooltip("Show hidden bookmarks"_string);
    m_additional->set_menu(m_additional_menu);
    auto bitmap_or_error = Gfx::Bitmap::load_from_file("/res/icons/16x16/overflow-menu.png"sv);
    if (!bitmap_or_error.is_error())
        m_additional->set_icon(bitmap_or_error.release_value());
    m_additional->set_button_style(Gfx::ButtonStyle::Coolbar);
    m_additional->set_fixed_size(22, 20);
    m_additional->set_focus_policy(GUI::FocusPolicy::TabFocus);

    m_separator = GUI::Widget::construct();

    m_context_menu = GUI::Menu::construct();
    auto default_action = GUI::Action::create(
        "&Open", g_icon_bag.go_to, [this](auto&) {
            if (on_bookmark_click)
                on_bookmark_click(m_context_menu_url, Open::InSameTab);
        },
        this);
    m_context_menu_default_action = default_action;
    m_context_menu->add_action(default_action);
    m_context_menu->add_action(GUI::Action::create(
        "Open in New &Tab", g_icon_bag.new_tab, [this](auto&) {
            if (on_bookmark_click)
                on_bookmark_click(m_context_menu_url, Open::InNewTab);
        },
        this));
    m_context_menu->add_action(GUI::Action::create(
        "Open in New Window", g_icon_bag.new_window, [this](auto&) {
            if (on_bookmark_click) {
                on_bookmark_click(m_context_menu_url, Open::InNewWindow);
            }
        },
        this));
    m_context_menu->add_separator();
    m_context_menu->add_action(GUI::Action::create(
        "&Edit...", g_icon_bag.rename, [this](auto&) {
            if (auto result = edit_bookmark(m_context_menu_url); result.is_error())
                GUI::MessageBox::show_error(this->window(), MUST(String::formatted("Failed to edit bookmark: {}", result.error())));
        },
        this));
    m_context_menu->add_action(GUI::CommonActions::make_delete_action(
        [this](auto&) {
            if (auto result = remove_bookmark(m_context_menu_url); result.is_error())
                GUI::MessageBox::show_error(this->window(), MUST(String::formatted("Failed to remove bookmark: {}", result.error())));
        },
        this));

    Vector<GUI::JsonArrayModel::FieldSpec> fields;
    fields.empend("title", "Title"_string, Gfx::TextAlignment::CenterLeft);
    fields.empend("url", "Url"_string, Gfx::TextAlignment::CenterRight);
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

        auto title = model()->index(item_index, 0).data().to_byte_string();
        auto url = model()->index(item_index, 1).data().to_byte_string();

        Gfx::IntRect rect { width, 0, font().width_rounded_up(title) + 32, height() };

        auto& button = add<GUI::Button>();
        m_bookmarks.append(button);

        button.set_button_style(Gfx::ButtonStyle::Coolbar);
        button.set_text(String::from_byte_string(title).release_value_but_fixme_should_propagate_errors());
        button.set_icon(g_icon_bag.filetype_html);
        button.set_fixed_size(font().width(title) + 32, 20);
        button.set_relative_rect(rect);
        button.set_focus_policy(GUI::FocusPolicy::TabFocus);
        button.set_tooltip(MUST(String::from_byte_string(url)));
        button.set_allowed_mouse_buttons_for_pressing(GUI::MouseButton::Primary | GUI::MouseButton::Middle);

        button.on_click = [title, url, this](auto) {
            if (on_bookmark_click)
                on_bookmark_click(url, Open::InSameTab);
        };

        button.on_middle_mouse_click = [title, url, this](auto) {
            if (on_bookmark_click)
                on_bookmark_click(url, Open::InNewTab);
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
        if (x_position + bookmark->width() + m_additional->width() > width()) {
            m_last_visible_index = i;
            break;
        }
        bookmark->set_x(x_position);
        bookmark->set_visible(true);
        x_position += bookmark->width();
    }

    if (m_last_visible_index < 0) {
        m_additional->set_visible(false);
    } else {
        // hide all items > m_last_visible_index and create new bookmarks menu for them
        m_additional->set_visible(true);
        m_additional_menu = GUI::Menu::construct("Additional Bookmarks"_string);
        m_additional->set_menu(m_additional_menu);
        for (size_t i = m_last_visible_index; i < m_bookmarks.size(); ++i) {
            auto& bookmark = m_bookmarks.at(i);
            bookmark->set_visible(false);
            m_additional_menu->add_action(GUI::Action::create(bookmark->text().to_byte_string(), g_icon_bag.filetype_html, [&](auto&) { bookmark->on_click(0); }));
        }
    }
}

bool BookmarksBarWidget::contains_bookmark(StringView url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto item_url = model()->index(item_index, 1).data().to_byte_string();
        if (item_url == url) {
            return true;
        }
    }
    return false;
}

ErrorOr<void> BookmarksBarWidget::remove_bookmark(StringView url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {

        auto item_url = model()->index(item_index, 1).data().to_byte_string();
        if (item_url == url) {
            auto& json_model = *static_cast<GUI::JsonArrayModel*>(model());

            TRY(json_model.remove(item_index));
            TRY(json_model.store());

            if (on_bookmark_change)
                on_bookmark_change();

            return {};
        }
    }

    return Error::from_string_view("Bookmark not found"sv);
}

ErrorOr<void> BookmarksBarWidget::add_bookmark(StringView url, StringView title)
{
    Vector<JsonValue> values;
    TRY(values.try_append(title));
    TRY(values.try_append(url));

    TRY(update_model(values, [](auto& model, auto&& values) -> ErrorOr<void> {
        return TRY(model.add(move(values)));
    }));

    if (on_bookmark_change)
        on_bookmark_change();

    values = BookmarkEditor::edit_bookmark(window(), title, url, PerformEditOn::NewBookmark);
    if (values.is_empty())
        return remove_bookmark(url);

    auto model_has_updated = false;
    for (int item_index = 0; item_index < model()->row_count(); item_index++) {
        auto item_url = model()->index(item_index, 1).data().to_byte_string();

        if (item_url == url) {
            TRY(update_model(values, [item_index](auto& model, auto&& values) {
                return model.set(item_index, move(values));
            }));
            model_has_updated = true;
            break;
        }
    }

    if (!model_has_updated)
        return Error::from_string_view("Bookmark not found"sv);

    if (on_bookmark_change)
        on_bookmark_change();

    return {};
}

ErrorOr<void> BookmarksBarWidget::edit_bookmark(StringView url)
{
    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {
        auto item_title = model()->index(item_index, 0).data().to_byte_string();
        auto item_url = model()->index(item_index, 1).data().to_byte_string();

        if (item_url == url) {
            auto values = BookmarkEditor::edit_bookmark(window(), item_title, item_url, PerformEditOn::ExistingBookmark);
            if (values.is_empty())
                return {};

            TRY(update_model(values, [item_index](auto& model, auto&& values) {
                return model.set(item_index, move(values));
            }));

            if (on_bookmark_change)
                on_bookmark_change();

            return {};
        }
    }

    return Error::from_string_view("Bookmark not found"sv);
}

ErrorOr<void> BookmarksBarWidget::update_model(Vector<JsonValue>& values, Function<ErrorOr<void>(GUI::JsonArrayModel& model, Vector<JsonValue>&& values)> perform_model_change)
{
    if (values.is_empty())
        return Error::from_string_view("No values to update model with"sv);

    auto& json_model = *static_cast<GUI::JsonArrayModel*>(model());

    TRY(perform_model_change(json_model, move(values)));
    TRY(json_model.store());

    return {};
}

}
