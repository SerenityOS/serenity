#!/usr/bin/env -S bash ../.port_include.sh
port=llvm
useconfigure=true
version=12.0.1
workdir=llvm-project-llvmorg-${version}
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files="https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-${version}.zip llvm.zip a076025f1229fd60f969e779a62ea4e221bf2c77900ac54121cca32fa808c14d"
auth_type=sha256

pre_patch() {
    host_env
    mkdir -p llvm-host-${version}
    cmake ${workdir}/llvm \
    -B llvm-host-${version} \
    -DLLVM_ENABLE_PROJECTS=clang
    make -C llvm-host-${version} -j $(nproc) llvm-tblgen clang-tblgen
    target_env
}

configure() {
    mkdir -p llvm-build-${version}
    cmake ${workdir}/llvm \
    -B llvm-build-${version} "${configopts[@]}" \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DLLVM_TARGETS_TO_BUILD=X86 \
    -DLLVM_ENABLE_PROJECTS="clang;lld;compiler-rt" \
    -DLLVM_TABLEGEN=$(pwd)/llvm-host-${version}/bin/llvm-tblgen \
    -DCMAKE_CROSSCOMPILING=True \
    -DLLVM_DEFAULT_TARGET_TRIPLE=i386-none-gnueabi \
    -DLLVM_TARGET_ARCH=X86 \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DCLANG_TABLEGEN=$(pwd)/llvm-host-${version}/bin/clang-tblgen
}

build() {
    make -C llvm-build-${version} -j $(nproc) --no-print-directory
}

install() {
    make -C llvm-build-${version} -j $(nproc) --no-print-directory install
}
