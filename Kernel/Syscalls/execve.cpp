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

#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <Kernel/Random.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>
#include <LibC/limits.h>
#include <LibELF/Image.h>
#include <LibELF/Validation.h>

//#define EXEC_DEBUG

namespace Kernel {

static bool validate_stack_size(const Vector<String>& arguments, const Vector<String>& environment)
{
    size_t total_blob_size = 0;
    for (auto& a : arguments)
        total_blob_size += a.length() + 1;
    for (auto& e : environment)
        total_blob_size += e.length() + 1;

    size_t total_meta_size = sizeof(char*) * (arguments.size() + 1) + sizeof(char*) * (environment.size() + 1);

    // FIXME: This doesn't account for the size of the auxiliary vector
    return (total_blob_size + total_meta_size) < Thread::default_userspace_stack_size;
}

KResultOr<Process::LoadResult> Process::load_elf_object(FileDescription& object_description, FlatPtr load_offset, ShouldAllocateTls should_allocate_tls)
{
    auto& inode = *(object_description.inode());
    auto vmobject = SharedInodeVMObject::create_with_inode(inode);
    if (vmobject->writable_mappings()) {
        dbgln("Refusing to execute a write-mapped program");
        return KResult(-ETXTBSY);
    }

    size_t executable_size = inode.size();

    auto region = MM.allocate_kernel_region_with_vmobject(*vmobject, PAGE_ROUND_UP(executable_size), "ELF loading", Region::Access::Read);
    if (!region) {
        dbgln("Could not allocate memory for ELF loading");
        return KResult(-ENOMEM);
    }

    auto elf_image = ELF::Image(region->vaddr().as_ptr(), executable_size);

    if (!elf_image.is_valid())
        return KResult(-ENOEXEC);

    Region* master_tls_region { nullptr };
    size_t master_tls_size = 0;
    size_t master_tls_alignment = 0;
    m_entry_eip = 0;
    FlatPtr load_base_address = 0;

    MM.enter_process_paging_scope(*this);
    String elf_name = object_description.absolute_path();
    ASSERT(!Processor::current().in_critical());

    KResult ph_load_result = KSuccess;
    elf_image.for_each_program_header([&](const ELF::Image::ProgramHeader& program_header) {
        if (program_header.type() == PT_TLS) {
            ASSERT(should_allocate_tls == ShouldAllocateTls::Yes);
            ASSERT(program_header.size_in_memory());

            if (!elf_image.is_within_image(program_header.raw_data(), program_header.size_in_image())) {
                dbgln("Shenanigans! ELF PT_TLS header sneaks outside of executable.");
                ph_load_result = KResult(-ENOEXEC);
                return IterationDecision::Break;
            }

            master_tls_region = allocate_region({}, program_header.size_in_memory(), String::formatted("{} (master-tls)", elf_name), PROT_READ | PROT_WRITE);
            if (!master_tls_region) {
                ph_load_result = KResult(-ENOMEM);
                return IterationDecision::Break;
            }
            master_tls_size = program_header.size_in_memory();
            master_tls_alignment = program_header.alignment();

            if (!copy_to_user(master_tls_region->vaddr().as_ptr(), program_header.raw_data(), program_header.size_in_image())) {
                ph_load_result = KResult(-EFAULT);
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
                ph_load_result = KResult(-ENOEXEC);
                return IterationDecision::Break;
            }

            int prot = 0;
            if (program_header.is_readable())
                prot |= PROT_READ;
            if (program_header.is_writable())
                prot |= PROT_WRITE;
            auto region_name = String::formatted("{} (data-{}{})", elf_name, program_header.is_readable() ? "r" : "", program_header.is_writable() ? "w" : "");
            auto* region = allocate_region(program_header.vaddr().offset(load_offset), program_header.size_in_memory(), move(region_name), prot);
            if (!region) {
                ph_load_result = KResult(-ENOMEM);
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
            if (!copy_to_user((u8*)region->vaddr().as_ptr() + page_offset.get(), program_header.raw_data(), program_header.size_in_image())) {
                ph_load_result = KResult(-EFAULT);
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
        auto* region = allocate_region_with_vmobject(program_header.vaddr().offset(load_offset), program_header.size_in_memory(), *vmobject, program_header.offset(), elf_name, prot);
        if (!region) {
            ph_load_result = KResult(-ENOMEM);
            return IterationDecision::Break;
        }
        region->set_shared(true);
        if (program_header.offset() == 0)
            load_base_address = (FlatPtr)region->vaddr().as_ptr();
        return IterationDecision::Continue;
    });

    if (ph_load_result.is_error()) {
        dbgln("do_exec: Failure loading program ({})", ph_load_result.error());
        return ph_load_result;
    }

    if (!elf_image.entry().offset(load_offset).get()) {
        dbgln("do_exec: Failure loading program, entry pointer is invalid! {})", elf_image.entry().offset(load_offset));
        return KResult(-ENOEXEC);
    }

    return LoadResult {
        load_base_address,
        elf_image.entry().offset(load_offset).get(),
        executable_size,
        VirtualAddress(elf_image.program_header_table_offset()).offset(load_offset).get(),
        elf_image.program_header_count(),
        master_tls_region ? master_tls_region->make_weak_ptr() : nullptr,
        master_tls_size,
        master_tls_alignment
    };
}

int Process::load(NonnullRefPtr<FileDescription> main_program_description, RefPtr<FileDescription> interpreter_description)
{
    RefPtr<PageDirectory> old_page_directory;
    NonnullOwnPtrVector<Region> old_regions;

    {
        // Need to make sure we don't swap contexts in the middle
        ScopedCritical critical;
        old_page_directory = move(m_page_directory);
        old_regions = move(m_regions);
        m_page_directory = PageDirectory::create_for_userspace(*this);
    }

    ArmedScopeGuard rollback_regions_guard([&]() {
        ASSERT(Process::current() == this);
        // Need to make sure we don't swap contexts in the middle
        ScopedCritical critical;
        m_page_directory = move(old_page_directory);
        m_regions = move(old_regions);
        MM.enter_process_paging_scope(*this);
    });

    if (!interpreter_description) {
        auto result = load_elf_object(main_program_description, FlatPtr { 0 }, ShouldAllocateTls::Yes);
        if (result.is_error())
            return result.error();

        m_load_base = result.value().load_base;
        m_entry_eip = result.value().entry_eip;
        m_master_tls_region = result.value().tls_region;
        m_master_tls_size = result.value().tls_size;
        m_master_tls_alignment = result.value().tls_alignment;

        rollback_regions_guard.disarm();
        return 0;
    }

    // TODO: I'm sure this can be randomized even better. :^)
    FlatPtr random_offset = get_good_random<u16>() * PAGE_SIZE;
    FlatPtr interpreter_load_offset = 0x08000000 + random_offset;

    auto interpreter_load_result = load_elf_object(*interpreter_description, interpreter_load_offset, ShouldAllocateTls::No);
    if (interpreter_load_result.is_error())
        return interpreter_load_result.error();

    m_load_base = interpreter_load_result.value().load_base;
    m_entry_eip = interpreter_load_result.value().entry_eip;

    // TLS allocation will be done in userspace by the loader
    m_master_tls_region = nullptr;
    m_master_tls_size = 0;
    m_master_tls_alignment = 0;

    rollback_regions_guard.disarm();
    return 0;
}

int Process::do_exec(NonnullRefPtr<FileDescription> main_program_description, Vector<String> arguments, Vector<String> environment, RefPtr<FileDescription> interpreter_description, Thread*& new_main_thread, u32& prev_flags)
{
    ASSERT(is_user_process());
    ASSERT(!Processor::current().in_critical());
    auto path = main_program_description->absolute_path();
#ifdef EXEC_DEBUG
    dbgln("do_exec({})", path);
#endif

    // FIXME: How much stack space does process startup need?
    if (!validate_stack_size(arguments, environment))
        return -E2BIG;

    auto parts = path.split('/');
    if (parts.is_empty())
        return -ENOENT;

    // Disable profiling temporarily in case it's running on this process.
    bool was_profiling = is_profiling();
    TemporaryChange profiling_disabler(m_profiling, false);

    // Mark this thread as the current thread that does exec
    // No other thread from this process will be scheduled to run
    auto current_thread = Thread::current();
    m_exec_tid = current_thread->tid();

    // NOTE: We switch credentials before altering the memory layout of the process.
    //       This ensures that ptrace access control takes the right credentials into account.

    // FIXME: This still feels rickety. Perhaps it would be better to simply block ptrace
    //        clients until we're ready to be traced? Or reject them with EPERM?

    auto main_program_metadata = main_program_description->metadata();

    auto old_euid = m_euid;
    auto old_suid = m_suid;
    auto old_egid = m_egid;
    auto old_sgid = m_sgid;

    ArmedScopeGuard cred_restore_guard = [&] {
        m_euid = old_euid;
        m_suid = old_suid;
        m_egid = old_egid;
        m_sgid = old_sgid;
    };

    if (!(main_program_description->custody()->mount_flags() & MS_NOSUID)) {
        if (main_program_metadata.is_setuid())
            m_euid = m_suid = main_program_metadata.uid;
        if (main_program_metadata.is_setgid())
            m_egid = m_sgid = main_program_metadata.gid;
    }

    int load_rc = load(main_program_description, interpreter_description);
    if (load_rc) {
        klog() << "do_exec: Failed to load main program or interpreter";
        return load_rc;
    }

    // We can commit to the new credentials at this point.
    cred_restore_guard.disarm();

    kill_threads_except_self();

#ifdef EXEC_DEBUG
    dbgln("Memory layout after ELF load:");
    dump_regions();
#endif

    m_executable = main_program_description->custody();

    m_promises = m_execpromises;

    m_veil_state = VeilState::None;
    m_unveiled_paths.clear();

    current_thread->set_default_signal_dispositions();
    current_thread->clear_signals();

    m_futex_queues.clear();

    m_region_lookup_cache = {};

    disown_all_shared_buffers();

    for (size_t i = 0; i < m_fds.size(); ++i) {
        auto& description_and_flags = m_fds[i];
        if (description_and_flags.description() && description_and_flags.flags() & FD_CLOEXEC)
            description_and_flags = {};
    }

    if (interpreter_description) {
        m_main_program_fd = alloc_fd();
        ASSERT(m_main_program_fd >= 0);
        main_program_description->seek(0, SEEK_SET);
        main_program_description->set_readable(true);
        m_fds[m_main_program_fd].set(move(main_program_description), FD_CLOEXEC);
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

    auto auxv = generate_auxiliary_vector();

    // NOTE: We create the new stack before disabling interrupts since it will zero-fault
    //       and we don't want to deal with faults after this point.
    auto make_stack_result = new_main_thread->make_userspace_stack_for_main_thread(move(arguments), move(environment), move(auxv));
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
    if (tsr_result.is_error())
        return tsr_result.error();
    new_main_thread->reset_fpu_state();

    auto& tss = new_main_thread->m_tss;
    tss.cs = GDT_SELECTOR_CODE3 | 3;
    tss.ds = GDT_SELECTOR_DATA3 | 3;
    tss.es = GDT_SELECTOR_DATA3 | 3;
    tss.ss = GDT_SELECTOR_DATA3 | 3;
    tss.fs = GDT_SELECTOR_DATA3 | 3;
    tss.gs = GDT_SELECTOR_TLS | 3;
    tss.eip = m_entry_eip;
    tss.esp = new_userspace_esp;
    tss.cr3 = m_page_directory->cr3();
    tss.ss2 = m_pid.value();

    if (was_profiling)
        Profiling::did_exec(path);

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

Vector<ELF::AuxiliaryValue> Process::generate_auxiliary_vector() const
{
    Vector<ELF::AuxiliaryValue> auxv;
    // PHDR/EXECFD
    // PH*
    auxv.append({ ELF::AuxiliaryValue::PageSize, PAGE_SIZE });
    auxv.append({ ELF::AuxiliaryValue::BaseAddress, (void*)m_load_base });

    auxv.append({ ELF::AuxiliaryValue::Entry, (void*)m_entry_eip });
    // NOTELF
    auxv.append({ ELF::AuxiliaryValue::Uid, (long)m_uid });
    auxv.append({ ELF::AuxiliaryValue::EUid, (long)m_euid });
    auxv.append({ ELF::AuxiliaryValue::Gid, (long)m_gid });
    auxv.append({ ELF::AuxiliaryValue::EGid, (long)m_egid });

    // FIXME: Don't hard code this? We might support other platforms later.. (e.g. x86_64)
    auxv.append({ ELF::AuxiliaryValue::Platform, "i386" });
    // FIXME: This is platform specific
    auxv.append({ ELF::AuxiliaryValue::HwCap, (long)CPUID(1).edx() });

    auxv.append({ ELF::AuxiliaryValue::ClockTick, (long)TimeManagement::the().ticks_per_second() });

    // FIXME: Also take into account things like extended filesystem permissions? That's what linux does...
    auxv.append({ ELF::AuxiliaryValue::Secure, ((m_uid != m_euid) || (m_gid != m_egid)) ? 1 : 0 });

    char random_bytes[16] {};
    get_fast_random_bytes((u8*)random_bytes, sizeof(random_bytes));

    auxv.append({ ELF::AuxiliaryValue::Random, String(random_bytes, sizeof(random_bytes)) });

    auxv.append({ ELF::AuxiliaryValue::ExecFilename, m_executable->absolute_path() });

    auxv.append({ ELF::AuxiliaryValue::ExecFileDescriptor, m_main_program_fd });

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

    return KResult(-ENOEXEC);
}

KResultOr<NonnullRefPtr<FileDescription>> Process::find_elf_interpreter_for_executable(const String& path, char (&first_page)[PAGE_SIZE], int nread, size_t file_size)
{
    if (nread < (int)sizeof(Elf32_Ehdr))
        return KResult(-ENOEXEC);

    auto elf_header = (Elf32_Ehdr*)first_page;
    if (!ELF::validate_elf_header(*elf_header, file_size)) {
        dbgln("exec({}): File has invalid ELF header", path);
        return KResult(-ENOEXEC);
    }

    // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
    String interpreter_path;
    if (!ELF::validate_program_headers(*elf_header, file_size, (u8*)first_page, nread, &interpreter_path)) {
        dbgln("exec({}): File has invalid ELF Program headers", path);
        return KResult(-ENOEXEC);
    }

    if (!interpreter_path.is_empty()) {

#ifdef EXEC_DEBUG
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
            return KResult(-ENOEXEC);

        memset(first_page, 0, sizeof(first_page));
        auto first_page_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&first_page);
        auto nread_or_error = interpreter_description->read(first_page_buffer, sizeof(first_page));
        if (nread_or_error.is_error())
            return KResult(-ENOEXEC);
        nread = nread_or_error.value();

        if (nread < (int)sizeof(Elf32_Ehdr))
            return KResult(-ENOEXEC);

        elf_header = (Elf32_Ehdr*)first_page;
        if (!ELF::validate_elf_header(*elf_header, interp_metadata.size)) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF header", path, interpreter_description->absolute_path());
            return KResult(-ENOEXEC);
        }

        // Not using KResultOr here because we'll want to do the same thing in userspace in the RTLD
        String interpreter_interpreter_path;
        if (!ELF::validate_program_headers(*elf_header, interp_metadata.size, (u8*)first_page, nread, &interpreter_interpreter_path)) {
            dbgln("exec({}): Interpreter ({}) has invalid ELF Program headers", path, interpreter_description->absolute_path());
            return KResult(-ENOEXEC);
        }

        // FIXME: Uncomment this
        //        How do we get gcc to not insert an interpreter section to /usr/lib/Loader.so itself?
        // if (!interpreter_interpreter_path.is_empty()) {
        //     dbgln("exec({}): Interpreter ({}) has its own interpreter ({})! No thank you!", path, interpreter_description->absolute_path(), interpreter_interpreter_path);
        //     return KResult(-ELOOP);
        // }

        return interpreter_description;
    }

    if (elf_header->e_type != ET_EXEC) {
        // We can't exec an ET_REL, that's just an object file from the compiler
        // If it's ET_DYN with no PT_INTERP, then we can't load it properly either
        return KResult(-ENOEXEC);
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
    auto elf_result = find_elf_interpreter_for_executable(path, first_page, nread_or_error.value(), metadata.size);
    RefPtr<FileDescription> interpreter_description;
    // We're getting either an interpreter, an error, or KSuccess (i.e. no interpreter but file checks out)
    if (!elf_result.is_error())
        interpreter_description = elf_result.value();
    else if (elf_result.error().is_error())
        return elf_result.error();

    // The bulk of exec() is done by do_exec(), which ensures that all locals
    // are cleaned up by the time we yield-teleport below.
    Thread* new_main_thread = nullptr;
    u32 prev_flags = 0;
    int rc = do_exec(move(description), move(arguments), move(environment), move(interpreter_description), new_main_thread, prev_flags);

    m_exec_tid = 0;

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
        Checked size = sizeof(list.strings);
        size *= list.length;
        if (size.has_overflow())
            return false;
        Vector<Syscall::StringArgument, 32> strings;
        strings.resize(list.length);
        if (!copy_from_user(strings.data(), list.strings, list.length * sizeof(Syscall::StringArgument)))
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
