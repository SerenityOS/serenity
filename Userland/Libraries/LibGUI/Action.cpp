/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>

namespace GUI {

NonnullRefPtr<Action> Action::create(ByteString text, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(ByteString text, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(ByteString text, Shortcut const& shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, move(callback), parent));
}

NonnullRefPtr<Action> Action::create(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, alternate_shortcut, move(callback), parent));
}

NonnullRefPtr<Action> Action::create(ByteString text, Shortcut const& shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, Shortcut {}, move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, alternate_shortcut, move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create_checkable(ByteString text, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(ByteString text, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), move(icon), move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(ByteString text, Shortcut const& shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(ByteString text, Shortcut const& shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, Shortcut {}, move(icon), move(callback), parent, true));
}

RefPtr<Action> Action::find_action_for_shortcut(Core::EventReceiver& object, Shortcut const& shortcut)
{
    RefPtr<Action> found_action = nullptr;
    object.for_each_child_of_type<Action>([&](auto& action) {
        if (action.shortcut() == shortcut || action.alternate_shortcut() == shortcut) {
            found_action = &action;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_action;
}

Action::Action(ByteString text, Function<void(Action&)> on_activation_callback, Core::EventReceiver* parent, bool checkable)
    : Action(move(text), Shortcut {}, Shortcut {}, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(ByteString text, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> on_activation_callback, Core::EventReceiver* parent, bool checkable)
    : Action(move(text), Shortcut {}, Shortcut {}, move(icon), move(on_activation_callback), parent, checkable)
{
}

Action::Action(ByteString text, Shortcut const& shortcut, Function<void(Action&)> on_activation_callback, Core::EventReceiver* parent, bool checkable)
    : Action(move(text), shortcut, Shortcut {}, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, Function<void(Action&)> on_activation_callback, Core::EventReceiver* parent, bool checkable)
    : Action(move(text), shortcut, alternate_shortcut, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> on_activation_callback, Core::EventReceiver* parent, bool checkable)
    : Core::EventReceiver(parent)
    , on_activation(move(on_activation_callback))
    , m_text(move(text))
    , m_icon(move(icon))
    , m_shortcut(shortcut)
    , m_alternate_shortcut(alternate_shortcut)
    , m_checkable(checkable)
{
    if (parent && is<Widget>(*parent)) {
        m_scope = ShortcutScope::WidgetLocal;
    } else if (parent && is<Window>(*parent)) {
        m_scope = ShortcutScope::WindowLocal;
    } else {
        m_scope = ShortcutScope::ApplicationGlobal;
        if (auto* app = Application::the()) {
            app->register_global_shortcut_action({}, *this);
        }
    }
}

Action::~Action()
{
    if (m_scope == ShortcutScope::ApplicationGlobal) {
        if (auto* app = Application::the())
            app->unregister_global_shortcut_action({}, *this);
    }
}

void Action::process_event(Window& window, Event& event)
{
    if (is_enabled() && is_visible() && !is_activating()) {
        flash_menubar_menu(window);
        activate();
        event.accept();
        return;
    }
    if (swallow_key_event_when_disabled()) {
        event.accept();
        return;
    }

    event.ignore();
}

void Action::activate(Core::EventReceiver* activator)
{
    if (is_activating())
        return;
    TemporaryChange change { m_activating, true };

    if (!on_activation)
        return;

    if (activator)
        m_activator = activator->make_weak_ptr();

    if (is_checkable()) {
        if (m_action_group) {
            if (m_action_group->is_unchecking_allowed())
                set_checked(!is_checked());
            else
                set_checked(true);
        } else {
            set_checked(!is_checked());
        }
    }

    if (activator == nullptr) {
        for_each_toolbar_button([](auto& button) {
            button.mimic_pressed();
        });
    }

    on_activation(*this);
    m_activator = nullptr;
}

void Action::flash_menubar_menu(GUI::Window& window)
{
    for (auto& menu_item : m_menu_items)
        window.flash_menubar_menu_for(*menu_item);
}

void Action::register_button(Badge<Button>, Button& button)
{
    m_buttons.set(&button);
}

void Action::unregister_button(Badge<Button>, Button& button)
{
    m_buttons.remove(&button);
}

void Action::register_menu_item(Badge<MenuItem>, MenuItem& menu_item)
{
    m_menu_items.set(&menu_item);
}

void Action::unregister_menu_item(Badge<MenuItem>, MenuItem& menu_item)
{
    m_menu_items.remove(&menu_item);
}

template<typename Callback>
void Action::for_each_toolbar_button(Callback callback)
{
    for (auto& it : m_buttons)
        callback(*it);
}

template<typename Callback>
void Action::for_each_menu_item(Callback callback)
{
    for (auto& it : m_menu_items)
        callback(*it);
}

void Action::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    for_each_toolbar_button([enabled](auto& button) {
        button.set_enabled(enabled);
    });
    for_each_menu_item([enabled](auto& item) {
        item.set_enabled(enabled);
    });
}

void Action::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    for_each_toolbar_button([visible](auto& button) {
        button.set_visible(visible);
    });
    for_each_menu_item([visible](auto& item) {
        item.set_visible(visible);
    });
}

void Action::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;

    if (m_checked && m_action_group) {
        m_action_group->for_each_action([this](auto& other_action) {
            if (this == &other_action)
                return IterationDecision::Continue;
            if (other_action.is_checkable())
                other_action.set_checked(false);
            return IterationDecision::Continue;
        });
    }

    for_each_toolbar_button([checked](auto& button) {
        button.set_checked(checked);
    });
    for_each_menu_item([checked](MenuItem& item) {
        item.set_checked(checked);
    });
}

void Action::set_group(Badge<ActionGroup>, ActionGroup* group)
{
    m_action_group = AK::make_weak_ptr_if_nonnull(group);
}

void Action::set_icon(Gfx::Bitmap const* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    for_each_toolbar_button([icon](auto& button) {
        button.set_icon(icon);
    });
    for_each_menu_item([](auto& menu_item) {
        menu_item.update_from_action({});
    });
}

void Action::set_text(ByteString text)
{
    if (m_text == text)
        return;
    m_text = move(text);
    for_each_toolbar_button([&](auto& button) {
        button.set_text(String::from_byte_string(m_text).release_value_but_fixme_should_propagate_errors());
    });
    for_each_menu_item([&](auto& menu_item) {
        menu_item.update_from_action({});
    });
}

ByteString Action::tooltip() const
{
    return m_tooltip.value_or_lazy_evaluated([this] { return Gfx::parse_ampersand_string(m_text); });
}

void Action::set_tooltip(ByteString tooltip)
{
    if (m_tooltip == tooltip)
        return;
    m_tooltip = move(tooltip);
    for_each_toolbar_button([&](auto& button) {
        button.set_tooltip(MUST(String::from_byte_string(*m_tooltip)));
    });
    for_each_menu_item([&](auto& menu_item) {
        menu_item.update_from_action({});
    });
}

Optional<String> Action::status_tip() const
{
    if (!m_status_tip.is_empty())
        return m_status_tip;

    auto maybe_parsed_action_text = String::from_byte_string(Gfx::parse_ampersand_string(m_text));
    if (maybe_parsed_action_text.is_error())
        return {};
    return maybe_parsed_action_text.release_value();
}

}
