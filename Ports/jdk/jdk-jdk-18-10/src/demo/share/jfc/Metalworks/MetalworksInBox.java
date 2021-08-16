/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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



import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;


/**
 * This is a subclass of JInternalFrame which displays a tree.
 *
 * @author Steve Wilson
 * @author Alexander Kouznetsov
 */
@SuppressWarnings("serial")
public class MetalworksInBox extends JInternalFrame {

    public MetalworksInBox() {
        super("In Box", true, true, true, true);

        DefaultMutableTreeNode unread;
        DefaultMutableTreeNode personal;
        DefaultMutableTreeNode business;
        DefaultMutableTreeNode spam;

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Mail Boxes");

        top.add(unread = new DefaultMutableTreeNode("Unread Mail"));
        top.add(personal = new DefaultMutableTreeNode("Personal"));
        top.add(business = new DefaultMutableTreeNode("Business"));
        top.add(spam = new DefaultMutableTreeNode("Spam"));

        unread.add(new DefaultMutableTreeNode("Buy Stuff Now"));
        unread.add(new DefaultMutableTreeNode("Read Me Now"));
        unread.add(new DefaultMutableTreeNode("Hot Offer"));
        unread.add(new DefaultMutableTreeNode("Re: Re: Thank You"));
        unread.add(new DefaultMutableTreeNode("Fwd: Good Joke"));

        personal.add(new DefaultMutableTreeNode("Hi"));
        personal.add(new DefaultMutableTreeNode("Good to hear from you"));
        personal.add(new DefaultMutableTreeNode("Re: Thank You"));

        business.add(new DefaultMutableTreeNode("Thanks for your order"));
        business.add(new DefaultMutableTreeNode("Price Quote"));
        business.add(new DefaultMutableTreeNode("Here is the invoice"));
        business.add(new DefaultMutableTreeNode(
                "Project Metal: delivered on time"));
        business.add(new DefaultMutableTreeNode("Your salary raise approved"));

        spam.add(new DefaultMutableTreeNode("Buy Now"));
        spam.add(new DefaultMutableTreeNode("Make $$$ Now"));
        spam.add(new DefaultMutableTreeNode("HOT HOT HOT"));
        spam.add(new DefaultMutableTreeNode("Buy Now"));
        spam.add(new DefaultMutableTreeNode("Don't Miss This"));
        spam.add(new DefaultMutableTreeNode("Opportunity in Precious Metals"));
        spam.add(new DefaultMutableTreeNode("Buy Now"));
        spam.add(new DefaultMutableTreeNode("Last Chance"));
        spam.add(new DefaultMutableTreeNode("Buy Now"));
        spam.add(new DefaultMutableTreeNode("Make $$$ Now"));
        spam.add(new DefaultMutableTreeNode("To Hot To Handle"));
        spam.add(new DefaultMutableTreeNode("I'm waiting for your call"));

        JTree tree = new JTree(top);
        JScrollPane treeScroller = new JScrollPane(tree);
        treeScroller.setBackground(tree.getBackground());
        setContentPane(treeScroller);
        setSize(325, 200);
        setLocation(75, 75);

    }
}
