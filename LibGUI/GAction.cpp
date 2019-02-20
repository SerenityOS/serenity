#include <LibGUI/GAction.h>

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

GAction::~GAction()
{
}

void GAction::activate()
{
    if (on_activation)
        on_activation(*this);
}
