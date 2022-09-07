/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Brandon Jordan <brandonjordan124@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/EmojiInput.h>
#include <LibGUI/EmojiInputDialogGML.h>
#include <LibGUI/Event.h>
#include <LibGUI/Frame.h>
#include <LibGUI/TextBox.h>
#include <LibUnicode/CharacterTypes.h>
#include <stdlib.h>

namespace GUI {

static Vector<Emoji> supported_emoji_code_points()
{
    Vector<Emoji> code_points;
    Core::DirIterator dt("/res/emoji", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto filename = dt.next_path();
        auto lexical_path = LexicalPath(filename);
        if (lexical_path.extension() != "png")
            continue;
        auto basename = lexical_path.basename();
        if (!basename.starts_with("U+"sv))
            continue;
        // FIXME: Handle multi code point emojis.
        if (basename.contains('_'))
            continue;

        u32 code_point = strtoul(basename.to_string().characters() + 2, nullptr, 16);

        String display_name = "";
        if (auto code_point_display_name = Unicode::code_point_display_name(code_point); code_point_display_name.has_value()) {
            display_name = String::formatted("{}", code_point_display_name.value()).to_titlecase();
        }
        code_points.append({ display_name, code_point });
    }
    return code_points;
}

EmojiInput::EmojiInput(Window* parent_window)
    : Dialog(parent_window)
{
    resize(300, 250);
    set_frameless(true);

    auto& main_widget = set_main_widget<Frame>();
    if (!main_widget.load_from_gml(emoji_input_dialog_gml))
        VERIFY_NOT_REACHED();

    main_widget.set_fill_with_background_color(true);
    main_widget.set_frame_shape(Gfx::FrameShape::Window);
    main_widget.set_frame_shadow(Gfx::FrameShadow::Raised);

    auto& scrollable_emojis_widget = *main_widget.find_descendant_of_type_named<AbstractScrollableWidget>("scrollable_emojis_widget");
    m_search_textbox = *main_widget.find_descendant_of_type_named<GUI::TextBox>("search_textbox");

    auto code_points = supported_emoji_code_points();
    display_emojis(code_points, main_widget);

    m_search_textbox->on_change = [this, &main_widget, &scrollable_emojis_widget] {
        Vector<Emoji> filtered_codepoints;
        auto term = m_search_textbox->text();
        auto code_points = supported_emoji_code_points();
        for (size_t idx = 0; idx < code_points.size(); ++idx) {
            if (code_points[idx].display_name.contains(term, AK::CaseSensitivity::CaseInsensitive))
                filtered_codepoints.append(code_points[idx]);
        }
        display_emojis(filtered_codepoints, main_widget);
        scrollable_emojis_widget.scroll_to_top();
    };

    on_active_window_change = [this](bool is_active_window) {
        if (!is_active_window)
            close();
    };

    m_search_textbox->set_focus(true);
}

void EmojiInput::display_emojis(Vector<Emoji>& code_points, GUI::Frame& main_widget)
{
    size_t index = 0;
    size_t columns = 13;
    size_t rows = ceil_div(code_points.size(), columns);
    constexpr int button_size = 20;

    auto& emojis_widget = *main_widget.find_descendant_of_type_named<Widget>("emojis_widget");
    emojis_widget.remove_all_children();

    for (size_t row = 0; row < rows && index < code_points.size(); ++row) {
        auto& horizontal_container = emojis_widget.add<Widget>();
        auto& horizontal_layout = horizontal_container.set_layout<HorizontalBoxLayout>();
        horizontal_container.set_preferred_height(GUI::SpecialDimension::Shrink);
        horizontal_layout.set_spacing(0);
        for (size_t column = 0; column < columns; ++column) {
            if (index < code_points.size()) {
                // FIXME: Also emit U+FE0F for single code point emojis, currently
                // they get shown as text glyphs if available.
                // This will require buttons to don't calculate their length as 2,
                // currently it just shows an ellipsis. It will also require some
                // tweaking of the mechanism that is currently being used to insert
                // which is a key event with a single code point.
                StringBuilder builder;
                builder.append(Utf32View(&code_points[index].code_point, 1));
                auto emoji_text = builder.to_string();
                auto& button = horizontal_container.add<Button>(emoji_text);
                button.set_fixed_size(button_size, button_size);
                button.set_button_style(Gfx::ButtonStyle::Coolbar);

                if (code_points[index].display_name != "") {
                    button.set_tooltip(code_points[index].display_name);
                }

                button.on_click = [this, button = &button](auto) {
                    m_selected_emoji_text = button->text();
                    done(ExecResult::OK);
                };

                index++;
            } else {
                horizontal_container.add<Widget>();
            }
        }
    }
}

void EmojiInput::event(Core::Event& event)
{
    if (event.type() == Event::KeyDown) {
        auto& key_event = static_cast<KeyEvent&>(event);
        if (key_event.key() == Key_Escape) {
            done(ExecResult::Cancel);
            return;
        }
    }
    Dialog::event(event);
}

}
