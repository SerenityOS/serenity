#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/RetainPtr.h>

class CProcessInfo : public Retainable<CProcessInfo> {
public:
    pid_t pid;
    unsigned nsched;
    String name;
    String state;
    String username;
    uid_t uid;
    String priority;
    size_t linear;
    size_t physical;
    unsigned syscalls;
};

class CProcessHelper {
public:
    CProcessHelper();
    HashMap<pid_t, RetainPtr<CProcessInfo>> get_map();

private:
    int update_map(HashMap<pid_t, RetainPtr<CProcessInfo>>& map);
    String get_username_from_uid(const uid_t uid);
  
    HashMap<uid_t, String> m_usernames;
};
