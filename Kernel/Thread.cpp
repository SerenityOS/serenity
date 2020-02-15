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

#include <AK/Demangle.h>
#include <AK/StringBuilder.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/signal_numbers.h>
#include <LibELF/ELFLoader.h>

//#define SIGNAL_DEBUG
//#define THREAD_DEBUG

static FPUState s_clean_fpu_state;

u16 thread_specific_selector()
{
    static u16 selector;
    if (!selector) {
        selector = gdt_alloc_entry();
        auto& descriptor = get_gdt_entry(selector);
        descriptor.dpl = 3;
        descriptor.segment_present = 1;
        descriptor.granularity = 0;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 1;
        descriptor.type = 2;
    }
    return selector;
}

Descriptor& thread_specific_descriptor()
{
    return get_gdt_entry(thread_specific_selector());
}

HashTable<Thread*>& thread_table()
{
    ASSERT_INTERRUPTS_DISABLED();
    static HashTable<Thread*>* table;
    if (!table)
        table = new HashTable<Thread*>;
    return *table;
}

Thread::Thread(Process& process)
    : m_process(process)
    , m_name(process.name())
{
    if (m_process.m_thread_count == 0) {
        // First thread gets TID == PID
        m_tid = process.pid();
    } else {
        m_tid = Process::allocate_pid();
    }
    process.m_thread_count++;
#ifdef THREAD_DEBUG
    dbg() << "Created new thread " << process.name() << "(" << process.pid() << ":" << m_tid << ")";
#endif
    set_default_signal_dispositions();
    m_fpu_state = (FPUState*)kmalloc_aligned(sizeof(FPUState), 16);
    memcpy(m_fpu_state, &s_clean_fpu_state, sizeof(FPUState));
    memset(&m_tss, 0, sizeof(m_tss));
    m_tss.iomapbase = sizeof(TSS32);

    // Only IF is set when a process boots.
    m_tss.eflags = 0x0202;
    u16 cs, ds, ss, gs;

    if (m_process.is_ring0()) {
        cs = 0x08;
        ds = 0x10;
        ss = 0x10;
        gs = 0;
    } else {
        cs = 0x1b;
        ds = 0x23;
        ss = 0x23;
        gs = thread_specific_selector() | 3;
    }

    m_tss.ds = ds;
    m_tss.es = ds;
    m_tss.fs = ds;
    m_tss.gs = gs;
    m_tss.ss = ss;
    m_tss.cs = cs;

    m_tss.cr3 = m_process.page_directory().cr3();

    m_kernel_stack_region = MM.allocate_kernel_region(default_kernel_stack_size, String::format("Kernel Stack (Thread %d)", m_tid), Region::Access::Read | Region::Access::Write, false, true);
    m_kernel_stack_region->set_stack(true);
    m_kernel_stack_base = m_kernel_stack_region->vaddr().get();
    m_kernel_stack_top = m_kernel_stack_region->vaddr().offset(default_kernel_stack_size).get() & 0xfffffff8u;

    if (m_process.is_ring0()) {
        m_tss.esp = m_kernel_stack_top;
    } else {
        // Ring 3 processes get a separate stack for ring 0.
        // The ring 3 stack will be assigned by exec().
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_kernel_stack_top;
    }

    if (m_process.pid() != 0) {
        InterruptDisabler disabler;
        thread_table().set(this);
        Scheduler::init_thread(*this);
    }
}

Thread::~Thread()
{
    kfree_aligned(m_fpu_state);
    {
        InterruptDisabler disabler;
        thread_table().remove(this);
    }

    if (selector())
        gdt_free_entry(selector());

    ASSERT(m_process.m_thread_count);
    m_process.m_thread_count--;
}

void Thread::unblock()
{
    if (current == this) {
        set_state(Thread::Running);
        return;
    }
    ASSERT(m_state != Thread::Runnable && m_state != Thread::Running);
    set_state(Thread::Runnable);
}

