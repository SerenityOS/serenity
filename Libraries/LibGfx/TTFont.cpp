/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
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

#include "TTFont.h"
#include <AK/LogStream.h>
#include <bits/stdint.h>
#include <LibCore/File.h>

namespace Gfx {

static u16 be_u16(const u8* ptr)
{
    return (((u16) ptr[0]) << 8) | ((u16) ptr[1]);
}

static u32 be_u32(const u8* ptr)
{
    return (((u32) ptr[0]) << 24) | (((u32) ptr[1]) << 16) | (((u32) ptr[2]) << 8) | ((u32) ptr[3]);
}

static i16 be_i16(const u8* ptr)
{
    return (((i16) ptr[0]) << 8) | ((i16) ptr[1]);
}

static u32 tag_from_str(const char *str)
{
    return be_u32((const u8*) str);
}

u16 TTFHead::units_per_em() const
{
    return be_u16(m_slice.offset_pointer(18));
}

i16 TTFHead::xmin() const
{
    return be_i16(m_slice.offset_pointer(36));
}

i16 TTFHead::ymin() const
{
    return be_i16(m_slice.offset_pointer(38));
}

i16 TTFHead::xmax() const
{
    return be_i16(m_slice.offset_pointer(40));
}

i16 TTFHead::ymax() const
{
    return be_i16(m_slice.offset_pointer(42));
}

u16 TTFHead::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer(46));
}

Result<TTFIndexToLocFormat, i16> TTFHead::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer(50));
    switch (raw) {
    case 0:
        return TTFIndexToLocFormat::Offset16;
    case 1:
        return TTFIndexToLocFormat::Offset32;
    default:
        return raw;
    }
}

OwnPtr<TTFont> TTFont::load_from_file(const StringView& path, unsigned index)
{
    dbg() << "path: " << path << " | index: " << index;
    auto file_or_error = Core::File::open(String(path), Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        dbg() << "Could not open file: " << file_or_error.error();
        return nullptr;
    }
    auto file = file_or_error.value();
    if (!file->open(Core::IODevice::ReadOnly)) {
        dbg() << "Could not open file";
        return nullptr;
    }
    auto buffer = file->read_all();
    if (buffer.size() < 4) {
        dbg() << "Font file too small";
        return nullptr;
    }
    u32 tag = be_u32(buffer.data());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        if (buffer.size() < 12 + 4 * (index + 1)) {
            dbg() << "Font file too small";
            return nullptr;
        }
        u32 offset = be_u32(buffer.offset_pointer(12 + 4 * index));
        return OwnPtr(new TTFont(move(buffer), offset));
    } else if (tag == tag_from_str("OTTO")) {
        dbg() << "CFF fonts not supported yet";
        return nullptr;
    } else if (tag != 0x00010000) {
        dbg() << "Not a valid TTF font";
        return nullptr;
    } else {
        return OwnPtr(new TTFont(move(buffer), 0));
    }
}

TTFont::TTFont(AK::ByteBuffer&& buffer, u32 offset)
    : m_buffer(move(buffer))
    {
        ASSERT(m_buffer.size() >= offset + 12);
        bool head_has_been_initialized = false;

        //auto sfnt_version = be_u32(data + offset);
        auto num_tables = be_u16(m_buffer.offset_pointer(offset + 4));
        ASSERT(m_buffer.size() >= offset + 12 + num_tables * 16);

        for (auto i = 0; i < num_tables; i++) {
            u32 record_offset = offset + 12 + i * 16;
            u32 tag = be_u32(m_buffer.offset_pointer(record_offset));
            u32 table_offset = be_u32(m_buffer.offset_pointer(record_offset + 8));
            u32 table_length = be_u32(m_buffer.offset_pointer(record_offset + 12));
            ASSERT(m_buffer.size() >= table_offset + table_length);

            // Get the tables we need
            if (tag == tag_from_str("head")) {
                auto buffer = ByteBuffer::wrap(m_buffer.offset_pointer(table_offset), table_length);
                m_head = TTFHead(move(buffer));
                head_has_been_initialized = true;
            }
        }

        // Check that we've got everything we need
        ASSERT(head_has_been_initialized);
    }
}
