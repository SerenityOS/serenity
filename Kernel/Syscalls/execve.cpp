/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ScopedProcessList.h>
#include <Kernel/Time/TimeManagement.h>
#include <LibELF/AuxiliaryVector.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

namespace Kernel {

extern Memory::Region* g_signal_trampoline_region;

struct LoadResult {
    FlatPtr load_base { 0 };
    FlatPtr entry_eip { 0 };
    size_t size { 0 };
    LockWeakPtr<Memory::Region> stack_region;
};

static constexpr size_t auxiliary_vector_size = 15;
static Array<ELF::AuxiliaryValue, auxiliary_vector_size> generate_auxiliary_vector(FlatPtr load_base, FlatPtr entry_eip, UserID uid, UserID euid, GroupID gid, GroupID egid, StringView executable_path, Optional<Process::ScopedDescriptionAllocation> const& main_program_fd_allocation);

static bool validate_stack_size(Vector<NonnullOwnPtr<KString>> const& arguments, Vector<NonnullOwnPtr<KString>>& environment, Array<ELF::AuxiliaryValue, auxiliary_vector_size> const& auxiliary)
{
    size_t total_arguments_size = 0;
    size_t total_environment_size = 0;
    size_t total_auxiliary_size = 0;

    for (auto const& a : arguments)
        total_arguments_size += a->length() + 1;
    for (auto const& e : environment)
        total_environment_size += e->length() + 1;
    for (auto const& v : auxiliary) {
        if (!v.optional_string.is_empty())
            total_auxiliary_size += round_up_to_power_of_two(v.optional_string.length() + 1, sizeof(FlatPtr));

        if (v.auxv.a_type == ELF::AuxiliaryValue::Random)
            total_auxiliary_size += round_up_to_power_of_two(16, sizeof(FlatPtr));
    }

    total_arguments_size += sizeof(char*) * (arguments.size() + 1);
    total_environment_size += sizeof(char*) * (environment.size() + 1);
    total_auxiliary_size += sizeof(auxv_t) * auxiliary.size();

    if (total_arguments_size > Process::max_arguments_size)
        return false;

    if (total_environment_size > Process::max_environment_size)
        return false;

    if (total_auxiliary_size > Process::max_auxiliary_size)
        return false;

    return true;
}

static ErrorOr<FlatPtr> make_userspace_context_for_main_thread([[maybe_unused]] ThreadRegisters& regs, Memory::Region& region, Vector<NonnullOwnPtr<KString>> const& arguments,
    Vector<NonnullOwnPtr<KString>> const& environment, Array<ELF::AuxiliaryValue, auxiliary_vector_size> auxiliary_values)
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
        push_string_on_new_stack(argument->view());
        TRY(argv_entries.try_append(new_sp));
    }

    Vector<FlatPtr> env_entries;
    for (auto const& variable : environment) {
        push_string_on_new_stack(variable->view());
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

#if ARCH(X86_64)
    regs.rdi = argv_entries.size();
    regs.rsi = argv;
    regs.rdx = envp;
#elif ARCH(AARCH64)
    regs.x[0] = argv_entries.size();
    regs.x[1] = argv;
    regs.x[2] = envp;
#elif ARCH(RISCV64)
    regs.x[9] = argv_entries.size();
    regs.x[10] = argv;
    regs.x[11] = envp;
#else
#    error Unknown architecture
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
    auto region = TRY(MM.allocate_kernel_region_with_vmobject(*vmobject, rounded_executable_size, "ELF memory range calculation"sv, Memory::Region::Access::Read));
    auto elf_image = ELF::Image(region->vaddr().as_ptr(), executable_size);
    if (!elf_image.is_valid()) {
        return EINVAL;
    }

    RequiredLoadRange range {};
    elf_image.for_each_program_header([&range](auto const& pheader) {
        if (pheader.type() != PT_LOAD)
            return;

        auto region_start = (FlatPtr)pheader.vaddr().as_ptr();
        auto region_end = region_start + pheader.size_in_memory();
        if (range.start == 0 || region_start < range.start)
            range.start = region_start;
        if (range.end == 0 || region_end > range.end)
            range.end = region_end;
    });

    // If there's nothing to load, there's nothing to execute
    if (range.start == range.end)
        return EINVAL;

    VERIFY(range.end > range.start);
    return range;
}

