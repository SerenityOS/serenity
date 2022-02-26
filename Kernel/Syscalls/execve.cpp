/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/WeakPtr.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Panic.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <LibC/limits.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

namespace Kernel {

extern Memory::Region* g_signal_trampoline_region;

struct LoadResult {
    OwnPtr<Memory::AddressSpace> space;
    FlatPtr load_base { 0 };
    FlatPtr entry_eip { 0 };
    size_t size { 0 };
    WeakPtr<Memory::Region> tls_region;
    size_t tls_size { 0 };
    size_t tls_alignment { 0 };
    WeakPtr<Memory::Region> stack_region;
};

static constexpr size_t auxiliary_vector_size = 15;
static Array<ELF::AuxiliaryValue, auxiliary_vector_size> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, UserID uid, UserID euid, GroupID gid, GroupID egid, StringView executable_path, Optional<Process::ScopedDescriptionAllocation> const& main_program_fd_allocation);

static bool validate_stack_size(NonnullOwnPtrVector<KString> const& arguments, NonnullOwnPtrVector<KString>& environment)
{
    size_t total_arguments_size = 0;
    size_t total_environment_size = 0;

    for (auto const& a : arguments)
        total_arguments_size += a.length() + 1;
    for (auto const& e : environment)
        total_environment_size += e.length() + 1;

    total_arguments_size += sizeof(char*) * (arguments.size() + 1);
    total_environment_size += sizeof(char*) * (environment.size() + 1);

    if (total_arguments_size > Process::max_arguments_size)
        return false;

    if (total_environment_size > Process::max_environment_size)
        return false;

    // FIXME: This doesn't account for the size of the auxiliary vector
    return true;
}

