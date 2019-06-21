#pragma once

#include <AK/AKString.h>
#include <SharedBuffer.h>

class WSClipboard {
public:
    static WSClipboard& the();
    ~WSClipboard();

    bool has_data() const
    {
        return m_shared_buffer;
    }

    const byte* data() const;
    int size() const;

    void clear();
    void set_data(NonnullRefPtr<SharedBuffer>&&, int contents_size);

private:
    WSClipboard();

    RefPtr<SharedBuffer> m_shared_buffer;
    int m_contents_size { 0 };
};
