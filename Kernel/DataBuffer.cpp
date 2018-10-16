#include "DataBuffer.h"
#include "StdLib.h"

#define SANITIZE_DATABUFFER

DataBuffer::DataBuffer(size_t length)
    : m_length(length)
    , m_owned(true)
{
    m_data = new BYTE[m_length];
#ifdef SANITIZE_DATABUFFER
    memset(m_data, 0x1a, length);
#endif
}

DataBuffer::DataBuffer(BYTE* data, size_t length, ConstructionMode mode)
    : m_length(length)
{
    switch (mode) {
    case Copy:
        m_data = new BYTE[m_length];
        memcpy(m_data, data, m_length);
        m_owned = true;
        break;
    case Adopt:
        m_data = data;
        m_owned = true;
        break;
    case Wrap:
        m_data = data;
        m_owned = false;
        break;
    }
}

DataBuffer::~DataBuffer()
{
    clear();
#ifdef SANITIZE_DATABUFFER
    m_data = (BYTE*)0x88888888;
#endif
}

void DataBuffer::clear()
{
    if (m_owned) {
#ifdef SANITIZE_DATABUFFER
        memset(m_data, 0x99, m_length);
#endif
        delete [] m_data;
    }
    m_owned = false;
    m_data = nullptr;
    m_length = 0;
}

RefPtr<DataBuffer> DataBuffer::createUninitialized(size_t length)
{
    return adoptRef(new DataBuffer(length));
}

RefPtr<DataBuffer> DataBuffer::copy(const BYTE* data, size_t length)
{
    return adoptRef(new DataBuffer(const_cast<BYTE*>(data), length, Copy));
}

RefPtr<DataBuffer> DataBuffer::wrap(BYTE* data, size_t length)
{
    return adoptRef(new DataBuffer(data, length, Wrap));
}

RefPtr<DataBuffer> DataBuffer::adopt(BYTE* data, size_t length)
{
    return adoptRef(new DataBuffer(data, length, Adopt));
}
