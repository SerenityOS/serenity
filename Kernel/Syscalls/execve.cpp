/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/WeakPtr.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/limits.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

namespace Kernel {

extern Region* g_signal_trampoline_region;

struct LoadResult {
    OwnPtr<Space> space;
    FlatPtr load_base { 0 };
    FlatPtr entry_eip { 0 };
    size_t size { 0 };
    WeakPtr<Region> tls_region;
    size_t tls_size { 0 };
    size_t tls_alignment { 0 };
    WeakPtr<Region> stack_region;
};

static Vector<ELF::AuxiliaryValue> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, uid_t uid, uid_t euid, gid_t gid, gid_t egid, String executable_path, int main_program_fd);

static bool validate_stack_size(const Vector<String>& arguments, const Vector<String>& environment)
{
    size_t total_arguments_size = 0;
    size_t total_environment_size = 0;

    for (auto& a : arguments)
        total_arguments_size += a.length() + 1;
    for (auto& e : environment)
        total_environment_size += e.length() + 1;

    total_arguments_size += sizeof(char*) * (arguments.size() + 1);
    total_environment_size += sizeof(char*) * (environment.size() + 1);

    static constexpr size_t max_arguments_size = Thread::default_userspace_stack_size / 8;
    static constexpr size_t max_environment_size = Thread::default_userspace_stack_size / 8;

    if (total_arguments_size > max_arguments_size)
        return false;

    if (total_environment_size > max_environment_size)
        return false;

    // FIXME: This doesn't account for the size of the auxiliary vector
    return true;
}

static KResultOr<FlatPtr> make_userspace_stack_for_main_thread(Region& region, Vector<String> arguments, Vector<String> environment, Vector<ELF::AuxiliaryValue> auxiliary_values)
{
    FlatPtr new_esp = region.vaddr().offset(Thread::default_userspace_stack_size).get();

    auto push_on_new_stack = [&new_esp](u32 value) {
        new_esp -= 4;
        Userspace<u32*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, &value);
    };

    auto push_aux_value_on_new_stack = [&new_esp](auxv_t value) {
        new_esp -= sizeof(auxv_t);
        Userspace<auxv_t*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, &value);
    };

    auto push_string_on_new_stack = [&new_esp](const String& string) {
        new_esp -= round_up_to_power_of_two(string.length() + 1, 4);
        Userspace<u32*> stack_ptr = new_esp;
        return copy_to_user(stack_ptr, string.characters(), string.length() + 1);
    };

    Vector<FlatPtr> argv_entries;
    for (auto& argument : arguments) {
        push_string_on_new_stack(argument);
        argv_entries.append(new_esp);
    }

    Vector<FlatPtr> env_entries;
    for (auto& variable : environment) {
        push_string_on_new_stack(variable);
        env_entries.append(new_esp);
    }

    for (auto& value : auxiliary_values) {
        if (!value.optional_string.is_empty()) {
            push_string_on_new_stack(value.optional_string);
            value.auxv.a_un.a_ptr = (void*)new_esp;
        }
    }

    for (ssize_t i = auxiliary_values.size() - 1; i >= 0; --i) {
        auto& value = auxiliary_values[i];
        push_aux_value_on_new_stack(value.auxv);
    }

    push_on_new_stack(0);
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(env_entries[i]);
    FlatPtr envp = new_esp;

    push_on_new_stack(0);
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(argv_entries[i]);
    FlatPtr argv = new_esp;

    // NOTE: The stack needs to be 16-byte aligned.
    new_esp -= new_esp % 16;

    push_on_new_stack((FlatPtr)envp);
    push_on_new_stack((FlatPtr)argv);
    push_on_new_stack((FlatPtr)argv_entries.size());
    push_on_new_stack(0);

    return new_esp;
}

struct RequiredLoadRange {
    FlatPtr start { 0 };
    FlatPtr end { 0 };
};

