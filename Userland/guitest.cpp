#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Kernel/GUITypes.h>
#include <Kernel/Syscall.h>

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

    }
    return 0;
}
