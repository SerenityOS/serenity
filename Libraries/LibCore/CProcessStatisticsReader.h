#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>

struct CProcessStatistics {
    // Keep this in sync with /proc/all.
    // From the kernel side:
    pid_t pid;
    unsigned times_scheduled;
    unsigned pgid;
    unsigned pgp;
    unsigned sid;
    uid_t uid;
    gid_t gid;
    String state;
    pid_t ppid;
    unsigned nfds;
    String name;
    String tty;
    size_t amount_virtual;
    size_t amount_resident;
    size_t amount_shared;
    unsigned ticks;
    String priority;
    unsigned syscall_count;
    int icon_id;

    // synthetic
    String username;
};

class CProcessStatisticsReader {
public:
    static HashMap<pid_t, CProcessStatistics> get_all();

private:
    static String username_from_uid(uid_t);
    static HashMap<uid_t, String> s_usernames;
};
