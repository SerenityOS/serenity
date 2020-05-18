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

#include <AK/StringView.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Process.h>

//#define FBFS_DEBUG

namespace Kernel {

struct CacheEntry {
    time_t timestamp { 0 };
    u32 block_index { 0 };
    u8* data { nullptr };
    bool has_data { false };
    bool is_dirty { false };
};

class DiskCache {
public:
    explicit DiskCache(FileBackedFS& fs)
        : m_fs(fs)
        , m_cached_block_data(KBuffer::create_with_size(m_entry_count * m_fs.block_size()))
        , m_entries(KBuffer::create_with_size(m_entry_count * sizeof(CacheEntry)))
    {
        for (size_t i = 0; i < m_entry_count; ++i) {
            entries()[i].data = m_cached_block_data.data() + i * m_fs.block_size();
        }
    }

    ~DiskCache() { }

    bool is_dirty() const { return m_dirty; }
    void set_dirty(bool b) { m_dirty = b; }

    CacheEntry& get(u32 block_index) const
    {
        auto now = kgettimeofday().tv_sec;

        CacheEntry* oldest_clean_entry = nullptr;
        for (size_t i = 0; i < m_entry_count; ++i) {
            auto& entry = const_cast<CacheEntry&>(entries()[i]);
            if (entry.block_index == block_index) {
                entry.timestamp = now;
                return entry;
            }
            if (!entry.is_dirty) {
                if (!oldest_clean_entry)
                    oldest_clean_entry = &entry;
                else if (entry.timestamp < oldest_clean_entry->timestamp)
                    oldest_clean_entry = &entry;
            }
        }
        if (!oldest_clean_entry) {
            // Not a single clean entry! Flush writes and try again.
            // NOTE: We want to make sure we only call FileBackedFS flush here,
            //       not some FileBackedFS subclass flush!
            m_fs.flush_writes_impl();
            return get(block_index);
        }

        // Replace the oldest clean entry.
        auto& new_entry = *oldest_clean_entry;
        new_entry.timestamp = now;
        new_entry.block_index = block_index;
        new_entry.has_data = false;
        new_entry.is_dirty = false;
        return new_entry;
    }

    const CacheEntry* entries() const { return (const CacheEntry*)m_entries.data(); }
    CacheEntry* entries() { return (CacheEntry*)m_entries.data(); }

