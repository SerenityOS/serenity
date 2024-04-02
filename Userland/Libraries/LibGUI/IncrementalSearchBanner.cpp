/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/IncrementalSearchBanner.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>

namespace GUI {

ErrorOr<NonnullRefPtr<IncrementalSearchBanner>> IncrementalSearchBanner::try_create(TextEditor& editor)
{
    auto widget = TRY(IncrementalSearchBanner::try_create());
    widget->m_editor = editor;
    return widget;
}

ErrorOr<void> IncrementalSearchBanner::initialize()
{
    m_index_label = find_descendant_of_type_named<Label>("incremental_search_banner_index_label");

    m_wrap_search_button = find_descendant_of_type_named<Button>("incremental_search_banner_wrap_search_button");
    m_wrap_search_button->on_checked = [this](auto is_checked) {
        m_wrap_search = is_checked
            ? TextDocument::SearchShouldWrap::Yes
            : TextDocument::SearchShouldWrap::No;
    };

    m_match_case_button = find_descendant_of_type_named<Button>("incremental_search_banner_match_case_button");
    m_match_case_button->on_checked = [this](auto is_checked) {
        m_match_case = is_checked;
        m_editor->reset_search_results();
        search(TextEditor::SearchDirection::Forward);
    };

    m_close_button = find_descendant_of_type_named<Button>("incremental_search_banner_close_button");
    m_close_button->set_text("\xE2\x9D\x8C"_string);
    m_close_button->on_click = [this](auto) {
        hide();
    };

    m_next_button = find_descendant_of_type_named<Button>("incremental_search_banner_next_button");
    m_next_button->on_click = [this](auto) {
        search(TextEditor::SearchDirection::Forward);
    };

    m_previous_button = find_descendant_of_type_named<Button>("incremental_search_banner_previous_button");
    m_previous_button->on_click = [this](auto) {
        search(TextEditor::SearchDirection::Backward);
    };

    m_search_textbox = find_descendant_of_type_named<TextBox>("incremental_search_banner_search_textbox");
    m_search_textbox->on_change = [this]() {
        m_editor->reset_search_results();
        search(TextEditor::SearchDirection::Forward);
    };

    m_search_textbox->on_return_pressed = [this]() {
        search(TextEditor::SearchDirection::Forward);
    };

    m_search_textbox->on_shift_return_pressed = [this]() {
        search(TextEditor::SearchDirection::Backward);
    };

    m_search_textbox->on_escape_pressed = [this]() {
        hide();
    };
    return {};
}

void IncrementalSearchBanner::show()
{
    set_visible(true);
    m_editor->do_layout();
    m_editor->update_scrollbar_ranges();
    m_search_textbox->set_focus(true);
}

void IncrementalSearchBanner::hide()
{
    set_visible(false);
    m_editor->do_layout();
    m_editor->update_scrollbar_ranges();
    m_editor->reset_search_results();
    m_editor->set_focus(true);
}

void IncrementalSearchBanner::search(TextEditor::SearchDirection direction)
{
    auto needle = m_search_textbox->text();
    if (needle.is_empty()) {
        m_editor->reset_search_results();
        m_index_label->set_text({});
        return;
    }

    auto index = m_editor->search_result_index().value_or(0) + 1;
    if (m_wrap_search == TextDocument::SearchShouldWrap::No) {
        auto forward = direction == TextEditor::SearchDirection::Forward;
        if ((index == m_editor->search_results().size() && forward) || (index == 1 && !forward))
            return;
    }

    auto result = m_editor->find_text(needle, direction, m_wrap_search, false, m_match_case);
    index = m_editor->search_result_index().value_or(0) + 1;
    if (result.is_valid())
        m_index_label->set_text(String::formatted("{} of {}", index, m_editor->search_results().size()).release_value_but_fixme_should_propagate_errors());
    else
        m_index_label->set_text({});
}

void IncrementalSearchBanner::paint_event(PaintEvent& event)
{
    Widget::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.draw_line({ 0, rect().bottom() - 2 }, { width(), rect().bottom() - 2 }, palette().threed_shadow1());
    painter.draw_line({ 0, rect().bottom() - 1 }, { width(), rect().bottom() - 1 }, palette().threed_shadow2());
}

Optional<UISize> IncrementalSearchBanner::calculated_min_size() const
{
    auto textbox_width = m_search_textbox->effective_min_size().width().as_int();
    auto textbox_height = m_search_textbox->effective_min_size().height().as_int();
    auto button_width = m_next_button->effective_min_size().width().as_int();
    VERIFY(layout());
    auto margins = layout()->margins();
    auto spacing = layout()->spacing();
    return { { margins.left() + textbox_width + spacing + button_width * 2 + margins.right(), textbox_height + margins.top() + margins.bottom() } };
}

}
