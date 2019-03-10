#pragma once

#include <AK/Types.h>
#include <Kernel/MACAddress.h>

class NetworkAdapter {
public:
    virtual ~NetworkAdapter();

    virtual const char* class_name() const = 0;
    MACAddress mac_address() { return m_mac_address; }

    virtual void send(const byte*, int) = 0;

protected:
    NetworkAdapter();
    void set_mac_address(const MACAddress& mac_address) { m_mac_address = mac_address; }

private:
    MACAddress m_mac_address;
};
