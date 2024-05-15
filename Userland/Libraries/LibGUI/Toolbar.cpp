/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
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
    REGISTER_BOOL_PROPERTY("grouped", is_grouped, set_grouped);

    if (m_orientation == Orientation::Horizontal)
        set_fixed_height(button_size);
    else
        set_fixed_width(button_size);

    set_layout<BoxLayout>(orientation, GUI::Margins { 2, 2, 2, 2 }, 0);
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
            set_text(String::from_byte_string(action.text()).release_value_but_fixme_should_propagate_errors());
        set_button_style(Gfx::ButtonStyle::Coolbar);
    }

    virtual void set_text(String text) override
    {
        auto const* action = this->action();
        VERIFY(action);

        set_tooltip(tooltip(*action));
        if (!action->icon())
            Button::set_text(move(text));
    }

    String tooltip(Action const& action) const
    {
        StringBuilder builder;
        builder.append(action.tooltip());
        if (action.shortcut().is_valid()) {
            builder.append(" ("sv);
            builder.append(action.shortcut().to_byte_string());
            builder.append(')');
        }
        return MUST(builder.to_string());
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

Button& Toolbar::add_action(Action& action)
{
    auto item = make<Item>();
    item->type = Item::Type::Action;
    item->action = action;

    item->widget = add<ToolbarButton>(action);
    item->widget->set_fixed_size(m_button_size, m_button_size);

    m_items.append(move(item));
    return *static_cast<Button*>(m_items.last()->widget.ptr());
}

void Toolbar::add_separator()
{
    auto item = make<Item>();
    item->type = Item::Type::Separator;
    item->widget = add<SeparatorWidget>(m_orientation == Gfx::Orientation::Horizontal ? Gfx::Orientation::Vertical : Gfx::Orientation::Horizontal);
    m_items.append(move(item));
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
    m_overflow_action = Action::create("Overflow Menu", { Mod_Ctrl | Mod_Shift, Key_O }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/overflow-menu.png"sv)), [&](auto&) {
        m_overflow_menu->popup(m_overflow_button->screen_relative_rect().bottom_left().moved_up(1), {}, m_overflow_button->rect());
    });
    m_overflow_action->set_status_tip("Show hidden toolbar actions"_string);
    m_overflow_action->set_enabled(false);

    add_spacer();

    m_overflow_button = add_action(*m_overflow_action);
    m_overflow_button->set_visible(false);

    return {};
}

ErrorOr<void> Toolbar::update_overflow_menu()
{
    if (!m_collapsible || m_items.is_empty())
        return {};

    Optional<size_t> marginal_index {};
    auto position { 0 };
    auto is_horizontal { m_orientation == Gfx::Orientation::Horizontal };
    auto margin { is_horizontal ? layout()->margins().horizontal_total() : layout()->margins().vertical_total() };
    auto spacing { layout()->spacing() };
    auto toolbar_size { is_horizontal ? width() : height() };

    for (size_t i = 0; i < m_items.size() - 1; ++i) {
        auto& item = m_items.at(i);
        auto item_size = is_horizontal ? item->widget->width() : item->widget->height();
        if (position + item_size + margin > toolbar_size) {
            marginal_index = i;
            break;
        }
        item->widget->set_visible(true);
        position += item_size + spacing;
    }

    if (!marginal_index.has_value()) {
        if (m_overflow_action) {
            m_overflow_action->set_enabled(false);
            m_overflow_button->set_visible(false);
        }
        return {};
    }

    if (marginal_index.value() > 0) {
        for (size_t i = marginal_index.value() - 1; i > 0; --i) {
            auto& item = m_items.at(i);
            auto item_size = is_horizontal ? item->widget->width() : item->widget->height();
            if (position + m_button_size + spacing + margin <= toolbar_size)
                break;
            item->widget->set_visible(false);
            position -= item_size + spacing;
            marginal_index = i;
        }
    }

    if (m_grouped) {
        for (size_t i = marginal_index.value(); i > 0; --i) {
            auto& item = m_items.at(i);
            if (item->type == Item::Type::Separator)
                break;
            item->widget->set_visible(false);
            marginal_index = i;
        }
    }

    if (!m_overflow_action)
        TRY(create_overflow_objects());
    m_overflow_action->set_enabled(true);
    m_overflow_button->set_visible(true);

    m_overflow_menu = Menu::construct();
    m_overflow_button->set_menu(m_overflow_menu);

    for (size_t i = marginal_index.value(); i < m_items.size(); ++i) {
        auto& item = m_items.at(i);
        Item* peek_item;
        if (i > 0) {
            peek_item = m_items[i - 1];
            if (peek_item->type == Item::Type::Separator)
                peek_item->widget->set_visible(false);
        }
        if (i < m_items.size() - 1) {
            item->widget->set_visible(false);
            peek_item = m_items[i + 1];
            if (item->action)
                m_overflow_menu->add_action(*item->action);
        }
        if (item->action && peek_item->type == Item::Type::Separator)
            m_overflow_menu->add_separator();
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