    template<typename Callback>
    void for_each_entry(Callback callback)
    {
        for (size_t i = 0; i < m_entry_count; ++i)
            callback(entries()[i]);
    }

private:
    FileBackedFS& m_fs;
    size_t m_entry_count { 10000 };
    KBuffer m_cached_block_data;
    KBuffer m_entries;
    bool m_dirty { false };
};

FileBackedFS::FileBackedFS(FileDescription& file_description)
    : m_file_description(file_description)
{
    ASSERT(m_file_description->file().is_seekable());
}

FileBackedFS::~FileBackedFS()
{
}

bool FileBackedFS::write_block(unsigned index, const u8* data, size_t count, size_t offset, bool allow_cache)
{
    ASSERT(m_logical_block_size);
    ASSERT(offset + count <= block_size());
#ifdef FBFS_DEBUG
    klog() << "FileBackedFileSystem::write_block " << index << ", size=" << data.size();
#endif

    if (!allow_cache) {
        flush_specific_block_if_needed(index);
        u32 base_offset = static_cast<u32>(index) * static_cast<u32>(block_size()) + offset;
        m_file_description->seek(base_offset, SEEK_SET);
        auto nwritten = m_file_description->write(data, count);
        if (nwritten < 0)
            return false;
        ASSERT(static_cast<size_t>(nwritten) == count);
        return true;
    }

    auto& entry = cache().get(index);
    if (count < block_size()) {
        // Fill the cache first.
        read_block(index, nullptr, block_size());
    }
    memcpy(entry.data + offset, data, count);
    entry.is_dirty = true;
    entry.has_data = true;

    cache().set_dirty(true);
    return true;
}

bool FileBackedFS::raw_read(unsigned index, u8* buffer)
{
    u32 base_offset = static_cast<u32>(index) * static_cast<u32>(m_logical_block_size);
    m_file_description->seek(base_offset, SEEK_SET);
    auto nread = m_file_description->read(buffer, m_logical_block_size);
    ASSERT((size_t)nread == m_logical_block_size);
    return true;
}
bool FileBackedFS::raw_write(unsigned index, const u8* buffer)
{
    u32 base_offset = static_cast<u32>(index) * static_cast<u32>(m_logical_block_size);
    m_file_description->seek(base_offset, SEEK_SET);
    auto nwritten = m_file_description->write(buffer, m_logical_block_size);
    ASSERT((size_t)nwritten == m_logical_block_size);
    return true;
}

bool FileBackedFS::raw_read_blocks(unsigned index, size_t count, u8* buffer)
{
    for (unsigned block = index; block < (index + count); block++) {
        if (!raw_read(block, buffer))
            return false;
        buffer += logical_block_size();
    }
    return true;
}
bool FileBackedFS::raw_write_blocks(unsigned index, size_t count, const u8* buffer)
{
    for (unsigned block = index; block < (index + count); block++) {
        if (!raw_write(block, buffer))
            return false;
        buffer += logical_block_size();
    }
    return true;
}

bool FileBackedFS::write_blocks(unsigned index, unsigned count, const u8* data, bool allow_cache)
{
    ASSERT(m_logical_block_size);
#ifdef FBFS_DEBUG
    klog() << "FileBackedFileSystem::write_blocks " << index << " x" << count;
#endif
    for (unsigned i = 0; i < count; ++i)
        write_block(index + i, data + i * block_size(), block_size(), 0, allow_cache);
    return true;
}

bool FileBackedFS::read_block(unsigned index, u8* buffer, size_t count, size_t offset, bool allow_cache) const
{
    ASSERT(m_logical_block_size);
    ASSERT(offset + count <= block_size());
#ifdef FBFS_DEBUG
    klog() << "FileBackedFileSystem::read_block " << index;
#endif

    if (!allow_cache) {
        const_cast<FileBackedFS*>(this)->flush_specific_block_if_needed(index);
        u32 base_offset = static_cast<u32>(index) * static_cast<u32>(block_size()) + static_cast<u32>(offset);
        m_file_description->seek(base_offset, SEEK_SET);
        auto nread = m_file_description->read(buffer, count);
        if (nread < 0)
            return false;
        ASSERT(static_cast<size_t>(nread) == count);
        return true;
    }

    auto& entry = cache().get(index);
    if (!entry.has_data) {
        u32 base_offset = static_cast<u32>(index) * static_cast<u32>(block_size());
        m_file_description->seek(base_offset, SEEK_SET);
        auto nread = m_file_description->read(entry.data, block_size());
        if (nread < 0)
            return false;
        ASSERT(static_cast<size_t>(nread) == block_size());
        entry.has_data = true;
    }
    if (buffer)
        memcpy(buffer, entry.data + offset, count);
    return true;
}

bool FileBackedFS::read_blocks(unsigned index, unsigned count, u8* buffer, bool allow_cache) const
{
    ASSERT(m_logical_block_size);
    if (!count)
        return false;
    if (count == 1)
        return read_block(index, buffer, block_size(), 0, allow_cache);
    u8* out = buffer;

    for (unsigned i = 0; i < count; ++i) {
        if (!read_block(index + i, out, block_size(), 0, allow_cache))
            return false;
        out += block_size();
    }

    return true;
}

void FileBackedFS::flush_specific_block_if_needed(unsigned index)
{
    LOCKER(m_lock);
    if (!cache().is_dirty())
        return;
    cache().for_each_entry([&](CacheEntry& entry) {
        if (entry.is_dirty && entry.block_index == index) {
            u32 base_offset = static_cast<u32>(entry.block_index) * static_cast<u32>(block_size());
            m_file_description->seek(base_offset, SEEK_SET);
            m_file_description->write(entry.data, block_size());
            entry.is_dirty = false;
        }
    });
}

void FileBackedFS::flush_writes_impl()
{
    LOCKER(m_lock);
    if (!cache().is_dirty())
        return;
    u32 count = 0;
    cache().for_each_entry([&](CacheEntry& entry) {
        if (!entry.is_dirty)
            return;
        u32 base_offset = static_cast<u32>(entry.block_index) * static_cast<u32>(block_size());
        m_file_description->seek(base_offset, SEEK_SET);
        m_file_description->write(entry.data, block_size());
        ++count;
        entry.is_dirty = false;
    });
    cache().set_dirty(false);
    dbg() << class_name() << ": Flushed " << count << " blocks to disk";
}

void FileBackedFS::flush_writes()
{
    flush_writes_impl();
}

DiskCache& FileBackedFS::cache() const
{
    if (!m_cache)
        m_cache = make<DiskCache>(const_cast<FileBackedFS&>(*this));
    return *m_cache;
}

}
