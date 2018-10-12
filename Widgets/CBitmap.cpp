#include "CBitmap.h"

CBitmap::CBitmap(const char* asciiData, unsigned width, unsigned height)
    : m_bits(asciiData)
    , m_size(width, height)
{
}

CBitmap::~CBitmap()
{
}

RetainPtr<CBitmap> CBitmap::createFromASCII(const char* asciiData, unsigned width, unsigned height)
{
    return adopt(*new CBitmap(asciiData, width, height));
}

