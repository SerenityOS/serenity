/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EditBookmarkDialog.h"
#include <AK/JsonValue.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Button.h>

namespace Browser {

    Vector<JsonValue> EditBookmarkDialog::edit_bookmark(GUI::Window *parent_window, RefPtr<Gfx::Bitmap> icon, StringView title, StringView url,
                                      PerformEditOn perform_edit_on) {
        auto editor_or_error = EditBookmarkDialog::try_create(parent_window, move(title), move(url));
        if (editor_or_error.is_error()) {
            GUI::MessageBox::show(parent_window, "Couldn't load \"edit bookmark\" dialog"sv, "Error while opening \"edit bookmark\" dialog"sv, GUI::MessageBox::Type::Error);
            return {};
        }

        auto editor = editor_or_error.release_value();

        if (perform_edit_on == PerformEditOn::NewBookmark) {
            editor->set_title("Add Bookmark");
        } else {
            editor->set_title("Edit Bookmark");
        }
        editor->set_icon(icon);

        if (editor->exec() == ExecResult::OK) {
            return Vector < JsonValue > {editor->title(), editor->url()};
        }

        return {};
    }

    ErrorOr<NonnullRefPtr<EditBookmarkDialog>>
    EditBookmarkDialog::try_create(GUI::Window *parent_window, StringView title, StringView url) {
        auto edit_bookmark_widget = TRY(EditBookmarkWidget::try_create());
        auto edit_bookmark_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow)
                                                                     EditBookmarkDialog(parent_window, move(title), move(url), edit_bookmark_widget)));
        return edit_bookmark_dialog;
    }

    EditBookmarkDialog::EditBookmarkDialog(GUI::Window *parent_window, StringView title, StringView url, NonnullRefPtr<EditBookmarkWidget> edit_bookmark_widget)
            : Dialog(parent_window) {
        set_resizable(false);
        resize(260, 85);

        set_main_widget(edit_bookmark_widget);

        m_title_textbox = *edit_bookmark_widget->find_descendant_of_type_named<GUI::TextBox>("title_textbox");
        m_title_textbox->set_text(title);
        m_title_textbox->set_focus(true);
        m_title_textbox->select_all();

        auto &ok_button = *edit_bookmark_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
        ok_button.on_click = [this](auto) {
            done(ExecResult::OK);
        };
        ok_button.set_default(true);

        m_url_textbox = *edit_bookmark_widget->find_descendant_of_type_named<GUI::TextBox>("url_textbox");
        m_url_textbox->set_text(url);
        m_url_textbox->on_change = [this, &ok_button]() {
            auto has_url = !m_url_textbox->text().is_empty();
            ok_button.set_enabled(has_url);
        };

        auto &cancel_button = *edit_bookmark_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
        cancel_button.on_click = [this](auto) {
            done(ExecResult::Cancel);
        };
    }

    ByteString EditBookmarkDialog::title() const {
        return m_title_textbox->text();
    }

    ByteString EditBookmarkDialog::url() const {
        return m_url_textbox->text();
    }

}