static ErrorOr<FlatPtr> get_load_offset(Elf_Ehdr const& main_program_header, OpenFileDescription& main_program_description, OpenFileDescription* interpreter_description)
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

enum class ShouldAllowSyscalls {
    No,
    Yes,
};

static ErrorOr<LoadResult> load_elf_object(Memory::AddressSpace& new_space, OpenFileDescription& object_description,
    FlatPtr load_offset, ShouldAllowSyscalls should_allow_syscalls, Optional<size_t> minimum_stack_size = {})
{
    auto& inode = *(object_description.inode());
    auto vmobject = TRY(Memory::SharedInodeVMObject::try_create_with_inode(inode));

    if (vmobject->writable_mappings()) {
        dbgln("Refusing to execute a write-mapped program");
        return ETXTBSY;
    }

    size_t executable_size = inode.size();
    size_t rounded_executable_size = TRY(Memory::page_round_up(executable_size));

    auto executable_region = TRY(MM.allocate_kernel_region_with_vmobject(*vmobject, rounded_executable_size, "ELF loading"sv, Memory::Region::Access::Read));
    auto elf_image = ELF::Image(executable_region->vaddr().as_ptr(), executable_size);

    if (!elf_image.is_valid())
        return ENOEXEC;

    FlatPtr load_base_address = 0;
    size_t stack_size = Thread::default_userspace_stack_size;

    if (minimum_stack_size.has_value() && minimum_stack_size.value() > stack_size)
        stack_size = minimum_stack_size.value();

    auto elf_name = TRY(object_description.pseudo_path());
    VERIFY(!Processor::in_critical());

    Memory::MemoryManager::enter_address_space(new_space);

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

        auto region = TRY(new_space.allocate_region(Memory::RandomizeVirtualAddress::Yes, range_base, range_end.get() - range_base.get(), PAGE_SIZE, region_name->view(), prot, AllocationStrategy::Reserve));

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
        auto region = TRY(new_space.allocate_region_with_vmobject(Memory::RandomizeVirtualAddress::Yes, range_base, range_end.get() - range_base.get(), program_header.alignment(), *vmobject, program_header.offset(), elf_name->view(), prot, true));
        if (program_header.is_executable())
            region->set_initially_loaded_executable_segment();

        if (should_allow_syscalls == ShouldAllowSyscalls::Yes)
            region->set_syscall_region(true);
        if (program_header.offset() == 0)
            load_base_address = (FlatPtr)region->vaddr().as_ptr();
        return {};
    };

    auto load_elf_program_header = [&](auto& program_header) -> ErrorOr<void> {
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

    auto* stack_region = TRY(new_space.allocate_region(Memory::RandomizeVirtualAddress::Yes, {}, stack_size, PAGE_SIZE, "Stack (Main thread)"sv, PROT_READ | PROT_WRITE, AllocationStrategy::Reserve));
    stack_region->set_stack(true);

    return LoadResult {
        load_base_address,
        elf_image.entry().offset(load_offset).get(),
        executable_size,
        TRY(stack_region->try_make_weak_ptr())
    };
}

ErrorOr<LoadResult>
Process::load(Memory::AddressSpace& new_space, NonnullRefPtr<OpenFileDescription> main_program_description,
    RefPtr<OpenFileDescription> interpreter_description, Elf_Ehdr const& main_program_header, Optional<size_t> minimum_stack_size)
{
    auto load_offset = TRY(get_load_offset(main_program_header, main_program_description, interpreter_description));

    if (interpreter_description.is_null())
        return TRY(load_elf_object(new_space, main_program_description, load_offset, ShouldAllowSyscalls::No, minimum_stack_size));

    return TRY(load_elf_object(new_space, *interpreter_description, load_offset, ShouldAllowSyscalls::Yes, minimum_stack_size));
}

void Process::clear_signal_handlers_for_exec()
{
    // Comments are as they are presented in the POSIX specification, but slightly out of order.
    for (size_t signal = 0; signal < m_signal_action_data.size(); signal++) {
        // Except for SIGCHLD, signals set to be ignored by the calling process image shall be set to be ignored by the new process image.
        // If the SIGCHLD signal is set to be ignored by the calling process image, it is unspecified whether the SIGCHLD signal is set
        // to be ignored or to the default action in the new process image.
        if (signal != SIGCHLD && m_signal_action_data[signal].handler_or_sigaction.get() == reinterpret_cast<FlatPtr>(SIG_IGN)) {
            m_signal_action_data[signal] = {};
            m_signal_action_data[signal].handler_or_sigaction.set(reinterpret_cast<FlatPtr>(SIG_IGN));
            continue;
        }

        // Signals set to the default action in the calling process image shall be set to the default action in the new process image.
        // Signals set to be caught by the calling process image shall be set to the default action in the new process image.
        m_signal_action_data[signal] = {};
    }
}

