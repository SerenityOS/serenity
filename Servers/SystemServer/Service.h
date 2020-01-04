#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

class CConfigFile;

namespace AK {
class JsonObject;
}

class Service final : public CObject {
    C_OBJECT(Service)

public:
    void activate();
    void did_exit(int exit_code);

    static Service* find_by_pid(pid_t);

    void save_to(AK::JsonObject&) override;

private:
    Service(const CConfigFile&, const StringView& name);

    void spawn();

    // Path to the executable. By default this is /bin/{m_name}.
    String m_executable_path;
    // Extra arguments, starting from argv[1], to pass when exec'ing.
    Vector<String> m_extra_arguments;
    // File path to open as stdio fds.
    String m_stdio_file_path;
    int m_priority { 1 };
    // Whether we should re-launch it if it exits.
    bool m_keep_alive { false };
    // Path to the socket to create and listen on on behalf of this service.
    String m_socket_path;
    // Whether we should only spawn this service once somebody connects to the socket.
    bool m_lazy;
    // The name of the user we should run this service as.
    String m_user;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };
    Vector<gid_t> m_extra_gids;

    // PID of the running instance of this service.
    pid_t m_pid { -1 };
    // An open fd to the socket.
    int m_socket_fd { -1 };
    RefPtr<CNotifier> m_socket_notifier;

    void resolve_user();
    void setup_socket();
    void setup_notifier();
};
