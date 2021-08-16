/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test %I% %E%
  @key headful
  @bug 6315717
  @summary verifies that modifiers are correct for standard (1, 2, 3, wheel) mouse buttons
  @author Andrei Dmitriev : area=awt.mouse
  @run main MouseModifiersUnitTest_Standard
 */

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

//the test verifies:
// 1) verifies that modifiers are correct for standard (1, 2, 3) mouse buttons
// TODO: 2) verifies that modifiers are correct for wheel
// TODO: 3)
// Case1. the test posts BUTTONx_MASK and verifies that paramString() contains correct modifiers and exModifiers
// Case2. the test posts BUTTONx_DOWN_MASK and verifies that paramString() contains correct modifiers and exModifiers
// Case3. the test posts getMaskForButton(n) and verifies that paramString() contains correct modifiers and exModifiers
// repeat all cases with SHIFT/ALT/CTRL modifiers verify that paramString() contains correct modifiers and exModifiers
// I'm verifying button, modifiers and extModifiers for now.

public class MouseModifiersUnitTest_Standard {
    static final int NONE = 0;
    static final int SHIFT = 1;
    static final int CTRL = 2;
    static final int ALT = 3;
    static boolean debug = true; //dump all errors (debug) or throw first(under jtreg) exception
    static boolean autorun = false; //use robot or manual run
    static int testModifier = NONE;
    //    static String testModifier = "NONE";
    static CheckingModifierAdapter adapterTest1;
    static CheckingModifierAdapter adapterTest2;
    static CheckingModifierAdapter adapterTest3;
    static CheckingModifierAdapter adapterTest4;
    static Frame f;
    final static int [] mouseButtons = new int [] {MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON2_MASK, MouseEvent.BUTTON3_MASK};
    // BUTTON1, 2, 3 press-release.
    final static int [] modifiersStandardTestNONE = new int[] {MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK,
    MouseEvent.BUTTON2_MASK, MouseEvent.BUTTON2_MASK, MouseEvent.BUTTON2_MASK,
    MouseEvent.BUTTON3_MASK, MouseEvent.BUTTON3_MASK, MouseEvent.BUTTON3_MASK };
    final static int [] modifiersExStandardTestNONE = new int[] {MouseEvent.BUTTON1_DOWN_MASK, 0, 0,
    MouseEvent.BUTTON2_DOWN_MASK, 0, 0,
    MouseEvent.BUTTON3_DOWN_MASK, 0, 0};
    // BUTTON1, 2, 3 press-release with shift modifier
    final static int [] modifiersStandardTestSHIFT = new int[] {MouseEvent.BUTTON1_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON1_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON1_MASK|InputEvent.SHIFT_MASK,
    MouseEvent.BUTTON2_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON2_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON2_MASK|InputEvent.SHIFT_MASK,
    MouseEvent.BUTTON3_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON3_MASK|InputEvent.SHIFT_MASK, MouseEvent.BUTTON3_MASK|InputEvent.SHIFT_MASK };
    final static int [] modifiersExStandardTestSHIFT = new int[] {MouseEvent.BUTTON1_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK,
    MouseEvent.BUTTON2_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK,
    MouseEvent.BUTTON3_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK};
    // BUTTON1, 2, 3 press-release with CTRL modifier
    final static int [] modifiersStandardTestCTRL = new int[] {MouseEvent.BUTTON1_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON1_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON1_MASK|InputEvent.CTRL_MASK,
    MouseEvent.BUTTON2_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON2_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON2_MASK|InputEvent.CTRL_MASK,
    MouseEvent.BUTTON3_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON3_MASK|InputEvent.CTRL_MASK, MouseEvent.BUTTON3_MASK|InputEvent.CTRL_MASK };
    final static int [] modifiersExStandardTestCTRL = new int[] {MouseEvent.BUTTON1_DOWN_MASK|InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK,
    MouseEvent.BUTTON2_DOWN_MASK|InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK,
    MouseEvent.BUTTON3_DOWN_MASK|InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.CTRL_DOWN_MASK};

    // BUTTON1, 2, 3 press-release with ALT modifier
    final static int [] modifiersStandardTestALT = new int[] {MouseEvent.BUTTON1_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON1_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON1_MASK|InputEvent.ALT_MASK,
    MouseEvent.BUTTON2_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON2_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON2_MASK|InputEvent.ALT_MASK,
    MouseEvent.BUTTON3_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON3_MASK|InputEvent.ALT_MASK, MouseEvent.BUTTON3_MASK|InputEvent.ALT_MASK };
    final static int [] modifiersExStandardTestALT = new int[] {MouseEvent.BUTTON1_DOWN_MASK|InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK,
    MouseEvent.BUTTON2_DOWN_MASK|InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK,
    MouseEvent.BUTTON3_DOWN_MASK|InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.ALT_DOWN_MASK};

