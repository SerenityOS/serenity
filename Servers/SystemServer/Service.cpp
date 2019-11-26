#include "Service.h"
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/CConfigFile.h>
#include <fcntl.h>
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

        if (!m_user.is_null()) {
            setuid(m_uid);
            setgid(m_gid);
        }

        char* argv[m_extra_arguments.size() + 2];
        argv[0] = const_cast<char*>(m_executable_path.characters());
        for (int i = 0; i < m_extra_arguments.size(); i++)
            argv[i + 1] = const_cast<char*>(m_extra_arguments[i].characters());
        argv[m_extra_arguments.size() + 1] = nullptr;

        rc = execv(argv[0], argv);
        perror("exec");
        ASSERT_NOT_REACHED();
    }
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
    if (prio == "idle")
        m_priority = 0;
    else if (prio == "low")
        m_priority = 1;
    else if (prio == "normal" || prio.is_null())
        m_priority = 2;
    else if (prio == "high")
        m_priority = 3;
    else
        ASSERT_NOT_REACHED();

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
    json.set("user", m_user);
    json.set("uid", m_uid);
    json.set("gid", m_gid);

    if (m_pid > 0)
        json.set("pid", m_pid);
    else
        json.set("pid", nullptr);
}