void Thread::set_should_die()
{
    if (m_should_die) {
#ifdef THREAD_DEBUG
        dbg() << *this << " Should already die";
#endif
        return;
    }
    InterruptDisabler disabler;

    // Remember that we should die instead of returning to
    // the userspace.
    m_should_die = true;

    if (is_blocked()) {
        ASSERT(in_kernel());
        ASSERT(m_blocker != nullptr);
        // We're blocked in the kernel.
        m_blocker->set_interrupted_by_death();
        unblock();
    } else if (!in_kernel()) {
        // We're executing in userspace (and we're clearly
        // not the current thread). No need to unwind, so
        // set the state to dying right away. This also
        // makes sure we won't be scheduled anymore.
        set_state(Thread::State::Dying);
    }
}

void Thread::die_if_needed()
{
    ASSERT(current == this);

    if (!m_should_die)
        return;

    unlock_process_if_locked();

    InterruptDisabler disabler;
    set_state(Thread::State::Dying);

    if (!Scheduler::is_active())
        Scheduler::pick_next_and_switch_now();
}

void Thread::yield_without_holding_big_lock()
{
    bool did_unlock = unlock_process_if_locked();
    Scheduler::yield();
    if (did_unlock)
        relock_process();
}

bool Thread::unlock_process_if_locked()
{
    return process().big_lock().force_unlock_if_locked();
}

void Thread::relock_process()
{
    process().big_lock().lock();
}

u64 Thread::sleep(u32 ticks)
{
    ASSERT(state() == Thread::Running);
    u64 wakeup_time = g_uptime + ticks;
    auto ret = current->block<Thread::SleepBlocker>(wakeup_time);
    if (wakeup_time > g_uptime) {
        ASSERT(ret != Thread::BlockResult::WokeNormally);
    }
    return wakeup_time;
}

u64 Thread::sleep_until(u64 wakeup_time)
{
    ASSERT(state() == Thread::Running);
    auto ret = current->block<Thread::SleepBlocker>(wakeup_time);
    if (wakeup_time > g_uptime)
        ASSERT(ret != Thread::BlockResult::WokeNormally);
    return wakeup_time;
}

const char* Thread::state_string() const
{
    switch (state()) {
    case Thread::Invalid:
        return "Invalid";
    case Thread::Runnable:
        return "Runnable";
    case Thread::Running:
        return "Running";
    case Thread::Dying:
        return "Dying";
    case Thread::Dead:
        return "Dead";
    case Thread::Stopped:
        return "Stopped";
    case Thread::Skip1SchedulerPass:
        return "Skip1";
    case Thread::Skip0SchedulerPasses:
        return "Skip0";
    case Thread::Queued:
        return "Queued";
    case Thread::Blocked:
        ASSERT(m_blocker != nullptr);
        return m_blocker->state_string();
    }
    kprintf("Thread::state_string(): Invalid state: %u\n", state());
    ASSERT_NOT_REACHED();
    return nullptr;
}

void Thread::finalize()
{
    ASSERT(current == g_finalizer);

#ifdef THREAD_DEBUG
    dbg() << "Finalizing thread " << *this;
#endif
    set_state(Thread::State::Dead);

    if (m_joiner) {
        ASSERT(m_joiner->m_joinee == this);
        static_cast<JoinBlocker*>(m_joiner->m_blocker)->set_joinee_exit_value(m_exit_value);
        m_joiner->m_joinee = nullptr;
        // NOTE: We clear the joiner pointer here as well, to be tidy.
        m_joiner = nullptr;
    }

    if (m_dump_backtrace_on_finalization)
        dbg() << backtrace_impl();
}

void Thread::finalize_dying_threads()
{
    ASSERT(current == g_finalizer);
    Vector<Thread*, 32> dying_threads;
    {
        InterruptDisabler disabler;
        for_each_in_state(Thread::State::Dying, [&](Thread& thread) {
            dying_threads.append(&thread);
            return IterationDecision::Continue;
        });
    }
    for (auto* thread : dying_threads) {
        auto& process = thread->process();
        thread->finalize();
        delete thread;
        if (process.m_thread_count == 0)
            process.finalize();
    }
}

bool Thread::tick()
{
    ++m_ticks;
    if (tss().cs & 3)
        ++m_process.m_ticks_in_user;
    else
        ++m_process.m_ticks_in_kernel;
    return --m_ticks_left;
}

