#include <Kernel/NetworkAdapter.h>
#include <Kernel/StdLib.h>

NetworkAdapter::NetworkAdapter()
{
    memset(&m_mac_address, 0, sizeof(m_mac_address));
}

NetworkAdapter::~NetworkAdapter()
{
}

