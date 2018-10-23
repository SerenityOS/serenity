#include "ProcFileSystem.h"
#include <AK/StdLib.h>

RetainPtr<ProcFileSystem> ProcFileSystem::create()
{
    return adopt(*new ProcFileSystem);
}

ProcFileSystem::ProcFileSystem()
{
}

ProcFileSystem::~ProcFileSystem()
{
}

bool ProcFileSystem::initialize()
{
    SyntheticFileSystem::initialize();
    addFile(createGeneratedFile("summary", [] {
        return String("Process summary!").toByteBuffer();
    }));
    return true;
}

const char* ProcFileSystem::className() const
{
    return "procfs";
}
