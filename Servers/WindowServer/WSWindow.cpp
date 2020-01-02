#include "WSWindow.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSScreen.h"
#include "WSWindowManager.h"
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WindowClientEndpoint.h>

static String default_window_icon_path()
{
    return "/res/icons/16x16/window.png";
}

static GraphicsBitmap& default_window_icon()
{
    static GraphicsBitmap* s_icon;
    if (!s_icon)
        s_icon = GraphicsBitmap::load_from_file(default_window_icon_path()).leak_ref();
    return *s_icon;
}

WSWindow::WSWindow(CObject& parent, WSWindowType type)
    : CObject(&parent)
    , m_type(type)
    , m_icon(default_window_icon())
    , m_frame(*this)
{
    WSWindowManager::the().add_window(*this);
}

WSWindow::WSWindow(WSClientConnection& client, WSWindowType window_type, int window_id, bool modal, bool resizable, bool fullscreen)
    : CObject(&client)
    , m_client(&client)
    , m_type(window_type)
    , m_modal(modal)
    , m_resizable(resizable)
    , m_fullscreen(fullscreen)
    , m_window_id(window_id)
    , m_icon(default_window_icon())
    , m_frame(*this)
{
    // FIXME: This should not be hard-coded here.
    if (m_type == WSWindowType::Taskbar) {
        m_wm_event_mask = WSWMEventMask::WindowStateChanges | WSWMEventMask::WindowRemovals | WSWMEventMask::WindowIconChanges;
        m_listens_to_wm_events = true;
    }
    WSWindowManager::the().add_window(*this);
}

WSWindow::~WSWindow()
{
    WSWindowManager::the().remove_window(*this);
}

void WSWindow::set_title(const String& title)
{
    if (m_title == title)
        return;
    m_title = title;
    WSWindowManager::the().notify_title_changed(*this);
}

void WSWindow::set_rect(const Rect& rect)
{
    Rect old_rect;
    if (m_rect == rect)
        return;
    old_rect = m_rect;
    m_rect = rect;
    if (!m_client && (!m_backing_store || old_rect.size() != rect.size())) {
        m_backing_store = GraphicsBitmap::create(GraphicsBitmap::Format::RGB32, m_rect.size());
    }
    m_frame.notify_window_rect_changed(old_rect, rect);
}

