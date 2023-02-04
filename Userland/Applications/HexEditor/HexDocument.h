/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <LibCore/File.h>
#include <LibGUI/Command.h>

constexpr Time COMMAND_COMMIT_TIME = Time::from_milliseconds(400);

class HexDocument : public Weakable<HexDocument> {
public:
    enum class Type {
        Memory,
        File
    };

    struct Cell {
        u8 value;
        bool modified;
    };

    virtual ~HexDocument() = default;
    virtual Cell get(size_t position) = 0;
    virtual u8 get_unchanged(size_t position) = 0;
    virtual void set(size_t position, u8 value);
    virtual size_t size() const = 0;
    virtual Type type() const = 0;
    virtual bool is_dirty() const;
    virtual void clear_changes() = 0;

protected:
    HashMap<size_t, u8> m_changes;
};

class HexDocumentMemory final : public HexDocument {
public:
    explicit HexDocumentMemory(ByteBuffer&& buffer);
    virtual ~HexDocumentMemory() = default;

    Cell get(size_t position) override;
    u8 get_unchanged(size_t position) override;
    size_t size() const override;
    Type type() const override;
    void clear_changes() override;
    ErrorOr<void> write_to_file(Core::Stream::File& file);

private:
    ByteBuffer m_buffer;
};

class HexDocumentFile final : public HexDocument {
public:
    static ErrorOr<NonnullOwnPtr<HexDocumentFile>> create(NonnullOwnPtr<Core::Stream::File> file);
    virtual ~HexDocumentFile() = default;

    HexDocumentFile(HexDocumentFile&&) = default;
    HexDocumentFile(HexDocumentFile const&) = delete;

    ErrorOr<void> set_file(NonnullOwnPtr<Core::Stream::File> file);
    NonnullOwnPtr<Core::Stream::File> const& file() const;
    ErrorOr<void> write_to_file();
    ErrorOr<void> write_to_file(Core::Stream::File& file);
    Cell get(size_t position) override;
    u8 get_unchanged(size_t position) override;
    size_t size() const override;
    Type type() const override;
    void clear_changes() override;

private:
    explicit HexDocumentFile(NonnullOwnPtr<Core::Stream::File> file);
    void ensure_position_in_buffer(size_t position);

    NonnullOwnPtr<Core::Stream::File> m_file;
    size_t m_file_size;

    Array<u8, 2048> m_buffer;
    size_t m_buffer_file_pos;
};

class HexDocumentUndoCommand : public GUI::Command {
public:
    HexDocumentUndoCommand(WeakPtr<HexDocument> document, size_t position);

    virtual void undo() override;
    virtual void redo() override;
    virtual DeprecatedString action_text() const override { return "Update cell"; }

    virtual bool merge_with(GUI::Command const& other) override;

    ErrorOr<void> try_add_changed_byte(u8 old_value, u8 new_value);
    ErrorOr<void> try_add_changed_bytes(ByteBuffer old_values, ByteBuffer new_values);

private:
    bool commit_time_expired() const { return Time::now_monotonic() - m_timestamp >= COMMAND_COMMIT_TIME; }

    Time m_timestamp = Time::now_monotonic();
    WeakPtr<HexDocument> m_document;
    size_t m_position;
    ByteBuffer m_old;
    ByteBuffer m_new;
};
