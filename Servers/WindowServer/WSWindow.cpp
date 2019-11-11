#include "WSWindow.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSScreen.h"
#include "WSWindowManager.h"
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSClientConnection.h>

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
        m_wm_event_mask = WSAPI_WMEventMask::WindowStateChanges | WSAPI_WMEventMask::WindowRemovals | WSAPI_WMEventMask::WindowIconChanges;
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

// FIXME: Just use the same types.
static WSAPI_MouseButton to_api(MouseButton button)
{
    switch (button) {
    case MouseButton::None:
        return WSAPI_MouseButton::NoButton;
    case MouseButton::Left:
        return WSAPI_MouseButton::Left;
    case MouseButton::Right:
        return WSAPI_MouseButton::Right;
    case MouseButton::Middle:
        return WSAPI_MouseButton::Middle;
    }
    ASSERT_NOT_REACHED();
}

void WSWindow::handle_mouse_event(const WSMouseEvent& event)
{
    set_automatic_cursor_tracking_enabled(event.buttons() != 0);

    WSAPI_ServerMessage server_message;
    server_message.window_id = window_id();

    switch (event.type()) {
    case WSEvent::MouseMove:
        server_message.type = WSAPI_ServerMessage::Type::MouseMove;
        break;
    case WSEvent::MouseDown:
        server_message.type = WSAPI_ServerMessage::Type::MouseDown;
        break;
    case WSEvent::MouseDoubleClick:
        server_message.type = WSAPI_ServerMessage::Type::MouseDoubleClick;
        break;
    case WSEvent::MouseUp:
        server_message.type = WSAPI_ServerMessage::Type::MouseUp;
        break;
    case WSEvent::MouseWheel:
        server_message.type = WSAPI_ServerMessage::Type::MouseWheel;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    server_message.mouse.position = event.position();
    server_message.mouse.button = to_api(event.button());
    server_message.mouse.buttons = event.buttons();
    server_message.mouse.modifiers = event.modifiers();
    server_message.mouse.wheel_delta = event.wheel_delta();

    m_client->post_message(server_message);
}

static WSAPI_WindowType to_api(WSWindowType ws_type)
{
    switch (ws_type) {
    case WSWindowType::Normal:
        return WSAPI_WindowType::Normal;
    case WSWindowType::Menu:
        return WSAPI_WindowType::Menu;
    case WSWindowType::WindowSwitcher:
        return WSAPI_WindowType::WindowSwitcher;
    case WSWindowType::Taskbar:
        return WSAPI_WindowType::Taskbar;
    case WSWindowType::Tooltip:
        return WSAPI_WindowType::Tooltip;
    case WSWindowType::Menubar:
        return WSAPI_WindowType::Menubar;
    default:
        ASSERT_NOT_REACHED();
    }
}

void WSWindow::set_minimized(bool minimized)
{
    if (m_minimized == minimized)
        return;
    m_minimized = minimized;
    if (!minimized)
        request_update({ {}, size() });
    invalidate();
    WSWindowManager::the().notify_minimization_state_changed(*this);
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

    WSAPI_ServerMessage server_message;
    server_message.window_id = window_id();

    if (static_cast<WSEvent&>(event).is_mouse_event())
        return handle_mouse_event(static_cast<const WSMouseEvent&>(event));

    switch (event.type()) {
    case WSEvent::WindowEntered:
        server_message.type = WSAPI_ServerMessage::Type::WindowEntered;
        break;
    case WSEvent::WindowLeft:
        server_message.type = WSAPI_ServerMessage::Type::WindowLeft;
        break;
    case WSEvent::KeyDown:
        server_message.type = WSAPI_ServerMessage::Type::KeyDown;
        server_message.key.character = static_cast<const WSKeyEvent&>(event).character();
        server_message.key.key = static_cast<const WSKeyEvent&>(event).key();
        server_message.key.modifiers = static_cast<const WSKeyEvent&>(event).modifiers();
        break;
    case WSEvent::KeyUp:
        server_message.type = WSAPI_ServerMessage::Type::KeyUp;
        server_message.key.character = static_cast<const WSKeyEvent&>(event).character();
        server_message.key.key = static_cast<const WSKeyEvent&>(event).key();
        server_message.key.modifiers = static_cast<const WSKeyEvent&>(event).modifiers();
        break;
    case WSEvent::WindowActivated:
        server_message.type = WSAPI_ServerMessage::Type::WindowActivated;
        break;
    case WSEvent::WindowDeactivated:
        server_message.type = WSAPI_ServerMessage::Type::WindowDeactivated;
        break;
    case WSEvent::WindowCloseRequest:
        server_message.type = WSAPI_ServerMessage::Type::WindowCloseRequest;
        break;
    case WSEvent::WindowResized:
        server_message.type = WSAPI_ServerMessage::Type::WindowResized;
        server_message.window.old_rect = static_cast<const WSResizeEvent&>(event).old_rect();
        server_message.window.rect = static_cast<const WSResizeEvent&>(event).rect();
        break;
    case WSEvent::WM_WindowRemoved: {
        auto& removed_event = static_cast<const WSWMWindowRemovedEvent&>(event);
        server_message.type = WSAPI_ServerMessage::Type::WM_WindowRemoved;
        server_message.wm.client_id = removed_event.client_id();
        server_message.wm.window_id = removed_event.window_id();
        break;
    }
    case WSEvent::WM_WindowStateChanged: {
        auto& changed_event = static_cast<const WSWMWindowStateChangedEvent&>(event);
        server_message.type = WSAPI_ServerMessage::Type::WM_WindowStateChanged;
        server_message.wm.client_id = changed_event.client_id();
        server_message.wm.window_id = changed_event.window_id();
        server_message.wm.is_active = changed_event.is_active();
        server_message.wm.is_minimized = changed_event.is_minimized();
        server_message.wm.window_type = to_api(changed_event.window_type());
        ASSERT(changed_event.title().length() < (int)sizeof(server_message.text));
        memcpy(server_message.text, changed_event.title().characters(), changed_event.title().length());
        server_message.text_length = changed_event.title().length();
        server_message.wm.rect = changed_event.rect();
        break;
    }

    case WSEvent::WM_WindowIconBitmapChanged: {
        auto& changed_event = static_cast<const WSWMWindowIconBitmapChangedEvent&>(event);
        server_message.type = WSAPI_ServerMessage::Type::WM_WindowIconBitmapChanged;
        server_message.wm.client_id = changed_event.client_id();
        server_message.wm.window_id = changed_event.window_id();
        server_message.wm.icon_buffer_id = changed_event.icon_buffer_id();
        server_message.wm.icon_size = changed_event.icon_size();

        // FIXME: Perhaps we should update the bitmap sharing list somewhere else instead?
        ASSERT(client());
        dbg() << "WindowServer: Sharing icon buffer " << changed_event.icon_buffer_id() << " with PID " << client()->client_pid();
        if (share_buffer_with(changed_event.icon_buffer_id(), client()->client_pid()) < 0) {
            ASSERT_NOT_REACHED();
        }
        break;
    }

    case WSEvent::WM_WindowRectChanged: {
        auto& changed_event = static_cast<const WSWMWindowRectChangedEvent&>(event);
        server_message.type = WSAPI_ServerMessage::Type::WM_WindowRectChanged;
        server_message.wm.client_id = changed_event.client_id();
        server_message.wm.window_id = changed_event.window_id();
        server_message.wm.rect = changed_event.rect();
        break;
    }

    default:
        break;
    }

    if (server_message.type == WSAPI_ServerMessage::Type::Invalid)
        return;

    m_client->post_message(server_message);
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
