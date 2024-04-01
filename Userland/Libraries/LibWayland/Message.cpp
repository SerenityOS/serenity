#include <LibWayland/Message.h>
#include <LibWayland/Object.h>

namespace Wayland {
void MessageIncoming::submit()
{
    VERIFY(is_resolved());

    if (m_object.is_null()) {
        return;
    }

    VERIFY(m_method != nullptr);
    auto* a = m_object.ptr();
    m_method->handler(*a, m_resolved);
}

size_t MessageIncoming::amount_unresolved_fds()
{
    size_t amount = 0;
    
    for (auto& r : m_resolved) {
        auto *a = r.ptr();
        VERIFY(a != nullptr);
        if (a->is_fd() && !a->is_fd_resolved()) {
            ++amount;
        }
    }

    return amount;
}

}
