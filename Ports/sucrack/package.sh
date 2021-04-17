#!/usr/bin/env -S bash ../.port_include.sh
port=sucrack
version=1.2.3
files="http://labs.portcullis.co.uk/download/sucrack-${version}.tar.gz sucrack-${version}.tar.gz f0cce09be99e1b3bc04e0d0e36424b61f187d1a6501597a30c7aeaf3bab73bab"
auth_type="sha256"
depends="ncurses"
useconfigure="true"
