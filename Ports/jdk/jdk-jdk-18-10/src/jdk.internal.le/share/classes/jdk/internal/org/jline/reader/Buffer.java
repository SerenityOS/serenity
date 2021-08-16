/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

public interface Buffer {

    /*
     * Read access
     */

    int cursor();

    int atChar(int i);

    int length();

    int currChar();

    int prevChar();

    int nextChar();

    /*
     * Movement
     */

    boolean cursor(int position);

    int move(int num);

    boolean up();

    boolean down();

    boolean moveXY(int dx, int dy);

    /*
     * Modification
     */

    boolean clear();

    boolean currChar(int c);

    void write(int c);

    void write(int c, boolean overTyping);

    void write(CharSequence str);

    void write(CharSequence str, boolean overTyping);

    boolean backspace();

    int backspace(int num);

    boolean delete();

    int delete(int num);

    /*
     * String
     */

    String substring(int start);

    String substring(int start, int end);

    String upToCursor();

    String toString();

    /*
     * Copy
     */

    Buffer copy();

    void copyFrom(Buffer buffer);

}
