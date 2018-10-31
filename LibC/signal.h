#pragma once

#include "types.h"

extern "C" {

int kill(pid_t, int sig);

#define SIGHUP    1
#define SIGINT    2
#define SIGQUIT   3 
#define SIGILL    4 
#define SIGTRAP   5
#define SIGABRT   6 
#define SIGBUS    7
#define SIGFPE    8
#define SIGKILL   9
#define SIGUSR1  10
#define SIGSEGV  11
#define SIGUSR2  12
#define SIGPIPE  13
#define SIGALRM  14
#define SIGTERM  15

}

