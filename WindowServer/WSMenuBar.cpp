#include "WSMenuBar.h"
#include "WSMenu.h"
#include "WSMenuItem.h"
#include <Kernel/Process.h>

WSMenuBar::WSMenuBar(int menubar_id, Process& process)
    : m_menubar_id(menubar_id)
    , m_process(process.make_weak_ptr())
{
}

WSMenuBar::~WSMenuBar()
{
}

