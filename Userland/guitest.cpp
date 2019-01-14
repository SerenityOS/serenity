#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>
#include <AK/StdLibExtras.h>

int gui_invalidate_window(int window_id)
{
    int rc = syscall(SC_gui_invalidate_window, window_id);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int main(int argc, char** argv)
{
    GUI_CreateWindowParameters wparams;
    wparams.rect = { { 200, 200 }, { 300, 200 } };
    wparams.background_color = 0xffc0c0;
    strcpy(wparams.title, "GUI test app");
    int window_id = syscall(SC_gui_create_window, &wparams);
    if (window_id < 0) {
        perror("gui_create_window");
        return 1;
    }

    int fd = open("/dev/gui_events", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    GUI_WindowBackingStoreInfo backing;
    int rc = syscall(SC_gui_get_window_backing_store, window_id, &backing);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        return 1;
    }

    sys_printf("(Client) window backing %ux%u @ %p\n", backing.size.width, backing.size.height, backing.pixels);

    fast_dword_fill(backing.pixels, 0x00ff00, backing.size.width * backing.size.height);

    rc = gui_invalidate_window(window_id);
    if (rc < 0) {
        perror("gui_invalidate_window");
        return 1;
    }

    for (;;) {
        GUI_Event event;
        ssize_t nread = read(fd, &event, sizeof(event));
        if (nread < 0) {
            perror("read");
            return 1;
        }
        assert(nread == sizeof(event));
        switch (event.type) {
        case GUI_Event::Type::Paint: sys_printf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height); break;
        case GUI_Event::Type::MouseDown: sys_printf("WID=%x MouseDown %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseUp: sys_printf("WID=%x MouseUp %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseMove: sys_printf("WID=%x MouseMove %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        }

        if (event.type == GUI_Event::Type::MouseDown) {
            byte r = rand() % 255;
            byte g = rand() % 255;
            byte b = rand() % 255;
            Color color(r, g, b);
            fast_dword_fill(backing.pixels, color.value(), backing.size.width * backing.size.height);
            gui_invalidate_window(window_id);
        }

    }
    return 0;
}
