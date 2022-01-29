/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Window.h>

namespace GUI {

NonnullRefPtr<Action> Action::create(String text, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(String text, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(String text, const Shortcut& shortcut, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, move(callback), parent));
}

NonnullRefPtr<Action> Action::create(String text, const Shortcut& shortcut, const Shortcut& alternate_shortcut, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, alternate_shortcut, move(callback), parent));
}

NonnullRefPtr<Action> Action::create(String text, const Shortcut& shortcut, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, Shortcut {}, move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create(String text, const Shortcut& shortcut, const Shortcut& alternate_shortcut, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, alternate_shortcut, move(icon), move(callback), parent));
}

NonnullRefPtr<Action> Action::create_checkable(String text, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(String text, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), move(icon), move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(String text, const Shortcut& shortcut, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, move(callback), parent, true));
}

NonnullRefPtr<Action> Action::create_checkable(String text, const Shortcut& shortcut, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> callback, Core::Object* parent)
{
    return adopt_ref(*new Action(move(text), shortcut, Shortcut {}, move(icon), move(callback), parent, true));
}

Action::Action(String text, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Action(move(text), Shortcut {}, Shortcut {}, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(String text, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Action(move(text), Shortcut {}, Shortcut {}, move(icon), move(on_activation_callback), parent, checkable)
{
}

Action::Action(String text, const Shortcut& shortcut, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Action(move(text), shortcut, Shortcut {}, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(String text, const Shortcut& shortcut, const Shortcut& alternate_shortcut, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Action(move(text), shortcut, alternate_shortcut, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(String text, const Shortcut& shortcut, const Shortcut& alternate_shortcut, RefPtr<Gfx::Bitmap> icon, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Core::Object(parent)
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
    if (m_shortcut.is_valid() && m_scope == ShortcutScope::ApplicationGlobal) {
        if (auto* app = Application::the())
            app->unregister_global_shortcut_action({}, *this);
    }
}

void Action::activate(Core::Object* activator)
{
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
    m_action_group = AK::try_make_weak_ptr(group);
}

void Action::set_icon(const Gfx::Bitmap* icon)
{
    m_icon = icon;
}

void Action::set_text(String text)
{
    if (m_text == text)
        return;
    m_text = move(text);
    for_each_menu_item([&](auto& menu_item) {
        menu_item.update_from_action({});
    });
}

}