static ErrorOr<FlatPtr> make_userspace_context_for_main_thread([[maybe_unused]] ThreadRegisters& regs, Memory::Region& region, NonnullOwnPtrVector<KString> const& arguments,
    NonnullOwnPtrVector<KString> const& environment, Array<ELF::AuxiliaryValue, auxiliary_vector_size> auxiliary_values)
{
    FlatPtr new_sp = region.range().end().get();

    // Add some bits of randomness to the user stack pointer.
    new_sp -= round_up_to_power_of_two(get_fast_random<u32>() % 4096, 16);

    auto push_on_new_stack = [&new_sp](FlatPtr value) {
        new_sp -= sizeof(FlatPtr);
        Userspace<FlatPtr*> stack_ptr = new_sp;
        auto result = copy_to_user(stack_ptr, &value);
        VERIFY(!result.is_error());
    };

    auto push_aux_value_on_new_stack = [&new_sp](auxv_t value) {
        new_sp -= sizeof(auxv_t);
        Userspace<auxv_t*> stack_ptr = new_sp;
        auto result = copy_to_user(stack_ptr, &value);
        VERIFY(!result.is_error());
    };

    auto push_string_on_new_stack = [&new_sp](StringView string) {
        new_sp -= round_up_to_power_of_two(string.length() + 1, sizeof(FlatPtr));
        Userspace<FlatPtr*> stack_ptr = new_sp;
        auto result = copy_to_user(stack_ptr, string.characters_without_null_termination(), string.length() + 1);
        VERIFY(!result.is_error());
    };

    Vector<FlatPtr> argv_entries;
    for (auto const& argument : arguments) {
        push_string_on_new_stack(argument.view());
        TRY(argv_entries.try_append(new_sp));
    }

    Vector<FlatPtr> env_entries;
    for (auto const& variable : environment) {
        push_string_on_new_stack(variable.view());
        TRY(env_entries.try_append(new_sp));
    }

    for (auto& value : auxiliary_values) {
        if (!value.optional_string.is_empty()) {
            push_string_on_new_stack(value.optional_string);
            value.auxv.a_un.a_ptr = (void*)new_sp;
        }
        if (value.auxv.a_type == ELF::AuxiliaryValue::Random) {
            u8 random_bytes[16] {};
            get_fast_random_bytes({ random_bytes, sizeof(random_bytes) });
            push_string_on_new_stack({ random_bytes, sizeof(random_bytes) });
            value.auxv.a_un.a_ptr = (void*)new_sp;
        }
    }

    for (ssize_t i = auxiliary_values.size() - 1; i >= 0; --i) {
        auto& value = auxiliary_values[i];
        push_aux_value_on_new_stack(value.auxv);
    }

    push_on_new_stack(0);
    for (ssize_t i = env_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(env_entries[i]);
    FlatPtr envp = new_sp;

    push_on_new_stack(0);
    for (ssize_t i = argv_entries.size() - 1; i >= 0; --i)
        push_on_new_stack(argv_entries[i]);
    FlatPtr argv = new_sp;

    // NOTE: The stack needs to be 16-byte aligned.
    new_sp -= new_sp % 16;

#if ARCH(I386)
    // GCC assumes that the return address has been pushed to the stack when it enters the function,
    // so we need to reserve an extra pointer's worth of bytes below this to make GCC's stack alignment
    // calculations work
    new_sp -= sizeof(void*);

    push_on_new_stack(envp);
    push_on_new_stack(argv);
    push_on_new_stack(argv_entries.size());
#else
    regs.rdi = argv_entries.size();
    regs.rsi = argv;
    regs.rdx = envp;
#endif

    VERIFY(new_sp % 16 == 0);

    // FIXME: The way we're setting up the stack and passing arguments to the entry point isn't ABI-compliant
    return new_sp;
}

struct RequiredLoadRange {
    FlatPtr start { 0 };
    FlatPtr end { 0 };
};

static ErrorOr<RequiredLoadRange> get_required_load_range(OpenFileDescription& program_description)
{
    auto& inode = *(program_description.inode());
    auto vmobject = TRY(Memory::SharedInodeVMObject::try_create_with_inode(inode));

    size_t executable_size = inode.size();
    size_t rounded_executable_size = TRY(Memory::page_round_up(executable_size));
    auto region = TRY(MM.allocate_kernel_region_with_vmobject(*vmobject, rounded_executable_size, "ELF memory range calculation", Memory::Region::Access::Read));
    auto elf_image = ELF::Image(region->vaddr().as_ptr(), executable_size);
    if (!elf_image.is_valid()) {
        return EINVAL;
    }

    RequiredLoadRange range {};
    elf_image.for_each_program_header([&range](const auto& pheader) {
        if (pheader.type() != PT_LOAD)
            return;

        auto region_start = (FlatPtr)pheader.vaddr().as_ptr();
        auto region_end = region_start + pheader.size_in_memory();
        if (range.start == 0 || region_start < range.start)
            range.start = region_start;
        if (range.end == 0 || region_end > range.end)
            range.end = region_end;
    });

    VERIFY(range.end > range.start);
    return range;
};

static ErrorOr<FlatPtr> get_load_offset(const ElfW(Ehdr) & main_program_header, OpenFileDescription& main_program_description, OpenFileDescription* interpreter_description)
{
    constexpr FlatPtr load_range_start = 0x08000000;
    constexpr FlatPtr load_range_size = 65536 * PAGE_SIZE; // 2**16 * PAGE_SIZE = 256MB
    constexpr FlatPtr minimum_load_offset_randomization_size = 10 * MiB;

    auto random_load_offset_in_range([](auto start, auto size) {
        return Memory::page_round_down(start + get_good_random<FlatPtr>() % size);
    });

    if (main_program_header.e_type == ET_DYN) {
        return random_load_offset_in_range(load_range_start, load_range_size);
    }

    if (main_program_header.e_type != ET_EXEC)
        return EINVAL;

    auto main_program_load_range = TRY(get_required_load_range(main_program_description));

    RequiredLoadRange selected_range {};

    if (interpreter_description) {
        auto interpreter_load_range = TRY(get_required_load_range(*interpreter_description));

        auto interpreter_size_in_memory = interpreter_load_range.end - interpreter_load_range.start;
        auto interpreter_load_range_end = load_range_start + load_range_size - interpreter_size_in_memory;

        // No intersection
        if (main_program_load_range.end < load_range_start || main_program_load_range.start > interpreter_load_range_end)
            return random_load_offset_in_range(load_range_start, load_range_size);

        RequiredLoadRange first_available_part = { load_range_start, main_program_load_range.start };
        RequiredLoadRange second_available_part = { main_program_load_range.end, interpreter_load_range_end };

        // Select larger part
        if (first_available_part.end - first_available_part.start > second_available_part.end - second_available_part.start)
            selected_range = first_available_part;
        else
            selected_range = second_available_part;
    } else
        selected_range = main_program_load_range;

    // If main program is too big and leaves us without enough space for adequate loader randomization
    if (selected_range.end - selected_range.start < minimum_load_offset_randomization_size)
        return E2BIG;

    return random_load_offset_in_range(selected_range.start, selected_range.end - selected_range.start);
}

enum class ShouldAllocateTls {
    No,
    Yes,
};

enum class ShouldAllowSyscalls {
    No,
    Yes,
};

static ErrorOr<LoadResult> load_elf_object(NonnullOwnPtr<Memory::AddressSpace> new_space, OpenFileDescription& object_description,
    FlatPtr load_offset, ShouldAllocateTls should_allocate_tls, ShouldAllowSyscalls should_allow_syscalls)
{
    auto& inode = *(object_description.inode());
    auto vmobject = TRY(Memory::SharedInodeVMObject::try_create_with_inode(inode));

    if (vmobject->writable_mappings()) {
        dbgln("Refusing to execute a write-mapped program");
        return ETXTBSY;
    }

    size_t executable_size = inode.size();
    size_t rounded_executable_size = TRY(Memory::page_round_up(executable_size));

    auto executable_region = TRY(MM.allocate_kernel_region_with_vmobject(*vmobject, rounded_executable_size, "ELF loading", Memory::Region::Access::Read));
    auto elf_image = ELF::Image(executable_region->vaddr().as_ptr(), executable_size);

    if (!elf_image.is_valid())
        return ENOEXEC;

    Memory::Region* master_tls_region { nullptr };
    size_t master_tls_size = 0;
    size_t master_tls_alignment = 0;
    FlatPtr load_base_address = 0;

    auto elf_name = TRY(object_description.pseudo_path());
    VERIFY(!Processor::in_critical());

    Memory::MemoryManager::enter_address_space(*new_space);

    auto load_tls_section = [&](auto& program_header) -> ErrorOr<void> {
        VERIFY(should_allocate_tls == ShouldAllocateTls::Yes);
        VERIFY(program_header.size_in_memory());

        if (!elf_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
            dbgln("Shenanigans! ELF PT_TLS header sneaks outside of executable.");
            return ENOEXEC;
        }

        auto range = TRY(new_space->try_allocate_range({}, program_header.size_in_memory()));
        auto region_name = TRY(KString::formatted("{} (master-tls)", elf_name));
        master_tls_region = TRY(new_space->allocate_region(range, region_name->view(), PROT_READ | PROT_WRITE, AllocationStrategy::Reserve));
        master_tls_size = program_header.size_in_memory();
        master_tls_alignment = program_header.alignment();

        TRY(copy_to_user(master_tls_region->vaddr().as_ptr(), program_header.raw_data(), program_header.size_in_image()));
        return {};
    };

    auto load_writable_section = [&](auto& program_header) -> ErrorOr<void> {
        // Writable section: create a copy in memory.
        VERIFY(program_header.alignment() % PAGE_SIZE == 0);

        if (!elf_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
            dbgln("Shenanigans! Writable ELF PT_LOAD header sneaks outside of executable.");
            return ENOEXEC;
        }

        int prot = 0;
        if (program_header.is_readable())
            prot |= PROT_READ;
        if (program_header.is_writable())
            prot |= PROT_WRITE;
        auto region_name = TRY(KString::formatted("{} (data-{}{})", elf_name, program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : ""));

        auto range_base = VirtualAddress { Memory::page_round_down(program_header.vaddr().offset(load_offset).get()) };
        size_t rounded_range_end = TRY(Memory::page_round_up(program_header.vaddr().offset(load_offset).offset(program_header.size_in_memory()).get()));
        auto range_end = VirtualAddress { rounded_range_end };

        auto range = TRY(new_space->try_allocate_range(range_base, range_end.get() - range_base.get(), program_header.alignment()));
        auto region = TRY(new_space->allocate_region(range, region_name->view(), prot, AllocationStrategy::Reserve));

        // It's not always the case with PIE executables (and very well shouldn't be) that the
        // virtual address in the program header matches the one we end up giving the process.
        // In order to copy the data image correctly into memory, we need to copy the data starting at
        // the right initial page offset into the pages allocated for the elf_alloc-XX section.
        // FIXME: There's an opportunity to munmap, or at least mprotect, the padding space between
        //     the .text and .data PT_LOAD sections of the executable.
        //     Accessing it would definitely be a bug.
        auto page_offset = program_header.vaddr();
        page_offset.mask(~PAGE_MASK);
        TRY(copy_to_user((u8*)region->vaddr().as_ptr() + page_offset.get(), program_header.raw_data(), program_header.size_in_image()));
        return {};
    };

    auto load_section = [&](auto& program_header) -> ErrorOr<void> {
        if (program_header.size_in_memory() == 0)
            return {};

        if (program_header.is_writable())
            return load_writable_section(program_header);

        // Non-writable section: map the executable itself in memory.
        VERIFY(program_header.alignment() % PAGE_SIZE == 0);
        int prot = 0;
        if (program_header.is_readable())
            prot |= PROT_READ;
        if (program_header.is_writable())
            prot |= PROT_WRITE;
        if (program_header.is_executable())
            prot |= PROT_EXEC;

        auto range_base = VirtualAddress { Memory::page_round_down(program_header.vaddr().offset(load_offset).get()) };
        size_t rounded_range_end = TRY(Memory::page_round_up(program_header.vaddr().offset(load_offset).offset(program_header.size_in_memory()).get()));
        auto range_end = VirtualAddress { rounded_range_end };
        auto range = TRY(new_space->try_allocate_range(range_base, range_end.get() - range_base.get(), program_header.alignment()));
        auto region = TRY(new_space->allocate_region_with_vmobject(range, *vmobject, program_header.offset(), elf_name->view(), prot, true));

        if (should_allow_syscalls == ShouldAllowSyscalls::Yes)
            region->set_syscall_region(true);
        if (program_header.offset() == 0)
            load_base_address = (FlatPtr)region->vaddr().as_ptr();
        return {};
    };

    auto load_elf_program_header = [&](auto& program_header) -> ErrorOr<void> {
        if (program_header.type() == PT_TLS)
            return load_tls_section(program_header);

        if (program_header.type() == PT_LOAD)
            return load_section(program_header);

        // NOTE: We ignore other program header types.
        return {};
    };

    TRY([&] {
        ErrorOr<void> result;
        elf_image.for_each_program_header([&](ELF::Image::ProgramHeader const& program_header) {
            result = load_elf_program_header(program_header);
            return result.is_error() ? IterationDecision::Break : IterationDecision::Continue;
        });
        return result;
    }());

    if (!elf_image.entry().offset(load_offset).get()) {
        dbgln("do_exec: Failure loading program, entry pointer is invalid! {})", elf_image.entry().offset(load_offset));
        return ENOEXEC;
    }

    auto stack_range = TRY(new_space->try_allocate_range({}, Thread::default_userspace_stack_size));
    auto* stack_region = TRY(new_space->allocate_region(stack_range, "Stack (Main thread)", PROT_READ | PROT_WRITE, AllocationStrategy::Reserve));
    stack_region->set_stack(true);

    return LoadResult {
        move(new_space),
        load_base_address,
        elf_image.entry().offset(load_offset).get(),
        executable_size,
        TRY(AK::try_make_weak_ptr_if_nonnull(master_tls_region)),
        master_tls_size,
        master_tls_alignment,
        TRY(stack_region->try_make_weak_ptr())
    };
}

ErrorOr<LoadResult>
Process::load(NonnullRefPtr<OpenFileDescription> main_program_description,
    RefPtr<OpenFileDescription> interpreter_description, const ElfW(Ehdr) & main_program_header)
{
    auto new_space = TRY(Memory::AddressSpace::try_create(nullptr));

    ScopeGuard space_guard([&]() {
        Memory::MemoryManager::enter_process_address_space(*this);
    });

    auto load_offset = TRY(get_load_offset(main_program_header, main_program_description, interpreter_description));

    if (interpreter_description.is_null()) {
        auto load_result = TRY(load_elf_object(move(new_space), main_program_description, load_offset, ShouldAllocateTls::Yes, ShouldAllowSyscalls::No));
        m_master_tls_region = load_result.tls_region;
        m_master_tls_size = load_result.tls_size;
        m_master_tls_alignment = load_result.tls_alignment;
        return load_result;
    }

    auto interpreter_load_result = TRY(load_elf_object(move(new_space), *interpreter_description, load_offset, ShouldAllocateTls::No, ShouldAllowSyscalls::Yes));

    // TLS allocation will be done in userspace by the loader
    VERIFY(!interpreter_load_result.tls_region);
    VERIFY(!interpreter_load_result.tls_alignment);
    VERIFY(!interpreter_load_result.tls_size);

    return interpreter_load_result;
}

ErrorOr<void> Process::do_exec(NonnullRefPtr<OpenFileDescription> main_program_description, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment,
    RefPtr<OpenFileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags, const ElfW(Ehdr) & main_program_header)
{
    VERIFY(is_user_process());
    VERIFY(!Processor::in_critical());
    // Although we *could* handle a pseudo_path here, trying to execute something that doesn't have
    // a custody (e.g. BlockDevice or RandomDevice) is pretty suspicious anyway.
    auto path = TRY(main_program_description->original_absolute_path());

    dbgln_if(EXEC_DEBUG, "do_exec: {}", path);

    // FIXME: How much stack space does process startup need?
    if (!validate_stack_size(arguments, environment))
        return E2BIG;

    // FIXME: split_view() currently allocates (Vector) without checking for failure.
    auto parts = path->view().split_view('/');
    if (parts.is_empty())
        return ENOENT;

    auto new_process_name = TRY(KString::try_create(parts.last()));
    auto new_main_thread_name = TRY(new_process_name->try_clone());

    auto load_result = TRY(load(main_program_description, interpreter_description, main_program_header));

    // NOTE: We don't need the interpreter executable description after this point.
    //       We destroy it here to prevent it from getting destroyed when we return from this function.
    //       That's important because when we're returning from this function, we're in a very delicate
    //       state where we can't block (e.g by trying to acquire a mutex in description teardown.)
    bool has_interpreter = interpreter_description;
    interpreter_description = nullptr;

    auto signal_trampoline_range = TRY(load_result.space->try_allocate_range({}, PAGE_SIZE));
    auto* signal_trampoline_region = TRY(load_result.space->allocate_region_with_vmobject(signal_trampoline_range, g_signal_trampoline_region->vmobject(), 0, "Signal trampoline", PROT_READ | PROT_EXEC, true));
    signal_trampoline_region->set_syscall_region(true);

    // (For dynamically linked executable) Allocate an FD for passing the main executable to the dynamic loader.
    Optional<ScopedDescriptionAllocation> main_program_fd_allocation;
    if (has_interpreter)
        main_program_fd_allocation = TRY(allocate_fd());

    // We commit to the new executable at this point. There is no turning back!

    // Prevent other processes from attaching to us with ptrace while we're doing this.
    MutexLocker ptrace_locker(ptrace_lock());

    // Disable profiling temporarily in case it's running on this process.
    auto was_profiling = m_profiling;
    TemporaryChange profiling_disabler(m_profiling, false);

    kill_threads_except_self();

    bool executable_is_setid = false;

    if (!(main_program_description->custody()->mount_flags() & MS_NOSUID)) {
        auto main_program_metadata = main_program_description->metadata();
        if (main_program_metadata.is_setuid()) {
            executable_is_setid = true;
            ProtectedDataMutationScope scope { *this };
            m_protected_values.euid = main_program_metadata.uid;
            m_protected_values.suid = main_program_metadata.uid;
        }
        if (main_program_metadata.is_setgid()) {
            executable_is_setid = true;
            ProtectedDataMutationScope scope { *this };
            m_protected_values.egid = main_program_metadata.gid;
            m_protected_values.sgid = main_program_metadata.gid;
        }
    }

    set_dumpable(!executable_is_setid);

    // We make sure to enter the new address space before destroying the old one.
    // This ensures that the process always has a valid page directory.
    Memory::MemoryManager::enter_address_space(*load_result.space);

    m_space = load_result.space.release_nonnull();

    m_executable = main_program_description->custody();
    m_arguments = move(arguments);
    m_environment = move(environment);

    m_veil_state = VeilState::None;
    m_unveiled_paths.clear();
    m_unveiled_paths.set_metadata({ TRY(KString::try_create("/"sv)), UnveilAccess::None, false });

    for (auto& property : m_coredump_properties)
        property = {};

    auto* current_thread = Thread::current();
    current_thread->reset_signals_for_exec();

    clear_futex_queues_on_exec();

    m_fds.with_exclusive([&](auto& fds) {
        fds.change_each([&](auto& file_description_metadata) {
            if (file_description_metadata.is_valid() && file_description_metadata.flags() & FD_CLOEXEC)
                file_description_metadata = {};
        });
    });

    if (main_program_fd_allocation.has_value()) {
        main_program_description->set_readable(true);
        m_fds.with_exclusive([&](auto& fds) { fds[main_program_fd_allocation->fd].set(move(main_program_description), FD_CLOEXEC); });
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
    VERIFY(new_main_thread);

    auto auxv = generate_auxiliary_vector(load_result.load_base, load_result.entry_eip, uid(), euid(), gid(), egid(), path->view(), main_program_fd_allocation);

    // NOTE: We create the new stack before disabling interrupts since it will zero-fault
    //       and we don't want to deal with faults after this point.
    auto new_userspace_sp = TRY(make_userspace_context_for_main_thread(new_main_thread->regs(), *load_result.stack_region.unsafe_ptr(), m_arguments, m_environment, move(auxv)));

    m_name = move(new_process_name);
    new_main_thread->set_name(move(new_main_thread_name));

    if (wait_for_tracer_at_next_execve()) {
        // Make sure we release the ptrace lock here or the tracer will block forever.
        ptrace_locker.unlock();
        Thread::current()->send_urgent_signal_to_self(SIGSTOP);
    } else {
        // Unlock regardless before disabling interrupts.
        // Ensure we always unlock after checking ptrace status to avoid TOCTOU ptrace issues
        ptrace_locker.unlock();
    }

    // We enter a critical section here because we don't want to get interrupted between do_exec()
    // and Processor::assume_context() or the next context switch.
    // If we used an InterruptDisabler that sti()'d on exit, we might timer tick'd too soon in exec().
    Processor::enter_critical();
    prev_flags = cpu_flags();
    cli();

    // NOTE: Be careful to not trigger any page faults below!

    {
        ProtectedDataMutationScope scope { *this };
        m_protected_values.promises = m_protected_values.execpromises.load();
        m_protected_values.has_promises = m_protected_values.has_execpromises.load();

        m_protected_values.execpromises = 0;
        m_protected_values.has_execpromises = false;

        m_protected_values.signal_trampoline = signal_trampoline_region->vaddr();

        // FIXME: PID/TID ISSUE
        m_protected_values.pid = new_main_thread->tid().value();
    }

    auto tsr_result = new_main_thread->make_thread_specific_region({});
    if (tsr_result.is_error()) {
        // FIXME: We cannot fail this late. Refactor this so the allocation happens before we commit to the new executable.
        VERIFY_NOT_REACHED();
    }
    new_main_thread->reset_fpu_state();

    auto& regs = new_main_thread->m_regs;
    regs.cs = GDT_SELECTOR_CODE3 | 3;
#if ARCH(I386)
    regs.ds = GDT_SELECTOR_DATA3 | 3;
    regs.es = GDT_SELECTOR_DATA3 | 3;
    regs.ss = GDT_SELECTOR_DATA3 | 3;
    regs.fs = GDT_SELECTOR_DATA3 | 3;
    regs.gs = GDT_SELECTOR_TLS | 3;
    regs.eip = load_result.entry_eip;
    regs.esp = new_userspace_sp;
#else
    regs.rip = load_result.entry_eip;
    regs.rsp = new_userspace_sp;
#endif
    regs.cr3 = address_space().page_directory().cr3();

    {
        TemporaryChange profiling_disabler(m_profiling, was_profiling);
        PerformanceManager::add_process_exec_event(*this);
    }

    u32 lock_count_to_restore;
    [[maybe_unused]] auto rc = big_lock().force_unlock_exclusive_if_locked(lock_count_to_restore);
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::in_critical());
    return {};
}

static Array<ELF::AuxiliaryValue, auxiliary_vector_size> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, UserID uid, UserID euid, GroupID gid, GroupID egid, StringView executable_path, Optional<Process::ScopedDescriptionAllocation> const& main_program_fd_allocation)
{
    return { {
        // PHDR/EXECFD
        // PH*
        { ELF::AuxiliaryValue::PageSize, PAGE_SIZE },
        { ELF::AuxiliaryValue::BaseAddress, (void*)load_base },

        { ELF::AuxiliaryValue::Entry, (void*)entry_eip },
        // NOTELF
        { ELF::AuxiliaryValue::Uid, (long)uid.value() },
        { ELF::AuxiliaryValue::EUid, (long)euid.value() },
        { ELF::AuxiliaryValue::Gid, (long)gid.value() },
        { ELF::AuxiliaryValue::EGid, (long)egid.value() },

        { ELF::AuxiliaryValue::Platform, Processor::platform_string() },
        // FIXME: This is platform specific
        { ELF::AuxiliaryValue::HwCap, (long)CPUID(1).edx() },

        { ELF::AuxiliaryValue::ClockTick, (long)TimeManagement::the().ticks_per_second() },

        // FIXME: Also take into account things like extended filesystem permissions? That's what linux does...
        { ELF::AuxiliaryValue::Secure, ((uid != euid) || (gid != egid)) ? 1 : 0 },

        { ELF::AuxiliaryValue::Random, nullptr },

        { ELF::AuxiliaryValue::ExecFilename, executable_path },

        main_program_fd_allocation.has_value() ? ELF::AuxiliaryValue { ELF::AuxiliaryValue::ExecFileDescriptor, main_program_fd_allocation->fd } : ELF::AuxiliaryValue { ELF::AuxiliaryValue::Ignore, 0L },

        { ELF::AuxiliaryValue::Null, 0L },
    } };
}

