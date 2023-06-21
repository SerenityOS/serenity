/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObjectSerializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Kernel/MemoryStatus.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT SysFSMemoryStatus::SysFSMemoryStatus(SysFSDirectory const& parent_directory)
    : SysFSGlobalInformation(parent_directory)
{
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSMemoryStatus> SysFSMemoryStatus::must_create(SysFSDirectory const& parent_directory)
{
    return adopt_ref_if_nonnull(new (nothrow) SysFSMemoryStatus(parent_directory)).release_nonnull();
}

ErrorOr<void> SysFSMemoryStatus::try_generate(KBufferBuilder& builder)
{
    kmalloc_stats stats;
    get_kmalloc_stats(stats);

    auto system_memory = MM.get_system_memory_info();

    auto json = TRY(JsonObjectSerializer<>::try_create(builder));
    TRY(json.add("kmalloc_allocated"sv, stats.bytes_allocated));
    TRY(json.add("kmalloc_available"sv, stats.bytes_free));
    TRY(json.add("physical_allocated"sv, system_memory.physical_pages_used));
    TRY(json.add("physical_available"sv, system_memory.physical_pages - system_memory.physical_pages_used));
    TRY(json.add("physical_committed"sv, system_memory.physical_pages_committed));
    TRY(json.add("physical_uncommitted"sv, system_memory.physical_pages_uncommitted));
    TRY(json.add("kmalloc_call_count"sv, stats.kmalloc_call_count));
    TRY(json.add("kfree_call_count"sv, stats.kfree_call_count));
    TRY(json.finish());
    return {};
}

}