static KResultOr<RequiredLoadRange> get_required_load_range(FileDescription& program_description)
{
    auto& inode = *(program_description.inode());
    auto vmobject = SharedInodeVMObject::create_with_inode(inode);

    size_t executable_size = inode.size();

    auto region = MM.allocate_kernel_region_with_vmobject(*vmobject, page_round_up(executable_size), "ELF memory range calculation", Region::Access::Read);
    if (!region) {
        dbgln("Could not allocate memory for ELF");
        return ENOMEM;
    }

    auto elf_image = ELF::Image(region->vaddr().as_ptr(), executable_size);
    if (!elf_image.is_valid()) {
        return EINVAL;
    }

    RequiredLoadRange range {};
    elf_image.for_each_program_header([&range](const auto& pheader) {
        if (pheader.type() != PT_LOAD)
            return IterationDecision::Continue;

        auto region_start = (FlatPtr)pheader.vaddr().as_ptr();
        auto region_end = region_start + pheader.size_in_memory();
        if (range.start == 0 || region_start < range.start)
            range.start = region_start;
        if (range.end == 0 || region_end > range.end)
            range.end = region_end;
        return IterationDecision::Continue;
    });

    ASSERT(range.end > range.start);
    return range;
};

static KResultOr<FlatPtr> get_interpreter_load_offset(const Elf32_Ehdr& main_program_header, FileDescription& main_program_description, FileDescription& interpreter_description)
{
    constexpr FlatPtr interpreter_load_range_start = 0x08000000;
    constexpr FlatPtr interpreter_load_range_size = 65536 * PAGE_SIZE; // 2**16 * PAGE_SIZE = 256MB
    constexpr FlatPtr minimum_interpreter_load_offset_randomization_size = 10 * MiB;

    auto random_load_offset_in_range([](auto start, auto size) {
        return page_round_down(start + get_good_random<FlatPtr>() % size);
    });

    if (main_program_header.e_type == ET_DYN) {
        return random_load_offset_in_range(interpreter_load_range_start, interpreter_load_range_size);
    }

    if (main_program_header.e_type != ET_EXEC)
        return -EINVAL;

    auto main_program_load_range_result = get_required_load_range(main_program_description);
    if (main_program_load_range_result.is_error())
        return main_program_load_range_result.error();

    auto main_program_load_range = main_program_load_range_result.value();

    auto interpreter_load_range_result = get_required_load_range(interpreter_description);
    if (interpreter_load_range_result.is_error())
        return interpreter_load_range_result.error();

    auto interpreter_size_in_memory = interpreter_load_range_result.value().end - interpreter_load_range_result.value().start;
    auto interpreter_load_range_end = interpreter_load_range_start + interpreter_load_range_size - interpreter_size_in_memory;

    // No intersection
    if (main_program_load_range.end < interpreter_load_range_start || main_program_load_range.start > interpreter_load_range_end)
        return random_load_offset_in_range(interpreter_load_range_start, interpreter_load_range_size);

    RequiredLoadRange first_available_part = { interpreter_load_range_start, main_program_load_range.start };
    RequiredLoadRange second_available_part = { main_program_load_range.end, interpreter_load_range_end };

    RequiredLoadRange selected_range {};
    // Select larger part
    if (first_available_part.end - first_available_part.start > second_available_part.end - second_available_part.start)
        selected_range = first_available_part;
    else
        selected_range = second_available_part;

    // If main program is too big and leaves us without enough space for adequate loader randmoization
    if (selected_range.end - selected_range.start < minimum_interpreter_load_offset_randomization_size)
        return -E2BIG;

    return random_load_offset_in_range(selected_range.start, selected_range.end - selected_range.start);
}

enum class ShouldAllocateTls {
    No,
    Yes,
};

