#pragma once

#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/CObject.h>

class CConfigFile;

namespace AK {
class JsonObject;
}

class Service final : public CObject {
    C_OBJECT(Service)

public:
    void spawn();
    void did_exit(int exit_code);

    static Service* find_by_pid(pid_t);

    void save_to(AK::JsonObject&) override;

private:
    Service(const CConfigFile&, const StringView& name);

    // Path to the executable. By default this is /bin/{m_name}.
    String m_executable_path;
    // Extra arguments, starting from argv[1], to pass when exec'ing.
    Vector<String> m_extra_arguments;
    // File path to open as stdio fds.
    String m_stdio_file_path;
    int m_priority { 1 };
    // Whether we should re-launch it if it exits.
    bool m_keep_alive { false };
    // The name of the user we should run this service as.
    String m_user;
    uid_t m_uid { 0 };
    gid_t m_gid { 0 };

    // PID of the running instance of this service.
    pid_t m_pid { -1 };

    void resolve_user();
};
