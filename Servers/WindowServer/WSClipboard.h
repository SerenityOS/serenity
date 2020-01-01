#pragma once

#include <AK/Function.h>
#include <AK/SharedBuffer.h>
#include <AK/String.h>

class WSClipboard {
public:
    static WSClipboard& the();
    ~WSClipboard();

    bool has_data() const
    {
        return m_shared_buffer;
    }

    const String& data_type() const { return m_data_type; }
    const u8* data() const;
    int size() const;

    void clear();
    void set_data(NonnullRefPtr<SharedBuffer>&&, int contents_size, const String& data_type);

    Function<void()> on_content_change;

private:
    WSClipboard();

    String m_data_type;
    RefPtr<SharedBuffer> m_shared_buffer;
    int m_contents_size { 0 };
};