static KResultOr<LoadResult> load_elf_object(NonnullOwnPtr<Space> new_space, FileDescription& object_description, FlatPtr load_offset, ShouldAllocateTls should_allocate_tls)
{
    auto& inode = *(object_description.inode());
    auto vmobject = SharedInodeVMObject::create_with_inode(inode);
    if (vmobject->writable_mappings()) {
        dbgln("Refusing to execute a write-mapped program");
        return ETXTBSY;
    }

    size_t executable_size = inode.size();

    auto executable_region = MM.allocate_kernel_region_with_vmobject(*vmobject, page_round_up(executable_size), "ELF loading", Region::Access::Read);
    if (!executable_region) {
        dbgln("Could not allocate memory for ELF loading");
        return ENOMEM;
    }

    auto elf_image = ELF::Image(executable_region->vaddr().as_ptr(), executable_size);

    if (!elf_image.is_valid())
        return ENOEXEC;

    Region* master_tls_region { nullptr };
    size_t master_tls_size = 0;
    size_t master_tls_alignment = 0;
    FlatPtr load_base_address = 0;

    String elf_name = object_description.absolute_path();
    ASSERT(!Processor::current().in_critical());

    MemoryManager::enter_space(*new_space);

    KResult ph_load_result = KSuccess;
    elf_image.for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_TLS) {
            ASSERT(should_allocate_tls == ShouldAllocateTls::Yes);
            ASSERT(program_header.size_in_memory());

            if (!elf_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
                dbgln("Shenanigans! ELF PT_TLS header sneaks outside of executable.");
                ph_load_result = ENOEXEC;
                return IterationDecision::Break;
            }

            auto range = new_space->allocate_range({}, program_header.size_in_memory());
            if (!range.has_value()) {
                ph_load_result = ENOMEM;
                return IterationDecision::Break;
            }

            auto region_or_error = new_space->allocate_region(range.value(), String::formatted("{} (master-tls)", elf_name), PROT_READ | PROT_WRITE, AllocationStrategy::Reserve);
            if (region_or_error.is_error()) {
                ph_load_result = region_or_error.error();
                return IterationDecision::Break;
            }
            master_tls_region = region_or_error.value();
            master_tls_size = program_header.size_in_memory();
            master_tls_alignment = program_header.alignment();

            if (!copy_to_user(master_tls_region->vaddr().as_ptr(), program_header.raw_data(), program_header.size_in_image())) {
                ph_load_result = EFAULT;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        }
        if (program_header.type() != PT_LOAD)
            return IterationDecision::Continue;

        if (program_header.is_writable()) {
            // Writable section: create a copy in memory.
            ASSERT(program_header.size_in_memory());
            ASSERT(program_header.alignment() == PAGE_SIZE);

            if (!elf_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
                dbgln("Shenanigans! Writable ELF PT_LOAD header sneaks outside of executable.");
                ph_load_result = ENOEXEC;
                return IterationDecision::Break;
            }

            int prot = 0;
            if (program_header.is_readable())
                prot |= PROT_READ;
            if (program_header.is_writable())
                prot |= PROT_WRITE;
            auto region_name = String::formatted("{} (data-{}{})", elf_name, program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "");
            auto range = new_space->allocate_range(program_header.vaddr().offset(load_offset), program_header.size_in_memory());
            if (!range.has_value()) {
                ph_load_result = ENOMEM;
                return IterationDecision::Break;
            }
            auto region_or_error = new_space->allocate_region(range.value(), region_name, prot, AllocationStrategy::Reserve);
            if (region_or_error.is_error()) {
                ph_load_result = region_or_error.error();
                return IterationDecision::Break;
            }

            // It's not always the case with PIE executables (and very well shouldn't be) that the
            // virtual address in the program header matches the one we end up giving the process.
            // In order to copy the data image correctly into memory, we need to copy the data starting at
            // the right initial page offset into the pages allocated for the elf_alloc-XX section.
            // FIXME: There's an opportunity to munmap, or at least mprotect, the padding space between
            //     the .text and .data PT_LOAD sections of the executable.
            //     Accessing it would definitely be a bug.
            auto page_offset = program_header.vaddr();
            page_offset.mask(~PAGE_MASK);
            if (!copy_to_user((u8*)region_or_error.value()->vaddr().as_ptr() + page_offset.get(), program_header.raw_data(), program_header.size_in_image())) {
                ph_load_result = EFAULT;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        }

        // Non-writable section: map the executable itself in memory.
        ASSERT(program_header.size_in_memory());
        ASSERT(program_header.alignment() == PAGE_SIZE);
        int prot = 0;
        if (program_header.is_readable())
            prot |= PROT_READ;
        if (program_header.is_writable())
            prot |= PROT_WRITE;
        if (program_header.is_executable())
            prot |= PROT_EXEC;
        auto range = new_space->allocate_range(program_header.vaddr().offset(load_offset), program_header.size_in_memory());
        if (!range.has_value()) {
            ph_load_result = ENOMEM;
            return IterationDecision::Break;
        }
        auto region_or_error = new_space->allocate_region_with_vmobject(range.value(), *vmobject, program_header.offset(), elf_name, prot, true);
        if (region_or_error.is_error()) {
            ph_load_result = region_or_error.error();
            return IterationDecision::Break;
        }
        if (program_header.offset() == 0)
            load_base_address = (FlatPtr)region_or_error.value()->vaddr().as_ptr();
        return IterationDecision::Continue;
    });

    if (ph_load_result.is_error()) {
        dbgln("do_exec: Failure loading program ({})", ph_load_result.error());
        return ph_load_result;
    }

    if (!elf_image.entry().offset(load_offset).get()) {
        dbgln("do_exec: Failure loading program, entry pointer is invalid! {})", elf_image.entry().offset(load_offset));
        return ENOEXEC;
    }

    auto stack_range = new_space->allocate_range({}, Thread::default_userspace_stack_size);
    if (!stack_range.has_value()) {
        dbgln("do_exec: Failed to allocate VM range for stack");
        return ENOMEM;
    }

    auto stack_region_or_error = new_space->allocate_region(stack_range.value(), "Stack (Main thread)", PROT_READ | PROT_WRITE, AllocationStrategy::Reserve);
    if (stack_region_or_error.is_error())
        return stack_region_or_error.error();
    auto& stack_region = *stack_region_or_error.value();
    stack_region.set_stack(true);

    return LoadResult {
        move(new_space),
        load_base_address,
        elf_image.entry().offset(load_offset).get(),
        executable_size,
        AK::try_make_weak_ptr(master_tls_region),
        master_tls_size,
        master_tls_alignment,
        stack_region.make_weak_ptr()
    };
}

