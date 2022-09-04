/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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
    REGISTER_BOOL_PROPERTY("collapsible", is_collapsible, set_collapsible);

    if (m_orientation == Orientation::Horizontal)
        set_fixed_height(button_size);
    else
        set_fixed_width(button_size);

    set_layout<BoxLayout>(orientation);
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });
}

class ToolbarButton final : public Button {
    C_OBJECT(ToolbarButton);

public:
    virtual ~ToolbarButton() override = default;

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
    String tooltip(Action const& action) const
    {
        StringBuilder builder;
        builder.append(action.text());
        if (action.shortcut().is_valid()) {
            builder.append(" ("sv);
            builder.append(action.shortcut().to_string());
            builder.append(')');
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

    item->widget = TRY(try_add<ToolbarButton>(action));
    item->widget->set_fixed_size(m_button_size, m_button_size);

    m_items.unchecked_append(move(item));
    return *static_cast<Button*>(m_items.last().widget.ptr());
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
    item->widget = TRY(try_add<SeparatorWidget>(m_orientation == Gfx::Orientation::Horizontal ? Gfx::Orientation::Vertical : Gfx::Orientation::Horizontal));
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

Optional<UISize> Toolbar::calculated_preferred_size() const
{
    if (m_orientation == Gfx::Orientation::Horizontal)
        return { { SpecialDimension::Grow, SpecialDimension::Fit } };
    else
        return { { SpecialDimension::Fit, SpecialDimension::Grow } };
    VERIFY_NOT_REACHED();
}

Optional<UISize> Toolbar::calculated_min_size() const
{
    if (m_collapsible) {
        if (m_orientation == Gfx::Orientation::Horizontal)
            return UISize(m_button_size, SpecialDimension::Shrink);
        else
            return UISize(SpecialDimension::Shrink, m_button_size);
    }
    VERIFY(layout());
    return { layout()->min_size() };
}

ErrorOr<void> Toolbar::create_overflow_objects()
{
    m_overflow_action = Action::create("Overflow Menu", { Mod_Ctrl | Mod_Shift, Key_O }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/overflow-menu.png"sv)), [&](auto&) {
        m_overflow_menu->popup(m_overflow_button->screen_relative_rect().bottom_left(), {}, m_overflow_button->rect());
    });
    m_overflow_action->set_status_tip("Show hidden toolbar actions");
    m_overflow_action->set_enabled(false);

    TRY(layout()->try_add_spacer());

    m_overflow_button = TRY(try_add_action(*m_overflow_action));
    m_overflow_button->set_visible(false);

    return {};
}

ErrorOr<void> Toolbar::update_overflow_menu()
{
    if (!m_collapsible)
        return {};

    Optional<size_t> marginal_index {};
    auto position { 0 };
    auto is_horizontal { m_orientation == Gfx::Orientation::Horizontal };
    auto margin { is_horizontal ? layout()->margins().right() : layout()->margins().bottom() };
    auto toolbar_size { is_horizontal ? width() : height() };

    for (size_t i = 0; i < m_items.size() - 1; ++i) {
        auto& item = m_items.at(i);
        auto item_size = is_horizontal ? item.widget->width() : item.widget->height();
        if (position + item_size + m_button_size + margin > toolbar_size) {
            marginal_index = i;
            break;
        }
        item.widget->set_visible(true);
        position += item_size;
    }

    if (!marginal_index.has_value()) {
        if (m_overflow_action) {
            m_overflow_action->set_enabled(false);
            m_overflow_button->set_visible(false);
        }
        return {};
    }

    if (!m_overflow_action)
        TRY(create_overflow_objects());
    m_overflow_action->set_enabled(true);
    m_overflow_button->set_visible(true);

    m_overflow_menu = TRY(Menu::try_create());
    m_overflow_button->set_menu(m_overflow_menu);

    for (size_t i = marginal_index.value(); i < m_items.size(); ++i) {
        auto& item = m_items.at(i);
        Item* peek_item;
        if (i > 0) {
            peek_item = &m_items.at(i - 1);
            if (peek_item->type == Item::Type::Separator)
                peek_item->widget->set_visible(false);
        }
        if (i < m_items.size() - 1) {
            item.widget->set_visible(false);
            peek_item = &m_items.at(i + 1);
            if (item.action)
                TRY(m_overflow_menu->try_add_action(*item.action));
        }
        if (item.action && peek_item->type == Item::Type::Separator)
            TRY(m_overflow_menu->try_add_separator());
    }

    return {};
}

void Toolbar::resize_event(GUI::ResizeEvent& event)
{
    Widget::resize_event(event);
    if (auto result = update_overflow_menu(); result.is_error())
        warnln("Failed to update overflow menu");
}

}
