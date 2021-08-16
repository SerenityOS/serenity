/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6190768 6190778
  @summary Tests that triggering events on AWT list by pressing CTRL + HOME,
           CTRL + END, PG-UP, PG-DOWN similar Motif behavior
  @library /test/lib
  @build jdk.test.lib.Platform
  @run main KeyEventsTest
*/

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import jdk.test.lib.Platform;

public class KeyEventsTest extends Frame implements ItemListener, FocusListener, KeyListener
{
    TestState currentState;
    final Object LOCK = new Object();
    final int ACTION_TIMEOUT = 500;

    List single = new List(3, false);
    List multiple = new List(3, true);

    Panel p1 = new Panel ();
    Panel p2 = new Panel ();

    public static void main(final String[] args) {
        KeyEventsTest app = new KeyEventsTest();
        app.init();
        app.start();
    }

    public void init()
    {
        setLayout (new BorderLayout ());

        single.add("0");
        single.add("1");
        single.add("2");
        single.add("3");
        single.add("4");
        single.add("5");
        single.add("6");
        single.add("7");
        single.add("8");

        multiple.add("0");
        multiple.add("1");
        multiple.add("2");
        multiple.add("3");
        multiple.add("4");
        multiple.add("5");
        multiple.add("6");
        multiple.add("7");
        multiple.add("8");

        single.addKeyListener(this);
        single.addItemListener(this);
        single.addFocusListener(this);
        p1.add(single);
        add("North", p1);

        multiple.addKeyListener(this);
        multiple.addItemListener(this);
        multiple.addFocusListener(this);
        p2.add(multiple);
        add("South", p2);

    }//End  init()

    public void start ()
    {

        try{
            setSize (200,200);
            validate();
            setUndecorated(true);
            setLocationRelativeTo(null);
            setVisible(true);

            doTest();
            System.out.println("Test passed.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("The test failed.");
        }

    }// start()

    public void itemStateChanged (ItemEvent ie) {
        System.out.println("itemStateChanged-"+ie);
        this.currentState.setAction(true);
    }

    public void focusGained(FocusEvent e){

        synchronized (LOCK) {
            LOCK.notifyAll();
        }

    }

    public void focusLost(FocusEvent e){
    }

    public void keyPressed(KeyEvent e){
        System.out.println("keyPressed-"+e);
    }

    public void keyReleased(KeyEvent e){
        System.out.println("keyReleased-"+e);
    }

    public void keyTyped(KeyEvent e){
        System.out.println("keyTyped-"+e);
    }

    private void test(TestState currentState)
      throws InterruptedException, InvocationTargetException {

        synchronized (LOCK) {

            this.currentState = currentState;
            System.out.println(this.currentState);

            List list;
            if (currentState.getMultiple()){
                list = multiple;
            }else{
                list = single;
            }

            Robot r;
            try {
                r = new Robot();
            } catch(AWTException e) {
                throw new RuntimeException(e.getMessage());
            }

            r.delay(10);
            Point loc = this.getLocationOnScreen();

            r.mouseMove(loc.x+10, loc.y+10);
            r.mousePress(InputEvent.BUTTON1_MASK);
            r.delay(10);
            r.mouseRelease(InputEvent.BUTTON1_MASK);
            r.delay(10);

            list.requestFocusInWindow();
            LOCK.wait(ACTION_TIMEOUT);
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != list){
                throw new RuntimeException("Test failed - list isn't focus owner.");
            }

            list.deselect(0);
            list.deselect(1);
            list.deselect(2);
            list.deselect(3);
            list.deselect(4);
            list.deselect(5);
            list.deselect(6);
            list.deselect(7);
            list.deselect(8);

            int selectIndex = 0;
            int visibleIndex = 0;

            if (currentState.getScrollMoved()){

                if (currentState.getKeyID() == KeyEvent.VK_PAGE_UP ||
                    currentState.getKeyID() == KeyEvent.VK_HOME){
                    selectIndex = 8;
                    visibleIndex = 8;
                }else if (currentState.getKeyID() == KeyEvent.VK_PAGE_DOWN ||
                    currentState.getKeyID() == KeyEvent.VK_END){
                    selectIndex = 0;
                    visibleIndex = 0;
                }

            }else{

                if (currentState.getKeyID() == KeyEvent.VK_PAGE_UP ||
                    currentState.getKeyID() == KeyEvent.VK_HOME){

                    if (currentState.getSelectedMoved()){
                        selectIndex = 1;
                        visibleIndex = 0;
                    }else{
                        selectIndex = 0;
                        visibleIndex = 0;
                    }

                }else if (currentState.getKeyID() == KeyEvent.VK_PAGE_DOWN ||
                    currentState.getKeyID() == KeyEvent.VK_END){

                    if (currentState.getSelectedMoved()){
                        selectIndex = 7;
                        visibleIndex = 8;
                    }else{
                        selectIndex = 8;
                        visibleIndex = 8;
                    }

                }

            }

            list.select(selectIndex);
            list.makeVisible(visibleIndex);

            r.delay(10);

            if (currentState.getKeyID() == KeyEvent.VK_HOME ||
                currentState.getKeyID() == KeyEvent.VK_END){
                r.keyPress(KeyEvent.VK_CONTROL);
            }

            r.delay(10);
            r.keyPress(currentState.getKeyID());
            r.delay(10);
            r.keyRelease(currentState.getKeyID());
            r.delay(10);

            if (currentState.getKeyID() == KeyEvent.VK_HOME ||
                currentState.getKeyID() == KeyEvent.VK_END){
                r.keyRelease(KeyEvent.VK_CONTROL);
            }

            r.waitForIdle();
            r.delay(200);

            if (currentState.getTemplate() != currentState.getAction())
                throw new RuntimeException("Test failed.");

        }

    }

