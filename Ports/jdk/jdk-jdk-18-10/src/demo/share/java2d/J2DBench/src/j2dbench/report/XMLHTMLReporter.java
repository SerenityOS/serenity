/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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


/**
 * XMLHTMLReporter.java
 *
 * Generates HTML reports from XML results
 *
 * @author Rakesh Menon
 */

package j2dbench.report;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class XMLHTMLReporter {

    /**
     * Flag to indicate - Generate new report or append to existing report
     */
    private static final int HTMLGEN_FILE_NEW = 1;
    private static final int HTMLGEN_FILE_UPDATE = 2;

    /**
     * Path to results directory where all results are stored
     */
    public static String resultsDir = ".";

    /**
     * Holds the groups and corresponding group-display-names
     */
    public static List groups = new ArrayList();
    public static Map groupNames = new HashMap();

    /**
     * Level at which tests are grouped to be displayed in summary
     */
    public static int LEVEL = 2;

    /**
     * Color -> Better, Same, Worse
     */
    private static final String[] color = {"#99FF99", "#CCFFFF", "#FFCC00"};

    /**
     * String for holding base-build and target-build version
     */
    private static String baseBuild = "";
    private static String targetBuild = "";

    private static final DecimalFormat decimalFormat =
        new DecimalFormat("0.##");
    private static final SimpleDateFormat dateFormat =
        new SimpleDateFormat("EEE, MMM d, yyyy G 'at' HH:mm:ss z");

    public static void setGroupLevel(int level) {
        XMLHTMLReporter.LEVEL = level;
    }

    /**
     * Add Test Group to the list
     */
    private static void addGroup(String testName) {

        String[] testNameSplit = testName.replace('.', '_').split("_");
        String group = testNameSplit[0];
        for(int i=1; i<LEVEL; i++) {
            group = group + "." + testNameSplit[i];
        }

        if( ! groups.contains(group)) {
            groups.add(group);
            groupNames.put(group, getDisplayGroupName(group));
        }
    }

    /**
     * Generate a Display Name for this group
     */
    private static String getDisplayGroupName(String group) {

        String[] groupSplit = group.replace('.', '_').split("_");

        StringBuffer groupName = new StringBuffer();
        String tempName = null;

        for(int i=0; i<groupSplit.length; i++) {
            tempName = groupSplit[i].substring(0, 1).toUpperCase() +
                groupSplit[i].substring(1);
            if(i == 0) {
                groupName.append(tempName);
            } else {
                groupName.append(" " + tempName);
            }
        }

        return groupName.toString();
    }

    /**
     * Get the group to which this testcase belongs
     */
    private static String getGroup(String testName) {

        String[] testNameSplit = testName.replace('.', '_').split("_");
        String group = testNameSplit[0];
        for(int i=1; i<LEVEL; i++) {
            group = group + "." + testNameSplit[i];
        }

        return group;
    }

    /**
     * Opens a File and returns a PrintWriter instance based on new/update
     * option specified in argument.
     */
    private static PrintWriter openFile(String name, int nSwitch) {

        FileOutputStream file = null;
        OutputStreamWriter writer = null;

        try {
            switch (nSwitch) {
                case 1: // HTMLGEN_FILE_NEW
                    file = new FileOutputStream(name, false);
                    break;
                case 2: // HTMLGEN_FILE_UPDATE
                    file = new FileOutputStream(name, true);
                    break;
            }
            writer = new OutputStreamWriter(file);
        } catch (IOException ee) {
            System.out.println("Error opening file: " + ee);
            System.exit(1);
        }

        return new PrintWriter(new BufferedWriter(writer));
    }

    /**
     * Generate an HTML report based on the XML results file passed -
     * J2DBench_Results.html
     */
    public static void generateReport(String resultsDir, String xmlFileName) {

        try {

            String strhtml = null;
            String strstr = null;
            String[] tempstr2 = new String[2];
            String[] tempstr = new String[2];

            J2DAnalyzer.readResults(xmlFileName);
            J2DAnalyzer.SingleResultSetHolder srsh =
                (J2DAnalyzer.SingleResultSetHolder)
                J2DAnalyzer.results.elementAt(0);
            Enumeration enum_ = srsh.getKeyEnumeration();
            Vector keyvector = new Vector();
            while (enum_.hasMoreElements()) {
                keyvector.add(enum_.nextElement());
            }
            String[] keys = new String[keyvector.size()];
            keyvector.copyInto(keys);
            J2DAnalyzer.sort(keys);

            File reportFile = new File(resultsDir, "J2DBench_Results.html");
            PrintWriter writer =
                openFile(reportFile.getAbsolutePath(), HTMLGEN_FILE_NEW);

            writer.println("<html><body bgcolor=\"#ffffff\"><hr size=\"1\">");
            writer.println("<center><h2>J2DBench2 - Report</h2>");
            writer.println("</center><hr size=\"1\"><br>");
            writer.println("<table cols=\"2\" cellspacing=\"2\" " +
                           "cellpadding=\"5\" " +
                           "border=\"0\" width=\"80%\">");
            writer.println("<tr><td bgcolor=\"#CCCCFF\" colspan=\"2\">" +
                           "<b>Build Details</b></td></tr>");
            writer.println("<tr>");
            writer.println("<td bgcolor=\"#f0f0f0\">Description</td>");
            writer.println("<td>" + srsh.getDescription() + "</td>");
            writer.println("</tr>");
            writer.println("<tr><td bgcolor=\"#f0f0f0\">From Date</td>");
            writer.println("<td>" +
                           dateFormat.format(new Date(srsh.getStartTime())) +
                           "</td></tr>");
            writer.println("<tr><td bgcolor=\"#f0f0f0\">To Date</td>");
            writer.println("<td>" +
                           dateFormat.format(new Date(srsh.getEndTime())) +
                           "</td></tr>");
            writer.flush();

            //System Properties
            writer.println("<tr><td bgcolor=\"#CCCCFF\"><b>System Property</b>"+
                           "</td><td bgcolor=\"#CCCCFF\">" +
                           "<b>Value</b></td></tr>");
            String key = null;
            String value = null;
            Map sysProps = srsh.getProperties();
            Iterator iter = sysProps.keySet().iterator();
            while(iter.hasNext()) {
                key = iter.next().toString();
                value = sysProps.get(key).toString();
                writer.println("<tr><td bgcolor=\"#f0f0f0\">" +
                               key + "</td><td>" + value + "&nbsp;</td></tr>");
            }
            writer.flush();

            writer.println("</table>");
            writer.println("<br>");
            writer.println("<hr size=\"1\">");
            writer.println("<br>");

            writer.println("<table cellspacing=\"0\" " +
                           "cellpadding=\"3\" border=\"1\" width=\"80%\">");
            writer.println("<tr>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Num Reps</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Num Units</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Name</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Options</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Score</b></td>");
            writer.println("</tr>");
            writer.flush();

            for (int k = 0; k < keys.length; k++) {

                J2DAnalyzer.ResultHolder testResult =
                    srsh.getResultByKey(keys[k]);

                writer.println("<tr>");
                writer.println("<td>" + testResult.getReps() + "</td>");
                writer.println("<td>" + testResult.getUnits() + "</td>");
                writer.println("<td>" + testResult.getName() + "</td>");
                writer.println("<td valign=\"center\"><ul>");
                Map map = testResult.getOptions();
                iter = map.keySet().iterator();
                while(iter.hasNext()) {
                    key = iter.next().toString();
                    value = map.get(key).toString();
                    writer.println("<li>" + key + " = " + value + "</li>");
                }
                writer.println("</ul></td>");
                writer.println("<td valign=\"center\">" +
                               decimalFormat.format(testResult.getScore()) +
                               "</td>");
                writer.println("</tr>");
            }
            writer.flush();

            writer.println("</table>");

            writer.println("<br><hr WIDTH=\"100%\" size=\"1\">");
            writer.println("</p><hr WIDTH=\"100%\" size=\"1\"></body></html>");

            writer.flush();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate the reports from the base & target result XML
     */
    public static void generateComparisonReport(String resultsDir,
                                                String baseXMLFileName,
                                                String targetXMLFileName) {

        XMLHTMLReporter.resultsDir = resultsDir;

        //Get Base XML File ResultSetHolder
        J2DAnalyzer.readResults(baseXMLFileName);
        J2DAnalyzer.SingleResultSetHolder baseSRSH =
            (J2DAnalyzer.SingleResultSetHolder) J2DAnalyzer.results.elementAt(0);
        Enumeration baseEnum_ = baseSRSH.getKeyEnumeration();
        Vector baseKeyvector = new Vector();
        while (baseEnum_.hasMoreElements()) {
            baseKeyvector.add(baseEnum_.nextElement());
        }
        String[] baseKeys = new String[baseKeyvector.size()];
        baseKeyvector.copyInto(baseKeys);
        J2DAnalyzer.sort(baseKeys);

        //Get Target XML File ResultSetHolder
        J2DAnalyzer.readResults(targetXMLFileName);
        J2DAnalyzer.SingleResultSetHolder targetSRSH =
            (J2DAnalyzer.SingleResultSetHolder)
                J2DAnalyzer.results.elementAt(1);
        Enumeration targetEnum_ = baseSRSH.getKeyEnumeration();
        Vector targetKeyvector = new Vector();
        while (targetEnum_.hasMoreElements()) {
            targetKeyvector.add(targetEnum_.nextElement());
        }
        String[] targetKeys = new String[targetKeyvector.size()];
        targetKeyvector.copyInto(targetKeys);
        J2DAnalyzer.sort(targetKeys);

        baseBuild = (String)baseSRSH.getProperties().get("java.vm.version");
        targetBuild = (String)targetSRSH.getProperties().get("java.vm.version");
        generateSysPropsReport(targetSRSH);

        File reportFile = new File(resultsDir, "J2DBench_Complete_Report.html");
        PrintWriter writer = openFile(reportFile.getAbsolutePath(),
                                      HTMLGEN_FILE_NEW);

        String header = getHeader(baseSRSH, targetSRSH,
                                  "J2DBench - Complete Report",
                                  "System_Properties.html");
        writer.println(header);
        writer.flush();

        StringBuffer startTags = new StringBuffer();
        startTags.append("<tr>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>Num Reps</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>Num Units</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>Name</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>Options</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>" + baseBuild + " Score</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\"><b>" +
                         targetBuild + " Score</b></td>");
        startTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                         "<b>% Speedup</b></td>");
        startTags.append("</tr>");

        StringBuffer worseResultTags = new StringBuffer(startTags.toString());
        StringBuffer sameResultTags = new StringBuffer(startTags.toString());
        StringBuffer betterResultTags = new StringBuffer(startTags.toString());

        Map consolBaseRes = new HashMap();
        Map consolTargetResult = new HashMap();

        Map testCaseBaseResult = new HashMap();
        Map testCaseResultCount = new HashMap();
        Map testCaseTargetResult = new HashMap();

        for (int k = 0; k < targetKeys.length; k++) {

            J2DAnalyzer.ResultHolder baseTCR =
                baseSRSH.getResultByKey(targetKeys[k]);
            J2DAnalyzer.ResultHolder targetTCR =
                targetSRSH.getResultByKey(targetKeys[k]);

            Object curTestCountObj = testCaseResultCount.get(baseTCR.getName());
            int curTestCount = 0;
            if(curTestCountObj != null) {
                curTestCount = ((Integer) curTestCountObj).intValue();
            }
            curTestCount++;
            testCaseBaseResult.put(baseTCR.getName() + "_" +
                                   (curTestCount - 1), baseTCR);
            testCaseTargetResult.put(targetTCR.getName() + "_" +
                                     (curTestCount - 1), targetTCR);
            testCaseResultCount.put(baseTCR.getName(),
                                    new Integer(curTestCount));

            /******************************************************************
             * Add the Test to Group List
             ******************************************************************/
            addGroup(baseTCR.getName());

            double baseScore = baseTCR.getScore();
            double targetScore = targetTCR.getScore();

            int selColorIndex = selectColor(baseScore, targetScore);

            StringBuffer tagBuffer = new StringBuffer();

            tagBuffer.append("<tr bgcolor=\""+ color[selColorIndex] + "\">");
            tagBuffer.append("<td align=\"center\">" + baseTCR.getScore() +
                             "</td>");
            tagBuffer.append("<td align=\"center\">" + baseTCR.getUnits() +
                             "</td>");
            tagBuffer.append("<td align=\"center\">" + baseTCR.getName() +
                             "</td>");
            tagBuffer.append("<td valign=\"center\"><ul>");
            Map map = baseTCR.getOptions();
            Iterator iter = map.keySet().iterator();
            while(iter.hasNext()) {
                Object key = iter.next().toString();
                Object value = map.get(key).toString();
                tagBuffer.append("<li>" + key + " = " + value + "</li>");
            }
            tagBuffer.append("</ul></td>");
            tagBuffer.append("<td valign=\"center\" align=\"center\">" +
                             decimalFormat.format(baseTCR.getScore()) +
                             "</td>");
            tagBuffer.append("<td valign=\"center\" align=\"center\">" +
                             decimalFormat.format(targetTCR.getScore()) +
                             "</td>");
            tagBuffer.append("<td valign=\"center\" align=\"center\">" +
                             decimalFormat.format(
                                 calculateSpeedupPercentage(
                                     baseTCR.getScore(),
                                     targetTCR.getScore())) + "</td>");
            tagBuffer.append("</tr>");

            switch(selColorIndex) {
                case 0:
                    betterResultTags.append(tagBuffer.toString());
                    break;
                case 1:
                    sameResultTags.append(tagBuffer.toString());
                    break;
                case 2:
                    worseResultTags.append(tagBuffer.toString());
                    break;
            }

            Object curTotalScoreObj = consolBaseRes.get(baseTCR.getName());
            double curTotalScore = 0;
            if(curTotalScoreObj != null) {
                curTotalScore = ((Double) curTotalScoreObj).doubleValue();
            }
            curTotalScore = curTotalScore + baseTCR.getScore();
            consolBaseRes.put(baseTCR.getName(), new Double(curTotalScore));

            curTotalScoreObj = consolTargetResult.get(targetTCR.getName());
            curTotalScore = 0;
            if(curTotalScoreObj != null) {
                curTotalScore = ((Double) curTotalScoreObj).doubleValue();
            }
            curTotalScore = curTotalScore + targetTCR.getScore();
            consolTargetResult.put(targetTCR.getName(),
                                   new Double(curTotalScore));
        }

        writer.println("<br><hr WIDTH=\"100%\" size=\"1\">");
        writer.println("<A NAME=\"results\"></A><H3>Results:</H3>");

        writer.println("<table cellspacing=\"0\" " +
                         "cellpadding=\"3\" border=\"1\" width=\"80%\">");

        writer.println("<tr><td colspan=\"7\" bgcolor=\"#f0f0f0\">" +
                       "<font size=\"+1\">Tests which run BETTER on target" +
                       "</font></td></tr>");
        writer.println(betterResultTags.toString());
        writer.flush();

        writer.println("<tr><td colspan=\"7\">&nbsp;<br>&nbsp;</td></tr>");
        writer.println("<tr><td colspan=\"7\" bgcolor=\"#f0f0f0\">" +
                       "<font size=\"+1\">Tests which run " +
                       "SAME on target</font></td></tr>");
        writer.println(sameResultTags.toString());
        writer.flush();

        writer.println("<tr><td colspan=\"7\">&nbsp;<br>&nbsp;</td></tr>");
        writer.println("<tr><td colspan=\"7\" bgcolor=\"#f0f0f0\">" +
                       "<font size=\"+1\">Tests which run WORSE on target" +
                       "</font></td></tr>");
        writer.println(worseResultTags.toString());
        writer.flush();

        writer.println("</table>");

        writer.println(getFooter());
        writer.flush();

        writer.close();

        generateTestCaseSummaryReport(baseSRSH, targetSRSH,
                                      consolBaseRes, consolTargetResult,
                                      testCaseBaseResult,
                                      testCaseResultCount,
                                      testCaseTargetResult);

        generateGroupSummaryReport(baseSRSH, targetSRSH,
                                   consolBaseRes, consolTargetResult,
                                   testCaseBaseResult,
                                   testCaseResultCount, testCaseTargetResult);
    }

    /**
     * Generate Group-Summary report - Summary_Report.html
     */
    private static void generateGroupSummaryReport(
        J2DAnalyzer.SingleResultSetHolder baseSRSH,
        J2DAnalyzer.SingleResultSetHolder targetSRSH,
        Map consolBaseResult,
        Map consolTargetResult,
        Map testCaseBaseResult,
        Map testCaseResultCount,
        Map testCaseTargetResult) {

        File groupSummaryReportFile =
            new File(resultsDir, "Summary_Report.html");
        PrintWriter writer =
            openFile(groupSummaryReportFile.getAbsolutePath(),
                     HTMLGEN_FILE_NEW);

        String header = getHeader(baseSRSH, targetSRSH,
                                  "J2DBench - Summary Report",
                                  "System_Properties.html");
        writer.println(header);
        writer.flush();

        writer.println("<br><hr size=\"1\">");

        Map baseValuesMap = new HashMap();
        Map targetValuesMap = new HashMap();

        String tempGroup = null;
        for(int i=0; i<groups.size(); i++) {
            tempGroup = groups.get(i).toString();
            baseValuesMap.put(tempGroup, new Double(0));
            targetValuesMap.put(tempGroup, new Double(0));
        }


        Object key = null;
        double baseValue = 0, targetValue = 0;
        Iterator resultsIter = consolBaseResult.keySet().iterator();

        while(resultsIter.hasNext()) {

            key = resultsIter.next();

            baseValue = ((Double) consolBaseResult.get(key)).doubleValue();
            targetValue = ((Double) consolTargetResult.get(key)).doubleValue();

            tempGroup = getGroup(key.toString());

            Object curTotalScoreObj = null;
            double curTotalScore = 0;

            curTotalScoreObj = baseValuesMap.get(tempGroup);
            if(curTotalScoreObj != null) {
                curTotalScore = ((Double) curTotalScoreObj).doubleValue();
            }
            curTotalScore = curTotalScore + baseValue;
            baseValuesMap.put(tempGroup, new Double(curTotalScore));

            curTotalScore = 0;
            curTotalScoreObj = targetValuesMap.get(tempGroup);
            if(curTotalScoreObj != null) {
                curTotalScore = ((Double) curTotalScoreObj).doubleValue();
            }
            curTotalScore = curTotalScore + targetValue;
            targetValuesMap.put(tempGroup, new Double(curTotalScore));
        }

        writer.println("<A NAME=\"results_summary\"></A>" +
                       "<H3>Results Summary:</H3>");
        writer.println("<table cols=\"4\" cellspacing=\"0\" " +
                       "cellpadding=\"3\" border=\"1\" width=\"80%\">");
        writer.println("<TR BGCOLOR=\"#CCCCFF\">");
        writer.println("<TD><B>Testcase</B></TD>");
        writer.println("<TD align=\"center\"><B>Score for " + baseBuild +
                       "</B></TD>");
        writer.println("<TD align=\"center\"><B>Score for " + targetBuild +
                       "</B></TD>");
        writer.println("<TD align=\"center\"><B>% Speedup</TD>");
        writer.println("</TR>");

        StringBuffer betterResultTags = new StringBuffer();
        StringBuffer sameResultTags = new StringBuffer();
        StringBuffer worseResultTags = new StringBuffer();

        resultsIter = baseValuesMap.keySet().iterator();

        double speedup = 0;

        while(resultsIter.hasNext()) {

            key = resultsIter.next();

            baseValue = ((Double) baseValuesMap.get(key)).doubleValue();
            targetValue = ((Double) targetValuesMap.get(key)).doubleValue();
            speedup = calculateSpeedupPercentage(baseValue, targetValue);

            int selColorIndex = selectColor(baseValue, targetValue);

            String tcFileName = key.toString().replace('.', '_');
            tcFileName = tcFileName.toLowerCase() + ".html";

            switch(selColorIndex) {
                case 0:
                    betterResultTags.append("<tr bgcolor=\""+
                                            color[selColorIndex] + "\">");
                    betterResultTags.append("<td><a href=" +
                        "\"Testcase_Summary_Report.html#status_" + key +
                                            "\">" + groupNames.get(key) +
                                            "</a></td>");
                    betterResultTags.append("<td align=\"center\">" +
                                            decimalFormat.format(baseValue) +
                                            "</td>");
                    betterResultTags.append("<td align=\"center\">" +
                                            decimalFormat.format(targetValue) +
                                            "</td>");
                    betterResultTags.append("<td align=\"center\">" +
                                            decimalFormat.format(speedup) +
                                            "</td>");
                    betterResultTags.append("</tr>");
                    break;
                case 1:
                    sameResultTags.append("<tr bgcolor=\""+
                                          color[selColorIndex] + "\">");
                    sameResultTags.append("<td>" +
                        "<a href=\"Testcase_Summary_Report.html#status_" + key +
                                          "\">" + groupNames.get(key) +
                                          "</a></td>");
                    sameResultTags.append("<td align=\"center\">" +
                                          decimalFormat.format(baseValue) +
                                          "</td>");
                    sameResultTags.append("<td align=\"center\">" +
                                          decimalFormat.format(targetValue) +
                                          "</td>");
                    sameResultTags.append("<td align=\"center\">" +
                                          decimalFormat.format(speedup) +
                                          "</td>");
                    sameResultTags.append("</tr>");
                    break;
                case 2:
                    worseResultTags.append("<tr bgcolor=\""+
                                           color[selColorIndex] + "\">");
                    worseResultTags.append("<td>" +
                        "<a href=\"Testcase_Summary_Report.html#status_" + key +
                                           "\">" + groupNames.get(key) +
                                           "</a></td>");
                    worseResultTags.append("<td align=\"center\">" +
                                           decimalFormat.format(baseValue) +
                                           "</td>");
                    worseResultTags.append("<td align=\"center\">" +
                                           decimalFormat.format(targetValue) +
                                           "</td>");
                    worseResultTags.append("<td align=\"center\">" +
                                           decimalFormat.format(speedup) +
                                           "</td>");
                    worseResultTags.append("</tr>");
                    break;
            }
        }

        writer.println(betterResultTags.toString());
        writer.flush();

        writer.println(sameResultTags.toString());
        writer.flush();

        writer.println(worseResultTags.toString());
        writer.flush();

        writer.println("</table>");

        writer.println(getFooter());
        writer.flush();
        writer.close();
    }

    /**
     * Generate Testcase Summary Report - Testcase_Summary_Report.html
     */
    private static void generateTestCaseSummaryReport(
        J2DAnalyzer.SingleResultSetHolder baseSRSH,
        J2DAnalyzer.SingleResultSetHolder targetSRSH,
        Map consolBaseResult,
        Map consolTargetResult,
        Map testCaseBaseResult,
        Map testCaseResultCount,
        Map testCaseTargetResult) {

        File tcSummaryReportFile =
            new File(resultsDir, "Testcase_Summary_Report.html");
        PrintWriter writer =
            openFile(tcSummaryReportFile.getAbsolutePath(), HTMLGEN_FILE_NEW);

        String header = getHeader(baseSRSH, targetSRSH,
                                  "J2DBench - Testcase Summary Report",
                                  "System_Properties.html");
        writer.println(header);
        writer.flush();

        StringBuffer testResultsStartBuffer = new StringBuffer();
        testResultsStartBuffer.append("<TR BGCOLOR=\"#CCCCFF\">");
        testResultsStartBuffer.append("<TD><B>Testcase</B></TD>");
        testResultsStartBuffer.append("<TD align=\"center\"><B>Score for " +
                                      baseBuild + "</B></TD>");
        testResultsStartBuffer.append("<TD align=\"center\"><B>Score for " +
                                     targetBuild + "</B></TD>");
        testResultsStartBuffer.append("<TD align=\"center\"><B>% Speedup</TD>");
        testResultsStartBuffer.append("</TR>");

        StringBuffer testResultsScoreBuffer = new StringBuffer();
        testResultsScoreBuffer.append("<table cols=\"4\" cellspacing=\"0\" " +
                                      "cellpadding=\"3\" border=\"1\" " +
                                      "width=\"80%\">");

        StringBuffer betterResultTags = new StringBuffer();
        StringBuffer sameResultTags = new StringBuffer();
        StringBuffer worseResultTags = new StringBuffer();

        Double baseValue = null, targetValue = null;

        String curGroupName = null;
        String curTestName = null;

        Object[] groupNameArray = groups.toArray();
        Arrays.sort(groupNameArray);

        Object[] testCaseList = consolBaseResult.keySet().toArray();
        Arrays.sort(testCaseList);

        writer.println("<br><hr size=\"1\"><br>");
        writer.println("<A NAME=\"status\"></A><H3>Status:</H3>");

        writer.println("<table cellspacing=\"0\" " +
                       "cellpadding=\"3\" border=\"1\" width=\"80%\">");

        for(int j=0; j<groupNameArray.length; j++) {

            if(j != 0) {
                testResultsScoreBuffer.append("<tr><td colspan=\"4\">&nbsp;" +
                                              "<br>&nbsp;</td></tr>");
                writer.println("<tr><td colspan=\"5\">&nbsp;<br>&nbsp;" +
                               "</td></tr>");
            }

            curGroupName = groupNameArray[j].toString();

            writer.println("<tr><td colspan=\"5\" valign=\"center\" " +
                           "bgcolor=\"#f0f0f0\">" +
                           "<A NAME=\"status_" + curGroupName + "\"></A>" +
                           "<font size=\"+1\">Status - " +
                           groupNames.get(curGroupName) + "</font></td></tr>");
            writer.println("<tr>");
            writer.println("<td bgcolor=\"#CCCCFF\"><b>Tests " +
                           "Performance</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>BETTER (Num / %)</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>SAME (Num / %)</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>WORSE (Num / %)</b></td>");
            writer.println("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Total</b></td>");
            writer.println("</tr>");
            writer.flush();

            testResultsScoreBuffer.append("<tr><td colspan=\"4\" " +
                                          "valign=\"center\" " +
                                          "bgcolor=\"#f0f0f0\">" +
                                          "<A NAME=\"test_result_" +
                                          curGroupName +
                                          "\"></A><font size=\"+1\">" +
                                          "Test Results - " +
                                          groupNames.get(curGroupName) +
                                          "</font></td></tr>");
            testResultsScoreBuffer.append(testResultsStartBuffer);

            String[] tableTags = null;

            for(int i=0; i<testCaseList.length; i++) {

                curTestName = testCaseList[i].toString();

                if(curTestName.startsWith(curGroupName)) {

                    tableTags = generateTestCaseReport(
                        curGroupName, curTestName, baseSRSH, targetSRSH,
                        testCaseResultCount, testCaseBaseResult,
                        testCaseTargetResult);

                    writer.println(tableTags[0]);
                    writer.flush();

                    testResultsScoreBuffer.append(tableTags[1]);
                }
            }
        }

        testResultsScoreBuffer.append("</table>");
        writer.println("</table>");

        writer.println("<br><hr size=\"1\"><br>");
        writer.println("<A NAME=\"test_results\"></A><H3>Test Results:</H3>");
        writer.println(testResultsScoreBuffer.toString());
        writer.flush();

        writer.println(getFooter());
        writer.flush();

        writer.close();
    }

    /**
     *|----------|------------------------|--------------------------|-----------|
     *| Testcase | Score for <base build> | Score for <target build> | % Speedup |
     *|----------|------------------------|--------------------------|-----------|
     *
     */
    private static String getTestResultsTableForSummary(String testName,
                                                        double baseScore,
                                                        double targetScore)
    {

        double totalScore = baseScore + targetScore;

        String fileName = testName.replace('.', '_');
        fileName = fileName.toLowerCase() + ".html";

        int selColorIndex = selectColor(baseScore, targetScore);

        StringBuffer buffer = new StringBuffer();

        buffer.append("<TR BGCOLOR=\"" + color[selColorIndex] + "\">");
        buffer.append("<TD><P><A HREF=\"testcases/" + fileName +
                      "\">" + testName + "</A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"testcases/" +
                      fileName +
                      "\"><B>" + decimalFormat.format(baseScore) +
                      "</B></A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"testcases/" +
                      fileName + "\"><B>" + decimalFormat.format(targetScore) +
                      "</B></A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"testcases/" +
                      fileName + "\"><B>" +
                      decimalFormat.format(calculateSpeedupPercentage(
                          baseScore,
                          targetScore)) +
                      "</B></A></P></TD>");
        buffer.append("</TR>");

        return buffer.toString();
    }

    /**
     *|-------------------|-------------------|-----------------|-------------------|--------|
     *| Tests Performance | BETTER  (Num / %) | SAME  (Num / %) | WORSE  ( Num / %) | Total  |
     *|-------------------|-------------------|-----------------|-------------------|--------|
     *
     */
    private static String getStatusTableForSummary(
        String curGroupName, String testName, int nBetter,
        int nSame, int nWorse)
    {

        String fileName = testName.replace('.', '_');
        fileName = fileName.toLowerCase() + ".html";

        int totalTests = nBetter + nSame + nWorse;

        StringBuffer buffer = new StringBuffer();

        buffer.append("<TR>");
        buffer.append("<TD><P><A HREF=\"#test_result_" + curGroupName +
                      "\">" + testName + "</A></P></TD>");
        buffer.append("<TD BGCOLOR=\"#99FF99\" align=\"center\"><P>" +
                      "<A HREF=\"#test_result_" + curGroupName +
                      "\"><B>" + nBetter + "</A></B>&nbsp;&nbsp;&nbsp;&nbsp;(" +
                      (nBetter * 100)/totalTests + "%)</P></TD>");
        buffer.append("<TD BGCOLOR=\"#CCFFFF\" align=\"center\"><P>" +
                      "<A HREF=\"#test_result_" + curGroupName +
                      "\"><B>" + nSame + "</A></B>&nbsp;&nbsp;&nbsp;&nbsp;(" +
                      (nSame * 100)/totalTests + "%)</P></TD>");
        buffer.append("<TD BGCOLOR=\"#FFCC00\" align=\"center\"><P>" +
                      "<A HREF=\"#test_result_" + curGroupName +
                      "\"><B>" + nWorse + "</A></B>&nbsp;&nbsp;&nbsp;&nbsp;(" +
                      (nWorse * 100)/totalTests + "%)</P></TD>");
        buffer.append("<TD BGCOLOR=\"#FFFFFF\" align=\"center\"><P>" +
                      "<A HREF=\"#test_result_" + curGroupName +
                      "\"><B>" + totalTests + "</B></A></P></TD>");
        buffer.append("</TR>");

        return buffer.toString();
    }

    /**
     *  |-------------------|-----------------|------------------------------|
     *  | Tests performance | Number of tests | % from total number of tests |
     *  |-------------------|-----------------|------------------------------|
     *
     */
    private static String getPerformanceTableForTestcase(
        String testName, int nBetter, int nSame, int nWorse) {

        StringBuffer buffer = new StringBuffer();

        int totalTests = nBetter + nSame + nWorse;

        buffer.append("<hr size=\"1\">");
        buffer.append("<H3>Status:</H3>");

        buffer.append("<table cols=\"4\" cellspacing=\"0\" " +
                      "cellpadding=\"3\" border=\"1\" width=\"80%\">");
        buffer.append("<TR BGCOLOR=\"#CCCCFF\">");
        buffer.append("<TD align=\"center\"><B>Tests performance</B></TD>");
        buffer.append("<TD align=\"center\"><B>Number of tests</B></TD>");
        buffer.append("<TD align=\"center\"><B>% from total number of " +
                      "tests</B></TD>");
        buffer.append("</TR>");

        buffer.append("<TR BGCOLOR=\"#99FF99\">");
        buffer.append("<TD><P><A HREF=\"#better\">" +
                      "Target is at least 10 percent BETTER</A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"#better\"><B>" +
                      nBetter + "</B></A></P></TD>");
        buffer.append("<TD align=\"center\"><P>" + (nBetter * 100/totalTests) +
                      "</P></TD>");
        buffer.append("</TR>");

        buffer.append("<TR BGCOLOR=\"#CCFFFF\">");
        buffer.append("<TD><P><A HREF=\"#same\">" +
                      "The same within 10 percent</A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"#same\"><B>" +
                      nSame + "</B></A></P></TD>");
        buffer.append("<TD align=\"center\"><P>" + (nSame * 100/totalTests) +
                      "</P></TD>");
        buffer.append("</TR>");

        buffer.append("<TR BGCOLOR=\"#FFCC00\">");
        buffer.append("<TD><P><A HREF=\"#worse\">" +
                      "Target is at least 10 percent WORSE</A></P></TD>");
        buffer.append("<TD align=\"center\"><P><A HREF=\"#worse\"><B>" +
                      nWorse + "</B></A></P></TD>");
        buffer.append("<TD align=\"center\"><P>" + (nWorse * 100/totalTests) +
                      "</P></TD>");
        buffer.append("</TR>");

        buffer.append("</TABLE>");

        return buffer.toString();
    }

    /**
     *  |-----------|---------|--------------------|----------------------|------------|
     *  | Num Units | Options | <base build> Score | <target build> Score | % Speedup  |
     *  |-----------|---------|--------------------|----------------------|------------|
     *
     *  String[0] = getStatusTableForSummary()
     *  String[1] = getTestResultsTableForSummary()
     *
     * Generate Testcase Report - testcases/<testcase name>.html
     */
    private static String[] generateTestCaseReport(
        String curGroupName,
        Object key,
        J2DAnalyzer.SingleResultSetHolder baseSRSH,
        J2DAnalyzer.SingleResultSetHolder targetSRSH,
        Map testCaseResultCount,
        Map testCaseBaseResult,
        Map testCaseTargetResult) {

        int numBetterTestCases = 0;
        int numWorseTestCases = 0;
        int numSameTestCases = 0;

        StringBuffer tcStartTags = new StringBuffer();
        tcStartTags.append("<tr>");
        tcStartTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Num Units</b></td>");
        tcStartTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>Options</b></td>");
        tcStartTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\"><b>" +
                           baseBuild + " Score</b></td>");
        tcStartTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\"><b>" +
                           targetBuild + " Score</b></td>");
        tcStartTags.append("<td bgcolor=\"#CCCCFF\" align=\"center\">" +
                           "<b>% Speedup</b></td>");
        tcStartTags.append("</tr>");

        StringBuffer worseTestcaseResultTags =
            new StringBuffer(tcStartTags.toString());
        StringBuffer sameTestcaseResultTags =
            new StringBuffer(tcStartTags.toString());
        StringBuffer betterTestcaseResultTags =
            new StringBuffer(tcStartTags.toString());

        Object curTestCountObj = testCaseResultCount.get(key.toString());
        int curTestCount = 0;
        if(curTestCountObj != null) {
            curTestCount = ((Integer) curTestCountObj).intValue();
        }

        String fileName = key.toString().replace('.', '_');
        fileName = fileName.toLowerCase() + ".html";
        File testcaseReportFile =
            new File(resultsDir + File.separator + "testcases", fileName);
        PrintWriter writer = openFile(
            testcaseReportFile.getAbsolutePath(), HTMLGEN_FILE_NEW);

        String header = getHeader(baseSRSH, targetSRSH,
                                  "J2DBench - " + key.toString(),
                                  "../System_Properties.html");
        writer.println(header);
        writer.flush();

        double totalBaseScore = 0;
        double totalTargetScore = 0;

        for(int i=0; i<curTestCount; i++) {

            J2DAnalyzer.ResultHolder baseTCR =
                (J2DAnalyzer.ResultHolder)testCaseBaseResult.get(
                    key.toString() + "_" + i);
            J2DAnalyzer.ResultHolder targetTCR =
                (J2DAnalyzer.ResultHolder) testCaseTargetResult.get(
                    key.toString() + "_" + i);

            double baseScore = baseTCR.getScore();
            double targetScore = targetTCR.getScore();

            StringBuffer tcTagBuffer = new StringBuffer();

            int selColorIndex = selectColor(baseScore, targetScore);
            tcTagBuffer.append("<tr bgcolor=\""+ color[selColorIndex] + "\">");
            tcTagBuffer.append("<td align=\"center\">" + baseTCR.getUnits() +
                               "</td>");
            tcTagBuffer.append("<td valign=\"center\">");

            Map map = baseTCR.getOptions();
            Iterator iter = map.keySet().iterator();
            Object subKey=null, subValue=null;
            tcTagBuffer.append("<ul>");
            while(iter.hasNext()) {
                subKey = iter.next().toString();
                subValue = map.get(subKey).toString();
                tcTagBuffer.append("<li>" + subKey + " = " +
                                   subValue + "</li>");
            }
            tcTagBuffer.append("</ul></td>");
            tcTagBuffer.append("<td valign=\"center\" align=\"center\">" +
                               decimalFormat.format(baseTCR.getScore()) +
                               "</td>");
            tcTagBuffer.append("<td valign=\"center\" align=\"center\">" +
                               decimalFormat.format(targetTCR.getScore()) +
                               "</td>");
            tcTagBuffer.append("<td valign=\"center\" align=\"center\">" +
                               decimalFormat.format(
                                   calculateSpeedupPercentage(
                                       baseTCR.getScore(),
                                       targetTCR.getScore())) +
                                   "</td>");
            tcTagBuffer.append("</tr>");

            totalBaseScore = totalBaseScore + baseTCR.getScore();
            totalTargetScore = totalTargetScore + targetTCR.getScore();

            switch(selColorIndex) {
                case 0:
                    betterTestcaseResultTags.append(tcTagBuffer.toString());
                    numBetterTestCases++;
                    break;
                case 1:
                    sameTestcaseResultTags.append(tcTagBuffer.toString());
                    numSameTestCases++;
                    break;
                case 2:
                    worseTestcaseResultTags.append(tcTagBuffer.toString());
                    numWorseTestCases++;
                    break;
            }
        }

        String performanceTable =
            getPerformanceTableForTestcase(key.toString(),
                                           numBetterTestCases, numSameTestCases,
                                           numWorseTestCases);

        writer.println(performanceTable);
        writer.flush();

        writer.println("<hr size=\"1\">");
        writer.println("<A NAME=\"details\"></A><H3>Details:</H3>");

        writer.println("<table cellspacing=\"0\" " +
                       "cellpadding=\"3\" border=\"1\" width=\"80%\">");

        writer.println("<tr><td colspan=\"5\" " +
                       "valign=\"center\" bgcolor=\"#f0f0f0\">" +
                       "<a name=\"better\"></a><font size=\"+1\">" +
                       key.toString() +
                       " Tests which run BETTER on target</font></td></tr>");
        writer.println(betterTestcaseResultTags.toString());
        writer.flush();

        writer.println("<tr><td colspan=\"5\">&nbsp;<br>&nbsp;</td></tr>");

        writer.println("<tr><td colspan=\"5\" " +
                       "valign=\"center\" bgcolor=\"#f0f0f0\">" +
                       "<a name=\"same\"></a><font size=\"+1\">" +
                       key.toString() +
                       " Tests which run SAME on target</font></td></tr>");
        writer.println(sameTestcaseResultTags.toString());
        writer.flush();

        writer.println("<tr><td colspan=\"5\">&nbsp;<br>&nbsp;</td></tr>");

        writer.println("<tr><td colspan=\"5\" " +
                       "valign=\"center\" bgcolor=\"#f0f0f0\">" +
                       "<a name=\"worse\"></a><font size=\"+1\">" +
                       key.toString() +
                       " Tests which run WORSE on target</font></td></tr>");
        writer.println(worseTestcaseResultTags.toString());
        writer.flush();

        writer.println("</table>");

        writer.println(getFooter());
        writer.flush();

        writer.close();

        String statusTable =
            getStatusTableForSummary(curGroupName, key.toString(),
                                     numBetterTestCases,
                                     numSameTestCases, numWorseTestCases);

        String testResultsTable =
            getTestResultsTableForSummary(key.toString(),
                                          totalBaseScore, totalTargetScore);

        return new String[] {statusTable, testResultsTable};
    }

    /**
     * Returns footer tag for HTML files
     */
    private static String getFooter() {

        StringBuffer buffer = new StringBuffer();

        buffer.append("<br><hr WIDTH=\"100%\" size=\"1\">");
        buffer.append("<A NAME=\"legend\"></A><H3>Legend:</H3>");
        buffer.append("<table cellspacing=\"0\" cellpadding=\"3\" " +
                      "border=\"1\" width=\"80%\">");
        buffer.append("<TR BGCOLOR=\"" + color[0] +
                      "\"><TD>The result for " + targetBuild +
                      " is at least 10 percent BETTER than for " + baseBuild +
                      "</TD></TR>");
        buffer.append("<TR BGCOLOR=\"" + color[1] +
                      "\"><TD>The results for " + targetBuild + " and " +
                      baseBuild + " are within 10 percent</TD></TR>");
        buffer.append("<TR BGCOLOR=\"" + color[2] +
                      "\"><TD>The result for " + targetBuild +
                      " is at least 10 percent WORSE than " + baseBuild +
                      "</TD></TR>");
        buffer.append("<TR><TD>The 'Score' is a number of " +
                      "successful rendering " +
                      "operations per second</TD></TR>");
        buffer.append("</table>");

        buffer.append("<br><hr WIDTH=\"100%\" size=\"1\">");
        buffer.append("</p><hr WIDTH=\"100%\" size=\"1\"></body></html>");

        return buffer.toString();
    }

    /**
     * Returns header tag for HTML files
     */
    private static String
        getHeader(J2DAnalyzer.SingleResultSetHolder baseSRSH,
                  J2DAnalyzer.SingleResultSetHolder targetSRSH,
                  String title,
                 String sysPropLoc)
    {

        StringBuffer buffer = new StringBuffer();

        String headerTitle = getHeaderTitle(title);
        buffer.append(headerTitle);

        //System Properties
        buffer.append("<tr><td bgcolor=\"#CCCCFF\">" +
                      "<b><A HREF=\"" + sysPropLoc + "\">System Property</A>" +
                      "</b></td>" +
                      "<td bgcolor=\"#CCCCFF\"><b><A HREF=\"" +
                      sysPropLoc + "\">Value<A></b></td></tr>");
        Map sysProps = targetSRSH.getProperties();
        buffer.append("<tr><td bgcolor=\"#f0f0f0\">os.name</td><td>" +
                      sysProps.get("os.name") + "</td></tr>");
        buffer.append("<tr><td bgcolor=\"#f0f0f0\">os.version</td><td>" +
                      sysProps.get("os.version") + "</td></tr>");
        buffer.append("<tr><td bgcolor=\"#f0f0f0\">os.arch</td><td>" +
                      sysProps.get("os.arch") + "</td></tr>");

        buffer.append("</table>");

        return buffer.toString();
    }

    /**
     * Returns start tag and title tag for HTML files
     */
    private static String getHeaderTitle(String title) {

        StringBuffer buffer = new StringBuffer();

        buffer.append("<html><head><title>" + title + "</title></head>");
        buffer.append("<body bgcolor=\"#ffffff\"><hr size=\"1\">");
        buffer.append("<center><h2>" + title + "</h2>");
        buffer.append("</center><hr size=\"1\"><br>");
        buffer.append("<table cols=\"2\" cellspacing=\"2\" cellpadding=\"5\" " +
                      "border=\"0\" width=\"80%\">");
        buffer.append("<tr><td bgcolor=\"#CCCCFF\" colspan=\"2\">" +
                      "<b>Test Details</b></td></tr>");
        buffer.append("<tr><td bgcolor=\"#f0f0f0\">Base Build</td>");
        buffer.append("<td>" + baseBuild + "</td></tr>");
        buffer.append("<tr><td bgcolor=\"#f0f0f0\">Target Build</td>");
        buffer.append("<td>" + targetBuild + "</td></tr>");

        return buffer.toString();
    }

    /**
     * Generats System-Properties HTML file - System_Property.html
     */
    private static void
        generateSysPropsReport(J2DAnalyzer.SingleResultSetHolder srsh)
    {

        File sysPropsFile =
            new File(resultsDir, "System_Properties.html");
        PrintWriter writer =
            openFile(sysPropsFile.getAbsolutePath(), HTMLGEN_FILE_NEW);

        String headerTitle = getHeaderTitle("System Properties");
        writer.println(headerTitle);
        writer.flush();

        writer.println("<tr><td bgcolor=\"#CCCCFF\"><b>" +
                       "System Property</b></td><td bgcolor=\"#CCCCFF\">" +
                       "<b>Value</b></td></tr>");

        String key = null;
        String value = null;
        Map sysProps = srsh.getProperties();
        Iterator iter = sysProps.keySet().iterator();
        while(iter.hasNext()) {
            key = iter.next().toString();
            value = sysProps.get(key).toString();
            writer.println("<tr><td bgcolor=\"#f0f0f0\">" +
                           key + "</td><td>" + value + "</td></tr>");
        }
        writer.println("</table>");
        writer.flush();

        writer.println("<br><hr WIDTH=\"100%\" size=\"1\">");
        writer.println("</p><hr WIDTH=\"100%\" size=\"1\"></body></html>");

        writer.flush();
    }

    /**
     * Returns the index of color from color array based on the results
     * Can change this implementation so as to select based on some analysis.
     */
    private static int selectColor(double baseScore, double targetScore) {

        double res = calculateSpeedupPercentage(baseScore, targetScore);

        if (res < -10) {
            return 2;
        } else if (res > 10) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Calculate Speedup Percentage ->
     *     ((target_score - base_score) * 100) / baseScore
     * Can change this implementation so as to provide some analysis.
     */
    private static double calculateSpeedupPercentage(double baseScore,
                                                     double targetScore)
    {
        return ((targetScore - baseScore) * 100)/baseScore;
    }

    private static void printUsage() {
        String usage =
            "\njava XMLHTMLReporter [options]      "     +
            "                                      \n\n" +
            "where options include:                "     +
            "                                      \n"   +
            "    -r | -results <result directory>  "     +
            "directory to which reports are stored \n"   +
            "    -basexml | -b <xml file path>     "     +
            "path to base-build result             \n"   +
            "    -targetxml | -t <xml file path>   "     +
            "path to target-build result           \n"   +
            "    -resultxml | -xml <xml file path> "     +
            "path to result XML                    \n"   +
            "    -group | -g  <level>              "     +
            "group-level for tests                 \n"   +
            "                                      "     +
            " [ 1 , 2 , 3 or 4 ]                   \n"   +
            "    -analyzermode | -am               "     +
            "mode to be used for finding score     \n"   +
            "                                      "     +
            " [ BEST , WORST , AVERAGE , MIDAVG ]  ";
        System.out.println(usage);
        System.exit(0);
    }

    /**
     * main
     */
    public static void main(String[] args) {

        String resDir = ".";
        String baseXML = null;
        String targetXML = null;
        String resultXML = null;
        int group = 2;

        /* ---- Analysis Mode ----
            BEST    = 1;
            WORST   = 2;
            AVERAGE = 3;
            MIDAVG  = 4;
         ------------------------ */
        int analyzerMode = 4;

        try {

            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-results") ||
                    args[i].startsWith("-r"))
                {
                    i++;
                    resDir = args[i];
                } else if (args[i].startsWith("-basexml") ||
                           args[i].startsWith("-b"))
                {
                    i++;
                    baseXML = args[i];
                } else if (args[i].startsWith("-targetxml") ||
                           args[i].startsWith("-t"))
                {
                    i++;
                    targetXML = args[i];
                } else if (args[i].startsWith("-resultxml") ||
                           args[i].startsWith("-xml"))
                {
                    i++;
                    resultXML = args[i];
                } else if (args[i].startsWith("-group") ||
                           args[i].startsWith("-g"))
                {
                    i++;
                    group = Integer.parseInt(args[i]);
                    System.out.println("Grouping Level for tests: " + group);
                } else if (args[i].startsWith("-analyzermode") ||
                           args[i].startsWith("-am"))
                {
                    i++;
                    String strAnalyzerMode = args[i];
                    if(strAnalyzerMode.equalsIgnoreCase("BEST")) {
                        analyzerMode = 0;
                    } else if (strAnalyzerMode.equalsIgnoreCase("WORST")) {
                        analyzerMode = 1;
                    } else if (strAnalyzerMode.equalsIgnoreCase("AVERAGE")) {
                        analyzerMode = 2;
                    } else if (strAnalyzerMode.equalsIgnoreCase("MIDAVG")) {
                        analyzerMode = 3;
                    } else {
                        printUsage();
                    }
                    System.out.println("Analyzer-Mode: " + analyzerMode);
                }
            }
        }
        catch(Exception e) {
            printUsage();
        }

        if(resDir != null) {

            XMLHTMLReporter.setGroupLevel(group);
            J2DAnalyzer.setMode(analyzerMode);

            if(targetXML != null && baseXML != null) {
                XMLHTMLReporter.generateComparisonReport(resDir, baseXML,
                                                         targetXML);
            } else if (resultXML != null) {
                XMLHTMLReporter.generateReport(resDir, resultXML);
            } else {
                printUsage();
            }
        } else {
            printUsage();
        }
    }
}
