/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibUnicode/Punycode.h>

namespace Unicode::Punycode {

#define ENUMERATE_TEST_CASES                                                                                                              \
    CASE(""sv, ""sv)                                                                                                                      \
    CASE("Well hello friends!"sv, "Well hello friends!-"sv)                                                                               \
    CASE("Well-hello-friends"sv, "Well-hello-friends-"sv)                                                                                 \
    CASE("Wгellд-бhellбвo"sv, "Well-hello-friends"sv)                                                                                     \
    CASE("Hallöchen Freunde!"sv, "Hallchen Freunde!-2zb"sv)                                                                               \
    CASE("Nåväl hej vänner"sv, "Nvl hej vnner-cfbhg"sv)                                                                                   \
    CASE("Ну привіт друзі"sv, "  -kjc9flsd9cjetgj5xg"sv)                                                                                  \
    CASE("ليهمابتكلموشعربي؟"sv, "egbpdaj6bu4bxfgehfvwxn"sv)                                                                               \
    CASE("他们为什么不说中文"sv, "ihqwcrb4cv8a8dqg056pqjye"sv)                                                                            \
    CASE("他們爲什麽不說中文"sv, "ihqwctvzc91f659drss3x8bo0yb"sv)                                                                         \
    CASE("Pročprostěnemluvíčesky"sv, "Proprostnemluvesky-uyb24dma41a"sv)                                                                  \
    CASE("למההםפשוטלאמדבריםעברית"sv, "4dbcagdahymbxekheh6e0a7fei0b"sv)                                                                    \
    CASE("यहलोगहिन्दीक्योंनहींबोलसकतेहैं"sv, "i1baa7eci9glrd9b2ae1bj0hfcgg6iyaf8o0a1dig0cd"sv)                                                   \
    CASE("なぜみんな日本語を話してくれないのか"sv, "n8jok5ay5dzabd5bym9f0cm5685rrjetr6pdxa"sv)                                            \
    CASE("세계의모든사람들이한국어를이해한다면얼마나좋을까"sv, "989aomsvi5e83db1d2a355cv1e0vak1dwrv93d5xbh15a0dt30a5jpsd879ccm6fea98c"sv) \
    CASE("почемужеонинеговорятпорусски"sv, "b1abfaaepdrnnbgefbadotcwatmq2g4l"sv)                                                          \
    CASE("PorquénopuedensimplementehablarenEspañol"sv, "PorqunopuedensimplementehablarenEspaol-fmd56a"sv)                                 \
    CASE("TạisaohọkhôngthểchỉnóitiếngViệt"sv, "TisaohkhngthchnitingVit-kjcr8268qyxafd2f1b9g"sv)                                           \
    CASE("3年B組金八先生"sv, "3B-ww4c5e180e575a65lsy2b"sv)                                                                                \
    CASE("安室奈美恵-with-SUPER-MONKEYS"sv, "-with-SUPER-MONKEYS-pc58ag80a8qai00g7n9n"sv)                                                 \
    CASE("Hello-Another-Way-それぞれの場所"sv, "Hello-Another-Way--fc4qua05auwb3674vfr0b"sv)                                              \
    CASE("ひとつ屋根の下2"sv, "2-u9tlzr9756bt3uc0v"sv)                                                                                    \
    CASE("MajiでKoiする5秒前"sv, "MajiKoi5-783gue6qz075azm5e"sv)                                                                          \
    CASE("パフィーdeルンバ"sv, "de-jg4avhby1noc0d"sv)                                                                                     \
    CASE("そのスピードで"sv, "d9juau41awczczp"sv)                                                                                         \
    CASE("-> $1.00 <-"sv, "-> $1.00 <--"sv)

TEST_CASE(decode)
{
#define CASE(a, b) EXPECT_EQ(TRY_OR_FAIL(decode(b)), a);
    ENUMERATE_TEST_CASES
#undef CASE
    EXPECT(decode("Well hello friends!"sv).is_error());
    EXPECT(decode("Nåväl hej vänner"sv).is_error());
}

TEST_CASE(encode)
{
#define CASE(a, b) EXPECT_EQ(TRY_OR_FAIL(encode(a)), b);
    ENUMERATE_TEST_CASES
#undef CASE
}

}
