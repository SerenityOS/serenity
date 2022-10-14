/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FuzzyMatch.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/Utf32View.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/EmojiInputDialogGML.h>
#include <LibGUI/Event.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ScrollableContainerWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/Emoji.h>
#include <stdlib.h>

namespace GUI {

struct EmojiCateogry {
    Unicode::EmojiGroup group;
    u32 emoji_code_point { 0 };
};

static constexpr auto s_emoji_groups = Array {
    EmojiCateogry { Unicode::EmojiGroup::SmileysAndEmotion, 0x1F600 },
    EmojiCateogry { Unicode::EmojiGroup::PeopleAndBody, 0x1FAF3 },
    EmojiCateogry { Unicode::EmojiGroup::AnimalsAndNature, 0x1F33B },
    EmojiCateogry { Unicode::EmojiGroup::FoodAndDrink, 0x1F355 },
    EmojiCateogry { Unicode::EmojiGroup::TravelAndPlaces, 0x1F3D6 },
    EmojiCateogry { Unicode::EmojiGroup::Activities, 0x1F3B3 },
    EmojiCateogry { Unicode::EmojiGroup::Objects, 0x1F4E6 },
    EmojiCateogry { Unicode::EmojiGroup::Symbols, 0x2764 },
    EmojiCateogry { Unicode::EmojiGroup::Flags, 0x1F6A9 },
    EmojiCateogry { Unicode::EmojiGroup::SerenityOS, 0x10CD0B },
};

static void resize_bitmap_if_needed(RefPtr<Gfx::Bitmap>& bitmap)
{
    constexpr int max_icon_size = 12;

    if ((bitmap->width() > max_icon_size) || (bitmap->height() > max_icon_size)) {
        auto x_ratio = static_cast<float>(max_icon_size) / static_cast<float>(bitmap->width());
        auto y_ratio = static_cast<float>(max_icon_size) / static_cast<float>(bitmap->height());
        auto ratio = min(x_ratio, y_ratio);

        bitmap = bitmap->scaled(ratio, ratio).release_value_but_fixme_should_propagate_errors();
    }
}

class EmojiButton final : public Button {
    C_OBJECT(EmojiButton);

private:
    explicit EmojiButton(Vector<u32> emoji_code_points)
        : Button()
        , m_emoji_code_points(move(emoji_code_points))
    {
    }

    virtual void paint_event(PaintEvent& event) override
    {
        if (m_first_paint_event) {
            m_first_paint_event = false;

            RefPtr<Gfx::Bitmap> bitmap = Gfx::Emoji::emoji_for_code_points(m_emoji_code_points);
            VERIFY(bitmap);

            resize_bitmap_if_needed(bitmap);
            set_icon(move(bitmap));
        }

        Button::paint_event(event);
    }