KResultOr<LoadResult> Process::load(NonnullRefPtr<FileDescription> main_program_description, RefPtr<FileDescription> interpreter_description, const Elf32_Ehdr& main_program_header)
{
    auto new_space = Space::create(*this, nullptr);
    if (!new_space)
        return ENOMEM;

    ScopeGuard space_guard([&]() {
        MemoryManager::enter_process_paging_scope(*this);
    });

    if (interpreter_description.is_null()) {
        auto result = load_elf_object(new_space.release_nonnull(), main_program_description, FlatPtr { 0 }, ShouldAllocateTls::Yes);
        if (result.is_error())
            return result.error();
        return result;
    }

    auto interpreter_load_offset = get_interpreter_load_offset(main_program_header, main_program_description, *interpreter_description);
    if (interpreter_load_offset.is_error()) {
        return interpreter_load_offset.error();
    }

    auto interpreter_load_result = load_elf_object(new_space.release_nonnull(), *interpreter_description, interpreter_load_offset.value(), ShouldAllocateTls::No);

    if (interpreter_load_result.is_error())
        return interpreter_load_result.error();

    // TLS allocation will be done in userspace by the loader
    ASSERT(!interpreter_load_result.value().tls_region);
    ASSERT(!interpreter_load_result.value().tls_alignment);
    ASSERT(!interpreter_load_result.value().tls_size);

    return interpreter_load_result;
}

