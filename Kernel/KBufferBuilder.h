#pragma once

#include <AK/AKString.h>
#include <Kernel/KBuffer.h>
#include <stdarg.h>

class KBufferBuilder {
public:
    using OutputType = KBuffer;

    explicit KBufferBuilder();
    ~KBufferBuilder() {}

    void append(const StringView&);
    void append(char);
    void append(const char*, int);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    KBuffer build();

private:
    bool can_append(size_t) const;
    u8* insertion_ptr() { return m_buffer.data() + m_size; }

    KBuffer m_buffer;
    size_t m_size { 0 };
};
