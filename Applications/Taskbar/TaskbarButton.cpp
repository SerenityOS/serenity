#include "TaskbarButton.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GMenu.h>
#include <WindowServer/WSAPITypes.h>

static void set_window_minimized_state(const WindowIdentifier& identifier, bool minimized)
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::WM_SetWindowMinimized;
    message.wm.client_id = identifier.client_id();
    message.wm.window_id = identifier.window_id();
    message.wm.minimized = minimized;
    bool success = GEventLoop::post_message_to_server(message);
    ASSERT(success);
}

TaskbarButton::TaskbarButton(const WindowIdentifier& identifier, GWidget* parent)
    : GButton(parent)
    , m_identifier(identifier)
{
}

TaskbarButton::~TaskbarButton()
{
}

void TaskbarButton::context_menu_event(GContextMenuEvent&)
{
    ensure_menu().popup(screen_relative_rect().location());
}

GMenu& TaskbarButton::ensure_menu()
{
    if (!m_menu) {
        m_menu = make<GMenu>("");
        m_menu->add_action(GAction::create("Minimize", [this](auto&) {
            set_window_minimized_state(m_identifier, true);
        }));
        m_menu->add_action(GAction::create("Unminimize", [this](auto&) {
            set_window_minimized_state(m_identifier, false);
        }));
        m_menu->add_action(GAction::create("Close", [this](auto&) {
            dbgprintf("FIXME: Close!\n");
        }));
    }
    return *m_menu;
}