    private void doTest()
      throws InterruptedException, InvocationTargetException {

        boolean isWin = false;
        if (Platform.isWindows()) {
            isWin = true;
        } else if (Platform.isOSX()) {
            System.out.println("Not for OS X");
            return;
        }

        System.out.println("multiple? selectedMoved? ?scrollMoved keyID? template? action?");
        test(new TestState(false, false, false, KeyEvent.VK_PAGE_UP, isWin?false:false));
        // SelectedMoved (false) != ScrollMoved (true) for single list not emulated
        test(new TestState(false, true, false, KeyEvent.VK_PAGE_UP, isWin?true:false));
        test(new TestState(false, true, true, KeyEvent.VK_PAGE_UP, isWin?true:true));
        test(new TestState(true, false, false, KeyEvent.VK_PAGE_UP, isWin?true:false));
        test(new TestState(true, false, true, KeyEvent.VK_PAGE_UP, isWin?true:false));
        test(new TestState(true, true, false, KeyEvent.VK_PAGE_UP, isWin?true:false));
        test(new TestState(true, true, true, KeyEvent.VK_PAGE_UP, isWin?true:false));

        test(new TestState(false, false, false, KeyEvent.VK_PAGE_DOWN, isWin?false:false));
        test(new TestState(false, true, false, KeyEvent.VK_PAGE_DOWN, isWin?true:false));
        test(new TestState(false, true, true, KeyEvent.VK_PAGE_DOWN, isWin?true:true));
        test(new TestState(true, false, false, KeyEvent.VK_PAGE_DOWN, isWin?true:false));
        test(new TestState(true, false, true, KeyEvent.VK_PAGE_DOWN, isWin?true:false));
        test(new TestState(true, true, false, KeyEvent.VK_PAGE_DOWN, isWin?true:false));
        test(new TestState(true, true, true, KeyEvent.VK_PAGE_DOWN, isWin?true:false));

        test(new TestState(false, false, false, KeyEvent.VK_HOME, isWin?false:true));
        test(new TestState(false, true, false, KeyEvent.VK_HOME, isWin?true:true));
        test(new TestState(false, true, true, KeyEvent.VK_HOME, isWin?true:true));
        test(new TestState(true, false, false, KeyEvent.VK_HOME, isWin?true:false));
        test(new TestState(true, false, true, KeyEvent.VK_HOME, isWin?true:false));
        test(new TestState(true, true, false, KeyEvent.VK_HOME, isWin?true:false));
        test(new TestState(true, true, true, KeyEvent.VK_HOME, isWin?true:false));

        test(new TestState(false, false, false, KeyEvent.VK_END, isWin?false:true));
        test(new TestState(false, true, false, KeyEvent.VK_END, isWin?true:true));
        test(new TestState(false, true, true, KeyEvent.VK_END, isWin?true:true));
        test(new TestState(true, false, false, KeyEvent.VK_END, isWin?true:false));
        test(new TestState(true, false, true, KeyEvent.VK_END, isWin?true:false));
        test(new TestState(true, true, false, KeyEvent.VK_END, isWin?true:false));
        test(new TestState(true, true, true, KeyEvent.VK_END, isWin?true:false));

    }
}// class KeyEventsTest

class TestState{

    private boolean multiple;
    // after key pressing selected item moved
    private final boolean selectedMoved;
    // after key pressing scroll moved
    private final boolean scrollMoved;
    private final int keyID;
    private final boolean template;
    private boolean action;

    public TestState(boolean multiple, boolean selectedMoved, boolean scrollMoved, int keyID, boolean template){
        this.multiple = multiple;
        this.selectedMoved = selectedMoved;
        this.scrollMoved = scrollMoved;
        this.keyID = keyID;
        this.template = template;
        this.action = false;
    }

    public boolean getMultiple(){
        return multiple;
    }
    public boolean getSelectedMoved(){
        return selectedMoved;
    }

    public boolean getScrollMoved(){
        return scrollMoved;
    }

    public int getKeyID(){
        return keyID;
    }

    public boolean getTemplate(){
        return template;
    }

    public boolean getAction(){
        return action;
    }

    public void setAction(boolean action){
        this.action = action;
    }

    public String toString(){
        return multiple + "," + selectedMoved + "," + scrollMoved + "," + keyID + "," + template + "," + action;
    }
}// TestState
