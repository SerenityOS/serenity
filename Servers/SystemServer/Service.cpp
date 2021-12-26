#include "Service.h"
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/CConfigFile.h>
#include <LibCore/CLocalSocket.h>
#include <fcntl.h>
#include <libgen.h>
#include <pwd.h>
#include <sched.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

struct UidAndGid {
    uid_t uid;
    gid_t gid;
};

static HashMap<String, UidAndGid>* s_user_map;
static HashMap<pid_t, Service*> s_service_map;

void Service::resolve_user()
{
    if (s_user_map == nullptr) {
        s_user_map = new HashMap<String, UidAndGid>;
        for (struct passwd* passwd = getpwent(); passwd; passwd = getpwent())
            s_user_map->set(passwd->pw_name, { passwd->pw_uid, passwd->pw_gid });
        endpwent();
    }

    auto user = s_user_map->get(m_user);
    if (!user.has_value()) {
        dbg() << "Failed to resolve user name " << m_user;
        ASSERT_NOT_REACHED();
    }
    m_uid = user.value().uid;
    m_gid = user.value().gid;
}

Service* Service::find_by_pid(pid_t pid)
{
    auto it = s_service_map.find(pid);
    if (it == s_service_map.end())
        return nullptr;
    return (*it).value;
}

static int ensure_parent_directories(const char* path)
{
    ASSERT(path[0] == '/');

    char* parent_buffer = strdup(path);
    const char* parent = dirname(parent_buffer);

    int rc = 0;
    while (true) {
        int rc = mkdir(parent, 0755);

        if (rc == 0)
            break;

        if (errno != ENOENT)
            break;

        ensure_parent_directories(parent);
    };

    free(parent_buffer);
    return rc;
}

void Service::setup_socket()
{
    ASSERT(!m_socket_path.is_null());
    ASSERT(m_socket_fd == -1);

    ensure_parent_directories(m_socket_path.characters());

    // Note: we use SOCK_CLOEXEC here to make sure we don't leak every socket to
    // all the clients. We'll make the one we do need to pass down !CLOEXEC later
    // after forking off the process.
    m_socket_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (m_socket_fd < 0) {
        perror("socket");
        ASSERT_NOT_REACHED();
    }

    auto socket_address = CSocketAddress::local(m_socket_path);
    auto un = socket_address.to_sockaddr_un();
    int rc = bind(m_socket_fd, (const sockaddr*)&un, sizeof(un));
    if (rc < 0) {
        perror("bind");
        ASSERT_NOT_REACHED();
    }

    rc = listen(m_socket_fd, 5);
    if (rc < 0) {
        perror("listen");
        ASSERT_NOT_REACHED();
    }
}

void Service::setup_notifier()
{
    ASSERT(m_lazy);
    ASSERT(m_socket_fd >= 0);
    ASSERT(!m_socket_notifier);

    m_socket_notifier = CNotifier::construct(m_socket_fd, CNotifier::Event::Read, this);
    m_socket_notifier->on_ready_to_read = [this] {
        dbg() << "Ready to read on behalf of " << name();
        remove_child(*m_socket_notifier);
        m_socket_notifier = nullptr;
        spawn();
    };
}

void Service::activate()
{
    ASSERT(m_pid < 0);

    if (m_lazy)
        setup_notifier();
    else
        spawn();
}

void Service::spawn()
{
    dbg() << "Spawning " << name();

    m_pid = fork();

    if (m_pid < 0) {
        perror("fork");
        ASSERT_NOT_REACHED();
    } else if (m_pid == 0) {
        // We are the child.

        struct sched_param p;
        p.sched_priority = m_priority;
        int rc = sched_setparam(0, &p);
        if (rc < 0) {
            perror("sched_setparam");
            ASSERT_NOT_REACHED();
        }

        if (!m_stdio_file_path.is_null()) {
            close(0);
            int fd = open_with_path_length(m_stdio_file_path.characters(), m_stdio_file_path.length(), O_RDWR, 0);
            ASSERT(fd <= 0);
            if (fd < 0) {
                perror("open");
                ASSERT_NOT_REACHED();
            }
            dup2(0, 1);
            dup2(0, 2);
        }

        if (!m_socket_path.is_null()) {
            ASSERT(m_socket_fd > 2);
            dup2(m_socket_fd, 3);
            // The new descriptor is !CLOEXEC here.
            // This is true even if m_socket_fd == 3.
            setenv("SOCKET_TAKEOVER", "1", true);
        }

        if (!m_user.is_null()) {
            if (setgid(m_gid) < 0 || setuid(m_uid) < 0) {
                fprintf(stderr, "Failed to drop privileges (GID=%u, UID=%u)\n", m_gid, m_uid);
                exit(1);
            }
        }

        char* argv[m_extra_arguments.size() + 2];
        argv[0] = const_cast<char*>(m_executable_path.characters());
        for (int i = 0; i < m_extra_arguments.size(); i++)
            argv[i + 1] = const_cast<char*>(m_extra_arguments[i].characters());
        argv[m_extra_arguments.size() + 1] = nullptr;

        rc = execv(argv[0], argv);
        perror("exec");
        ASSERT_NOT_REACHED();
    } else {
        // We are the parent.
        s_service_map.set(m_pid, this);
    }
}

void Service::did_exit(int exit_code)
{
    ASSERT(m_pid > 0);
    (void)exit_code;

    dbg() << "Service " << name() << " has exited";

    s_service_map.remove(m_pid);
    m_pid = -1;

    if (m_keep_alive)
        activate();
}

Service::Service(const CConfigFile& config, const StringView& name)
    : CObject(nullptr)
{
    ASSERT(config.has_group(name));

    set_name(name);
    m_executable_path = config.read_entry(name, "Executable", String::format("/bin/%s", this->name().characters()));
    m_extra_arguments = config.read_entry(name, "Arguments", "").split(' ');
    m_stdio_file_path = config.read_entry(name, "StdIO");

    String prio = config.read_entry(name, "Priority");
    if (prio == "low")
        m_priority = 10;
    else if (prio == "normal" || prio.is_null())
        m_priority = 30;
    else if (prio == "high")
        m_priority = 50;
    else
        ASSERT_NOT_REACHED();

    m_keep_alive = config.read_bool_entry(name, "KeepAlive");
    m_lazy = config.read_bool_entry(name, "Lazy");

    m_socket_path = config.read_entry(name, "Socket");
    if (!m_socket_path.is_null()) {
        setup_socket();
    }

    m_user = config.read_entry(name, "User");
    if (!m_user.is_null())
        resolve_user();
}

void Service::save_to(JsonObject& json)
{
    CObject::save_to(json);

    json.set("executable_path", m_executable_path);

    // FIXME: This crashes Inspector.
    /*
    JsonArray extra_args;
    for (String& arg : m_extra_arguments)
        extra_args.append(arg);
    json.set("extra_arguments", move(extra_args));
    */

    json.set("stdio_file_path", m_stdio_file_path);
    json.set("priority", m_priority);
    json.set("keep_alive", m_keep_alive);
    json.set("socket_path", m_socket_path);
    json.set("lazy", m_lazy);
    json.set("user", m_user);
    json.set("uid", m_uid);
    json.set("gid", m_gid);

    if (m_pid > 0)
        json.set("pid", m_pid);
    else
        json.set("pid", nullptr);
}
