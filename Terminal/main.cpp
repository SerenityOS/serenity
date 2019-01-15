#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Widgets/Font.h>
#include <Widgets/GraphicsBitmap.h>
#include <Widgets/Painter.h>
#include <gui.h>
#include "Terminal.h"

static void paint(GraphicsBitmap& bitmap, int width, int height);

int main(int argc, char** argv)
{
    int fd = open("/dev/gui_events", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    Terminal terminal;
    terminal.create_window();
    terminal.paint();

    for (;;) {
        GUI_Event event;
        ssize_t nread = read(fd, &event, sizeof(event));
        if (nread < 0) {
            perror("read");
            return 1;
        }
        assert(nread == sizeof(event));
        dbgprintf("(Terminal:%d) ", getpid());
        switch (event.type) {
        case GUI_Event::Type::Paint: dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height); break;
        case GUI_Event::Type::MouseDown: dbgprintf("WID=%x MouseDown %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseUp: dbgprintf("WID=%x MouseUp %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseMove: dbgprintf("WID=%x MouseMove %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        }

        if (event.type == GUI_Event::Type::MouseDown)
            terminal.paint();
    }
    return 0;
}
