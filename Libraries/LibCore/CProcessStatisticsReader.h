#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <unistd.h>

struct CThreadStatistics {
    int tid;
    unsigned times_scheduled;
    unsigned ticks;
    unsigned syscall_count;
    unsigned inode_faults;
    unsigned zero_faults;
    unsigned cow_faults;
    unsigned unix_socket_read_bytes;
    unsigned unix_socket_write_bytes;
    unsigned ipv4_socket_read_bytes;
    unsigned ipv4_socket_write_bytes;
    unsigned file_read_bytes;
    unsigned file_write_bytes;
    String state;
    String priority;
    String name;
};

struct CProcessStatistics {
    // Keep this in sync with /proc/all.
    // From the kernel side:
    pid_t pid;
    unsigned pgid;
    unsigned pgp;
    unsigned sid;
    uid_t uid;
    gid_t gid;
    pid_t ppid;
    unsigned nfds;
    String name;
    String tty;
    size_t amount_virtual;
    size_t amount_resident;
    size_t amount_shared;
    int icon_id;

    Vector<CThreadStatistics> threads;

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
