#!/usr/bin/env -S bash ../.port_include.sh

ZIG_COMMIT="31af7876d5feeb5505a345f558157136aee3cf03"
ZIG_COMMIT_SHORT="31af7876d"

port="zig"
version="0.9.0+git-$ZIG_COMMIT_SHORT"
files="https://github.com/ziglang/zig-bootstrap/archive/refs/tags/0.9.0.zip zig-bootstrap-0.9.0.zip d669c95ca349cf584fa2bb5b66ced23ba0daafa9b4e3494ad84dc6ea070b1755
https://github.com/sin-ack/zig/archive/$ZIG_COMMIT.zip zig-$ZIG_COMMIT_SHORT.zip 30cb53d2abc96318b15d94e805b93392914a88600dda2d169f74239a78f6cdc1"
auth_type="sha256"
workdir="zig-bootstrap-0.9.0"

pre_fetch() {
    case $SERENITY_ARCH in
        i686) export ZIG_ARCH=i386;;
        **) echo "The Zig port is currently not supported on $SERENITY_ARCH."; exit 1;;
    esac
    export ZIG_WORKDIR="$workdir"
}

post_fetch() {
    # Replace zig 0.9.0 with the master version we want, if it's been extracted.
    if [[ -d zig-$ZIG_COMMIT ]]; then
        run rm -rf zig
        run_nocd mv zig-$ZIG_COMMIT $workdir/zig
        # Since we just overwrote the zig dir, let's make sure that the libcxx
        # patches are applied again.
        if [[ -f "$workdir/.0003-libc-Add-support-for-SerenityOS.patch_applied" ]]; then
            run rm .0003-libc-Add-support-for-SerenityOS.patch_applied
        fi
    fi

    # HACK: We want to use the LLVM toolchain patches, but the libcxx lives in
    #       the zig/lib directory. So we move libcxx out to apply patches on it.
    run mv zig/lib/libcxx libcxx
}

build() {
    # HACK: Move back the libcxx directory.
    run mv libcxx zig/lib/libcxx

    # Prepare the libc installation and CMakeToolchain files.
    run ../scripts/prepare-cmake-toolchain-file.sh
    run ../scripts/generate-libc-installation-file.sh

    # Run the build. Note that the build first has to run in the host
    # environment, and will take over the target toolchain switch by itself.
    host_env
    run ./build -j$(nproc) i386-serenity-none native
}

install() {
    # Copy the zig standard library next to the zig binary.
    # FIXME: This is nasty. Find a proper place for this.
    run cp -R zig/lib/ $SERENITY_BUILD_DIR/Root/usr/local/bin
}
