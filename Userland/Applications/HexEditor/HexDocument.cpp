/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexDocument.h"

void HexDocument::set(size_t position, u8 value)
{
    auto unchanged_value = get_unchanged(position);

    if (value == unchanged_value) {
        m_changes.remove(position);
    } else {
        m_changes.set(position, value);
    }
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

u8 HexDocumentMemory::get_unchanged(size_t position)
{
    return m_buffer[position];
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

    ensure_position_in_buffer(position);
    return { m_buffer[position - m_buffer_file_pos], false };
}

u8 HexDocumentFile::get_unchanged(size_t position)
{
    ensure_position_in_buffer(position);
    return m_buffer[position - m_buffer_file_pos];
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

void HexDocumentFile::ensure_position_in_buffer(size_t position)
{
    if (position < m_buffer_file_pos || position >= m_buffer_file_pos + m_buffer.size()) {
        m_file->seek(position, Core::SeekMode::SetPosition);
        m_file->read(m_buffer.data(), m_buffer.size());
        m_buffer_file_pos = position;
    }
}

HexDocumentUndoCommand::HexDocumentUndoCommand(WeakPtr<HexDocument> document, size_t position)
    : m_document(move(document))
    , m_position(position)
{
}

void HexDocumentUndoCommand::undo()
{
    for (size_t i = 0; i < m_old.size(); i++)
        m_document->set(m_position + i, m_old[i]);
}

void HexDocumentUndoCommand::redo()
{
    for (size_t i = 0; i < m_new.size(); i++)
        m_document->set(m_position + i, m_new[i]);
}

bool HexDocumentUndoCommand::merge_with(GUI::Command const& other)
{
    if (!is<HexDocumentUndoCommand>(other) || commit_time_expired())
        return false;

    auto const& typed_other = static_cast<HexDocumentUndoCommand const&>(other);

    size_t relative_start = typed_other.m_position - m_position;
    size_t other_length = typed_other.m_old.size();
    size_t length = m_old.size();

    if (typed_other.m_position < m_position || m_position + length < typed_other.m_position)
        return false;

    m_old.resize(relative_start + other_length);
    m_new.resize(relative_start + other_length);

    for (size_t i = 0; i < other_length; i++) {
        m_new[relative_start + i] = typed_other.m_new[i];

        if (relative_start + i >= length)
            m_old[relative_start + i] = typed_other.m_old[i];
    }

    m_timestamp = Time::now_monotonic();
    return true;
}

ErrorOr<void> HexDocumentUndoCommand::try_add_changed_byte(u8 old_value, u8 new_value)
{
    TRY(m_old.try_append(old_value));
    TRY(m_new.try_append(new_value));
    return {};
}

ErrorOr<void> HexDocumentUndoCommand::try_add_changed_bytes(ByteBuffer old_values, ByteBuffer new_values)
{
    TRY(m_old.try_append(move(old_values)));
    TRY(m_new.try_append(move(new_values)));
    return {};
}