    static Robot robot;

    public static void main(String s[]){
        initParams(s);
        initAdapters();
        f = new Frame();
        final int [] modifiers = {InputEvent.SHIFT_MASK, InputEvent.CTRL_MASK};
        final String [] modifierNames = {"InputEvent.SHIFT_MASK", "InputEvent.CTRL_MASK"};
        f.setLayout(new FlowLayout());
        f.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                System.out.println("WHEEL "+e);
            }
        });
        f.setSize(300, 300);
        f.setVisible(true);

        try {
            robot = new Robot();
            robot.delay(500);
            robot.mouseMove(f.getLocationOnScreen().x + f.getWidth()/2, f.getLocationOnScreen().y + f.getHeight()/2);
            if (autorun) {
                //testing buttons 1, 2, 3 only
                testPlainButtons();
                robot.delay(500);

                //testing buttons 1, 2, 3 with SHIFT, CTRL, ALT keyboard modifiers
                testButtonsWithShift();
                robot.delay(500);

                testButtonsWithControl();
                robot.delay(500);

                testButtonsWithAlt();
                robot.delay(500);
            } else {
                switch (testModifier){
                    case SHIFT:
                        f.addMouseListener(adapterTest2);
                        break;
                    case CTRL:
                        f.addMouseListener(adapterTest3);
                        break;
                    case ALT:
                        f.addMouseListener(adapterTest4);
                        break;
                    default:  //NONE inclusive
                        f.addMouseListener(adapterTest1);
                }
            }
        } catch (Exception e){
            throw new RuntimeException("Test failed.");
        }

    }

    public static void initAdapters(){
        adapterTest1 = new CheckingModifierAdapter(NONE);
        adapterTest2 = new CheckingModifierAdapter(SHIFT);
        adapterTest3 = new CheckingModifierAdapter(CTRL);
        adapterTest4 = new CheckingModifierAdapter(ALT);
    }

    /*======================================================================*/
    public static void checkPressedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardModifiers = getStandardArray(testModifier);
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 1)*3;
        //        int index = (button - 4)*3;
        dumpValues(button, modifiers, curStandardModifiers[index], modifiersEx, curStandardExModifiers[index]);
        if (modifiers != curStandardModifiers[index]){
            if (debug){
                System.out.println("Test failed :  Pressed. modifiers != modifiersStandard");
            } else {
                throw new RuntimeException("Test failed :  Pressed. modifiers != modifiersStandard");
            }
        }

        if (modifiersEx != curStandardExModifiers[index]){
//            System.out.println(">>>>>>>>>>>>>>> Pressed. modifiersEx "+modifiersEx +" : "+!= curStandardExModifiers");
            if (debug){
                System.out.println("Test failed :  Pressed. modifiersEx != curStandardExModifiers");
            } else {
                throw new RuntimeException("Test failed :  Pressed. modifiersEx != curStandardExModifiers");
            }
        }
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        System.out.println(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        checkExtModifiersOnPress(testModifier, paramStringElements, button);
    }

    public static void checkButton(HashMap<String, String> h, int button){
        if (h.get("button") == null) {
            throw new RuntimeException("Test failed :  Clicked. button is absent in paramString()");
        }
        if (Integer.parseInt(h.get("button")) != button) {
            throw new RuntimeException("Test failed :  Clicked. button in paramString() doesn't equal to button being pressed.");
        }
    }

    public static void checkExtModifiersOnPress(int testModifier, HashMap h, int button){
        String ethalon = "";
        if (h.get("extModifiers") == null) {
            System.out.println("Test failed :  Pressed. extModifiers == null");
            throw new RuntimeException("Test failed :  Pressed. extModifiers == null");
        }
        switch (testModifier){
            case SHIFT:{
                ethalon = "Shift+";
                break;
            }
            case ALT:{
                ethalon = "Alt+";
                break;
            }
            case CTRL:{
                ethalon = "Ctrl+";
                break;
            }
            default: {
                ethalon = "";
            }
            ethalon = ethalon + "Button" +button;

            if (!h.get("extModifiers").equals(ethalon)) {
                System.out.println("Test failed :  Pressed. extModifiers = " +h.get("extModifiers")+" instead of : "+ethalon);
                throw new RuntimeException("Test failed :  Pressed. extModifiers = " +h.get("extModifiers")+" instead of : "+ethalon);
            }
        }
    }



    public static void checkModifiers(int testModifier, HashMap<String, String> h, int button){
        // none of modifiers should be null
        if (h.get("modifiers") == null) {
            System.out.println("Test failed : modifiers == null");
            throw new RuntimeException("Test failed :  modifiers == null");
        }
        Vector <String> modifierElements = tokenizeModifiers(h.get("modifiers"));
        //check that ButtonX is there
        String buttonEthalon = "Button" + button;
        if (modifierElements.contains(buttonEthalon)){
            modifierElements.remove(buttonEthalon);
        } else {
            System.out.println("Test failed :  modifiers doesn't contain Button "+h.get("modifiers"));
            throw new RuntimeException("Test failed :  modifiers doesn't contain Button "+h.get("modifiers"));
        }


        //Check all explicitly pressed modifires
//        boolean altIncluded = false; //don't duplicate Alt when ALT is pressed and BUTTON2_MASK.
        String excplicitModifier = "";
        boolean altIncluded = false;
        switch (testModifier){
            case SHIFT:{
                excplicitModifier = "Shift";
                break;
            }
            case ALT:{
                excplicitModifier = "Alt";
                altIncluded = true; //there should be only on "Alt" for two modifiers. So check it.
                break;
            }
            case CTRL:{
                excplicitModifier = "Ctrl";
                break;
            }
        }
        if (!excplicitModifier.equals("")){
            if (modifierElements.contains(excplicitModifier)){
                modifierElements.remove(excplicitModifier);
            } else {
                System.out.println("Test failed :  modifiers doesn't contain explicit modifier "+excplicitModifier + " in "+ h.get("modifiers"));
                throw new RuntimeException("Test failed :  modifiers doesn't contain explicit modifier "+excplicitModifier + " in "+ h.get("modifiers"));
            }
        }

        //Button 2 and 3 reports about Alt+Button2 and Meta+Button3 respectively.
        //Check these values too
        String extraModifiers = "";
        String extraModifiersButton3 = "";
        switch (button){
            //BUTTON1 with ALT reports about Alt+Button1+Button2.
            //We should fix this but I would not change this.
            case 1: {
                //Alt+Button1+Button2:
                // 1) we already handled "Alt" in excplicitModifier
                // 2) we already took "Button1" in buttonEthalon
                // 3) so "Button2" is only remained.
                // This should only happen when ALT+Button1 is pressed
                if (altIncluded){
                    extraModifiers = "Button2";
                }
                break;
            }
            case 2: {
                //Alt+Button2 report about "Alt+Button2".
                extraModifiers = "Alt";
                break;
            }
            case 3: {
                //ALT+BUTTON3 reports about "Alt+Meta+Button2+Button3"
                // This should only happen when ALT+Button3 is pressed
                extraModifiers = "Meta";
                if (altIncluded){
                    extraModifiersButton3 = "Button2";
                }
                break;
            }
        }//switch

        if (!extraModifiers.equals("")){
            if (modifierElements.contains(extraModifiers)){
                modifierElements.remove(extraModifiers);
            } else {
                //we may already removed "Alt" when filtered explicit modifiers.
                //Here is no failure in this case.
                if (!altIncluded) {
                    System.out.println("Test failed :  modifiers doesn't contain a modifier from BUTTON2 or BUTTON3 "+extraModifiers + " in "+ h.get("modifiers"));
                    throw new RuntimeException("Test failed :  modifiers doesn't contain a modifier from BUTTON2 or BUTTON3 "+extraModifiers + " in "+ h.get("modifiers"));
                }
            }
        }

        if (!extraModifiersButton3.equals("")){
            if (modifierElements.contains(extraModifiersButton3)){
                modifierElements.remove(extraModifiersButton3);
            } else {
                System.out.println("Test failed :  modifiers doesn't contain a modifier from BUTTON2 or BUTTON3 "+extraModifiersButton3 + " in "+ h.get("modifiers"));
                throw new RuntimeException("Test failed :  modifiers doesn't contain a modifier from BUTTON2 or BUTTON3 "+extraModifiersButton3 + " in "+ h.get("modifiers"));
            }
        }

        //the length of vector should now be zero
        if (!modifierElements.isEmpty()){
            System.out.println("Test failed :  there is some more elements in modifiers that shouldn't be there: "+h.get("modifiers"));
            throw new RuntimeException("Test failed :  there is some more elements in modifiers that shouldn't be there: "+h.get("modifiers"));
        }
    }

    public static void checkExtModifiersOnReleaseClick(int testModifier, HashMap h, int button){
        String ethalon = "";
        switch (testModifier){
            case SHIFT:{
                ethalon = "Shift+";
                break;
            }
            case ALT:{
                ethalon = "Alt+";
                break;
            }
            case CTRL:{
                ethalon = "Ctrl+";
                break;
            }
            default: {
                if (h.get("extModifiers") != null) {
                    System.out.println("Test failed :  Released. extModifiers != null but no modifiers keys are pressed");
                    throw new RuntimeException("Test failed :  Released. extModifiers != null but no modifiers keys are pressed");
                } else {
                    //no modifiers
                    return;
                }
            }
        }
        if (h.get("extModifiers").equals(ethalon)) {
            System.out.println("Test failed :  Released. extModifiers = "+ h.get("extModifiers") +" instead of : "+ethalon);
            throw new RuntimeException("Test failed :  Released. extModifiers = "+ h.get("extModifiers") +" instead of : "+ethalon);
        }
    }

    public static void checkReleasedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardModifiers = getStandardArray(testModifier);
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        //        int index = (button - 4)*3 + 1;
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 1)*3 + 1;
        dumpValues(button, modifiers, curStandardModifiers[index], modifiersEx, curStandardExModifiers[index]);
        if (modifiers != curStandardModifiers[index]){
            if (debug){
                System.out.println("Test failed :  Released. modifiers != modifiersStandard");
            } else {
                throw new RuntimeException("Test failed :  Released. modifiers != modifiersStandard");
            }
        }
        if (modifiersEx != curStandardExModifiers[index]){
            if (debug){
                System.out.println("Test failed :  Released. modifiersEx != curStandardExModifiers");
            } else {
                throw new RuntimeException("Test failed :  Released. modifiersEx != curStandardExModifiers");
            }
        }
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        System.out.println(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        checkExtModifiersOnReleaseClick(testModifier, paramStringElements, button);
    }

    public static void checkClickedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardModifiers = getStandardArray(testModifier);
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        //        int index = (button - 4)*3 + 2;
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 1)*3 + 2;
        dumpValues(button, modifiers, curStandardModifiers[index], modifiersEx, curStandardExModifiers[index]);
        if (modifiers != curStandardModifiers[index]){
            if (debug){
                System.out.println("Test failed :  Clicked. modifiers != modifiersStandard");
            } else {
                throw new RuntimeException("Test failed :  Clicked. modifiers != modifiersStandard");
            }
        }
        if (modifiersEx != curStandardExModifiers[index]){
            if (debug){
                System.out.println("Test failed :  Clicked. modifiersEx != curStandardExModifiers");
            } else {
                throw new RuntimeException("Test failed :  Clicked. modifiersEx != curStandardExModifiers");
            }
        }
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        checkExtModifiersOnReleaseClick(testModifier, paramStringElements, button);
    }
    /*======================================================================*/

    public static HashMap<String, String> tokenizeParamString(String param){
        HashMap <String, String> params = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(param, ",=");
        while (st.hasMoreTokens()){
            String tmp = st.nextToken();
//            System.out.println("PARSER : "+tmp);
            if (tmp.equals("button") ||
                    tmp.equals("modifiers") ||
                    tmp.equals("extModifiers")) {
                params.put(tmp, st.nextToken());
            }
        }
        return params;
    }

    public static Vector<String> tokenizeModifiers(String modifierList){
        Vector<String> modifiers = new Vector<String>();
        StringTokenizer st = new StringTokenizer(modifierList, "+");
        while (st.hasMoreTokens()){
            String tmp = st.nextToken();
            modifiers.addElement(tmp);
            System.out.println("MODIFIER PARSER : "+tmp);
        }
        return modifiers;
    }


    //test BUTTON1, 2 and 3 without any modifiers keys
    public static void  testPlainButtons(){
        System.out.println("Testing buttons without modifiers.");
        f.addMouseListener(adapterTest1);
        for (int button : mouseButtons){
            robot.mousePress(button);
            robot.delay(100);
            robot.mouseRelease(button);
        }
        robot.delay(1000);
        f.removeMouseListener(adapterTest1);
    }

    //test BUTTON1, 2 and 3 with SHIFT key
    public static void  testButtonsWithShift(){
        System.out.println("Testing buttons with SHIFT modifier.");
        f.addMouseListener(adapterTest2);

        for (int button : mouseButtons){
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.mousePress(button);
            robot.delay(100);
            robot.mouseRelease(button);
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
        robot.delay(1000);
        f.removeMouseListener(adapterTest2);
    }

    //test BUTTON1, 2 and 3 with CTRL key
    public static void  testButtonsWithControl(){
        System.out.println("Testing buttons with CONTROL modifier.");
        f.addMouseListener(adapterTest3);
        for (int button : mouseButtons){
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.mousePress(button);
            robot.delay(100);
            robot.mouseRelease(button);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }
        robot.delay(1000);
        f.removeMouseListener(adapterTest3);
    }

    //test BUTTON1, 2 and 3 with ALT key
    public static void  testButtonsWithAlt(){
        System.out.println("Testing buttons with ALT modifier.");
        f.addMouseListener(adapterTest4);
        for (int button : mouseButtons){
            robot.keyPress(KeyEvent.VK_ALT);
            robot.mousePress(button);
            robot.delay(100);
            robot.mouseRelease(button);
            robot.keyRelease(KeyEvent.VK_ALT);
        }
        robot.delay(1000);
        f.removeMouseListener(adapterTest4);
    }

    public static void initParams(String []s){
        if (s.length != 3){
            autorun = true;
            debug = false;
            testModifier = NONE;
        } else {
            autorun = Boolean.valueOf(s[0]);
            debug = Boolean.valueOf(s[1]);

            if (s[2].equals("NONE")){
                testModifier = NONE;
            }
            if (s[2].equals("SHIFT")){
                testModifier = SHIFT;
            }
            if (s[2].equals("CTRL")){
                testModifier = CTRL;
            }
            if (s[2].equals("ALT")){
                testModifier = ALT;
            }
        }
        System.out.println("Autorun : " +autorun);
        System.out.println("Debug mode : " +debug);
        System.out.println("Modifier to verify : " + testModifier);
    }

    public static void dumpValues(int button, int modifiers, int modifiersStandard, int modifiersEx, int modifiersExStandard){
        System.out.println("Button = "+button + "Modifiers = "+ modifiers + " standard = "+ modifiersStandard);
        System.out.println("                   ModifiersEx = "+ modifiersEx + " standardEx = "+ modifiersExStandard);
    }

    private static int[] getStandardExArray(int testModifier) {
        int [] curStandardExModifiers;
        switch (testModifier){
            case SHIFT:
                curStandardExModifiers = modifiersExStandardTestSHIFT;
                break;
            case CTRL:
                curStandardExModifiers = modifiersExStandardTestCTRL;
                break;
            case ALT:
                curStandardExModifiers = modifiersExStandardTestALT;
                break;
            default: //NONE by default
                curStandardExModifiers = modifiersExStandardTestNONE;
        }
        return curStandardExModifiers;
    }

    private static int[] getStandardArray(int testModifier) {
        int [] curStandardModifiers;
        switch (testModifier){
            case SHIFT:
                curStandardModifiers = modifiersStandardTestSHIFT;
                break;
            case CTRL:
                curStandardModifiers = modifiersStandardTestCTRL;
                break;
            case ALT:
                curStandardModifiers = modifiersStandardTestALT;
                break;
            default: //NONE by default
                curStandardModifiers = modifiersStandardTestNONE;
        }
        return curStandardModifiers;
    }

}


/* A class that invoke appropriate verification
 * routine with current modifier.
 */
class CheckingModifierAdapter extends MouseAdapter{
    int modifier;
    public CheckingModifierAdapter(int modifier){
        this.modifier = modifier;
    }

    public void mousePressed(MouseEvent e) {
        System.out.println("PRESSED "+e);
        if (e.getButton() > MouseEvent.BUTTON3) {
            System.out.println("Extra button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Standard.checkPressedModifiersTest(modifier, e); // e.getButton(), e.getModifiers(), e.getModifiersEx(),
        }
    }
    public void mouseReleased(MouseEvent e) {
        System.out.println("RELEASED "+e);
        if (e.getButton() > MouseEvent.BUTTON3) {
            System.out.println("Extra button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Standard.checkReleasedModifiersTest(modifier, e); // e.getButton(), e.getModifiers(), e.getModifiersEx()
        }
    }
    public void mouseClicked(MouseEvent e) {
        System.out.println("CLICKED "+e);
        if (e.getButton() > MouseEvent.BUTTON3) {
            System.out.println("Extra button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Standard.checkClickedModifiersTest(modifier, e); //e.getButton(), e.getModifiers(), e.getModifiersEx()
        }
    }
}

