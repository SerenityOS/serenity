#include <AK/StringUtils.h>
#include <AK/TestSuite.h>

TEST_CASE(matches_null)
{
    EXPECT(AK::StringUtils::matches(StringView(), StringView()));

    EXPECT(!AK::StringUtils::matches(StringView(), ""));
    EXPECT(!AK::StringUtils::matches(StringView(), "*"));
    EXPECT(!AK::StringUtils::matches(StringView(), "?"));
    EXPECT(!AK::StringUtils::matches(StringView(), "a"));

    EXPECT(!AK::StringUtils::matches("", StringView()));
    EXPECT(!AK::StringUtils::matches("a", StringView()));
}

TEST_CASE(matches_empty)
{
    EXPECT(AK::StringUtils::matches("", ""));

    EXPECT(AK::StringUtils::matches("", "*"));
    EXPECT(!AK::StringUtils::matches("", "?"));
    EXPECT(!AK::StringUtils::matches("", "a"));

    EXPECT(!AK::StringUtils::matches("a", ""));
}

TEST_CASE(matches_case_sensitive)
{
    EXPECT(AK::StringUtils::matches("a", "a", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::matches("a", "A", CaseSensitivity::CaseSensitive));
    EXPECT(!AK::StringUtils::matches("A", "a", CaseSensitivity::CaseSensitive));
}

TEST_CASE(matches_case_insensitive)
{
    EXPECT(!AK::StringUtils::matches("aa", "a"));
    EXPECT(AK::StringUtils::matches("aa", "*"));
    EXPECT(!AK::StringUtils::matches("cb", "?a"));
    EXPECT(AK::StringUtils::matches("adceb", "a*b"));
    EXPECT(!AK::StringUtils::matches("acdcb", "a*c?b"));
}

TEST_MAIN(StringUtils)
