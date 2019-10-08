#pragma once

#include <AK/Function.h>
#include <AK/URL.h>

class ResourceLoader {
public:
    static ResourceLoader& the();

    void load(const URL&, Function<void(const ByteBuffer&)>);

private:
    ResourceLoader() {}
};
