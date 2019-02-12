#include <LibGUI/GAction.h>

GAction::GAction(const String& text, const String& custom_data, Function<void(const GAction&)> on_activation_callback)
    : m_text(text)
    , on_activation(move(on_activation_callback))
    , m_custom_data(custom_data)
{
}

GAction::GAction(const String& text, Function<void(const GAction&)> on_activation_callback)
    : GAction(text, String(), move(on_activation_callback))
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
