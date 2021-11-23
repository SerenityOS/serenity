/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/File.h>

class HexDocument {
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
    size_t size() const override;
    Type type() const override;
    void clear_changes() override;
    bool write_to_file(NonnullRefPtr<Core::File> file);

private:
    ByteBuffer m_buffer;
};

class HexDocumentFile final : public HexDocument {
public:
    explicit HexDocumentFile(NonnullRefPtr<Core::File> file);
    virtual ~HexDocumentFile() = default;

    HexDocumentFile(const HexDocumentFile&) = delete;

    void set_file(NonnullRefPtr<Core::File> file);
    NonnullRefPtr<Core::File> file() const;
    void write_to_file();
    bool write_to_file(NonnullRefPtr<Core::File> file);
    Cell get(size_t position) override;
    size_t size() const override;
    Type type() const override;
    void clear_changes() override;

private:
    NonnullRefPtr<Core::File> m_file;
    size_t m_file_size;

    Array<u8, 2048> m_buffer;
    size_t m_buffer_file_pos;
};
