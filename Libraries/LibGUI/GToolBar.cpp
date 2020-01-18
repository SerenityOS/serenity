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

#include <LibDraw/Palette.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GToolBar.h>

GToolBar::GToolBar(GWidget* parent)
    : GToolBar(Orientation::Horizontal, 16, parent)
{
}

GToolBar::GToolBar(Orientation orientation, int button_size, GWidget* parent)
    : GWidget(parent)
    , m_button_size(button_size)
{
    if (orientation == Orientation::Horizontal) {
        set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        set_preferred_size(0, button_size + 12);
    } else {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        set_preferred_size(button_size + 12, 0);
    }
    set_layout(make<GBoxLayout>(orientation));
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });
}

GToolBar::~GToolBar()
{
}

void GToolBar::add_action(GAction& action)
{
    auto item = make<Item>();
    item->type = Item::Action;
    item->action = action;

    auto button = GButton::construct(this);
    if (action.group() && action.group()->is_exclusive())
        button->set_exclusive(true);
    button->set_action(*item->action);
    button->set_tooltip(item->action->text());
    if (item->action->icon())
        button->set_icon(item->action->icon());
    else
        button->set_text(item->action->text());

    button->set_button_style(ButtonStyle::CoolBar);
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    ASSERT(button->size_policy(Orientation::Horizontal) == SizePolicy::Fixed);
    ASSERT(button->size_policy(Orientation::Vertical) == SizePolicy::Fixed);
    button->set_preferred_size(m_button_size + 8, m_button_size + 8);

    m_items.append(move(item));
}

class SeparatorWidget final : public GWidget {
    C_OBJECT(SeparatorWidget)
public:
    SeparatorWidget(GWidget* parent)
        : GWidget(parent)
    {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        set_preferred_size(8, 22);
    }
    virtual ~SeparatorWidget() override {}

    virtual void paint_event(GPaintEvent& event) override
    {
        GPainter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.translate(rect().center().x() - 1, 0);
        painter.draw_line({ 0, 0 }, { 0, rect().bottom() }, palette().threed_shadow1());
        painter.draw_line({ 1, 0 }, { 1, rect().bottom() }, palette().threed_highlight());
    }
};

void GToolBar::add_separator()
{
    auto item = make<Item>();
    item->type = Item::Separator;
    new SeparatorWidget(this);
    m_items.append(move(item));
}

void GToolBar::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_has_frame)
        StylePainter::paint_surface(painter, rect(), palette(), x() != 0, y() != 0);
    else
        painter.fill_rect(event.rect(), palette().button());
}