void Thread::send_signal(u8 signal, [[maybe_unused]] Process* sender)
{
    ASSERT(signal < 32);
    InterruptDisabler disabler;

    // FIXME: Figure out what to do for masked signals. Should we also ignore them here?
    if (should_ignore_signal(signal)) {
#ifdef SIGNAL_DEBUG
        dbg() << "signal " << signal << " was ignored by " << process();
#endif
        return;
    }

#ifdef SIGNAL_DEBUG
    if (sender)
        dbgprintf("signal: %s(%u) sent %d to %s(%u)\n", sender->name().characters(), sender->pid(), signal, process().name().characters(), pid());
    else
        dbgprintf("signal: kernel sent %d to %s(%u)\n", signal, process().name().characters(), pid());
#endif

    m_pending_signals |= 1 << (signal - 1);
}

// Certain exceptions, such as SIGSEGV and SIGILL, put a
// thread into a state where the signal handler must be
// invoked immediately, otherwise it will continue to fault.
// This function should be used in an exception handler to
// ensure that when the thread resumes, it's executing in
// the appropriate signal handler.
void Thread::send_urgent_signal_to_self(u8 signal)
{
    // FIXME: because of a bug in dispatch_signal we can't
    // setup a signal while we are the current thread. Because of
    // this we use a work-around where we send the signal and then
    // block, allowing the scheduler to properly dispatch the signal
    // before the thread is next run.
    send_signal(signal, &process());
    (void)block<SemiPermanentBlocker>(SemiPermanentBlocker::Reason::Signal);
}

bool Thread::has_unmasked_pending_signals() const
{
    return m_pending_signals & ~m_signal_mask;
}

ShouldUnblockThread Thread::dispatch_one_pending_signal()
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 signal_candidates = m_pending_signals & ~m_signal_mask;
    ASSERT(signal_candidates);

    u8 signal = 1;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << (signal - 1))) {
            break;
        }
    }
    return dispatch_signal(signal);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

DefaultSignalAction default_signal_action(u8 signal)
{
    ASSERT(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
    case SIGPWR:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    ASSERT_NOT_REACHED();
}

bool Thread::should_ignore_signal(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    if (action.handler_or_sigaction.is_null())
        return default_signal_action(signal) == DefaultSignalAction::Ignore;
    if (action.handler_or_sigaction.as_ptr() == SIG_IGN)
        return true;
    return false;
}

bool Thread::has_signal_handler(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    return !action.handler_or_sigaction.is_null();
}

static void push_value_on_user_stack(u32* stack, u32 data)
{
    *stack -= 4;
    copy_to_user((u32*)*stack, &data);
}

ShouldUnblockThread Thread::dispatch_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal > 0 && signal <= 32);
    ASSERT(!process().is_ring0());

#ifdef SIGNAL_DEBUG
    kprintf("dispatch_signal %s(%u) <- %u\n", process().name().characters(), pid(), signal);
