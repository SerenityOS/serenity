#!/usr/bin/env -S bash ../.port_include.sh
port=llvm
useconfigure=true
version=12.0.0
workdir=llvm-project-llvmorg-${version}
configopts="-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
files="https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-${version}.zip llvm.zip f77723b70a5d4ab14899feda87d6cf601612165899abb2f6c7b670e517f45e2d"
auth_type=sha256

pre_patch() {
    host_env
    mkdir -p llvm-host
    cmake ${workdir}/llvm \
    -B llvm-host \
    -DLLVM_ENABLE_PROJECTS=clang
    make -C llvm-host -j $(nproc) llvm-tblgen clang-tblgen
    target_env
}

configure() {
    mkdir -p llvm-build
    cmake ${workdir}/llvm \
    -B llvm-build $configopts \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DLLVM_TARGETS_TO_BUILD=X86 \
    -DLLVM_ENABLE_PROJECTS="clang;lld;compiler-rt" \
    -DLLVM_TABLEGEN=$(pwd)/llvm-host/bin/llvm-tblgen \
    -DCMAKE_CROSSCOMPILING=True \
    -DLLVM_DEFAULT_TARGET_TRIPLE=i386-none-gnueabi \
    -DLLVM_TARGET_ARCH=X86 \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DCLANG_TABLEGEN=$(pwd)/llvm-host/bin/clang-tblgen
}

build() {
    make -C llvm-build -j $(nproc) --no-print-directory
}

install() {
    make -C llvm-build -j $(nproc) --no-print-directory install
}
