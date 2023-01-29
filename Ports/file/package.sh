#!/usr/bin/env -S bash ../.port_include.sh
port='file'
version='5.43'
useconfigure='true'
use_fresh_config_sub='true'
config_sub_paths=('config.sub')
files="http://ftp.astron.com/pub/file/file-${version}.tar.gz file-${version}.tar.gz 8c8015e91ae0e8d0321d94c78239892ef9dbc70c4ade0008c0e95894abfb1991"
auth_type='sha256'
