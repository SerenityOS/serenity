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
        u32 code_point { 0 };
        RefPtr<Button> button;
        Optional<Unicode::Emoji> emoji;
    };

public:
    String const& selected_emoji_text() const { return m_selected_emoji_text; }

private:
    virtual void event(Core::Event&) override;
    explicit EmojiInputDialog(Window* parent_window);

    Vector<Emoji> supported_emoji();
    void update_displayed_emoji();

    OwnPtr<ActionGroup> m_category_action_group;
    Optional<Unicode::EmojiGroup> m_selected_category;

    RefPtr<TextBox> m_search_box;
    RefPtr<Toolbar> m_toolbar;
    RefPtr<Widget> m_emojis_widget;
    Vector<Emoji> m_emojis;
    String m_selected_emoji_text;
};

}
