#!/usr/bin/env -S bash ../.port_include.sh
port=RISCVEmu
version=ad8ad6a0eb8591385318b2ec1cffde6078ff0185
files=(
    "https://github.com/IdanHo/RISCVEmu/archive/${version}.tar.gz#b4636284dd407e490ba6dd783b65caf8c019785285d6a86aece3860465276b33"
)

build() {
    run "${CXX}" -o RISCVEmu RISCVEmu.cpp RISCV.cpp
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp RISCVEmu "${SERENITY_INSTALL_ROOT}/usr/local/bin"
}
