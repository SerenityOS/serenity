#include "CUserInfo.h"
#include <pwd.h>
#include <stdlib.h>
#include <unistd.h>

const char* get_current_user_home_path()
{
    if (auto* home_env = getenv("HOME"))
        return home_env;

    auto d = "/";
    uid_t uid = getuid();
    if (auto* pwd = getpwuid(uid))
        return pwd->pw_dir;

    return d;
}
