/*
 * Copyright (c) 2024, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EditBookmarkWidget.h"
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGUI/Dialog.h>

namespace Browser {

    enum class PerformEditOn {
        NewBookmark,
        ExistingBookmark
    };

    class EditBookmarkDialog final : public GUI::Dialog {
    C_OBJECT_ABSTRACT(EditBookmarkDialog)

    public:
        static Vector<JsonValue>
        edit_bookmark(GUI::Window *parent_window, RefPtr<Gfx::Bitmap> icon, StringView title, StringView url, PerformEditOn perform_edit_on);
        static ErrorOr<NonnullRefPtr<EditBookmarkDialog>> try_create(Window* parent_window, StringView title, StringView url);

    private:
        EditBookmarkDialog(GUI::Window* parent_window, StringView title, StringView url, NonnullRefPtr<EditBookmarkWidget> edit_bookmark_widget);

        ByteString title() const;

        ByteString url() const;

        RefPtr<GUI::TextBox> m_title_textbox;
        RefPtr<GUI::TextBox> m_url_textbox;
    };

}
