/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypedTransfer.h>
#include <LibCore/FileLikeIODevice.h>

namespace Core {
// This is not necessarily a valid iterator in all contexts,
// if we had concepts, this would be InputIterator, not Copyable, Movable.
template<typename UnderlyingDeviceT>
class LineIterator {
    AK_MAKE_NONCOPYABLE(LineIterator);

public:
    explicit LineIterator(BufferingIODevice<UnderlyingDeviceT>& device, bool is_end = false)
        : m_device(device)
        , m_is_end(is_end)
    {
        ++*this;
    }

    bool operator==(const LineIterator& other) const { return &other == this || (at_end() && other.is_end()) || (other.at_end() && is_end()); }
    bool is_end() const { return m_is_end; }
    bool at_end() const;

    LineIterator& operator++();

    StringView operator*() const { return m_buffer; }

private:
    NonnullRefPtr<BufferingIODevice<UnderlyingDeviceT>> m_device;
    bool m_is_end { false };
    String m_buffer;
};

template<typename UnderlyingDeviceT>
class BufferingIODevice : public UnderlyingDeviceT {
    C_OBJECT(BufferingIODevice)
public:
    size_t read(Bytes bytes) override
    {
        if (bytes.is_empty())
            return 0;

        size_t bytes_read = 0;
        if (!m_buffered_data.is_empty()) {
            auto size = min(m_buffered_data.size(), bytes.size());
            ReadonlyBytes { m_buffered_data }.slice(0, size).copy_to(bytes.slice(0, size));
            drop_bytes_from_buffer(size);
            bytes = bytes.slice(size);
            bytes_read += size;
        }

        if (bytes.is_empty())
            return bytes_read;

        return UnderlyingDeviceT::read(bytes) + bytes_read;
    }

    bool discard_or_error(size_t count) override
    {
        if (m_buffered_data.is_empty())
            return UnderlyingDeviceT::discard_or_error(count);

        auto trim_size = min(m_buffered_data.size(), count);
        drop_bytes_from_buffer(trim_size);

        if (trim_size < count)
            return UnderlyingDeviceT::discard_or_error(count - trim_size);

        return true;
    }
    bool unreliable_eof() const override
    {
        if (!m_buffered_data.is_empty())
            return false;
        return UnderlyingDeviceT::unreliable_eof();
    }

    bool can_read_line() const
    {
        if (this->unreliable_eof() && !m_buffered_data.is_empty())
            return true;
        if (m_buffered_data.contains_slow('\n'))
            return true;

        populate_read_buffer();

        if (this->unreliable_eof() && !m_buffered_data.is_empty())
            return true;
        return m_buffered_data.contains_slow('\n');
    }

    String read_line(size_t max_size = 16384)
    {
        if (!max_size)
            return {};

        if (!can_read_line())
            return {};

        if (this->unreliable_eof()) {
            if (m_buffered_data.size() > max_size) {
                dbgln("BuferringIODevice::read_line: At EOF but there's more than max_size({}) buffered", max_size);
                return {};
            }
            auto line = String((const char*)m_buffered_data.data(), m_buffered_data.size(), Chomp);
            m_buffered_data.clear();
            return line;
        }

        auto line = ByteBuffer::create_uninitialized(max_size + 1);
        size_t line_index = 0;
        while (line_index < max_size) {
            u8 ch = m_buffered_data[line_index];
            line[line_index++] = ch;
            if (ch == '\n') {
                Vector<u8> new_buffered_data;
                new_buffered_data.append(m_buffered_data.data() + line_index, m_buffered_data.size() - line_index);
                m_buffered_data = move(new_buffered_data);
                line.trim(line_index);
                return String::copy(line, Chomp);
            }
        }
        return {};
    }

    auto line_begin() { return LineIterator<UnderlyingDeviceT>(*this); }
    auto line_end() { return LineIterator<UnderlyingDeviceT>(*this, true); }

    using UnderlyingDeviceT::read;

protected:
    template<typename... Args>
    requires(IsBaseOf<Socket, UnderlyingDeviceT>) explicit BufferingIODevice(Args&&... args, Object* parent)
        : IODevice(parent)
        , SocketLikeIODevice(parent)
        , FileLikeIODevice(parent)
        , UnderlyingDeviceT(forward<Args>(args)..., parent)
    {
    }

    template<typename... Args>
    requires(!IsBaseOf<Socket, UnderlyingDeviceT>) explicit BufferingIODevice(Args&&... args, Object* parent)
        : IODevice(parent)
        , UnderlyingDeviceT(forward<Args>(args)..., parent)
    {
    }

private:
    void drop_bytes_from_buffer(size_t count)
    {
        auto new_size = m_buffered_data.size() - count;
        if (new_size == 0) {
            m_buffered_data.clear();
        } else {
            Bytes data { m_buffered_data };
            AK::TypedTransfer<u8>::move(data.data(), data.offset_pointer(count), new_size);
            m_buffered_data.resize(new_size);
        }
    }

    bool populate_read_buffer() const
    {
        u8 buffer[1024];

        auto nread = const_cast<BufferingIODevice<UnderlyingDeviceT>*>(this)->read({ buffer, 1024 });
        if (nread == 0)
            return false;

        m_buffered_data.append(buffer, nread);
        return true;
    }

    mutable Vector<u8> m_buffered_data;
};

template<typename T>
bool LineIterator<T>::at_end() const
{
    return m_device->eof();
}

template<typename T>
LineIterator<T>& LineIterator<T>::operator++()
{
    m_buffer = m_device->read_line();
    return *this;
}

}
