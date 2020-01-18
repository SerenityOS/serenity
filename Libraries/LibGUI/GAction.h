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
#include <LibDraw/GraphicsBitmap.h>
#include <LibGUI/GShortcut.h>
#include <LibGUI/GWindow.h>

class GAction;
class GActionGroup;
class GButton;
class GMenuItem;
class GWidget;

namespace GCommonActions {
NonnullRefPtr<GAction> make_open_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_undo_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_redo_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_cut_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_copy_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_paste_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_delete_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_move_to_front_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_move_to_back_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_fullscreen_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_quit_action(Function<void(GAction&)>);
NonnullRefPtr<GAction> make_go_back_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_go_forward_action(Function<void(GAction&)>, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_go_home_action(Function<void(GAction&)> callback, GWidget* widget = nullptr);
NonnullRefPtr<GAction> make_reload_action(Function<void(GAction&)>, GWidget* widget = nullptr);
};

class GAction : public RefCounted<GAction>
    , public Weakable<GAction> {
public:
    enum class ShortcutScope {
        None,
        ApplicationGlobal,
        WidgetLocal,
    };
    static NonnullRefPtr<GAction> create(const StringView& text, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, move(icon), move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, const GShortcut& shortcut, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(callback), widget));
    }
    static NonnullRefPtr<GAction> create(const StringView& text, const GShortcut& shortcut, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> callback, GWidget* widget = nullptr)
    {
        return adopt(*new GAction(text, shortcut, move(icon), move(callback), widget));
    }
    ~GAction();
    GWidget* widget() { return m_widget.ptr(); }
    const GWidget* widget() const { return m_widget.ptr(); }

    String text() const { return m_text; }
    GShortcut shortcut() const { return m_shortcut; }
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    void set_icon(const GraphicsBitmap* icon) { m_icon = icon; }

    const CObject* activator() const { return m_activator.ptr(); }
    CObject* activator() { return m_activator.ptr(); }

    Function<void(GAction&)> on_activation;

    void activate(CObject* activator = nullptr);

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

    void register_button(Badge<GButton>, GButton&);
    void unregister_button(Badge<GButton>, GButton&);
    void register_menu_item(Badge<GMenuItem>, GMenuItem&);
    void unregister_menu_item(Badge<GMenuItem>, GMenuItem&);

    const GActionGroup* group() const { return m_action_group.ptr(); }
    void set_group(Badge<GActionGroup>, GActionGroup*);

private:
    GAction(const StringView& text, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, const GShortcut&, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, const GShortcut&, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);
    GAction(const StringView& text, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> = nullptr, GWidget* = nullptr);

    template<typename Callback>
    void for_each_toolbar_button(Callback);
    template<typename Callback>
    void for_each_menu_item(Callback);

    String m_text;
    RefPtr<GraphicsBitmap> m_icon;
    GShortcut m_shortcut;
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    ShortcutScope m_scope { ShortcutScope::None };

    HashTable<GButton*> m_buttons;
    HashTable<GMenuItem*> m_menu_items;
    WeakPtr<GWidget> m_widget;
    WeakPtr<GActionGroup> m_action_group;
    WeakPtr<CObject> m_activator;
};
