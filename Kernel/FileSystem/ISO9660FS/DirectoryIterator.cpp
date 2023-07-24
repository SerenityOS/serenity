/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/FileSystem/ISO9660FS/Definitions.h>
#include <Kernel/FileSystem/ISO9660FS/DirectoryIterator.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

ISO9660DirectoryIterator::ISO9660DirectoryIterator(ISO9660FS& fs, ISO::DirectoryRecordHeader const& header)
    : m_fs(fs)
    , m_current_header(&header)
{
    // FIXME: Panic or alternative method?
    (void)read_directory_contents();
    get_header();
}

ErrorOr<bool> ISO9660DirectoryIterator::next()
{
    if (done())
        return false;
    dbgln_if(ISO9660_VERY_DEBUG, "next(): Called");

    if (has_flag(m_current_header->file_flags, ISO::FileFlags::Directory)) {
        dbgln_if(ISO9660_VERY_DEBUG, "next(): Recursing");
        {
            TRY(m_directory_stack.try_append(move(m_current_directory)));
        }

        dbgln_if(ISO9660_VERY_DEBUG, "next(): Pushed into directory stack");

        TRY(read_directory_contents());

        dbgln_if(ISO9660_VERY_DEBUG, "next(): Read directory contents");

        m_current_directory.offset = 0;
        get_header();
        if (m_current_header->length == 0) {
            // We have found an empty directory, let's continue with the
            // next one.
            if (!go_up())
                return false;
        } else {
            // We cannot skip here, as this is the first record in this
            // extent.
            return true;
        }
    }

    return skip();
}

bool ISO9660DirectoryIterator::skip()
{
    VERIFY(m_current_directory.entry);

    if (m_current_directory.offset >= m_current_directory.entry->length) {
        dbgln_if(ISO9660_VERY_DEBUG, "skip(): Was at last item already");
        return false;
    }

    m_current_directory.offset += m_current_header->length;
    get_header();
    if (m_current_header->length == 0) {
        // According to ECMA 119, if a logical block contains directory
        // records, then the leftover bytes in the logical block are
        // all zeros. So if our directory header has a length of 0,
        // we're probably looking at padding.
        //
        // Of course, this doesn't mean we're done; it only means that there
        // are no more directory entries in *this* logical block.  If we
        // have at least one more logical block of data length to go, we
        // need to snap to the next logical block, because directory records
        // cannot span multiple logical blocks.
        u32 remaining_bytes = m_current_directory.entry->length - m_current_directory.offset;
        if (remaining_bytes > m_fs.device_block_size()) {
            m_current_directory.offset += remaining_bytes % m_fs.device_block_size();
            get_header();

            dbgln_if(ISO9660_VERY_DEBUG, "skip(): Snapped to next logical block (succeeded)");
            return true;
        }

        dbgln_if(ISO9660_VERY_DEBUG, "skip(): Was at the last logical block, at padding now (offset {}, data length {})", m_current_directory.entry->length, m_current_directory.offset);
        return false;
    }

    dbgln_if(ISO9660_VERY_DEBUG, "skip(): Skipped to next item");
    return true;
}

bool ISO9660DirectoryIterator::go_up()
{
    if (m_directory_stack.is_empty()) {
        dbgln_if(ISO9660_VERY_DEBUG, "go_up(): Empty directory stack");
        return false;
    }

    m_current_directory = m_directory_stack.take_last();
    get_header();

    dbgln_if(ISO9660_VERY_DEBUG, "go_up(): Went up a directory");
    return true;
}

bool ISO9660DirectoryIterator::done() const
{
    VERIFY(m_current_directory.entry);
    auto result = m_directory_stack.is_empty() && m_current_directory.offset >= m_current_directory.entry->length;
    dbgln_if(ISO9660_VERY_DEBUG, "done(): {}", result);
    return result;
}

ErrorOr<void> ISO9660DirectoryIterator::read_directory_contents()
{
    m_current_directory.entry = TRY(m_fs.directory_entry_for_record({}, m_current_header));
    return {};
}

void ISO9660DirectoryIterator::get_header()
{
    VERIFY(m_current_directory.entry);
    if (!m_current_directory.entry->blocks)
        return;

    m_current_header = reinterpret_cast<ISO::DirectoryRecordHeader const*>(m_current_directory.entry->blocks->data() + m_current_directory.offset);
}

}
