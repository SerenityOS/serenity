#pragma once

#include <LibGUI/GIODevice.h>
#include <LibGUI/GSocketAddress.h>

class GNotifier;

class GSocket : public GIODevice {
public:
    enum class Type { Invalid, TCP, UDP };
    virtual ~GSocket() override;

    bool connect(const String& hostname, int port);
    bool connect(const GSocketAddress&, int port);

    ByteBuffer receive(int max_size);
    bool send(const ByteBuffer&);

    bool is_connected() const { return m_connected; }

    GSocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    GSocketAddress destination_address() const { return m_source_address; }
    int destination_port() const { return m_destination_port; }

    Function<void()> on_connected;

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
    OwnPtr<GNotifier> m_notifier;
};
