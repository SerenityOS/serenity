/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/ToolBar.h>
#include <LibGfx/Palette.h>

namespace GUI {

ToolBar::ToolBar(Orientation orientation, int button_size)
    : m_orientation(orientation)
    , m_button_size(button_size)
{
    if (m_orientation == Orientation::Horizontal) {
        set_fixed_height(button_size + 8);
    } else {
        set_fixed_width(button_size + 8);
    }
    set_layout<BoxLayout>(orientation);
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });
}

ToolBar::~ToolBar()
{
}

class ToolBarButton final : public Button {
    C_OBJECT(ToolBarButton);

public:
    virtual ~ToolBarButton() override { }

private:
    explicit ToolBarButton(Action& action)
    {
        if (action.group() && action.group()->is_exclusive())
            set_exclusive(true);
        set_action(action);
        set_tooltip(tooltip(action));
        set_focus_policy(FocusPolicy::TabFocus);
        if (action.icon())
            set_icon(action.icon());
        else
            set_text(action.text());
        set_button_style(Gfx::ButtonStyle::CoolBar);
    }
    String tooltip(const Action& action) const
    {
        StringBuilder builder;
        builder.append(action.text());
        if (action.shortcut().is_valid()) {
            builder.append(" (");
            builder.append(action.shortcut().to_string());
            builder.append(")");
        }
        return builder.to_string();
    }
};

void ToolBar::add_action(Action& action)
{
    auto item = make<Item>();
    item->type = Item::Type::Action;
    item->action = action;

    auto& button = add<ToolBarButton>(action);
    button.set_fixed_size(m_button_size + 8, m_button_size + 8);

    m_items.append(move(item));
}

void ToolBar::add_separator()
{
    auto item = make<Item>();
    item->type = Item::Type::Separator;
    add<SeparatorWidget>(m_orientation == Gfx::Orientation::Horizontal ? Gfx::Orientation::Vertical : Gfx::Orientation::Horizontal);
    m_items.append(move(item));
}

void ToolBar::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), palette().button());
}

}
