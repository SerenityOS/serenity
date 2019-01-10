#include "CharacterBitmap.h"

CharacterBitmap::CharacterBitmap(const char* asciiData, unsigned width, unsigned height)
    : m_bits(asciiData)
    , m_size(width, height)
{
}

CharacterBitmap::~CharacterBitmap()
{
}

RetainPtr<CharacterBitmap> CharacterBitmap::createFromASCII(const char* asciiData, unsigned width, unsigned height)
{
    return adopt(*new CharacterBitmap(asciiData, width, height));
}