void WSWindow::handle_mouse_event(const WSMouseEvent& event)
{
    set_automatic_cursor_tracking_enabled(event.buttons() != 0);

    switch (event.type()) {
    case WSEvent::MouseMove:
        m_client->post_message(WindowClient::MouseMove(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case WSEvent::MouseDown:
        m_client->post_message(WindowClient::MouseDown(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case WSEvent::MouseDoubleClick:
        m_client->post_message(WindowClient::MouseDoubleClick(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case WSEvent::MouseUp:
        m_client->post_message(WindowClient::MouseUp(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    case WSEvent::MouseWheel:
        m_client->post_message(WindowClient::MouseWheel(m_window_id, event.position(), (u32)event.button(), event.buttons(), event.modifiers(), event.wheel_delta()));
        break;
    default:
        ASSERT_NOT_REACHED();
    }
}

void WSWindow::set_minimized(bool minimized)
{
    if (m_minimized == minimized)
        return;
    m_minimized = minimized;
    start_minimize_animation();
    if (!minimized)
        request_update({ {}, size() });
    invalidate();
    WSWindowManager::the().notify_minimization_state_changed(*this);
}

void WSWindow::set_opacity(float opacity)
{
     if (m_opacity == opacity)
         return;
     m_opacity = opacity;
     WSWindowManager::the().notify_opacity_changed(*this);
}

void WSWindow::set_occluded(bool occluded)
{
    if (m_occluded == occluded)
        return;
    m_occluded = occluded;
    WSWindowManager::the().notify_occlusion_state_changed(*this);
}

void WSWindow::set_maximized(bool maximized)
{
    if (m_maximized == maximized)
        return;
    m_maximized = maximized;
    auto old_rect = m_rect;
    if (maximized) {
        m_unmaximized_rect = m_rect;
        set_rect(WSWindowManager::the().maximized_window_rect(*this));
    } else {
        set_rect(m_unmaximized_rect);
    }
    m_frame.did_set_maximized({}, maximized);
    CEventLoop::current().post_event(*this, make<WSResizeEvent>(old_rect, m_rect));
}

void WSWindow::event(CEvent& event)
{
    if (!m_client) {
        ASSERT(parent());
        event.ignore();
        return;
    }

    if (is_blocked_by_modal_window())
        return;

    if (static_cast<WSEvent&>(event).is_mouse_event())
        return handle_mouse_event(static_cast<const WSMouseEvent&>(event));

    switch (event.type()) {
    case WSEvent::WindowEntered:
        m_client->post_message(WindowClient::WindowEntered(m_window_id));
        break;
    case WSEvent::WindowLeft:
        m_client->post_message(WindowClient::WindowLeft(m_window_id));
        break;
    case WSEvent::KeyDown:
        m_client->post_message(
            WindowClient::KeyDown(m_window_id,
                (u8) static_cast<const WSKeyEvent&>(event).character(),
                (u32) static_cast<const WSKeyEvent&>(event).key(),
                static_cast<const WSKeyEvent&>(event).modifiers()));
        break;
    case WSEvent::KeyUp:
        m_client->post_message(
            WindowClient::KeyUp(m_window_id,
                (u8) static_cast<const WSKeyEvent&>(event).character(),
                (u32) static_cast<const WSKeyEvent&>(event).key(),
                static_cast<const WSKeyEvent&>(event).modifiers()));
        break;
    case WSEvent::WindowActivated:
        m_client->post_message(WindowClient::WindowActivated(m_window_id));
        break;
    case WSEvent::WindowDeactivated:
        m_client->post_message(WindowClient::WindowDeactivated(m_window_id));
        break;
    case WSEvent::WindowCloseRequest:
        m_client->post_message(WindowClient::WindowCloseRequest(m_window_id));
        break;
    case WSEvent::WindowResized:
        m_client->post_message(
            WindowClient::WindowResized(
                m_window_id,
                static_cast<const WSResizeEvent&>(event).old_rect(),
                static_cast<const WSResizeEvent&>(event).rect()));
        break;
    default:
        break;
    }
}

void WSWindow::set_global_cursor_tracking_enabled(bool enabled)
{
    m_global_cursor_tracking_enabled = enabled;
}

void WSWindow::set_visible(bool b)
{
    if (m_visible == b)
        return;
    m_visible = b;
    invalidate();
}

void WSWindow::invalidate()
{
    WSWindowManager::the().invalidate(*this);
}

void WSWindow::invalidate(const Rect& rect)
{
    WSWindowManager::the().invalidate(*this, rect);
}

bool WSWindow::is_active() const
{
    return WSWindowManager::the().active_window() == this;
}

bool WSWindow::is_blocked_by_modal_window() const
{
    return !is_modal() && client() && client()->is_showing_modal_window();
}

void WSWindow::set_default_icon()
{
    m_icon = default_window_icon();
}

void WSWindow::request_update(const Rect& rect)
{
    if (m_pending_paint_rects.is_empty()) {
        deferred_invoke([this](auto&) {
            client()->post_paint_message(*this);
        });
    }
    m_pending_paint_rects.add(rect);
}

void WSWindow::popup_window_menu(const Point& position)
{
    if (!m_window_menu) {
        m_window_menu = WSMenu::construct(nullptr, -1, "(Window Menu)");
        m_window_menu->add_item(make<WSMenuItem>(*m_window_menu, 1, "Minimize"));
        m_window_menu->add_item(make<WSMenuItem>(*m_window_menu, 2, "Unminimize"));
        m_window_menu->add_item(make<WSMenuItem>(*m_window_menu, WSMenuItem::Type::Separator));
        m_window_menu->add_item(make<WSMenuItem>(*m_window_menu, 3, "Close"));

        m_window_menu->on_item_activation = [&](auto& item) {
            switch (item.identifier()) {
            case 1:
                set_minimized(true);
                break;
            case 2:
                set_minimized(false);
                break;
            case 3:
                request_close();
                break;
            }
        };
    }
    m_window_menu->popup(position);
}

void WSWindow::request_close()
{
    WSEvent close_request(WSEvent::WindowCloseRequest);
    event(close_request);
}

void WSWindow::set_fullscreen(bool fullscreen)
{
    if (m_fullscreen == fullscreen)
        return;
    m_fullscreen = fullscreen;
    Rect new_window_rect = m_rect;
    if (m_fullscreen) {
        m_saved_nonfullscreen_rect = m_rect;
        new_window_rect = WSScreen::the().rect();
    } else if (!m_saved_nonfullscreen_rect.is_empty()) {
        new_window_rect = m_saved_nonfullscreen_rect;
    }
    CEventLoop::current().post_event(*this, make<WSResizeEvent>(m_rect, new_window_rect));
    set_rect(new_window_rect);
}

void WSWindow::set_tiled(WindowTileType tiled)
{
    if (m_tiled == tiled)
        return;
    m_tiled = tiled;
    auto old_rect = m_rect;

    auto frame_width = m_frame.rect().width() - m_rect.width();
    switch (tiled) {
        case WindowTileType::None :
        set_rect(m_untiled_rect);
        break;
    case WindowTileType::Left :
        m_untiled_rect = m_rect;
        set_rect(0,
                WSWindowManager::the().maximized_window_rect(*this).y(),
                WSScreen::the().width() / 2,
                WSWindowManager::the().maximized_window_rect(*this).height());
        break;
    case WindowTileType::Right :
        m_untiled_rect = m_rect;
        set_rect(WSScreen::the().width() / 2 + frame_width,
                WSWindowManager::the().maximized_window_rect(*this).y(),
                (WSScreen::the().width() / 2),
                WSWindowManager::the().maximized_window_rect(*this).height());
        break;
    }
    CEventLoop::current().post_event(*this, make<WSResizeEvent>(old_rect, m_rect));
}
