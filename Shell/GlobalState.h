#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
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
    struct termios default_termios;
    bool was_interrupted { false };
    bool was_resized { false };
    int last_return_code { 0 };
    Vector<String> directory_stack;
};

extern GlobalState g;
