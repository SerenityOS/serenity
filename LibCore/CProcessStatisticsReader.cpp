#include "CProcessStatisticsReader.h"
#include "CFile.h"

#include <pwd.h>
#include <stdio.h>

CProcessStatisticsReader::CProcessStatisticsReader()
{
    setpwent();
    while (auto* passwd = getpwent())
        m_usernames.set(passwd->pw_uid, passwd->pw_name);
    endpwent();
}

HashMap<pid_t, CProcessStatistics> CProcessStatisticsReader::get_map()
{
    HashMap<pid_t, CProcessStatistics> res;
    update_map(res);
    return res;
}

void CProcessStatisticsReader::update_map(HashMap<pid_t, CProcessStatistics>& map)
{
    CFile file("/proc/all");
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "CProcessHelper : failed to open /proc/all: %s\n", file.error_string());
        return;
    }

    for (;;) {
        auto line = file.read_line(1024);

        if (line.is_empty())
            break;

        auto chomped = String((const char*)line.pointer(), line.size() - 1, Chomp);
        auto parts = chomped.split_view(',');

        if (parts.size() < 18)
            break;

        bool ok = false;
        CProcessStatistics process;

        process.pid = parts[0].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid pid\n", parts[0].characters());
            return;
        }

        process.nsched = parts[1].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid nsched value\n", parts[1].characters());
            return;
        }

        uid_t uid = parts[5].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid uid value\n", parts[5].characters());
            return;
        }

        process.uid = uid;
        process.username = get_username_from_uid(uid);

        process.priority = parts[16];

        process.syscalls = parts[17].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid syscalls count value\n", parts[17].characters());
            return;
        }

        process.state = parts[7];

        process.name = parts[11];
        process.linear = parts[12].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid amount of linear address space used\n", parts[12].characters());
            return;
        }

        process.physical = parts[13].to_uint(ok);
        if (!ok) {
            fprintf(stderr, "CProcessHelper : couldn't convert %s to a valid amount of physical address space used\n", parts[13].characters());
            return;
        }

        map.set(process.pid, process);
    }
}

String CProcessStatisticsReader::get_username_from_uid(const uid_t uid)
{
    auto it = m_usernames.find(uid);
    if (it != m_usernames.end())
        return (*it).value;
    else
        return String::format("%u", uid);
}
