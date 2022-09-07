/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Brandon Jordan <brandonjordan124@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

struct Emoji {
    String display_name;
    u32 code_point;
};

class EmojiInput final : public Dialog {
    C_OBJECT(EmojiInput);

public:
    String const& selected_emoji_text() const { return m_selected_emoji_text; }

private:
    virtual void event(Core::Event&) override;
    explicit EmojiInput(Window* parent_window);
    void display_emojis(Vector<Emoji>& code_points, GUI::Frame& main_widget);

    String m_selected_emoji_text;
    RefPtr<TextBox> m_search_textbox;
};

}
