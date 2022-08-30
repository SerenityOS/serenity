/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Version.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Icon.h>

namespace GUI {

namespace CommonActions {

NonnullRefPtr<Action> make_about_action(String const& app_name, Icon const& app_icon, Window* parent)
{
    auto weak_parent = AK::make_weak_ptr_if_nonnull<Window>(parent);
    auto action = Action::create(String::formatted("&About {}", app_name), app_icon.bitmap_for_size(16), [=](auto&) {
        AboutDialog::show(app_name, Core::Version::read_long_version_string(), app_icon.bitmap_for_size(32), weak_parent.ptr());
    });
    action->set_status_tip("Show application about box");
    return action;
}

NonnullRefPtr<Action> make_open_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Open...", { Mod_Ctrl, Key_O }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Open an existing file");
    return action;
}

NonnullRefPtr<Action> make_save_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Save", { Mod_Ctrl, Key_S }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/save.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Save the current file");
    return action;
}

NonnullRefPtr<Action> make_save_as_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Save &As...", { Mod_Ctrl | Mod_Shift, Key_S }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/save-as.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Save the current file with a new name");
    return action;
}

NonnullRefPtr<Action> make_move_to_front_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Move to &Front", { Mod_Ctrl | Mod_Shift, Key_Up }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/move-to-front.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Move to the top of the stack");
    return action;
}

NonnullRefPtr<Action> make_move_to_back_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Move to &Back", { Mod_Ctrl | Mod_Shift, Key_Down }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/move-to-back.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Move to the bottom of the stack");
    return action;
}

NonnullRefPtr<Action> make_undo_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("&Undo", { Mod_Ctrl, Key_Z }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/undo.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_redo_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("&Redo", { Mod_Ctrl | Mod_Shift, Key_Z }, { Mod_Ctrl, Key_Y }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/redo.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_delete_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("&Delete", { Mod_None, Key_Delete }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_cut_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Cu&t", { Mod_Ctrl, Key_X }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-cut.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Cut to clipboard");
    return action;
}

NonnullRefPtr<Action> make_copy_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Copy", { Mod_Ctrl, Key_C }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Copy to clipboard");
    return action;
}

NonnullRefPtr<Action> make_paste_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Paste", { Mod_Ctrl, Key_V }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/paste.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Paste from clipboard");
    return action;
}

NonnullRefPtr<Action> make_insert_emoji_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Insert Emoji...", { Mod_Ctrl | Mod_Alt, Key_Space }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/emoji.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Open the Emoji Picker");
    return action;
}

NonnullRefPtr<Action> make_fullscreen_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Fullscreen", { Mod_None, Key_F11 }, move(callback), parent);
    action->set_status_tip("Enter fullscreen mode");
    action->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/fullscreen.png"sv).release_value_but_fixme_should_propagate_errors());
    return action;
}

NonnullRefPtr<Action> make_quit_action(Function<void(Action&)> callback)
{
    auto action = Action::create("&Quit", { Mod_Alt, Key_F4 }, move(callback));
    action->set_status_tip("Quit the application");
    return action;
}

NonnullRefPtr<Action> make_help_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Manual", { Mod_None, Key_F1 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-help.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Show help contents");
    return action;
}

NonnullRefPtr<Action> make_go_back_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Go &Back", { Mod_Alt, Key_Left }, { MouseButton::Backward }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Move one step backward in history");
    return action;
}

NonnullRefPtr<Action> make_go_forward_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("Go &Forward", { Mod_Alt, Key_Right }, { MouseButton::Forward }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Move one step forward in history");
    return action;
}

NonnullRefPtr<Action> make_go_home_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Go &Home", { Mod_Alt, Key_Home }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-home.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_close_tab_action(Function<void(Action&)> callback, Core::Object* parent)
{
    auto action = Action::create("&Close Tab", { Mod_Ctrl, Key_W }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/close-tab.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
    action->set_status_tip("Close current tab");
    return action;
}

NonnullRefPtr<Action> make_reload_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("&Reload", { Mod_Ctrl, Key_R }, Key_F5, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_select_all_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Select &All", { Mod_Ctrl, Key_A }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/select-all.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_rename_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("Re&name", Key_F2, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/rename.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_properties_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return Action::create("P&roperties", { Mod_Alt, Key_Return }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/properties.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_zoom_in_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return GUI::Action::create("Zoom &In", { Mod_Ctrl, Key_Equal }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/zoom-in.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_reset_zoom_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return GUI::Action::create("&Reset Zoom", { Mod_Ctrl, Key_0 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/zoom-reset.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_zoom_out_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return GUI::Action::create("Zoom &Out", { Mod_Ctrl, Key_Minus }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/zoom-out.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_rotate_clockwise_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return GUI::Action::create("Rotate Clock&wise", { Mod_Ctrl | Mod_Shift, Key_GreaterThan }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-cw.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

NonnullRefPtr<Action> make_rotate_counterclockwise_action(Function<void(Action&)> callback, Core::Object* parent)
{
    return GUI::Action::create("Rotate &Counterclockwise", { Mod_Ctrl | Mod_Shift, Key_LessThan }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-rotate-ccw.png"sv).release_value_but_fixme_should_propagate_errors(), move(callback), parent);
}

}

}
