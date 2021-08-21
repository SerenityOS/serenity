/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <klingi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/JsonObjectSerializer.h>
#include <Kernel/CoreDump.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/ProcessPagingScope.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <LibC/elf.h>
#include <LibELF/CoreDump.h>

namespace Kernel {

OwnPtr<CoreDump> CoreDump::create(NonnullRefPtr<Process> process, const String& output_path)
{
    if (!process->is_dumpable()) {
        dbgln("Refusing to generate CoreDump for non-dumpable process {}", process->pid().value());
        return {};
    }

    auto fd = create_target_file(process, output_path);
    if (!fd)
        return {};
    return adopt_own_if_nonnull(new (nothrow) CoreDump(move(process), fd.release_nonnull()));
}

CoreDump::CoreDump(NonnullRefPtr<Process> process, NonnullRefPtr<FileDescription>&& fd)
    : m_process(move(process))
    , m_fd(move(fd))
    , m_num_program_headers(m_process->address_space().region_count() + 1) // +1 for NOTE segment
{
}

RefPtr<FileDescription> CoreDump::create_target_file(const Process& process, const String& output_path)
{
    auto output_directory = KLexicalPath::dirname(output_path);
    auto dump_directory = VirtualFileSystem::the().open_directory(output_directory, VirtualFileSystem::the().root_custody());
    if (dump_directory.is_error()) {
        dbgln("Can't find directory '{}' for core dump", output_directory);
        return nullptr;
    }
    auto dump_directory_metadata = dump_directory.value()->inode().metadata();
    if (dump_directory_metadata.uid != 0 || dump_directory_metadata.gid != 0 || dump_directory_metadata.mode != 040777) {
        dbgln("Refusing to put core dump in sketchy directory '{}'", output_directory);
        return nullptr;
    }
    auto fd_or_error = VirtualFileSystem::the().open(
        KLexicalPath::basename(output_path),
        O_CREAT | O_WRONLY | O_EXCL,
        S_IFREG, // We will enable reading from userspace when we finish generating the coredump file
        *dump_directory.value(),
        UidAndGid { process.uid(), process.gid() });

    if (fd_or_error.is_error()) {
        dbgln("Failed to open core dump '{}' for writing", output_path);
        return nullptr;
    }

    return fd_or_error.value();
}

KResult CoreDump::write_elf_header()
{
    ElfW(Ehdr) elf_file_header;
    elf_file_header.e_ident[EI_MAG0] = 0x7f;
    elf_file_header.e_ident[EI_MAG1] = 'E';
    elf_file_header.e_ident[EI_MAG2] = 'L';
    elf_file_header.e_ident[EI_MAG3] = 'F';
#if ARCH(I386)
    elf_file_header.e_ident[EI_CLASS] = ELFCLASS32;
#else
    elf_file_header.e_ident[EI_CLASS] = ELFCLASS64;
#endif
    elf_file_header.e_ident[EI_DATA] = ELFDATA2LSB;
    elf_file_header.e_ident[EI_VERSION] = EV_CURRENT;
    elf_file_header.e_ident[EI_OSABI] = 0; // ELFOSABI_NONE
    elf_file_header.e_ident[EI_ABIVERSION] = 0;
    elf_file_header.e_ident[EI_PAD + 1] = 0;
    elf_file_header.e_ident[EI_PAD + 2] = 0;
    elf_file_header.e_ident[EI_PAD + 3] = 0;
    elf_file_header.e_ident[EI_PAD + 4] = 0;
    elf_file_header.e_ident[EI_PAD + 5] = 0;
    elf_file_header.e_ident[EI_PAD + 6] = 0;
    elf_file_header.e_type = ET_CORE;
#if ARCH(I386)
    elf_file_header.e_machine = EM_386;
#else
    elf_file_header.e_machine = EM_X86_64;
#endif
    elf_file_header.e_version = 1;
    elf_file_header.e_entry = 0;
    elf_file_header.e_phoff = sizeof(ElfW(Ehdr));
    elf_file_header.e_shoff = 0;
    elf_file_header.e_flags = 0;
    elf_file_header.e_ehsize = sizeof(ElfW(Ehdr));
    elf_file_header.e_shentsize = sizeof(ElfW(Shdr));
    elf_file_header.e_phentsize = sizeof(ElfW(Phdr));
    elf_file_header.e_phnum = m_num_program_headers;
    elf_file_header.e_shnum = 0;
    elf_file_header.e_shstrndx = SHN_UNDEF;

    auto result = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&elf_file_header)), sizeof(ElfW(Ehdr)));
    if (result.is_error())
        return result.error();
    return KSuccess;
}

