#pragma once
#include <LibCore/CSocket.h>

class CLocalSocket final : public CSocket {
public:
    explicit CLocalSocket(CObject* parent = nullptr);
    virtual ~CLocalSocket() override;
};
