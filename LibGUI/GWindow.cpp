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
    m_rect_when_windowless = { 100, 400, 140, 140 };
    m_title_when_windowless = "GWindow";
}

GWindow::~GWindow()
{
    if (m_main_widget)
        delete m_main_widget;
    hide();
}

void GWindow::close()
{
    // FIXME: If we exit the event loop, we're never gonna deal with the delete_later request!
    //        This will become relevant once we support nested event loops.
    if (should_exit_app_on_close())
        GEventLoop::main().exit(0);
    delete_later();
}

void GWindow::show()
{
    if (m_window_id)
        return;

    GUI_WindowParameters wparams;
    wparams.rect = m_rect_when_windowless;
    wparams.background_color = 0xffc0c0;
    strcpy(wparams.title, m_title_when_windowless.characters());
    m_window_id = gui_create_window(&wparams);
    if (m_window_id < 0) {
        perror("gui_create_window");
        exit(1);
    }

    windows().set(m_window_id, this);
    update();
}

void GWindow::hide()
{
    if (!m_window_id)
        return;
    windows().remove(m_window_id);
    int rc = gui_destroy_window(m_window_id);
    if (rc < 0) {
        perror("gui_destroy_window");
        exit(1);
    }
}

void GWindow::set_title(String&& title)
{
    dbgprintf("GWindow::set_title \"%s\"\n", title.characters());
    m_title_when_windowless = title;
    if (m_window_id) {
        int rc = gui_set_window_title(m_window_id, title.characters(), title.length());
        if (rc < 0) {
            perror("gui_set_window_title");
            exit(1);
        }
    }
}

String GWindow::title() const
{
    if (m_window_id) {
        char buffer[256];
        int rc = gui_get_window_title(m_window_id, buffer, sizeof(buffer));
        ASSERT(rc >= 0);
        return String(buffer, rc);
    }
    return m_title_when_windowless;
}

void GWindow::set_rect(const Rect& a_rect)
{
    dbgprintf("GWindow::set_rect! %d,%d %dx%d\n", a_rect.x(), a_rect.y(), a_rect.width(), a_rect.height());
    m_rect_when_windowless = a_rect;
    if (m_window_id) {
        GUI_Rect rect = a_rect;
        int rc = gui_set_window_rect(m_window_id, &rect);
        ASSERT(rc == 0);
    }
}

void GWindow::event(GEvent& event)
{
    if (event.is_mouse_event()) {
        if (m_global_cursor_tracking_widget) {
            // FIXME: This won't work for widgets-within-widgets.
            auto& mouse_event = static_cast<GMouseEvent&>(event);
            Point local_point { mouse_event.x() - m_global_cursor_tracking_widget->relative_rect().x(), mouse_event.y() - m_global_cursor_tracking_widget->relative_rect().y() };
            auto local_event = make<GMouseEvent>(event.type(), local_point, mouse_event.buttons(), mouse_event.button());
            m_global_cursor_tracking_widget->event(*local_event);
        }
        if (!m_main_widget)
            return;
        auto& mouse_event = static_cast<GMouseEvent&>(event);
        if (m_main_widget) {
            auto result = m_main_widget->hit_test(mouse_event.x(), mouse_event.y());
            auto local_event = make<GMouseEvent>(event.type(), Point { result.localX, result.localY }, mouse_event.buttons(), mouse_event.button());
            ASSERT(result.widget);
            return result.widget->event(*local_event);
        }
        return;
    }

    if (event.is_paint_event()) {
        if (!m_main_widget)
            return;
        auto& paint_event = static_cast<GPaintEvent&>(event);
        auto rect = paint_event.rect();
        if (rect.is_empty())
            rect = m_main_widget->rect();
        m_main_widget->event(*make<GPaintEvent>(rect));
        if (m_window_id) {
            GUI_Rect gui_rect = rect;
            int rc = gui_notify_paint_finished(m_window_id, &gui_rect);
            ASSERT(rc == 0);
        }
        return;
    }

    if (event.is_key_event()) {
        if (!m_focused_widget)
            return;
        return m_focused_widget->event(event);
    }

    if (event.type() == GEvent::WindowBecameActive || event.type() == GEvent::WindowBecameInactive) {
        m_is_active = event.type() == GEvent::WindowBecameActive;
        if (m_focused_widget)
            m_focused_widget->update();
        return;
    }

    if (event.type() == GEvent::WindowCloseRequest) {
        close();
        return;
    }

    GObject::event(event);
}

bool GWindow::is_visible() const
{
    return false;
}

void GWindow::update(const Rect& a_rect)
{
    if (!m_window_id)
        return;
    GUI_Rect rect = a_rect;
    int rc = gui_invalidate_window(m_window_id, a_rect.is_null() ? nullptr : &rect);
    ASSERT(rc == 0);
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

void GWindow::set_focused_widget(GWidget* widget)
{
    if (m_focused_widget == widget)
        return;
    if (m_focused_widget) {
        GEventLoop::main().post_event(m_focused_widget, make<GEvent>(GEvent::FocusOut));
        m_focused_widget->update();
    }
    m_focused_widget = widget;
    if (m_focused_widget) {
        GEventLoop::main().post_event(m_focused_widget, make<GEvent>(GEvent::FocusIn));
        m_focused_widget->update();
    }
}

void GWindow::set_global_cursor_tracking_widget(GWidget* widget)
{
    ASSERT(m_window_id);
    if (widget == m_global_cursor_tracking_widget.ptr())
        return;
    m_global_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;
    gui_set_global_cursor_tracking_enabled(m_window_id, widget != nullptr);
}
