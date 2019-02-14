#include "WSMenuBar.h"
#include "WSMenu.h"
#include "WSMenuItem.h"

WSMenuBar::WSMenuBar(int client_id, int menubar_id)
    : m_client_id(client_id)
    , m_menubar_id(menubar_id)
{
}

WSMenuBar::~WSMenuBar()
{
}

