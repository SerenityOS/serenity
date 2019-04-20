#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenuItem.h>

GAction::GAction(const String& text, const String& custom_data, Function<void(const GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_custom_data(custom_data)
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
}

GAction::GAction(const String& text, Function<void(const GAction&)> on_activation_callback, GWidget* widget)
    : GAction(text, String(), move(on_activation_callback), widget)
{
}

GAction::GAction(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
}

GAction::GAction(const String& text, const GShortcut& shortcut, Function<void(const GAction&)> on_activation_callback, GWidget* widget)
    : GAction(text, shortcut, nullptr, move(on_activation_callback), widget)
{
}


GAction::GAction(const String& text, const GShortcut& shortcut, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> on_activation_callback, GWidget* widget)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_shortcut(shortcut)
    , m_widget(widget ? widget->make_weak_ptr() : nullptr)
{
    if (m_widget) {
        m_scope = ShortcutScope::WidgetLocal;
        m_widget->register_local_shortcut_action(Badge<GAction>(), *this);
    } else {
        m_scope = ShortcutScope::ApplicationGlobal;
        GApplication::the().register_global_shortcut_action(Badge<GAction>(), *this);
    }
}

GAction::~GAction()
{
    if (m_shortcut.is_valid() && m_scope == ShortcutScope::ApplicationGlobal)
        GApplication::the().unregister_global_shortcut_action(Badge<GAction>(), *this);
    if (m_widget && m_scope == ShortcutScope::WidgetLocal)
        m_widget->unregister_local_shortcut_action(Badge<GAction>(), *this);
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
    for_each_toolbar_button([enabled] (GButton& button) {
        button.set_enabled(enabled);
    });
    for_each_menu_item([enabled] (GMenuItem& item) {
        item.set_enabled(enabled);
    });
}
