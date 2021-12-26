#include <Kernel/Net/Routing.h>
#include <Kernel/Net/LoopbackAdapter.h>

NetworkAdapter* adapter_for_route_to(const IPv4Address& ipv4_address)
{
    // FIXME: Have an actual routing table.
    if (ipv4_address == IPv4Address(127, 0, 0, 1))
        return &LoopbackAdapter::the();
    return NetworkAdapter::from_ipv4_address(IPv4Address(192, 168, 5, 2));
}
