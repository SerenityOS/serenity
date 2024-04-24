/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <signal.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

// Supposed to use volatile everywhere here but good lord does C++ make that a pain
sig_atomic_t volatile saved_signal;
siginfo_t volatile saved_siginfo;
ucontext_t volatile saved_ucontext;
siginfo_t* sig_info_addr;
ucontext_t* ucontext_addr;
void* stack_ptr;
bool volatile signal_was_delivered = false;

static void signal_handler(int sig, siginfo_t* sig_info, void* u_context)
{
    stack_ptr = __builtin_frame_address(0);
    signal_was_delivered = true;

    saved_signal = sig;
    // grumble grumble, assignment operator on volatile types not a thing
    // grumble grumble more, can't memcpy voltile either, that casts away volatile
    // grumble grumble even more, can't std::copy to volatile.
    // screw it, just write all the fields
    sig_info_addr = sig_info;
    saved_siginfo.si_status = sig_info->si_status;
    saved_siginfo.si_signo = sig_info->si_signo;
    saved_siginfo.si_code = sig_info->si_code;
    saved_siginfo.si_pid = sig_info->si_pid;
    saved_siginfo.si_uid = sig_info->si_uid;
    saved_siginfo.si_value.sival_int = sig_info->si_value.sival_int;
    auto user_context = (ucontext_t*)u_context;
    ucontext_addr = user_context;
    saved_ucontext.uc_link = user_context->uc_link;
    saved_ucontext.uc_sigmask = user_context->uc_sigmask;
    saved_ucontext.uc_stack.ss_sp = user_context->uc_stack.ss_sp;
    saved_ucontext.uc_stack.ss_size = user_context->uc_stack.ss_size;
    saved_ucontext.uc_stack.ss_flags = user_context->uc_stack.ss_flags;
    // saved_ucontext.uc_mcontext = user_context->uc_mcontext;
}

static int print_signal_results()
{
    if (!signal_was_delivered) {
        fprintf(stderr, "Where was my signal bro?\n");
        return 2;
    }

    sig_atomic_t read_the_signal = saved_signal;
    siginfo_t read_the_siginfo = {};
    read_the_siginfo.si_status = saved_siginfo.si_status;
    read_the_siginfo.si_signo = saved_siginfo.si_signo;
    read_the_siginfo.si_code = saved_siginfo.si_code;
    read_the_siginfo.si_pid = saved_siginfo.si_pid;
    read_the_siginfo.si_uid = saved_siginfo.si_uid;
    read_the_siginfo.si_value.sival_int = saved_siginfo.si_value.sival_int;

    ucontext_t read_the_ucontext = {};
    read_the_ucontext.uc_link = saved_ucontext.uc_link;
    read_the_ucontext.uc_sigmask = saved_ucontext.uc_sigmask;
    read_the_ucontext.uc_stack.ss_sp = saved_ucontext.uc_stack.ss_sp;
    read_the_ucontext.uc_stack.ss_size = saved_ucontext.uc_stack.ss_size;
    read_the_ucontext.uc_stack.ss_flags = saved_ucontext.uc_stack.ss_flags;
    // read_the_ucontext.uc_mcontext = saved_ucontext.uc_mcontext;

    printf("Handled signal: %d\n", read_the_signal);
    printf("Stack sorta started as %p\n", stack_ptr);
    printf("Siginfo was stored at %p:\n", sig_info_addr);
    printf("\tsi_signo: %d\n", read_the_siginfo.si_signo);
    printf("\tsi_code, %x\n", read_the_siginfo.si_code);
    printf("\tsi_pid, %d\n", read_the_siginfo.si_pid);
    printf("\tsi_uid, %d\n", read_the_siginfo.si_uid);
    printf("\tsi_status, %x\n", read_the_siginfo.si_status);
    printf("\tsi_value.sival_int, %x\n", read_the_siginfo.si_value.sival_int);
    printf("ucontext was stored at %p:\n", ucontext_addr);
    printf("\tuc_link, %p\n", read_the_ucontext.uc_link);
    printf("\tuc_sigmask, %d\n", read_the_ucontext.uc_sigmask);
    printf("\tuc_stack.ss_sp, %p\n", read_the_ucontext.uc_stack.ss_sp);
    printf("\tuc_stack.ss_size, %zu\n", read_the_ucontext.uc_stack.ss_size);
    printf("\tuc_stack.ss_flags, %d\n", read_the_ucontext.uc_stack.ss_flags);
    // printf("\tuc_mcontext, %d\n", read_the_ucontext.uc_mcontext);

    return 0;
}

int main()
{
    struct sigaction action = {};
    action.sa_flags = SA_SIGINFO;
    sigemptyset(&action.sa_mask);
    action.sa_sigaction = signal_handler;

    for (size_t i = 0; i < NSIG; ++i)
        (void)sigaction(i, &action, nullptr);

    printf("Sleeping for a long time waiting for kill -<N> %d\n", getpid());

    sleep(1000);
    return print_signal_results();
}
