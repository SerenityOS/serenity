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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/BreadcrumbBar.h>
#include <LibGUI/Button.h>
#include <LibGfx/Font.h>

namespace GUI {

// FIXME: Move this somewhere else
class UnuncheckableButton : public GUI::Button {
    C_OBJECT(UnuncheckableButton);

public:
    virtual ~UnuncheckableButton() override { }

    virtual bool is_uncheckable() const override { return false; }

private:
    UnuncheckableButton() { }
};

BreadcrumbBar::BreadcrumbBar()
{
    auto& layout = set_layout<HorizontalBoxLayout>();
    layout.set_spacing(0);
}

BreadcrumbBar::~BreadcrumbBar()
{
}

void BreadcrumbBar::clear_segments()
{
    m_segments.clear();

    // FIXME: Should Core::Object have something like "remove_all_children()" perhaps?
    auto children = this->children();
    for (auto& child : children)
        child.remove_from_parent();
}

void BreadcrumbBar::append_segment(const String& text, const Gfx::Bitmap* icon, const String& data)
{
    auto& button = add<UnuncheckableButton>();
    button.set_button_style(Gfx::ButtonStyle::CoolBar);
    button.set_text(text);
    button.set_icon(icon);
    button.set_focus_policy(FocusPolicy::TabFocus);
    button.set_checkable(true);
    button.set_exclusive(true);
    button.on_click = [this, index = m_segments.size()](auto) {
        if (on_segment_click)
            on_segment_click(index);
    };

    button.set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);

    auto button_text_width = button.font().width(text);
    auto icon_width = icon ? icon->width() : 0;
    auto icon_padding = icon ? 4 : 0;
    button.set_preferred_size(button_text_width + icon_width + icon_padding + 16, 16 + 8);

    Segment segment { icon, text, data, button.make_weak_ptr<GUI::Button>() };

    m_segments.append(move(segment));
}

void BreadcrumbBar::set_selected_segment(Optional<size_t> index)
{
    if (!index.has_value()) {
        for_each_child_of_type<GUI::AbstractButton>([&](auto& button) {
            button.set_checked(false);
            return IterationDecision::Continue;
        });
        return;
    }

    auto& segment = m_segments[index.value()];
    ASSERT(segment.button);
    segment.button->set_checked(true);
}

}
