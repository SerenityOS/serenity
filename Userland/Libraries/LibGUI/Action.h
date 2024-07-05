/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteString.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Timer.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Shortcut.h>
#include <LibGfx/Forward.h>

namespace GUI {

namespace CommonActions {
enum class QuitAltShortcut {
    None,
    CtrlW
};

NonnullRefPtr<Action> make_about_action(String const& app_name, Icon const& app_icon, Window* parent = nullptr);
NonnullRefPtr<Action> make_open_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_save_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_save_as_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_undo_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_redo_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_cut_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_copy_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_paste_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_delete_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_move_to_front_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_move_to_back_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_insert_emoji_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_fullscreen_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_quit_action(Function<void(Action&)>, QuitAltShortcut = QuitAltShortcut::CtrlW);
NonnullRefPtr<Action> make_help_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_go_back_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_go_forward_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_go_home_action(Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_close_tab_action(Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_reload_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_select_all_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_rename_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_properties_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_zoom_in_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_zoom_out_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_reset_zoom_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_rotate_clockwise_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_rotate_counterclockwise_action(Function<void(Action&)>, Core::EventReceiver* parent = nullptr);
NonnullRefPtr<Action> make_command_palette_action(Window* window = nullptr);

};

class Action final : public Core::EventReceiver {
    C_OBJECT(Action)
public:
    enum class ShortcutScope {
        None,
        WidgetLocal,
        WindowLocal,
        ApplicationGlobal,
    };
    static NonnullRefPtr<Action> create(ByteString text, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create(ByteString text, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create(ByteString text, Shortcut const& shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create(ByteString text, Shortcut const& shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create(ByteString text, Shortcut const& shortcut, Shortcut const& alternate_shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create_checkable(ByteString text, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create_checkable(ByteString text, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create_checkable(ByteString text, Shortcut const& shortcut, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);
    static NonnullRefPtr<Action> create_checkable(ByteString text, Shortcut const& shortcut, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> callback, Core::EventReceiver* parent = nullptr);

    static RefPtr<Action> find_action_for_shortcut(Core::EventReceiver& object, Shortcut const& shortcut);

    virtual ~Action() override;

    ByteString text() const { return m_text; }
    void set_text(ByteString);

    ByteString tooltip() const;
    void set_tooltip(ByteString);

    Optional<String> status_tip() const;
    void set_status_tip(String status_tip) { m_status_tip = move(status_tip); }

    Shortcut const& shortcut() const { return m_shortcut; }
    Shortcut const& alternate_shortcut() const { return m_alternate_shortcut; }
    Gfx::Bitmap const* icon() const { return m_icon.ptr(); }
    void set_icon(Gfx::Bitmap const*);

    Core::EventReceiver const* activator() const { return m_activator.ptr(); }
    Core::EventReceiver* activator() { return m_activator.ptr(); }

    Function<void(Action&)> on_activation;

    void activate(Core::EventReceiver* activator = nullptr);
    void process_event(Window& window, Event& event);
    void flash_menubar_menu(GUI::Window& window);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const
    {
        VERIFY(is_checkable());
        return m_checked;
    }
    void set_checked(bool);

    bool is_activating() const { return m_activating; }

    bool swallow_key_event_when_disabled() const { return m_swallow_key_event_when_disabled; }
    void set_swallow_key_event_when_disabled(bool swallow) { m_swallow_key_event_when_disabled = swallow; }

    void register_button(Badge<Button>, Button&);
    void unregister_button(Badge<Button>, Button&);
    void register_menu_item(Badge<MenuItem>, MenuItem&);
    void unregister_menu_item(Badge<MenuItem>, MenuItem&);

    ActionGroup const* group() const { return m_action_group.ptr(); }
    void set_group(Badge<ActionGroup>, ActionGroup*);

    HashTable<MenuItem*> const& menu_items() const { return m_menu_items; }

private:
    Action(ByteString, Function<void(Action&)> = nullptr, Core::EventReceiver* = nullptr, bool checkable = false);
    Action(ByteString, Shortcut const&, Function<void(Action&)> = nullptr, Core::EventReceiver* = nullptr, bool checkable = false);
    Action(ByteString, Shortcut const&, Shortcut const&, Function<void(Action&)> = nullptr, Core::EventReceiver* = nullptr, bool checkable = false);
    Action(ByteString, Shortcut const&, Shortcut const&, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> = nullptr, Core::EventReceiver* = nullptr, bool checkable = false);
    Action(ByteString, RefPtr<Gfx::Bitmap const> icon, Function<void(Action&)> = nullptr, Core::EventReceiver* = nullptr, bool checkable = false);

    template<typename Callback>
    void for_each_toolbar_button(Callback);
    template<typename Callback>
    void for_each_menu_item(Callback);

    ByteString m_text;
    Optional<ByteString> m_tooltip;
    String m_status_tip;
    RefPtr<Gfx::Bitmap const> m_icon;
    Shortcut m_shortcut;
    Shortcut m_alternate_shortcut;
    bool m_enabled { true };
    bool m_visible { true };
    bool m_checkable { false };
    bool m_checked { false };
    bool m_swallow_key_event_when_disabled { false };
    bool m_activating { false };
    ShortcutScope m_scope { ShortcutScope::None };

    HashTable<Button*> m_buttons;
    HashTable<MenuItem*> m_menu_items;
    WeakPtr<ActionGroup> m_action_group;
    WeakPtr<Core::EventReceiver> m_activator;
};

}
