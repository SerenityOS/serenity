#!/usr/bin/env bash

set -euo pipefail

cp "$SERENITY_SOURCE_DIR/Toolchain/CMake/ClangToolchain.txt.in" CMakeToolchain.txt
# Set triple to one Zig expects
sed -i 's/set(triple.*/set(triple '"$ZIG_ARCH"'-serenity-none)/' CMakeToolchain.txt
# Set the parameters
sed -i 's#@SERENITY_SOURCE_DIR@#'"$SERENITY_SOURCE_DIR"'#' CMakeToolchain.txt
sed -i 's#@SERENITY_BUILD_DIR@#'"$SERENITY_BUILD_DIR"'#' CMakeToolchain.txt
sed -i 's/@SERENITY_ARCH@/'"$SERENITY_ARCH"'/' CMakeToolchain.txt
# Update toolchain root
sed -i 's#Toolchain/Local/clang#Ports/zig/'"$ZIG_WORKDIR"'/out/host#' CMakeToolchain.txt
# Set C, C++ and asm compilers to zig cc, zig c++ and llvm-as respectively
sed -i 's#CMAKE_C_COMPILER.*clang#CMAKE_C_COMPILER ${TOOLCHAIN_PATH}/zig cc#' CMakeToolchain.txt
sed -i 's#CMAKE_CXX_COMPILER.*clang#CMAKE_CXX_COMPILER ${TOOLCHAIN_PATH}/zig c#' CMakeToolchain.txt
sed -i 's#CMAKE_ASM_COMPILER.*clang#CMAKE_ASM_COMPILER ${TOOLCHAIN_PATH}/llvm-as#' CMakeToolchain.txt
# Set linker flags for required libs
sed -i 's/set(CMAKE_EXE_LINKER_FLAGS_INIT.*/set(CMAKE_EXE_LINKER_FLAGS_INIT "-lc++ -lm -lpthread -ldl")/' CMakeToolchain.txt
# Don't make CMake assume that the compiler works.
sed -i 's/set(CMAKE_C_COMPILER_WORKS.*//' CMakeToolchain.txt
sed -i 's/set(CMAKE_CXX_COMPILER_WORKS.*//' CMakeToolchain.txt
