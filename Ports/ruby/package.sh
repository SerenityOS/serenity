#!/usr/bin/env -S bash ../.port_include.sh

source version.sh

export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/ruby/bin:${PATH}"

port=ruby
version=${RUBY_VERSION}
useconfigure="true"
use_fresh_config_sub=true
config_sub_paths=("tool/config.sub")
files=(
    "${RUBY_ARCHIVE_URL}#${RUBY_ARCHIVE_SHA256SUM}"
    "https://cache.ruby-lang.org/pub/misc/logo/ruby-logo-kit.zip#7f0a980e09874d35d80b958949dc2460e683957de3d2494a1499aea9d9989055"
)
launcher_name="Ruby IRB"
launcher_category="D&evelopment"
launcher_command="/usr/local/bin/ruby /usr/local/bin/irb --legacy"
launcher_run_in_terminal="true"
icon_file="../ruby-kit/ruby.png"

configopts=(
    "--disable-install-doc"
)

case "${SERENITY_ARCH}" in
    x86_64)
        configopts+=("--with-coroutine=amd64")
        ;;
    *)
        echo "Error: Architecture ${SERENITY_ARCH} is not supported for this port"
        exit 1
esac

export CFLAGS="-DNGROUPS_MAX=65536"

# Note: The showproperty command is used when linting ports, we don't actually need ruby at this time.
if [ "$1" != "showproperty" ]; then
    if [ -x "$(command -v ruby)" ]; then
        # Check if major and minor version of ruby are matching
        if [ $(ruby --version | awk {'printf $2'} | awk -Fp {'print $1'}) != "$RUBY_VERSION" ]; then
            echo "Error: ruby version does not match needed version to build ${RUBY_VERSION}" >&2
            echo "Build this ruby version on your host using Toolchain/BuildRuby.sh or install it otherwise and try again." >&2
            exit 1
        fi
    else
        echo "Error: ruby is not installed but is required to build ${RUBY_VERSION}" >&2
        echo "Build this ruby version on your host using Toolchain/BuildRuby.sh or install it otherwise and try again." >&2
        exit 1
    fi
fi