ErrorOr<void> Process::do_exec(NonnullRefPtr<OpenFileDescription> main_program_description, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment,
    RefPtr<OpenFileDescription> interpreter_description, Thread*& new_main_thread, InterruptsState& previous_interrupts_state, Elf_Ehdr const& main_program_header, Optional<size_t> minimum_stack_size)
{
    VERIFY(is_user_process());
    VERIFY(!Processor::in_critical());
    auto main_program_metadata = main_program_description->metadata();
    // NOTE: Don't allow running SUID binaries at all if we are in a jail.
    if (Process::current().is_jailed() && (main_program_metadata.is_setuid() || main_program_metadata.is_setgid()))
        return Error::from_errno(EPERM);

    // Although we *could* handle a pseudo_path here, trying to execute something that doesn't have
    // a custody (e.g. BlockDevice or RandomDevice) is pretty suspicious anyway.
    auto path = TRY(main_program_description->original_absolute_path());

    dbgln_if(EXEC_DEBUG, "do_exec: {}", path);

    auto last_part = path->view().find_last_split_view('/');

    auto allocated_space = TRY(Memory::AddressSpace::try_create(*this, nullptr));
    OwnPtr<Memory::AddressSpace> old_space;
    auto& new_space = m_space.with([&](auto& space) -> Memory::AddressSpace& {
        old_space = move(space);
        space = move(allocated_space);
        return *space;
    });
    ArmedScopeGuard space_guard([&]() {
        // If we failed at any point from now on we have to revert back to the old address space
        m_space.with([&](auto& space) {
            space = old_space.release_nonnull();
        });
        Memory::MemoryManager::enter_process_address_space(*this);
    });

    auto load_result = TRY(load(new_space, main_program_description, interpreter_description, main_program_header, minimum_stack_size));

    // NOTE: We don't need the interpreter executable description after this point.
    //       We destroy it here to prevent it from getting destroyed when we return from this function.
    //       That's important because when we're returning from this function, we're in a very delicate
    //       state where we can't block (e.g by trying to acquire a mutex in description teardown.)
    bool has_interpreter = interpreter_description;
    interpreter_description = nullptr;

    auto* signal_trampoline_region = TRY(new_space.allocate_region_with_vmobject(Memory::RandomizeVirtualAddress::Yes, {}, PAGE_SIZE, PAGE_SIZE, g_signal_trampoline_region->vmobject(), 0, "Signal trampoline"sv, PROT_READ | PROT_EXEC, true));
    signal_trampoline_region->set_syscall_region(true);

    // (For dynamically linked executable) Allocate an FD for passing the main executable to the dynamic loader.
    Optional<ScopedDescriptionAllocation> main_program_fd_allocation;
    if (has_interpreter)
        main_program_fd_allocation = TRY(allocate_fd());

    auto old_credentials = this->credentials();
    auto new_credentials = old_credentials;
    auto old_scoped_list = m_scoped_process_list.with([&](auto& list) -> RefPtr<ScopedProcessList> { return list; });

    bool executable_is_setid = false;

    if (!(main_program_description->custody()->mount_flags() & MS_NOSUID)) {
        auto new_euid = old_credentials->euid();
        auto new_egid = old_credentials->egid();
        auto new_suid = old_credentials->suid();
        auto new_sgid = old_credentials->sgid();

        if (main_program_metadata.is_setuid()) {
            executable_is_setid = true;
            new_euid = main_program_metadata.uid;
            new_suid = main_program_metadata.uid;
        }
        if (main_program_metadata.is_setgid()) {
            executable_is_setid = true;
            new_egid = main_program_metadata.gid;
            new_sgid = main_program_metadata.gid;
        }

        if (executable_is_setid) {
            new_credentials = TRY(Credentials::create(
                old_credentials->uid(),
                old_credentials->gid(),
                new_euid,
                new_egid,
                new_suid,
                new_sgid,
                old_credentials->extra_gids(),
                old_credentials->sid(),
                old_credentials->pgid()));
        }
    }

    // We commit to the new executable at this point. There is no turning back!
    space_guard.disarm();

    // Prevent other processes from attaching to us with ptrace while we're doing this.
    MutexLocker ptrace_locker(ptrace_lock());

    // Disable profiling temporarily in case it's running on this process.
    auto was_profiling = m_profiling;
    TemporaryChange profiling_disabler(m_profiling, false);

    kill_threads_except_self();

    with_mutable_protected_data([&](auto& protected_data) {
        protected_data.credentials = move(new_credentials);
        protected_data.dumpable = !executable_is_setid;
        protected_data.executable_is_setid = executable_is_setid;
        protected_data.jailed_until_exec = false;
    });

    m_executable.with([&](auto& executable) { executable = main_program_description->custody(); });
    m_arguments = move(arguments);
    m_scoped_process_list.with([&](auto& list) {
        list = old_scoped_list;
    });

    m_environment = move(environment);

    TRY(m_unveil_data.with([&](auto& unveil_data) -> ErrorOr<void> {
        TRY(m_exec_unveil_data.with([&](auto& exec_unveil_data) -> ErrorOr<void> {
            // Note: If we have exec unveil data being waiting to be dispatched
            // to the current execve'd program, then we apply the unveil data and
            // ensure it is locked in the new program.
            if (exec_unveil_data.state == VeilState::Dropped) {
                unveil_data.state = VeilState::LockedInherited;
                exec_unveil_data.state = VeilState::None;
                unveil_data.paths = TRY(exec_unveil_data.paths.deep_copy());
            } else {
                unveil_data.state = VeilState::None;
                exec_unveil_data.state = VeilState::None;
                unveil_data.paths.clear();
                unveil_data.paths.set_metadata({ TRY(KString::try_create("/"sv)), UnveilAccess::None, false });
            }
            exec_unveil_data.paths.clear();
            exec_unveil_data.paths.set_metadata({ TRY(KString::try_create("/"sv)), UnveilAccess::None, false });
            return {};
        }));
        return {};
    }));

    m_coredump_properties.for_each([](auto& property) {
        property = {};
    });

    auto* current_thread = Thread::current();
    current_thread->reset_signals_for_exec();

    clear_signal_handlers_for_exec();

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

    auto credentials = this->credentials();
    auto auxv = generate_auxiliary_vector(load_result.load_base, load_result.entry_eip, credentials->uid(), credentials->euid(), credentials->gid(), credentials->egid(), path->view(), main_program_fd_allocation);

    // FIXME: How much stack space does process startup need?
    if (!validate_stack_size(m_arguments, m_environment, auxv))
        return E2BIG;

    // NOTE: We create the new stack before disabling interrupts since it will zero-fault
    //       and we don't want to deal with faults after this point.
    auto new_userspace_sp = TRY(make_userspace_context_for_main_thread(new_main_thread->regs(), *load_result.stack_region.unsafe_ptr(), m_arguments, m_environment, move(auxv)));

    // NOTE: The Process and its first thread share the same name.
    set_name(last_part);
    new_main_thread->set_name(last_part);

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
    // If we used an InterruptDisabler that calls enable_interrupts() on exit, we might timer tick'd too soon in exec().
    Processor::enter_critical();
    previous_interrupts_state = Processor::interrupts_state();
    Processor::disable_interrupts();

    // NOTE: Be careful to not trigger any page faults below!

    with_mutable_protected_data([&](auto& protected_data) {
        protected_data.promises = protected_data.execpromises;
        protected_data.has_promises = protected_data.has_execpromises;

        protected_data.execpromises = 0;
        protected_data.has_execpromises = false;

        protected_data.signal_trampoline = signal_trampoline_region->vaddr();

        // FIXME: PID/TID ISSUE
        protected_data.pid = new_main_thread->tid().value();
    });

    new_main_thread->reset_fpu_state();

    auto& regs = new_main_thread->m_regs;
    address_space().with([&](auto& space) {
        regs.set_exec_state(load_result.entry_eip, new_userspace_sp, *space);
    });

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
#if ARCH(X86_64)
        { ELF::AuxiliaryValue::HwCap, (long)CPUID(1).edx() },
#elif ARCH(AARCH64)
        { ELF::AuxiliaryValue::HwCap, (long)0 },
#elif ARCH(RISCV64)
        { ELF::AuxiliaryValue::HwCap, (long)0 }, // TODO
#else
#    error "Unknown architecture"
#endif

        { ELF::AuxiliaryValue::ClockTick, (long)TimeManagement::the().ticks_per_second() },

        // FIXME: Also take into account things like extended filesystem permissions? That's what linux does...
        { ELF::AuxiliaryValue::Secure, ((uid != euid) || (gid != egid)) ? 1 : 0 },

        { ELF::AuxiliaryValue::Random, nullptr },

        { ELF::AuxiliaryValue::ExecFilename, executable_path },

        main_program_fd_allocation.has_value() ? ELF::AuxiliaryValue { ELF::AuxiliaryValue::ExecFileDescriptor, main_program_fd_allocation->fd } : ELF::AuxiliaryValue { ELF::AuxiliaryValue::Ignore, 0L },

        { ELF::AuxiliaryValue::Null, 0L },
    } };
}

