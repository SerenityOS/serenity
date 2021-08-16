/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.io.IOException;
import java.util.List;

public interface Editor {
    public void open(List<String> files) throws IOException;
    public void run() throws IOException;
    public void setRestricted(boolean restricted);
}
