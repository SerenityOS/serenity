/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/EventLoop.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Toolbar.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Toolbar)

namespace GUI {

Toolbar::Toolbar(Orientation orientation, int button_size)
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

Toolbar::~Toolbar()
{
}

class ToolbarButton final : public Button {
    C_OBJECT(ToolbarButton);

public:
    virtual ~ToolbarButton() override { }

private:
    explicit ToolbarButton(Action& action)
    {
        if (action.group() && action.group()->is_exclusive())
            set_exclusive(true);
        set_action(action);
        set_tooltip(tooltip(action));
        set_focus_policy(FocusPolicy::NoFocus);
        if (action.icon())
            set_icon(action.icon());
        else
            set_text(action.text());
        set_button_style(Gfx::ButtonStyle::Coolbar);
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

    virtual void enter_event(Core::Event& event) override
    {
        auto* app = Application::the();
        if (app && action())
            Core::EventLoop::current().post_event(*app, make<ActionEvent>(ActionEvent::Type::ActionEnter, *action()));
        return Button::enter_event(event);
    }

    virtual void leave_event(Core::Event& event) override
    {
        auto* app = Application::the();
        if (app && action())
            Core::EventLoop::current().post_event(*app, make<ActionEvent>(ActionEvent::Type::ActionLeave, *action()));
        return Button::leave_event(event);
    }
};

ErrorOr<NonnullRefPtr<GUI::Button>> Toolbar::try_add_action(Action& action)
{
    auto item = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Item));
    item->type = Item::Type::Action;
    item->action = action;

    // NOTE: Grow the m_items capacity before potentially adding a child widget.
    //       This avoids having to untangle the child widget in case of allocation failure.
    TRY(m_items.try_ensure_capacity(m_items.size() + 1));

    auto button = TRY(try_add<ToolbarButton>(action));
    button->set_fixed_size(m_button_size + 8, m_button_size + 8);

    m_items.unchecked_append(move(item));
    return button;
}

GUI::Button& Toolbar::add_action(Action& action)
{
    auto button = MUST(try_add_action(action));
    return *button;
}

ErrorOr<void> Toolbar::try_add_separator()
{
    // NOTE: Grow the m_items capacity before potentially adding a child widget.
    TRY(m_items.try_ensure_capacity(m_items.size() + 1));

    auto item = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Item));
    item->type = Item::Type::Separator;
    (void)TRY(try_add<SeparatorWidget>(m_orientation == Gfx::Orientation::Horizontal ? Gfx::Orientation::Vertical : Gfx::Orientation::Horizontal));
    m_items.unchecked_append(move(item));
    return {};
}

void Toolbar::add_separator()
{
    MUST(try_add_separator());
}

void Toolbar::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), palette().button());
}

}
