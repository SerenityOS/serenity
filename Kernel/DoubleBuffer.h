#pragma once

#include <AK/Types.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>

class DoubleBuffer {
public:
    explicit DoubleBuffer(size_t capacity = 65536);

    ssize_t write(const u8*, ssize_t);
    ssize_t read(u8*, ssize_t);

    bool is_empty() const { return m_empty; }

    size_t space_for_writing() const { return m_space_for_writing; }

private:
    void flip();
    void compute_lockfree_metadata();

    struct InnerBuffer {
        u8* data { nullptr };
        size_t size;
    };

    InnerBuffer* m_write_buffer { nullptr };
    InnerBuffer* m_read_buffer { nullptr };
    InnerBuffer m_buffer1;
    InnerBuffer m_buffer2;

    KBuffer m_storage;
    size_t m_capacity { 0 };
    size_t m_read_buffer_index { 0 };
    size_t m_space_for_writing { 0 };
    bool m_empty { true };
    mutable Lock m_lock { "DoubleBuffer" };
};
