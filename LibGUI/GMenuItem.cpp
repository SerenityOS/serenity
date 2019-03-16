#include <LibGUI/GMenuItem.h>
#include <LibGUI/GAction.h>

GMenuItem::GMenuItem(Type type)
    : m_type(type)
{
}

GMenuItem::GMenuItem(Retained<GAction>&& action)
    : m_type(Action)
    , m_action(move(action))
{
}

GMenuItem::~GMenuItem()
{
}

