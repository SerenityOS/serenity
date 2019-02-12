#include "WSMenuBar.h"
#include "WSMenu.h"
#include "WSMenuItem.h"
#include <Kernel/Process.h>

WSMenuBar::WSMenuBar(Process& process)
    : m_process(process.make_weak_ptr())
{
}

WSMenuBar::~WSMenuBar()
{
}

