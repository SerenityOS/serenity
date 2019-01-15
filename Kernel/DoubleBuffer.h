#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/Lock.h>

class DoubleBuffer {
public:
    DoubleBuffer()
        : m_write_buffer(&m_buffer1)
        , m_read_buffer(&m_buffer2)
    {
    }

    ssize_t write(const byte*, size_t);
    ssize_t read(byte*, size_t);

    bool is_empty() const { return m_empty; }

    // FIXME: Isn't this racy? What if we get interrupted between getting the buffer pointer and dereferencing it?
    size_t bytes_in_write_buffer() const { return m_write_buffer->size(); }

private:
    void flip();
    void compute_emptiness();

    Vector<byte>* m_write_buffer { nullptr };
    Vector<byte>* m_read_buffer { nullptr };
    Vector<byte> m_buffer1;
    Vector<byte> m_buffer2;
    size_t m_read_buffer_index { 0 };
    bool m_empty { true };
    SpinLock m_lock;
};
