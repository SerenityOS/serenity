/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Assertions.h>
#include <semaphore.h>

int sem_close(sem_t*)
{
    VERIFY_NOT_REACHED();
}
int sem_destroy(sem_t*)
{
    VERIFY_NOT_REACHED();
}
int sem_getvalue(sem_t*, int*)
{
    VERIFY_NOT_REACHED();
}
int sem_init(sem_t*, int, unsigned int)
{
    VERIFY_NOT_REACHED();
}
sem_t* sem_open(const char*, int, ...)
{
    VERIFY_NOT_REACHED();
}
int sem_post(sem_t*)
{
    VERIFY_NOT_REACHED();
}
int sem_trywait(sem_t*)
{
    VERIFY_NOT_REACHED();
}
int sem_unlink(const char*)
{
    VERIFY_NOT_REACHED();
}
int sem_wait(sem_t*)
{
    VERIFY_NOT_REACHED();
}