#endif

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << (signal - 1));

    if (signal == SIGSTOP) {
        m_stop_signal = SIGSTOP;
        set_state(Stopped);
        return ShouldUnblockThread::No;
    }

    if (signal == SIGCONT && state() == Stopped)
        set_state(Runnable);

    auto handler_vaddr = action.handler_or_sigaction;
    if (handler_vaddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            m_stop_signal = signal;
            set_state(Stopped);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::DumpCore:
            process().for_each_thread([](auto& thread) {
                thread.set_dump_backtrace_on_finalization();
                return IterationDecision::Continue;
            });
            [[fallthrough]];
        case DefaultSignalAction::Terminate:
            m_process.terminate_due_to_signal(signal);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::Ignore:
            ASSERT_NOT_REACHED();
        case DefaultSignalAction::Continue:
            return ShouldUnblockThread::Yes;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_vaddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        kprintf("%s(%u) ignored signal %u\n", process().name().characters(), pid(), signal);
#endif
        return ShouldUnblockThread::Yes;
    }

    ProcessPagingScope paging_scope(m_process);

    u32 old_signal_mask = m_signal_mask;
    u32 new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << (signal - 1));
    else
        new_signal_mask |= 1 << (signal - 1);

    m_signal_mask |= new_signal_mask;

    auto setup_stack = [&]<typename ThreadState>(ThreadState state, u32 * stack)
    {
        u32 old_esp = *stack;
        u32 ret_eip = state.eip;
        u32 ret_eflags = state.eflags;

        // Align the stack to 16 bytes.
        // Note that we push 56 bytes (4 * 14) on to the stack,
        // so we need to account for this here.
        u32 stack_alignment = (*stack - 56) % 16;
        *stack -= stack_alignment;

        push_value_on_user_stack(stack, ret_eflags);

        push_value_on_user_stack(stack, ret_eip);
        push_value_on_user_stack(stack, state.eax);
        push_value_on_user_stack(stack, state.ecx);
        push_value_on_user_stack(stack, state.edx);
        push_value_on_user_stack(stack, state.ebx);
        push_value_on_user_stack(stack, old_esp);
        push_value_on_user_stack(stack, state.ebp);
        push_value_on_user_stack(stack, state.esi);
        push_value_on_user_stack(stack, state.edi);

        // PUSH old_signal_mask
        push_value_on_user_stack(stack, old_signal_mask);

        push_value_on_user_stack(stack, signal);
        push_value_on_user_stack(stack, handler_vaddr.get());
        push_value_on_user_stack(stack, 0); //push fake return address

        ASSERT((*stack % 16) == 0);
    };

    // We now place the thread state on the userspace stack.
    // Note that when we are in the kernel (ie. blocking) we cannot use the
    // tss, as that will contain kernel state; instead, we use a RegisterState.
    // Conversely, when the thread isn't blocking the RegisterState may not be
    // valid (fork, exec etc) but the tss will, so we use that instead.
    if (!in_kernel()) {
        u32* stack = &m_tss.esp;
        setup_stack(m_tss, stack);

        Scheduler::prepare_to_modify_tss(*this);
        m_tss.cs = 0x1b;
        m_tss.ds = 0x23;
        m_tss.es = 0x23;
        m_tss.fs = 0x23;
        m_tss.gs = thread_specific_selector() | 3;
        m_tss.eip = g_return_to_ring3_from_signal_trampoline.get();
        // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
        set_state(Skip1SchedulerPass);
    } else {
        auto& regs = get_register_dump_from_stack();
        u32* stack = &regs.userspace_esp;
        setup_stack(regs, stack);
        regs.eip = g_return_to_ring3_from_signal_trampoline.get();
    }

#ifdef SIGNAL_DEBUG
    kprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", process().name().characters(), pid(), state_string(), m_tss.cs, m_tss.eip);
#endif
    return ShouldUnblockThread::Yes;
}

void Thread::set_default_signal_dispositions()
{
    // FIXME: Set up all the right default actions. See signal(7).
    memset(&m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_action_data[SIGCHLD].handler_or_sigaction = VirtualAddress(SIG_IGN);
    m_signal_action_data[SIGWINCH].handler_or_sigaction = VirtualAddress(SIG_IGN);
}

void Thread::push_value_on_stack(uintptr_t value)
{
    m_tss.esp -= 4;
    uintptr_t* stack_ptr = (uintptr_t*)m_tss.esp;
    copy_to_user(stack_ptr, &value);
}

RegisterState& Thread::get_register_dump_from_stack()
{
    // The userspace registers should be stored at the top of the stack
    // We have to subtract 2 because the processor decrements the kernel
    // stack before pushing the args.
    return *(RegisterState*)(kernel_stack_top() - sizeof(RegisterState));
}

u32 Thread::make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment)
{
    auto* region = m_process.allocate_region(VirtualAddress(), default_userspace_stack_size, "Stack (Main thread)", PROT_READ | PROT_WRITE, false);
    ASSERT(region);
    region->set_stack(true);

    u32 new_esp = region->vaddr().offset(default_userspace_stack_size).get();

    // FIXME: This is weird, we put the argument contents at the base of the stack,
    //        and the argument pointers at the top? Why?
    char* stack_base = (char*)region->vaddr().get();
    int argc = arguments.size();
    char** argv = (char**)stack_base;
    char** env = argv + arguments.size() + 1;
    char* bufptr = stack_base + (sizeof(char*) * (arguments.size() + 1)) + (sizeof(char*) * (environment.size() + 1));

    SmapDisabler disabler;

    for (int i = 0; i < arguments.size(); ++i) {
        argv[i] = bufptr;
        memcpy(bufptr, arguments[i].characters(), arguments[i].length());
        bufptr += arguments[i].length();
        *(bufptr++) = '\0';
    }
    argv[arguments.size()] = nullptr;

    for (int i = 0; i < environment.size(); ++i) {
        env[i] = bufptr;
        memcpy(bufptr, environment[i].characters(), environment[i].length());
        bufptr += environment[i].length();
        *(bufptr++) = '\0';
    }
    env[environment.size()] = nullptr;

    auto push_on_new_stack = [&new_esp](u32 value) {
        new_esp -= 4;
        u32* stack_ptr = (u32*)new_esp;
        *stack_ptr = value;
    };

    // NOTE: The stack needs to be 16-byte aligned.
    push_on_new_stack((uintptr_t)env);
    push_on_new_stack((uintptr_t)argv);
    push_on_new_stack((uintptr_t)argc);
    push_on_new_stack(0);
    return new_esp;
}