KResult CoreDump::write_program_headers(size_t notes_size)
{
    size_t offset = sizeof(ElfW(Ehdr)) + m_num_program_headers * sizeof(ElfW(Phdr));
    for (auto& region : m_process->address_space().regions()) {
        ElfW(Phdr) phdr {};

        phdr.p_type = PT_LOAD;
        phdr.p_offset = offset;
        phdr.p_vaddr = region->vaddr().get();
        phdr.p_paddr = 0;

        phdr.p_filesz = region->page_count() * PAGE_SIZE;
        phdr.p_memsz = region->page_count() * PAGE_SIZE;
        phdr.p_align = 0;

        phdr.p_flags = region->is_readable() ? PF_R : 0;
        if (region->is_writable())
            phdr.p_flags |= PF_W;
        if (region->is_executable())
            phdr.p_flags |= PF_X;

        offset += phdr.p_filesz;

        [[maybe_unused]] auto rc = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&phdr)), sizeof(ElfW(Phdr)));
    }

    ElfW(Phdr) notes_pheader {};
    notes_pheader.p_type = PT_NOTE;
    notes_pheader.p_offset = offset;
    notes_pheader.p_vaddr = 0;
    notes_pheader.p_paddr = 0;
    notes_pheader.p_filesz = notes_size;
    notes_pheader.p_memsz = notes_size;
    notes_pheader.p_align = 0;
    notes_pheader.p_flags = 0;

    auto result = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&notes_pheader)), sizeof(ElfW(Phdr)));
    if (result.is_error())
        return result.error();
    return KSuccess;
}

KResult CoreDump::write_regions()
{
    for (auto& region : m_process->address_space().regions()) {
        if (region->is_kernel())
            continue;

        region->set_readable(true);
        region->remap();

        for (size_t i = 0; i < region->page_count(); i++) {
            auto* page = region->physical_page(i);

            uint8_t zero_buffer[PAGE_SIZE] = {};
            Optional<UserOrKernelBuffer> src_buffer;

            if (page) {
                src_buffer = UserOrKernelBuffer::for_user_buffer(reinterpret_cast<uint8_t*>((region->vaddr().as_ptr() + (i * PAGE_SIZE))), PAGE_SIZE);
            } else {
                // If the current page is not backed by a physical page, we zero it in the coredump file.
                // TODO: Do we want to include the contents of pages that have not been faulted-in in the coredump?
                //       (A page may not be backed by a physical page because it has never been faulted in when the process ran).
                src_buffer = UserOrKernelBuffer::for_kernel_buffer(zero_buffer);
            }
            auto result = m_fd->write(src_buffer.value(), PAGE_SIZE);
            if (result.is_error())
                return result.error();
        }
    }
    return KSuccess;
}

KResult CoreDump::write_notes_segment(ByteBuffer& notes_segment)
{
    auto result = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(notes_segment.data()), notes_segment.size());
    if (result.is_error())
        return result.error();
    return KSuccess;
}

