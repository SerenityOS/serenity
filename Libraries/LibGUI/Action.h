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

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibGfx/Bitmap.h>
#include <LibGUI/Shortcut.h>
#include <LibGUI/Window.h>

namespace GUI {

class Action;
class ActionGroup;
class Button;
class MenuItem;

namespace CommonActions {
NonnullRefPtr<Action> make_open_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_undo_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_redo_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_cut_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_copy_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_paste_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_delete_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_move_to_front_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_move_to_back_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_fullscreen_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_quit_action(Function<void(Action&)>);
NonnullRefPtr<Action> make_go_back_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_go_forward_action(Function<void(Action&)>, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_go_home_action(Function<void(Action&)> callback, Core::Object* parent = nullptr);
NonnullRefPtr<Action> make_reload_action(Function<void(Action&)>, Core::Object* parent = nullptr);
};

class Action final : public Core::Object {
    C_OBJECT(Action)
public:
    enum class ShortcutScope {
        None,
        WidgetLocal,
        WindowLocal,
        ApplicationGlobal,
    };
    static NonnullRefPtr<Action> create(const StringView& text, Function<void(Action&)> callback, Core::Object* parent = nullptr)
    {
        return adopt(*new Action(text, move(callback), parent));
    }
    static NonnullRefPtr<Action> create(const StringView& text, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> callback, Core::Object* parent = nullptr)
    {
        return adopt(*new Action(text, move(icon), move(callback), parent));
    }
    static NonnullRefPtr<Action> create(const StringView& text, const Shortcut& shortcut, Function<void(Action&)> callback, Core::Object* parent = nullptr)
    {
        return adopt(*new Action(text, shortcut, move(callback), parent));
    }
    static NonnullRefPtr<Action> create(const StringView& text, const Shortcut& shortcut, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> callback, Core::Object* parent = nullptr)
    {
        return adopt(*new Action(text, shortcut, move(icon), move(callback), parent));
    }
    virtual ~Action() override;

    String text() const { return m_text; }
    Shortcut shortcut() const { return m_shortcut; }
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }
    void set_icon(const Gfx::Bitmap* icon) { m_icon = icon; }

    const Core::Object* activator() const { return m_activator.ptr(); }
    Core::Object* activator() { return m_activator.ptr(); }

    Function<void(Action&)> on_activation;

    void activate(Core::Object* activator = nullptr);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const
    {
        ASSERT(is_checkable());
        return m_checked;
    }
    void set_checked(bool);

    void register_button(Badge<Button>, Button&);
    void unregister_button(Badge<Button>, Button&);
    void register_menu_item(Badge<MenuItem>, MenuItem&);
    void unregister_menu_item(Badge<MenuItem>, MenuItem&);

    const ActionGroup* group() const { return m_action_group.ptr(); }
    void set_group(Badge<ActionGroup>, ActionGroup*);

private:
    virtual bool is_action() const override { return true; }

    Action(const StringView& text, Function<void(Action&)> = nullptr, Core::Object* = nullptr);
    Action(const StringView& text, const Shortcut&, Function<void(Action&)> = nullptr, Core::Object* = nullptr);
    Action(const StringView& text, const Shortcut&, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> = nullptr, Core::Object* = nullptr);
    Action(const StringView& text, RefPtr<Gfx::Bitmap>&& icon, Function<void(Action&)> = nullptr, Core::Object* = nullptr);

    template<typename Callback>
    void for_each_toolbar_button(Callback);
    template<typename Callback>
    void for_each_menu_item(Callback);

    String m_text;
    RefPtr<Gfx::Bitmap> m_icon;
    Shortcut m_shortcut;
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    ShortcutScope m_scope { ShortcutScope::None };

    HashTable<Button*> m_buttons;
    HashTable<MenuItem*> m_menu_items;
    WeakPtr<ActionGroup> m_action_group;
    WeakPtr<Core::Object> m_activator;
};

}

template<>
inline bool Core::is<GUI::Action>(const Core::Object& object)
{
    return object.is_action();
}
