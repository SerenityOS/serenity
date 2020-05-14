#pragma once

#include <AK/Function.h>
#include <AK/SharedBuffer.h>
#include <AK/String.h>

namespace Clipboard {

class Storage {
public:
    static Storage& the();
    ~Storage();

    bool has_data() const { return m_shared_buffer; }

    const String& mime_type() const { return m_mime_type; }

    const u8* data() const
    {
        if (!has_data())
            return nullptr;
        return static_cast<const u8*>(m_shared_buffer->data());
    }

    size_t data_size() const
    {
        if (has_data())
            return m_data_size;
        return 0;
    }

    void set_data(NonnullRefPtr<SharedBuffer>, size_t data_size, const String& mime_type);

    Function<void()> on_content_change;

private:
    Storage();

    String m_mime_type;
    RefPtr<SharedBuffer> m_shared_buffer;
    size_t m_data_size { 0 };
};

}
