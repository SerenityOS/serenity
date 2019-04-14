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
    void set_data(Retained<SharedBuffer>&&, int contents_size);

private:
    WSClipboard();

    RetainPtr<SharedBuffer> m_shared_buffer;
    int m_contents_size { 0 };
};
