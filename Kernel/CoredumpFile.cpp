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
#include <Kernel/CoredumpFile.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Jail.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>
#include <LibC/elf.h>
#include <LibELF/Core.h>

#define INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS 0

namespace Kernel {

static Singleton<SpinlockProtected<CoredumpFile::List>> s_all_instances;

SpinlockProtected<CoredumpFile::List>& CoredumpFile::all_instances()
{
    return s_all_instances;
}

ErrorOr<void> CoredumpFile::for_each_in_same_associated_jail(Function<ErrorOr<void>(CoredumpFile&)> callback)
{
    TRY(Process::current().jail().with([&](auto& my_jail) -> ErrorOr<void> {
        TRY(CoredumpFile::all_instances().with([&](auto const& list) -> ErrorOr<void> {
            if (my_jail) {
                for (auto& coredump : list) {
                    if (!coredump.process_was_associated_to_jail())
                        continue;
                    auto coredump_associated_jail = coredump.associated_jail();
                    LockRefPtr<Jail> associated_jail = coredump_associated_jail.strong_ref();
                    if (my_jail.ptr() == associated_jail.ptr() && !associated_jail.is_null())
                        TRY(callback(coredump));
                }
                return {};
            }
            for (auto& coredump : list) {
                TRY(callback(coredump));
            }
            return {};
        }));
        return {};
    }));
    return {};
}

LockRefPtr<CoredumpFile> CoredumpFile::from_pid_in_same_associated_jail(ProcessID pid)
{
    return Process::current().jail().with([&](auto& my_jail) -> LockRefPtr<CoredumpFile> {
        return CoredumpFile::all_instances().with([&](auto const& list) -> LockRefPtr<CoredumpFile> {
            if (my_jail) {
                for (auto& coredump : list) {
                    if (coredump.associated_pid() == pid) {
                        if (!coredump.process_was_associated_to_jail())
                            return {};
                        auto coredump_associated_jail = coredump.associated_jail().strong_ref();
                        if (my_jail.ptr() == coredump_associated_jail.ptr() && !coredump_associated_jail.is_null())
                            return coredump;
                        return {};
                    }
                }
                return {};
            }
            for (auto& coredump : list) {
                if (coredump.associated_pid() == pid) {
                    return coredump;
                }
            }
            return {};
        });
    });
}

bool CoredumpFile::FlatRegionData::looks_like_userspace_heap_region() const
{
    return name().starts_with("LibJS:"sv) || name().starts_with("malloc:"sv);
}

bool CoredumpFile::FlatRegionData::is_consistent_with_region(Memory::Region const& region) const
{
    if (m_access != region.access())
        return false;

    if (m_page_count != region.page_count() || m_size != region.size())
        return false;

    if (m_vaddr != region.vaddr())
        return false;

    return true;
}

static ErrorOr<void> write_elf_header(KBufferBuilder& builder, size_t num_program_headers)
{
    ElfW(Ehdr) elf_file_header;
    elf_file_header.e_ident[EI_MAG0] = 0x7f;
    elf_file_header.e_ident[EI_MAG1] = 'E';
    elf_file_header.e_ident[EI_MAG2] = 'L';
    elf_file_header.e_ident[EI_MAG3] = 'F';
#if ARCH(I386)
    elf_file_header.e_ident[EI_CLASS] = ELFCLASS32;
#elif ARCH(X86_64) || ARCH(AARCH64)
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
#if ARCH(I386)
    elf_file_header.e_machine = EM_386;
#elif ARCH(X86_64)
    elf_file_header.e_machine = EM_X86_64;
#elif ARCH(AARCH64)
    elf_file_header.e_machine = EM_AARCH64;
#else
#    error Unknown architecture
#endif
    elf_file_header.e_version = 1;
    elf_file_header.e_entry = 0;
    elf_file_header.e_phoff = sizeof(ElfW(Ehdr));
    elf_file_header.e_shoff = 0;
    elf_file_header.e_flags = 0;
    elf_file_header.e_ehsize = sizeof(ElfW(Ehdr));
    elf_file_header.e_shentsize = sizeof(ElfW(Shdr));
    elf_file_header.e_phentsize = sizeof(ElfW(Phdr));
    elf_file_header.e_phnum = num_program_headers;
    elf_file_header.e_shnum = 0;
    elf_file_header.e_shstrndx = SHN_UNDEF;

    TRY(builder.append_bytes(ReadonlyBytes { reinterpret_cast<uint8_t*>(&elf_file_header), sizeof(ElfW(Ehdr)) }));
    return {};
}

static ErrorOr<void> write_regions(NonnullLockRefPtr<Process> process, Vector<CoredumpFile::FlatRegionData>& regions, KBufferBuilder& builder)
{
    u8 zero_buffer[PAGE_SIZE] = {};

    for (auto& region : regions) {
        VERIFY(!region.is_kernel());

#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;

        auto buffer = TRY(KBuffer::try_create_with_size("Coredump Region Copy Buffer"sv, region.page_count() * PAGE_SIZE));

        TRY(process->address_space().with([&](auto& space) -> ErrorOr<void> {
            auto* real_region = space->region_tree().regions().find(region.vaddr().get());

            if (!real_region)
                return Error::from_string_view("Failed to find matching region in the process"sv);

            if (!region.is_consistent_with_region(*real_region))
                return Error::from_string_view("Found region does not match stored metadata"sv);

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

        TRY(builder.append_bytes(ReadonlyBytes { buffer->data(), buffer->size() }));
    }

    return {};
}

static ErrorOr<void> create_notes_process_data(NonnullLockRefPtr<Process> process, KBufferBuilder& builder)
{
    ELF::Core::ProcessInfo info {};
    info.header.type = ELF::Core::NotesEntryHeader::Type::ProcessInfo;
    TRY(builder.append_bytes(ReadonlyBytes { (void*)&info, sizeof(info) }));

    {
        auto process_obj = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(process_obj.add("pid"sv, process->pid().value()));
        TRY(process_obj.add("termination_signal"sv, process->termination_signal()));
        TRY(process_obj.add("executable_path"sv, process->executable() ? TRY(process->executable()->try_serialize_absolute_path())->view() : ""sv));

        {
            auto arguments_array = TRY(process_obj.add_array("arguments"sv));
            for (auto const& argument : process->arguments())
                TRY(arguments_array.add(argument.view()));
            TRY(arguments_array.finish());
        }

        {
            auto environment_array = TRY(process_obj.add_array("environment"sv));
            for (auto const& variable : process->environment())
                TRY(environment_array.add(variable.view()));
            TRY(environment_array.finish());
        }

        TRY(process_obj.finish());
    }

    TRY(builder.append('\0'));
    return {};
}

ErrorOr<void> CoredumpFile::create_notes_threads_data(NonnullLockRefPtr<Process> process, KBufferBuilder& builder)
{
    for (auto const& thread : process->threads_for_coredump({})) {
        ELF::Core::ThreadInfo info {};
        info.header.type = ELF::Core::NotesEntryHeader::Type::ThreadInfo;
        info.tid = thread.tid().value();

        if (thread.current_trap())
            copy_kernel_registers_into_ptrace_registers(info.regs, thread.get_register_dump_from_stack());

        TRY(builder.append_bytes(ReadonlyBytes { &info, sizeof(info) }));
    }
    return {};
}

static ErrorOr<void> create_notes_regions_data(Vector<CoredumpFile::FlatRegionData>& regions, KBufferBuilder& builder)
{
    size_t region_index = 0;
    for (auto const& region : regions) {
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

static ErrorOr<void> create_notes_metadata_data(NonnullLockRefPtr<Process> process, KBufferBuilder& builder)
{
    ELF::Core::Metadata metadata {};
    metadata.header.type = ELF::Core::NotesEntryHeader::Type::Metadata;
    TRY(builder.append_bytes(ReadonlyBytes { (void*)&metadata, sizeof(metadata) }));

    {
        auto metadata_obj = TRY(JsonObjectSerializer<>::try_create(builder));
        TRY(process->for_each_coredump_property([&](auto& key, auto& value) -> ErrorOr<void> {
            TRY(metadata_obj.add(key.view(), value.view()));
            return {};
        }));
        TRY(metadata_obj.finish());
    }
    TRY(builder.append('\0'));
    return {};
}

ErrorOr<void> CoredumpFile::create_notes_segment_data_buffer(NonnullLockRefPtr<Process> process, Vector<CoredumpFile::FlatRegionData>& regions, KBufferBuilder& builder)
{
    TRY(create_notes_process_data(process, builder));
    TRY(CoredumpFile::create_notes_threads_data(process, builder));
    TRY(create_notes_regions_data(regions, builder));
    TRY(create_notes_metadata_data(process, builder));

    ELF::Core::NotesEntryHeader null_entry {};
    null_entry.type = ELF::Core::NotesEntryHeader::Type::Null;
    TRY(builder.append(ReadonlyBytes { &null_entry, sizeof(null_entry) }));

    return {};
}

static ErrorOr<void> write_program_headers(KBufferBuilder& builder, Vector<CoredumpFile::FlatRegionData>& regions, size_t num_program_headers, size_t notes_size)
{
    size_t offset = sizeof(ElfW(Ehdr)) + num_program_headers * sizeof(ElfW(Phdr));
    for (auto& region : regions) {
#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif

        if (region.access() == Memory::Region::Access::None)
            continue;

        ElfW(Phdr) phdr {};

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

        TRY(builder.append(ReadonlyBytes { reinterpret_cast<uint8_t*>(&phdr), sizeof(ElfW(Phdr)) }));
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

    TRY(builder.append(ReadonlyBytes { reinterpret_cast<uint8_t*>(&notes_pheader), sizeof(ElfW(Phdr)) }));

    return {};
}

ErrorOr<size_t> CoredumpFile::read(Badge<CoredumpFSInode>, u64 offset, UserOrKernelBuffer& buffer, size_t length)
{
    MutexLocker locker(m_content_lock);
    if (!m_content)
        return 0;
    if (offset > m_content->size())
        return 0;
    size_t nread = min(m_content->size() - offset, length);
    TRY(buffer.write(m_content->bytes().slice(offset, nread)));
    return nread;
}

void CoredumpFile::truncate(Badge<CoredumpFSInode>)
{
    MutexLocker locker(m_content_lock);
    m_content.clear();
}

size_t CoredumpFile::size() const
{
    MutexLocker locker(m_content_lock);
    if (!m_content)
        return 0;
    return m_content->size();
}

ErrorOr<NonnullLockRefPtr<CoredumpFile>> CoredumpFile::try_create(NonnullLockRefPtr<Process> process)
{
    if (!process->is_dumpable()) {
        dbgln("Refusing to generate CoredumpFile for non-dumpable process {}", process->pid().value());
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

    size_t num_program_headers = 0;
    for (auto& region : regions) {
#if !INCLUDE_USERSPACE_HEAP_MEMORY_IN_COREDUMPS
        if (region.looks_like_userspace_heap_region())
            continue;
#endif
        if (region.access() == Memory::Region::Access::None)
            continue;
        ++num_program_headers;
    }
    ++num_program_headers; // +1 for NOTE segment

    // NOTE: The coredump layout is as follows:
    // 1. ELF Header
    // 2. ELF Program Headers
    // 3. Dumped Process Regions
    // 4. Notes Segment
    // We are able to store all main content on a KBuffer, but because to be able
    // to write program headers correctly, we generate the notes segment data
    // beforehand to provide an accurate calculation for its program header.

    auto notes_segment_data_buffer_builder = TRY(KBufferBuilder::try_create());
    auto contents_buffer_builder = TRY(KBufferBuilder::try_create());
    TRY(create_notes_segment_data_buffer(process, regions, notes_segment_data_buffer_builder));
    TRY(write_elf_header(contents_buffer_builder, num_program_headers));
    TRY(write_program_headers(contents_buffer_builder, regions, num_program_headers, notes_segment_data_buffer_builder.length()));

    // NOTE: We only need to switch briefly to copy actual memory regions from the other dumped process.
    {
        ScopedAddressSpaceSwitcher switcher(*process);
        TRY(write_regions(process, regions, contents_buffer_builder));
    }

    auto notes_segment_data_buffer = notes_segment_data_buffer_builder.build();
    if (!notes_segment_data_buffer)
        return Error::from_errno(ENOMEM);
    TRY(contents_buffer_builder.append_bytes(notes_segment_data_buffer->bytes()));

    auto contents_buffer = contents_buffer_builder.build();
    if (!contents_buffer)
        return Error::from_errno(ENOMEM);

    RefPtr<Jail> jail = process->jail().with([&](auto& jail) { return jail; });
    auto uid = process->procfs_traits()->owner_user();
    auto gid = process->procfs_traits()->owner_group();
    if (jail)
        return adopt_nonnull_lock_ref_or_enomem(new (nothrow) CoredumpFile(process->pid(), uid, gid, *jail, contents_buffer.release_nonnull()));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) CoredumpFile(process->pid(), uid, gid, contents_buffer.release_nonnull()));
}

CoredumpFile::CoredumpFile(ProcessID associated_pid, UserID associated_uid, GroupID associated_gid, Jail& jail, NonnullOwnPtr<KBuffer> main_content)
    : m_associated_jail(jail)
    , m_process_was_associated_to_jail(true)
    , m_associated_pid(associated_pid)
    , m_associated_uid(associated_uid)
    , m_associated_gid(associated_gid)
    , m_content(move(main_content))
    , m_creation_time(TimeManagement::now())
{
}

CoredumpFile::CoredumpFile(ProcessID associated_pid, UserID associated_uid, GroupID associated_gid, NonnullOwnPtr<KBuffer> main_content)
    : m_process_was_associated_to_jail(false)
    , m_associated_pid(associated_pid)
    , m_associated_uid(associated_uid)
    , m_associated_gid(associated_gid)
    , m_content(move(main_content))
    , m_creation_time(TimeManagement::now())
{
}

}
