#include "CUserInfo.h"
#include <pwd.h>
#include <stdlib.h>
#include <unistd.h>

String get_current_user_home_path()
{
    if (auto* home_env = getenv("HOME"))
        return home_env;

    auto* pwd = getpwuid(getuid());
    String path = pwd ? pwd->pw_dir : "/";
    endpwent();
    return path;
}
