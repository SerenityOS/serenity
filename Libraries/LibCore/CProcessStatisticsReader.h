#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>

struct CProcessStatistics {
    pid_t pid;
    unsigned nsched;
    String name;
    String state;
    String username;
    uid_t uid;
    String priority;
    size_t virtual_size;
    size_t physical_size;
    unsigned syscalls;
};

class CProcessStatisticsReader {
public:
    static HashMap<pid_t, CProcessStatistics> get_all();

private:
    static String username_from_uid(uid_t);
    static HashMap<uid_t, String> s_usernames;
};
