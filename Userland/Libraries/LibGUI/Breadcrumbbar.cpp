/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Breadcrumbbar)

namespace GUI {

class BreadcrumbButton : public Button {
    C_OBJECT(BreadcrumbButton);

public:
    virtual ~BreadcrumbButton() override = default;

    virtual bool is_uncheckable() const override { return false; }
    virtual void drop_event(DropEvent& event) override
    {
        if (on_drop)
            on_drop(event);
    }

    virtual void drag_enter_event(DragEvent& event) override
    {
        update();
        if (on_drag_enter)
            on_drag_enter(event);
    }

    virtual void drag_leave_event(Event&) override
    {
        update();
    }

    virtual void paint_event(PaintEvent& event) override
    {
        Button::paint_event(event);
        if (has_pending_drop()) {
            Painter painter(*this);
            painter.draw_rect(rect(), palette().selection(), true);
        }
    }

    Function<void(DropEvent&)> on_drop;
    Function<void(DragEvent&)> on_drag_enter;

private:
    BreadcrumbButton() = default;
};

Breadcrumbbar::Breadcrumbbar()
{
    set_layout<HorizontalBoxLayout>(GUI::Margins {}, 0);
}

void Breadcrumbbar::clear_segments()
{
    m_segments.clear();
    remove_all_children();
    m_selected_segment = {};
}

void Breadcrumbbar::append_segment(ByteString text, Gfx::Bitmap const* icon, ByteString data, String tooltip)
{
    auto& button = add<BreadcrumbButton>();
    button.set_button_style(Gfx::ButtonStyle::Coolbar);
    button.set_text(String::from_byte_string(text).release_value_but_fixme_should_propagate_errors());
    button.set_icon(icon);
    button.set_tooltip(move(tooltip));
    button.set_focus_policy(FocusPolicy::TabFocus);
    button.set_checkable(true);
    button.set_exclusive(true);
    button.on_click = [this, index = m_segments.size()](auto) {
        if (on_segment_click)
            on_segment_click(index);
        if (on_segment_change && m_selected_segment != index)
            on_segment_change(index);
    };
    button.on_double_click = [this](auto modifiers) {
        if (on_doubleclick)
            on_doubleclick(modifiers);
    };
    button.on_focus_change = [this, index = m_segments.size()](auto has_focus, auto) {
        if (has_focus && on_segment_change && m_selected_segment != index)
            on_segment_change(index);
    };
    button.on_drop = [this, index = m_segments.size()](auto& drop_event) {
        if (on_segment_drop)
            on_segment_drop(index, drop_event);
    };
    button.on_drag_enter = [this, index = m_segments.size()](auto& event) {
        if (on_segment_drag_enter)
            on_segment_drag_enter(index, event);
    };

    m_segments.append(Segment {
        .icon = icon,
        .text = move(text),
        .data = move(data),
        .width = 0,
        .shrunken_width = 0,
        .button = button.make_weak_ptr<GUI::Button>(),
    });
    relayout();
}

void Breadcrumbbar::remove_end_segments(size_t start_segment_index)
{
    while (segment_count() > start_segment_index) {
        auto segment = m_segments.take_last();
        remove_child(*segment.button);
    }
    if (m_selected_segment.has_value() && *m_selected_segment >= start_segment_index)
        m_selected_segment = {};
}

Optional<size_t> Breadcrumbbar::find_segment_with_data(ByteString const& data)
{
    for (size_t i = 0; i < segment_count(); ++i) {
        if (segment_data(i) == data)
            return i;
    }
    return {};
}

void Breadcrumbbar::set_selected_segment(Optional<size_t> index)
{
    if (m_selected_segment == index)
        return;
    m_selected_segment = index;

    if (!index.has_value()) {
        for_each_child_of_type<GUI::AbstractButton>([&](auto& button) {
            button.set_checked(false);
            return IterationDecision::Continue;
        });
        return;
    }

    auto& segment = m_segments[index.value()];
    VERIFY(segment.button);
    segment.button->set_checked(true);
    if (on_segment_change)
        on_segment_change(index);
    relayout();
}

void Breadcrumbbar::doubleclick_event(MouseEvent& event)
{
    if (on_doubleclick)
        on_doubleclick(event.modifiers());
}

void Breadcrumbbar::resize_event(ResizeEvent&)
{
    relayout();
}

void Breadcrumbbar::did_change_font()
{
    Widget::did_change_font();
    relayout();
}

void Breadcrumbbar::relayout()
{
    auto total_width = 0;
    for (auto& segment : m_segments) {
        VERIFY(segment.button);
        auto& button = *segment.button;
        // NOTE: We use our own font instead of the button's font here in case we're being notified about
        //       a system font change, and the button hasn't been notified yet.
        auto button_text_width = font().width(segment.text);
        auto icon_width = button.icon() ? button.icon()->width() : 0;
        auto icon_padding = button.icon() ? 4 : 0;

        int const max_button_width = 100;

        segment.width = static_cast<int>(ceilf(min(button_text_width + icon_width + icon_padding + 16, max_button_width)));
        segment.shrunken_width = icon_width + icon_padding + (button.icon() ? 4 : 16);

        button.set_max_size(segment.width, 16 + 8);
        button.set_min_size(segment.shrunken_width, 16 + 8);

        total_width += segment.width;
    }

    auto remaining_width = total_width;

    for (auto& segment : m_segments) {
        if (remaining_width > width() && !segment.button->is_checked()) {
            segment.button->set_preferred_width(segment.shrunken_width);
            remaining_width -= (segment.width - segment.shrunken_width);
            continue;
        }
        segment.button->set_preferred_width(segment.width);
    }
}

}
