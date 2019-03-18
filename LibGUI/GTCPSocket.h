#include <LibGUI/GSocket.h>

class GTCPSocket final : public GSocket {
public:
    explicit GTCPSocket(GObject* parent);
    virtual ~GTCPSocket() override;

private:
};

