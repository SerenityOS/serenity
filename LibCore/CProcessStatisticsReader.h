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
    CProcessStatisticsReader();
    HashMap<pid_t, CProcessStatistics> get_map();

private:
    void update_map(HashMap<pid_t, CProcessStatistics>& map);
    String get_username_from_uid(const uid_t uid);

    HashMap<uid_t, String> m_usernames;
};
