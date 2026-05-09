#!/usr/bin/env -S bash ../.port_include.sh
port=emu2
version="4948d1e12b952f0a30c7e4aa8fa380b51c0c84a7"
files=(
    "https://github.com/dmsc/emu2/archive/${version}.zip#407b0a1bbd4ea561ef8b547a74d811fb705d7e0f5c72b6d7d5db5900708c5f19"
)

build() {
    run make "${installopts[@]}"
}
