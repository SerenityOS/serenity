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

#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/Frame.h>
#include <stdlib.h>

namespace GUI {

static Vector<u32> supported_emoji_codepoints()
{
    Vector<u32> codepoints;
    Core::DirIterator dt("/res/emoji", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto filename = dt.next_path();
        auto fspath = FileSystemPath(filename);
        if (fspath.extension() != "png")
            continue;
        auto basename = fspath.basename();
        if (!basename.starts_with("U+"))
            continue;
        u32 codepoint = strtoul(basename.characters() + 2, nullptr, 16);
        codepoints.append(codepoint);
    }
    return codepoints;
}

EmojiInputDialog::EmojiInputDialog(Window* parent_window)
    : Dialog(parent_window)
{
    set_frameless(true);

    auto& main_widget = set_main_widget<Frame>();
    main_widget.set_frame_shape(Gfx::FrameShape::Container);
    main_widget.set_frame_shadow(Gfx::FrameShadow::Raised);
    main_widget.set_fill_with_background_color(true);
    auto& main_layout = main_widget.set_layout<VerticalBoxLayout>();
    main_layout.set_spacing(0);

    auto codepoints = supported_emoji_codepoints();

    size_t index = 0;
    size_t columns = 5;
    size_t rows = ceil_div(codepoints.size(), columns);

    for (size_t row = 0; row < rows && index < codepoints.size(); ++row) {
        auto& horizontal_container = main_widget.add<Widget>();
        auto& horizontal_layout = horizontal_container.set_layout<HorizontalBoxLayout>();
        horizontal_layout.set_spacing(0);
        for (size_t column = 0; column < columns; ++column) {
            StringBuilder builder;
            builder.append(Utf32View(&codepoints[index++], 1));
            auto emoji_text = builder.to_string();
            auto& button = horizontal_container.add<Button>(emoji_text);
            button.set_button_style(Gfx::ButtonStyle::CoolBar);
            button.on_click = [this, button = &button](auto) {
                m_selected_emoji_text = button->text();
                done(ExecOK);
            };
            if (index >= codepoints.size()) {
                horizontal_layout.add_spacer();
                break;
            }
        }
    }
}

void EmojiInputDialog::event(Core::Event& event)
{
    if (event.type() == Event::KeyDown) {
        auto& key_event = static_cast<KeyEvent&>(event);
        if (key_event.key() == Key_Escape) {
            done(ExecCancel);
            return;
        }
    }
    Dialog::event(event);
}

}