static ErrorOr<NonnullOwnPtrVector<KString>> find_shebang_interpreter_for_executable(char const first_page[], size_t nread)
{
    int word_start = 2;
    size_t word_length = 0;
    if (nread > 2 && first_page[0] == '#' && first_page[1] == '!') {
        NonnullOwnPtrVector<KString> interpreter_words;

        for (size_t i = 2; i < nread; ++i) {
            if (first_page[i] == '\n') {
                break;
            }

            if (first_page[i] != ' ') {
                ++word_length;
            }

            if (first_page[i] == ' ') {
                if (word_length > 0) {
                    auto word = TRY(KString::try_create(StringView { &first_page[word_start], word_length }));
                    TRY(interpreter_words.try_append(move(word)));
                }
                word_length = 0;
                word_start = i + 1;
            }
        }

        if (word_length > 0) {
            auto word = TRY(KString::try_create(StringView { &first_page[word_start], word_length }));
            TRY(interpreter_words.try_append(move(word)));
        }

        if (!interpreter_words.is_empty())
            return interpreter_words;
    }

    return ENOEXEC;
}

ErrorOr<RefPtr<OpenFileDescription>> Process::find_elf_interpreter_for_executable(StringView path, ElfW(Ehdr) const& main_executable_header, size_t main_executable_header_size, size_t file_size)
{
    // Not using ErrorOr here because we'll want to do the same thing in userspace in the RTLD
    StringBuilder interpreter_path_builder;
    if (!TRY(ELF::validate_program_headers(main_executable_header, file_size, { &main_executable_header, main_executable_header_size }, &interpreter_path_builder))) {
        dbgln("exec({}): File has invalid ELF Program headers", path);
        return ENOEXEC;
    }
    auto interpreter_path = interpreter_path_builder.string_view();

    if (!interpreter_path.is_empty()) {
        dbgln_if(EXEC_DEBUG, "exec({}): Using program interpreter {}", path, interpreter_path);
        auto interpreter_description = TRY(VirtualFileSystem::the().open(interpreter_path, O_EXEC, 0, current_directory()));
        auto interp_metadata = interpreter_description->metadata();

        VERIFY(interpreter_description->inode());

        // Validate the program interpreter as a valid elf binary.
        // If your program interpreter is a #! file or something, it's time to stop playing games :)
        if (interp_metadata.size < (int)sizeof(ElfW(Ehdr)))
            return ENOEXEC;

        char first_page[PAGE_SIZE] = {};
        auto first_page_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&first_page);
        auto nread = TRY(interpreter_description->read(first_page_buffer, sizeof(first_page)));

        if (nread < sizeof(ElfW(Ehdr)))
            return ENOEXEC;

        auto* elf_header = (ElfW(Ehdr)*)first_page;
        if (!ELF::validate_elf_header(*elf_header, interp_metadata.size)) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF header", path, interpreter_path);
            return ENOEXEC;
        }

        // Not using ErrorOr here because we'll want to do the same thing in userspace in the RTLD
        StringBuilder interpreter_interpreter_path_builder;
        if (!TRY(ELF::validate_program_headers(*elf_header, interp_metadata.size, { first_page, nread }, &interpreter_interpreter_path_builder))) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF Program headers", path, interpreter_path);
            return ENOEXEC;
        }
        auto interpreter_interpreter_path = interpreter_interpreter_path_builder.string_view();

        if (!interpreter_interpreter_path.is_empty()) {
            dbgln("exec({}): Interpreter ({}) has its own interpreter ({})! No thank you!", path, interpreter_path, interpreter_interpreter_path);
            return ELOOP;
        }

        return interpreter_description;
    }

    if (main_executable_header.e_type == ET_REL) {
        // We can't exec an ET_REL, that's just an object file from the compiler
        return ENOEXEC;
    }
    if (main_executable_header.e_type == ET_DYN) {
        // If it's ET_DYN with no PT_INTERP, then it's a dynamic executable responsible
        // for its own relocation (i.e. it's /usr/lib/Loader.so)
        if (path != "/usr/lib/Loader.so")
            dbgln("exec({}): WARNING - Dynamic ELF executable without a PT_INTERP header, and isn't /usr/lib/Loader.so", path);
        return nullptr;
    }

    // No interpreter, but, path refers to a valid elf image
    return nullptr;
}

