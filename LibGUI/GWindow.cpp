#include "GWindow.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWidget.h"
#include <SharedGraphics/GraphicsBitmap.h>
#include <LibC/gui.h>
#include <LibC/stdio.h>
#include <LibC/stdlib.h>
#include <LibC/unistd.h>
#include <AK/HashMap.h>

static HashMap<int, GWindow*>* s_windows;

static HashMap<int, GWindow*>& windows()
{
    if (!s_windows)
        s_windows = new HashMap<int, GWindow*>;
    return *s_windows;
}

GWindow* GWindow::from_window_id(int window_id)
{
    auto it = windows().find(window_id);
    if (it != windows().end())
        return (*it).value;
    return nullptr;
}

GWindow::GWindow(GObject* parent)
    : GObject(parent)
{
    GUI_WindowParameters wparams;
    wparams.rect = { { 100, 400 }, { 140, 140 } };
    wparams.background_color = 0xffc0c0;
    strcpy(wparams.title, "GWindow");
    m_window_id = gui_create_window(&wparams);
    if (m_window_id < 0) {
        perror("gui_create_window");
        exit(1);
    }

    GUI_WindowBackingStoreInfo backing;
    int rc = gui_get_window_backing_store(m_window_id, &backing);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        exit(1);
    }
    m_backing = GraphicsBitmap::create_wrapper(backing.size, backing.pixels);

    windows().set(m_window_id, this);
}

GWindow::~GWindow()
{
}

void GWindow::set_title(String&& title)
{
    dbgprintf("GWindow::set_title \"%s\"\n", title.characters());
    GUI_WindowParameters params;
    int rc = gui_get_window_parameters(m_window_id, &params);
    ASSERT(rc == 0);
    strcpy(params.title, title.characters());;
    rc = gui_set_window_parameters(m_window_id, &params);
    ASSERT(rc == 0);
    m_title = move(title);
}

void GWindow::set_rect(const Rect& rect)
{
    // FIXME: This is a hack to fudge the race with WSWindowManager trying to display @ old rect.
    sleep(10);

    dbgprintf("GWindow::set_rect %d,%d %dx%d\n", m_rect.x(), m_rect.y(), m_rect.width(), m_rect.height());
    GUI_WindowParameters params;
    int rc = gui_get_window_parameters(m_window_id, &params);
    ASSERT(rc == 0);
    params.rect = rect;
    rc = gui_set_window_parameters(m_window_id, &params);
    ASSERT(rc == 0);
    m_rect = rect;
    GUI_WindowBackingStoreInfo backing;
    rc = gui_get_window_backing_store(m_window_id, &backing);
    if (rc < 0) {
        perror("gui_get_window_backing_store");
        exit(1);
    }
    m_backing = GraphicsBitmap::create_wrapper(backing.size, backing.pixels);
}

void GWindow::event(GEvent& event)
{
    if (event.is_mouse_event()) {
        if (!m_main_widget)
            return;
        auto& mouse_event = static_cast<GMouseEvent&>(event);
        if (m_main_widget) {
            auto result = m_main_widget->hit_test(mouse_event.x(), mouse_event.y());
            auto local_event = make<GMouseEvent>(event.type(), Point { result.localX, result.localY }, mouse_event.buttons(), mouse_event.button());
            ASSERT(result.widget);
            return result.widget->event(*local_event);
        }
    }

    if (event.is_paint_event()) {
        if (!m_main_widget)
            return;
        auto& paint_event = static_cast<GPaintEvent&>(event);
        auto rect = paint_event.rect();
        if (rect.is_empty())
            rect = m_main_widget->rect();
        m_main_widget->event(*make<GPaintEvent>(rect));
        GUI_Rect gui_rect = rect;
        int rc = gui_invalidate_window(m_window_id, &gui_rect);
        ASSERT(rc == 0);
    }

    return GObject::event(event);
}

bool GWindow::is_visible() const
{
    return false;
}

void GWindow::close()
{
}

void GWindow::show()
{
}

void GWindow::update()
{
    GEventLoop::main().post_event(this, make<GPaintEvent>());
}

void GWindow::set_main_widget(GWidget* widget)
{
    if (m_main_widget == widget)
        return;
    m_main_widget = widget;
    if (widget)
        widget->set_window(this);
    update();
}
