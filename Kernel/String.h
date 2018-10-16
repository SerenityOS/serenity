#pragma once

#include "DataBuffer.h"
#include "Vector.h"

class String {
public:
    String();
    String(const char* characters);
    String(const char* characters, size_t length);
    String(String&&);
    String(const String&);
    String& operator=(const String&);
    String& operator=(const String&&);
    ~String();

    bool isEmpty() const { return m_data ? m_data->isEmpty() : true; }
    size_t length() const { return m_data ? m_data->length() : 0; }

    char operator[](size_t i) const { return (char)m_data->data()[i]; }

    const char* characters() const { return m_data ? (const char*)m_data->data() : nullptr; }

    bool operator==(const String&) const;

    Vector<String> split(char separator) const;
    String substring(size_t start, size_t length) const;

private:
    RefPtr<DataBuffer> m_data;
};
