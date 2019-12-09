#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenuItem.h>

namespace GCommonActions {

NonnullRefPtr<GAction> make_open_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Open...", { Mod_Ctrl, Key_O }, GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_move_to_front_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Move to front", { Mod_Ctrl | Mod_Shift, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/move-to-front.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_move_to_back_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Move to back", { Mod_Ctrl | Mod_Shift, Key_Down }, GraphicsBitmap::load_from_file("/res/icons/16x16/move-to-back.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_undo_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Undo", { Mod_Ctrl, Key_Z }, GraphicsBitmap::load_from_file("/res/icons/16x16/undo.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_redo_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Redo", { Mod_Ctrl, Key_Y }, GraphicsBitmap::load_from_file("/res/icons/16x16/redo.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_delete_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Delete", { Mod_None, Key_Delete }, GraphicsBitmap::load_from_file("/res/icons/16x16/delete.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_cut_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Cut", { Mod_Ctrl, Key_X }, GraphicsBitmap::load_from_file("/res/icons/cut16.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_copy_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Copy", { Mod_Ctrl, Key_C }, GraphicsBitmap::load_from_file("/res/icons/16x16/edit-copy.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_paste_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Paste", { Mod_Ctrl, Key_V }, GraphicsBitmap::load_from_file("/res/icons/paste16.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_fullscreen_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Fullscreen", { Mod_None, Key_F11 }, move(callback), widget);
}

NonnullRefPtr<GAction> make_quit_action(Function<void(GAction&)> callback)
{
    return GAction::create("Quit", { Mod_Alt, Key_F4 }, move(callback));
}

NonnullRefPtr<GAction> make_go_back_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Go back", { Mod_Alt, Key_Left }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-back.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_go_forward_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Go forward", { Mod_Alt, Key_Right }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_go_home_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Go home", { Mod_Alt, Key_Home }, GraphicsBitmap::load_from_file("/res/icons/16x16/go-home.png"), move(callback), widget);
}

NonnullRefPtr<GAction> make_reload_action(Function<void(GAction&)> callback, GWidget* widget)
{
    return GAction::create("Reload", { Mod_Ctrl, Key_R }, GraphicsBitmap::load_from_file("/res/icons/16x16/reload.png"), move(callback), widget);
}

}

GAction::GAction(const StringView& text, Function<void(GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
}

GAction::GAction(const StringView& text, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
}

GAction::GAction(const StringView& text, const GShortcut& shortcut, Function<void(GAction&)> on_activation_callback, GWidget* widget)
    : GAction(text, shortcut, nullptr, move(on_activation_callback), widget)
{
}

GAction::GAction(const StringView& text, const GShortcut& shortcut, RefPtr<GraphicsBitmap>&& icon, Function<void(GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_shortcut(shortcut)
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
    if (m_widget) {
        m_scope = ShortcutScope::WidgetLocal;
        m_widget->register_local_shortcut_action({}, *this);
    } else {
        m_scope = ShortcutScope::ApplicationGlobal;
        GApplication::the().register_global_shortcut_action({}, *this);
    }
}

GAction::~GAction()
{
    if (m_shortcut.is_valid() && m_scope == ShortcutScope::ApplicationGlobal)
        GApplication::the().unregister_global_shortcut_action({}, *this);
    if (m_widget && m_scope == ShortcutScope::WidgetLocal)
        m_widget->unregister_local_shortcut_action({}, *this);
}

void GAction::activate(CObject* activator)
{
    if (activator)
        m_activator = activator->make_weak_ptr();
    if (on_activation)
        on_activation(*this);
    m_activator = nullptr;
}

void GAction::register_button(Badge<GButton>, GButton& button)
{
    m_buttons.set(&button);
}

void GAction::unregister_button(Badge<GButton>, GButton& button)
{
    m_buttons.remove(&button);
}

void GAction::register_menu_item(Badge<GMenuItem>, GMenuItem& menu_item)
{
    m_menu_items.set(&menu_item);
}

void GAction::unregister_menu_item(Badge<GMenuItem>, GMenuItem& menu_item)
{
    m_menu_items.remove(&menu_item);
}

template<typename Callback>
void GAction::for_each_toolbar_button(Callback callback)
{
    for (auto& it : m_buttons)
        callback(*it);
}

template<typename Callback>
void GAction::for_each_menu_item(Callback callback)
{
    for (auto& it : m_menu_items)
        callback(*it);
}

void GAction::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    for_each_toolbar_button([enabled](GButton& button) {
        button.set_enabled(enabled);
    });
    for_each_menu_item([enabled](GMenuItem& item) {
        item.set_enabled(enabled);
    });
}

void GAction::set_checked(bool checked)
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

    for_each_toolbar_button([checked](GButton& button) {
        button.set_checked(checked);
    });
    for_each_menu_item([checked](GMenuItem& item) {
        item.set_checked(checked);
    });
}

void GAction::set_group(Badge<GActionGroup>, GActionGroup* group)
{
    m_action_group = group ? group->make_weak_ptr() : nullptr;
}
