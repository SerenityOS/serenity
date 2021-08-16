#! /bin/sh
#
# Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# Generate exception classes

SPEC=$1
DST=$2

gen() {
  ID=$1
  WHAT=$2
  SVUID=$3
  ARG_TYPE=$4
  ARG_ID=$5
  ARG_PROP=$6
  ARG_PHRASE=$7
  ARG_PARAM="$ARG_TYPE$ $ARG_ID"
  echo '-->' $DST/$ID.java
  out=$DST/${ID}.java

  $SH ${SCRIPTS}/addNotices.sh "$COPYRIGHT_YEARS" > $out

  cat >>$out <<__END__

// -- This file was mechanically generated: Do not edit! -- //

package $PACKAGE;


/**$WHAT
 *
 * @since $SINCE
 */

public `if [ ${ABSTRACT:-0} = 1 ];
        then echo 'abstract '; fi`class $ID
    extends ${SUPER}
{

    @java.io.Serial
    private static final long serialVersionUID = $SVUID;
__END__

  if [ $ARG_ID ]; then

    cat >>$out <<__END__

    /**
     * The $ARG_PHRASE.
     */
    private $ARG_TYPE $ARG_ID;

    /**
     * Constructs an instance of this class.
     *
     * @param  $ARG_ID
     *         The $ARG_PHRASE
     */
    public $ID($ARG_TYPE $ARG_ID) {
        super(String.valueOf($ARG_ID));
	this.$ARG_ID = $ARG_ID;
    }

    /**
     * Retrieves the $ARG_PHRASE.
     *
     * @return  The $ARG_PHRASE
     */
    public $ARG_TYPE get$ARG_PROP() {
        return $ARG_ID;
    }

}
__END__

  else

    cat >>$out <<__END__

    /**
     * Constructs an instance of this class.
     */
    public $ID() { }

}
__END__

  fi
}

. $SPEC
