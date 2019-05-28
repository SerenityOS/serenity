#pragma once

#include <LibCore/CEventLoop.h>
#include <LibGUI/GEvent.h>
#include <WindowServer/WSAPITypes.h>

class GAction;
class CObject;
class CNotifier;
class GWindow;

class GEventLoop final : public CEventLoop {
public:
    GEventLoop();
    virtual ~GEventLoop() override;

    static GEventLoop& current() { return static_cast<GEventLoop&>(CEventLoop::current()); }

    static bool post_message_to_server(const WSAPI_ClientMessage&, const ByteBuffer& extra_data = {});
    bool wait_for_specific_event(WSAPI_ServerMessage::Type, WSAPI_ServerMessage&);
    WSAPI_ServerMessage sync_request(const WSAPI_ClientMessage& request, WSAPI_ServerMessage::Type response_type);

    static pid_t server_pid() { return s_server_pid; }
    static int my_client_id() { return s_my_client_id; }

    virtual void take_pending_events_from(CEventLoop& other) override
    {
        CEventLoop::take_pending_events_from(other);
        m_unprocessed_bundles.append(move(static_cast<GEventLoop&>(other).m_unprocessed_bundles));
    }

private:
    virtual void add_file_descriptors_for_select(fd_set& fds, int& max_fd_added) override
    {
        FD_SET(s_windowserver_fd, &fds);
        max_fd_added = s_windowserver_fd;
    }

    virtual void process_file_descriptors_after_select(const fd_set& fds) override
    {
        if (FD_ISSET(s_windowserver_fd, &fds))
            drain_messages_from_server();
    }

    virtual void do_processing() override
    {
        while (!m_unprocessed_bundles.is_empty())
            process_unprocessed_bundles();
    }

    void wait_for_event();
    bool drain_messages_from_server();
    void process_unprocessed_bundles();
    void handle_paint_event(const WSAPI_ServerMessage&, GWindow&, const ByteBuffer& extra_data);
    void handle_resize_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_mouse_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_key_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_activation_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_menu_event(const WSAPI_ServerMessage&);
    void handle_window_entered_or_left_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_wm_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_greeting(WSAPI_ServerMessage&);
    void connect_to_server();

    struct IncomingWSMessageBundle {
        WSAPI_ServerMessage message;
        ByteBuffer extra_data;
    };

    Vector<IncomingWSMessageBundle> m_unprocessed_bundles;
    static pid_t s_server_pid;
    static int s_my_client_id;
    static int s_windowserver_fd;
};
