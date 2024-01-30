/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexDocument.h"
#include <AK/TypeCasts.h>
#include <LibCore/File.h>

HexDocument::HexDocument()
    : m_annotations(make_ref_counted<AnnotationsModel>())
{
}

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

ErrorOr<void> HexDocumentMemory::write_to_file(Core::File& file)
{
    TRY(file.seek(0, SeekMode::SetPosition));
    TRY(file.write_until_depleted(m_buffer));
    for (auto& change : m_changes) {
        TRY(file.seek(change.key, SeekMode::SetPosition));
        TRY(file.write_until_depleted({ &change.value, 1 }));
    }
    return {};
}

ErrorOr<NonnullOwnPtr<HexDocumentFile>> HexDocumentFile::create(NonnullOwnPtr<Core::File> file)
{
    auto document = TRY(adopt_nonnull_own_or_enomem(new HexDocumentFile(move(file))));
    TRY(document->initialize_internal_state());

    return document;
}

HexDocumentFile::HexDocumentFile(NonnullOwnPtr<Core::File> file)
    : m_file(move(file))
{
}

ErrorOr<void> HexDocumentFile::write_to_file()
{
    for (auto& change : m_changes) {
        TRY(m_file->seek(change.key, SeekMode::SetPosition));
        TRY(m_file->write_until_depleted({ &change.value, 1 }));
    }
    clear_changes();
    // make sure the next get operation triggers a read
    m_buffer_file_pos = m_file_size + 1;
    return {};
}

ErrorOr<void> HexDocumentFile::write_to_file(Core::File& file)
{
    TRY(file.truncate(size()));

    TRY(file.seek(0, SeekMode::SetPosition));
    TRY(m_file->seek(0, SeekMode::SetPosition));

    while (true) {
        Array<u8, 64 * KiB> buffer;
        auto copy_buffer = TRY(m_file->read_some(buffer));
        if (copy_buffer.size() == 0)
            break;
        TRY(file.write_until_depleted(copy_buffer));
    }

    for (auto& change : m_changes) {
        TRY(file.seek(change.key, SeekMode::SetPosition));
        TRY(file.write_until_depleted({ &change.value, 1 }));
    }

    return {};
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

ErrorOr<void> HexDocumentFile::set_file(NonnullOwnPtr<Core::File> file)
{
    m_file = move(file);
    TRY(initialize_internal_state());
    return {};
}

ErrorOr<void> HexDocumentFile::initialize_internal_state()
{
    if (auto result = m_file->seek(0, SeekMode::FromEndPosition); result.is_error())
        m_file_size = 0;
    else
        m_file_size = result.value();

    TRY(m_file->seek(0, SeekMode::SetPosition));

    clear_changes();
    // make sure the next get operation triggers a read
    m_buffer_file_pos = m_file_size + 1;
    return {};
}

NonnullOwnPtr<Core::File> const& HexDocumentFile::file() const
{
    return m_file;
}

void HexDocumentFile::ensure_position_in_buffer(size_t position)
{
    if (position < m_buffer_file_pos || position >= m_buffer_file_pos + m_buffer.size()) {
        m_file->seek(position, SeekMode::SetPosition).release_value_but_fixme_should_propagate_errors();
        // FIXME: This seems wrong. We don't track how much of the buffer is actually filled.
        m_file->read_some(m_buffer).release_value_but_fixme_should_propagate_errors();
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

    m_timestamp = MonotonicTime::now();
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
