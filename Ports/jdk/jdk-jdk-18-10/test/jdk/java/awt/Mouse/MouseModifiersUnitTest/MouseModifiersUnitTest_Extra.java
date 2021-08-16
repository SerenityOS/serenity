/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @summary verifies that modifiers are correct for extra buttons
  @author Andrei Dmitriev : area=awt.mouse
  @library /test/lib
  @build jdk.test.lib.Platform
  @run main MouseModifiersUnitTest_Extra
 */

import jdk.test.lib.Platform;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

// will process extra buttons only
// asking parameters from CMD: manual/automatic, modifier to test

public class MouseModifiersUnitTest_Extra extends Frame {
    static final int NONE = 0;
    static final int SHIFT = 1;
    static final int CTRL = 2;
    static final int ALT = 3;
    static CheckingModifierAdapterExtra adapterTest1;
    static CheckingModifierAdapterExtra adapterTest2;
    static CheckingModifierAdapterExtra adapterTest3;
    static CheckingModifierAdapterExtra adapterTest4;

    static boolean debug = true; //dump all errors (debug) or throw first(under jtreg) exception
    static boolean autorun = false; //use robot or manual run
    static int testModifier = NONE;

    static int [] mouseButtonDownMasks;

    //an arrays representing a modifiersEx of extra mouse buttons while using ALT/CTRL/SHIFT or none of them
    static int [] modifiersExStandard;
    static int [] modifiersExStandardSHIFT;
    static int [] modifiersExStandardCTRL;
    static int [] modifiersExStandardALT;

    private final static String SHIFT_MODIFIER = Platform.isOSX() ?
                                                "\u21e7" : "Shift";

    private final static String ALT_MODIFIER = Platform.isOSX() ?
                                                "\u2325" : "Alt";


    private final static String CTRL_MODIFIER = Platform.isOSX() ?
                                                "\u2303" : "Ctrl";


    // BUTTON1, 2, 3 press-release.
    final static int  modifiersStandard = 0; //InputEvent.BUTTON_DOWN_MASK;

