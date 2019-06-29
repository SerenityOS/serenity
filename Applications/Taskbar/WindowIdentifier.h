#pragma once

#include <AK/Traits.h>
#include <AK/kstdio.h>

class WindowIdentifier {
public:
    WindowIdentifier(int client_id, int window_id)
        : m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

    bool operator==(const WindowIdentifier& other) const
    {
        return m_client_id == other.m_client_id && m_window_id == other.m_window_id;
    }

private:
    int m_client_id { -1 };
    int m_window_id { -1 };
};

namespace AK {
template<>
struct Traits<WindowIdentifier> : public GenericTraits<WindowIdentifier> {
    static unsigned hash(const WindowIdentifier& w) { return pair_int_hash(w.client_id(), w.window_id()); }
    static void dump(const WindowIdentifier& w) { kprintf("WindowIdentifier(%d, %d)", w.client_id(), w.window_id()); }
};
}
