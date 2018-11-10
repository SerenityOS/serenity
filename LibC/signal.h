#pragma once

#include <sys/types.h>
#include <signal_numbers.h>

__BEGIN_DECLS

typedef void (*__sighandler_t)(int);
typedef __sighandler_t sighandler_t;

typedef uint32_t sigset_t;
typedef void siginfo_t;

struct sigaction {
    union {
        void (*sa_handler)(int);
        void (*sa_sigaction)(int, siginfo_t*, void*);
    };
    sigset_t sa_mask;
    int sa_flags;
    void (*sa_restorer)(void);
};

int kill(pid_t, int sig);
sighandler_t signal(int sig, sighandler_t);
int sigaction(int sig, const struct sigaction* act, struct sigaction* old_act);
int sigemptyset(sigset_t*);
int sigfillset(sigset_t*);
int sigaddset(sigset_t*, int sig);
int sigdelset(sigset_t*, int sig);
int sigismember(const sigset_t*, int sig);
int sigprocmask(int how, const sigset_t* set, sigset_t* old_set);
int sigpending(sigset_t*);

#define NSIG 32
extern const char* sys_siglist[NSIG];

#define SIG_DFL ((__sighandler_t)0)
#define SIG_ERR ((__sighandler_t)-1)
#define SIG_IGN ((__sighandler_t)1)

#define SA_NOCLDSTOP 1
#define SA_NOCLDWAIT 2
#define SA_SIGINFO 4

#define SIG_BLOCK 0
#define SIG_UNBLOCK 1
#define SIG_SETMASK 2

__END_DECLS

