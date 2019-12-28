#include <LibCore/CConfigFile.h>
#include <LibDraw/Palette.h>
#include <LibDraw/SystemTheme.h>
#include <WindowServer/WSCompositor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <signal.h>
#include <stdio.h>

int main(int, char**)
{
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    auto wm_config = CConfigFile::get_for_app("WindowManager");
    auto theme_name = wm_config->read_entry("Theme", "Name", "Default");

    auto theme = load_system_theme(String::format("/res/themes/%s.ini", theme_name.characters()));
    ASSERT(theme);
    set_system_theme(*theme);
    auto palette = PaletteImpl::create_with_shared_buffer(*theme);

    WSEventLoop loop;

    WSScreen screen(wm_config->read_num_entry("Screen", "Width", 1024),
        wm_config->read_num_entry("Screen", "Height", 768));
    WSCompositor::the();
    auto wm = WSWindowManager::construct(*palette);

    dbgprintf("Entering WindowServer main loop.\n");
    loop.exec();
    ASSERT_NOT_REACHED();
}