int Process::do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const Elf32_Ehdr& main_program_header)
{
    ASSERT(is_user_process());
    ASSERT(!Processor::current().in_critical());
    auto path = main_program_description->absolute_path();
#if EXEC_DEBUG
    dbgln("do_exec({})", path);
#endif

    // FIXME: How much stack space does process startup need?
    if (!validate_stack_size(arguments, environment))
        return -E2BIG;

    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    auto main_program_metadata = main_program_description->metadata();

    auto load_result_or_error = load(main_program_description, interpreter_description, main_program_header);
    if (load_result_or_error.is_error()) {
        dbgln("do_exec({}): Failed to load main program or interpreter", path);
        return load_result_or_error.error();
    }

    auto signal_trampoline_range = load_result_or_error.value().space->allocate_range({}, PAGE_SIZE);
    if (!signal_trampoline_range.has_value()) {
        dbgln("do_exec: Failed to allocate VM for signal trampoline");
        return -ENOMEM;
    }

    // We commit to the new executable at this point. There is no turning back!

    // Prevent other processes from attaching to us with ptrace while we're doing this.
    Locker ptrace_locker(ptrace_lock());

    // Disable profiling temporarily in case it's running on this process.
    TemporaryChange profiling_disabler(m_profiling, false);

    kill_threads_except_self();

    auto& load_result = load_result_or_error.value();
    bool executable_is_setid = false;

    if (!(main_program_description->custody()->mount_flags() & MS_NOSUID)) {
        if (main_program_metadata.is_setuid()) {
            executable_is_setid = true;
            m_euid = m_suid = main_program_metadata.uid;
        }
        if (main_program_metadata.is_setgid()) {
            executable_is_setid = true;
            m_egid = m_sgid = main_program_metadata.gid;
        }
    }

    set_dumpable(!executable_is_setid);

    m_space = load_result.space.release_nonnull();
    MemoryManager::enter_space(*m_space);

    auto signal_trampoline_region = m_space->allocate_region_with_vmobject(signal_trampoline_range.value(), g_signal_trampoline_region->vmobject(), 0, "Signal trampoline", PROT_READ | PROT_EXEC, true);
    if (signal_trampoline_region.is_error()) {
        ASSERT_NOT_REACHED();
    }

    signal_trampoline_region.value()->set_syscall_region(true);
    m_signal_trampoline = signal_trampoline_region.value()->vaddr();

    m_executable = main_program_description->custody();
    m_arguments = arguments;
    m_environment = environment;

    m_promises = m_execpromises;
    m_has_promises = m_has_execpromises;

    m_execpromises = 0;
    m_has_execpromises = false;

    m_veil_state = VeilState::None;
    m_unveiled_paths.clear();

    m_coredump_metadata.clear();

    auto current_thread = Thread::current();
    current_thread->set_default_signal_dispositions();
    current_thread->clear_signals();

    clear_futex_queues_on_exec();

    for (size_t i = 0; i < m_fds.size(); ++i) {
        auto& description_and_flags = m_fds[i];
        if (description_and_flags.description() && description_and_flags.flags() & FD_CLOEXEC)
            description_and_flags = {};
    }

    int main_program_fd = -1;
    if (interpreter_description) {
        main_program_fd = alloc_fd();
        ASSERT(main_program_fd >= 0);
        main_program_description->seek(0, SEEK_SET);
        main_program_description->set_readable(true);
        m_fds[main_program_fd].set(move(main_program_description), FD_CLOEXEC);
    }

    new_main_thread = nullptr;
    if (&current_thread->process() == this) {
        new_main_thread = current_thread;
    } else {
        for_each_thread([&](auto& thread) {
            new_main_thread = &thread;
            return IterationDecision::Break;
        });
    }
    ASSERT(new_main_thread);

    auto auxv = generate_auxiliary_vector(load_result.load_base, load_result.entry_eip, m_uid, m_euid, m_gid, m_egid, path, main_program_fd);

    // NOTE: We create the new stack before disabling interrupts since it will zero-fault
    //       and we don't want to deal with faults after this point.
    auto make_stack_result = make_userspace_stack_for_main_thread(*load_result.stack_region.unsafe_ptr(), move(arguments), move(environment), move(auxv));
    if (make_stack_result.is_error())
        return make_stack_result.error();
    u32 new_userspace_esp = make_stack_result.value();

    if (wait_for_tracer_at_next_execve())
        Thread::current()->send_urgent_signal_to_self(SIGSTOP);

    // We enter a critical section here because we don't want to get interrupted between do_exec()
    // and Processor::assume_context() or the next context switch.
    // If we used an InterruptDisabler that sti()'d on exit, we might timer tick'd too soon in exec().
    Processor::current().enter_critical(prev_flags);

    // NOTE: Be careful to not trigger any page faults below!

    m_name = parts.take_last();
    new_main_thread->set_name(m_name);

    // FIXME: PID/TID ISSUE
    m_pid = new_main_thread->tid().value();
    auto tsr_result = new_main_thread->make_thread_specific_region({});
    if (tsr_result.is_error()) {
        // FIXME: We cannot fail this late. Refactor this so the allocation happens before we commit to the new executable.
        ASSERT_NOT_REACHED();
    }
    new_main_thread->reset_fpu_state();

    auto& tss = new_main_thread->m_tss;
    tss.cs = GDT_SELECTOR_CODE3 | 3;
    tss.ds = GDT_SELECTOR_DATA3 | 3;
    tss.es = GDT_SELECTOR_DATA3 | 3;
    tss.ss = GDT_SELECTOR_DATA3 | 3;
    tss.fs = GDT_SELECTOR_DATA3 | 3;
    tss.gs = GDT_SELECTOR_TLS | 3;
    tss.eip = load_result.entry_eip;
    tss.esp = new_userspace_esp;
    tss.cr3 = space().page_directory().cr3();
    tss.ss2 = m_pid.value();

    // Throw away any recorded performance events in this process.
    if (m_perf_event_buffer)
        m_perf_event_buffer->clear();

    {
        ScopedSpinLock lock(g_scheduler_lock);
        new_main_thread->set_state(Thread::State::Runnable);
    }
    u32 lock_count_to_restore;
    [[maybe_unused]] auto rc = big_lock().force_unlock_if_locked(lock_count_to_restore);
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(Processor::current().in_critical());
    return 0;
}