    public static void checkPressedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 4)*3;
        dumpValues(button, modifiers, modifiersStandard, modifiersEx, curStandardExModifiers[index]);
        if (modifiers != modifiersStandard){
            MessageLogger.reportError("Test failed :  Pressed. modifiers != modifiersStandard");
        }

        if (modifiersEx != curStandardExModifiers[index]){
//            System.out.println(">>>>>>>>>>>>>>> Pressed. modifiersEx "+modifiersEx +" : "+!= curStandardExModifiers");
            MessageLogger.reportError("Test failed :  Pressed. modifiersEx != curStandardExModifiers. Got: "
                    + modifiersEx + " , Expected: " + curStandardExModifiers[index]);
        }

     //check event.paramString() output
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        System.out.println(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        checkExtModifiersOnPress(testModifier, paramStringElements, button);
    }

    public static void checkExtModifiersOnReleaseClick(int testModifier, HashMap<String, String> h, int button){
        String ethalon = "";
        switch (testModifier){
            case SHIFT:{
                ethalon = SHIFT_MODIFIER;
                break;
            }
            case ALT:{
                ethalon = ALT_MODIFIER;
                break;
            }
            case CTRL:{
                ethalon = CTRL_MODIFIER;
                break;
            }
        }

        if (h.get("extModifiers") == null){
            h.put("extModifiers", "");
        }

        if (!ethalon.equals(h.get("extModifiers"))) {
            MessageLogger.reportError("Test failed :  Released/Clicked. extModifiers = "
                    + h.get("extModifiers") + " instead of : " + ethalon);
        }
    }

    public static void checkExtModifiersOnPress(int testModifier, HashMap<String, String> h, int button){
        String ethalon = "";
        switch (testModifier){
            case SHIFT:{
                ethalon = SHIFT_MODIFIER + "+";
                break;
            }
            case ALT:{
                ethalon = ALT_MODIFIER + "+";
                break;
            }
            case CTRL:{
                ethalon = CTRL_MODIFIER + "+";
                break;
            }
        }
        ethalon = ethalon + "Button" +button;

        if (!h.get("extModifiers").equals(ethalon)) {
            MessageLogger.reportError("Test failed :  Pressed. extModifiers = " +h.get("extModifiers")+" instead of : "
                    + ethalon);
        }
    }

    public static void checkModifiers(int testModifier, HashMap<String, String> h, int button){
        // none of modifiers for extra button should be null
        if (h.get("modifiers") != null) {
            MessageLogger.reportError("Test failed : modifiers != null");
        }
    }

    public static void checkButton(HashMap<String, String> h, int button){
        if (h.get("button") == null) {
            MessageLogger.reportError("Test failed :  checkButton(). button is absent in paramString()");
        }
        if (Integer.parseInt(h.get("button")) != button) {
            MessageLogger.reportError("Test failed :  checkButton. button in paramString() doesn't equal to button being pressed.");
        }
    }
    public static HashMap<String, String> tokenizeParamString(String param){
        HashMap <String, String> params = new HashMap<>();
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
        Vector<String> modifiers = new Vector<>();
        StringTokenizer st = new StringTokenizer(modifierList, "+");
        while (st.hasMoreTokens()){
            String tmp = st.nextToken();
            modifiers.addElement(tmp);
            System.out.println("MODIFIER PARSER : "+tmp);
        }
        return modifiers;
    }

    public static void checkReleasedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 4)*3 + 1;
        dumpValues(button, modifiers, modifiersStandard, modifiersEx, curStandardExModifiers[index]);
        if (modifiers != modifiersStandard){
            MessageLogger.reportError("Test failed :  Released. modifiers != modifiersStandard");
        }

        if (modifiersEx != curStandardExModifiers[index]){
            MessageLogger.reportError("Test failed :  Released. modifiersEx != curStandardExModifiers. Got: "
                    + modifiersEx + " , Expected: " + curStandardExModifiers[index]);
        }

     //check event.paramString() output
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        System.out.println("paramStringElements = "+paramStringElements);
        checkExtModifiersOnReleaseClick(testModifier, paramStringElements, button);
    }

    public static void checkClickedModifiersTest(int testModifier, MouseEvent event){
        int [] curStandardExModifiers = getStandardExArray(testModifier);
        int button = event.getButton();
        int modifiers = event.getModifiers();
        int modifiersEx = event.getModifiersEx();
        int index = (button - 4)*3 + 2;
        dumpValues(button, modifiers, modifiersStandard, modifiersEx, curStandardExModifiers[index]);
        if (modifiers != modifiersStandard){
            MessageLogger.reportError("Test failed :  Clicked. modifiers != modifiersStandard");
        }

        if (modifiersEx != curStandardExModifiers[index]){
            MessageLogger.reportError("Test failed :  Clicked. modifiersEx != curStandardExModifiers. Got: "
                    + modifiersEx + " , Expected: " + curStandardExModifiers[index]);
        }

     //check event.paramString() output
        HashMap <String, String> paramStringElements = tokenizeParamString(event.paramString());
        checkButton(paramStringElements, button);
        checkModifiers(testModifier, paramStringElements, button);
        checkExtModifiersOnReleaseClick(testModifier, paramStringElements, button);
    }

    private static int[] getStandardExArray(int testModifier) {
        int [] curStandardExModifiers;
        switch (testModifier){
            case SHIFT:
                curStandardExModifiers = modifiersExStandardSHIFT;
                break;
            case CTRL:
                curStandardExModifiers = modifiersExStandardCTRL;
                break;
            case ALT:
                curStandardExModifiers = modifiersExStandardALT;
                break;
            default: //NONE by default
                curStandardExModifiers = modifiersExStandard;
        }
        return curStandardExModifiers;
    }

    static Robot robot;
    public void init() {
        this.setLayout(new BorderLayout());
        try {
            robot  = new Robot();
            robot.setAutoDelay(100);
            robot.setAutoWaitForIdle(true);
        } catch (Exception e) {
            MessageLogger.reportError("Test failed. "+e);
        }
    }//End  init()

    public void start() {
        //Get things going.  Request focus, set size, et cetera
        setSize(200,200);
        setVisible(true);
        validate();
        if (autorun) {
            testNONE();
            testSHIFT();
            testCTRL();
            testALT();
        } else {
            switch (testModifier){
                case SHIFT:
                    this.addMouseListener(adapterTest2);
                    break;
                case CTRL:
                    this.addMouseListener(adapterTest3);
                    break;
                case ALT:
                    this.addMouseListener(adapterTest4);
                    break;
                default:  //NONE by default
                    this.addMouseListener(adapterTest1);
            }
        }
    }// start()

    //000000000000000000000000000000000000000000000000000000000000000
    public void testNONE(){
        this.addMouseListener(adapterTest1);
        robot.delay(1000);
        robot.mouseMove(getLocationOnScreen().x + getWidth()/2, getLocationOnScreen().y + getHeight()/2);
        for (int i = 3; i< mouseButtonDownMasks.length; i++){
            System.out.println("testNONE() => " + mouseButtonDownMasks[i]);
            robot.mousePress(mouseButtonDownMasks[i]);
            robot.mouseRelease(mouseButtonDownMasks[i]);
        }
        robot.delay(1000);
        this.removeMouseListener(adapterTest1);
    }

    public void testSHIFT(){
        this.addMouseListener(adapterTest2);
        robot.delay(1000);
        robot.mouseMove(getLocationOnScreen().x + getWidth()/2, getLocationOnScreen().y + getHeight()/2);
        for (int i = 3; i< mouseButtonDownMasks.length; i++){
            robot.keyPress(KeyEvent.VK_SHIFT);
            System.out.println("testSHIFT() => " + mouseButtonDownMasks[i]);
            robot.mousePress(mouseButtonDownMasks[i]);
            robot.mouseRelease(mouseButtonDownMasks[i]);
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
        robot.delay(1000);
        this.removeMouseListener(adapterTest2);
    }

    public void testCTRL(){
        this.addMouseListener(adapterTest3);
        robot.delay(1000);
        robot.mouseMove(getLocationOnScreen().x + getWidth()/2, getLocationOnScreen().y + getHeight()/2);
        for (int i = 3; i< mouseButtonDownMasks.length; i++){
            robot.keyPress(KeyEvent.VK_CONTROL);
            System.out.println("testCTRL() => " + mouseButtonDownMasks[i]);
            robot.mousePress(mouseButtonDownMasks[i]);
            robot.mouseRelease(mouseButtonDownMasks[i]);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }
        robot.delay(1000);
        this.removeMouseListener(adapterTest3);
    }

    public void testALT(){
        this.addMouseListener(adapterTest4);
        robot.delay(1000);
        robot.mouseMove(getLocationOnScreen().x + getWidth()/2, getLocationOnScreen().y + getHeight()/2);
        for (int i = 3; i< mouseButtonDownMasks.length; i++){
            robot.keyPress(KeyEvent.VK_ALT);
            System.out.println("testALT() => " + mouseButtonDownMasks[i]);
            robot.mousePress(mouseButtonDownMasks[i]);
            robot.mouseRelease(mouseButtonDownMasks[i]);
            robot.keyRelease(KeyEvent.VK_ALT);
        }
        robot.delay(1000);
        this.removeMouseListener(adapterTest4);
    }

    //**************************************************************************************************
    public static void dumpValues(int button, int modifiers, int modifiersStandard, int modifiersEx, int modifiersExStandard){
        System.out.println("Button = "+button + "Modifiers = "+ modifiers + "standard = "+ modifiersStandard);
        System.out.println("Button = "+button + "ModifiersEx = "+ modifiersEx + "standardEx = "+ modifiersExStandard);
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
        MessageLogger.setDebug(debug);
        System.out.println("Autorun : " +autorun);
        System.out.println("Debug mode : " +debug);
        System.out.println("Modifier to verify : " + testModifier);
    }

    public static void initAdapters(){
        adapterTest1 = new CheckingModifierAdapterExtra(NONE);
        adapterTest2 = new CheckingModifierAdapterExtra(SHIFT);
        adapterTest3 = new CheckingModifierAdapterExtra(CTRL);
        adapterTest4 = new CheckingModifierAdapterExtra(ALT);
    }

    public static void initVars(){
        //Init the array of the mouse button masks. It will be used for generating mouse events.
        mouseButtonDownMasks = new int [MouseInfo.getNumberOfButtons()];
        for (int i = 0; i < mouseButtonDownMasks.length; i++){
            mouseButtonDownMasks[i] = InputEvent.getMaskForButton(i+1);
            System.out.println("MouseArray [i] == "+mouseButtonDownMasks[i]);
        }

        // So we need to get the number of extra buttons on the mouse:  "MouseInfo.getNumberOfButtons() - 3"
        // and multyply on 3 because each button will generate three events : PRESS, RELEASE and CLICK.
        int [] tmp = new int [(MouseInfo.getNumberOfButtons()-3)*3];

        //Fill array of expected results for the case when mouse buttons are only used (no-modifier keys)
        Arrays.fill(tmp, 0);
        for (int i = 0, j = 3; i < tmp.length; i = i + 3, j++){
            tmp[i] = mouseButtonDownMasks[j];
        }
        modifiersExStandard = Arrays.copyOf(tmp, tmp.length);

        //Fill array of expected results for the case when mouse buttons are only used with SHIFT modifier key
        Arrays.fill(tmp, InputEvent.SHIFT_DOWN_MASK);
        for (int i = 0, j = 3; i < tmp.length; i = i + 3, j++){
            System.out.println("modifiersExStandardSHIFT FILLING : " + tmp[i] + " + " + mouseButtonDownMasks[j]);
            tmp[i] = tmp[i] | mouseButtonDownMasks[j];
        }
        modifiersExStandardSHIFT = Arrays.copyOf(tmp, tmp.length);

        //Fill array of expected results for the case when mouse buttons are only used with CTRL modifier key
        Arrays.fill(tmp, InputEvent.CTRL_DOWN_MASK);
        for (int i = 0, j = 3; i < tmp.length; i = i + 3, j++){
            System.out.println("modifiersExStandardCTRL FILLING : " + tmp[i] + " + " + mouseButtonDownMasks[j]);
            tmp[i] = tmp[i] | mouseButtonDownMasks[j];
        }
        modifiersExStandardCTRL = Arrays.copyOf(tmp, tmp.length);

        //Fill array of expected results for the case when mouse buttons are only used with ALT modifier key
        Arrays.fill(tmp, InputEvent.ALT_DOWN_MASK);
        for (int i = 0, j = 3; i < tmp.length; i = i + 3, j++){
            System.out.println("modifiersExStandardALT FILLING : " + tmp[i] + " + " + mouseButtonDownMasks[j]);
            tmp[i] = tmp[i] | mouseButtonDownMasks[j];
        }
        modifiersExStandardALT = Arrays.copyOf(tmp, tmp.length);
    }

    public static void main(String []s){
        if (MouseInfo.getNumberOfButtons() < 4){
            System.out.println("There are less than 4 buttons on the mouse. The test may not be accomplished. Skipping.");
            return;
        }
        initVars();
        MouseModifiersUnitTest_Extra frame = new MouseModifiersUnitTest_Extra();
        frame.initParams(s);
        frame.init();
        initAdapters();
        frame.start();
    }

}// class

