#include "WindowActions.h"
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

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
        "New tab", { Mod_Ctrl, Key_T }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"), [this](auto&) {
            if (on_create_new_tab)
                on_create_new_tab();
        },
        &window);

    m_next_tab_action = GUI::Action::create(
        "Next tab", { Mod_Ctrl, Key_PageDown }, [this](auto&) {
            if (on_next_tab)
                on_next_tab();
        },
        &window);

    m_previous_tab_action = GUI::Action::create(
        "Previous tab", { Mod_Ctrl, Key_PageUp }, [this](auto&) {
            if (on_previous_tab)
                on_previous_tab();
        },
        &window);

    m_about_action = GUI::Action::create(
        "About", [this](const GUI::Action&) {
            if (on_about)
                on_about();
        },
        &window);
}

}
