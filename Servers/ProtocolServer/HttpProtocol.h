#pragma once

#include <ProtocolServer/Protocol.h>

class HttpProtocol final : public Protocol {
public:
    HttpProtocol();
    virtual ~HttpProtocol() override;

    virtual RefPtr<Download> start_download(PSClientConnection&, const URL&) override;
};
