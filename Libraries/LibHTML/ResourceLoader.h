#pragma once

#include <AK/Function.h>
#include <AK/URL.h>
#include <LibCore/CObject.h>

class ResourceLoader : public CObject {
    C_OBJECT(ResourceLoader)
public:
    static ResourceLoader& the();

    void load(const URL&, Function<void(const ByteBuffer&)>);

private:
    ResourceLoader() {}
};
