/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

namespace Kernel {

int Process::sys$create_thread(void* (*entry)(void*), Userspace<const Syscall::SC_create_thread_params*> user_params)
{
    REQUIRE_PROMISE(thread);

    Syscall::SC_create_thread_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    unsigned detach_state = params.m_detach_state;
    int schedule_priority = params.m_schedule_priority;
    unsigned stack_size = params.m_stack_size;

    auto user_stack_address = (u8*)params.m_stack_location + stack_size;

    if (!MM.validate_user_stack(*this, VirtualAddress(user_stack_address - 4)))
        return -EFAULT;

    // FIXME: return EAGAIN if Thread::all_threads().size() is greater than PTHREAD_THREADS_MAX

    int requested_thread_priority = schedule_priority;
    if (requested_thread_priority < THREAD_PRIORITY_MIN || requested_thread_priority > THREAD_PRIORITY_MAX)
        return -EINVAL;

    bool is_thread_joinable = (0 == detach_state);

    // FIXME: Do something with guard pages?

    auto* thread = new Thread(*this);

    // We know this thread is not the main_thread,
    // So give it a unique name until the user calls $set_thread_name on it
    // length + 4 to give space for our extra junk at the end
    StringBuilder builder(m_name.length() + 4);
    builder.append(m_name);
    builder.appendf("[%d]", thread->tid().value());
    thread->set_name(builder.to_string());

    thread->set_priority(requested_thread_priority);
    if (!is_thread_joinable)
        thread->detach();

    auto& tss = thread->tss();
    tss.eip = (FlatPtr)entry;
    tss.eflags = 0x0202;
    tss.cr3 = page_directory().cr3();
    tss.esp = (u32)user_stack_address;

    auto tsr_result = thread->make_thread_specific_region({});
    if (tsr_result.is_error())
        return tsr_result.error();
    thread->set_state(Thread::State::Runnable);
    return thread->tid().value();
}

void Process::sys$exit_thread(Userspace<void*> exit_value)
{
    REQUIRE_PROMISE(thread);
    cli();
    auto current_thread = Thread::current();
    current_thread->m_exit_value = reinterpret_cast<void*>(exit_value.ptr());
    current_thread->set_should_die();
    big_lock().force_unlock_if_locked();
    current_thread->die_if_needed();
    ASSERT_NOT_REACHED();
}

int Process::sys$detach_thread(pid_t tid)
{
    REQUIRE_PROMISE(thread);
    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    if (!thread->is_joinable())
        return -EINVAL;

    thread->detach();
    return 0;
}

int Process::sys$join_thread(pid_t tid, Userspace<void**> exit_value)
{
    REQUIRE_PROMISE(thread);

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    auto current_thread = Thread::current();
    if (thread == current_thread)
        return -EDEADLK;

    void* joinee_exit_value = nullptr;

    // NOTE: pthread_join() cannot be interrupted by signals. Only by death.
    for (;;) {
        KResult try_join_result(KSuccess);
        auto result = current_thread->block<Thread::JoinBlocker>(nullptr, *thread, try_join_result, joinee_exit_value);
        if (result == Thread::BlockResult::NotBlocked) {
            if (try_join_result.is_error())
                return try_join_result.error();
            break;
        }
        if (result == Thread::BlockResult::InterruptedByDeath)
            return 0; // we're not going to return back to user mode
    }

    if (exit_value && !copy_to_user(exit_value, &joinee_exit_value))
        return -EFAULT;
    return 0;
}

int Process::sys$set_thread_name(pid_t tid, Userspace<const char*> user_name, size_t user_name_length)
{
    REQUIRE_PROMISE(thread);
    auto name = copy_string_from_user(user_name, user_name_length);
    if (name.is_null())
        return -EFAULT;

    const size_t max_thread_name_size = 64;
    if (name.length() > max_thread_name_size)
        return -EINVAL;

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    thread->set_name(move(name));
    return 0;
}

int Process::sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size)
{
    REQUIRE_PROMISE(thread);
    if (buffer_size == 0)
        return -EINVAL;

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return -ESRCH;

    // We must make a temporary copy here to avoid a race with sys$set_thread_name
    auto thread_name = thread->name();
    if (thread_name.length() + 1 > (size_t)buffer_size)
        return -ENAMETOOLONG;

    if (!copy_to_user(buffer, thread_name.characters(), thread_name.length() + 1))
        return -EFAULT;
    return 0;
}

int Process::sys$gettid()
{
    REQUIRE_PROMISE(stdio);
    return Thread::current()->tid().value();
}

}
