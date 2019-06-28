#include <LibCore/CConfigFile.h>
#include <WindowServer/WSCompositor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <stdio.h>

int main(int, char**)
{
    WSEventLoop loop;

    auto wm_config = CConfigFile::get_for_app("WindowManager");
    WSScreen screen(wm_config->read_num_entry("Screen", "Width", 1024),
        wm_config->read_num_entry("Screen", "Height", 768));
    WSCompositor::the();
    WSWindowManager window_manager;

    dbgprintf("Entering WindowServer main loop.\n");
    WSEventLoop::the().exec();
    ASSERT_NOT_REACHED();
}