static bool is_executable_starting_with_shebang(Span<u8> const& preliminary_buffer)
{
    return preliminary_buffer.size() > 2 && preliminary_buffer[0] == '#' && preliminary_buffer[1] == '!';
}

static ErrorOr<Vector<NonnullOwnPtr<KString>>> find_shebang_interpreter_for_executable(Span<u8> const& line_buffer)
{
    VERIFY(is_executable_starting_with_shebang(line_buffer));
    int word_start = 2;
    size_t word_length = 0;
    Vector<NonnullOwnPtr<KString>> interpreter_words;

    bool found_line_break = false;

    for (size_t i = 2; i < line_buffer.size(); ++i) {
        if (line_buffer[i] == '\n') {
            found_line_break = true;
            break;
        }

        if (line_buffer[i] != ' ') {
            ++word_length;
        }

        if (line_buffer[i] == ' ') {
            if (word_length > 0) {
                auto word = TRY(KString::try_create(StringView { line_buffer.slice(word_start, word_length) }));
                TRY(interpreter_words.try_append(move(word)));
            }
            word_length = 0;
            word_start = i + 1;
        }
    }

    // This is an indication that the provided buffer has not sufficient data
    // within it to find the entire interpreter path.
    if (!found_line_break)
        return ENOBUFS;

    if (word_length > 0) {
        auto word = TRY(KString::try_create(StringView { line_buffer.slice(word_start, word_length) }));
        TRY(interpreter_words.try_append(move(word)));
    }

    if (!interpreter_words.is_empty())
        return interpreter_words;

    return ENOEXEC;
}

