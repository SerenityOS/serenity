#pragma once

#include "types.h"
#include "RefCounted.h"
#include "RefPtr.h"

class DataBuffer : public RefCounted<DataBuffer> {
public:
    ~DataBuffer();

    BYTE operator[](size_t i) const { return m_data[i]; }
    bool isEmpty() const { return !m_length; }
    size_t length() const { return m_length; }
    BYTE* data() { return m_data; }
    const BYTE* data() const { return m_data; }

    static RefPtr<DataBuffer> copy(const BYTE*, size_t length);
    static RefPtr<DataBuffer> wrap(BYTE*, size_t length);
    static RefPtr<DataBuffer> adopt(BYTE*, size_t length);
    static RefPtr<DataBuffer> createUninitialized(size_t length);

    void clear();
    void leak() { m_data = nullptr; m_length = 0; m_owned = false; }

private:
    DataBuffer() { }
    DataBuffer(DataBuffer&&) = delete;
    DataBuffer& operator=(DataBuffer&&) = delete;

    enum ConstructionMode { Copy, Wrap, Adopt };
    explicit DataBuffer(size_t length);
    DataBuffer(BYTE*, size_t length, ConstructionMode);
    DataBuffer(const DataBuffer&) = delete;
    DataBuffer& operator=(const DataBuffer&) = delete;

    BYTE* m_data { nullptr };
    size_t m_length { 0 };
    bool m_owned { false };
};
