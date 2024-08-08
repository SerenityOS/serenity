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
#include <AK/Singleton.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Tasks/Coredump.h>
#include <Kernel/Tasks/Process.h>
#include <LibELF/Core.h>
#include <LibELF/ELFABI.h>

#define INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS 0

static Singleton<SpinlockProtected<OwnPtr<KString>, LockRank::None>> s_coredump_directory_path;

namespace Kernel {

SpinlockProtected<OwnPtr<KString>, LockRank::None>& Coredump::directory_path()
{
    return s_coredump_directory_path;
}

bool Coredump::FlatRegionData::looks_like_userspace_heap_region() const
{
    return name().starts_with("LibJS:"sv) || name().starts_with("malloc:"sv);
}

bool Coredump::FlatRegionData::is_consistent_with_region(Memory::Region const& region) const
{
    if (m_access != region.access())
        return false;

    if (m_page_count != region.page_count() || m_size != region.size())
        return false;

    if (m_vaddr != region.vaddr())
        return false;

    return true;
}

ErrorOr<NonnullOwnPtr<Coredump>> Coredump::try_create(NonnullRefPtr<Process> process, StringView output_path)
{
    if (!process->is_dumpable()) {
        dbgln("Refusing to generate coredump for non-dumpable process {}", process->pid().value());
        return EPERM;
    }

    Vector<FlatRegionData> regions;
    size_t number_of_regions = process->address_space().with([](auto& space) {
        return space->region_tree().regions().size();
    });
    TRY(regions.try_ensure_capacity(number_of_regions));
    TRY(process->address_space().with([&](auto& space) -> ErrorOr<void> {
        for (auto& region : space->region_tree().regions())
            TRY(regions.try_empend(region, TRY(KString::try_create(region.name()))));
        return {};
    }));

    auto description = TRY(try_create_target_file(process, output_path));
    return adopt_nonnull_own_or_enomem(new (nothrow) Coredump(move(process), move(description), move(regions)));
}

Coredump::Coredump(NonnullRefPtr<Process> process, NonnullRefPtr<OpenFileDescription> description, Vector<FlatRegionData> regions)
    : m_process(move(process))
    , m_description(move(description))
    , m_regions(move(regions))
{
    m_num_program_headers = 0;
    for (auto& region : m_regions) {
#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;
        ++m_num_program_headers;
    }
    ++m_num_program_headers; // +1 for NOTE segment
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> Coredump::try_create_target_file(Process const& process, StringView output_path)
{
    auto output_directory = KLexicalPath::dirname(output_path);
    auto process_vfs_root_context = process.vfs_root_context();
    auto vfs_root_context_root_custody = process_vfs_root_context->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
        return custody;
    });
    auto dump_directory = TRY(VirtualFileSystem::open_directory(process_vfs_root_context, Process::current().credentials(), output_directory, *vfs_root_context_root_custody));
    auto dump_directory_metadata = dump_directory->inode().metadata();
    if (dump_directory_metadata.uid != 0 || dump_directory_metadata.gid != 0 || dump_directory_metadata.mode != 040777) {
        dbgln("Refusing to put coredump in sketchy directory '{}'", output_directory);
        return EINVAL;
    }

    auto process_credentials = process.credentials();
    return TRY(VirtualFileSystem::open(
        process_vfs_root_context,
        Process::current().credentials(),
        KLexicalPath::basename(output_path),
        O_CREAT | O_WRONLY | O_EXCL,
        S_IFREG, // We will enable reading from userspace when we finish generating the coredump file
        dump_directory,
        UidAndGid { process_credentials->uid(), process_credentials->gid() }));
}

