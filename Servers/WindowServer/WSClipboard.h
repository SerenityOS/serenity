#pragma once

#include <AK/AKString.h>
#include <WindowServer/WSMessageReceiver.h>
#include <SharedBuffer.h>

class WSClipboard final : public WSMessageReceiver {
public:
    static WSClipboard& the();
    virtual ~WSClipboard() override;

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
    virtual void on_message(const WSMessage&) override;

    RetainPtr<SharedBuffer> m_shared_buffer;
    int m_contents_size { 0 };
};
