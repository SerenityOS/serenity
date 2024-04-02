/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FuzzyMatch.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/EmojiInputDialogWidget.h>
#include <LibGUI/Frame.h>
#include <LibGUI/ScrollableContainerWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Toolbar.h>

namespace GUI {

struct EmojiCategory {
    Unicode::EmojiGroup group;
    StringView representative_emoji;
};

static constexpr auto s_emoji_groups = Array {
    EmojiCategory { Unicode::EmojiGroup::SmileysAndEmotion, "😀"sv },
    EmojiCategory { Unicode::EmojiGroup::PeopleAndBody, "🫳"sv },
    EmojiCategory { Unicode::EmojiGroup::AnimalsAndNature, "🌻"sv },
    EmojiCategory { Unicode::EmojiGroup::FoodAndDrink, "🍕"sv },
    EmojiCategory { Unicode::EmojiGroup::TravelAndPlaces, "🏖"sv },
    EmojiCategory { Unicode::EmojiGroup::Activities, "🎳"sv },
    EmojiCategory { Unicode::EmojiGroup::Objects, "📦"sv },
    EmojiCategory { Unicode::EmojiGroup::Symbols, "❤️"sv },
    EmojiCategory { Unicode::EmojiGroup::Flags, "🚩"sv },
    EmojiCategory { Unicode::EmojiGroup::SerenityOS, "\U0010CD0B"sv },
};

EmojiInputDialog::EmojiInputDialog(Window* parent_window)
    : Dialog(parent_window)
    , m_category_action_group(make<ActionGroup>())
{
    auto main_widget = EmojiInputDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(main_widget);

    set_window_type(GUI::WindowType::Popup);
    set_window_mode(GUI::WindowMode::Modeless);
    set_blocks_emoji_input(true);
    resize(410, 300);

    auto& scrollable_container = *main_widget->find_descendant_of_type_named<GUI::ScrollableContainerWidget>("scrollable_container"sv);
    m_search_box = main_widget->find_descendant_of_type_named<GUI::TextBox>("search_box"sv);
    m_toolbar = main_widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar"sv);
    m_emojis_widget = scrollable_container.widget();
    m_emojis = supported_emoji();

    m_category_action_group->set_exclusive(true);
    m_category_action_group->set_unchecking_allowed(true);

    for (auto const& category : s_emoji_groups) {
        auto name = Unicode::emoji_group_to_string(category.group);
        ByteString tooltip = name;

        auto set_filter_action = Action::create_checkable(
            category.representative_emoji,
            [this, group = category.group](auto& action) {
                if (action.is_checked())
                    m_selected_category = group;
                else
                    m_selected_category = {};

                m_search_box->set_text({}, AllowCallback::No);
                update_displayed_emoji();
            },
            this);
        set_filter_action->set_tooltip(move(tooltip));

        m_category_action_group->add_action(*set_filter_action);
        m_toolbar->add_action(*set_filter_action);
    }

    scrollable_container.horizontal_scrollbar().set_visible(false);
    update_displayed_emoji();

    m_search_box->on_change = [this]() {
        update_displayed_emoji();
    };

    m_search_box->on_return_pressed = [this]() {
        select_first_displayed_emoji();
    };
}

auto EmojiInputDialog::supported_emoji() -> Vector<Emoji>
{
    static constexpr int button_size = 22;

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
        auto text = builder.to_byte_string();

        auto emoji = Unicode::find_emoji_for_code_points(code_points);
        if (!emoji.has_value()) {
            emoji = Unicode::Emoji {};
            emoji->group = Unicode::EmojiGroup::Unknown;
            emoji->display_order = NumericLimits<u32>::max();
        }

        auto button = Button::construct(String::from_byte_string(text).release_value_but_fixme_should_propagate_errors());
        button->set_fixed_size(button_size, button_size);
        button->set_button_style(Gfx::ButtonStyle::Coolbar);
        button->on_click = [this, text](auto) mutable {
            m_selected_emoji_text = move(text);
            done(ExecResult::OK);
        };

        if (!emoji->name.is_empty())
            button->set_tooltip(MUST(String::from_utf8(emoji->name)));

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

    static constexpr size_t columns = 17;
    size_t rows = ceil_div(m_emojis.size(), columns);
    size_t index = 0;

    auto query = m_search_box->text();

    for (size_t row = 0; row < rows && index < m_emojis.size(); ++row) {
        auto& horizontal_container = m_emojis_widget->add<Widget>();
        horizontal_container.set_preferred_height(SpecialDimension::Fit);
        horizontal_container.set_layout<HorizontalBoxLayout>(GUI::Margins {}, 0);

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

}