static ErrorOr<FixedArray<u8>> read_elf_buffer_including_program_headers(OpenFileDescription& elf_file, Elf_Ehdr const& main_executable_header)
{
    auto program_header_offset = static_cast<size_t>(main_executable_header.e_phoff);
    auto program_header_entry_size = static_cast<size_t>(main_executable_header.e_phentsize);
    auto program_header_entries_count = static_cast<size_t>(main_executable_header.e_phnum);

    if (Checked<size_t>::multiplication_would_overflow(program_header_entry_size, program_header_entries_count))
        return EOVERFLOW;

    if (Checked<size_t>::addition_would_overflow(program_header_offset, (program_header_entry_size * program_header_entries_count)))
        return EOVERFLOW;

    auto last_needed_byte_offset_on_program_header_list = program_header_offset + (program_header_entry_size * program_header_entries_count);
    if (last_needed_byte_offset_on_program_header_list < sizeof(Elf_Ehdr))
        return EINVAL;

    auto elf_buffer = TRY(FixedArray<u8>::create(last_needed_byte_offset_on_program_header_list));
    auto elf_read_buffer = UserOrKernelBuffer::for_kernel_buffer(elf_buffer.data());
    {
        auto nread = TRY(elf_file.read(elf_read_buffer, 0, elf_buffer.span().size()));
        if (nread < elf_buffer.span().size())
            return EIO;
    }
    return elf_buffer;
}