    bool m_first_paint_event { true };
    Vector<u32> m_emoji_code_points;
};

EmojiInputDialog::EmojiInputDialog(Window* parent_window)
    : Dialog(parent_window)
    , m_category_action_group(make<ActionGroup>())
{
    auto& main_widget = set_main_widget<Frame>();
    if (!main_widget.load_from_gml(emoji_input_dialog_gml))
        VERIFY_NOT_REACHED();

    set_frameless(true);
    set_blocks_emoji_input(true);
    set_window_mode(GUI::WindowMode::CaptureInput);
    resize(400, 300);

    auto& scrollable_container = *main_widget.find_descendant_of_type_named<GUI::ScrollableContainerWidget>("scrollable_container"sv);
    m_search_box = main_widget.find_descendant_of_type_named<GUI::TextBox>("search_box"sv);
    m_toolbar = main_widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar"sv);
    m_emojis_widget = main_widget.find_descendant_of_type_named<GUI::Widget>("emojis"sv);
    m_emojis = supported_emoji();

    m_category_action_group->set_exclusive(true);
    m_category_action_group->set_unchecking_allowed(true);

    for (auto const& category : s_emoji_groups) {
        auto name = Unicode::emoji_group_to_string(category.group);
        auto tooltip = name.replace("&"sv, "&&"sv, ReplaceMode::FirstOnly);

        RefPtr<Gfx::Bitmap> bitmap = Gfx::Emoji::emoji_for_code_point(category.emoji_code_point);
        VERIFY(bitmap);
        resize_bitmap_if_needed(bitmap);

        auto set_filter_action = Action::create_checkable(
            tooltip, bitmap, [this, group = category.group](auto& action) {
                if (action.is_checked())
                    m_selected_category = group;
                else
                    m_selected_category = {};

                m_search_box->set_text({}, AllowCallback::No);
                update_displayed_emoji();
            },
            this);

        m_category_action_group->add_action(*set_filter_action);
        m_toolbar->add_action(*set_filter_action);
    }

    scrollable_container.horizontal_scrollbar().set_visible(false);
    update_displayed_emoji();

    on_active_input_change = [this](bool is_active_input) {
        if (!is_active_input)
            close();
    };

    on_input_preemption = [this](InputPreemptor preemptor) {
        if (preemptor != InputPreemptor::ContextMenu)
            close();
    };

    m_search_box->on_change = [this]() {
        update_displayed_emoji();
    };

    m_search_box->on_return_pressed = [this]() {
        select_first_displayed_emoji();
    };
}

auto EmojiInputDialog::supported_emoji() -> Vector<Emoji>
{
    constexpr int button_size = 20;

    Vector<Emoji> emojis;
    Core::DirIterator dt("/res/emoji", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto filename = dt.next_path();
        auto lexical_path = LexicalPath(filename);
        if (lexical_path.extension() != "png")
            continue;
        auto basename = lexical_path.basename();
        if (!basename.starts_with("U+"sv))
            continue;

        basename = basename.substring_view(0, basename.length() - lexical_path.extension().length() - 1);

        StringBuilder builder;
        Vector<u32> code_points;

        basename.for_each_split_view('_', SplitBehavior::Nothing, [&](auto segment) {
            auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segment.substring_view(2));
            VERIFY(code_point.has_value());

            builder.append_code_point(*code_point);
            code_points.append(*code_point);
        });
        auto text = builder.to_string();

        auto emoji = Unicode::find_emoji_for_code_points(code_points);
        if (!emoji.has_value()) {
            emoji = Unicode::Emoji {};
            emoji->group = Unicode::EmojiGroup::Unknown;
            emoji->display_order = NumericLimits<u32>::max();
        }

        auto button = EmojiButton::construct(move(code_points));
        button->set_fixed_size(button_size, button_size);
        button->set_button_style(Gfx::ButtonStyle::Coolbar);
        button->on_click = [this, text](auto) {
            m_selected_emoji_text = move(text);
            done(ExecResult::OK);
        };

        if (!emoji->name.is_empty())
            button->set_tooltip(emoji->name);

        emojis.empend(move(button), emoji.release_value(), move(text));
    }

    quick_sort(emojis, [](auto const& lhs, auto const& rhs) {
        return lhs.emoji.display_order < rhs.emoji.display_order;
    });

    return emojis;
}

void EmojiInputDialog::update_displayed_emoji()
{
    ScopeGuard guard { [&] { m_emojis_widget->set_updates_enabled(true); } };
    m_emojis_widget->set_updates_enabled(false);

    m_emojis_widget->remove_all_children();
    m_first_displayed_emoji = nullptr;

    constexpr size_t columns = 18;
    size_t rows = ceil_div(m_emojis.size(), columns);
    size_t index = 0;

    auto query = m_search_box->text();

    for (size_t row = 0; row < rows && index < m_emojis.size(); ++row) {
        auto& horizontal_container = m_emojis_widget->add<Widget>();
        horizontal_container.set_preferred_height(SpecialDimension::Fit);

        auto& horizontal_layout = horizontal_container.set_layout<HorizontalBoxLayout>();
        horizontal_layout.set_spacing(0);

        for (size_t column = 0; column < columns; ++column) {
            bool found_match = false;

            while (!found_match && (index < m_emojis.size())) {
                auto& emoji = m_emojis[index++];

                if (m_selected_category.has_value() && emoji.emoji.group != m_selected_category)
                    continue;

                if (query.is_empty()) {
                    found_match = true;
                } else if (!emoji.emoji.name.is_empty()) {
                    auto result = fuzzy_match(query, emoji.emoji.name);
                    found_match = result.score > 0;
                }

                if (found_match) {
                    horizontal_container.add_child(*emoji.button);

                    if (m_first_displayed_emoji == nullptr)
                        m_first_displayed_emoji = &emoji;
                }
            }
        }
    }
}

void EmojiInputDialog::select_first_displayed_emoji()
{
    if (m_first_displayed_emoji == nullptr)
        return;

    m_selected_emoji_text = m_first_displayed_emoji->text;
    done(ExecResult::OK);
}

void EmojiInputDialog::event(Core::Event& event)
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
