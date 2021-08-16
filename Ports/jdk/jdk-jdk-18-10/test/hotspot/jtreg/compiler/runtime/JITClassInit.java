/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 4154887
 * @summary self-parser test causes JDK 1.2 Beta4K segmentation fault
 * @author Tom Rodriguez
 * @run main/othervm compiler.runtime.JITClassInit
 */

package compiler.runtime;

public class JITClassInit {
    public static void main(String[] args) {
        Token t = new Token();
        new TokenTable();
    }

}

class TokenTable {
    public TokenTable() {
        new TokenTypeIterator(this);
    }

    public void for_token_type(Token t) {
        t.keyword_character_class();
    }
}

class Token {
    public Object keyword_character_class() {
        return new Object();
    }
}

class NameOrKeywordToken extends Token {
    static TokenTable kt = new TokenTable();
    public Object keyword_character_class() {
        return new Object();
    }
}

class CapKeywordToken extends NameOrKeywordToken {
    public Object keyword_character_class() {
        return new Object();
    }
};


class TokenTypeIterator {
    public TokenTypeIterator(TokenTable c) {
        c.for_token_type(new CapKeywordToken());
        c.for_token_type(new NameOrKeywordToken());
    }
}
