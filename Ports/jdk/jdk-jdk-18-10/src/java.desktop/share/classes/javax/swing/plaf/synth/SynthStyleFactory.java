/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package javax.swing.plaf.synth;

import javax.swing.JComponent;

/**
 * Factory used for obtaining <code>SynthStyle</code>s.  Each of the
 * Synth <code>ComponentUI</code>s will call into the current
 * <code>SynthStyleFactory</code> to obtain a <code>SynthStyle</code>
 * for each of the distinct regions they have.
 * <p>
 * The following example creates a custom <code>SynthStyleFactory</code>
 * that returns a different style based on the <code>Region</code>:
 * <pre>
 * class MyStyleFactory extends SynthStyleFactory {
 *     public SynthStyle getStyle(JComponent c, Region id) {
 *         if (id == Region.BUTTON) {
 *             return buttonStyle;
 *         }
 *         else if (id == Region.TREE) {
 *             return treeStyle;
 *         }
 *         return defaultStyle;
 *     }
 * }
 * SynthLookAndFeel laf = new SynthLookAndFeel();
 * UIManager.setLookAndFeel(laf);
 * SynthLookAndFeel.setStyleFactory(new MyStyleFactory());
 * </pre>
 *
 * @see SynthLookAndFeel
 * @see SynthStyle
 *
 * @since 1.5
 * @author Scott Violet
 */
public abstract class SynthStyleFactory {
    /**
     * Creates a <code>SynthStyleFactory</code>.
     */
    public SynthStyleFactory() {
    }

    /**
     * Returns the style for the specified Component.
     *
     * @param c Component asking for
     * @param id Region identifier
     * @return SynthStyle for region.
     */
    public abstract SynthStyle getStyle(JComponent c, Region id);
}
