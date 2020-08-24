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

#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Window.h>

namespace GUI {

namespace CommonActions {

NonnullRefPtr<Action> make_open_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Open...", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_save_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Save", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_save_as_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Save As...", { Mod_Ctrl | Mod_Shift, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_move_to_front_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Move to front", { Mod_Ctrl | Mod_Shift, Key_Up }, Gfx::Bitmap::load_from_file("/res/icons/16x16/move-to-front.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_move_to_back_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Move to back", { Mod_Ctrl | Mod_Shift, Key_Down }, Gfx::Bitmap::load_from_file("/res/icons/16x16/move-to-back.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_undo_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Undo", { Mod_Ctrl, Key_Z }, Gfx::Bitmap::load_from_file("/res/icons/16x16/undo.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_redo_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Redo", { Mod_Ctrl, Key_Y }, Gfx::Bitmap::load_from_file("/res/icons/16x16/redo.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_delete_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Delete", { Mod_None, Key_Delete }, Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_cut_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Cut", { Mod_Ctrl, Key_X }, Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-cut.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_copy_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Copy", { Mod_Ctrl, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_paste_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Paste", { Mod_Ctrl, Key_V }, Gfx::Bitmap::load_from_file("/res/icons/16x16/paste.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_fullscreen_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Fullscreen", { Mod_None, Key_F11 }, move(callback), parent);
}

NonnullRefPtr<Action> make_quit_action(Function<void(Action&)> callback)
{
    return Action::create("Quit", { Mod_Alt, Key_F4 }, move(callback));
}

NonnullRefPtr<Action> make_go_back_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Go back", { Mod_Alt, Key_Left }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_go_forward_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Go forward", { Mod_Alt, Key_Right }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_go_home_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Go home", { Mod_Alt, Key_Home }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-home.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_reload_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Reload", { Mod_Ctrl, Key_R }, Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"), move(callback), parent);
}

NonnullRefPtr<Action> make_select_all_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Select all", { Mod_Ctrl, Key_A }, Gfx::Bitmap::load_from_file("/res/icons/16x16/select-all.png"), move(callback), parent);
}

}

Action::Action(const StringView& text, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Core::Object(parent)
    , on_activation(move(on_activation_callback))
    , m_text(text)
    , m_checkable(checkable)
{
}

Action::Action(const StringView& text, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Core::Object(parent)
    , on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_checkable(checkable)
{
}

Action::Action(const StringView& text, const Shortcut& shortcut, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Action(text, shortcut, nullptr, move(on_activation_callback), parent, checkable)
{
}

Action::Action(const StringView& text, const Shortcut& shortcut, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> on_activation_callback, Core::Object* parent, bool checkable)
    : Core::Object(parent)
    , on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_shortcut(shortcut)
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
    m_action_group = group ? group->make_weak_ptr() : nullptr;
}

void Action::set_icon(const Gfx::Bitmap* icon)
{
    m_icon = icon;
}

}
