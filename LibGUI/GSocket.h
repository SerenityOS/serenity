#pragma once

#include <LibGUI/GIODevice.h>
#include <AK/AKString.h>
#include <Kernel/IPv4.h>

class GSocketAddress {
public:
    enum class Type { Invalid, IPv4, Local };

    GSocketAddress() { }
    GSocketAddress(const IPv4Address& address)
        : m_type(Type::IPv4)
        , m_ipv4_address(address)
    {
    }

    Type type() const { return m_type; }
    bool is_valid() const { return m_type != Type::Invalid; }
    IPv4Address ipv4_address() const { return m_ipv4_address; }

    String to_string() const
    {
        switch (m_type) {
        case Type::IPv4: return m_ipv4_address.to_string();
        default: return "[GSocketAddress]";
        }
    }

private:
    Type m_type { Type::Invalid };
    IPv4Address m_ipv4_address;
};

class GSocket : public GIODevice {
public:
    enum class Type { Invalid, TCP, UDP };
    virtual ~GSocket() override;

    bool connect(const GSocketAddress&, int port);

    ByteBuffer receive(int max_size);
    bool send(const ByteBuffer&);

    bool is_connected() const { return m_connected; }

    GSocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    GSocketAddress destination_address() const { return m_source_address; }
    int destination_port() const { return m_destination_port; }

    virtual const char* class_name() const override { return "GSocket"; }

protected:
    GSocket(Type, GObject* parent);

    GSocketAddress m_source_address;
    GSocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

private:
    virtual bool open(GIODevice::OpenMode) override { ASSERT_NOT_REACHED(); }
    Type m_type { Type::Invalid };

};
