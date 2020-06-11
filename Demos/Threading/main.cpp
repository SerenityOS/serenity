/*
 * Copyright (c) 2020, Christopher Joseph Dean Schaefer <disks86@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdio.h>

#include <AK/String.h>
#include <LibThread/ConditionVariable.h>
#include <LibThread/LockGuard.h>
#include <LibThread/Thread.h>

LibThread::ConditionVariable cv;
LibThread::Mutex cv_m;
volatile int i = 0;

int waits()
{
    LibThread::UniqueLock lk(cv_m);
    printf("Waiting... \n");

    cv.wait(lk, [] { return i == 1; });
    printf("...finished waiting. i == 1\n");

    return 0;
}

int signals()
{
    sleep(1);
    {
        LibThread::LockGuard lk(cv_m);
        printf("Notifying...\n");
    }
    cv.notify_all();

    sleep(1);
    {
        LibThread::LockGuard lk(cv_m);
        i = 1;
        printf("Notifying again...\n");
    }
    cv.notify_all();

    return 0;
}

int main()
{
    /*
     * Currently there appears to be a bug in pthread that causes only one thread to be notified.
     * The can be reproduced by adding another wait thread.
     */
    auto wait_thread = LibThread::Thread::construct(waits);
    auto signal_thread = LibThread::Thread::construct(signals);

    wait_thread->start();
    printf("Started Wait Thread\n");

    signal_thread->start();
    printf("Started Signal Thread\n");

    wait_thread->join();
    printf("Joined Wait Thread \n");

    signal_thread->join();
    printf("Joined Signal Thread \n");

    return 0;
}