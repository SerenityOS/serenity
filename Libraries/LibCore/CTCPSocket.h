#include <LibCore/CSocket.h>

class CTCPSocket final : public CSocket {
    C_OBJECT(CTCPSocket)
public:
    explicit CTCPSocket(CObject* parent = nullptr);
    virtual ~CTCPSocket() override;
};
