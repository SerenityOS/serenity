#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>

GAction::GAction(const String& text, const String& custom_data, Function<void(const GAction&)> on_activation_callback)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_custom_data(custom_data)
{
}

GAction::GAction(const String& text, Function<void(const GAction&)> on_activation_callback)
    : GAction(text, String(), move(on_activation_callback))
{
}

GAction::GAction(const String& text, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> on_activation_callback)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
{
}

GAction::GAction(const String& text, const GShortcut& shortcut, Function<void(const GAction&)> on_activation_callback)
    : GAction(text, shortcut, nullptr, move(on_activation_callback))
{
}


GAction::GAction(const String& text, const GShortcut& shortcut, RetainPtr<GraphicsBitmap>&& icon, Function<void(const GAction&)> on_activation_callback)
    : on_activation(move(on_activation_callback))
    , m_text(text)
    , m_icon(move(icon))
    , m_shortcut(shortcut)
{
    GApplication::the().register_shortcut_action(Badge<GAction>(), *this);
}

GAction::~GAction()
{
    if (m_shortcut.is_valid())
        GApplication::the().unregister_shortcut_action(Badge<GAction>(), *this);
}

void GAction::activate()
{
    if (on_activation)
        on_activation(*this);
}
