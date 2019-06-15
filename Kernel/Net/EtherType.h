#pragma once

#include <AK/Types.h>

struct EtherType {
    enum : word {
        ARP = 0x0806,
        IPv4 = 0x0800,
    };
};
