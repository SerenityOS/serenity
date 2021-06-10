# LLVM port for SerenityOS

This folder contains the SerenityOS port of LLVM/Clang. Right now it builds LLVM, Clang, lld and compiler-rt.
To compile programs it is recommended to install the GCC port because Clang is not able to use lld yet, 
that's why GNU ld should be used.
