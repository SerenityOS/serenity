# gn build for SerenityOS and Ladybird

Warning The GN build is experimental and best-effort. It might not work, and if you use it you're expected to feel comfortable to unbreak it if necessary. Serenity's official build system is CMake, if in doubt use that. If you add files, you're expected to update the CMake build but you don't need to update GN build files. Reviewers should not ask authors to update GN build files. Keeping the GN build files up-to-date is on the people who use the GN build.

GN is a metabuild system. It always creates ninja files, but it can create some IDE projects (MSVC, Xcode, ...) which then shell out to ninja for the actual build.

This is a good [overview of GN](https://docs.google.com/presentation/d/15Zwb53JcncHfEwHpnG_PoIbbzQ3GQi_cpujYwbpcbZo/edit#slide=id.g119d702868_0_12).

For more information, motivation, philosophy, and inspriation, see the LLVM documentation on its [GN build](https://github.com/llvm/llvm-project/tree/main/llvm/utils/gn#quick-start)