static Vector<ELF::AuxiliaryValue> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, uid_t uid, uid_t euid, gid_t gid, gid_t egid, String executable_path, int main_program_fd)
{
    Vector<ELF::AuxiliaryValue> auxv;
    // PHDR/EXECFD
    // PH*
    auxv.append({ ELF::AuxiliaryValue::PageSize, PAGE_SIZE });
    auxv.append({ ELF::AuxiliaryValue::BaseAddress, (void*)load_base });

    auxv.append({ ELF::AuxiliaryValue::Entry, (void*)entry_eip });
    // NOTELF
    auxv.append({ ELF::AuxiliaryValue::Uid, (long)uid });
    auxv.append({ ELF::AuxiliaryValue::EUid, (long)euid });
    auxv.append({ ELF::AuxiliaryValue::Gid, (long)gid });
    auxv.append({ ELF::AuxiliaryValue::EGid, (long)egid });

    // FIXME: Don't hard code this? We might support other platforms later.. (e.g. x86_64)
    auxv.append({ ELF::AuxiliaryValue::Platform, "i386" });
    // FIXME: This is platform specific
    auxv.append({ ELF::AuxiliaryValue::HwCap, (long)CPUID(1).edx() });

    auxv.append({ ELF::AuxiliaryValue::ClockTick, (long)TimeManagement::the().ticks_per_second() });

    // FIXME: Also take into account things like extended filesystem permissions? That's what linux does...
    auxv.append({ ELF::AuxiliaryValue::Secure, ((uid != euid) || (gid != egid)) ? 1 : 0 });

    char random_bytes[16] {};
    get_fast_random_bytes((u8*)random_bytes, sizeof(random_bytes));

    auxv.append({ ELF::AuxiliaryValue::Random, String(random_bytes, sizeof(random_bytes)) });

    auxv.append({ ELF::AuxiliaryValue::ExecFilename, executable_path });

    auxv.append({ ELF::AuxiliaryValue::ExecFileDescriptor, main_program_fd });

    auxv.append({ ELF::AuxiliaryValue::Null, 0L });
    return auxv;
}

static KResultOr<Vector<String>> find_shebang_interpreter_for_executable(const char first_page[], int nread)
{
    int word_start = 2;
    int word_length = 0;
    if (nread > 2 && first_page[0] == '#' && first_page[1] == '!') {
        Vector<String> interpreter_words;

        for (int i = 2; i < nread; ++i) {
            if (first_page[i] == '\n') {
                break;
            }

            if (first_page[i] != ' ') {
                ++word_length;
            }

            if (first_page[i] == ' ') {
                if (word_length > 0) {
                    interpreter_words.append(String(&first_page[word_start], word_length));
                }
                word_length = 0;
                word_start = i + 1;
            }
        }

        if (word_length > 0)
            interpreter_words.append(String(&first_page[word_start], word_length));

        if (!interpreter_words.is_empty())
            return interpreter_words;
    }

    return ENOEXEC;
}