ByteBuffer CoreDump::create_notes_process_data() const
{
    ByteBuffer process_data;

    ELF::Core::ProcessInfo info {};
    info.header.type = ELF::Core::NotesEntryHeader::Type::ProcessInfo;
    process_data.append((void*)&info, sizeof(info));

    StringBuilder builder;
    {
        JsonObjectSerializer process_obj { builder };
        process_obj.add("pid"sv, m_process->pid().value());
        process_obj.add("termination_signal"sv, m_process->termination_signal());
        process_obj.add("executable_path"sv, m_process->executable() ? m_process->executable()->absolute_path() : String::empty());

        {
            auto arguments_array = process_obj.add_array("arguments"sv);
            for (auto& argument : m_process->arguments())
                arguments_array.add(argument);
        }

        {
            auto environment_array = process_obj.add_array("environment"sv);
            for (auto& variable : m_process->environment())
                environment_array.add(variable);
        }
    }

    builder.append(0);
    process_data.append(builder.string_view().characters_without_null_termination(), builder.length());

    return process_data;
}

ByteBuffer CoreDump::create_notes_threads_data() const
{
    ByteBuffer threads_data;

    for (auto& thread : m_process->threads_for_coredump({})) {
        ByteBuffer entry_buff;

        ELF::Core::ThreadInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::ThreadInfo;
        info.tid = thread.tid().value();

        if (thread.current_trap())
            copy_kernel_registers_into_ptrace_registers(info.regs, thread.get_register_dump_from_stack());

        entry_buff.append((void*)&info, sizeof(info));

        threads_data += entry_buff;
    }
    return threads_data;
}

ByteBuffer CoreDump::create_notes_regions_data() const
{
    ByteBuffer regions_data;
    size_t region_index = 0;
    for (auto& region : m_process->address_space().regions()) {

        ByteBuffer memory_region_info_buffer;
        ELF::Core::MemoryRegionInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo;

        info.region_start = region->vaddr().get();
        info.region_end = region->vaddr().offset(region->size()).get();
        info.program_header_index = region_index++;

        memory_region_info_buffer.append((void*)&info, sizeof(info));
        // NOTE: The region name *is* null-terminated, so the following is ok:
        auto name = region->name();
        if (name.is_empty()) {
            char null_terminator = '\0';
            memory_region_info_buffer.append(&null_terminator, 1);
        } else {
            memory_region_info_buffer.append(name.characters_without_null_termination(), name.length() + 1);
        }

        regions_data += memory_region_info_buffer;
    }
    return regions_data;
}

ByteBuffer CoreDump::create_notes_metadata_data() const
{
    ByteBuffer metadata_data;

    ELF::Core::Metadata metadata {};
    metadata.header.type = ELF::Core::NotesEntryHeader::Type::Metadata;
    metadata_data.append((void*)&metadata, sizeof(metadata));

    StringBuilder builder;
    {
        JsonObjectSerializer metadata_obj { builder };
        m_process->for_each_coredump_property([&](auto& key, auto& value) {
            metadata_obj.add(key.view(), value.view());
        });
    }
    builder.append(0);
    metadata_data.append(builder.string_view().characters_without_null_termination(), builder.length());

    return metadata_data;
}

ByteBuffer CoreDump::create_notes_segment_data() const
{
    ByteBuffer notes_buffer;

    notes_buffer += create_notes_process_data();
    notes_buffer += create_notes_threads_data();
    notes_buffer += create_notes_regions_data();
    notes_buffer += create_notes_metadata_data();

    ELF::Core::NotesEntryHeader null_entry {};
    null_entry.type = ELF::Core::NotesEntryHeader::Type::Null;
    notes_buffer.append(&null_entry, sizeof(null_entry));

    return notes_buffer;
}

KResult CoreDump::write()
{
    SpinlockLocker lock(m_process->address_space().get_lock());
    ProcessPagingScope scope(m_process);

    ByteBuffer notes_segment = create_notes_segment_data();

    auto result = write_elf_header();
    if (result.is_error())
        return result;
    result = write_program_headers(notes_segment.size());
    if (result.is_error())
        return result;
    result = write_regions();
    if (result.is_error())
        return result;
    result = write_notes_segment(notes_segment);
    if (result.is_error())
        return result;

    return m_fd->chmod(0600); // Make coredump file read/writable
}

}
