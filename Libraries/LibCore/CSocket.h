#pragma once

#include <LibCore/CIODevice.h>
#include <LibCore/CSocketAddress.h>

class CNotifier;

class CSocket : public CIODevice {
    C_OBJECT(CSocket)
public:
    enum class Type {
        Invalid,
        TCP,
        UDP,
        Local,
    };
    virtual ~CSocket() override;

    Type type() const { return m_type; }

    bool connect(const String& hostname, int port);
    bool connect(const CSocketAddress&, int port);
    bool connect(const CSocketAddress&);

    ByteBuffer receive(int max_size);
    bool send(const ByteBuffer&);

    bool is_connected() const { return m_connected; }
    void set_blocking(bool blocking);

    CSocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    CSocketAddress destination_address() const { return m_source_address; }
    int destination_port() const { return m_destination_port; }

    Function<void()> on_connected;
    Function<void()> on_ready_to_read;

protected:
    CSocket(Type, CObject* parent);

    CSocketAddress m_source_address;
    CSocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

    virtual void did_update_fd(int) override;

private:
    virtual bool open(CIODevice::OpenMode) override { ASSERT_NOT_REACHED(); }
    Type m_type { Type::Invalid };
    OwnPtr<CNotifier> m_notifier;
    OwnPtr<CNotifier> m_read_notifier;
};
