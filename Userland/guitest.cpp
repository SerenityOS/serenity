#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Kernel/Syscall.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibC/gui.h>

static void paint(GraphicsBitmap& bitmap, int width, int height);

int main(int argc, char** argv)
{
    GUI_WindowParameters wparams;
    wparams.rect = { { 100, 100 }, { 120, 120 } };
    wparams.background_color = 0xffc0c0;
    strcpy(wparams.title, "GUI test app");
    int window_id = gui_create_window(&wparams);
    if (window_id < 0) {
        perror("gui_create_window");
        return 1;
    }

    int fd = open("/dev/gui_events", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    // NOTE: We never release the backing store. This is just a simple app. :^)
    GUI_WindowBackingStoreInfo backing;
    int rc = gui_get_window_backing_store(window_id, &backing);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        return 1;
    }

    auto bitmap = GraphicsBitmap::create_wrapper(backing.size, backing.pixels);

    dbgprintf("(Client) window backing %ux%u @ %p\n", backing.size.width, backing.size.height, backing.pixels);

    paint(*bitmap, backing.size.width, backing.size.height);

    rc = gui_invalidate_window(window_id, nullptr);
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
        dbgprintf("(%d) ", getpid());
        assert(nread == sizeof(event));
        switch (event.type) {
        case GUI_Event::Type::Paint: dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height); break;
        case GUI_Event::Type::MouseDown: dbgprintf("WID=%x MouseDown %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseUp: dbgprintf("WID=%x MouseUp %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::MouseMove: dbgprintf("WID=%x MouseMove %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
        case GUI_Event::Type::WindowActivated: dbgprintf("WID=%x WindowActivated\n", event.window_id); break;
        case GUI_Event::Type::WindowDeactivated: dbgprintf("WID=%x WindowDeactivated\n", event.window_id); break;
        case GUI_Event::Type::WindowCloseRequest: return 0;
        }

        if (event.type == GUI_Event::Type::Paint) {
            paint(*bitmap, backing.size.width, backing.size.height);
            gui_notify_paint_finished(window_id, nullptr);
        }

        if (event.type == GUI_Event::Type::MouseDown) {
            gui_invalidate_window(window_id, nullptr);
        }

    }
    return 0;
}

void paint(GraphicsBitmap& bitmap, int width, int height)
{
    byte r = rand() % 255;
    byte g = rand() % 255;
    byte b = rand() % 255;
    Color color(r, g, b);
    Painter painter(bitmap);
    painter.fill_rect({0, 0, width, height}, color);
    painter.draw_text({0, 0, width, height}, "Hello World!", TextAlignment::Center, Color::Black);
}
