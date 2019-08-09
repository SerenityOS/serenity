#pragma once

#include <Kernel/Net/NetworkAdapter.h>

WeakPtr<NetworkAdapter> adapter_for_route_to(const IPv4Address&);