ErrorOr<void> Process::exec(NonnullOwnPtr<KString> path, NonnullOwnPtrVector<KString> arguments, NonnullOwnPtrVector<KString> environment, Thread*& new_main_thread, u32& prev_flags, int recursion_depth)
{
    if (recursion_depth > 2) {
        dbgln("exec({}): SHENANIGANS! recursed too far trying to find #! interpreter", path);
        return ELOOP;
    }

    // Open the file to check what kind of binary format it is
    // Currently supported formats:
    //    - #! interpreted file
    //    - ELF32
    //        * ET_EXEC binary that just gets loaded
    //        * ET_DYN binary that requires a program interpreter
    //
    auto description = TRY(VirtualFileSystem::the().open(path->view(), O_EXEC, 0, current_directory()));
    auto metadata = description->metadata();

    if (!metadata.is_regular_file())
        return EACCES;

    // Always gonna need at least 3 bytes. these are for #!X
    if (metadata.size < 3)
        return ENOEXEC;

    VERIFY(description->inode());

    // Read the first page of the program into memory so we can validate the binfmt of it
    char first_page[PAGE_SIZE];
    auto first_page_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&first_page);
    auto nread = TRY(description->read(first_page_buffer, sizeof(first_page)));

    // 1) #! interpreted file
    auto shebang_result = find_shebang_interpreter_for_executable(first_page, nread);
    if (!shebang_result.is_error()) {
        auto shebang_words = shebang_result.release_value();
        auto shebang_path = TRY(shebang_words.first().try_clone());
        arguments.ptr_at(0) = move(path);
        TRY(arguments.try_prepend(move(shebang_words)));
        return exec(move(shebang_path), move(arguments), move(environment), new_main_thread, prev_flags, ++recursion_depth);
    }

    // #2) ELF32 for i386

    if (nread < sizeof(ElfW(Ehdr)))
        return ENOEXEC;
    auto const* main_program_header = (ElfW(Ehdr)*)first_page;

    if (!ELF::validate_elf_header(*main_program_header, metadata.size)) {
        dbgln("exec({}): File has invalid ELF header", path);
        return ENOEXEC;
    }

    auto interpreter_description = TRY(find_elf_interpreter_for_executable(path->view(), *main_program_header, nread, metadata.size));
    return do_exec(move(description), move(arguments), move(environment), move(interpreter_description), new_main_thread, prev_flags, *main_program_header);
}

