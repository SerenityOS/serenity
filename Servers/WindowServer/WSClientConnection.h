#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <WindowServer/WSEvent.h>

class WSWindow;
class WSMenu;
class WSMenuBar;
struct WSAPI_ServerMessage;

class WSClientConnection final : public CObject {
public:
    explicit WSClientConnection(int fd);
    virtual ~WSClientConnection() override;

    static WSClientConnection* from_client_id(int client_id);
    static void for_each_client(Function<void(WSClientConnection&)>);

    void post_message(const WSAPI_ServerMessage&, const ByteBuffer& = {});

    int client_id() const { return m_client_id; }
    WSMenuBar* app_menubar() { return m_app_menubar.ptr(); }

    int fd() const { return m_fd; }
    pid_t pid() const { return m_pid; }

    bool is_showing_modal_window() const;

    void set_client_pid(pid_t pid) { m_pid = pid; }

    template<typename Matching, typename Callback>
    void for_each_window_matching(Matching, Callback);
    template<typename Callback>
    void for_each_window(Callback);

    void notify_about_new_screen_rect(const Rect&);
    void post_paint_message(WSWindow&);

    void did_misbehave();

    void on_ready_read();

private:
    virtual void event(CEvent&) override;

    bool handle_message(const WSAPI_ClientMessage& message, ByteBuffer&& extra_data);
    void on_request(const WSAPIClientRequest&);
    void handle_request(const WSAPICreateMenubarRequest&);
    void handle_request(const WSAPIDestroyMenubarRequest&);
    void handle_request(const WSAPICreateMenuRequest&);
    void handle_request(const WSAPIDestroyMenuRequest&);
    void handle_request(const WSAPISetApplicationMenubarRequest&);
    void handle_request(const WSAPIAddMenuToMenubarRequest&);
    void handle_request(const WSAPIAddMenuItemRequest&);
    void handle_request(const WSAPIUpdateMenuItemRequest&);
    void handle_request(const WSAPIAddMenuSeparatorRequest&);
    void handle_request(const WSAPISetWindowTitleRequest&);
    void handle_request(const WSAPIGetWindowTitleRequest&);
    void handle_request(const WSAPISetWindowRectRequest&);
    void handle_request(const WSAPIGetWindowRectRequest&);
    void handle_request(const WSAPISetWindowIconRequest&);
    void handle_request(const WSAPISetClipboardContentsRequest&);
    void handle_request(const WSAPIGetClipboardContentsRequest&);
    void handle_request(const WSAPICreateWindowRequest&);
    void handle_request(const WSAPIDestroyWindowRequest&);
    void handle_request(const WSAPIInvalidateRectRequest&);
    void handle_request(const WSAPIDidFinishPaintingNotification&);
    void handle_request(const WSAPIGetWindowBackingStoreRequest&);
    void handle_request(const WSAPISetWindowBackingStoreRequest&);
    void handle_request(const WSAPISetGlobalCursorTrackingRequest&);
    void handle_request(const WSAPISetWindowOpacityRequest&);
    void handle_request(const WSAPISetWallpaperRequest&);
    void handle_request(const WSAPIGetWallpaperRequest&);
    void handle_request(const WSAPISetWindowOverrideCursorRequest&);
    void handle_request(const WSWMAPISetActiveWindowRequest&);
    void handle_request(const WSWMAPISetWindowMinimizedRequest&);
    void handle_request(const WSWMAPIStartWindowResizeRequest&);
    void handle_request(const WSWMAPIPopupWindowMenuRequest&);
    void handle_request(const WSAPIPopupMenuRequest&);
    void handle_request(const WSAPIDismissMenuRequest&);
    void handle_request(const WSAPISetWindowHasAlphaChannelRequest&);
    void handle_request(const WSAPIMoveWindowToFrontRequest&);

    void post_error(const String&);

    int m_client_id { 0 };
    int m_fd { -1 };
    pid_t m_pid { -1 };

    HashMap<int, OwnPtr<WSWindow>> m_windows;
    HashMap<int, OwnPtr<WSMenuBar>> m_menubars;
    HashMap<int, OwnPtr<WSMenu>> m_menus;
    WeakPtr<WSMenuBar> m_app_menubar;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };

    RefPtr<SharedBuffer> m_last_sent_clipboard_content;
};

template<typename Matching, typename Callback>
void WSClientConnection::for_each_window_matching(Matching matching, Callback callback)
{
    for (auto& it : m_windows) {
        if (matching(*it.value)) {
            if (callback(*it.value) == IterationDecision::Break)
                return;
        }
    }
}

template<typename Callback>
void WSClientConnection::for_each_window(Callback callback)
{
    for (auto& it : m_windows) {
        if (callback(*it.value) == IterationDecision::Break)
            return;
    }
}
