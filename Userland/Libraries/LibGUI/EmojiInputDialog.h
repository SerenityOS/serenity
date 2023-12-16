/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <LibGUI/Dialog.h>
#include <LibUnicode/Emoji.h>

namespace GUI {

class EmojiInputDialog final : public Dialog {
    C_OBJECT(EmojiInputDialog);

    struct Emoji {
        RefPtr<Button> button;
        Unicode::Emoji emoji;
        ByteString text;
    };

public:
    ByteString const& selected_emoji_text() const { return m_selected_emoji_text; }

private:
    explicit EmojiInputDialog(Window* parent_window);

    Vector<Emoji> supported_emoji();
    void update_displayed_emoji();
    void select_first_displayed_emoji();

    OwnPtr<ActionGroup> m_category_action_group;
    Optional<Unicode::EmojiGroup> m_selected_category;

    RefPtr<TextBox> m_search_box;
    RefPtr<Toolbar> m_toolbar;
    RefPtr<Widget> m_emojis_widget;
    Vector<Emoji> m_emojis;
    Emoji const* m_first_displayed_emoji { nullptr };
    ByteString m_selected_emoji_text;
};

}
