#include "CharacterBitmap.h"

CharacterBitmap::CharacterBitmap(const char* ascii_data, unsigned width, unsigned height)
    : m_bits(ascii_data)
    , m_size(width, height)
{
}

CharacterBitmap::~CharacterBitmap()
{
}

NonnullRefPtr<CharacterBitmap> CharacterBitmap::create_from_ascii(const char* asciiData, unsigned width, unsigned height)
{
    return adopt(*new CharacterBitmap(asciiData, width, height));
}
