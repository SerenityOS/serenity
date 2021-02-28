/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <Kernel/CoreDump.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/ProcessPagingScope.h>
#include <LibELF/CoreDump.h>
#include <LibELF/exec_elf.h>

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
    return adopt_own(*new CoreDump(move(process), fd.release_nonnull()));
}

CoreDump::CoreDump(NonnullRefPtr<Process> process, NonnullRefPtr<FileDescription>&& fd)
    : m_process(move(process))
    , m_fd(move(fd))
    , m_num_program_headers(m_process->space().region_count() + 1) // +1 for NOTE segment
{
}

RefPtr<FileDescription> CoreDump::create_target_file(const Process& process, const String& output_path)
{
    LexicalPath lexical_path(output_path);
    const auto& output_directory = lexical_path.dirname();
    auto dump_directory = VFS::the().open_directory(output_directory, VFS::the().root_custody());
    if (dump_directory.is_error()) {
        dbgln("Can't find directory '{}' for core dump", output_directory);
        return nullptr;
    }
    auto dump_directory_metadata = dump_directory.value()->inode().metadata();
    if (dump_directory_metadata.uid != 0 || dump_directory_metadata.gid != 0 || dump_directory_metadata.mode != 040755) {
        dbgln("Refusing to put core dump in sketchy directory '{}'", output_directory);
        return nullptr;
    }
    auto fd_or_error = VFS::the().open(
        lexical_path.basename(),
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
    Elf32_Ehdr elf_file_header;
    elf_file_header.e_ident[EI_MAG0] = 0x7f;
    elf_file_header.e_ident[EI_MAG1] = 'E';
    elf_file_header.e_ident[EI_MAG2] = 'L';
    elf_file_header.e_ident[EI_MAG3] = 'F';
    elf_file_header.e_ident[EI_CLASS] = ELFCLASS32;
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
    elf_file_header.e_machine = EM_386;
    elf_file_header.e_version = 1;
    elf_file_header.e_entry = 0;
    elf_file_header.e_phoff = sizeof(Elf32_Ehdr);
    elf_file_header.e_shoff = 0;
    elf_file_header.e_flags = 0;
    elf_file_header.e_ehsize = sizeof(Elf32_Ehdr);
    elf_file_header.e_shentsize = sizeof(Elf32_Shdr);
    elf_file_header.e_phentsize = sizeof(Elf32_Phdr);
    elf_file_header.e_phnum = m_num_program_headers;
    elf_file_header.e_shnum = 0;
    elf_file_header.e_shstrndx = SHN_UNDEF;

    auto result = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&elf_file_header)), sizeof(Elf32_Ehdr));
    if (result.is_error())
        return result.error();
    return KSuccess;
}

KResult CoreDump::write_program_headers(size_t notes_size)
{
    size_t offset = sizeof(Elf32_Ehdr) + m_num_program_headers * sizeof(Elf32_Phdr);
    for (auto& region : m_process->space().regions()) {
        Elf32_Phdr phdr {};

        phdr.p_type = PT_LOAD;
        phdr.p_offset = offset;
        phdr.p_vaddr = region.vaddr().get();
        phdr.p_paddr = 0;

        phdr.p_filesz = region.page_count() * PAGE_SIZE;
        phdr.p_memsz = region.page_count() * PAGE_SIZE;
        phdr.p_align = 0;

        phdr.p_flags = region.is_readable() ? PF_R : 0;
        if (region.is_writable())
            phdr.p_flags |= PF_W;
        if (region.is_executable())
            phdr.p_flags |= PF_X;

        offset += phdr.p_filesz;

        [[maybe_unused]] auto rc = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&phdr)), sizeof(Elf32_Phdr));
    }

    Elf32_Phdr notes_pheader {};
    notes_pheader.p_type = PT_NOTE;
    notes_pheader.p_offset = offset;
    notes_pheader.p_vaddr = 0;
    notes_pheader.p_paddr = 0;
    notes_pheader.p_filesz = notes_size;
    notes_pheader.p_memsz = notes_size;
    notes_pheader.p_align = 0;
    notes_pheader.p_flags = 0;

    auto result = m_fd->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&notes_pheader)), sizeof(Elf32_Phdr));
    if (result.is_error())
        return result.error();
    return KSuccess;
}

KResult CoreDump::write_regions()
{
    for (auto& region : m_process->space().regions()) {
        if (region.is_kernel())
            continue;

        region.set_readable(true);
        region.remap();

        for (size_t i = 0; i < region.page_count(); i++) {
            auto* page = region.physical_page(i);

            uint8_t zero_buffer[PAGE_SIZE] = {};
            Optional<UserOrKernelBuffer> src_buffer;

            if (page) {
                src_buffer = UserOrKernelBuffer::for_user_buffer(reinterpret_cast<uint8_t*>((region.vaddr().as_ptr() + (i * PAGE_SIZE))), PAGE_SIZE);
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

    JsonObject process_obj;
    process_obj.set("pid", m_process->pid().value());
    process_obj.set("termination_signal", m_process->termination_signal());
    process_obj.set("executable_path", m_process->executable() ? m_process->executable()->absolute_path() : String::empty());
    process_obj.set("arguments", JsonArray(m_process->arguments()));
    process_obj.set("environment", JsonArray(m_process->environment()));

    auto json_data = process_obj.to_string();
    process_data.append(json_data.characters(), json_data.length() + 1);

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
        copy_kernel_registers_into_ptrace_registers(info.regs, thread.get_register_dump_from_stack());

        entry_buff.append((void*)&info, sizeof(info));

        threads_data += entry_buff;
    }
    return threads_data;
}

ByteBuffer CoreDump::create_notes_regions_data() const
{
    ByteBuffer regions_data;
    for (size_t region_index = 0; region_index < m_process->space().region_count(); ++region_index) {

        ByteBuffer memory_region_info_buffer;
        ELF::Core::MemoryRegionInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo;

        auto& region = m_process->space().regions()[region_index];
        info.region_start = region.vaddr().get();
        info.region_end = region.vaddr().offset(region.size()).get();
        info.program_header_index = region_index;

        memory_region_info_buffer.append((void*)&info, sizeof(info));

        auto name = region.name();
        if (name.is_null())
            name = String::empty();
        memory_region_info_buffer.append(name.characters(), name.length() + 1);

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

    JsonObject metadata_obj;
    for (auto& it : m_process->coredump_metadata())
        metadata_obj.set(it.key, it.value);
    auto json_data = metadata_obj.to_string();
    metadata_data.append(json_data.characters(), json_data.length() + 1);

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
    ScopedSpinLock lock(m_process->space().get_lock());
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

    return m_fd->chmod(0400); // Make coredump file readable
}

}
