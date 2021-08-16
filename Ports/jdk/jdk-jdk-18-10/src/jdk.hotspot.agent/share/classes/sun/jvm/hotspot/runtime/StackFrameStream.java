/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.runtime;

import sun.jvm.hotspot.utilities.*;

/** <P> StackFrameStream iterates through the frames of a thread
    starting from top most frame. It automatically takes care of
    updating the location of all (callee-saved) registers. Notice: If
    a thread is stopped at a safepoint, all registers are saved, not
    only the callee-saved ones. </P>

    <P> Use: </P>

    <PRE>
    for(StackFrameStream fst = new StackFrameStream(thread); !fst.isDone(); fst.next()) {
      ...
    }
    </PRE>
*/

public class StackFrameStream {
  private Frame       fr;
  private RegisterMap regMap;
  private boolean     isDone;

  /** Equivalent to StackFrameStream(thread, true) */
  public StackFrameStream(JavaThread thread) {
    this(thread, true);
  }

  public StackFrameStream(JavaThread thread, boolean update) {
    if (!VM.getVM().isDebugging()) {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(thread.hasLastJavaFrame(), "sanity check");
      }
      fr = thread.getLastFrame();
      regMap = thread.newRegisterMap(update);
      isDone = false;
    } else {
      // Special case code to find the topmost Java frame
      // FIXME: should check to see whether we're at safepoint, and if
      // so, skip the "current frame guess" call and unnecessary
      // stackwalking work
      fr = thread.getCurrentFrameGuess();
      regMap = thread.newRegisterMap(update);
      while ((fr != null) && (!fr.isJavaFrame())) {
        if (fr.isFirstFrame()) {
          fr = null;
        } else {
          fr = fr.sender(regMap);
        }
      }
      if (fr == null) {
        isDone = true;
      }
    }
  }

  /** Iteration */
  public boolean isDone() {
    if (isDone) {
      return true;
    } else {
      if (fr == null) {
        isDone = true;
        return true;
      }
      isDone = fr.isFirstFrame();
      return false;
    }
  }

  public void next() {
    if (!isDone) {
      fr = fr.sender(regMap);
    }
  }

  /** Query */
  public Frame getCurrent()           { return fr;     }
  public RegisterMap getRegisterMap() { return regMap; }
}