ErrorOr<void> Coredump::write_elf_header()
{
    Elf_Ehdr elf_file_header;
    elf_file_header.e_ident[EI_MAG0] = 0x7f;
    elf_file_header.e_ident[EI_MAG1] = 'E';
    elf_file_header.e_ident[EI_MAG2] = 'L';
    elf_file_header.e_ident[EI_MAG3] = 'F';
#if ARCH(X86_64) || ARCH(AARCH64) || ARCH(RISCV64)
    elf_file_header.e_ident[EI_CLASS] = ELFCLASS64;
#else
#    error Unknown architecture
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
#if ARCH(X86_64)
    elf_file_header.e_machine = EM_X86_64;
#elif ARCH(AARCH64)
    elf_file_header.e_machine = EM_AARCH64;
#elif ARCH(RISCV64)
    elf_file_header.e_machine = EM_RISCV;
#else
#    error Unknown architecture
#endif
    elf_file_header.e_version = 1;
    elf_file_header.e_entry = 0;
    elf_file_header.e_phoff = sizeof(Elf_Ehdr);
    elf_file_header.e_shoff = 0;
    elf_file_header.e_flags = 0;
    elf_file_header.e_ehsize = sizeof(Elf_Ehdr);
    elf_file_header.e_shentsize = sizeof(Elf_Shdr);
    elf_file_header.e_phentsize = sizeof(Elf_Phdr);
    elf_file_header.e_phnum = m_num_program_headers;
    elf_file_header.e_shnum = 0;
    elf_file_header.e_shstrndx = SHN_UNDEF;

    TRY(m_description->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&elf_file_header)), sizeof(Elf_Ehdr)));

    return {};
}

ErrorOr<void> Coredump::write_program_headers(size_t notes_size)
{
    size_t offset = sizeof(Elf_Ehdr) + m_num_program_headers * sizeof(Elf_Phdr);
    for (auto& region : m_regions) {
#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;

        Elf_Phdr phdr {};

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

        [[maybe_unused]] auto rc = m_description->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&phdr)), sizeof(Elf_Phdr));
    }

    Elf_Phdr notes_pheader {};
    notes_pheader.p_type = PT_NOTE;
    notes_pheader.p_offset = offset;
    notes_pheader.p_vaddr = 0;
    notes_pheader.p_paddr = 0;
    notes_pheader.p_filesz = notes_size;
    notes_pheader.p_memsz = notes_size;
    notes_pheader.p_align = 0;
    notes_pheader.p_flags = 0;

    TRY(m_description->write(UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<uint8_t*>(&notes_pheader)), sizeof(Elf_Phdr)));

    return {};
}

ErrorOr<void> Coredump::write_regions()
{
    u8 zero_buffer[PAGE_SIZE] = {};

    for (auto& region : m_regions) {
        VERIFY(!region.is_kernel());

#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;

        auto buffer = TRY(KBuffer::try_create_with_size("Coredump Region Copy Buffer"sv, region.page_count() * PAGE_SIZE));

        TRY(m_process->address_space().with([&](auto& space) -> ErrorOr<void> {
            auto* real_region = space->region_tree().regions().find(region.vaddr().get());

            if (!real_region) {
                dmesgln("Coredump::write_regions: Failed to find matching region in the process");
                return Error::from_errno(EFAULT);
            }

            if (!region.is_consistent_with_region(*real_region)) {
                dmesgln("Coredump::write_regions: Found region does not match stored metadata");
                return Error::from_errno(EINVAL);
            }

            // If we crashed in the middle of mapping in Regions, they do not have a page directory yet, and will crash on a remap() call
            if (!real_region->is_mapped())
                return {};

            real_region->set_readable(true);
            real_region->remap();

            for (size_t i = 0; i < region.page_count(); i++) {
                auto page = real_region->physical_page(i);
                auto src_buffer = [&]() -> ErrorOr<UserOrKernelBuffer> {
                    if (page)
                        return UserOrKernelBuffer::for_user_buffer(reinterpret_cast<uint8_t*>((region.vaddr().as_ptr() + (i * PAGE_SIZE))), PAGE_SIZE);
                    // If the current page is not backed by a physical page, we zero it in the coredump file.
                    return UserOrKernelBuffer::for_kernel_buffer(zero_buffer);
                }();
                TRY(src_buffer.value().read(buffer->bytes().slice(i * PAGE_SIZE, PAGE_SIZE)));
            }

            return {};
        }));

        TRY(m_description->write(UserOrKernelBuffer::for_kernel_buffer(buffer->data()), buffer->size()));
    }

    return {};
}

ErrorOr<void> Coredump::write_notes_segment(ReadonlyBytes notes_segment)
{
    TRY(m_description->write(UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>(notes_segment.data())), notes_segment.size()));
    return {};
}

