#pragma once

#include <AK/Types.h>

class NetworkAdapter {
public:
    virtual ~NetworkAdapter();

    virtual const char* class_name() const = 0;
    const byte* mac_address() { return m_mac_address; }

protected:
    NetworkAdapter();
    void set_mac_address(const byte*);

private:
    byte m_mac_address[6];
};
