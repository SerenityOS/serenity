#!/usr/bin/env -S bash ../.port_include.sh
port=mruby
version=3.0.0
files=(
    "https://github.com/mruby/mruby/archive/refs/tags/${version}.tar.gz#95b798cdd931ef29d388e2b0b267cba4dc469e8722c37d4ef8ee5248bc9075b0"
)

build_config="serenity"
build_dir="build/${build_config}"

build() {
    unset CC
    unset AR
    unset LD
    unset CXX
    export MRUBY_CONFIG="$build_config"
    run rake
}

install() {
    run cp "${build_dir}/bin/mirb" "${SERENITY_INSTALL_ROOT}/bin/"
    run cp "${build_dir}/bin/mrbc" "${SERENITY_INSTALL_ROOT}/bin/"
    run cp "${build_dir}/bin/mruby" "${SERENITY_INSTALL_ROOT}/bin/"
    run cp "${build_dir}/lib/libmruby.a" "${SERENITY_INSTALL_ROOT}/usr/lib/"
    run cp "${build_dir}/lib/libmruby_core.a" "${SERENITY_INSTALL_ROOT}/usr/lib/"
}
