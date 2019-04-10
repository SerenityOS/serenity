#include <LibGUI/GSocket.h>

class GTCPSocket final : public GSocket {
public:
    explicit GTCPSocket(CObject* parent);
    virtual ~GTCPSocket() override;

private:
};

