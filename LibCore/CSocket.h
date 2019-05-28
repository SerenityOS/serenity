#pragma once

#include <LibCore/CIODevice.h>
#include <LibCore/CSocketAddress.h>

class CNotifier;

class CSocket : public CIODevice {
public:
    enum class Type
    {
        Invalid,
        TCP,
        UDP
    };
    virtual ~CSocket() override;

    bool connect(const String& hostname, int port);
    bool connect(const CSocketAddress&, int port);

    ByteBuffer receive(int max_size);
    bool send(const ByteBuffer&);

    bool is_connected() const { return m_connected; }

    CSocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    CSocketAddress destination_address() const { return m_source_address; }
    int destination_port() const { return m_destination_port; }

    Function<void()> on_connected;

    virtual const char* class_name() const override { return "CSocket"; }

protected:
    CSocket(Type, CObject* parent);

    CSocketAddress m_source_address;
    CSocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

private:
    virtual bool open(CIODevice::OpenMode) override { ASSERT_NOT_REACHED(); }
    Type m_type { Type::Invalid };
    OwnPtr<CNotifier> m_notifier;
};
