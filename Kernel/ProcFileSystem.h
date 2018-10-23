#pragma once

#include <AK/Types.h>
#include <VirtualFileSystem/SyntheticFileSystem.h>

class ProcFileSystem final : public SyntheticFileSystem {
public:
    virtual ~ProcFileSystem() override;
    static RetainPtr<ProcFileSystem> create();

    virtual bool initialize() override;
    virtual const char* className() const override;

private:
    ProcFileSystem();
};