ErrorOr<RefPtr<OpenFileDescription>> Process::find_elf_interpreter_for_executable(OpenFileDescription& elf_file, StringView path, Elf_Ehdr const& main_executable_header, size_t file_size, Optional<size_t>& minimum_stack_size)
{
    // We can't exec an ET_REL, as that's just an object file from the compiler,
    // and we can't exec an ET_CORE as it's just a coredump.
    // The only allowed ELF files on execve are executables or shared object files
    // which are dynamically linked programs (or static-pie programs like the dynamic loader).
    if (main_executable_header.e_type != ET_EXEC && main_executable_header.e_type != ET_DYN)
        return ENOEXEC;

    auto main_program_with_program_headers_buffer = TRY(read_elf_buffer_including_program_headers(elf_file, main_executable_header));

    Optional<size_t> main_executable_requested_stack_size {};
    Optional<Elf_Phdr> maybe_interpreter_path_program_header {};
    if (!ELF::validate_program_headers(main_executable_header, file_size, main_program_with_program_headers_buffer.span(), maybe_interpreter_path_program_header, &main_executable_requested_stack_size)) {
        dbgln("exec({}): File has invalid ELF Program headers", path);
        return ENOEXEC;
    }

    if (main_executable_requested_stack_size.has_value() && (!minimum_stack_size.has_value() || *minimum_stack_size < *main_executable_requested_stack_size))
        minimum_stack_size = main_executable_requested_stack_size;

    // The ELF file might not have any INTERP header, which in such
    // case we can't do anything and therefore we should just continue
    // without loading any interpreter.
    if (!maybe_interpreter_path_program_header.has_value())
        return nullptr;

    auto interpreter_path_program_header = maybe_interpreter_path_program_header.release_value();

    auto buffer = TRY(KBuffer::try_create_with_size("ELF interpreter program path"sv, static_cast<size_t>(interpreter_path_program_header.p_filesz) - 1));
    auto buffer_as_kernel_buffer = buffer->as_kernel_buffer();
    {
        auto nread = TRY(elf_file.read(buffer_as_kernel_buffer, static_cast<size_t>(interpreter_path_program_header.p_offset), buffer->size()));
        if (nread < buffer->size())
            return EIO;
    }

    auto interpreter_path = StringView(buffer->bytes());
    if (interpreter_path.is_empty()) {
        dbgln("exec({}): WARNING: File has an INTERP program header, but the interpreter path is empty", path);
        return nullptr;
    }

    dbgln_if(EXEC_DEBUG, "exec({}): Using program interpreter {}", path, interpreter_path);
    auto interpreter_description = TRY(VirtualFileSystem::open(vfs_root_context(), credentials(), interpreter_path, O_EXEC, 0, current_directory()));
    auto interp_metadata = interpreter_description->metadata();

    if (!interp_metadata.is_regular_file())
        return ENOEXEC;

    VERIFY(interpreter_description->inode());

    // The program interpreter should be at least the size of an ELF header
    // so it's easy to check this and fail accordingly.
    if (interp_metadata.size < static_cast<int>(sizeof(Elf_Ehdr)))
        return ENOEXEC;

    static_assert(sizeof(Elf_Ehdr) < PAGE_SIZE);
    auto elf_interpreter_buffer = Array<u8, sizeof(Elf_Ehdr)>::from_repeated_value(0);
    auto elf_interpreter_read_buffer = UserOrKernelBuffer::for_kernel_buffer(elf_interpreter_buffer.data());
    auto nread = TRY(interpreter_description->read(elf_interpreter_read_buffer, elf_interpreter_buffer.span().size()));
    if (nread < sizeof(Elf_Ehdr))
        return ENOEXEC;

    auto const* interpreter_executable_header = bit_cast<Elf_Ehdr const*>(elf_interpreter_buffer.data());
    if (!ELF::validate_elf_header(*interpreter_executable_header, interp_metadata.size)) {
        dbgln("exec({}): Interpreter ({}) has invalid ELF header", path, interpreter_path);
        return ENOEXEC;
    }

    auto elf_interpreter_with_program_headers_buffer = TRY(read_elf_buffer_including_program_headers(*interpreter_description, *interpreter_executable_header));

    Optional<size_t> interpreter_requested_stack_size {};
    Optional<Elf_Phdr> maybe_interpreter_interpreter_path_program_header {};
    if (!ELF::validate_program_headers(*interpreter_executable_header, interp_metadata.size, elf_interpreter_with_program_headers_buffer.span(), maybe_interpreter_interpreter_path_program_header, &interpreter_requested_stack_size)) {
        dbgln("exec({}): Interpreter ({}) has invalid ELF Program headers", path, interpreter_path);
        return ENOEXEC;
    }

    // NOTE: This ELF file should not have any INTERP header, because it's already the
    // interpreter of the previously loaded ELF file!
    if (maybe_interpreter_interpreter_path_program_header.has_value()) {
        dbgln("exec({}): Interpreter ({}) has its own interpreter! No thank you!", path, interpreter_path);
        return ELOOP;
    }

    if (interpreter_requested_stack_size.has_value() && (!minimum_stack_size.has_value() || *minimum_stack_size < *interpreter_requested_stack_size))
        minimum_stack_size = interpreter_requested_stack_size;

    return interpreter_description;
}

