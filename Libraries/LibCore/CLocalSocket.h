#pragma once
#include <LibCore/CSocket.h>

class CLocalSocket final : public CSocket {
    C_OBJECT(CLocalSocket)
public:
    explicit CLocalSocket(CObject* parent = nullptr);
    virtual ~CLocalSocket() override;

    virtual bool bind(const CSocketAddress&) override;
};
