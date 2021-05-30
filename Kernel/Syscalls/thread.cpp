/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>

namespace Kernel {

KResultOr<int> Process::sys$create_thread(void* (*entry)(void*), Userspace<const Syscall::SC_create_thread_params*> user_params)
{
    REQUIRE_PROMISE(thread);

    Syscall::SC_create_thread_params params;
    if (!copy_from_user(&params, user_params))
        return EFAULT;

    unsigned detach_state = params.m_detach_state;
    int schedule_priority = params.m_schedule_priority;
    unsigned stack_size = params.m_stack_size;

    auto user_esp = Checked<FlatPtr>((FlatPtr)params.m_stack_location);
    user_esp += stack_size;
    if (user_esp.has_overflow())
        return EOVERFLOW;

    if (!MM.validate_user_stack(*this, VirtualAddress(user_esp.value() - 4)))
        return EFAULT;

    // FIXME: return EAGAIN if Thread::all_threads().size() is greater than PTHREAD_THREADS_MAX

    int requested_thread_priority = schedule_priority;
    if (requested_thread_priority < THREAD_PRIORITY_MIN || requested_thread_priority > THREAD_PRIORITY_MAX)
        return EINVAL;

    bool is_thread_joinable = (0 == detach_state);

    // FIXME: Do something with guard pages?

    auto thread_or_error = Thread::try_create(*this);
    if (thread_or_error.is_error())
        return thread_or_error.error();

    auto& thread = thread_or_error.value();

    // We know this thread is not the main_thread,
    // So give it a unique name until the user calls $set_thread_name on it
    // length + 4 to give space for our extra junk at the end
    StringBuilder builder(m_name.length() + 4);
    thread->set_name(String::formatted("{} [{}]", m_name, thread->tid().value()));

    if (!is_thread_joinable)
        thread->detach();

    auto& tss = thread->tss();
    tss.eip = (FlatPtr)entry;
    tss.eflags = 0x0202;
    tss.cr3 = space().page_directory().cr3();
    tss.esp = user_esp.value();

    auto tsr_result = thread->make_thread_specific_region({});
    if (tsr_result.is_error())
        return tsr_result.error();

    PerformanceManager::add_thread_created_event(*thread);

    ScopedSpinLock lock(g_scheduler_lock);
    thread->set_priority(requested_thread_priority);
    thread->set_state(Thread::State::Runnable);
    return thread->tid().value();
}

void Process::sys$exit_thread(Userspace<void*> exit_value, Userspace<void*> stack_location, size_t stack_size)
{
    REQUIRE_PROMISE(thread);

    if (this->thread_count() == 1) {
        // If this is the last thread, instead kill the process.
        this->sys$exit(0);
    }

    auto current_thread = Thread::current();
    current_thread->set_profiling_suppressed();
    PerformanceManager::add_thread_exit_event(*current_thread);

    if (stack_location) {
        auto unmap_result = space().unmap_mmap_range(VirtualAddress { stack_location }, stack_size);
        if (unmap_result.is_error())
            dbgln("Failed to unmap thread stack, terminating thread anyway. Error code: {}", unmap_result.error());
    }

    current_thread->exit(reinterpret_cast<void*>(exit_value.ptr()));
    VERIFY_NOT_REACHED();
}

KResultOr<int> Process::sys$detach_thread(pid_t tid)
{
    REQUIRE_PROMISE(thread);
    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return ESRCH;

    if (!thread->is_joinable())
        return EINVAL;

    thread->detach();
    return 0;
}

KResultOr<int> Process::sys$join_thread(pid_t tid, Userspace<void**> exit_value)
{
    REQUIRE_PROMISE(thread);

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return ESRCH;

    auto current_thread = Thread::current();
    if (thread == current_thread)
        return EDEADLK;

    void* joinee_exit_value = nullptr;

    // NOTE: pthread_join() cannot be interrupted by signals. Only by death.
    for (;;) {
        KResult try_join_result(KSuccess);
        auto result = current_thread->block<Thread::JoinBlocker>({}, *thread, try_join_result, joinee_exit_value);
        if (result == Thread::BlockResult::NotBlocked) {
            if (try_join_result.is_error())
                return try_join_result.error();
            break;
        }
        if (result == Thread::BlockResult::InterruptedByDeath)
            break;
        dbgln("join_thread: retrying");
    }

    if (exit_value && !copy_to_user(exit_value, &joinee_exit_value))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$set_thread_name(pid_t tid, Userspace<const char*> user_name, size_t user_name_length)
{
    REQUIRE_PROMISE(stdio);
    auto name = copy_string_from_user(user_name, user_name_length);
    if (name.is_null())
        return EFAULT;

    const size_t max_thread_name_size = 64;
    if (name.length() > max_thread_name_size)
        return EINVAL;

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return ESRCH;

    thread->set_name(move(name));
    return 0;
}

KResultOr<int> Process::sys$get_thread_name(pid_t tid, Userspace<char*> buffer, size_t buffer_size)
{
    REQUIRE_PROMISE(thread);
    if (buffer_size == 0)
        return EINVAL;

    auto thread = Thread::from_tid(tid);
    if (!thread || thread->pid() != pid())
        return ESRCH;

    // We must make a temporary copy here to avoid a race with sys$set_thread_name
    auto thread_name = thread->name();
    if (thread_name.length() + 1 > (size_t)buffer_size)
        return ENAMETOOLONG;

    if (!copy_to_user(buffer, thread_name.characters(), thread_name.length() + 1))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$gettid()
{
    REQUIRE_PROMISE(stdio);
    return Thread::current()->tid().value();
}

}
