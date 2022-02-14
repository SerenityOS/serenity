/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Breadcrumbbar)

namespace GUI {

class BreadcrumbButton : public Button {
    C_OBJECT(BreadcrumbButton);

public:
    virtual ~BreadcrumbButton() override { }

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
    BreadcrumbButton() { }
};

Breadcrumbbar::Breadcrumbbar()
{
    auto& layout = set_layout<HorizontalBoxLayout>();
    layout.set_spacing(0);
}

Breadcrumbbar::~Breadcrumbbar()
{
}

void Breadcrumbbar::clear_segments()
{
    m_segments.clear();
    remove_all_children();
}

void Breadcrumbbar::append_segment(String text, Gfx::Bitmap const* icon, String data, String tooltip)
{
    auto& button = add<BreadcrumbButton>();
    button.set_button_style(Gfx::ButtonStyle::Coolbar);
    button.set_text(text);
    button.set_icon(icon);
    button.set_tooltip(move(tooltip));
    button.set_focus_policy(FocusPolicy::TabFocus);
    button.set_checkable(true);
    button.set_exclusive(true);
    button.on_click = [this, index = m_segments.size()](auto) {
        if (on_segment_click)
            on_segment_click(index);
    };
    button.on_focus_change = [this, index = m_segments.size()](auto has_focus, auto) {
        if (has_focus && on_segment_click)
            on_segment_click(index);
    };
    button.on_drop = [this, index = m_segments.size()](auto& drop_event) {
        if (on_segment_drop)
            on_segment_drop(index, drop_event);
    };
    button.on_drag_enter = [this, index = m_segments.size()](auto& event) {
        if (on_segment_drag_enter)
            on_segment_drag_enter(index, event);
    };

    auto button_text_width = button.font().width(text);
    auto icon_width = icon ? icon->width() : 0;
    auto icon_padding = icon ? 4 : 0;

    const int max_button_width = 100;

    auto button_width = min(button_text_width + icon_width + icon_padding + 16, max_button_width);
    auto shrunken_width = icon_width + icon_padding + (icon ? 4 : 16);

    button.set_fixed_size(button_width, 16 + 8);

    Segment segment { icon, text, data, button_width, shrunken_width, button.make_weak_ptr<GUI::Button>() };

    m_segments.append(move(segment));
    relayout();
}

void Breadcrumbbar::remove_end_segments(size_t start_segment_index)
{
    while (segment_count() > start_segment_index) {
        auto segment = m_segments.take_last();
        remove_child(*segment.button);
    }
}

Optional<size_t> Breadcrumbbar::find_segment_with_data(String const& data)
{
    for (size_t i = 0; i < segment_count(); ++i) {
        if (segment_data(i) == data)
            return i;
    }
    return {};
}

void Breadcrumbbar::set_selected_segment(Optional<size_t> index)
{
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
    relayout();
}

void Breadcrumbbar::doubleclick_event(MouseEvent& event)
{
    if (on_doubleclick)
        on_doubleclick(event);
}

void Breadcrumbbar::resize_event(ResizeEvent&)
{
    relayout();
}

void Breadcrumbbar::relayout()
{
    auto remaining_width = 0;

    for (auto& segment : m_segments)
        remaining_width += segment.width;

    for (auto& segment : m_segments) {
        if (remaining_width > width() && !segment.button->is_checked()) {
            segment.button->set_fixed_width(segment.shrunken_width);
            remaining_width -= (segment.width - segment.shrunken_width);
            continue;
        }
        segment.button->set_fixed_width(segment.width);
    }
}

}
