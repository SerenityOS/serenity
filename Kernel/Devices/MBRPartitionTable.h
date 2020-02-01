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

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/Devices/DiskPartition.h>

#define MBR_SIGNATURE 0xaa55
#define MBR_PROTECTIVE 0xEE

struct [[gnu::packed]] MBRPartitionEntry
{
    u8 status;
    u8 chs1[3];
    u8 type;
    u8 chs2[3];
    u32 offset;
    u32 length;
};

struct [[gnu::packed]] MBRPartitionHeader
{
    u8 code1[218];
    u16 ts_zero;
    u8 ts_drive, ts_seconds, ts_minutes, ts_hours;
    u8 code2[216];
    u32 disk_signature;
    u16 disk_signature_zero;
    MBRPartitionEntry entry[4];
    u16 mbr_signature;
};

class MBRPartitionTable {
    AK_MAKE_ETERNAL

public:
    MBRPartitionTable(NonnullRefPtr<DiskDevice>);
    ~MBRPartitionTable();

    bool initialize();
    bool is_protective_mbr() const;
    RefPtr<DiskPartition> partition(unsigned index);

private:
    NonnullRefPtr<DiskDevice> m_device;

    const MBRPartitionHeader& header() const;

    u8 m_cached_header[512];
};
