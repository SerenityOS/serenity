#pragma once

#include <AK/AKString.h>
#include <termios.h>

struct GlobalState {
    String cwd;
    String username;
    String home;
    char ttyname[32];
    char hostname[32];
    pid_t sid;
    uid_t uid;
    struct termios termios;
    bool was_interrupted { false };
    bool was_resized { false };
};

extern GlobalState g;
