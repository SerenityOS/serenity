#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenuItem.h>

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

void GAction::activate()
{
    if (on_activation)
        on_activation(*this);
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