KResultOr<RefPtr<FileDescription>> Process::find_elf_interpreter_for_executable(const String& path, const Elf32_Ehdr& main_program_header, int nread, size_t file_size)
{

    // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
    String interpreter_path;
    if (!ELF::validate_program_headers(main_program_header, file_size, (const u8*)&main_program_header, nread, &interpreter_path)) {
        dbgln("exec({}): File has invalid ELF Program headers", path);
        return ENOEXEC;
    }

    if (!interpreter_path.is_empty()) {

#if EXEC_DEBUG
        dbgln("exec({}): Using program interpreter {}", path, interpreter_path);
#endif
        auto interp_result = VFS::the().open(interpreter_path, O_EXEC, 0, current_directory());
        if (interp_result.is_error()) {
            dbgln("exec({}): Unable to open program interpreter {}", path, interpreter_path);
            return interp_result.error();
        }
        auto interpreter_description = interp_result.value();
        auto interp_metadata = interpreter_description->metadata();

        ASSERT(interpreter_description->inode());

        // Validate the program interpreter as a valid elf binary.
        // If your program interpreter is a #! file or something, it's time to stop playing games :)
        if (interp_metadata.size < (int)sizeof(Elf32_Ehdr))
            return ENOEXEC;

        char first_page[PAGE_SIZE] = {};
        auto first_page_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&first_page);
        auto nread_or_error = interpreter_description->read(first_page_buffer, sizeof(first_page));
        if (nread_or_error.is_error())
            return ENOEXEC;
        nread = nread_or_error.value();

        if (nread < (int)sizeof(Elf32_Ehdr))
            return ENOEXEC;

        auto elf_header = (Elf32_Ehdr*)first_page;
        if (!ELF::validate_elf_header(*elf_header, interp_metadata.size)) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF header", path, interpreter_description->absolute_path());
            return ENOEXEC;
        }

        // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
        String interpreter_interpreter_path;
        if (!ELF::validate_program_headers(*elf_header, interp_metadata.size, (u8*)first_page, nread, &interpreter_interpreter_path)) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF Program headers", path, interpreter_description->absolute_path());
            return ENOEXEC;
        }

        if (!interpreter_interpreter_path.is_empty()) {
            dbgln("exec({}): Interpreter ({}) has its own interpreter ({})! No thank you!", path, interpreter_description->absolute_path(), interpreter_interpreter_path);
            return ELOOP;
        }

        return interpreter_description;
    }

    if (main_program_header.e_type == ET_REL) {
        // We can't exec an ET_REL, that's just an object file from the compiler
        return ENOEXEC;
    }
    if (main_program_header.e_type == ET_DYN) {
        // If it's ET_DYN with no PT_INTERP, then it's a dynamic executable responsible
        // for its own relocation (i.e. it's /usr/lib/Loader.so)
        if (path != "/usr/lib/Loader.so")
            dbgln("exec({}): WARNING - Dynamic ELF executable without a PT_INTERP header, and isn't /usr/lib/Loader.so", path);
        return nullptr;
    }

    // No interpreter, but, path refers to a valid elf image
    return KResult(KSuccess);
}

