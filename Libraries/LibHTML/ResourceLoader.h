#pragma once

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibCore/CObject.h>

namespace LibProtocol {
class Client;
}

class ResourceLoader : public CObject {
    C_OBJECT(ResourceLoader)
public:
    static ResourceLoader& the();

    void load(const URL&, Function<void(const ByteBuffer&)>);

    Function<void()> on_load_counter_change;

    int pending_loads() const { return m_pending_loads; }

private:
    ResourceLoader();

    int m_pending_loads { 0 };

    LibProtocol::Client& protocol_client() { return *m_protocol_client; }
    RefPtr<LibProtocol::Client> m_protocol_client;
};
