#include "WSMenuBar.h"
#include "WSMenu.h"
#include "WSMenuItem.h"

WSMenuBar::WSMenuBar(WSClientConnection& client, int menubar_id)
    : m_client(client)
    , m_menubar_id(menubar_id)
{
}

WSMenuBar::~WSMenuBar()
{
}
