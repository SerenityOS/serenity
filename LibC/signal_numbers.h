#pragma once

#define __ENUMERATE_ALL_SIGNALS \
    __SIGNAL(SIGINVAL,  "Invalid signal number") \
    __SIGNAL(SIGHUP,    "Hangup") \
    __SIGNAL(SIGINT,    "Interrupt") \
    __SIGNAL(SIGQUIT,   "Quit") \
    __SIGNAL(SIGILL,    "Illegal instruction") \
    __SIGNAL(SIGTRAP,   "Trap") \
    __SIGNAL(SIGABRT,   "Aborted") \
    __SIGNAL(SIGBUS,    "Bus error") \
    __SIGNAL(SIGFPE,    "FP exception") \
    __SIGNAL(SIGKILL,   "Killed") \
    __SIGNAL(SIGUSR1,   "User signal 1") \
    __SIGNAL(SIGSEGV,   "Segmentation violation") \
    __SIGNAL(SIGUSR2,   "User signal 2") \
    __SIGNAL(SIGPIPE,   "Broken pipe") \
    __SIGNAL(SIGALRM,   "Alarm clock") \
    __SIGNAL(SIGTERM,   "Terminated") \
    __SIGNAL(SIGSTKFLT, "Stack fault") \
    __SIGNAL(SIGCHLD,   "Child exited") \
    __SIGNAL(SIGCONT,   "Continued") \
    __SIGNAL(SIGSTOP,   "Stopped (signal)") \
    __SIGNAL(SIGTSTP,   "Stopped") \
    __SIGNAL(SIGTTIN,   "Stopped (tty input)") \
    __SIGNAL(SIGTTOU,   "Stopped (tty output)") \
    __SIGNAL(SIGURG,    "Urgent I/O condition)") \
    __SIGNAL(SIGXCPU,   "CPU limit exceeded") \
    __SIGNAL(SIGXFSZ,   "File size limit exceeded") \
    __SIGNAL(SIGVTALRM, "Virtual timer expired") \
    __SIGNAL(SIGPROF,   "Profiling timer expired") \
    __SIGNAL(SIGWINCH,  "Window changed") \
    __SIGNAL(SIGIO,     "I/O possible") \
    __SIGNAL(SIGPWR,    "Power failure") \
    __SIGNAL(SIGSYS,    "Bad system call") \


enum __signal_numbers {
#undef __SIGNAL
#define __SIGNAL(a, b) a,
    __ENUMERATE_ALL_SIGNALS
#undef __SIGNAL
    __signal_count
};