Thread* Thread::clone(Process& process)
{
    auto* clone = new Thread(process);
    memcpy(clone->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    clone->m_signal_mask = m_signal_mask;
    memcpy(clone->m_fpu_state, m_fpu_state, sizeof(FPUState));
    clone->m_thread_specific_data = m_thread_specific_data;
    return clone;
}

void Thread::initialize()
{
    Scheduler::initialize();
    asm volatile("fninit");
    asm volatile("fxsave %0"
                 : "=m"(s_clean_fpu_state));
}

Vector<Thread*> Thread::all_threads()
{
    Vector<Thread*> threads;
    InterruptDisabler disabler;
    threads.ensure_capacity(thread_table().size());
    for (auto* thread : thread_table())
        threads.unchecked_append(thread);
    return threads;
}

bool Thread::is_thread(void* ptr)
{
    ASSERT_INTERRUPTS_DISABLED();
    return thread_table().contains((Thread*)ptr);
}

void Thread::set_state(State new_state)
{
    InterruptDisabler disabler;
    if (new_state == m_state)
        return;

    if (new_state == Blocked) {
        // we should always have a Blocker while blocked
        ASSERT(m_blocker != nullptr);
    }

    m_state = new_state;
    if (m_process.pid() != 0) {
        Scheduler::update_state_for_thread(*this);
    }

    if (new_state == Dying) {
        g_finalizer_has_work = true;
        g_finalizer_wait_queue->wake_all();
    }
}

String Thread::backtrace(ProcessInspectionHandle&) const
{
    return backtrace_impl();
}

struct RecognizedSymbol {
    u32 address;
    const KSym* ksym;
};

static bool symbolicate(const RecognizedSymbol& symbol, const Process& process, StringBuilder& builder)
{
    if (!symbol.address)
        return false;

    bool mask_kernel_addresses = !process.is_superuser();
    if (!symbol.ksym) {
        if (!is_user_address(VirtualAddress(symbol.address))) {
            builder.append("0xdeadc0de\n");
        } else {
            if (!Scheduler::is_active() && process.elf_loader() && process.elf_loader()->has_symbols())
                builder.appendf("%p  %s\n", symbol.address, process.elf_loader()->symbolicate(symbol.address).characters());
            else
                builder.appendf("%p\n", symbol.address);
        }
        return true;
    }
    unsigned offset = symbol.address - symbol.ksym->address;
    if (symbol.ksym->address == ksym_highest_address && offset > 4096) {
        builder.appendf("%p\n", mask_kernel_addresses ? 0xdeadc0de : symbol.address);
    } else {
        builder.appendf("%p  %s +%u\n", mask_kernel_addresses ? 0xdeadc0de : symbol.address, demangle(symbol.ksym->name).characters(), offset);
    }
    return true;
}

String Thread::backtrace_impl() const
{
    Vector<RecognizedSymbol, 128> recognized_symbols;

    u32 start_frame;
    if (current == this) {
        asm volatile("movl %%ebp, %%eax"
                     : "=a"(start_frame));
    } else {
        start_frame = frame_ptr();
        recognized_symbols.append({ tss().eip, ksymbolicate(tss().eip) });
    }

    auto& process = const_cast<Process&>(this->process());
    ProcessPagingScope paging_scope(process);

    uintptr_t stack_ptr = start_frame;
    for (;;) {
        if (!process.validate_read_from_kernel(VirtualAddress(stack_ptr), sizeof(void*) * 2))
            break;
        uintptr_t retaddr;

        if (is_user_range(VirtualAddress(stack_ptr), sizeof(uintptr_t) * 2)) {
            copy_from_user(&retaddr, &((uintptr_t*)stack_ptr)[1]);
            recognized_symbols.append({ retaddr, ksymbolicate(retaddr) });
            copy_from_user(&stack_ptr, (uintptr_t*)stack_ptr);
        } else {
            memcpy(&retaddr, &((uintptr_t*)stack_ptr)[1], sizeof(uintptr_t));
            recognized_symbols.append({ retaddr, ksymbolicate(retaddr) });
            memcpy(&stack_ptr, (uintptr_t*)stack_ptr, sizeof(uintptr_t));
        }
    }

    StringBuilder builder;
    for (auto& symbol : recognized_symbols) {
        if (!symbolicate(symbol, process, builder))
            break;
    }
    return builder.to_string();
}

Vector<uintptr_t> Thread::raw_backtrace(uintptr_t ebp) const
{
    auto& process = const_cast<Process&>(this->process());
    ProcessPagingScope paging_scope(process);
    Vector<uintptr_t, Profiling::max_stack_frame_count> backtrace;
    backtrace.append(ebp);
    for (uintptr_t* stack_ptr = (uintptr_t*)ebp; process.validate_read_from_kernel(VirtualAddress(stack_ptr), sizeof(uintptr_t) * 2); stack_ptr = (uintptr_t*)*stack_ptr) {
        uintptr_t retaddr = stack_ptr[1];
        backtrace.append(retaddr);
        if (backtrace.size() == Profiling::max_stack_frame_count)
            break;
    }
    return backtrace;
}

void Thread::make_thread_specific_region(Badge<Process>)
{
    size_t thread_specific_region_alignment = max(process().m_master_tls_alignment, alignof(ThreadSpecificData));
    size_t thread_specific_region_size = align_up_to(process().m_master_tls_size, thread_specific_region_alignment) + sizeof(ThreadSpecificData);
    auto* region = process().allocate_region({}, thread_specific_region_size, "Thread-specific", PROT_READ | PROT_WRITE, true);
    SmapDisabler disabler;
    auto* thread_specific_data = (ThreadSpecificData*)region->vaddr().offset(align_up_to(process().m_master_tls_size, thread_specific_region_alignment)).as_ptr();
    auto* thread_local_storage = (u8*)((u8*)thread_specific_data) - align_up_to(process().m_master_tls_size, process().m_master_tls_alignment);
    m_thread_specific_data = VirtualAddress(thread_specific_data);
    thread_specific_data->self = thread_specific_data;
    if (process().m_master_tls_size)
        memcpy(thread_local_storage, process().m_master_tls_region->vaddr().as_ptr(), process().m_master_tls_size);
}

const LogStream& operator<<(const LogStream& stream, const Thread& value)
{
    return stream << value.process().name() << "(" << value.pid() << ":" << value.tid() << ")";
}

void Thread::wait_on(WaitQueue& queue, Atomic<bool>* lock, Thread* beneficiary, const char* reason)
{
    cli();
    bool did_unlock = unlock_process_if_locked();
    if (lock)
        *lock = false;
    set_state(State::Queued);
    queue.enqueue(*current);
    // Yield and wait for the queue to wake us up again.
    if (beneficiary)
        Scheduler::donate_to(beneficiary, reason);
    else
        Scheduler::yield();
    // We've unblocked, relock the process if needed and carry on.
    if (did_unlock)
        relock_process();
}

void Thread::wake_from_queue()
{
    ASSERT(state() == State::Queued);
    set_state(State::Runnable);
}

Thread* Thread::from_tid(int tid)
{
    InterruptDisabler disabler;
    Thread* found_thread = nullptr;
    Thread::for_each([&](auto& thread) {
        if (thread.tid() == tid) {
            found_thread = &thread;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_thread;
}
