#include <LibCore/CSocket.h>

class CTCPSocket final : public CSocket {
public:
    explicit CTCPSocket(CObject* parent = nullptr);
    virtual ~CTCPSocket() override;

private:
};
