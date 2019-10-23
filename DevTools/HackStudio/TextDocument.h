#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>

class TextDocument : public RefCounted<TextDocument> {
public:
    static NonnullRefPtr<TextDocument> construct_with_name(const String& name)
    {
        return adopt(*new TextDocument(name));
    }

    const String& name() const { return m_name; }

    const ByteBuffer& contents() const;

    Vector<int> find(const StringView&) const;

private:
    explicit TextDocument(const String& name)
        : m_name(name)
    {
    }

    String m_name;
    mutable ByteBuffer m_contents;
};
