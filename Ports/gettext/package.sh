#!/usr/bin/env -S bash ../.port_include.sh
port='gettext'
version='0.21.1'
useconfigure='true'
files=(
    "https://ftpmirror.gnu.org/gettext/gettext-${version}.tar.gz e8c3650e1d8cee875c4f355642382c1df83058bd5a11ee8555c0cf276d646d45"
)
depends=("libiconv")
use_fresh_config_sub='true'
config_sub_paths=("build-aux/config.sub" "libtextstyle/build-aux/config.sub")
