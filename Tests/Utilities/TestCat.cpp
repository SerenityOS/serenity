/*
* SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/StringView.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

static void run_cat(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    MUST(arguments.try_insert(0, "cat"));
    MUST(arguments.try_append(nullptr));
    auto cat = MUST(Core::Command::create("cat"sv, arguments.data()));
    MUST(cat->write(standard_input));
    auto [stdout, stderr] = MUST(cat->read_all());
    auto status = MUST(cat->status());
    if (status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(ByteString::formatted("cat didn't exit cleanly: status: {}, stdout:{}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() }));
    }

    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(show_non_visible_chars)
{
    // Create an array of chars of size 256
    char array[256];
    for (int i = 0; i < 256; ++i) {
        array[i] = i;
    }
    run_cat({ "-v" }, StringView(array, 256), "^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X^X\n !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\n^?\nM-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-\n\nM-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^M-^\nM- M-!M-\"M-#M-$M-%M-&M-'M-(M-)M-*M-+M-,M--M-.M-/M-0M-1M-2M-3M-4M-5M-6M-7M-8M-9M-:M-;M-<M-=M->M-?M-@M-AM-BM-CM-DM-EM-FM-GM-HM-IM-JM-KM-LM-MM-NM-OM-PM-QM-RM-SM-TM-UM-VM-WM-XM-YM-ZM-[M-\\M-]M-^M-_M-`M-aM-bM-cM-dM-eM-fM-gM-hM-iM-jM-kM-lM-mM-nM-oM-pM-qM-rM-sM-tM-uM-vM-wM-xM-yM-zM-{M-|M-}M-~M-^?"sv);
}
