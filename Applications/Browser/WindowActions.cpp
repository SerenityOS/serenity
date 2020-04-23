#include "WindowActions.h"
#include <LibGUI/Window.h>

namespace Browser {

static WindowActions* s_the;

WindowActions& WindowActions::the()
{
    ASSERT(s_the);
    return *s_the;
}

WindowActions::WindowActions(GUI::Window& window)
{
    ASSERT(!s_the);
    s_the = this;
    m_create_new_tab_action = GUI::Action::create(
        "New tab", { Mod_Ctrl, Key_T }, [this](auto&) {
            if (on_create_new_tab)
                on_create_new_tab();
        },
        &window);
}

}
