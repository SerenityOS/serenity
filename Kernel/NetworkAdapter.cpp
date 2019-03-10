#include <Kernel/NetworkAdapter.h>
#include <Kernel/StdLib.h>

NetworkAdapter::NetworkAdapter()
{
    memset(&m_mac_address, 0, sizeof(m_mac_address));
}

NetworkAdapter::~NetworkAdapter()
{
}

void NetworkAdapter::set_mac_address(const byte* mac_address)
{
    memcpy(m_mac_address, mac_address, 6);
}