/* A class that invoke appropriate verification
 * routine with current modifier.
 */
class CheckingModifierAdapterExtra extends MouseAdapter{
    int modifier;
    public CheckingModifierAdapterExtra(int modifier){
        this.modifier = modifier;
    }

    public void mousePressed(MouseEvent e) {
        System.out.println("PRESSED "+e);
        if (e.getButton() <= MouseEvent.BUTTON3) {
            System.out.println("Standard button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Extra.checkPressedModifiersTest(modifier, e);
        }
    }
    public void mouseReleased(MouseEvent e) {
        System.out.println("RELEASED "+e);
        if (e.getButton() <= MouseEvent.BUTTON3) {
            System.out.println("Standard button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Extra.checkReleasedModifiersTest(modifier, e);
        }
    }
    public void mouseClicked(MouseEvent e) {
        System.out.println("CLICKED "+e);
        if (e.getButton() <= MouseEvent.BUTTON3) {
            System.out.println("Standard button affected. Skip.");
        } else {
            MouseModifiersUnitTest_Extra.checkClickedModifiersTest(modifier, e);
        }
    }
}
//Utility class that could report a message depending on current purpose of the test run
class MessageLogger{
    private static boolean debug;

    public static void setDebug(boolean d){
        debug = d;
        log("Switch to "+ ((debug)?"debug":"trial") +" mode");
    }

    public static void log(String message){
        System.out.println(message);
    }

    public static void reportError(String message){
        if (debug){
            System.out.println(message);
        } else {
            throw new RuntimeException(message);
        }
    }
}
