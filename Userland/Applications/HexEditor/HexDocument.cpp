/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexDocument.h"

void HexDocument::set(size_t position, u8 value)
{
    m_changes.set(position, value);
}

bool HexDocument::is_dirty() const
{
    return m_changes.size() > 0;
}

HexDocumentMemory::HexDocumentMemory(ByteBuffer&& buffer)
    : m_buffer(move(buffer))
{
}

HexDocument::Cell HexDocumentMemory::get(size_t position)
{
    auto tracked_change = m_changes.get(position);
    if (tracked_change.has_value()) {
        return Cell { tracked_change.value(), true };
    } else {
        return Cell { m_buffer[position], false };
    }
}

size_t HexDocumentMemory::size() const
{
    return m_buffer.size();
}

HexDocument::Type HexDocumentMemory::type() const
{
    return Type::Memory;
}

void HexDocumentMemory::clear_changes()
{
    m_changes.clear();
}

bool HexDocumentMemory::write_to_file(NonnullRefPtr<Core::File> file)
{
    if (!file->seek(0))
        return false;
    if (!file->write(m_buffer.data(), m_buffer.size()))
        return false;
    for (auto& change : m_changes) {
        file->seek(change.key, Core::SeekMode::SetPosition);
        file->write(&change.value, 1);
    }
    return true;
}

HexDocumentFile::HexDocumentFile(NonnullRefPtr<Core::File> file)
    : m_file(file)
{
    set_file(file);
}

void HexDocumentFile::write_to_file()
{
    for (auto& change : m_changes) {
        m_file->seek(change.key, Core::SeekMode::SetPosition);
        m_file->write(&change.value, 1);
    }
    clear_changes();
    // make sure the next get operation triggers a read
    m_buffer_file_pos = m_file_size + 1;
}

bool HexDocumentFile::write_to_file(NonnullRefPtr<Core::File> file)
{
    if (!file->truncate(size())) {
        return false;
    }

    if (!file->seek(0) || !m_file->seek(0)) {
        return false;
    }

    while (true) {
        auto copy_buffer = m_file->read(64 * KiB);
        if (copy_buffer.size() == 0)
            break;
        file->write(copy_buffer.data(), copy_buffer.size());
    }

    for (auto& change : m_changes) {
        file->seek(change.key, Core::SeekMode::SetPosition);
        file->write(&change.value, 1);
    }

    return true;
}

HexDocument::Cell HexDocumentFile::get(size_t position)
{
    auto tracked_change = m_changes.get(position);
    if (tracked_change.has_value()) {
        return Cell { tracked_change.value(), true };
    }

    if (position < m_buffer_file_pos || position >= m_buffer_file_pos + m_buffer.size()) {
        m_file->seek(position, Core::SeekMode::SetPosition);
        m_file->read(m_buffer.data(), m_buffer.size());
        m_buffer_file_pos = position;
    }
    return { m_buffer[position - m_buffer_file_pos], false };
}

size_t HexDocumentFile::size() const
{
    return m_file_size;
}

HexDocument::Type HexDocumentFile::type() const
{
    return Type::File;
}

void HexDocumentFile::clear_changes()
{
    m_changes.clear();
}

void HexDocumentFile::set_file(NonnullRefPtr<Core::File> file)
{
    m_file = file;

    off_t size = 0;
    if (!file->seek(0, Core::SeekMode::FromEndPosition, &size)) {
        m_file_size = 0;
    } else {
        m_file_size = size;
    }
    file->seek(0, Core::SeekMode::SetPosition);

    clear_changes();
    // make sure the next get operation triggers a read
    m_buffer_file_pos = m_file_size + 1;
}

NonnullRefPtr<Core::File> HexDocumentFile::file() const
{
    return m_file;
}