ErrorOr<void> Process::exec(NonnullOwnPtr<KString> path, Vector<NonnullOwnPtr<KString>> arguments, Vector<NonnullOwnPtr<KString>> environment, Thread*& new_main_thread, InterruptsState& previous_interrupts_state, int recursion_depth)
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
    auto description = TRY(VirtualFileSystem::open(vfs_root_context(), credentials(), path->view(), O_EXEC, 0, current_directory()));
    auto metadata = description->metadata();

    if (!metadata.is_regular_file())
        return EACCES;

    // Always gonna need at least 3 bytes. these are for #!X
    if (metadata.size < 3)
        return ENOEXEC;

    VERIFY(description->inode());

    // Read just the size of ELF header of the program into memory so we can start parsing it.
    // The size of a ELF header should suffice to find the shebang sign (known as #! sign) if the file has it
    // in the start.
    auto preliminary_buffer = Array<u8, sizeof(Elf_Ehdr)>::from_repeated_value(0);
    auto preliminary_read_buffer = UserOrKernelBuffer::for_kernel_buffer(preliminary_buffer.data());
    auto nread = TRY(description->read(preliminary_read_buffer, 0, preliminary_buffer.span().size()));

    // 1) #! interpreted file
    if (is_executable_starting_with_shebang(preliminary_buffer)) {
        // FIXME: PAGE_SIZE seems like enough for specifying an interpreter for now.
        // We might need to re-evaluate this but to avoid further allocations, this is how it is for now.
        auto shebang_line_buffer = Array<u8, PAGE_SIZE>::from_repeated_value(0);
        auto shebang_line_read_buffer = UserOrKernelBuffer::for_kernel_buffer(shebang_line_buffer.data());
        TRY(description->read(shebang_line_read_buffer, 0, shebang_line_buffer.span().size()));
        auto shebang_words = TRY(find_shebang_interpreter_for_executable(shebang_line_buffer));
        auto shebang_path = TRY(shebang_words.first()->try_clone());
        arguments[0] = move(path);
        TRY(arguments.try_prepend(move(shebang_words)));
        return exec(move(shebang_path), move(arguments), move(environment), new_main_thread, previous_interrupts_state, ++recursion_depth);
    }

    // #2) ELF32 for i386

    if (nread < sizeof(Elf_Ehdr))
        return ENOEXEC;
    auto const* main_program_header = bit_cast<Elf_Ehdr const*>(preliminary_buffer.data());

    if (!ELF::validate_elf_header(*main_program_header, metadata.size)) {
        dbgln("exec({}): File has invalid ELF header", path);
        return ENOEXEC;
    }

    Optional<size_t> minimum_stack_size {};
    auto interpreter_description = TRY(find_elf_interpreter_for_executable(*description, path->view(), *main_program_header, metadata.size, minimum_stack_size));
    return do_exec(move(description), move(arguments), move(environment), move(interpreter_description), new_main_thread, previous_interrupts_state, *main_program_header, minimum_stack_size);
}

ErrorOr<FlatPtr> Process::sys$execve(Userspace<Syscall::SC_execve_params const*> user_params)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::exec));

    Thread* new_main_thread = nullptr;
    InterruptsState previous_interrupts_state = InterruptsState::Enabled;

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

        auto copy_user_strings = [](auto const& list, auto& output) -> ErrorOr<void> {
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

        Vector<NonnullOwnPtr<KString>> arguments;
        TRY(copy_user_strings(params.arguments, arguments));

        Vector<NonnullOwnPtr<KString>> environment;
        TRY(copy_user_strings(params.environment, environment));

        TRY(exec(move(path), move(arguments), move(environment), new_main_thread, previous_interrupts_state));
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
        Processor::assume_context(*current_thread, previous_interrupts_state);
        VERIFY_NOT_REACHED();
    }

    // NOTE: This code path is taken in the non-syscall case, i.e when the kernel spawns
    //       a userspace process directly (such as /bin/SystemServer on startup)

    Processor::restore_interrupts_state(previous_interrupts_state);
    Processor::leave_critical();
    return 0;
}

}
