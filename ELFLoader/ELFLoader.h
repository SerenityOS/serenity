#pragma once

#include <AK/HashMap.h>
#include <AK/MappedFile.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include "ExecSpace.h"
#include "ELFImage.h"

class ELFLoader {
public:
    ELFLoader(ExecSpace&, MappedFile&&);
    ~ELFLoader();

    bool load();

private:
    void layout();
    void performRelocations();
    void exportSymbols();
    void* lookup(const ELFImage::Symbol&);
    char* areaForSection(const ELFImage::Section&);
    char* areaForSectionName(const char*);

    ExecSpace& m_execSpace;
    HashMap<String, char*> m_sections;
    OwnPtr<ELFImage> m_image;
};

