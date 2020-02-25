/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/DiskPartition.h>
#include <Kernel/Devices/MBRPartitionTable.h>

namespace Kernel {

struct [[gnu::packed]] EBRPartitionExtension
{
    u8 unused_area[446];
    MBRPartitionEntry entry;
    MBRPartitionEntry next_chained_ebr_extension;
    MBRPartitionEntry unused[2];
    u16 mbr_signature;
};

class EBRPartitionTable {

public:
    explicit EBRPartitionTable(NonnullRefPtr<BlockDevice>);
    ~EBRPartitionTable();

    bool initialize();
    RefPtr<DiskPartition> partition(unsigned index);

private:
    int index_of_ebr_container() const;
    NonnullRefPtr<BlockDevice> m_device;

    const MBRPartitionHeader& header() const;
    const EBRPartitionExtension& ebr_extension() const;

    bool index_is_extended_partition(unsigned index) const;

    RefPtr<DiskPartition> get_extended_partition(unsigned index);
    RefPtr<DiskPartition> get_non_extended_partition(unsigned index);
    u8 m_ebr_container_id { 0 };
    size_t m_ebr_chained_extensions_count { 0 };
    u8 m_cached_mbr_header[512];
    u8 m_cached_ebr_header[512];
};

}