int Process::exec(String path, Vector<String> arguments, Vector<String> environment, int recursion_depth)
{
    if (recursion_depth > 2) {
        dbgln("exec({}): SHENANIGANS! recursed too far trying to find #! interpreter", path);
        return -ELOOP;
    }

    // Open the file to check what kind of binary format it is
    // Currently supported formats:
    //    - #! interpreted file
    //    - ELF32
    //        * ET_EXEC binary that just gets loaded
    //        * ET_DYN binary that requires a program interpreter
    //
    auto result = VFS::the().open(path, O_EXEC, 0, current_directory());
    if (result.is_error())
        return result.error();
    auto description = result.release_value();
    auto metadata = description->metadata();

    // Always gonna need at least 3 bytes. these are for #!X
    if (metadata.size < 3)
        return -ENOEXEC;

    ASSERT(description->inode());

    // Read the first page of the program into memory so we can validate the binfmt of it
    char first_page[PAGE_SIZE];
    auto first_page_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&first_page);
    auto nread_or_error = description->read(first_page_buffer, sizeof(first_page));
    if (nread_or_error.is_error())
        return -ENOEXEC;

    // 1) #! interpreted file
    auto shebang_result = find_shebang_interpreter_for_executable(first_page, nread_or_error.value());
    if (!shebang_result.is_error()) {
        Vector<String> new_arguments(shebang_result.value());

        new_arguments.append(path);

        arguments.remove(0);
        new_arguments.append(move(arguments));

        return exec(shebang_result.value().first(), move(new_arguments), move(environment), ++recursion_depth);
    }

    // #2) ELF32 for i386

    if (nread_or_error.value() < (int)sizeof(Elf32_Ehdr))
        return -ENOEXEC;
    auto main_program_header = (Elf32_Ehdr*)first_page;

    if (!ELF::validate_elf_header(*main_program_header, metadata.size)) {
        dbgln("exec({}): File has invalid ELF header", path);
        return -ENOEXEC;
    }

    auto elf_result = find_elf_interpreter_for_executable(path, *main_program_header, nread_or_error.value(), metadata.size);
    // Assume a static ELF executable by default
    RefPtr<FileDescription> interpreter_description;
    // We're getting either an interpreter, an error, or KSuccess (i.e. no interpreter but file checks out)
    if (!elf_result.is_error()) {
        // It's a dynamic ELF executable, with or without an interpreter. Do not allocate TLS
        interpreter_description = elf_result.value();
    } else if (elf_result.error().is_error())
        return elf_result.error();

    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    Thread* new_main_thread = nullptr;
    u32 prev_flags = 0;
    int rc = do_exec(move(description), move(arguments), move(environment), move(interpreter_description), new_main_thread, prev_flags, *main_program_header);

    if (rc < 0)
        return rc;

    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(Processor::current().in_critical());

    auto current_thread = Thread::current();
    if (current_thread == new_main_thread) {
        // We need to enter the scheduler lock before changing the state
        // and it will be released after the context switch into that
        // thread. We should also still be in our critical section
        ASSERT(!g_scheduler_lock.own_lock());
        ASSERT(Processor::current().in_critical() == 1);
        g_scheduler_lock.lock();
        current_thread->set_state(Thread::State::Running);
        Processor::assume_context(*current_thread, prev_flags);
        ASSERT_NOT_REACHED();
    }

    Processor::current().leave_critical(prev_flags);
    return 0;
}

int Process::sys$execve(Userspace<const Syscall::SC_execve_params*> user_params)
{
    REQUIRE_PROMISE(exec);

    // NOTE: Be extremely careful with allocating any kernel memory in exec().
    //       On success, the kernel stack will be lost.
    Syscall::SC_execve_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    if (params.arguments.length > ARG_MAX || params.environment.length > ARG_MAX)
        return -E2BIG;

    String path;
    {
        auto path_arg = get_syscall_path_argument(params.path);
        if (path_arg.is_error())
            return path_arg.error();
        path = path_arg.value();
    }

    auto copy_user_strings = [](const auto& list, auto& output) {
        if (!list.length)
            return true;
        Checked size = sizeof(*list.strings);
        size *= list.length;
        if (size.has_overflow())
            return false;
        Vector<Syscall::StringArgument, 32> strings;
        strings.resize(list.length);
        if (!copy_from_user(strings.data(), list.strings, list.length * sizeof(*list.strings)))
            return false;
        for (size_t i = 0; i < list.length; ++i) {
            auto string = copy_string_from_user(strings[i]);
            if (string.is_null())
                return false;
            output.append(move(string));
        }
        return true;
    };

    Vector<String> arguments;
    if (!copy_user_strings(params.arguments, arguments))
        return -EFAULT;

    Vector<String> environment;
    if (!copy_user_strings(params.environment, environment))
        return -EFAULT;

    int rc = exec(move(path), move(arguments), move(environment));
    ASSERT(rc < 0); // We should never continue after a successful exec!
    return rc;
}

}
