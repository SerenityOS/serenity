#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>

void GActionGroup::add_action(GAction& action)
{
    action.set_group({}, this);
    m_actions.set(&action);
}

void GActionGroup::remove_action(GAction& action)
{
    action.set_group({}, nullptr);
    m_actions.remove(&action);
}