ErrorOr<FlatPtr> Process::sys$execve(Userspace<const Syscall::SC_execve_params*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::exec));

    Thread* new_main_thread = nullptr;
    u32 prev_flags = 0;

    // NOTE: Be extremely careful with allocating any kernel memory in this function.
    //       On success, the kernel stack will be lost.
    //       The explicit block scope below is specifically placed to minimize the number
    //       of stack locals in this function.
    {
        auto params = TRY(copy_typed_from_user(user_params));

        if (params.arguments.length > ARG_MAX || params.environment.length > ARG_MAX)
            return E2BIG;

        // NOTE: The caller is expected to always pass at least one argument by convention,
        //       the program path that was passed as params.path.
        if (params.arguments.length == 0)
            return EINVAL;

        auto path = TRY(get_syscall_path_argument(params.path));

        auto copy_user_strings = [](const auto& list, auto& output) -> ErrorOr<void> {
            if (!list.length)
                return {};
            Checked<size_t> size = sizeof(*list.strings);
            size *= list.length;
            if (size.has_overflow())
                return EOVERFLOW;
            Vector<Syscall::StringArgument, 32> strings;
            TRY(strings.try_resize(list.length));
            TRY(copy_from_user(strings.data(), list.strings, size.value()));
            for (size_t i = 0; i < list.length; ++i) {
                auto string = TRY(try_copy_kstring_from_user(strings[i]));
                TRY(output.try_append(move(string)));
            }
            return {};
        };

        NonnullOwnPtrVector<KString> arguments;
        TRY(copy_user_strings(params.arguments, arguments));

        NonnullOwnPtrVector<KString> environment;
        TRY(copy_user_strings(params.environment, environment));

        TRY(exec(move(path), move(arguments), move(environment), new_main_thread, prev_flags));
    }

    // NOTE: If we're here, the exec has succeeded and we've got a new executable image!
    //       We will not return normally from this function. Instead, the next time we
    //       get scheduled, it'll be at the entry point of the new executable.

    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::in_critical());

    auto* current_thread = Thread::current();
    if (current_thread == new_main_thread) {
        // We need to enter the scheduler lock before changing the state
        // and it will be released after the context switch into that
        // thread. We should also still be in our critical section
        VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
        VERIFY(Processor::in_critical() == 1);
        g_scheduler_lock.lock();
        current_thread->set_state(Thread::State::Running);
        Processor::assume_context(*current_thread, prev_flags);
        VERIFY_NOT_REACHED();
    }

    // NOTE: This code path is taken in the non-syscall case, i.e when the kernel spawns
    //       a userspace process directly (such as /bin/SystemServer on startup)

    if (prev_flags & 0x200)
        sti();
    Processor::leave_critical();
    return 0;
}

}
