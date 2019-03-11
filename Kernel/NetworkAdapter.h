#pragma once

#include <AK/ByteBuffer.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <Kernel/MACAddress.h>
#include <Kernel/ARPPacket.h>

class NetworkAdapter {
public:
    virtual ~NetworkAdapter();

    virtual const char* class_name() const = 0;
    MACAddress mac_address() { return m_mac_address; }

    void send(const MACAddress&, const ARPPacket&);

    ByteBuffer dequeue_packet();

protected:
    NetworkAdapter();
    void set_mac_address(const MACAddress& mac_address) { m_mac_address = mac_address; }
    virtual void send_raw(const byte*, int) = 0;
    void did_receive(const byte*, int);

private:
    MACAddress m_mac_address;
    SinglyLinkedList<ByteBuffer> m_packet_queue;
};