ErrorOr<void> Coredump::create_notes_process_data(auto& builder) const
{
    ELF::Core::ProcessInfo info {};
    info.header.type = ELF::Core::NotesEntryHeader::Type::ProcessInfo;
    TRY(builder.append_bytes(ReadonlyBytes { (void*)&info, sizeof(info) }));

    {
        auto process_obj = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(process_obj.add("pid"sv, m_process->pid().value()));
        TRY(process_obj.add("termination_signal"sv, m_process->termination_signal()));
        TRY(process_obj.add("executable_path"sv, m_process->executable() ? TRY(m_process->executable()->try_serialize_absolute_path())->view() : ""sv));

        {
            auto arguments_array = TRY(process_obj.add_array("arguments"sv));
            for (auto const& argument : m_process->arguments())
                TRY(arguments_array.add(argument->view()));
            TRY(arguments_array.finish());
        }

        {
            auto environment_array = TRY(process_obj.add_array("environment"sv));
            for (auto const& variable : m_process->environment())
                TRY(environment_array.add(variable->view()));
            TRY(environment_array.finish());
        }

        TRY(process_obj.finish());
    }

    TRY(builder.append('\0'));
    return {};
}

ErrorOr<void> Coredump::create_notes_threads_data(auto& builder) const
{
    for (auto const& thread : m_process->threads_for_coredump({})) {
        ELF::Core::ThreadInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::ThreadInfo;
        info.tid = thread->tid().value();

        if (thread->current_trap())
            copy_kernel_registers_into_ptrace_registers(info.regs, thread->get_register_dump_from_stack());

        TRY(builder.append_bytes(ReadonlyBytes { &info, sizeof(info) }));
    }
    return {};
}

ErrorOr<void> Coredump::create_notes_regions_data(auto& builder) const
{
    size_t region_index = 0;
    for (auto const& region : m_regions) {
#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;

        ELF::Core::MemoryRegionInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::MemoryRegionInfo;

        info.region_start = region.vaddr().get();
        info.region_end = region.vaddr().offset(region.size()).get();
        info.program_header_index = region_index++;

        TRY(builder.append_bytes(ReadonlyBytes { (void*)&info, sizeof(info) }));

        // NOTE: The region name *is* null-terminated, so the following is ok:
        auto name = region.name();
        if (name.is_empty())
            TRY(builder.append('\0'));
        else
            TRY(builder.append(name.characters_without_null_termination(), name.length() + 1));
    }

    return {};
}

ErrorOr<void> Coredump::create_notes_metadata_data(auto& builder) const
{
    ELF::Core::Metadata metadata {};
    metadata.header.type = ELF::Core::NotesEntryHeader::Type::Metadata;
    TRY(builder.append_bytes(ReadonlyBytes { (void*)&metadata, sizeof(metadata) }));

    {
        auto metadata_obj = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(m_process->for_each_coredump_property([&](auto& key, auto& value) -> ErrorOr<void> {
            TRY(metadata_obj.add(key.view(), value.view()));
            return {};
        }));
        TRY(metadata_obj.finish());
    }
    TRY(builder.append('\0'));
    return {};
}

ErrorOr<void> Coredump::create_notes_segment_data(auto& builder) const
{
    TRY(create_notes_process_data(builder));
    TRY(create_notes_threads_data(builder));
    TRY(create_notes_regions_data(builder));
    TRY(create_notes_metadata_data(builder));

    ELF::Core::NotesEntryHeader null_entry {};
    null_entry.type = ELF::Core::NotesEntryHeader::Type::Null;
    TRY(builder.append(ReadonlyBytes { &null_entry, sizeof(null_entry) }));

    return {};
}

ErrorOr<void> Coredump::write()
{
    ScopedAddressSpaceSwitcher switcher(m_process);

    auto builder = TRY(KBufferBuilder::try_create());
    TRY(create_notes_segment_data(builder));
    TRY(write_elf_header());
    TRY(write_program_headers(builder.bytes().size()));
    TRY(write_regions());
    TRY(write_notes_segment(builder.bytes()));

    return m_description->chmod(Process::current().credentials(), 0600); // Make coredump file read/writable
}

}
