/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Random;
import javax.swing.tree.DefaultMutableTreeNode;


/**
 * DynamicTreeNode illustrates one of the possible ways in which dynamic
 * loading can be used in tree.  The basic premise behind this is that
 * getChildCount() will be messaged from JTreeModel before any children
 * are asked for.  So, the first time getChildCount() is issued the
 * children are loaded.<p>
 * It should be noted that isLeaf will also be messaged from the model.
 * The default behavior of TreeNode is to message getChildCount to
 * determine this. As such, isLeaf is subclassed to always return false.<p>
 * There are others ways this could be accomplished as well.  Instead of
 * subclassing TreeNode you could subclass JTreeModel and do the same
 * thing in getChildCount().  Or, if you aren't using TreeNode you could
 * write your own TreeModel implementation.
 * Another solution would be to listen for TreeNodeExpansion events and
 * the first time a node has been expanded post the appropriate insertion
 * events.  I would not recommend this approach though, the other two
 * are much simpler and cleaner (and are faster from the perspective of
 * how tree deals with it).
 *
 * NOTE: getAllowsChildren() can be messaged before getChildCount().
 *       For this example the nodes always allow children, so it isn't
 *       a problem, but if you do support true leaf nodes you may want
 *       to check for loading in getAllowsChildren too.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial")
public class DynamicTreeNode extends DefaultMutableTreeNode {
    // Class stuff.

    /** Number of names. */
    protected static float nameCount;
    /** Names to use for children. */
    protected static final String[] NAMES;
    /** Potential fonts used to draw with. */
    protected static Font[] fonts;
    /** Used to generate the names. */
    protected static Random nameGen;
    /** Number of children to create for each node. */
    protected static final int DEFAULT_CHILDREN_COUNT = 7;

    static {
        String[] fontNames;

        try {
            fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().
                    getAvailableFontFamilyNames();

        } catch (Exception e) {
            fontNames = null;
        }
        if (fontNames == null || fontNames.length == 0) {
            NAMES = new String[] { "Mark Andrews", "Tom Ball", "Alan Chung",
                        "Rob Davis", "Jeff Dinkins",
                        "Amy Fowler", "James Gosling",
                        "David Karlton", "Dave Kloba",
                        "Dave Moore", "Hans Muller",
                        "Rick Levenson", "Tim Prinzing",
                        "Chester Rose", "Ray Ryan",
                        "Georges Saab", "Scott Violet",
                        "Kathy Walrath", "Arnaud Weber" };
        } else {
            /* Create the Fonts, creating fonts is slow, much better to
            do it once. */
            int fontSize = 12;

            NAMES = fontNames;
            fonts = new Font[NAMES.length];
            for (int counter = 0, maxCounter = NAMES.length;
                    counter < maxCounter; counter++) {
                try {
                    fonts[counter] = new Font(fontNames[counter], 0, fontSize);
                } catch (Exception e) {
                    fonts[counter] = null;
                }
                fontSize = ((fontSize + 2 - 12) % 12) + 12;
            }
        }
        nameCount = (float) NAMES.length;
        nameGen = new Random(System.currentTimeMillis());
    }
    /** Have the children of this node been loaded yet? */
    protected boolean hasLoaded;

    /**
     * Constructs a new DynamicTreeNode instance with o as the user
     * object.
     */
    public DynamicTreeNode(Object o) {
        super(o);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * If hasLoaded is false, meaning the children have not yet been
     * loaded, loadChildren is messaged and super is messaged for
     * the return value.
     */
    @Override
    public int getChildCount() {
        if (!hasLoaded) {
            loadChildren();
        }
        return super.getChildCount();
    }

    /**
     * Messaged the first time getChildCount is messaged.  Creates
     * children with random names from names.
     */
    protected void loadChildren() {
        DynamicTreeNode newNode;
        Font font;
        int randomIndex;
        SampleData data;

        for (int counter = 0; counter < DynamicTreeNode.DEFAULT_CHILDREN_COUNT;
                counter++) {
            randomIndex = (int) (nameGen.nextFloat() * nameCount);
            String displayString = NAMES[randomIndex];
            if (fonts == null || fonts[randomIndex].canDisplayUpTo(displayString)
                    != -1) {
                font = null;
            } else {
                font = fonts[randomIndex];
            }

            if (counter % 2 == 0) {
                data = new SampleData(font, Color.red, displayString);
            } else {
                data = new SampleData(font, Color.blue, displayString);
            }
            newNode = new DynamicTreeNode(data);
            /* Don't use add() here, add calls insert(newNode, getChildCount())
            so if you want to use add, just be sure to set hasLoaded = true
            first. */
            insert(newNode, counter);
        }
        /* This node has now been loaded, mark it so. */
        hasLoaded = true;
    }
}
