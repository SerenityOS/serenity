<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:TRI="http://www.exchangenetwork.net/schema/TRI/6"
                xmlns:sc="urn:us:net:exchangenetwork:sc:1:0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="TRI sc">
  <xsl:output method="html" version="4.0" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd" />
  <xsl:template match="/">
    <html>
    <head>
      <title>TRI Reporting Form</title>
      <script type="text/javascript">
				var PhantomJSPrinting = {
					header: {
						height: '0.0in',
						contents: function(pageNum, numPages) { return ""; }
						},
					footer: {
						height: '0.0in',
						contents: function(pageNum, numPages) { return ""; }
						}
				};
	  </script>
      <style type="text/css">
          p {
            font-family: Arial;
            padding: 0;
            margin: 0;
            border: 0;
          }

          .answerText {
              color: blue;
              font-size: 9pt;
              font-weight: bold;
          }

          .smallAnswer {
              color: blue;
              font-size: 8pt;
              font-weight: bold;
          }

          .teqAnswer {
              color: blue;
              font-size: 8pt;
              font-weight: bold;
          }

          .fieldLabel {
              font-family: Arial, Helvetica, sans-serif;
              font-size: 12px;
              font-weight: bold;
              padding-right: 5px;
          }

          a { padding-right: 10px; }

          @page land { size: landscape; margin: 0.1in; }

          .landscapeArea { page: land; width: 1010px; page-break-before: always; }

          @page { margin: 0.1in; }

          }
        </style>
    </head>
    <body style="font-family: arial">
    <xsl:for-each select="//TRI:Report">
      <xsl:variable name="formID"><xsl:value-of select="sc:ReportIdentifier"/></xsl:variable>
	<xsl:variable name="OMBNumberFormR">
	<xsl:choose>
	          <xsl:when test="TRI:SubmissionReportingYear = '2005'">2070-0093</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2006'">2070-0093</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2007'">2070-0093</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2008'">2070-0093</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2009'">2025-0009</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2010'">2025-0009</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2011'">2025-0009</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2012'">2025-0009</xsl:when>
	        </xsl:choose>
	</xsl:variable>
	<xsl:variable name="OMBNumberFormA">
	 <xsl:choose>
	          <xsl:when test="TRI:SubmissionReportingYear = '2005'">2070-0143</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2006'">2070-0143</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2007'">2070-0143</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2008'">2070-0143</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2009'">2025-0010</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2010'">2025-0010</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2011'">2025-0009</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2012'">2025-0009</xsl:when>
	        </xsl:choose>
	</xsl:variable>
	<xsl:variable name="OMBNumberSchedule1">
	  <xsl:choose>
	          <xsl:when test="TRI:SubmissionReportingYear = '2008'">2025-0007</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2009'">2025-0007</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2010'">2025-0007</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2011'">2025-0009</xsl:when>
	          <xsl:when test="TRI:SubmissionReportingYear = '2012'">2025-0009</xsl:when>
	        </xsl:choose>
	</xsl:variable>

      <xsl:variable name="RevisionDateFormR">
        <xsl:choose>
           <xsl:when test="TRI:SubmissionReportingYear = '2005'">08/2005</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2006'">08/2006</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2007'">01/2008</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2008'">08/2008</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2009'">10/2009</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2010'">10/2009</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2011'">10/2011</xsl:when>
           <xsl:when test="TRI:SubmissionReportingYear = '2012'">10/2012</xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="RevisionDateFormA">
        <xsl:choose>
          <xsl:when test="TRI:SubmissionReportingYear = '2005'">08/2005</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2006'">11/2006</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2007'">01/2008</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2008'">03/2009</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2009'">10/2009</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2010'">03/2009</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2011'">10/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2012'">10/2012</xsl:when>
        </xsl:choose>
      </xsl:variable>

      <xsl:variable name="ApprovalDateFormR">
        <xsl:choose>
          <xsl:when test="TRI:SubmissionReportingYear = '2005'">01/31/2008</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2006'">01/31/2008</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2007'">01/31/2010</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2008'">03/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2009'">07/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2010'">07/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2011'">10/31/2014</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2012'">10/31/2014</xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="ApprovalDateFormA">
        <xsl:choose>
          <xsl:when test="TRI:SubmissionReportingYear = '2005'">01/31/2008</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2006'">01/31/2008</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2007'">03/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2008'">03/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2009'">07/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2010'">07/31/2011</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2011'">10/31/2014</xsl:when>
          <xsl:when test="TRI:SubmissionReportingYear = '2012'">10/31/2014</xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="ApprovalDateSchedule1">
		  <xsl:choose>
			  <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'"> 07/31/2011</xsl:when>
			  <xsl:otherwise>10/31/2014</xsl:otherwise>
		  </xsl:choose>
      </xsl:variable>

      <xsl:variable name="ScheduleOneNA">
      <xsl:choose>
          <xsl:when test="TRI:SubmissionReportingYear &gt; '2007'
                          and TRI:ChemicalIdentification/sc:CASNumber = 'N150'
                          and count(descendant-or-self::TRI:ToxicEquivalencyIdentification[TRI:ToxicEquivalencyNAIndicator = 'false']) &gt; 0">false</xsl:when>
          <xsl:otherwise>true</xsl:otherwise>
      </xsl:choose>
      </xsl:variable>

      <xsl:choose>
      <xsl:when test="TRI:ReportType/TRI:ReportTypeCode = 'TRI_FORM_R'">
          <xsl:if test="count(preceding::TRI:Report) &gt; 0">
            <p style="page-break-before: always">&#160;</p>
          </xsl:if>

          <!-- Page 1 : Facility Information -->
          <br/>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" border="0">
            <tr>
              <td align="center" width="100%">
                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
                  *** Do not send to EPA: This is the final copy of your form.***
                &#160;</span>
              </td>
            </tr>

            <xsl:if test="SubmissionStatusText">
              <tr>
                <td align="left">
                  <span class="fieldLabel" style="color:red">
                    Form Status:
                    <xsl:value-of select="SubmissionStatusText"/>
                  &#160;</span>
                </td>
              </tr>
            </xsl:if>
            <xsl:if test="ValidationStatusText">
              <tr>
                <td align="left">
                  <span class="fieldLabel" style="color:red">
                    Validation Status:
                    <xsl:value-of select="ValidationStatusText"/>
                  &#160;</span>
                </td>
              </tr>
            </xsl:if>


          </table>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
            <tr>
              <td width="30%">
              	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                	<p style="font-size: 8pt">
                	  Form Approved OMB Number:
                	  <span class="smallAnswer">
                	    <xsl:value-of select="$OMBNumberFormR"/>
                	  &#160;</span>
                	</p>
                </xsl:if>
              </td>
              <td width="10%">
                <br/>
              </td>
            </tr>
            <tr>
              <td width="60%">
              	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	                <p style="font-size: 8pt"><i>(IMPORTANT: Read instructions before completing form; type or use fill-and-print form)</i></p>
	            </xsl:if>
              </td>
              <td width="30%">
              	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	                <p style="font-size: 8pt">
    	              Approval Expires:
        	          <span class="smallAnswer">
            	        <xsl:value-of select="$ApprovalDateFormR"/>
                	  &#160;</span>
                	</p>
                </xsl:if>
              </td>
              <td width="10%">
                <p style="font-size: 8pt">
                  <b>Page 1 of 5 </b>
                </p>
              </td>
            </tr>

          </table>
          <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt;">
              <tr>
                <td colspan="2" align="center">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="0" cellspacing="0" cellpadding="0" frame="void">
                    <tr>
                      <td style="font-size: 8pt">
                        <p style="font-size: 10pt"><b>EPA</b></p>
                      </td>
                      <td align="center" style="font-size: 10pt">
                        <p style="font-size: 14pt"><b>FORM R</b></p>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <p style="font-size: 8pt">
                          United States <br/> Environmental Protection<br/>Agency
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          Section 313 of the Emergency Planning and Community Right-to-know Act of 1986,
                          <br/>
                          also known as Title III of the Superfund Amendments and Reauthorization Act.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
                <td style="font-size:8pt">
                    TRI Facility ID Number
                    <hr />
                    <span class="answerText">
                      <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                    &#160;</span>
                  <hr />
                  Toxic Chemical, Category, or Generic Name
                  <hr/>
                  <span class="answerText">
                    <xsl:choose>
                      <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                        <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                      </xsl:otherwise>
                    </xsl:choose>
                  &#160;</span>
                </td>
              </tr>
              <tr>
                <td colspan="3">
                  <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt" frame="void">
                    <tr>
                      <td align="center" width="15%">
                        <p style="font-size: 8pt">WHERE TO SEND COMPLETED FORMS: </p>
                      </td>
                      <td nowrap="nowrap">
                        <p style="font-size: 8pt">
                          1. TRI Data Processing Center
                          <br />
                          P.O. Box 10163
                          <br />
                          Fairfax, VA 22038
                          <br />
                        </p>
                      </td>
                      <td nowrap="nowrap">
                        <p style="font-size: 8pt">2. APPROPRIATE STATE OFFICE<br/>(See instructions in Appendix F)</p>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="3"></td>
                    </tr>
                  </table>
                  <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" frame="void">
                    <tr>
                      <td width="33%">
                        <p style="font-size: 9pt">This section only applies if you are revising or withdrawing a previously submitted form, otherwise leave blank:</p>
                      </td>
                      <td align="center" width="33%">
                        <p style="font-size: 9pt">
                          Revision (Enter up to two code(s))
                          <br/>
                          <br/>
                          [
                          <span class="answerText">
                            <xsl:value-of select="TRI:ChemicalReportRevisionCode[1]"/>
                          &#160;</span>
                          ] [
                          <span class="answerText">
                            <xsl:value-of select="TRI:ChemicalReportRevisionCode[2]"/>
                          &#160;</span>
                          ]
                        </p>
                      </td>
                      <td align="center" width="34%">
                        <p style="font-size: 9pt">
                          Withdrawal (Enter up to two code(s))
                          <br/>
                          <br/>
                          [
                          <span class="answerText">
                            <xsl:value-of select="TRI:ChemicalReportWithdrawalCode[1]"/>
                          &#160;</span>
                          ] [
                          <span class="answerText">
                            <xsl:value-of select="TRI:ChemicalReportWithdrawalCode[2]"/>
                          &#160;</span>
                          ]
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td align="left" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">Important: See Instructions to determine when "Not Applicable (NA)" boxes should be checked.</p>
                </td>
              </tr>
              <tr>
                <td align="center" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">Part I. FACILITY IDENTIFICATION INFORMATION </p>
                </td>
              </tr>
              <tr>
                <td align="left" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">SECTION 1. REPORTING YEAR :
                    <u><span class="answerText"><xsl:value-of select="TRI:SubmissionReportingYear"/></span></u>
                  </p>
                </td>
              </tr>
              <tr>
                <td align="left" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">SECTION 2. TRADE SECRET INFORMATION</p>
                </td>
              </tr>
              <tr>
                <td align="left" style="font-size: 8pt">
                    <dl>
                      <dt>2.1 Are you claiming the toxic chemical identified on
                          page 2 trade secret?</dt>
                      <dd>
                        [
                        <xsl:choose>
                          <xsl:when test="TRI:ChemicalTradeSecretIndicator = 'true'">
                            <span class="answerText">X</span>
                          </xsl:when>
                        </xsl:choose>
                        ] Yes (Answer question 2.2; attach substantiation forms)
                      </dd>
                      <dd>
                        [
                        <xsl:choose>
                          <xsl:when test="TRI:ChemicalTradeSecretIndicator = 'false'">
                            <span class="answerText">X</span>
                          </xsl:when>
                        </xsl:choose>
                        ] NO (Do not answer 2.2; go to Section 3)
                      </dd>
                    </dl>
                </td>
                <td align="left" style="font-size: 8pt">
                    <dl>
                      <dt>2.2 Is this copy</dt>
                      <dd>
                        [
                        ] Sanitized [
                        ] Unsanitized
                      </dd>
                      <dd>(Answer only if "Yes" in 2.1)</dd>
                    </dl>
                </td>
                <td>
                  <br/>
                </td>
              </tr>
              <tr>
                <td align="left" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">SECTION 3. CERTIFICATION (Important: Read and sign after completing all form sections.)</p>
                </td>
              </tr>
              <tr>
                <td align="left" colspan="3">
                  <p style="font-size: 8pt">
                        I hereby certify that I have reviewed the attached documents and that, to the best of my knowledge and belief, the submitted
                        information is true and complete and that the amounts and values in this report are accurate based on reasonable estimates using data
                        available to the preparers of this report.
                  </p>
                  <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt" frame="above">
                    <tr>
                      <td>Name and official title of owner/operator or senior management official:</td>
                      <td>Signature:</td>
                      <td>Date Signed:</td>
                    </tr>
                    <tr>
                      <td>
  						<p>
      						<span class="answerText"><xsl:value-of select="TRI:CertifierName"/>&#160;&#160;&#160;</span>
      						<span class="answerText"><xsl:value-of select="TRI:CertifierTitleText"/></span>
  						</p>
					  </td>
					  <td>
    					<p>
      						<span style="font-size: 9pt; color: red; font-weight: bold;">Reference Copy: Copy of Record Resides in CDX</span>
    					</p>
					  </td>
					  <td>
     					<span class="answerText">
					    	<xsl:if test="TRI:CertificationSignedDate != '' and TRI:CertificationSignedDate != '1900-01-01'">
	                    		<xsl:value-of select="TRI:CertificationSignedDate"/>
	                   		</xsl:if>
     					</span>
					  </td>
                    </tr>
                  </table>
                  </td>
              </tr>
              <tr>
                <td align="left" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">SECTION 4. FACILITY IDENTIFICATION </p>
                </td>
              </tr>
              <tr>
                <td colspan="3">
                  <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0" border="1" frame="void" style="font-size: 8pt">
                    <tr>
                      <td width="5%" style="font-size: 8pt">
                        <p style="font-size: 8pt">4.1</p>
                      </td>
                      <td colspan="4" width="45%">&#160;</td>
                      <td colspan="2" width="20%">
                        <p style="font-size: 8pt">TRI Facility ID Number</p>
                      </td>
                      <td colspan="2" width="30%">
                        <p>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                          &#160;</span>
                        </p>
                      </td>
                    </tr>

                    <xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
	                    <tr>
	                      <td colspan="5">
	                        <p style="font-size: 7pt">
	                          <u style="font-size: 7pt">Facility or Establishment Name</u>
	                          <br/>
	                          <span class="answerText">
	                            <xsl:value-of select="../TRI:Facility/sc:FacilitySiteName"/>
	                          </span>
	                        </p>
	                      </td>
	                      <td colspan="4">
	                        <p style="font-size: 7pt">
	                          <u style="font-size: 7pt">Facility or Establishment Name or Mailing Address(if different from street address)</u>
	                          <br/>
	                          <span class="answerText">
	                            <xsl:value-of select="../TRI:Facility/TRI:MailingFacilitySiteName"/>
	                          </span>
	                        </p>
	                      </td>
	                    </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
		                  <td colspan="9">
		                   <p>
		                    <u style="font-size: 7pt">Facility or Establishment Name</u>
		                   </p>
		                   <p>
		                    <span class="answerText">
		                     <xsl:value-of select="../TRI:Facility/sc:FacilitySiteName"/>
		                    &#160;</span>
		                   </p>
		                  </td>
		                </tr>
                      </xsl:otherwise>
                    </xsl:choose>

                    <tr>
                      <td colspan="5">
                        <p>
                          <u style="font-size: 7pt">Street</u>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocationAddressText"/>
                          &#160;</span>
                        </p>
                      </td>
                      <td colspan="4">
                        <p>
                          <u style="font-size: 7pt">Mailing Address (if different from physical street address)</u>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:MailingAddressText"/>
                          &#160;</span>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="5">
                        <p>
                        <xsl:choose>
                            <xsl:when test="TRI:SubmissionReportingYear >= '2012' ">
                               <u style="font-size: 7pt">City/County/Tribe/State/ZIP Code</u>
                               <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocalityName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                          &#160;</span>
                           /
                          <span class="answerText">
                           BIA Code: <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:TribalIdentity/sc:TribalCode"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:AddressPostalCode"/>
                          &#160;</span>
                          </xsl:when>

                          <xsl:otherwise>
                            <u style="font-size: 7pt">City/County/State/ZIP Code</u>
                               <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocalityName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:AddressPostalCode"/>
                          &#160;</span>
                          </xsl:otherwise>
                          </xsl:choose>

                        </p>
                      </td>
                      <td colspan="3">
                        <p>
                          <u style="font-size: 7pt">City/State/ZIP Code</u>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:MailingAddressCityName"/>
                          &#160;</span>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/TRI:ProvinceNameText"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:StateIdentity/sc:StateName"/>
                          &#160;</span>
                          /
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:AddressPostalCode"/>
                          &#160;</span>
                        </p>
                      </td>
                      <td colspan="1" width="15%">
                        <p>
                          <u style="font-size: 7pt">Country (Non-US)</u>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:CountryIdentity/sc:CountryName"/>
                          &#160;</span>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="3">
                  <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0" border="1"
                         frame="void" style="font-size: 8pt">
                    <tr>
                      <td width="5%" style="font-size: 8pt">
                        <p style="font-size: 8pt">4.2</p>
                      </td>
                      <td nowrap="nowrap">
                        <p style="font-size: 8pt">
                          This report contains information for :
                          <br/>
                          (
                          <u>Important: </u>
                          check a or b; check c or d if applicable)
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          a. [
                          <xsl:choose>
                            <xsl:when test="TRI:SubmissionPartialFacilityIndicator = 'false'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ] An Entire facility
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          b. [
                          <xsl:choose>
                            <xsl:when test="TRI:SubmissionPartialFacilityIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ] Part of a facility
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          c. [
                          <xsl:choose>
                            <xsl:when test="TRI:SubmissionFederalFacilityIndicator = 'Y'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ] A Federal facility
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          d. [
                          <xsl:choose>
                            <xsl:when test="TRI:SubmissionGOCOFacilityIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ] GOCO
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="3">
                  <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0" border="1"
                         frame="void" style="font-size: 8pt">
                    <tr>
                      <td width="5%" style="font-size: 8pt">
                        <p style="font-size: 8pt">4.3</p>
                      </td>
                      <td colspan="2" nowrap="nowrap" align="center">
                        <p style="font-size: 8pt">Technical Contact name </p>
                      </td>
                      <td colspan="2">
                        <span class="answerText" style="width:150px; word-wrap:break-word;">
                          <xsl:value-of select="TRI:TechnicalContactNameText/sc:IndividualFullName"/>
                        &#160;</span>
                      </td>
                      <td colspan="2" nowrap="nowrap">
                        <p style="font-size: 8pt">
                          <u style="font-size: 7pt">Email Address</u>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="TRI:TechnicalContactEmailAddressText"/>
                          &#160;</span>
                        </p>
                      </td>
                      <td colspan="2" nowrap="nowrap">
                        <p style="font-size: 8pt">
       					<xsl:choose>
							<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
        						<u style="font-size: 7pt">Telephone Number (include area code and ext.)</u>
        		 			</xsl:when>
        					<xsl:otherwise>
        						<u style="font-size: 7pt">Telephone Number (include area code)</u>
        					</xsl:otherwise>
       					</xsl:choose>
                          <br/>
                          <span class="answerText">
                            <xsl:value-of select="substring(TRI:TechnicalContactPhoneText,1, 3)"/>&#45;<xsl:value-of select="substring(TRI:TechnicalContactPhoneText,4, 3)"/>&#45;<xsl:value-of select="substring(TRI:TechnicalContactPhoneText,7)"/>
                          &#160;</span>
                          <xsl:if test="TRI:SubmissionReportingYear &gt; '2013' ">
                          	 <span class="answerText">
                            	<xsl:if test="string-length(TRI:TechnicalContactPhoneExtText) &gt; 0 ">
									&#045; &#160;<xsl:value-of select="TRI:TechnicalContactPhoneExtText"/>
								</xsl:if>
                          	&#160;</span>
                          </xsl:if>
                        </p>
                      </td>
                    </tr>
                    <xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear >= '2007' or TRI:SubmissionReportingYear &lt;= '2004'">
                        <tr>
                          <td width="5%" style="font-size: 8pt">
                            <p style="font-size: 8pt">4.4</p>
                          </td>
                          <td colspan="2" nowrap="nowrap" align="center">
                            <p style="font-size: 8pt">Public Contact name </p>
                          </td>
                          <td colspan="2">
                            <span class="answerText">
                              <xsl:value-of select="TRI:PublicContactNameText"/>
                            &#160;</span>
                          </td>
                          <td colspan="2" nowrap="nowrap">
                            <p style="font-size: 8pt">
                              <u style="font-size: 7pt">Email Address</u>
                              <br/>
                              <span class="answerText">
                                <xsl:value-of select="TRI:PublicContactEmailAddressText"/>
                              &#160;</span>
                            </p>
                          </td>
                          <td colspan="2" nowrap="nowrap">
                            <p style="font-size: 8pt">
                            <xsl:choose>
								<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
        							<u style="font-size: 7pt">Telephone Number (include area code and ext.)</u>
        						</xsl:when>
        						<xsl:otherwise>
        							<u style="font-size: 7pt">Telephone Number (include area code)</u>
        						</xsl:otherwise>
       						</xsl:choose>
                              <br/>
                              <span class="answerText">
                              	<xsl:if test="string-length(TRI:PublicContactPhoneText)  &gt; 0">
                              		<xsl:value-of select="substring(TRI:PublicContactPhoneText,1, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,4, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,7)"/>
                              	</xsl:if>
                              &#160;</span>
                              <xsl:if test="TRI:SubmissionReportingYear &gt; '2013' ">
                          		 <span class="answerText">
                            		<xsl:if test="string-length(TRI:PublicContactPhoneExtText) &gt; 0 ">
										&#045; &#160;<xsl:value-of select="TRI:PublicContactPhoneExtText"/>
									</xsl:if>
                          		&#160;</span>
                          	  </xsl:if>
                            </p>
                          </td>
                        </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
                          <td width="5%" style="font-size: 8pt">
                            <p style="font-size: 8pt">4.4</p>
                          </td>
                          <td colspan="2" nowrap="nowrap" align="center">
                            <p style="font-size: 8pt">Public Contact name </p>
                          </td>
                          <td colspan="4">
                            <span class="answerText">
                              <xsl:value-of select="TRI:PublicContactNameText"/>
                            &#160;</span>
                          </td>
                          <td colspan="2" nowrap="nowrap">
                            <p style="font-size: 8pt">
                            <xsl:choose>
								<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
        							<u style="font-size: 7pt">Telephone Number (include area code and ext.)</u>
        						</xsl:when>
        						<xsl:otherwise>
        							<u style="font-size: 7pt">Telephone Number (include area code)</u>
        						</xsl:otherwise>
       						</xsl:choose>
                              <br/>
                              <span class="answerText">
                               <xsl:if test="string-length(TRI:PublicContactPhoneText)  &gt; 0">
                                <xsl:value-of select="substring(TRI:PublicContactPhoneText,1, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,4, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,7)"/>
                                </xsl:if>
                              &#160;</span>
                              <xsl:if test="TRI:SubmissionReportingYear &gt; '2013' ">
                          		 <span class="answerText">
                            		<xsl:if test="string-length(TRI:PublicContactPhoneExtText) &gt; 0 ">
										&#045; &#160;<xsl:value-of select="TRI:PublicContactPhoneExtText"/>
									</xsl:if>
                          		&#160;</span>
                          	  </xsl:if>
                            </p>
                          </td>
                        </tr>
                      </xsl:otherwise>
                    </xsl:choose>
                    <xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear &lt;= '2005'">
                        <tr>
                          <td width="5%" style="font-size: 8pt">
                            <p style="font-size: 8pt">4.5</p>
                          </td>
                          <td colspan="2" nowrap="nowrap" width="35%"
                              align="center">
                            <p style="font-size: 8pt">SIC Code(s) (4 digits)</p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              a.
                              <xsl:for-each select="../TRI:Facility/TRI:FacilitySIC">
                                <xsl:choose>
                                  <xsl:when test="sc:SICPrimaryIndicator = 'Primary'">
                                    <span class="answerText">
                                      <xsl:value-of select="sc:SICCode"/>
                                      (Primary)
                                    &#160;</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              b.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 1">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[2]/sc:SICCode"/>
                                  &#160;</span>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              c.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 2">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[3]/sc:SICCode"/>
                                  &#160;</span>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              d.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 3">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[4]/sc:SICCode"/>
                                  &#160;</span>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              e.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 4">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[5]/sc:SICCode"/>
                                  &#160;</span>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              f.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 5">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[6]/sc:SICCode"/>
                                  &#160;</span>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                        </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
                          <td width="5%" style="font-size: 8pt">
                            <p style="font-size: 8pt">4.5</p>
                          </td>
                          <td colspan="2" nowrap="nowrap" width="35%"
                              align="center">
                            <p style="font-size: 8pt">NAICS Code(s) (6 digits)</p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              a.
                              <xsl:for-each select="../TRI:Facility/TRI:FacilityNAICS">
                                <xsl:choose>
                                  <xsl:when test="sc:NAICSPrimaryIndicator = 'Primary'">
                                    <span class="answerText">
                                      <xsl:value-of select="sc:NAICSCode"/>
                                      (Primary)
                                    &#160;</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              b.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 1">
                                <xsl:choose>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[1]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[1]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[2]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[2]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              c.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 2">
                                  <xsl:choose>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[3]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[3]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              d.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 3">
                                   <xsl:choose>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[4]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[4]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              e.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 4">
                                   <xsl:choose>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[5]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[5]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                          <td colspan="1" width="10%">
                            <p style="font-size: 8pt">
                              f.
                              <xsl:choose>
                                <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 5">
                                   <xsl:choose>
                                  <xsl:when test="../TRI:Facility/TRI:FacilityNAICS[6]/sc:NAICSPrimaryIndicator = 'Unknown'">
                                  <span class="answerText">
                                    <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[6]/sc:NAICSCode"/>
                                  &#160;</span>
                                  </xsl:when>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </p>
                          </td>
                        </tr>
                      </xsl:otherwise>
                    </xsl:choose>
                    <tr>
                      <td colspan="9">
                        <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0"
                               border="1" frame="void" style="font-size: 8pt"
                               rules="all">
                          <tr>
                            <td width="5%" style="font-size: 8pt">
                              <p style="font-size: 8pt">4.6</p>
                            </td>
                            <td nowrap="nowrap">
                              <p style="font-size: 8pt">
                                Dun and Bradstreet
                                <br/>
                                Number(s) (9 digits)
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td colspan="2">
                              <p style="font-size: 8pt">
                                a.
                                <span class="answerText">
                                  <xsl:value-of select="../TRI:Facility/TRI:FacilityDunBradstreetCode[1]"/>
                                &#160;</span>
                              </p>
                            </td>
                          </tr>
                          <tr>
                            <td colspan="2">
                              <p style="font-size: 8pt">
                                b.
                                <span class="answerText">
                                  <xsl:value-of select="../TRI:Facility/TRI:FacilityDunBradstreetCode[2]"/>
                                &#160;</span>
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td align="left" colspan="9" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 5. PARENT COMPANY INFORMATION</p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" style="font-size: 8pt">
                        <p style="font-size: 8pt">5.1</p>
                      </td>
                      <td colspan="2" align="left">
                        <p style="font-size: 8pt">
                        <xsl:choose>
						  <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
						    Name of Parent Company
						  </xsl:when>
						  <xsl:otherwise>
						    Name of U.S. Parent Company (for TRI Reporting purposes)
						  </xsl:otherwise>
						</xsl:choose>
                        </p>
                      </td>
                      <xsl:choose>
						  <xsl:when test="TRI:SubmissionReportingYear &gt; '2010'">
						  <td colspan="5">
                        <span class="answerText">
                        <xsl:if test="../TRI:Facility/TRI:ParentCompanyNameText != 'NA'">
                    		<xsl:value-of select="../TRI:Facility/TRI:ParentCompanyNameText"/>
                   		</xsl:if>
                        &#160;</span>
                        <br/>
                      </td>
                      <td colspan="1">
                        <p style="font-size: 8pt">
						    No U.S. Parent Company (for TRI Reporting purposes) [
					    <xsl:choose>
                            <xsl:when test="../TRI:Facility/TRI:ParentCompanyNameNAIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ]
                        </p>
                      </td>
						  </xsl:when>
						  <xsl:otherwise>
					                       <td colspan="1">
                        <p style="font-size: 8pt">
						    NA [

					    <xsl:choose>
                            <xsl:when test="../TRI:Facility/TRI:ParentCompanyNameNAIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ]
                        </p>
                      </td>
                      <td colspan="5">
                        <span class="answerText">
                          <xsl:value-of select="../TRI:Facility/TRI:ParentCompanyNameText"/>
                        &#160;</span>
                        <br/>
                      </td>
					  	  </xsl:otherwise>
					  </xsl:choose>

                    </tr>
                    <tr>
                      <td width="5%" style="font-size: 8pt">
                        <p style="font-size: 8pt">5.2</p>
                      </td>
                      <td colspan="2" align="left">
                        <p style="font-size: 8pt">Parent Company's Dun &amp; Bradstreet Number </p>
                      </td>
                      <td colspan="1">
                        <p style="font-size: 8pt">
                          NA [
                          <xsl:choose>
                            <xsl:when test="../TRI:Facility/TRI:ParentDunBradstreetCode = 'NA'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ]
                        </p>
                      </td>
                      <td colspan="5">
                        <xsl:choose>
                          <xsl:when test="../TRI:Facility/TRI:ParentDunBradstreetCode != 'NA'">
                            <span class="answerText">
                              <xsl:value-of select="../TRI:Facility/TRI:ParentDunBradstreetCode"/>
                            &#160;</span>
                          </xsl:when>
                        </xsl:choose>
                        <br/>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </center>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
              <td width="60%">
                <p style="font-size: 8pt">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		EPA Form 9350-1 (Rev. <xsl:value-of select="$RevisionDateFormR"/>) - Previous editions are obsolete.
                  	</xsl:if>
                </p>
              </td>
              <td width="30%">
                <p style="font-size: 8pt">Printed using TRI-MEweb</p>
              </td>
            </tr>
          </table>

          <p style="page-break-before: always">&#160;</p>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1" style="font-size: 8pt" border="0">
            <tr>
            <!--<td width="90%">
                <a name="PG-2_{$formID}"></a>
                <p style="font-size: 8pt">
                  <a href="#PG-1_{$formID}">1</a>
                  <a href="#PG-2_{$formID}">2</a>
                  <a href="#PG-3_{$formID}">3</a>
                  <a href="#PG-4_{$formID}">4</a>
                  <a href="#PG-5_{$formID}">5</a>
                  <a href="#PG-6_{$formID}">Additional Info</a>
                  <xsl:if test="$ScheduleOneNA = 'false'">
                      <a href="#S1_PG-1_{$formID}">Schedule 1</a>
                  </xsl:if>
                </p>
              </td>-->
              <td width="10%">
                <p style="font-size: 8pt">
                  <b>Page 2 of 5 </b>
                </p>
              </td>
            </tr>
          </table>
          <center>
          <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
           </tr>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt">
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                         cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td width="65%" align="center" style="font-size: 10pt">
                        <p style="font-size: 10pt">
                          <b>EPA FORM R</b>
                          <br/>
                          <b>PART II. CHEMICAL - SPECIFIC INFORMATION</b>
                        </p>
                      </td>
                      <td>
                          TRI Facility ID Number
                          <hr />
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                          &#160;</span>
                        <hr />
                        Toxic Chemical, Category, or Generic Name
                        <hr/>
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <tr>
                      <td align="left" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 1. TOXIC CHEMICAL IDENTITY </p>
                      </td>
                      <td align="left" style="font-size: 8pt">
                        <p style="font-size: 8pt">(Important: DO NOT complete this section if you are reporting a mixture component in Section 2 below.)</p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td align="center" width="5%">
                  <p style="font-size: 8pt">1.1</p>
                </td>
                <td width="95%" style="font-size: 8pt">
                    CAS Number (Important: Enter only one number exactly as it
                    appears on the Section 313 list. Enter category code if
                    reporting a chemical category.)
                    <hr />
                    <span style="color: blue;text-indent: 5em;font-weight:bold">
                      <xsl:value-of select="TRI:ChemicalIdentification/sc:CASNumber"/>
                    &#160;</span>
                </td>
              </tr>
              <tr>
                <td align="center" width="5%">
                  <p style="font-size: 8pt">1.2</p>
                </td>
                <td style="font-size: 8pt">
                    Toxic Chemical or Chemical Category Name (Important: Enter
                    only one name exactly as it appears on the Section 313 list.)
                    <hr />
                    <span style="color: blue;font-size: 8pt;text-indent: 5em;font-weight:bold">
                      <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                    &#160;</span>
                </td>
              </tr>
              <tr>
                <td align="center" width="5%">
                  <p style="font-size: 8pt">1.3</p>
                </td>
                <td style="font-size: 8pt">
                    Generic Chemical Name (Important: Complete only if Part I,
                    Section 2.1 is checked "Yes". Generic Name must be
                    structurally descriptive).
                    <hr />
                    <span style="color: blue;font-size: 8pt;text-indent: 5em;font-weight:bold">NA&#160;</span>
                </td>
              </tr>

              <!-- Section 1.4, only display if < RY08 -->
              <xsl:if test="TRI:SubmissionReportingYear &lt;= '2008' and TRI:SubmissionReportingYear >= '2000'">
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td align="center" width="5%">
                        <p style="font-size: 8pt">1.4</p>
                      </td>
                      <td colspan="17">
                        <p style="font-size: 8pt">
                          Distribution of Each Member of the Dioxin and
                          Dioxin-like Compounds Category.
                          <br/>
                          (If there are any numbers in boxes 1-17, then every
                          field must be filled in with either 0 or some number
                          between 0.01 and 100. Distribution should be reported
                          in percentages and the total should equal 100%. If you
                          do not have speciation data available, indicate NA.)
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <xsl:for-each select="TRI:ChemicalIdentification">
                        <xsl:choose>
                          <xsl:when test="TRI:DioxinDistributionNAIndicator= 'true'">
                            <td align="center" width="5%">
                              <p style="font-size: 8pt">
                                NA [
                                <span class="answerText">X</span>
                                ]
                              </p>
                            </td>
                            <td><p style="font-size: 8pt">1<hr/></p></td>
                            <td><p style="font-size: 8pt">2<hr/></p></td>
                            <td><p style="font-size: 8pt">3<hr/></p></td>
                            <td><p style="font-size: 8pt">4<hr/></p></td>
                            <td><p style="font-size: 8pt">5<hr/></p></td>
                            <td><p style="font-size: 8pt">6<hr/></p></td>
                            <td><p style="font-size: 8pt">7<hr/></p></td>
                            <td><p style="font-size: 8pt">8<hr/></p></td>
                            <td><p style="font-size: 8pt">9<hr/></p></td>
                            <td><p style="font-size: 8pt">10<hr/></p></td>
                            <td><p style="font-size: 8pt">11<hr/></p></td>
                            <td><p style="font-size: 8pt">12<hr/></p></td>
                            <td><p style="font-size: 8pt">13<hr/></p></td>
                            <td><p style="font-size: 8pt">14<hr/></p></td>
                            <td><p style="font-size: 8pt">15<hr/></p></td>
                            <td><p style="font-size: 8pt">16<hr/></p></td>
                            <td><p style="font-size: 8pt">17<hr/></p></td>
                          </xsl:when>
                          <xsl:otherwise>
                            <td align="center" width="5%">
                              <p style="font-size: 8pt">NA [ ]</p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                1
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution1Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                2
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution2Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                3
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution3Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                4
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution4Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                5
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution5Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                6
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution6Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                7
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution7Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                8
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution8Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                9
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution9Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                10
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution10Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                11
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution11Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                12
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution12Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                13
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution13Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                14
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution14Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                15
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution15Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                16
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution16Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                17
                                <hr/>
                                <span class="answerText">
                                  <xsl:value-of select="TRI:DioxinDistribution17Percent"/>
                                &#160;</span>
                              </p>
                            </td>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:for-each>
                    </tr>
                  </table>
                </td>
              </tr>
              </xsl:if>
              <!-- End prior to RY08 dioxin section -->
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <tr>
                      <td align="left" colspan="2" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 2. MIXTURE COMPONENT
                                                  IDENTITY (Important: DO NOT
                                                  complete this section if you
                                                  completed Section 1.)</p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td align="center" width="5%">
                  <p style="font-size: 8pt">2.1</p>
                </td>
                <td style="font-size: 8pt">
                    Generic Chemical Name Provided by Supplier (Important:
                    Maximum of 70 characters, including numbers, spaces, and
                    punctuation.)
                    <hr />
                    <b style="color: blue;font-size: 9pt; font-family:arial">
                      <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                    </b>
                    <br />
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
                    <tr>
                      <td align="left" colspan="2" style="font-size: 8pt">
                          SECTION 3. ACTIVITIES AND USES OF THE TOXIC CHEMICAL AT THE FACILITY
                          <br />
                          (Important: Check all that apply.)
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td align="center" width="5%">3.1</td>
                      <td>Manufacture the toxic chemical:</td>
                      <td align="center" width="5%">3.2</td>
                      <td>Process the toxic chemical:</td>
                      <td align="center" width="5%">3.3</td>
                      <td>Otherwise use the toxic chemical:</td>
                    </tr>
                    <tr>
                      <td colspan="2" align="center">
                          a. [
                          <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                            <xsl:choose>
                              <xsl:when test="TRI:ChemicalProducedIndicator = 'true'">
                                <span class="answerText">X</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ] Produce b. [
                          <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                            <xsl:choose>
                              <xsl:when test="TRI:ChemicalImportedIndicator = 'true'">
                                <span class="answerText">X</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ] Import
                      </td>
                      <td colspan="4"><br /></td>
                    </tr>
                    <tr>
                      <td colspan="2" style="font-size: 8pt">
                          <dl>
                            <dt>If produce or import:</dt>
                            <dd>
                              c. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalUsedProcessedIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] For on-site use/processing
                            </dd>
                            <dd>
                              d. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalSalesDistributionIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] For sale/distribution
                            </dd>
                            <dd>
                              e. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalByproductIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As a byproduct
                            </dd>
                            <dd>
                              f. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalManufactureImpurityIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As an impurity
                            </dd>
                          </dl>
                      </td>
                      <td colspan="2" style="font-size: 8pt">
                          <dl>
                            <dd>
                              a. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalReactantIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As a reactant
                            </dd>
                            <dd>
                              b. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalFormulationComponentIndicator= 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As a formulation component
                            </dd>
                            <dd>
                              c. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalArticleComponentIndicator= 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As an article component
                            </dd>
                            <dd>
                              d. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalRepackagingIndicator= 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] Repackaging
                            </dd>
                            <dd>
                              e. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalProcessImpurityIndicator= 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As an impurity
                            </dd>
                          </dl>
                      </td>
                      <td colspan="2" style="font-size: 8pt">
                          <dl>
                            <dd>
                              a. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalProcessingAidIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As a chemical processing aid
                            </dd>
                            <dd>
                              b. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalManufactureAidIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] As a manufacturing aid
                            </dd>
                            <dd>
                              c. [
                              <xsl:for-each select="TRI:ChemicalActivitiesAndUses">
                                <xsl:choose>
                                  <xsl:when test="TRI:ChemicalAncillaryUsageIndicator= 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:for-each>
                              ] Ancillary or other use
                            </dd>
                          </dl>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <tr>
                      <td align="left" colspan="2" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 4. MAXIMUM AMOUNT OF
                                                  THE TOXIC CHEMICAL ON-SITE AT
                                                  ANY TIME DURING THE CALENDAR
                                                  YEAR </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td align="right" width="5%">
                  <p style="font-size: 8pt">4.1</p>
                </td>
                <td style="font-size: 8pt">
                  [
                  <span class="answerText">
                    <xsl:if test="not(string-length(TRI:MaximumChemicalAmountCode)=0)">
                      <xsl:value-of select="TRI:MaximumChemicalAmountCode"/>
                    </xsl:if>
                  &#160;</span>
                  ] (Enter two-digit code from instruction package.)
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <tr>
                      <td align="left" colspan="2" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 5.QUANTITY OF THE
                                                  TOXIC CHEMICAL ENTERING EACH
                                                  ENVIRONMENTAL MEDIUM ON-SITE </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                         cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td colspan="3"></td>
                      <td>
                        <p style="font-size: 8pt">
                          A. Total Release (pounds/year*)
                          <br/>
                          (Enter range code or estimate**)
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          B. Basis of Estimate
                          <br/>
                          (Enter code)
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">C. Percent from Stormwater</p>
                      </td>
                    </tr>
                    <tr>
                      <td align="center" width="5%">
                        <p style="font-size: 8pt">5.1</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Fugitive or non-point
                          <br/>
                          air emissions
                        </p>
                      </td>
                      <td style="font-size: 8pt">
                        <p style="font-size: 8pt">
                          NA [
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR FUG'">
                                <xsl:choose>
                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ]
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR FUG'">
                                <span class="answerText">
                                  <xsl:choose>
                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
                                      <br/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                      <br/>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR FUG'">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td style="background-color: gray">&#160;</td>
                    </tr>
                    <tr>
                      <td align="center" width="5%">
                        <p style="font-size: 8pt">5.2</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Stack or point
                          <br/>
                          air emissions
                        </p>
                      </td>
                      <td style="font-size: 8pt">
                        <p style="font-size: 8pt">
                          NA [
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR STACK'">
                                <xsl:choose>
                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ]
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR STACK'">
                                <span class="answerText">
                                  <xsl:choose>
                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
                                      <br/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                      <br/>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'AIR STACK'">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td style="background-color: gray">&#160;</td>
                    </tr>
                    <tr>
                      <td align="center" width="5%">
                        <p style="font-size: 8pt">5.3</p>
                      </td>
	                  <td align="left">
	                        <p style="font-size: 8pt">
	                          Discharges to receiving streams or
	                          <br/>
	                          water bodies (Enter one name per box)
	                        </p>
	                      </td>
	                      <td>
	                        <xsl:choose>
							  <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
							    &#160;
							  </xsl:when>
							  <xsl:otherwise>
								<p style="font-size: 8pt">
								  NA [
								  <xsl:for-each select="TRI:OnsiteReleaseQuantity">
									<xsl:choose>
									  <xsl:when test="TRI:EnvironmentalMediumCode = 'WATER'">
										<xsl:choose>
										  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
											<span class="answerText">X</span>
										  </xsl:when>
										</xsl:choose>
									  </xsl:when>
									</xsl:choose>
								  </xsl:for-each>
								  ]
								</p>
							  </xsl:otherwise>
							</xsl:choose>
	                  </td>
					  <td colspan="2" style="background-color: gray">&#160;</td>
                      <td style="background-color: gray">&#160;</td>
                    </tr>
                    <tr>
                      <td colspan="2" style="font-size: 9pt" align="center">
                        <p style="font-size: 8pt">Stream or Water Body Name </p>
                      </td>
                      <xsl:choose>
                      	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
                      		<td style="font-size: 9pt" align="center">
                      			<p style="font-size: 8pt">Reach Code (optional)</p>
                      		</td>
                      	</xsl:when>
                      </xsl:choose>
                      <td>
                        <br/>
                      </td>
                      <td>
                        <br/>
                      </td>
                      <td>
                        <br/>
                      </td>
                    </tr>
                    <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                      <xsl:choose>
                        <xsl:when test="TRI:EnvironmentalMediumCode = 'WATER'">
                          <xsl:choose>
                            <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                              <tr>
                                <td align="center" width="5%">
                                  <p style="font-size: 8pt">5.3.1</p>
                                </td>
                                <td colspan="2">
                                  <p style="font-size: 8pt">
                                    <span class="answerText">NA&#160;</span>
                                    <br/>
                                  </p>
                                </td>
                                <td></td>
                                <td></td>
                                <td></td>
                                <xsl:choose>
                                	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
                                		<td></td>
                               	 	</xsl:when>
                                </xsl:choose>
                              </tr>
                            </xsl:when>
                            <xsl:otherwise>
                              <tr>
                                <td align="center" width="5%">
                                  <p style="font-size: 8pt">
                                    5.3.<xsl:value-of select="TRI:WaterStream/TRI:WaterSequenceNumber"/>
                                  </p>
                                </td>
                                <td>
                                  <p style="font-size: 8pt">
                                    <span class="answerText">
                                      <xsl:value-of select="TRI:WaterStream/TRI:StreamName"/>
                                    &#160;</span>
                                    <br/>
                                  </p>
                                </td>
                                <xsl:choose>
	                            <xsl:when test="../TRI:SubmissionReportingYear &gt; '2013'">
	                                	<td>
	                                	  <p style="font-size: 8pt">
	                                	    <span class="answerText">
	                                	      <xsl:value-of select="TRI:WaterStream/TRI:StreamReachCode"/>
	                                	    &#160;</span>
	                                	    <br/>
	                                	  </p>
	                                	</td>
	                                </xsl:when>
                                </xsl:choose>
                                <td>
                                  <p style="font-size: 8pt">
                                    <span class="answerText">
                                      <xsl:choose>
                                        <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
                                          <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                          <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                        </xsl:otherwise>
                                      </xsl:choose>
                                    &#160;</span>
                                    <br/>
                                  </p>
                                </td>
                                <td>
                                  <p style="font-size: 8pt">
                                    <span class="answerText">
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                    &#160;</span>
                                    <br/>
                                  </p>
                                </td>
                                <td>
                                  <p style="font-size: 8pt">
                                    <span class="answerText">
                                      <xsl:choose>
                                      <xsl:when test="TRI:WaterStream/TRI:ReleaseStormWaterNAIndicator = 'true'">NA</xsl:when>
                                      <xsl:otherwise><xsl:value-of select="TRI:WaterStream/TRI:ReleaseStormWaterPercent"/>%</xsl:otherwise>
                                      </xsl:choose>
                                    &#160;</span>
                                    <br/>
                                  </p>
                                </td>
                              </tr>
                            </xsl:otherwise>
                          </xsl:choose>
                        </xsl:when>
                      </xsl:choose>
                    </xsl:for-each>
                  </table>
                </td>
              </tr>
            </table>
          </center>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0"
                 style="font-size: 8pt" border="0">
            <tr>
              <td colspan="2" align="right">
                <p style="font-size: 8pt">*For Dioxin and Dioxin-like Compounds,
                                          report in grams/year</p>
              </td>
            </tr>
            <tr>
              <td width="50%">
                <p style="font-size: 8pt">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		EPA Form 9350-1 (Rev. <xsl:value-of select="$RevisionDateFormR"/>) - Previous editions are obsolete.
                  	</xsl:if>
                </p>
              </td>
              <td width="50%" align="right">
                <p style="font-size: 8pt">**Range Codes: A=1-10 pounds; B=11-499
                                          pounds; C=500-999 pounds.</p>
              </td>
            </tr>
          </table>
          <p style="page-break-before: always">&#160;</p>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0">
            <tr>
             <!--<td width="90%">
                <a name="PG-3_{$formID}"></a>
                <p style="font-size: 8pt">
                  <a href="#PG-1_{$formID}">1</a>
                  <a href="#PG-2_{$formID}">2</a>
                  <a href="#PG-3_{$formID}">3</a>
                  <a href="#PG-4_{$formID}">4</a>
                  <a href="#PG-5_{$formID}">5</a>
                  <a href="#PG-6_{$formID}">Additional Info</a>
                  <xsl:if test="$ScheduleOneNA = 'false'">
                      <a href="#S1_PG-1_{$formID}">Schedule 1</a>
                  </xsl:if>
                </p>
              </td>-->
              <td width="10%">
                <p style="font-size: 8pt">
                  <b>Page 3 of 5 </b>
                </p>
              </td>
            </tr>
          </table>
          <center>
          <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
          </tr>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%"
                   style="font-size: 8pt">
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td width="65%" align="center" style="font-size: 10pt">
                        <p style="font-size: 10pt">
                          <b>EPA FORM R</b>
                          <br/>
                          <b>PART II. CHEMICAL - SPECIFIC INFORMATION (CONTINUED)</b>
                        </p>
                      </td>
                      <td>
                          TRI Facility ID Number
                          <hr />
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                          &#160;</span>
                        <hr />
                        Toxic Chemical, Category, or Generic Name
                        <hr />
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <tr>
                      <td align="left" style="font-size: 8pt" colspan="2">
                        <p style="font-size: 8pt">
                          SECTION 5. QUANTITY OF THE TOXIC CHEMICAL ENTERING
                          EACH ENVIRONMENTAL MEDIUM ON-SITE
                          <font style="font-size: 7pt">(Continued)</font>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                         cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td colspan="2"></td>
                      <td style="font-size: 8pt" align="center">
                        <p style="font-size: 8pt">
                          <b>NA</b>
                        </p>
                      </td>
                      <td align="center">
                        <p style="font-size: 8pt">A. Total Release
                                                  (pounds/year*) (Enter range
                                                  code** or estimate)</p>
                      </td>
                      <td align="center">
                        <p style="font-size: 8pt">B. Basis of Estimate (Enter
                                                  code)</p>
                      </td>
                    </tr>
                    <xsl:choose>
                    	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
                    		<tr>
                    	  		<td width="5%" align="center">
                    	  			<p style="font-size: 8pt">5.4-5.5</p>
                    	  		</td>
                    	  		<td align="left">
                    	  	  		<p style="font-size: 8pt">
                    		      		Disposal to land on-site
                    		      		<br/>
                    		    	</p>
                    		  	</td>
                    		  	<td colspan="3" style="background-color: gray">&#160;</td>
                    		</tr>
                    	</xsl:when>
                    </xsl:choose>
                    <xsl:choose>
          		      <xsl:when test="TRI:SubmissionReportingYear &gt;= '1991' and TRI:SubmissionReportingYear &lt;= '1995'">
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.4</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          UIC Injections Aggregate
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ8795'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ8795'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ8795'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
		                </tr>
	                  </xsl:when>
	                  <xsl:otherwise>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.4.1</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
		                        <xsl:choose>
		                        	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
		                          		Class I Underground
		                          		<br/>
		                          		Injection wells
		                        	</xsl:when>
		                        	<xsl:otherwise>
		                        		Underground Injection onsite
		                        	  	<br/>
		                        	  	to Class I wells
		                        	</xsl:otherwise>
		                        </xsl:choose>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ I'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ I'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                        </p>
	                        <br/>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ I'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                        </p>
	                        <br/>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.4.2</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                        <xsl:choose>
	                        	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
	                        		Class II-V Underground
	                          		<br/>
	                          		Injection wells
								</xsl:when>
								<xsl:otherwise>
									Underground Injection onsite
	                          		<br/>
	                          		to Class II-V wells
								</xsl:otherwise>
	                        </xsl:choose>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ IIV'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ IIV'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'UNINJ IIV'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
		                </tr>
	                  </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="TRI:SubmissionReportingYear &lt; '2014'">
                    	<tr>
                      		<td width="5%" align="center">
                        		<p style="font-size: 8pt">5.5</p>
                      		</td>
                      		<td align="left">
                        		<p style="font-size: 8pt">
                          			Disposal to land on-site
                          			<br/>
                        		</p>
                      		</td>
                      		<td colspan="3" style="background-color: gray">&#160;</td>
                    	</tr>
                    </xsl:if>
                    <xsl:choose>
          		      <xsl:when test="TRI:SubmissionReportingYear &gt;= '1991' and TRI:SubmissionReportingYear &lt;= '1995'">
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.1</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Total Landfill Releases
	                          <br/>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LANDF8795'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LANDF8795'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LANDF8795'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.1.A</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          RCRA subtitle C landfills
	                          <br/>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'RCRA C'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'RCRA C'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'RCRA C'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>

	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.1.B</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Other landfills
	                          <br/>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH LANDF'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH LANDF'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH LANDF'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                 </xsl:otherwise>
                    </xsl:choose>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">5.5.2</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Land treatment/application
                          <br/>
                          farming
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          [
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LAND TREA'">
                                <xsl:choose>
                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ]
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LAND TREA'">
                                <span class="answerText">
                                  <xsl:choose>
                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
                                      <br/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                      <br/>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'LAND TREA'">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <xsl:choose>
          		      <xsl:when test="TRI:SubmissionReportingYear &gt;= '1991' and TRI:SubmissionReportingYear &lt;= '2002'">
          		        <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.3</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Surface impoundment
	                          <br/>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SURF IMP'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SURF IMP'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SURF IMP'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
          		      </xsl:when>
          		      <xsl:otherwise>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.3A</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          RCRA Subtitle C
	                          <br/>
	                          surface impoundments
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3A'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3A'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3A'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">5.5.3B</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Other surface impoundments
	                          <br/>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3B'">
	                                <xsl:choose>
	                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
	                                    <span class="answerText">X</span>
	                                  </xsl:when>
	                                </xsl:choose>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          ]
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3B'">
	                                <span class="answerText">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
	                                      <br/>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
	                                      <br/>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
	                            <xsl:choose>
	                              <xsl:when test="TRI:EnvironmentalMediumCode = 'SI 5.5.3B'">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
	                                &#160;</span>
	                              </xsl:when>
	                            </xsl:choose>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                  </xsl:otherwise>
	                </xsl:choose>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">5.5.4</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Other disposal
                          <br/>
                          <br/>
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          [
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH DISP'">
                                <xsl:choose>
                                  <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                                    <span class="answerText">X</span>
                                  </xsl:when>
                                </xsl:choose>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ]
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH DISP'">
                                <span class="answerText">
                                  <xsl:choose>
                                    <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityMeasure"/>
                                      <br/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                      <br/>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:OnsiteReleaseQuantity">
                            <xsl:choose>
                              <xsl:when test="TRI:EnvironmentalMediumCode = 'OTH DISP'">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:OnsiteWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                &#160;</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                         cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                    	<xsl:choose>
                    		<xsl:when test="TRI:SubmissionReportingYear &lt; '2014'">
                    			<td align="left" colspan="8" style="font-size: 8pt">
                        			<p style="font-size: 8pt">SECTION 6. TRANSFER(S) OF THE
                                                  TOXIC CHEMICAL IN WASTES TO
                                                  OFF-SITE LOCATIONS </p>
                      			</td>
                    		</xsl:when>
                    		<xsl:otherwise>
                    			<td align="left" colspan="10" style="font-size: 8pt">
                        			<p style="font-size: 8pt">SECTION 6. TRANSFER(S) OF THE
                                                  TOXIC CHEMICAL IN WASTES TO
                                                  OFF-SITE LOCATIONS </p>
                      			</td>
                    		</xsl:otherwise>
                    	</xsl:choose>
                    </tr>

                    <xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear &gt; '2010'">
                      <xsl:choose>
	                      <xsl:when test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true' or not(TRI:POTWWasteQuantity)">
		                    <tr>
		                      <td align="left" style="font-size: 8pt;">
		                        <p style="font-size: 8pt">6.1 DISCHARGES TO PUBLICLY
		                                                  OWNED TREATMENT WORKS (POTWs)</p>
		                      </td>
		                      <td align="left" style="font-size: 8pt">
		                        <p style="font-size: 8pt">
		                          NA [
		                            <xsl:if test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true' or not(TRI:POTWWasteQuantity)">
		                              <span class="answerText">X</span>
		                            </xsl:if>
		                          ]
		                        </p>
		                      </td>
		                    </tr>
		                  </xsl:when>
		                  <xsl:otherwise>
		                    <tr>
		                      <td align="left" style="font-size: 8pt;" colspan="3">
		                        <p style="font-size: 8pt">6.1 DISCHARGES TO PUBLICLY
		                                                  OWNED TREATMENT WORKS (POTWs)</p>
		                      </td>
		                      	<xsl:choose>
									<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
											<td align="left" style="font-size: 8pt" colspan="8">  <!-- 2014 -->
		                        				<p style="font-size: 8pt">
		                          				NA [
		                            				<xsl:if test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true' or not(TRI:POTWWasteQuantity)">
		                            					<span class="answerText">X</span>
		                            				</xsl:if>
		                          				]
		                        				</p>
		                      				</td>
								    	</xsl:when>
        								<xsl:otherwise>
											<td align="left" style="font-size: 6pt" colspan="6">  <!-- 2013 Issue here -->
		                        				<p style="font-size: 8pt">
		                          				NA [
		                            				<xsl:if test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true' or not(TRI:POTWWasteQuantity)">
		                            					<span class="answerText">X</span>
		                            				</xsl:if>
		                          				]
		                        				</p>
		                      				</td>
        								</xsl:otherwise>
								</xsl:choose>
		                    </tr>
		                  </xsl:otherwise>
	                  </xsl:choose>
	                </xsl:when>
	                <xsl:otherwise>
						<tr>
						  <td align="left" colspan="8" style="font-size: 8pt">
							<p style="font-size: 8pt">6.1 DISCHARGES TO PUBLICLY
													  OWNED TREATMENT WORKS (POTWs)</p>
						  </td>
						</tr>
						<tr>
						  <td align="left" colspan="8" style="font-size: 8pt">
							<p style="font-size: 8pt">6.1.A Total Quantity
													  Transferred to POTWs and Basis
													  of Estimate</p>
						  </td>
						</tr>
						<tr>
						  <td align="left" colspan="4">
							<p style="font-size: 8pt">
							  6.1.A.1 Total Transfers (pounds/year*)
							  <br/>
							  (Enter range code** or estimate)
							</p>
						  </td>
						  <td align="center" colspan="4">
							<p style="font-size: 8pt">
							  6.1.A.2 Basis of Estimate
							  <br/>
							  (Enter code)
							</p>
						  </td>
						</tr>
						<tr>
						  <td align="center" colspan="4">
							<p style="font-size: 8pt">
							  <span class="answerText">
								<xsl:choose>
								  <xsl:when test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">NA</xsl:when>
								  <xsl:otherwise>
									<xsl:choose>
									  <xsl:when test="TRI:POTWWasteQuantity/TRI:WasteQuantityMeasure >= '0'">
										<xsl:value-of select="TRI:POTWWasteQuantity/TRI:WasteQuantityMeasure"/>
										<br/>
									  </xsl:when>
									  <xsl:otherwise>
										<xsl:value-of select="TRI:POTWWasteQuantity/TRI:WasteQuantityRangeCode"/>
										<br/>
									  </xsl:otherwise>
									</xsl:choose>
								  </xsl:otherwise>
								</xsl:choose>
							  </span>
							  <br/>
							</p>
						  </td>
						  <td align="center" colspan="4">
							<p style="font-size: 8pt">
							  <span class="answerText">
								<xsl:value-of select="TRI:POTWWasteQuantity/TRI:QuantityBasisEstimationCode"/>
							  </span>
							  <br/>
							</p>
						  </td>
						</tr>
                      </xsl:otherwise>
                    </xsl:choose>

                    <xsl:for-each select="TRI:TransferLocation">
                      <xsl:if test="TRI:POTWIndicator = 'true'">
                        <tr>
                          <td align="center" colspan="2">
                            <p style="font-size: 8pt">
                              <br/>
                               <xsl:choose>
							     <xsl:when test="../TRI:SubmissionReportingYear &gt; '2010'">
								  6.1.<xsl:value-of select="TRI:TransferLocationSequenceNumber"/>
							     </xsl:when>
							     <xsl:otherwise>
							      6.1.B.<xsl:value-of select="TRI:TransferLocationSequenceNumber"/>
							     </xsl:otherwise>
							   </xsl:choose>
                              <br/>
                              <u>POTW Name</u>
                              <br/>
                            </p>
                          </td>
								<xsl:choose>
									<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
                          				<td align="left" colspan="8"> <!-- 2014  -->
                            				<p style="font-size: 8pt">
                              				<span class="answerText">
                                			<xsl:value-of select="sc:FacilitySiteName"/>
                              				&#160;</span>
                              				<br/>
                            				</p>
                          				</td>

								    </xsl:when>
        							<xsl:otherwise>
			                          <td align="left" colspan="6"> <!-- 2013  -->
                            			<p style="font-size: 8pt">
                              				<span class="answerText">
                                		<xsl:value-of select="sc:FacilitySiteName"/>
                              			&#160;</span>
                              			<br/>
                            			</p>
                          			</td>
     								</xsl:otherwise>
							</xsl:choose>
                        </tr>
                        <tr>
                          <td align="center" colspan="2" width="25%">
                            <p style="font-size: 8pt">
                              <br/>
                              POTW Address
                              <br/>
                            </p>
                          </td>
                          <xsl:choose>
                          	<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
					                    <td align="left" colspan="8"> <!-- 2014  -->
                            				<p style="font-size: 8pt">
                              				<span class="answerText">
                                			<xsl:value-of select="sc:LocationAddress/sc:LocationAddressText"/>
				                              &#160; </span>
                              				<br/>
                            				</p>
                          				</td>
                          	</xsl:when>
                          	<xsl:otherwise>
                          	            <td align="left" colspan="6"> <!-- 2013  -->
                            				<p style="font-size: 8pt">
                              				<span class="answerText">
                                				<xsl:value-of select="sc:LocationAddress/sc:LocationAddressText"/>
                              					&#160; </span>
                              					<br/>
                            				</p>
                          				</td>
                          	</xsl:otherwise>
                          </xsl:choose>
                        </tr>
                        <tr>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <br/>
                              City
                              <br/>
                            </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:LocalityName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                          <xsl:choose>
							<xsl:when test="../TRI:SubmissionReportingYear &lt; '2011'">
							  <td width="5%">
								<p style="font-size: 8pt">
								  <br/>
								  State
								  <br/>
								</p>
							  </td>
							  <td>
								<p style="font-size: 8pt">
								  <span class="answerText">
									<xsl:value-of select="sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
								  </span>
                              <br/>
                            </p>
                          </td>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <br/>
                              County
                              <br/>
                            </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                              &#160;</span>
	  <br/>
								</p>
							  </td>
                          </xsl:when>
						  <xsl:otherwise>
						  <td width="5%">
								<p style="font-size: 8pt">
								  <br/>
								  County
								  <br/>
								</p>
							  </td>
							  <td>
								<p style="font-size: 8pt">
								  <span class="answerText">
									<xsl:value-of select="sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
								  </span>
                              <br/>
                            </p>
                          </td>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <br/>
                              State
                              <br/>
                            </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
						  </xsl:otherwise>
						</xsl:choose>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <br/>
                              ZIP
                              <br/>
                            </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:AddressPostalCode"/>
                              </span>
                              <br/>
                            </p>
                          </td>
                               <!-- Country column for RY-2014 and later -->
                          <xsl:if test="../TRI:SubmissionReportingYear &gt; '2013' ">
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <br/>
                              Country
                              <br/>
                              (Non-US)
                            </p>
                          </td>

                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:CountryIdentity/sc:CountryName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                          </xsl:if>

                        </tr>

                         <xsl:if test="../TRI:SubmissionReportingYear &gt; '2010'">
	                        <tr>
		                            <xsl:choose>
										<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
											<td align="center" colspan="5">  <!-- 2014-->
											<p style="font-size: 8pt">
		                          				A. Quantity Transferred to this POTW
		                          				<br/>
		                          				(pounds/year*) (Enter range code**or estimate)
		                        			</p>
		                      				</td>
								    	</xsl:when>
        								<xsl:otherwise>
											<td align="center" colspan="4">  <!-- 2013 -->
											<p style="font-size: 8pt">
		                          				A. Quantity Transferred to this POTW
		                          				<br/>
		                          				(pounds/year*) (Enter range code**or estimate)
		                        			</p>
		                      				</td>

        								</xsl:otherwise>
       								</xsl:choose>
		                           <xsl:choose>
										<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
											<td align="center" colspan="5"> <!-- 2014-->
											<p style="font-size: 8pt">
		                          			B. Basis of Estimate
		                          				<br/>
		                          			(Enter code)
		                        			</p>
		                      				</td>
								    	</xsl:when>
        								<xsl:otherwise>
											<td align="center" colspan="4"> <!-- 2013 -->
											<p style="font-size: 8pt">
		                          			B. Basis of Estimate
		                          				<br/>
		                          			(Enter code)
		                        			</p>
		                      				</td>

        								</xsl:otherwise>
       								</xsl:choose>
		                    </tr>

		                    <xsl:variable name="locationSequence" select="TRI:TransferLocationSequenceNumber"/>
		                    <xsl:for-each select="../TRI:POTWWasteQuantity">
			                    <xsl:if test="TRI:POTWSequenceNumber = $locationSequence">
				                 <tr>
       								<xsl:choose>
									<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
									<td align="center" colspan="5">  <!-- 2014 -->
			                        <p style="font-size: 8pt">
			                          <span class="answerText">
			                            <xsl:choose>
			                              <xsl:when test="TRI:WasteQuantityNAIndicator = 'true'">NA</xsl:when>
			                              <xsl:otherwise>
			                                <xsl:choose>
			                                  <xsl:when test="TRI:WasteQuantityMeasure >= '0'">
			                                    <xsl:value-of select="TRI:WasteQuantityMeasure"/>
			                                  </xsl:when>
			                                  <xsl:otherwise>
			                                    <xsl:value-of select="TRI:WasteQuantityRangeCode"/>
			                                    <br/>
			                                  </xsl:otherwise>
			                                </xsl:choose>
			                              </xsl:otherwise>
			                            </xsl:choose>
			                          &#160;</span>
			                          <br/>
			                        </p>
			                      </td>
								  </xsl:when>
        						  <xsl:otherwise>
									<td align="center" colspan="4">  <!-- 2013 -->
										<p style="font-size: 8pt">
			                          <span class="answerText">
			                            <xsl:choose>
			                              <xsl:when test="TRI:WasteQuantityNAIndicator = 'true'">NA</xsl:when>
			                              <xsl:otherwise>
			                                <xsl:choose>
			                                  <xsl:when test="TRI:WasteQuantityMeasure >= '0'">
			                                    <xsl:value-of select="TRI:WasteQuantityMeasure"/>
			                                  </xsl:when>
			                                  <xsl:otherwise>
			                                    <xsl:value-of select="TRI:WasteQuantityRangeCode"/>
			                                    <br/>
			                                  </xsl:otherwise>
			                                </xsl:choose>
			                              </xsl:otherwise>
			                            </xsl:choose>
			                          &#160;</span>
			                          <br/>
			                        </p>
			                      </td>
    								</xsl:otherwise>
								</xsl:choose>
								<xsl:choose>
										<xsl:when test="../TRI:SubmissionReportingYear &gt; '2013' ">
											<td align="center" colspan="5"> <!-- 2014 -->
					                        <p style="font-size: 8pt">
					                          <span class="answerText">
			                            	<xsl:value-of select="TRI:QuantityBasisEstimationCode"/>
			                          			&#160;</span>
			                          			<br/>
			                        		</p>
			                      			</td>
								    	</xsl:when>
        								<xsl:otherwise>
											<td align="center" colspan="4"> <!-- 2013 -->
					                        <p style="font-size: 8pt">
					                          <span class="answerText">
			                            	<xsl:value-of select="TRI:QuantityBasisEstimationCode"/>
			                          			&#160;</span>
			                          			<br/>
			                        		</p>
			                      			</td>

        								</xsl:otherwise>
									</xsl:choose>
			                    </tr>
			                   </xsl:if>
		                  	</xsl:for-each>
	                    </xsl:if>


                      </xsl:if>
                    </xsl:for-each>
                  </table>
                </td>
              </tr>
            </table>
          </center>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0">
            <tr>
              <td colspan="2" align="right">
                <p style="font-size: 8pt">*For Dioxin and Dioxin-like Compounds, report in grams/year</p>
              </td>
            </tr>
            <tr>
              <td width="50%">
                <p style="font-size: 8pt">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		EPA Form 9350-1 (Rev. <xsl:value-of select="$RevisionDateFormR"/>) - Previous editions are obsolete.
                  	</xsl:if>
                </p>
              </td>
              <td width="50%" align="right">
                <p style="font-size: 8pt">**Range Codes: A=1-10 pounds; B=11-499 pounds; C=500-999 pounds.</p>
              </td>
            </tr>
          </table>
          <p style="page-break-before: always">&#160;</p>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0" rules="all">
            <tr>
            <!--<td width="90%">
                <a name="PG-4_{$formID}"></a>
                <p style="font-size: 8pt">
                  <a href="#PG-1_{$formID}">1</a>
                  <a href="#PG-2_{$formID}">2</a>
                  <a href="#PG-3_{$formID}">3</a>
                  <a href="#PG-4_{$formID}">4</a>
                  <a href="#PG-5_{$formID}">5</a>
                  <a href="#PG-6_{$formID}">Additional Info</a>
                  <xsl:if test="$ScheduleOneNA = 'false'">
                      <a href="#S1_PG-1_{$formID}">Schedule 1</a>
                  </xsl:if>
                </p>
              </td>-->
              <td width="10%">
                <p style="font-size: 8pt">
                  <b>Page 4 of 5 </b>
                </p>
              </td>
            </tr>
          </table>
          <center>
          <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
          </tr>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%"
                   style="font-size: 8pt">
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td width="65%" align="center" style="font-size: 10pt">
                        <p style="font-size: 10pt">
                          <b>EPA FORM R</b>
                          <br/>
                          <b>PART II. CHEMICAL - SPECIFIC INFORMATION (CONTINUED)</b>
                        </p>
                      </td>
                      <td>
                          TRI Facility ID Number
                          <hr />
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                          &#160;</span>
                        <hr />
                        Toxic Chemical, Category, or Generic Name
                        <hr />
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
               <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" border="0" style="font-size: 8pt" cellspacing="0"
                         cellpadding="1">
                    <xsl:if test="TRI:SubmissionReportingYear &gt; '2010'">
	                    <tr>
	                      <td align="left" style="font-size: 8pt; width: 405px; border-right: ridge 2px;">
	                        <p style="font-size: 8pt">SECTION 6.2 TRANSFERS TO OTHER OFF-SITE LOCATIONS </p>
	                      </td>
	                      <td align="left" style="font-size: 8pt;">
	                        <p style="font-size: 8pt">
		                      NA [

		                      	  <xsl:if test="not(TRI:TransferLocation)">

			                              <span class="answerText">X</span>

		                      	  </xsl:if>

	                      	 ]
		                    </p>
	                      </td>
	                    </tr>
                    </xsl:if>
                    <xsl:if test="TRI:SubmissionReportingYear &lt; '2011'">
                      <tr>
                    	<td align="left" style="font-size: 8pt;">
	                        <p style="font-size: 8pt">SECTION 6.2 TRANSFERS TO OTHER OFF-SITE LOCATIONS </p>
	                    </td>
	                  </tr>
                    </xsl:if>
                  </table>
                </td>
              </tr>
              <xsl:for-each select="TRI:TransferLocation">
                <xsl:if test="TRI:POTWIndicator = 'false'">
                  <tr>
                    <td colspan="2">
                      <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                        <tr>
                          <xsl:choose>
                            <xsl:when test="../TRI:TransferLocation[1]/TRI:POTWIndicator = 'true'">
                              <td width="55%" style="font-size: 10pt">
                                <p style="font-size: 8pt">
                                  6.2.<xsl:value-of select="count(preceding-sibling::TRI:TransferLocation) - 1"/>
                                  Off-Site EPA Identification Number (RCRA ID
                                  No.)
                                </p>
                              </td>
                            </xsl:when>
                            <xsl:otherwise>
                              <td width="55%" style="font-size: 10pt">
                                <p style="font-size: 8pt">
                                  6.2.<xsl:value-of select="count(preceding-sibling::TRI:TransferLocation) + 1"/>
                                  Off-Site EPA Identification Number (RCRA ID No.)
                                </p>
                              </td>
                            </xsl:otherwise>
                          </xsl:choose>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="TRI:RCRAIdentificationNumber"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td width="25%" style="text-indent: 2em">
                            <p style="font-size: 8pt">Off-Site Location Name: </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:FacilitySiteName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td width="25%" style="text-indent: 2em">
                            <p style="font-size: 8pt">Off-Site Address: </p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:LocationAddressText"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                             cellspacing="0" cellpadding="1" frame="void">
                        <tr>
                          <td width="5%">
                            <p style="font-size: 8pt">City</p>
                          </td>
                          <td width="20%">
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:LocalityName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
						<xsl:choose>
						<xsl:when test="../TRI:SubmissionReportingYear &lt; '2011'">
						  <td width="5%">
							<p style="font-size: 8pt">State</p>
						  </td>
						  <td width="5%">
							<p style="font-size: 8pt">
							  <span class="answerText">
								<xsl:value-of select="sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
							  </span>
							  <br/>
							</p>
						  </td>
						  <td width="10%">
							<p style="font-size: 8pt">County</p>
						  </td>
						  <td width="20%">
							<p style="font-size: 8pt">
							  <span class="answerText">
								<xsl:value-of select="sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
							  </span>
							  <br/>
							</p>
						  </td>
						</xsl:when>
						<xsl:otherwise>
                          <td width="10%">
                            <p style="font-size: 8pt">County</p>
                          </td>
                          <td width="20%">
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                              </span>
                              <br/>
                            </p>
                          </td>
                          <td width="5%">
                            <p style="font-size: 8pt">State</p>
                          </td>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
						</xsl:otherwise>
						</xsl:choose>
                          <td width="5%">
                            <p style="font-size: 8pt">ZIP</p>
                          </td>
                          <td width="15%">
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:AddressPostalCode"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                          <td width="10%">
                            <p style="font-size: 8pt">
                              Country
                              <br/>
                              (Non-US)
                            </p>
                          </td>
                          <td width="5%">
                            <p style="font-size: 8pt">
                              <span class="answerText">
                                <xsl:value-of select="sc:LocationAddress/sc:CountryIdentity/sc:CountryName"/>
                              &#160;</span>
                              <br/>
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td colspan="6" style="text-indent: 4em">
                            <p style="font-size: 8pt">Is location under control
                                                      of reporting facility or
                                                      parent company?</p>
                          </td>
                          <td colspan="4" style="font-size: 8pt">
                            <p style="font-size: 8pt">
                              <br/>
                              [
                              <xsl:choose>
                                <xsl:when test="sc:FacilitySiteName = 'NA'"></xsl:when>
                                <xsl:otherwise>
                                  <xsl:choose>
                                    <xsl:when test="TRI:ControlledLocationIndicator = 'true'">
                                      <span class="answerText">X</span>
                                    </xsl:when>
                                  </xsl:choose>
                                </xsl:otherwise>
                              </xsl:choose>
                              ] Yes [
                              <xsl:choose>
                                <xsl:when test="sc:FacilitySiteName = 'NA'"></xsl:when>
                                <xsl:otherwise>
                                  <xsl:choose>
                                    <xsl:when test="TRI:ControlledLocationIndicator = 'false'">
                                      <span class="answerText">X</span>
                                    </xsl:when>
                                  </xsl:choose>
                                </xsl:otherwise>
                              </xsl:choose>
                              ] No
                              <br/>
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="2">
                      <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                             cellspacing="0" cellpadding="1" frame="void">
                        <tr>
                          <td align="center">
                            <p style="font-size: 8pt">
                              A. Total Transfer (pounds/year*)
                              <br/>
                              (Enter range code** or estimate)
                            </p>
                          </td>
                          <td align="center">
                            <p style="font-size: 8pt">
                              B. Basis of Estimate
                              <br/>
                              (Enter code)
                            </p>
                          </td>
                          <td align="center">
                            <p style="font-size: 8pt">
                              C. Type of Waste Treatment/Disposal/
                              <br/>
                              Recycling/Energy Recovery (Enter code)
                            </p>
                          </td>
                        </tr>
                        <xsl:for-each select="TRI:TransferQuantity">
                          <tr style="height: 25px">
                            <td style="text-indent: 2em">
                              <p style="font-size: 8pt">
                                <xsl:value-of select="count(preceding-sibling::TRI:TransferQuantity) + 1"/>
                                .
                                <span class="answerText">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TransferWasteQuantity/TRI:WasteQuantityMeasure[.!='']">
                                      <xsl:value-of select="TRI:TransferWasteQuantity/TRI:WasteQuantityMeasure"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <xsl:value-of select="TRI:TransferWasteQuantity/TRI:WasteQuantityRangeCode"/>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                &#160;</span>
                              </p>
                            </td>
                            <td style="text-indent: 1em">
                              <p style="font-size: 8pt">
                                <xsl:value-of select="count(preceding-sibling::TRI:TransferQuantity) + 1"/>
                                .
                                <span class="answerText">
                                  <xsl:value-of select="TRI:TransferWasteQuantity/TRI:QuantityBasisEstimationCode"/>
                                &#160;</span>
                              </p>
                            </td>
                            <td style="text-indent: 2em">
                              <p style="font-size: 8pt">
                                <xsl:value-of select="count(preceding-sibling::TRI:TransferQuantity) + 1"/>
                                .
                                <span class="answerText">
                                  <xsl:value-of select="TRI:WasteManagementTypeCode"/>
                                &#160;</span>
                              </p>
                            </td>
                          </tr>
                        </xsl:for-each>
                      </table>
                    </td>
                  </tr>
                </xsl:if>
              </xsl:for-each>
              <tr>
                <td colspan="2">
                  <xsl:choose>
                    <xsl:when test="TRI:SubmissionReportingYear &gt;= '1991' and TRI:SubmissionReportingYear &lt; '2005'">
                      <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
	                         cellspacing="0" cellpadding="1" frame="void">
	                    <tr>
	                      <td colspan="7" style="font-size: 8pt">
	                        <p style="font-size: 8pt">SECTION 7A. ONSITE WASTE TREATMENT METHODS AND EFFICIENCY</p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td colspan="7">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:choose>
	                            <xsl:when test="TRI:WasteTreatmentNAIndicator = 'true'">
	                              <span class="answerText">X</span>
	                            </xsl:when>
	                            <xsl:otherwise></xsl:otherwise>
	                          </xsl:choose>
	                          ] Not Applicable (NA) - Check here if no on-site waste
	                          treatment is applied to any waste stream containing
	                          the toxic chemical or chemical category.
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="15%" align="center">
	                        <p style="font-size: 8pt">
	                          a. General
	                          <br/>
	                          Waste Stream
	                          <br/>
	                          (enter code)
	                        </p>
	                      </td>
	                      <td width="45%" align="center">
	                        <p style="font-size: 8pt">
	                          b. Waste Treatment Method(s) Sequence
	                          <br/>
	                          [enter 3-character code(s)]
	                        </p>
	                      </td>
	                      <td align="center">
	                      	<p style="font-size: 8pt">
	                          c. Influent Concentration
	                        </p>
	                      </td>
	                      <td align="center">
	                        <p style="font-size: 8pt">
	                          d. % Waste Treatment
	                          <br/>
	                          Efficiency
	                        </p>
	                      </td>
	                      <td align="center">
	                        <p style="font-size: 8pt">
	                          e. Based on
	                          <br/>
	                          Operating Data?
	                        </p>
	                      </td>
	                    </tr>
	                    <xsl:choose>
	                      <xsl:when test="TRI:WasteTreatmentNAIndicator = 'true'"></xsl:when>
	                      <xsl:otherwise>
	                        <xsl:for-each select="TRI:WasteTreatmentDetails">
	                          <tr>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  a
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                            <td width="45%" align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  b
	                                </b>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  c
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  d
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  e
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                          </tr>
	                          <tr>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:WasteStreamTypeCode"/>
	                                &#160;</span>
	                              </p>
	                            </td>
	                            <td width="45%" align="center">
	                              <p style="font-size: 8pt">
	                                <xsl:for-each select="TRI:WasteTreatmentMethod">
	                                  <xsl:value-of select="TRI:WasteTreatmentSequenceNumber + 1"/>
	                                  :
	                                  <span class="answerText">
	                                    <xsl:value-of select="TRI:WasteTreatmentMethodCode"/>
	                                    <xsl:text> </xsl:text>
	                                  &#160;</span>
	                                </xsl:for-each>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <span class="answerText">
	                                <xsl:value-of select="TRI:InfluentConcentrationRangeCode"/>
	                              &#160;</span>
	                            </td>
	                            <td align="center">
	                              <span class="answerText">
	                                <xsl:value-of select="TRI:TreatmentEfficiencyEstimatePercent"/>
	                              &#160;</span>
	                            </td>
	                            <td align="center">
	                              <span class="answerText">
	                                <xsl:choose>
		                              <xsl:when test="TRI:OperatingDataIndicator = 'true'">
		                                <span class="answerText">X</span>
		                              </xsl:when>
		                              <xsl:otherwise></xsl:otherwise>
		                            </xsl:choose>
	                              &#160;</span>
	                            </td>
	                          </tr>
	                        </xsl:for-each>
	                      </xsl:otherwise>
	                    </xsl:choose>
	                  </table>
                    </xsl:when>
                    <xsl:otherwise>
	                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
	                         cellspacing="0" cellpadding="1" frame="void">
	                    <tr>
	                      <td colspan="5" style="font-size: 8pt">
	                        <p style="font-size: 8pt">SECTION 7A. ONSITE WASTE TREATMENT METHODS AND EFFICIENCY</p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td colspan="5">
	                        <p style="font-size: 8pt">
	                          [
	                          <xsl:choose>
	                            <xsl:when test="TRI:WasteTreatmentNAIndicator = 'true'">
	                              <span class="answerText">X</span>
	                            </xsl:when>
	                            <xsl:otherwise></xsl:otherwise>
	                          </xsl:choose>
	                          ] Not Applicable (NA) - Check here if no on-site waste
	                          treatment is applied to any waste stream containing
	                          the toxic chemical or chemical category.
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="15%" align="center">
	                        <p style="font-size: 8pt">
	                          a. General
	                          <br/>
	                          Waste Stream
	                          <br/>
	                          (enter code)
	                        </p>
	                      </td>
	                      <td width="45%" align="center">
	                        <p style="font-size: 8pt">
	                          b. Waste Treatment Method(s) Sequence
	                          <br/>
	                          [enter 3-character code(s)]
	                        </p>
	                      </td>
	                      <td align="center">
	                        <p style="font-size: 8pt">
	                          d. Waste Treatment
	                          <br/>
	                          Efficiency
	                          <br/>
	                          Estimate
	                        </p>
	                      </td>
	                    </tr>
	                    <xsl:choose>
	                      <xsl:when test="TRI:WasteTreatmentNAIndicator = 'true'"></xsl:when>
	                      <xsl:otherwise>
	                        <xsl:for-each select="TRI:WasteTreatmentDetails">
	                          <tr>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  a
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                            <td width="45%" align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  b
	                                </b>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <b style="color: black;font-size: 9pt">
	                                  7A.
	                                  <xsl:value-of select="count(preceding-sibling::TRI:WasteTreatmentDetails) + 1"/>
	                                  d
	                                </b>
	                                <br/>
	                              </p>
	                            </td>
	                          </tr>
	                          <tr>
	                            <td align="center">
	                              <p style="font-size: 8pt">
	                                <span class="answerText">
	                                  <xsl:value-of select="TRI:WasteStreamTypeCode"/>
	                                &#160;</span>
	                              </p>
	                            </td>
	                            <td width="45%" align="center">
	                              <p style="font-size: 8pt">
	                                <xsl:for-each select="TRI:WasteTreatmentMethod">
	                                  <xsl:value-of select="TRI:WasteTreatmentSequenceNumber + 1"/>
	                                  :
	                                  <span class="answerText">
	                                    <xsl:value-of select="TRI:WasteTreatmentMethodCode"/>
	                                    <xsl:text> </xsl:text>
	                                  &#160;</span>
	                                </xsl:for-each>
	                              </p>
	                            </td>
	                            <td align="center">
	                              <span class="answerText">
	                                <xsl:value-of select="TRI:TreatmentEfficiencyRangeCode"/>
	                              &#160;</span>
	                            </td>
	                          </tr>
	                        </xsl:for-each>
	                      </xsl:otherwise>
	                    </xsl:choose>
	                  </table>
	                </xsl:otherwise>
                  </xsl:choose>
                </td>
              </tr>
            </table>
          </center>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0">
            <tr>
              <td colspan="2" align="right">
                <p style="font-size: 8pt">*For Dioxin and Dioxin-like Compounds,
                                          report in grams/year</p>
              </td>
            </tr>
            <tr>
              <td width="50%">
                <p style="font-size: 8pt">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		EPA Form 9350-1 (Rev. <xsl:value-of select="$RevisionDateFormR"/>) - Previous editions are obsolete.
                  	</xsl:if>
                </p>
              </td>
              <td width="50%" align="right">
                <p style="font-size: 8pt">**Range Codes: A=1-10 pounds; B=11-499
                                          pounds; C=500-999 pounds.</p>
              </td>
            </tr>
          </table>
          <p style="page-break-before: always">&#160;</p>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0">
            <tr>
             <!--<td width="90%">
                <a name="PG-5_{$formID}"></a>
                <p style="font-size: 8pt">
                  <a href="#PG-1_{$formID}">1</a>
                  <a href="#PG-2_{$formID}">2</a>
                  <a href="#PG-3_{$formID}">3</a>
                  <a href="#PG-4_{$formID}">4</a>
                  <a href="#PG-5_{$formID}">5</a>
                  <a href="#PG-6_{$formID}">Additional Info</a>
                  <xsl:if test="$ScheduleOneNA = 'false'">
                      <a href="#S1_PG-1_{$formID}">Schedule 1</a>
                  </xsl:if>
                </p>
              </td>-->
              <td width="10%">
                <p style="font-size: 8pt">
                  <b>Page 5 of 5 </b>
                </p>
              </td>
            </tr>
          </table>
          <center>
          <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
          </tr>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%"
                   style="font-size: 8pt">
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td width="65%" align="center" style="font-size: 10pt">
                        <p style="font-size: 10pt">
                          <b>EPA FORM R</b>
                          <br/>
                          <b>PART II. CHEMICAL - SPECIFIC INFORMATION (CONTINUED)</b>
                        </p>
                      </td>
                      <td>
                          TRI Facility ID Number
                          <hr />
                          <span class="answerText">
                            <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                          &#160;</span>
                        <hr />
                        Toxic Chemical, Category, or Generic Name
                        <hr />
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0" cellpadding="1" border="0" frame="void">
                    <tr>
                      <td align="left" colspan="8">SECTION 7B. ON-SITE ENERGY RECOVERY PROCESSES</td>
                    </tr>
                    <tr>
                      <td align="left" colspan="8">
                          [
                          <xsl:for-each select="TRI:OnsiteRecoveryProcess">
                            <xsl:choose>
                              <xsl:when test="TRI:EnergyRecoveryNAIndicator = 'true'">
                                <span class="answerText">X</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ] NA - Check here if no on-site
                          energy recovery is applied to any waste
                          <br />
                          stream containing the toxic chemical or chemical
                          category.
                      </td>
                    </tr>
                    <tr>
                      <td align="left" colspan="8">
                        <p style="font-size: 8pt">
                          Energy Recovery Methods [Enter 3-character code(s)]
                          <br/>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td align="left" colspan="8">
                      <xsl:for-each select="TRI:OnsiteRecoveryProcess">
                        <xsl:choose>
                          <xsl:when test="TRI:EnergyRecoveryNAIndicator = 'true'"></xsl:when>
                          <xsl:otherwise>
                            <xsl:for-each select="TRI:EnergyRecoveryMethodCode">
                              <span style="border: solid; border-width: thin; padding-left:20px; padding-right:20px;">
                                  <xsl:value-of select="count(preceding-sibling::*) + 1"/>.
                                  <span class="answerText">
                                    <xsl:value-of select="."/>
                                  &#160;</span>
                              &#160;</span>&#160;&#160;
                            </xsl:for-each>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:for-each>
                      &#160;
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0" cellpadding="1" border="0" frame="void">
                    <tr>
                      <td align="left" colspan="6">
                        SECTION 7C. ON-SITE RECYCLING PROCESSES
                      </td>
                    </tr>
                    <tr>
                      <td align="left" colspan="6">
                          [
                          <xsl:for-each select="TRI:OnsiteRecyclingProcess">
                            <xsl:choose>
                              <xsl:when test="TRI:OnsiteRecyclingNAIndicator = 'true'">
                                <span class="answerText">X</span>
                              </xsl:when>
                            </xsl:choose>
                          </xsl:for-each>
                          ] NA - Check here if no on-site
                          recycling is applied to any waste
                          <br/>
                          stream containing the toxic chemical or chemical
                          category.
                      </td>
                    </tr>
                    <tr>
                      <td align="left" colspan="6">
                        <p style="font-size: 8pt">
                          Recycling Methods [Enter 3-character code(s)]
                          <br/>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td>
                      <xsl:for-each select="TRI:OnsiteRecyclingProcess">
                        <xsl:choose>
                          <xsl:when test="TRI:OnsiteRecyclingNAIndicator = 'true'"></xsl:when>
                          <xsl:otherwise>
                            <xsl:for-each select="TRI:OnsiteRecyclingMethodCode">
                              <span style="border: solid; border-width: thin; padding-left: 20px; padding-right:20px; width: 85px;">
                                  <xsl:value-of select="count(preceding-sibling::*) + 1"/>.
                                  <span class="answerText">
                                    <xsl:value-of select="."/>
                                  &#160;</span>
                              &#160;</span>&#160;&#160;
                            </xsl:for-each>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:for-each>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                    <tr>
                      <td colspan="6" align="left" style="font-size: 8pt">
                        <p style="font-size: 8pt">SECTION 8. SOURCE REDUCTION AND WASTE MANAGEMENT</p>
                      </td>
                    </tr>
                    <tr>
                      <td colspan="2"></td>
                      <td align="center">
                        <p style="font-size: 8pt">
                          Column A
                          <br />
                          Prior Year
                          <br />
                          (pounds/year*)
                        </p>
                      </td>
                      <td align="center">
                        <p style="font-size: 8pt">
                          Column B
                          <br/>
                          Current Reporting Year
                          <br/>
                          (pounds/year*)
                        </p>
                      </td>
                      <td align="center">
                        <p style="font-size: 8pt">
                          Column C
                          <br/>
                          Following Year
                          <br/>
                          (pounds/year*)
                        </p>
                      </td>
                      <td align="center">
                        <p style="font-size: 8pt">
                          Column D
                          <br/>
                          Second Following Year
                          <br/>
                          (pounds/year*)
                        </p>
                      </td>
                    </tr>
                    <xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear &gt;= '1991' and TRI:SubmissionReportingYear &lt;= '2002'">
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">8.1</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Quantity Released
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
                      </xsl:when>
                      <xsl:otherwise>
	                    <tr>
	                    	<xsl:choose>
	                    		<xsl:when test="TRI:SubmissionReportingYear &lt; '2014'">
	                    			<td width="5%" align="center">
	                        			<p style="font-size: 8pt">8.1</p>
	                      			</td>
	                      			<td align="center">
	                        			<p style="font-size: 8pt">&#160;</p>
	                      			</td>
	                    		</xsl:when>
	                    		<xsl:otherwise>
	                    			<td width="5%" align="center" colspan="2">
	                        			<p style="font-size: 8pt">8.1 - 8.7 Production-Related Waste Managed</p>
	                      			</td>
	                    		</xsl:otherwise>
	                    	</xsl:choose>

	                      <td>
	                        <p style="background-color: gray">
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="background-color: gray">
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="background-color: gray">
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="background-color: gray">
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">8.1a</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Total on-site disposal to Class I
	                          <br/>
	                          Underground Injection Wells, RCRA
	                          <br/>
	                          Subtitle C landfills, and other landfills
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">8.1b</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Total other on-site disposal or other
	                          <br/>
	                          releases
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OnsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">8.1c</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Total off-site disposal to Class I
	                          <br/>
	                          Underground Injection Wells, RCRA
	                          <br/>
	                          Subtitle C landfills, and other landfills
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteUICDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                    <tr>
	                      <td width="5%" align="center">
	                        <p style="font-size: 8pt">8.1d</p>
	                      </td>
	                      <td align="left">
	                        <p style="font-size: 8pt">
	                          Total other off-site disposal or other
	                          <br/>
	                          releases
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                      <td>
	                        <p style="font-size: 8pt">
	                          <xsl:for-each select="TRI:SourceReductionQuantity">
	                            <xsl:for-each select="TRI:OffsiteOtherDisposalQuantity">
	                              <xsl:choose>
	                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
	                                  <xsl:choose>
	                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
	                                      <span class="answerText">NA&#160;</span>
	                                    </xsl:when>
	                                    <xsl:otherwise>
	                                      <span class="answerText">
	                                        <xsl:value-of select="TRI:TotalQuantity"/>
	                                      &#160;</span>
	                                    </xsl:otherwise>
	                                  </xsl:choose>
	                                </xsl:when>
	                              </xsl:choose>
	                            </xsl:for-each>
	                          </xsl:for-each>
	                          <br/>
	                        </p>
	                      </td>
	                    </tr>
	                  </xsl:otherwise>
                    </xsl:choose>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.2</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity used for energy recovery
                          <br/>
                          on-site
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.3</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity used for energy recovery
                          <br/>
                          off-site
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteEnergyRecoveryQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.4</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity recycled on-site
                          <br/>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.5</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity recycled off-site
                          <br/>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteRecycledQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.6</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity treated on-site
                          <br/>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OnsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.7</p>
                      </td>
                      <td align="left">
                        <p style="font-size: 8pt">
                          Quantity treated off-site
                          <br/>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '-1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '0'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '1'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                          <xsl:for-each select="TRI:SourceReductionQuantity">
                            <xsl:for-each select="TRI:OffsiteTreatedQuantity">
                              <xsl:choose>
                                <xsl:when test="TRI:YearOffsetMeasure = '2'">
                                  <xsl:choose>
                                    <xsl:when test="TRI:TotalQuantityNAIndicator = 'true'">
                                      <span class="answerText">NA&#160;</span>
                                    </xsl:when>
                                    <xsl:otherwise>
                                      <span class="answerText">
                                        <xsl:value-of select="TRI:TotalQuantity"/>
                                      &#160;</span>
                                    </xsl:otherwise>
                                  </xsl:choose>
                                </xsl:when>
                              </xsl:choose>
                            </xsl:for-each>
                          </xsl:for-each>
                          <br/>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.8</p>
                      </td>
                      <td colspan="2" align="left">
                        <p style="font-size: 8pt">
                        	<xsl:choose>
                        		<xsl:when test="TRI:SubmissionReportingYear &lt; '2014'">
                        			Quantity released to the environment as a result of
                          			remedial actions,
                          			<br/>
                          			catastrophic events, or one-time events not associated
                          			with production processes (pounds/year)
                        		</xsl:when>
                        		<xsl:otherwise>
                        			Non-production-related waste managed**
                        		</xsl:otherwise>
                        	</xsl:choose>
                        </p>
                      </td>
                      <td colspan="3">
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:SourceReductionQuantity/TRI:OneTimeReleaseNAIndicator = 'true'">NA</xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OneTimeReleaseQuantity"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                        <br/>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.9</p>
                      </td>
                      <xsl:choose>
	                      <xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
	                      	<td colspan="2" align="left">
		                        <p style="font-size: 8pt">
		                          <xsl:choose>
		                            <xsl:when test="TRI:SourceReductionQuantity/TRI:ProductionRatioType = 'PRODUCTION'">[<span class="answerText">X</span>]</xsl:when>
		                            <xsl:otherwise>[&#160;]</xsl:otherwise>
		                          </xsl:choose>
		                          Production ratio or
		                          <xsl:choose>
		                            <xsl:when test="TRI:SourceReductionQuantity/TRI:ProductionRatioType = 'ACTIVITY'">[<span class="answerText">X</span>]</xsl:when>
		                            <xsl:otherwise>[&#160;]</xsl:otherwise>
		                          </xsl:choose>
		                          Activity ratio (select one and enter value to right)
		                        </p>
		                      </td>
	                      </xsl:when>
	                      <xsl:otherwise>
		                      <td colspan="2" align="left">
		                        <p style="font-size: 8pt">Production ratio or activity index</p>
		                      </td>
	                      </xsl:otherwise>
                      </xsl:choose>
                      <td colspan="3">
                        <span class="answerText">
                          <xsl:choose>
                            <xsl:when test="TRI:SourceReductionQuantity/TRI:ProductionRatioNAIndicator = 'true'">NA</xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="TRI:SourceReductionQuantity/TRI:ProductionRatioMeasure"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        &#160;</span>
                        <br/>
                      </td>
                    </tr>
                    <tr>
                      <td width="5%" align="center">
                        <p style="font-size: 8pt">8.10</p>
                      </td>
                      <xsl:choose>
						<xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
						   <td colspan="5">
						   <p style="font-size: 8pt">
						     Did your facility engage in any source reduction activities for this chemical during the reporting
year? If not, enter "NA" in Section 8.10.1 and answer Section 8.11.
						   </p>
						  </td>
						</xsl:when>
						<xsl:otherwise>
						   <td colspan="2">
						   <p style="font-size: 8pt">
							  Did your facility engage in any newly implemented source reduction activities for this
							  chemical during the reporting year?
							  <br/>
							  If so, complete the following section; if not, check NA.
							</p>
						  </td>
						  <td colspan="3">
								NA [
	                          <xsl:choose>
	                            <xsl:when test="TRI:SourceReductionNAIndicator = 'true'">
	                              <span class="answerText">X</span>
	                            </xsl:when>
	                            <xsl:otherwise>&#160;</xsl:otherwise>
	                          </xsl:choose>
	                          ]

							<br/>
							</td>
						</xsl:otherwise>
					</xsl:choose>

                    </tr>
                  </table>
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1"
                         cellspacing="0" cellpadding="1" frame="above">
                    <tr>
                      <td width="5%"></td>
                      <td align="center">
                        <p style="font-size: 8pt">
                          Source Reduction Activities
                          <br/>
                          (Enter code(s))
                        </p>
                      </td>
                      <td colspan="3" align="center">
                        <p style="font-size: 8pt">Methods to Identify Activity
                                                  (Enter code(s))</p>
                      </td>
                      <xsl:if test="TRI:SubmissionReportingYear &gt; '2013'">
                      	<td align="center">
                        	<p style="font-size: 8pt">Estimated annual
                        								reduction
                        								(Enter code(s))
                        								(optional)</p>
                      	</td>
                      </xsl:if>
                    </tr>
                    <xsl:choose>
                      <xsl:when test="TRI:SourceReductionNAIndicator = 'true'">
                        <tr>
                          <td width="5%" align="center">
                            <p style="font-size: 8pt">8.10.1</p>
                          </td>
                          <td>
                            <p style="font-size: 8pt">
                              <b style="color: blue;font-size: 9pt">NA</b>
                              <br/>
                            </p>
                          </td>
                          <td style="text-indent: 1em">
                            <p style="font-size: 8pt">
                              <br/>
                              <br/>
                            </p>
                          </td>
                          <td style="text-indent: 1em">
                            <p style="font-size: 8pt">
                              <br/>
                              <br/>
                            </p>
                          </td>
                          <td style="text-indent: 1em">
                            <p style="font-size: 8pt">
                              <br/>
                              <br/>
                            </p>
                          </td>
                        </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:for-each select="TRI:SourceReductionActivity">
                          <tr>
                            <td width="5%" align="center">
                              <p style="font-size: 8pt">
                                8.10.<xsl:value-of select="count(preceding-sibling::TRI:SourceReductionActivity) + 1"/>
                              </p>
                            </td>
                            <td>
                              <p style="font-size: 8pt">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:SourceReductionActivityCode"/>
                                &#160;</span>
                                <br/>
                              </p>
                            </td>
                            <td style="text-indent: 1em" width="15%"
                                align="center">
                              <p style="font-size: 8pt">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:SourceReductionMethodCode[1]"/>
                                &#160;</span>
                                <br/>
                                <br/>
                              </p>
                            </td>
                            <td style="text-indent: 1em" width="15%"
                                align="center">
                              <p style="font-size: 8pt">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:SourceReductionMethodCode[2]"/>
                                &#160;</span>
                                <br/>
                                <br/>
                              </p>
                            </td>
                            <td style="text-indent: 1em" width="15%"
                                align="center">
                              <p style="font-size: 8pt">
                                <span class="answerText">
                                  <xsl:value-of select="TRI:SourceReductionMethodCode[3]"/>
                                &#160;</span>
                                <br/>
                                <br/>
                              </p>
                            </td>
                            <xsl:choose>
	                            <xsl:when test="../TRI:SubmissionReportingYear &gt; '2013'">
	                            	<td style="text-indent: 1em" width="15%"
		                                align="center">
		                              <p style="font-size: 8pt">
		                                <span class="answerText">
		                                  <xsl:value-of select="TRI:SourceReductionEfficiencyCode"/>
		                                &#160;</span>
		                                <br/>
		                                <br/>
		                              </p>
		                            </td>
	                            </xsl:when>
                            </xsl:choose>
                          </tr>
                        </xsl:for-each>
                      </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="TRI:SubmissionReportingYear &lt; '2011'">
                      <tr>

						  <td width="5%" align="center">
							<p style="font-size: 8pt">8.11</p>
						  </td>
						  <td colspan="2">

							<p style="font-size: 8pt">If you wish to submit
													  additional optional
													  information on source
													  reduction, recycling, or
													  pollution control activities,
													  check "Yes."</p>
						  </td>
						  <td colspan="2" style="font-size: 8pt">
							<p style="font-size: 8pt">
							  Yes [

							  <xsl:choose>
								<xsl:when test="TRI:SubmissionAdditionalDataIndicator = 'true'">
								  <span class="answerText">X</span>
								</xsl:when>
							  </xsl:choose>
							  ]

							</p>
						  </td>
						</tr>
                    </xsl:if>
                  </table>
                </td>
              </tr>
            </table>
          </center>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0"
                 style="font-size: 8pt" border="0">
            <tr>
              <td width="50%">
                <p style="font-size: 8pt">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		EPA Form 9350-1 (Rev. <xsl:value-of select="$RevisionDateFormR"/>) - Previous editions are obsolete.
                  	</xsl:if>
                </p>
              </td>
              <td align="right">
                <p style="font-size: 8pt">
                	*For Dioxin and Dioxin-like Compounds, report in grams/year<br/>
                	<xsl:if test="TRI:SubmissionReportingYear &gt; '2013'">
                		** Includes quantities released to the environment or transferred off-site as a result of remedial actions, catastrophic events, or other one-time events not associated with production processes
                	</xsl:if>
                </p>
              </td>
            </tr>
          </table>
          <p style="page-break-before: always">&#160;</p>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1"
                 style="font-size: 8pt" border="0">
            <tr>
             <!--   <td width="90%">
                <a name="PG-6_{$formID}"></a>
                <p style="font-size: 8pt">
                  <a href="#PG-1_{$formID}">1</a>
                  <a href="#PG-2_{$formID}">2</a>
                  <a href="#PG-3_{$formID}">3</a>
                  <a href="#PG-4_{$formID}">4</a>
                  <a href="#PG-5_{$formID}">5</a>
                  <a href="#PG-6_{$formID}">Additional Info</a>
                  <xsl:if test="$ScheduleOneNA = 'false'">
                      <a href="#S1_PG-1_{$formID}">Schedule 1</a>
                  </xsl:if>
                </p>
              </td>-->
            </tr>
          </table>
          <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%"
                 style="font-size: 8pt">
            <tr>
              <td colspan="2">
                <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="1" cellspacing="0" cellpadding="1" frame="void">
                  <tr>
                    <td>
                        TRI Facility ID Number
                        <hr />
                        <span class="answerText">
                          <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                        &#160;</span>
                      <hr />
                      Toxic Chemical, Category, or Generic Name
                      <hr />
                      <span class="answerText">
                        <xsl:choose>
                          <xsl:when test="TRI:ChemicalIdentification/TRI:ChemicalNameText='NA'">
                            <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalNameText"/>
                          </xsl:otherwise>
                        </xsl:choose>
                      &#160;</span>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
          <br/>
          <table border="1" width="99%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
            <tr>
              <td align="left" style="font-size: 8pt">
                <p style="font-size: 8pt">
                  <b>Additional optional information on source reduction,
                     recycling, or pollution control activities.</b>
                </p>
              </td>
            </tr>
            <tr>
              <td align="left" style="font-size: 8pt">
                <span class="answerText">
                  <xsl:value-of select="TRI:OptionalInformationText"/>
                  &#160;
                </span>
              </td>
            </tr>
          </table>
          <br/>
          <xsl:choose>
          	<xsl:when test="TRI:SubmissionReportingYear &gt; '2013'">
          		<table summary="table used for layout purposes" border="1" width="99%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
		            <tr>
		              <td colspan="2" align="left" style="font-size: 8pt">
		                <p style="font-size: 8pt">
		                  <b>Section 8.11: If you wish to submit additional optional information on source reduction, recycling, or pollution control activities, provide it here.</b>
		                </p>
		              </td>
		            </tr>
		            <tr>
			           <td style="width: 20%;">
			            <p style="font-size: 8pt">
			             <b>Topic</b>
			            </p>
			           </td>
			           <td>
			            <p style="font-size: 8pt">
			             <b>Comment</b>
			            </p>
			           </td>
	        		</tr>
		            <xsl:for-each select="TRI:TRIComment">
		            	<xsl:if test="TRI:TRICommentSection = '8.11'">
			            	<tr>
			            		<td style="width: 20%;">
			            			<p style="font-size: 8pt">
			            				<xsl:value-of select="TRI:TRICommentTypeDescription"/>
			            			</p>
			            		</td>
			            		<td>
			            			<p style="font-size: 8pt">
			            				<xsl:value-of select="TRI:TRICommentText"/>
			            			</p>
			            		</td>
			            	</tr>
		            	</xsl:if>
		            </xsl:for-each>
		          </table>
		          <br/>
		          <table summary="table used for layout purposes" border="1" width="99%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
		            <tr>
		              <td colspan="2" align="left" style="font-size: 8pt">
		                <p style="font-size: 8pt">
		                  <b>Section 9.1: If you wish to submit any miscellaneous, additional, or optional information regarding your Form R submission, provide it here.</b>
		                </p>
		              </td>
		            </tr>
		            <tr>
			           <td style="width: 20%;">
			            <p style="font-size: 8pt">
			             <b>Topic</b>
			            </p>
			           </td>
			           <td>
			            <p style="font-size: 8pt">
			             <b>Comment</b>
			            </p>
			           </td>
	        		</tr>
		            <xsl:for-each select="TRI:TRIComment">
		            	<xsl:if test="TRI:TRICommentSection = '9.1'">
			            	<xsl:if test="TRI:TRICommentText !=''">
			            	<tr>
			            		<td style="width: 20%;">
			            			<p style="font-size: 8pt">
			            				<xsl:value-of select="TRI:TRICommentTypeDescription"/>
			            			</p>
			            		</td>
			            		<td>
			            			<p style="font-size: 8pt">
			            				<xsl:value-of select="TRI:TRICommentText"/>
			            			</p>
			            		</td>
			            	</tr>
		            	</xsl:if>
		            	</xsl:if>
		            </xsl:for-each>
		          </table>
          	</xsl:when>
	        <xsl:when test="TRI:SubmissionReportingYear &gt; '2010'">
		        <table summary="table used for layout purposes" border="1" width="99%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
		          <tr>
		            <td align="left" style="font-size: 8pt">
		              <p style="font-size: 8pt">
		                <b>Miscellaneous, additional, or optional information regarding the Form R submission</b>
		              </p>
		            </td>
		          </tr>
		          <tr>
		            <td align="left" style="font-size: 8pt">
		              <span class="answerText">
		                <xsl:value-of select="TRI:MiscellaneousInformationText"/>
		              &#160;</span>
		            </td>
		          </tr>
		        </table>
		    </xsl:when>
	      </xsl:choose>

          <p style="page-break-before: always"></p>

          <xsl:if test="$ScheduleOneNA = 'false'">
              <!-- Schedule 1 Page One -->

              <xsl:call-template name="ScheduleOnePageOne">
                <xsl:with-param name="baseStreamID">0</xsl:with-param>
                <xsl:with-param name="formID"><xsl:value-of select="$formID" /></xsl:with-param>
                <xsl:with-param name="OMBNumberSchedule1"><xsl:value-of select="$OMBNumberSchedule1" /></xsl:with-param>
                <xsl:with-param name="ApprovalDateSchedule1"><xsl:value-of select="$ApprovalDateSchedule1" /></xsl:with-param>
              </xsl:call-template>

              <!-- Additional Page One(s) if necessary -->
              <xsl:for-each select="TRI:OnsiteReleaseQuantity/TRI:WaterStream">
                <xsl:if test="TRI:WaterSequenceNumber > 2 and TRI:WaterSequenceNumber mod 3 = 0">
                  <xsl:variable name="baseStreamID" select="TRI:WaterSequenceNumber" />
                  <xsl:for-each select="../..">
                    <xsl:call-template name="ScheduleOnePageOne">
                      <xsl:with-param name="baseStreamID"><xsl:value-of select="$baseStreamID" /></xsl:with-param>
                      <xsl:with-param name="formID"><xsl:value-of select="$formID" /></xsl:with-param>
                      <xsl:with-param name="OMBNumberSchedule1"><xsl:value-of select="$OMBNumberSchedule1" /></xsl:with-param>
                      <xsl:with-param name="ApprovalDateSchedule1"><xsl:value-of select="$ApprovalDateSchedule1" /></xsl:with-param>
                    </xsl:call-template>
                  </xsl:for-each>
                </xsl:if>
              </xsl:for-each>

          <!-- Schedule 1 Page Two -->
          <div class="landscapeArea">
          <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
           </tr>
            <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
              <tr>
                <td width="30%">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	                  Form Approved OMB Number:<span class="smallAnswer"><xsl:value-of select="$OMBNumberSchedule1" />&#160;</span><br />
	                  Approval Expires: <span class="smallAnswer"><xsl:value-of select="$ApprovalDateSchedule1" />&#160;</span>
	                </xsl:if>
                </td>
                <td width="10%">
                  <p style="font-size: 8pt"><b>Page 2 of 4</b></p>
                </td>
              </tr>
            </table>

          <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt;">

              <tr>
                <td colspan="26" align="center">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="0" cellspacing="0" cellpadding="0" frame="void">
                    <tr>
                      <td style="font-size: 10pt"><b> EPA </b></td>
                      <td align="center" style="font-size: 14pt"><b>FORM R Schedule 1</b></td>
                      <td><p style="font-size: 8pt">TRI Facility ID Number:</p></td>
                    </tr>
                    <tr>
                      <td>
                        <p style="font-size: 8pt">United States<br />Environmental Protection<br />Agency</p>
                      </td>
                      <td align="center">
                        <p style="font-size: 14pt"><b>PART II.  CHEMICAL-SPECIFIC INFORMATION (continued)</b></p>
                      </td>
                      <td class="answerText"><xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/></td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td align="left" colspan="26" style="font-size: 8pt">
                  <p style="font-size: 9pt"><b>Section 5. Quantity Of Dioxin And Dioxin-Like Compounds Entering Each Environmental Medium On-site (continued)</b></p>
                </td>
              </tr>

              <tr>
                <td colspan="2" rowspan="3" style="background-color: gray">&#160;</td>
                <td align="center" colspan="6" style="font-size: 8pt">
                  <p style="font-size: 8pt"><b>5.4</b>&#160;&#160; Underground Injection</p>
                </td>
                <td align="center" colspan="18" style="font-size: 8pt">
                  <p style="font-size: 8pt"><b>5.5</b>&#160;&#160; Disposal to Land On-site</p>
                </td>
              </tr>

              <tr>
                <td style="font-size: 8pt" align="center"><b>5.4.1</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'UNINJ I'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.4.2</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'UNINJ IIV'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.1A</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'RCRA C'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.1B</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'OTH LANDF'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.2</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'LAND TREA'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.3A</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'SI 5.5.3A'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.3B</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'SI 5.5.3B'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

                <td style="font-size: 8pt" align="center"><b>5.5.4</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator[../../TRI:EnvironmentalMediumCode = 'OTH DISP'] = 'true'">
                  X
                  </xsl:if>&#160;
                </td>

              </tr>

              <tr>
                <td colspan="3" style="font-size: 8pt" align="center">
                	<xsl:choose>
                		<xsl:when test="TRI:SubmissionReportingYear &lt; '2014'">
                			Underground Injection on-site to Class I Wells
                		</xsl:when>
                		<xsl:otherwise>
                			Class I Underground Injection Wells
                		</xsl:otherwise>
                	</xsl:choose>
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                	<xsl:choose>
                		<xsl:when test="TRI:SubmissionReportingYear &lt; '2014'">
                			Underground Injection on-site to Class II-V Wells
                		</xsl:when>
                		<xsl:otherwise>
                			Class II-V Underground Injection Wells
                		</xsl:otherwise>
                	</xsl:choose>

                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  RCRA Subtitle C landfills
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  Other landfills
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  Land treatment/application farming
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  RCRA Subtitle C surface impoundment
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  Other surface impoundment
                </td>
                <td colspan="3" style="font-size: 8pt" align="center">
                  Other disposal
                </td>
              </tr>

              <xsl:call-template name="ScheduleOnePageTwoRow" />

            </table>
          </center>

          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
              <td width="60%">
                <p style="font-size: 8pt">
                  EPA Form 9350-3
                </p>
              </td>
              <td width="30%">
                <p style="font-size: 8pt">Printed using TRI-MEweb</p>
              </td>
            </tr>
          </table>
          </div>

          <!-- Schedule 1 Page Three -->
          <xsl:variable name="potwTEQ" select="TRI:POTWWasteQuantity/TRI:ToxicEquivalencyIdentification" />

            <div style="width: 1010px;" class="landscapeArea">
            <tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
            </tr>
            <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
              <tr>
                <td width="30%">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	                  Form Approved OMB Number:<span class="smallAnswer"><xsl:value-of select="$OMBNumberSchedule1" />&#160;</span><br />
    	              Approval Expires: <span class="smallAnswer"><xsl:value-of select="$ApprovalDateSchedule1" />&#160;</span>
    	            </xsl:if>
                </td>
                <td width="10%">
                  <p style="font-size: 8pt"><b>Page 3 of 4</b></p>
                </td>
              </tr>
            </table>

          <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt;">
              <tr>
                <td colspan="18" align="center">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="0" cellspacing="0" cellpadding="0" frame="void">
                    <tr>
                      <td style="font-size: 8pt">
                        <p style="font-size: 10pt"><b> EPA </b></p>
                      </td>
                      <td align="center" style="font-size: 10pt">
                        <p style="font-size: 14pt"><b>FORM R Schedule 1</b></p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">TRI Facility ID Number:</p>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <p style="font-size: 8pt">United States<br />Environmental Protection<br />Agency</p>
                      </td>
                      <td align="center">
                        <p style="font-size: 14pt"><b>PART II.  CHEMICAL-SPECIFIC INFORMATION (continued)</b></p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                            <span class="answerText">
                              <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                            &#160;</span>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td align="left" colspan="18" style="font-size: 8pt">
                  <p style="font-size: 9pt"><b>SECTION 6. TRANSFERS OF DIOXIN AND DIOXIN-LIKE COMPOUNDS IN WASTES TO OFF-SITE LOCATIONS</b></p>
                </td>
              </tr>
              <tr>
                 <xsl:choose>
				  <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
					 <td align="left" colspan="18" style="font-size: 8pt" width="50%">
					  <p style="font-size: 9pt"><b>6.1 DISCHARGES TO PUBLICLY-OWNED TREATMENT WORKS (POTWs)</b></p>
					</td>
				  </xsl:when>
				  <xsl:otherwise>
					<td align="left" colspan="9" style="font-size: 8pt" width="50%">
					  <p style="font-size: 9pt"><b>6.1 DISCHARGES TO PUBLICLY-OWNED TREATMENT WORKS (POTWs)</b></p>
					</td>
					<td align="left" colspan="9" style="font-size: 8pt" width="50%">
					  <p style="font-size: 8pt">
						NA [
						  <xsl:if test="TRI:POTWWasteQuantity/TRI:WasteQuantityNAIndicator = 'true' or not(TRI:POTWWasteQuantity)">
							<span class="answerText">X</span>
						  </xsl:if>
						]
					  </p>
					</td>
				  </xsl:otherwise>
				</xsl:choose>
              </tr>
              <xsl:for-each select="TRI:POTWWasteQuantity">
              <xsl:choose>
				<xsl:when test="../TRI:SubmissionReportingYear &lt; '2011'">
				  <tr>
					  <td colspan="18" align="center" style="font-size: 8pt"><b>6.1.A.3 Mass (grams) of each compound in the category (1-17)
					  </b>
					</td>
				  </tr>
				   <tr>
					  <td style="font-size: 8pt">1</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency1Value" />&#160;</td>

					  <td style="font-size: 8pt">2</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency2Value" />&#160;</td>

					  <td style="font-size: 8pt">3</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency3Value" />&#160;</td>

					  <td style="font-size: 8pt">4</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency4Value" />&#160;</td>

					  <td style="font-size: 8pt">5</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency5Value" />&#160;</td>

					  <td style="font-size: 8pt">6</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency6Value" />&#160;</td>

					  <td style="font-size: 8pt">7</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency7Value" />&#160;</td>

					  <td style="font-size: 8pt">8</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency8Value" />&#160;</td>

					  <td style="font-size: 8pt">9</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency9Value" />&#160;</td>
					</tr>
					<tr>
					  <td style="font-size: 8pt">10</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency10Value" />&#160;</td>

					  <td style="font-size: 8pt">11</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency11Value" />&#160;</td>

					  <td style="font-size: 8pt">12</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency12Value" />&#160;</td>

					  <td style="font-size: 8pt">13</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency13Value" />&#160;</td>

					  <td style="font-size: 8pt">14</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency14Value" />&#160;</td>

					  <td style="font-size: 8pt">15</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency15Value" />&#160;</td>

					  <td style="font-size: 8pt">16</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency16Value" />&#160;</td>

					  <td style="font-size: 8pt">17</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency17Value" />&#160;</td>

					  <td colspan="2" style="background-color:gray">&#160;</td>
				  </tr>
				</xsl:when>
				<xsl:otherwise>
				  <tr>
					  <td colspan="2" align="left" style="font-size: 9pt"><b>6.1.<xsl:value-of select="TRI:POTWSequenceNumber" />
					  </b></td>
					  <td colspan="16" align="left" style="font-size: 8pt"><b>C. Mass (grams) of each compound in the category (1-17)
					  </b></td>
				  </tr>
				  <tr>
					  <td align="center" colspan="2" style="font-size: 8pt">
						  <b><xsl:value-of select="TRI:POTWSequenceNumber" />.</b>
					  </td>
					  <td style="font-size: 8pt">1</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency1Value" />&#160;</td>

					  <td style="font-size: 8pt">2</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency2Value" />&#160;</td>

					  <td style="font-size: 8pt">3</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency3Value" />&#160;</td>

					  <td style="font-size: 8pt">4</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency4Value" />&#160;</td>

					  <td style="font-size: 8pt">5</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency5Value" />&#160;</td>

					  <td style="font-size: 8pt">6</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency6Value" />&#160;</td>

					  <td style="font-size: 8pt">7</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency7Value" />&#160;</td>

					  <td style="font-size: 8pt">8</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency8Value" />&#160;</td>
					</tr>
					<tr>
					  <td style="font-size: 8pt">9</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency9Value" />&#160;</td>

					  <td style="font-size: 8pt">10</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency10Value" />&#160;</td>

					  <td style="font-size: 8pt">11</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency11Value" />&#160;</td>

					  <td style="font-size: 8pt">12</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency12Value" />&#160;</td>

					  <td style="font-size: 8pt">13</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency13Value" />&#160;</td>

					  <td style="font-size: 8pt">14</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency14Value" />&#160;</td>

					  <td style="font-size: 8pt">15</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency15Value" />&#160;</td>

					  <td style="font-size: 8pt">16</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency16Value" />&#160;</td>

					  <td style="font-size: 8pt">17</td>
					  <td align="center" class="teqAnswer"><xsl:value-of select="TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency17Value" />&#160;</td>
				  </tr>
		        </xsl:otherwise>
                </xsl:choose>

          	</xsl:for-each>
              <tr>
                <xsl:choose>
				  <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
				    <td align="left" colspan="18" style="font-size: 8pt">
					  <p style="font-size: 9pt"><b>6.2  TRANSFERS TO OTHER OFF-SITE LOCATIONS</b></p>
					</td>
				  </xsl:when>
				  <xsl:otherwise>
				    <td align="left" colspan="9" style="font-size: 8pt">
					  <p style="font-size: 9pt"><b>6.2  TRANSFERS TO OTHER OFF-SITE LOCATIONS</b></p>
					</td>
					<td align="left" colspan="9" style="font-size: 8pt;">
						<p style="font-size: 8pt">
					   NA [

						  <xsl:if test="not(TRI:TransferLocation)">

								<span class="answerText">X</span>

						  </xsl:if>

						 ]
					 </p>
					</td>
				  </xsl:otherwise>
				</xsl:choose>
              </tr>


              <!-- Loop through transfers; if not potw write header and call template for transfer TEQ quantities -->
              <xsl:for-each select="TRI:TransferLocation">
                <xsl:if test="TRI:POTWIndicator = 'false'">
                  <tr>
                    <td colspan="2" align="left" style="font-size: 8pt">
                      <p style="font-size: 9pt">
                        <xsl:choose>
                        <xsl:when test="../TRI:TransferLocation[1]/TRI:POTWIndicator = 'true'">
                          <b>6.2.<xsl:value-of select="count(preceding-sibling::TRI:TransferLocation)-1"/></b>
                        </xsl:when>
                        <xsl:otherwise>
                          <b>6.2.<xsl:value-of select="count(preceding-sibling::TRI:TransferLocation)-1"/></b>
                        </xsl:otherwise>
                        </xsl:choose>
                      </p>
                    </td>
                    <td colspan="16" align="left" style="font-size: 8pt">
                      <p style="font-size: 8pt"><b>D. Mass (grams) of each compound in the category (1-17)</b></p>
                    </td>
                  </tr>

                  <!-- Loop through TEQ values -->
                  <xsl:for-each select="TRI:TransferQuantity">
                    <xsl:call-template name="ScheduleOnePageThreeRow" />
                  </xsl:for-each>

                </xsl:if>
              </xsl:for-each>

              <!-- Put in 2 blank section 6.2 TEQ sections if there is no data -->
              <xsl:if test="count(TRI:TransferLocation) = 0 or (count(TRI:TransferLocation) = 1 and TRI:TransferLocation[1]/TRI:POTWIndicator = 'true')">
                  <xsl:call-template name="ScheduleOnePageThreeRowBlank"><xsl:with-param name="i">1</xsl:with-param></xsl:call-template>
                  <xsl:call-template name="ScheduleOnePageThreeRowBlank"><xsl:with-param name="i">2</xsl:with-param></xsl:call-template>
              </xsl:if>

              <tr>
                <td align="left" colspan="18" style="font-size: 8pt">
                  <!--p style="font-size: 8pt">
                    If additional pages of Section 6.2 are attached, indicate the total number of pages in this box&#160;&#160;
                    <span style="border: 1px solid black; padding: 2px; text-align:center; width: 25px;">
                      &#160;
                    &#160;</span>
                    <br />
                    and indicate the Section 6.2 page number in this box&#160;&#160;
                    <span style="border: 1px solid black; padding: 2px; text-align:center; width: 25px;">
                      &#160;
                    &#160;</span>&#160;
                    (example: 1,2,3, etc.)
                  </p-->
                  &#160;
                </td>
              </tr>

            </table>
          </center>

          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
              <td width="60%">
                <p style="font-size: 8pt">
                  EPA Form 9350-3
                </p>
              </td>
              <td width="30%">
                <p style="font-size: 8pt">Printed using TRI-MEweb</p>
              </td>
            </tr>
          </table>
          </div>

          <!-- Schedule 1 Page Four -->

         <div width="1010px;" class="landscapeArea">
         	<tr>
	              <td align="center" width="100%">
	                <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
	                  *** Do not send to EPA: This is the final copy of your form.***
	                &#160;</span>
	              </td>
            </tr>
            <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
              <tr>
                <td width="30%">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                  		Form Approved OMB Number:<span class="smallAnswer"><xsl:value-of select="$OMBNumberSchedule1" />&#160;</span><br />
                  		Approval Expires: <span class="smallAnswer"><xsl:value-of select="$ApprovalDateSchedule1" />&#160;</span>
                  	</xsl:if>
                </td>
                <td width="10%">
                  <p style="font-size: 8pt"><b>Page 4 of 4</b></p>
                </td>
              </tr>
            </table>

          <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt;">
              <tr>
                <td colspan="13" align="center">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="0" cellspacing="0" cellpadding="0" frame="void">
                    <tr>
                      <td style="font-size: 8pt">
                        <p style="font-size: 10pt"><b> EPA </b></p>
                      </td>
                      <td align="center" style="font-size: 10pt">
                        <p style="font-size: 14pt"><b>FORM R Schedule 1</b></p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">TRI Facility ID Number:</p>
                      </td>
                    </tr>
                    <tr>
                      <td>
                        <p style="font-size: 8pt">United States<br />Environmental Protection<br />Agency</p>
                      </td>
                      <td align="center">
                        <p style="font-size: 14pt"><b>PART II.  CHEMICAL-SPECIFIC INFORMATION (continued)</b></p>
                      </td>
                      <td>
                        <p style="font-size: 8pt">
                            <span class="answerText">
                              <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                            &#160;</span>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td align="left" colspan="13" style="font-size: 8pt">
                  <p style="font-size: 9pt"><b>SECTION 8. SOURCE REDUCTION AND WASTE MANAGEMENT FOR DIOXIN AND DIOXIN-LIKE COMPOUNDS (current year only)</b></p>
                </td>
              </tr>


              <tr>
                <td colspan="2" rowspan="2" style="background-color:gray">&#160;</td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.1a</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.1b</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.1c</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.1d</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.2</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.3</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.4</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.5</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.6</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.7</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">8.8</p>
                </td>
              </tr>

              <tr>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Total on-site disposal to Class 1 Underground Injection Wells, RCRA Subtitle C landfills, and other landfills</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Total other on-site disposal or other releases</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Total off-site disposal to Class 1 Underground Injection Wells, RCRA Subtitle C landfills, and other landfills</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Total other off-site disposal or other releases</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity used for energy recovery on-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity used for energy recovery off-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity recycled on-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity recycled off-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity treated on-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity treated off-site</p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">Quantity released to the environment as a result of remedial actions, catastrophic events, or one-time events not associated with production processes</p>
                </td>
              </tr>

              <xsl:call-template name="ScheduleOnePageFourRow" />

            </table>
          </center>

          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
              <td width="60%">
                <p style="font-size: 8pt">
                  EPA Form 9350-3
                </p>
              </td>
              <td width="30%">
                <p style="font-size: 8pt">Printed using TRI-MEweb</p>
              </td>
            </tr>
          </table>
          </div>
          </xsl:if>

      </xsl:when>
      <xsl:otherwise>
          <!-- Begin Form A : Form A data will be grouped together by revision codes, withdrawal codes, and public and technical contact infomration.
                So, the following test chooses only to enter the form a section if the form a data encountered has not been encountered yet.
          -->

          <xsl:if test="count(preceding::TRI:Report[TRI:ReportType/TRI:ReportTypeCode = current()/TRI:ReportType/TRI:ReportTypeCode
                                                         and concat(TRI:TechnicalContactNameText/sc:IndividualFullName, 'xx') = concat(current()/TRI:TechnicalContactNameText/sc:IndividualFullName, 'xx')
                                                         and concat(TRI:TechnicalContactPhoneText, 'xx') = concat(current()/TRI:TechnicalContactPhoneText, 'xx')
                                                         and concat(TRI:TechnicalContactEmailAddressText, 'xx') = concat(current()/TRI:TechnicalContactEmailAddressText, 'xx')
                                                         and concat(TRI:PublicContactNameText/sc:IndividualFullName, 'xx') = concat(current()/TRI:PublicContactNameText/sc:IndividualFullName, 'xx')
                                                         and concat(TRI:PublicContactPhoneText, 'xx') = concat(current()/TRI:PublicContactPhoneText, 'xx')
                                                         and concat(TRI:PublicContactEmailAddressText, 'xx') = concat(current()/TRI:PublicContactEmailAddressText, 'xx')
                                                         and sc:RevisionIndicator = current()/sc:RevisionIndicator
                                                         and concat(TRI:ChemicalReportRevisionCode[1], 'xx') = concat(current()/TRI:ChemicalReportRevisionCode[1], 'xx')
                                                         and concat(TRI:ChemicalReportRevisionCode[2], 'xx') = concat(current()/TRI:ChemicalReportRevisionCode[2], 'xx')
                                                         and concat(TRI:ChemicalReportWithdrawalCode[1], 'xx') = concat(current()/TRI:ChemicalReportWithdrawalCode[1], 'xx')
                                                         and concat(TRI:ChemicalReportWithdrawalCode[2], 'xx') = concat(current()/TRI:ChemicalReportWithdrawalCode[2], 'xx')
                                                         and ../TRI:Facility = current()/../TRI:Facility
                                                         ]) = 0">
          <xsl:if test="count(preceding::TRI:Report) &gt; 0">
            <p style="page-break-before: always">&#160;</p>
          </xsl:if>
          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" border="0">
            <tr>
             <td align="center" width="100%">
              <span style="font-size: 12pt; color: red; font-weight: bold;" class="noPrint">
               *** File Copy Only: Do Not Submit Paper Form to EPA ***
              &#160;</span>
             </td>
            </tr>

            <xsl:if test="SubmissionStatusText">
             <tr>
              <td>
               <span class="fieldLabel" style="color:red">
                Form Status:
                <xsl:value-of select="SubmissionStatusText"/>
               &#160;</span>
              </td>
             </tr>
            </xsl:if>
            <xsl:if test="ValidationStatusText">
             <tr>
              <td>
               <span class="fieldLabel" style="color:red">
                Validation Status:
                <xsl:value-of select="ValidationStatusText"/>
               &#160;</span>
              </td>
             </tr>
            </xsl:if>

           </table>
           <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
            <tr>
             <td width="30%">
             	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	              <p style="font-size: 8pt">
    	          Form Approved OMB Number:
        	      <span class="smallAnswer">
        	       <xsl:value-of select="$OMBNumberFormA"/>
        	      &#160;</span>
        	      </p>
        	    </xsl:if>
             </td>
             <td width="10%">
              <br/>
             </td>
            </tr>
            <tr>
             <td width="60%">
              <p style="font-size: 8pt">
              	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	               <i>(IMPORTANT: Read instructions before completing form; type or use fill-and-print form)</i>
	            </xsl:if>
              </p>
             </td>
             <td width="30%">
             	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	              <p style="font-size: 8pt">
    	          Approval Expires:
    	          <span class="smallAnswer">
    	           <xsl:value-of select="$ApprovalDateFormA"/>
    	          &#160;</span>
    	          </p>
    	        </xsl:if>
             </td>
             <td width="10%">
              <br/>
             </td>
            </tr>
           </table>
           <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-family: 'Arial'; font-size: 8pt">
             <tr>
              <td colspan="2" align="center">
               <table summary="table used for layout purposes" width="100%" style="font-size: 10pt" cellspacing="0" cellpadding="1">
                <tr>
                 <td width="20%">
                  <p style="font-size: 9pt">
                   <span style="font-weight:bold">
                    United States
                    <br/>
                    Environmental Protection Agency
                   &#160;</span>
                  </p>
                 </td>
                 <td nowrap="nowrap" align="center">
                  <p style="font-size: 10pt">
                   <span style="font-weight:bold">
                    TOXICS CHEMICAL RELEASE INVENTORY
                    <br/>
                    FORM A
                   &#160;</span>
                  </p>
                 </td>
                </tr>
               </table>
              </td>
              <td style="font-size: 8pt">
                TRI Facility ID Number
                <hr />
                <span class="answerText">
                 <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>&#160;</span>
              </td>
             </tr>
             <tr>
              <td colspan="3">
               <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt" frame="void">
                <tr>
                 <td align="center" width="15%">
                  <p style="font-size: 8pt">WHERE TO SEND COMPLETED FORMS: </p>
                 </td>
                 <td nowrap="nowrap">
                  <p style="font-size: 8pt">
                   1. TRI Data Processing Center
                   <br />
                   P.O. Box 10163
                   <br />
                   Fairfax, VA 22038
                   <br />
                   <span style="font-size: 8pt; color: red; font-weight: bold;">
                   *** File Copy Only: Do Not Submit Paper Form to EPA ***
                   &#160;</span>
                  </p>
                 </td>
                 <td nowrap="nowrap">
                  <p style="font-size: 8pt">
                   2. APPROPRIATE STATE OFFICE
                   <br/>
                   (See instructions in Appendix F)
                  </p>
                 </td>
                </tr>
                <tr>
                 <td colspan="3"></td>
                </tr>
               </table>
               <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" frame="void">
                <tr>
                 <td width="33%">
                  <p style="font-size: 9pt">This section only applies if you are revising or withdrawing a previously submitted form, otherwise leave blank:</p>
                 </td>
                 <td align="center" width="33%">
                  <p style="font-size: 9pt">
                   Revision (Enter up to two code(s))
                   <br/>
                   <br/>
                   [
                   <span class="answerText">
                    <xsl:value-of select="TRI:ChemicalReportRevisionCode[1]"/>
                   &#160;</span>
                   ] [
                   <span class="answerText">
                    <xsl:value-of select="TRI:ChemicalReportRevisionCode[2]"/>
                   &#160;</span>
                   ]
                  </p>
                 </td>
                 <td align="center" width="34%">
                  <p style="font-size: 9pt">
                   Withdrawal (Enter up to two code(s))
                   <br/>
                   <br/>
                   [
                   <span class="answerText">
                    <xsl:value-of select="TRI:ChemicalReportWithdrawalCode[1]"/>
                   &#160;</span>
                   ] [
                   <span class="answerText">
                    <xsl:value-of select="TRI:ChemicalReportWithdrawalCode[2]"/>
                   &#160;</span>
                   ]
                  </p>
                 </td>
                </tr>
               </table>
              </td>
             </tr>
             <tr>
              <td align="left" colspan="3" style="font-size: 8pt">
               <p style="font-size: 8pt">Important: See Instructions to determine when "Not Applicable (NA)" boxes should be checked.</p>
              </td>
             </tr>
             <tr>
              <td align="center" colspan="3">
               <p style="font-size: 8pt">Part I. FACILITY IDENTIFICATION INFORMATION </p>
              </td>
             </tr>
             <tr>
              <td align="left" colspan="3" style="font-size: 8pt">
                SECTION 1. REPORTING YEAR :
                <u><span class="answerText"><xsl:value-of select="TRI:SubmissionReportingYear"/></span></u>
                <br />
              </td>
             </tr>
             <tr>
              <td align="left" colspan="3">
               <p style="font-size: 8pt">SECTION 2. TRADE SECRET INFORMATION </p>
              </td>
             </tr>
             <tr>
              <td align="left" style="font-size: 8pt">
               <dl>
                <dt>
                 2.1 Are you claiming the toxic chemical identified on page 2 trade secret?
                </dt>
                <dd>
                  [
                  <xsl:choose>
                   <xsl:when test="TRI:ChemicalTradeSecretIndicator = 'true'">
                    <span class="answerText">X</span>
                   </xsl:when>
                  </xsl:choose>
                  ] Yes (Answer question 2.2; attach substantiation forms)
                </dd>
                <dd>
                  [
                  <xsl:choose>
                   <xsl:when test="TRI:ChemicalTradeSecretIndicator = 'false'">
                    <span class="answerText">X</span>
                   </xsl:when>
                  </xsl:choose>
                  ] NO (Do not answer 2.2; go to Section 3)
                </dd>
               </dl>
              </td>
              <td align="left">
               <dl>
                <dt>
                 2.2 Is this copy
                </dt>
                <dd>
                 <p style="font-size: 8pt">[ ] Sanitized [ ] Unsanitized</p>
                </dd>
                <dd>
                 <p style="font-size: 8pt">(Answer only if "Yes" in 2.1)</p>
                </dd>
               </dl>
              </td>
              <td>
               <br/>
              </td>
             </tr>
             <tr>
              <td align="left" colspan="3">
               <p style="font-size: 8pt">SECTION 3. CERTIFICATION (Important: Read and sign after completing all form sections.)</p>
              </td>
             </tr>
             <tr>
              <td align="left" colspan="3">
               <p style="font-size: 8pt">
                <xsl:choose>
                 <xsl:when test="TRI:SubmissionReportingYear = '2006'">
                      Pursuant to 40 CFR 372.27(a)(1), "I hereby certify that to the best of my knowledge and belief for the
                      toxic chemical(s) listed in this statement, for this reporting year, the annual reportable amount for each
                      chemical, as defined in 40 CFR 372.27(a)(1), did not exceed 5,000 pounds, which included no more than 2,000
                      pounds of total disposal or other releases to the environment, and that the chemical was manufactured,
                      or processed, or otherwise used in an amount not exceeding 1 million pounds during this reporting
                      year;" and/or Pursuant to 40 CFR 372.27(a)(2), "I hereby certify that to the best of my knowledge and
                      belief for the toxic chemical(s) of special concern listed in this statement, there were zero disposals or
                      other releases to the environment (including disposals or other releases that resulted from catastrophic
                      events) for this reporting year, the "Annual Reportable Amount of a Chemical of Special Concern"
                      for each such chemical, as defined in 40 CFR 372.27(a)(2), did not exceed 500 pounds for this
                      reporting year, and that the chemical was manufactured, or processed, or otherwise used in an
                      amount not exceeding 1 million pounds during this reporting year."
                 </xsl:when>
				 <xsl:when test="TRI:SubmissionReportingYear = '2007'">
                      Pursuant to 40 CFR 372.27(a)(1), "I hereby certify that to the best of my knowledge and belief for the
                      toxic chemical(s) listed in this statement, for this reporting year, the annual reportable amount for each
                      chemical, as defined in 40 CFR 372.27(a)(1), did not exceed 5,000 pounds, which included no more than 2,000
                      pounds of total disposal or other releases to the environment, and that the chemical was manufactured,
                      or processed, or otherwise used in an amount not exceeding 1 million pounds during this reporting
                      year;" and/or Pursuant to 40 CFR 372.27(a)(2), "I hereby certify that to the best of my knowledge and
                      belief for the toxic chemical(s) of special concern listed in this statement, there were zero disposals or
                      other releases to the environment (including disposals or other releases that resulted from catastrophic
                      events) for this reporting year, the "Annual Reportable Amount of a Chemical of Special Concern"
                      for each such chemical, as defined in 40 CFR 372.27(a)(2), did not exceed 500 pounds for this
                      reporting year, and that the chemical was manufactured, or processed, or otherwise used in an
                      amount not exceeding 1 million pounds during this reporting year."
                 </xsl:when>
                 <xsl:otherwise>
				 I hereby certify that to the best of my knowledge and belief, for each toxic chemical listed in the statement,
				 the annual reportable amount as defined in 40 CFR 372.27 (a), did not exceed 500 pounds for this reporting year
				 and the chemical was manufactured, processed, or otherwise used in an amount not exceeding 1 million pounds during
				 this reporting year.
				 </xsl:otherwise>
                </xsl:choose>
               </p>
               <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt" frame="above">
                <tr>
                 <td>
                  <p style="font-size: 8pt">Name and official title of owner/operator or senior management official:</p>
                 </td>
                 <td>
                  <p style="font-size: 8pt">Signature:</p>
                 </td>
                 <td>
                  <p style="font-size: 8pt">Date Signed: </p>
                 </td>
                </tr>
                <tr>
                      <td>
  						<p>
      						<span class="answerText"><xsl:value-of select="TRI:CertifierName"/>&#160;&#160;&#160;</span>
      						<span class="answerText"><xsl:value-of select="TRI:CertifierTitleText"/></span>
  						</p>
					  </td>
					  <td>
    					<p>
      						<span style="font-size: 9pt; color: red; font-weight: bold;">Reference Copy: Copy of Record Resides in CDX</span>
    					</p>
					  </td>
					  <td>
     					<span class="answerText">
					    	<xsl:if test="TRI:CertificationSignedDate != '' and TRI:CertificationSignedDate != '1900-01-01'">
	                    		<xsl:value-of select="TRI:CertificationSignedDate"/>
	                   		</xsl:if>
     					</span>
					  </td>
                    </tr>
               </table>
               </td>
             </tr>
             <tr>
              <td align="left" colspan="3">
               <p style="font-size: 8pt">SECTION 4. FACILITY IDENTIFICATION </p>
              </td>
             </tr>
             <tr>
              <td colspan="3">
               <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0" border="1" frame="void" style="font-size: 8pt">
                <tr>
                 <td width="5%">4.1</td>
                 <td colspan="4" width="45%">&#160;</td>
                 <td colspan="2" width="20%">TRI Facility ID Number</td>
                 <td colspan="2" width="30%">
                  <p>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
                   &#160;</span>
                  </p>
                 </td>
                </tr>
 				<xsl:choose>
                      <xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
	                    <tr>
	                      <td colspan="5">
	                        <p style="font-size: 7pt">
	                          <u style="font-size: 7pt">Facility or Establishment Name</u>
	                          <br/>
	                          <span class="answerText">
	                            <xsl:value-of select="../TRI:Facility/sc:FacilitySiteName"/>
	                          </span>
	                        </p>
	                      </td>
	                      <td colspan="4">
	                        <p style="font-size: 7pt">
	                          <u style="font-size: 7pt">Facility or Establishment Name or Mailing Address(if different from street address)</u>
	                          <br/>
	                          <span class="answerText">
	                            <xsl:value-of select="../TRI:Facility/TRI:MailingFacilitySiteName"/>
	                          </span>
	                        </p>
	                      </td>
	                    </tr>
                      </xsl:when>
                      <xsl:otherwise>
                        <tr>
		                  <td colspan="9">
		                   <p>
		                    <u style="font-size: 7pt">Facility or Establishment Name</u>
		                   </p>
		                   <p>
		                    <span class="answerText">
		                     <xsl:value-of select="../TRI:Facility/sc:FacilitySiteName"/>
		                    &#160;</span>
		                   </p>
		                  </td>
		                </tr>
                      </xsl:otherwise>
                </xsl:choose>
                <tr>
                 <td colspan="5">
                  <p>
                   <u style="font-size: 7pt">Street</u>
                  </p>
                  <p>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocationAddressText"/>
                   &#160;</span>
                  </p>
                 </td>
                 <td colspan="4">
                  <p>
                   <u style="font-size: 7pt">Mailing Address (if different from physical street address)</u>
                  </p>
                  <p>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:MailingAddressText"/>
                   &#160;</span>
                  </p>
                 </td>
                </tr>
                <tr>
                 <td colspan="5">
                  <p>
                  <xsl:choose>
                   <xsl:when test="TRI:SubmissionReportingYear >= '2012' ">
                      <u style="font-size: 7pt">City/County/Tribe/State/ZIP Code</u>
                      <br/>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocalityName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                   &#160;</span>
                    /
                   <span class="answerText">
                     BIA Code: <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:TribalIdentity/sc:TribalCode"/>
                     &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:AddressPostalCode"/>
                   &#160;</span>
                   </xsl:when>

                   <xsl:otherwise>
                     <u style="font-size: 7pt">City/County/State/ZIP Code</u>
                      <br/>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:LocalityName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:CountyIdentity/sc:CountyName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:StateIdentity/sc:StateName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/sc:LocationAddress/sc:AddressPostalCode"/>
                   &#160;</span>
                   </xsl:otherwise>
                   </xsl:choose>
                  </p>
                 </td>
                 <td colspan="3">
                  <p>
                   <u style="font-size: 7pt">City/State/ZIP Code</u>
                   <br/>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:MailingAddressCityName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:StateIdentity/sc:StateName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:AddressPostalCode"/>
                   &#160;</span>
                  </p>
                 </td>
                 <td colspan="1" width="15%">
                  <p>
                   <u style="font-size: 7pt">Country (Non-US)</u>
                   <br/>
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/sc:CountryIdentity/sc:CountryName"/>
                   &#160;</span>
                   /
                   <span class="answerText">
                    <xsl:value-of select="../TRI:Facility/TRI:MailingAddress/TRI:ProvinceNameText"/>
                   &#160;</span>
                  </p>
                 </td>
                </tr>
                <tr>
                 <td width="5%">
                  <p style="font-size: 8pt">4.2</p>
                 </td>
                 <td colspan="4">
                  <p style="font-size: 8pt">
                   This report contains information for : (
                   <u>Important:</u>
                   check c or d if applicable)
                  </p>
                 </td>
                 <td colspan="2">
                  c. [
                  <xsl:choose>
                   <xsl:when test="TRI:SubmissionFederalFacilityIndicator = 'Y'">
                    <span class="answerText">X</span>
                   </xsl:when>
                  </xsl:choose>
                  ] A Federal facility
                 </td>
                 <td colspan="2">
                  d. [
                  <xsl:choose>
                   <xsl:when test="TRI:SubmissionGOCOFacilityIndicator = 'true'">
                    <span class="answerText">X</span>
                   </xsl:when>
                  </xsl:choose>
                  ] GOCO
                 </td>
                </tr>
                <tr>
                 <td width="5%">
                  <p style="font-size: 8pt">4.3</p>
                 </td>
                 <td colspan="2" nowrap="nowrap" align="center">
                  <p style="font-size: 8pt">Technical Contact name</p>
                 </td>
                 <td colspan="2">
                  <span class="answerText">
                   <xsl:value-of select="TRI:TechnicalContactNameText/sc:IndividualFullName"/>
                  &#160;</span>
                 </td>
                 <td colspan="2" nowrap="nowrap">
                  <p>
                   <u style="font-size: 7pt">Email Address</u>
                   <br/>
                   <span class="answerText">
                    <xsl:value-of select="TRI:TechnicalContactEmailAddressText"/>
                   &#160;</span>
                  </p>
                 </td>
                 <td colspan="2" nowrap="nowrap">
                  <p>
                   <xsl:choose>
						<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
							<u style="font-size: 7pt">Telephone Number (include area code and ext.)</u>
						</xsl:when>
        				<xsl:otherwise>
							<u style="font-size: 7pt">Telephone Number (include area code)</u>
        				</xsl:otherwise>
					</xsl:choose>
                   <br/>
                   <span class="answerText">
                    <xsl:value-of select="substring(TRI:TechnicalContactPhoneText,1, 3)"/>&#45;<xsl:value-of select="substring(TRI:TechnicalContactPhoneText,4, 3)"/>&#45;<xsl:value-of select="substring(TRI:TechnicalContactPhoneText,7)"/>
                   &#160;</span>
                   <xsl:if test="TRI:SubmissionReportingYear &gt; '2013' ">
                          	 <span class="answerText">
                            	<xsl:if test="string-length(TRI:TechnicalContactPhoneExtText) &gt; 0 ">
									&#045; &#160;<xsl:value-of select="TRI:TechnicalContactPhoneExtText"/>
								</xsl:if>
                          	&#160;</span>
                    </xsl:if>
                  </p>
                 </td>
                </tr>
                <xsl:choose>
                 <xsl:when test="TRI:SubmissionReportingYear >= '2007' or TRI:SubmissionReportingYear &lt;= '2004'">
                  <tr>
                   <td width="5%" style="font-size: 8pt">
                    <p style="font-size: 8pt">4.4</p>
                   </td>
                   <td colspan="2" nowrap="nowrap" align="center">
                    <p style="font-size: 8pt">Public Contact name </p>
                   </td>
                   <td colspan="2">
                    <span class="answerText">
                     <xsl:value-of select="TRI:PublicContactNameText"/>
                    &#160;</span>
                   </td>
                   <td colspan="2" nowrap="nowrap">
                    <p style="font-size: 8pt">
                     <u style="font-size: 7pt">Email Address</u>
                     <br/>
                     <span class="answerText">
                      <xsl:value-of select="TRI:PublicContactEmailAddressText"/>
                     &#160;</span>
                    </p>
                   </td>
                   <td colspan="2" nowrap="nowrap">
                    <p style="font-size: 8pt">
                   <xsl:choose>
						<xsl:when test="TRI:SubmissionReportingYear &gt; '2013' ">
							<u style="font-size: 7pt">Telephone Number (include area code and ext.)</u>
						</xsl:when>
        				<xsl:otherwise>
							<u style="font-size: 7pt">Telephone Number (include area code)</u>
        				</xsl:otherwise>
					</xsl:choose>
                     <br/>
                     <span class="answerText">
                     <xsl:if test="string-length(TRI:PublicContactPhoneText)  &gt; 0">
                      <xsl:value-of select="substring(TRI:PublicContactPhoneText,1, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,4, 3)"/>&#45;<xsl:value-of select="substring(TRI:PublicContactPhoneText,7)"/>
                      </xsl:if>
                     &#160;</span>
                     <xsl:if test="TRI:SubmissionReportingYear &gt; '2013' ">
                          		 <span class="answerText">
                            		<xsl:if test="string-length(TRI:PublicContactPhoneExtText) &gt; 0 ">
										&#045; &#160;<xsl:value-of select="TRI:PublicContactPhoneExtText"/>
									</xsl:if>
                          		&#160;</span>
                     </xsl:if>

                    </p>
                   </td>
                  </tr>
                 </xsl:when>
                 <xsl:otherwise>
                  <tr>
                   <td width="5%">
                    <p style="font-size: 8pt">4.4</p>
                   </td>
                   <td colspan="8">
                    <p style="font-size: 8pt">Intentionally left blank</p>
                   </td>
                  </tr>
                 </xsl:otherwise>
                </xsl:choose>
                <xsl:choose>
                 <xsl:when test="TRI:SubmissionReportingYear &lt;= '2005'">
                  <tr>
                   <td width="5%" style="font-size: 8pt">
                    <p style="font-size: 8pt">4.5</p>
                   </td>
                   <td colspan="2" nowrap="nowrap" width="35%" align="center">
                    <p style="font-size: 8pt">SIC Code(s) (4 digits)</p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     a.
                     <xsl:for-each select="../TRI:Facility/TRI:FacilitySIC">
                      <xsl:choose>
                       <xsl:when test="sc:SICPrimaryIndicator = 'Primary'">
                        <span class="answerText">
                         <xsl:value-of select="sc:SICCode"/>
                         (Primary)
                        &#160;</span>
                       </xsl:when>
                      </xsl:choose>
                     </xsl:for-each>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     b.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 1">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[2]/sc:SICCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     c.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 2">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[3]/sc:SICCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     d.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 3">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[4]/sc:SICCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     e.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 4">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[5]/sc:SICCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     f.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilitySIC) > 5">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilitySIC[6]/sc:SICCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                  </tr>
                 </xsl:when>
                 <xsl:otherwise>
                  <tr>
                   <td width="5%" style="font-size: 8pt">
                    <p style="font-size: 8pt">4.5</p>
                   </td>
                   <td colspan="2" nowrap="nowrap" width="35%" align="center">
                    <p style="font-size: 8pt">NAICS Code(s) (6 digits)</p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     a.
                     <xsl:for-each select="../TRI:Facility/TRI:FacilityNAICS">
                      <xsl:choose>
                       <xsl:when test="sc:NAICSPrimaryIndicator = 'Primary'">
                        <span class="answerText">
                         <xsl:value-of select="sc:NAICSCode"/>
                         (Primary)
                        &#160;</span>
                       </xsl:when>
                      </xsl:choose>
                     </xsl:for-each>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     b.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 1">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[2]/sc:NAICSCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     c.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 2">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[3]/sc:NAICSCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     d.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 3">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[4]/sc:NAICSCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     e.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 4">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[5]/sc:NAICSCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                   <td colspan="1" width="10%">
                    <p style="font-size: 8pt">
                     f.
                     <xsl:choose>
                      <xsl:when test="count(../TRI:Facility/TRI:FacilityNAICS) > 5">
                       <span class="answerText">
                        <xsl:value-of select="../TRI:Facility/TRI:FacilityNAICS[6]/sc:NAICSCode"/>
                       &#160;</span>
                      </xsl:when>
                     </xsl:choose>
                    </p>
                   </td>
                  </tr>
                 </xsl:otherwise>
                </xsl:choose>
                <tr>
                 <td colspan="9">
                  <table summary="table used for layout purposes" width="100%" cellpadding="1" cellspacing="0" border="1" frame="void" style="font-size: 8pt" rules="all">
                   <tr>
                    <td width="5%" style="font-size: 8pt">
                     <p style="font-size: 8pt">4.7</p>
                    </td>
                    <td nowrap="nowrap">
                     <p style="font-size: 8pt">
                      Dun and Bradstreet
                      <br/>
                      Number(s) (9 digits)
                     </p>
                    </td>
                   </tr>
                   <tr>
                    <td colspan="2">
                     <p style="font-size: 8pt">
                      a.
                      <span class="answerText">
                       <xsl:value-of select="../TRI:Facility/TRI:FacilityDunBradstreetCode[1]"/>
                      &#160;</span>
                     </p>
                    </td>
                   </tr>
                   <tr>
                    <td colspan="2">
                     <p style="font-size: 8pt">
                      b.
                      <span class="answerText">
                       <xsl:value-of select="../TRI:Facility/TRI:FacilityDunBradstreetCode[2]"/>
                      &#160;</span>
                     </p>
                    </td>
                   </tr>
                  </table>
                 </td>
                </tr>
                <tr>
                 <td align="left" colspan="9">
                  <p style="font-size: 8pt">SECTION 5. PARENT COMPANY INFORMATION</p>
                 </td>
                </tr>
                <tr>
                 <td width="5%" style="font-size: 8pt">
                  <p style="font-size: 8pt">5.1</p>
                 </td>
                 <td colspan="2" align="center">
                  <p style="font-size: 8pt">
				  <xsl:choose>
					<xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
					  Name of Parent Company
					</xsl:when>
					<xsl:otherwise>
					  Name of U.S. Parent Company (for TRI Reporting purposes)
					</xsl:otherwise>
			       </xsl:choose>
                  </p>
                 </td>
                 <xsl:choose>
						  <xsl:when test="TRI:SubmissionReportingYear &gt; '2010'">
						  <td colspan="5">
                        <span class="answerText">
                        <xsl:if test="../TRI:Facility/TRI:ParentCompanyNameText != 'NA'">
                    		<xsl:value-of select="../TRI:Facility/TRI:ParentCompanyNameText"/>
                   		</xsl:if>
                        &#160;</span>
                        <br/>
                      </td>
					  <td colspan="1">
                        <p style="font-size: 8pt">
						    No U.S. Parent Company (for TRI Reporting purposes) [
					    <xsl:choose>
                            <xsl:when test="../TRI:Facility/TRI:ParentCompanyNameNAIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ]
                        </p>
                      </td>
						  </xsl:when>
						  <xsl:otherwise>
					                       <td colspan="1">
                        <p style="font-size: 8pt">
						    NA [

					    <xsl:choose>
                            <xsl:when test="../TRI:Facility/TRI:ParentCompanyNameNAIndicator = 'true'">
                              <span class="answerText">X</span>
                            </xsl:when>
                          </xsl:choose>
                          ]
                        </p>
                      </td>
                      <td colspan="5">
                        <span class="answerText">
                          <xsl:value-of select="../TRI:Facility/TRI:ParentCompanyNameText"/>
                        &#160;</span>
                        <br/>
                      </td>
					  	  </xsl:otherwise>
					  </xsl:choose>
                </tr>
                <tr>
                 <td width="5%" style="font-size: 8pt">
                  <p style="font-size: 8pt">5.2</p>
                 </td>
                 <td colspan="2" align="left">
                  <p style="font-size: 8pt">Parent Company's Dun &amp; Bradstreet Number </p>
                 </td>
                 <td colspan="1">
                  <p style="font-size: 8pt">
                   NA [
                   <xsl:choose>
                    <xsl:when test="../TRI:Facility/TRI:ParentDunBradstreetCode = 'NA'">
                     <span class="answerText">X</span>
                    </xsl:when>
                   </xsl:choose>
                   ]
                  </p>
                 </td>
                 <td colspan="5">
                  <xsl:choose>
                   <xsl:when test="../TRI:Facility/TRI:ParentDunBradstreetCode != 'NA'">
                    <span class="answerText">
                     <xsl:value-of select="../TRI:Facility/TRI:ParentDunBradstreetCode"/>
                    &#160;</span>
                   </xsl:when>
                  </xsl:choose>
                  <br/>
                 </td>
                </tr>
               </table>
              </td>
             </tr>
            </table>
           </center>
           <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="1" style="font-size: 8pt" border="0">
            <tr>
             <td width="60%">
              <p style="font-size: 8pt">
              	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
               		EPA Form 9350-2 (Rev. <xsl:value-of select="$RevisionDateFormA"/>) - Previous editions are obsolete.
               	</xsl:if>
              </p>
             </td>
             <td width="30%">
              <p style="font-size: 8pt">Printed using TRI-MEweb</p>
             </td>
            </tr>
           </table>

           <!-- Start Form A Page Two -->
          <xsl:for-each select="//TRI:Report[TRI:ReportType/TRI:ReportTypeCode = current()/TRI:ReportType/TRI:ReportTypeCode
                                                         and concat(TRI:TechnicalContactNameText/sc:IndividualFullName, 'xx') = concat(current()/TRI:TechnicalContactNameText/sc:IndividualFullName, 'xx')
                                                         and concat(TRI:TechnicalContactPhoneText, 'xx') = concat(current()/TRI:TechnicalContactPhoneText, 'xx')
                                                         and concat(TRI:TechnicalContactEmailAddressText, 'xx') = concat(current()/TRI:TechnicalContactEmailAddressText, 'xx')
                                                         and concat(TRI:PublicContactNameText/sc:IndividualFullName, 'xx') = concat(current()/TRI:PublicContactNameText/sc:IndividualFullName, 'xx')
                                                         and concat(TRI:PublicContactPhoneText, 'xx') = concat(current()/TRI:PublicContactPhoneText, 'xx')
                                                         and concat(TRI:PublicContactEmailAddressText, 'xx') = concat(current()/TRI:PublicContactEmailAddressText, 'xx')
                                                         and sc:RevisionIndicator = current()/sc:RevisionIndicator
                                                         and concat(TRI:ChemicalReportRevisionCode[1], 'xx') = concat(current()/TRI:ChemicalReportRevisionCode[1], 'xx')
                                                         and concat(TRI:ChemicalReportRevisionCode[2], 'xx') = concat(current()/TRI:ChemicalReportRevisionCode[2], 'xx')
                                                         and concat(TRI:ChemicalReportWithdrawalCode[1], 'xx') = concat(current()/TRI:ChemicalReportWithdrawalCode[1], 'xx')
                                                         and concat(TRI:ChemicalReportWithdrawalCode[2], 'xx') = concat(current()/TRI:ChemicalReportWithdrawalCode[2], 'xx')
                                                         and ../TRI:Facility = current()/../TRI:Facility
                                                         ]">
            <xsl:choose>
            <xsl:when test="(position() - 1) mod 4 = 0">
           <p style="page-break-before: always">&#160;</p>
           <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
             <td width="60%">
              <p style="font-size: 8pt">
               <xsl:call-template name="FormAPage2Links">
                  <xsl:with-param name="reportID"><xsl:value-of select="$formID"/></xsl:with-param>
                  <xsl:with-param name="lastPosition"><xsl:value-of select="last()"/></xsl:with-param>
                  <xsl:with-param name="currentPosition">4</xsl:with-param>
               </xsl:call-template>
               	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	               IMPORTANT: Read instructions before completing form; type or use fill-and-print form
	            </xsl:if>
              </p>
             </td>
             <td width="10%">

             </td>
            </tr>
           </table>

           <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt">
            <tr>
             <td colspan="2">
              <table summary="table used for layout purposes" width="100%" style="font-size: 10pt" cellspacing="0" cellpadding="1">
               <tr>
                <td width="80%" align="center" style="font-size: 9pt">
                  <span style="font-weight:bold">EPA FORM A</span>
                  <br/>
                  <span style="font-weight:bold">PART II. CHEMICAL IDENTIFICATION</span>
                  <br/>
                  <p style="font-size: 8pt">
                   <xsl:choose>
                    <xsl:when test="TRI:SubmissionReportingYear = '2005'">
                        Do not use this form for reporting PBT chemicals including Dioxin and Dioxin-like Compounds*
                    </xsl:when>
                    <xsl:otherwise>
                        Do not use this form for reporting Dioxin and Dioxin-like Compounds*</xsl:otherwise>
                   </xsl:choose>
                  </p>
                </td>
               </tr>
              </table>
             </td>
             <td>TRI Facility ID Number
               <hr />
               <span class="answerText">
                <xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/>
               &#160;</span>
             </td>
            </tr>
           </table>
           </xsl:when>
           </xsl:choose>

           <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt">
            <tr>
             <td colspan="2">
              <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0" cellpadding="1">
               <tr>
                <td align="left" width="75%">
                 <p style="font-size: 7pt">SECTION 1. TOXIC CHEMICAL IDENTITY</p>
                </td>
                <td align="right">
                 <p style="font-size: 7pt">Report <span class="smallAnswer"><xsl:value-of  select="position()"/></span> of <span class="smallAnswer"><xsl:value-of  select="last()"/></span></p>
                </td>
               </tr>
              </table>
             </td>
            </tr>
              <xsl:for-each select="TRI:ChemicalIdentification">
               <tr>
                <td align="center" width="5%">
                 <p style="font-size: 7pt">1.1</p>
                </td>
                <td width="95%" style="font-size: 7pt">
                  CAS Number (Important: Enter only one number as it appears on the
                  Section 313 list. Enter category code if reporting a chemical
                  category.)
                  <hr/>
                  <span style="color: blue;text-indent: 5em;font-weight:bold">
                   <xsl:value-of select="sc:CASNumber"/>
                  &#160;</span>
                </td>
               </tr>
               <tr>
                <td align="center" width="5%">
                 <p style="font-family: arial;font-size: 7pt">1.2</p>
                </td>
                <td style="font-size: 7pt">
                  Toxic Chemical or Chemical Category Name (Important: Enter only one
                  name exactly as it appears on the Section 313 list.)
                  <hr/>
                  <span style="color: blue;font-size: 7pt;text-indent: 5em;font-weight:bold">
                   <xsl:value-of select="TRI:ChemicalNameText"/>
                  &#160;</span>
                </td>
               </tr>
              </xsl:for-each>
              <tr>
               <td align="center" width="5%">
                <p style=" font-family: arial;font-size: 7pt">1.3</p>
               </td>
               <td style="font-size: 7pt">
                 Generic Chemical Name (Important: Complete only if Part I, Section
                 2.1 is checked "Yes". Generic Name must be structurally descriptive).
                 <hr/>
                 <span style="color: blue;font-size: 7pt;text-indent: 5em;font-weight:bold">NA</span>
               </td>
              </tr>
              <tr>
               <td colspan="2">
                <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" cellspacing="0"
                       cellpadding="1">
                 <tr>
                  <td align="left" colspan="2">
                   <p style="font-family: arial;font-size: 8pt">
                      SECTION 2. MIXTURE COMPONENT IDENTITY (Important: DO NOT complete this section if you completed Section 1.)
                   </p>
                  </td>
                 </tr>
                </table>
               </td>
              </tr>
              <tr>
               <td align="center" width="5%">
                <p style="font-family: arial;font-size: 7pt">2.1</p>
               </td>
               <td style="font-size: 8pt">
                 Generic Chemical Name Provided by Supplier (Important: Maximum of 70
                 characters, including numbers, spaces, and punctuation.)
                 <hr/>
                 <b style="color: blue;font-size: 9pt; font-family:arial">
                  <xsl:value-of select="TRI:ChemicalIdentification/TRI:ChemicalMixtureNameText"/>
                 </b>
                 <br/>
               </td>
              </tr>
             </table>
             <br />
        <xsl:choose>
        <xsl:when test="((position() mod 4 = 0) or (position() = last()))">

               <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
                <tr>
                 <td colspan="2" align="center">
                  <p style="font-family: arial;font-size: 7pt">
                   <xsl:choose>
                    <xsl:when test="TRI:SubmissionReportingYear = '2005'">
                        *See the TRI Reporting Forms and Instructions Manual for the list of PBT
                         Chemicals(including Dioxin and Dioxin-like Compounds)
                    </xsl:when>
                    <xsl:otherwise>*See the TRI Reporting Forms and Instructions
                                   Manual for the TRI-listed Dioxin and Dioxin-like
                                   Compounds</xsl:otherwise>
                   </xsl:choose>
                  </p>
                 </td>
                </tr>
                <tr>
                 <td width="60%">
                  <p style="font-family: arial;font-size: 7pt">
                  	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
                   		EPA Form 9350-2 (Rev. <xsl:value-of select="$RevisionDateFormA"/>) - Previous editions are obsolete.
                   	</xsl:if>
                  </p>
                 </td>
                 <td width="40%" align="right" style="font-size: 7pt">
                  <p style="font-family: arial;font-size: 7pt">&#160;</p>
                 </td>
                </tr>
               </table>
            </xsl:when>
          </xsl:choose>
           <!-- End Form A Page Two -->
           </xsl:for-each>
        </xsl:if>
      </xsl:otherwise>
      </xsl:choose>
      </xsl:for-each>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="ScheduleOnePageOne">
    <xsl:param name="baseStreamID" />
    <xsl:param name="formID" />

    <xsl:param name="OMBNumberSchedule1" />
    <xsl:param name="ApprovalDateSchedule1" />

    <xsl:param name="fugitiveRelease" select="TRI:OnsiteReleaseQuantity[TRI:EnvironmentalMediumCode = 'AIR FUG' and $baseStreamID = 0]" />
    <xsl:param name="stackRelease" select="TRI:OnsiteReleaseQuantity[TRI:EnvironmentalMediumCode = 'AIR STACK' and $baseStreamID = 0]" />
    <xsl:param name="firstStreamRelease" select="TRI:OnsiteReleaseQuantity[TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 1]" />
    <xsl:param name="secondStreamRelease" select="TRI:OnsiteReleaseQuantity[TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 2]" />
    <xsl:param name="thirdStreamRelease" select="TRI:OnsiteReleaseQuantity[TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 3]" />

    <div class="landscapeArea">
        <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 8pt" border="0">
              <tr>
                <td width="30%">
                	<xsl:if test="TRI:SubmissionReportingYear &lt; '2014' ">
	                  Form Approved OMB Number:<span class="smallAnswer"><xsl:value-of select="$OMBNumberSchedule1" />&#160;</span><br />
    	              Approval Expires: <span class="smallAnswer"><xsl:value-of select="$ApprovalDateSchedule1" />&#160;</span>
    	            </xsl:if>
                </td>
                <td width="10%">
                  <p style="font-size: 8pt"><b>Page 1 of 4</b></p>
                </td>
              </tr>
            </table>

          <center>
            <table summary="table used for layout purposes" border="1" cellspacing="0" cellpadding="1" width="100%" style="font-size: 8pt;">

              <tr>
                <td colspan="13" align="center">
                  <table summary="table used for layout purposes" width="100%" style="font-size: 8pt" border="0" cellspacing="0" cellpadding="0" frame="void">
                    <tr>
                      <td style="font-size: 10pt"><b> EPA </b></td>
                      <td align="center" style="font-size: 14pt"><b>FORM R Schedule 1</b></td>
                      <td><p style="font-size: 8pt">TRI Facility ID Number:</p></td>
                    </tr>
                    <tr>
                      <td><p style="font-size: 8pt">United States<br />Environmental Protection<br />Agency</p></td>
                      <td align="center"><p style="font-size: 14pt"><b>PART II.  CHEMICAL-SPECIFIC INFORMATION (continued)</b></p></td>
                      <td class="answerText"><xsl:value-of select="../TRI:Facility/TRI:FacilityIdentifier"/></td>
                    </tr>
                  </table>
                </td>
              </tr>

              <tr>
                <td align="left" colspan="13" style="font-size: 8pt">
                  <p style="font-size: 9pt"><b>Section 5. Quantity Of Dioxin And Dioxin-Like Compounds Entering Each Environmental Medium On-site</b></p>
                </td>
              </tr>

              <tr>
                <td colspan="2" rowspan="2" style="background-color: gray">&#160;</td>
                <td align="center" style="font-size: 8pt"><b>5.1</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="$fugitiveRelease/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                    X
                  </xsl:if>&#160;
                </td>

                <td align="center" style="font-size: 8pt"><b>5.2</b></td>
                <td align="center" style="font-size: 8pt">NA</td>
                <td align="center" class="smallAnswer">
                  <xsl:if test="$stackRelease/TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
                    X
                  </xsl:if>&#160;
                </td>

                <td align="center" colspan="5" style="font-size: 8pt">
                  <b>5.3</b>&#160;&#160;&#160;Discharges to receiving streams or water
                  bodies&#160;&#160;&#160;&#160;&#160;&#160;
				  <xsl:choose>
					<xsl:when test="TRI:SubmissionReportingYear &lt; '2011'">
					  &#160;
					</xsl:when>
					<xsl:otherwise>
					  NA&#160;&#160;&#160;
                  [&#160;<xsl:for-each select="TRI:OnsiteReleaseQuantity">
					  <xsl:choose>
					    <xsl:when test="TRI:EnvironmentalMediumCode = 'WATER'">
					      <xsl:choose>
					        <xsl:when test="TRI:OnsiteWasteQuantity/TRI:WasteQuantityNAIndicator = 'true'">
					          <span class="smallAnswer">X</span>
					        </xsl:when>
					      </xsl:choose>
					    </xsl:when>
					  </xsl:choose>
					</xsl:for-each>&#160;]
					</xsl:otherwise>
				  </xsl:choose>
                </td>
              </tr>
              <tr>
                <td align="center" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">Fugitive or non-point air emissions</p>
                </td>

                <td align="center" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">Stack or point air emissions</p>
                </td>

                <td align="center" colspan="3" style="font-size: 8pt">
                  <p style="font-size: 8pt">5.3.<xsl:value-of select="$baseStreamID + 1" />&#160;
                    <xsl:value-of select="$firstStreamRelease/TRI:WaterStream/TRI:StreamName" />
                  </p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">5.3.<xsl:value-of select="$baseStreamID + 2" />&#160;
                    <xsl:value-of select="$secondStreamRelease/TRI:WaterStream/TRI:StreamName" />
                  </p>
                </td>
                <td align="center" style="font-size: 8pt">
                  <p style="font-size: 8pt">5.3.<xsl:value-of select="$baseStreamID + 3" />&#160;
                    <xsl:value-of select="$thirdStreamRelease/TRI:WaterStream/TRI:StreamName" />
                  </p>
                </td>
              </tr>

            <xsl:call-template name="ScheduleOnePageOneRow">
              <xsl:with-param name="baseStreamID"><xsl:value-of select="$baseStreamID" /></xsl:with-param>
            </xsl:call-template>

              <tr>
                <td align="left" colspan="13" style="font-size: 8pt">
                  <p style="font-size: 8pt">
                    If additional pages of Section 6.1 or 6.2 are attached, indicate the total number of pages in this box&#160;&#160;
                    <span style="border: 1px solid black; padding: 2px; text-align:center; width: 25px;">
                      <xsl:if test="count(TRI:OnsiteReleaseQuantity/TRI:WaterStream) &gt; 3">
                          <span class="answerText"><xsl:value-of select="(floor(count(TRI:OnsiteReleaseQuantity/TRI:WaterStream) div 3)) + 1" />&#160;</span>
                      </xsl:if>&#160;
                    &#160;</span>
                    <br />
                    and indicate the Section 6.1 or 6.2 page number in this box&#160;&#160;
                    <span style="border: 1px solid black; padding: 2px; text-align:center; width: 25px;">
                      <xsl:if test="count(TRI:OnsiteReleaseQuantity/TRI:WaterStream) &gt; 3">
                          <span class="answerText"><xsl:value-of select="($baseStreamID div 3) + 1" />&#160;</span>
                      </xsl:if>&#160;
                    &#160;</span>&#160;
                    (Example: 1,2,3, etc.)
                  </p>
                </td>
              </tr>

            </table>
          </center>

          <table summary="table used for layout purposes" width="100%" cellspacing="0" cellpadding="0" style="font-size: 7pt" border="0">
            <tr>
              <td width="60%">
                <p style="font-size: 8pt">
                  EPA Form 9350-3
                </p>
              </td>
              <td width="30%">
                <p style="font-size: 8pt">Printed using TRI-MEweb</p>
              </td>
            </tr>
          </table>
          </div>
    </xsl:template>

    <xsl:template name="ScheduleOnePageOneRow">
        <xsl:param name="baseStreamID" />
        <xsl:param name="i">1</xsl:param>
        <xsl:param name="teqNodeName"><xsl:value-of select="concat('TRI:ToxicEquivalency',$i,'Value')" /></xsl:param>
        <xsl:param name="fugitiveTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'AIR FUG' and $baseStreamID = 0]" />
        <xsl:param name="stackTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'AIR STACK' and $baseStreamID = 0]" />
        <xsl:param name="firstStreamTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 1]" />
        <xsl:param name="secondStreamTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 2]" />
        <xsl:param name="thirdStreamTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:WaterStream/TRI:WaterSequenceNumber = $baseStreamID + 3]" />

        <xsl:if test="$i &lt; 18">
        <tr>
          <xsl:if test="$i = 1">
            <td align="center" rowspan="17" style="font-size: 8pt">
              <p style="font-size: 8pt"><b>D. Mass<br />(grams)<br />of<br />each<br />compound<br />in<br />the<br />category<br />(1-17)</b></p>
            </td>
          </xsl:if>

            <td align="center" style="font-size: 8pt">
              <xsl:value-of select="$i" />
            </td>
            <td align="center" colspan="3" class="teqAnswer">
                <xsl:value-of select="$fugitiveTEQ/*[name() = $teqNodeName]" /><br />
            </td>
            <td align="center" colspan="3" class="teqAnswer">
                <xsl:value-of select="$stackTEQ/*[name() = $teqNodeName]" /><br />
            </td>
            <td align="center" colspan="3" class="teqAnswer">
                <xsl:value-of select="$firstStreamTEQ/*[name() = $teqNodeName]" /><br />
            </td>
            <td align="center" class="teqAnswer">
                <xsl:value-of select="$secondStreamTEQ/*[name() = $teqNodeName]" /><br />
            </td>
            <td align="center" class="teqAnswer">
                <xsl:value-of select="$thirdStreamTEQ/*[name() = $teqNodeName]" /><br />
            </td>
          </tr>

          <xsl:call-template name="ScheduleOnePageOneRow">
              <xsl:with-param name="i"><xsl:value-of select="$i + 1" /></xsl:with-param>
              <xsl:with-param name="baseStreamID"><xsl:value-of select="$baseStreamID" /></xsl:with-param>
          </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="ScheduleOnePageTwoRow">
        <xsl:param name="i">1</xsl:param>
        <xsl:param name="teqNodeName"><xsl:value-of select="concat('TRI:ToxicEquivalency',$i,'Value')" /></xsl:param>
        <xsl:param name="uicClass1TEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'UNINJ I']" />
        <xsl:param name="uicClass25TEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'UNINJ IIV']" />
        <xsl:param name="rcraLandfillTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'RCRA C']" />
        <xsl:param name="otherLandfillTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'OTH LANDF']" />
        <xsl:param name="farmingTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'LAND TREA']" />
        <xsl:param name="rcraSITEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'SI 5.5.3A']" />
        <xsl:param name="otherSITEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'SI 5.5.3B']" />
        <xsl:param name="otherTEQ" select="TRI:OnsiteReleaseQuantity/TRI:OnsiteWasteQuantity/TRI:ToxicEquivalencyIdentification[../../TRI:EnvironmentalMediumCode = 'OTH DISP']" />

        <xsl:if test="$i &lt; 18">
            <tr>
              <xsl:if test="$i = 1">
              <td align="center" rowspan="17" style="font-size: 8pt">
                <b>C. Mass<br />(grams)<br />of<br />each<br />compound<br />in<br />the<br />category<br />(1-17)</b>
              </td>
              </xsl:if>
              <td align="center" style="font-size: 8pt">
                <xsl:value-of select="$i" />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$uicClass1TEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$uicClass25TEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$rcraLandfillTEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$otherLandfillTEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$farmingTEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$rcraSITEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$otherSITEQ/*[name() = $teqNodeName]" /><br />
              </td>
              <td colspan="3" align="center" class="teqAnswer">
                <xsl:value-of select="$otherTEQ/*[name() = $teqNodeName]" /><br />
              </td>
            </tr>

            <xsl:call-template name="ScheduleOnePageTwoRow">
              <xsl:with-param name="i"><xsl:value-of select="$i + 1" /></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="ScheduleOnePageThreeRowBlank">
        <xsl:param name="i">1</xsl:param>

        <tr>
          <td colspan="2" align="left" style="font-size: 9pt"><b>6.2_<xsl:value-of select="$i" /></b></td>
          <td colspan="16" align="left" style="font-size: 8pt"><b>. Mass (grams) of each compound in the Category (1-17)</b></td>
        </tr>

        <xsl:call-template name="ScheduleOnePageThreeDetailBlank"><xsl:with-param name="j">1</xsl:with-param></xsl:call-template>
        <xsl:call-template name="ScheduleOnePageThreeDetailBlank"><xsl:with-param name="j">2</xsl:with-param></xsl:call-template>
        <xsl:call-template name="ScheduleOnePageThreeDetailBlank"><xsl:with-param name="j">3</xsl:with-param></xsl:call-template>
        <xsl:call-template name="ScheduleOnePageThreeDetailBlank"><xsl:with-param name="j">4</xsl:with-param></xsl:call-template>
    </xsl:template>

    <xsl:template name="ScheduleOnePageThreeDetailBlank">
        <xsl:param name="j">1</xsl:param>

        <tr>
          <td align="center" colspan="2" style="font-size: 8pt">
              <b><xsl:value-of select="$j" />.</b>
          </td>
          <td style="font-size: 8pt">1</td><td>&#160;</td>
          <td style="font-size: 8pt">2</td><td>&#160;</td>
          <td style="font-size: 8pt">3</td><td>&#160;</td>
          <td style="font-size: 8pt">4</td><td>&#160;</td>
          <td style="font-size: 8pt">5</td><td>&#160;</td>
          <td style="font-size: 8pt">6</td><td>&#160;</td>
          <td style="font-size: 8pt">7</td><td>&#160;</td>
          <td style="font-size: 8pt">8</td><td>&#160;</td>
        </tr>
        <tr>
          <td style="font-size: 8pt">9</td><td>&#160;</td>
          <td style="font-size: 8pt">10</td><td>&#160;</td>
          <td style="font-size: 8pt">11</td><td>&#160;</td>
          <td style="font-size: 8pt">12</td><td>&#160;</td>
          <td style="font-size: 8pt">13</td><td>&#160;</td>
          <td style="font-size: 8pt">14</td><td>&#160;</td>
          <td style="font-size: 8pt">15</td><td>&#160;</td>
          <td style="font-size: 8pt">16</td><td>&#160;</td>
          <td style="font-size: 8pt">17</td><td>&#160;</td>
        </tr>
    </xsl:template>

    <xsl:template name="ScheduleOnePageThreeRow">
        <tr>
          <td align="center" colspan="2" style="font-size: 8pt">
              <b><xsl:value-of select="TRI:TransferSequenceNumber + 1" />.</b>
          </td>
          <td style="font-size: 8pt">1</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency1Value" />&#160;</td>

          <td style="font-size: 8pt">2</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency2Value" />&#160;</td>

          <td style="font-size: 8pt">3</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency3Value" />&#160;</td>

          <td style="font-size: 8pt">4</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency4Value" />&#160;</td>

          <td style="font-size: 8pt">5</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency5Value" />&#160;</td>

          <td style="font-size: 8pt">6</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency6Value" />&#160;</td>

          <td style="font-size: 8pt">7</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency7Value" />&#160;</td>

          <td style="font-size: 8pt">8</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency8Value" />&#160;</td>
        </tr>

        <tr>
          <td style="font-size: 8pt">9</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency9Value" />&#160;</td>

          <td style="font-size: 8pt">10</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency10Value" />&#160;</td>

          <td style="font-size: 8pt">11</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency11Value" />&#160;</td>

          <td style="font-size: 8pt">12</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency12Value" />&#160;</td>

          <td style="font-size: 8pt">13</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency13Value" />&#160;</td>

          <td style="font-size: 8pt">14</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency14Value" />&#160;</td>

          <td style="font-size: 8pt">15</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency15Value" />&#160;</td>

          <td style="font-size: 8pt">16</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency16Value" />&#160;</td>

          <td style="font-size: 8pt">17</td>
          <td align="center" class="teqAnswer"><xsl:value-of select="TRI:TransferWasteQuantity/TRI:ToxicEquivalencyIdentification/TRI:ToxicEquivalency17Value" />&#160;</td>
        </tr>
    </xsl:template>

    <xsl:template name="ScheduleOnePageFourRow">
      <xsl:param name="i">1</xsl:param>
      <xsl:param name="teqNodeName"><xsl:value-of select="concat('TRI:ToxicEquivalency',$i,'Value')" /></xsl:param>

      <xsl:if test="$i &lt; 18">
        <tr>
          <xsl:if test="$i = 1">
          <td rowspan="17" align="center" style="font-size: 8pt">
            <b>F. Mass<br />(grams)<br />of<br />each<br />compound<br />in<br />the<br />category<br />(1-17)</b>
          </td>
          </xsl:if>

          <td align="center" style="font-size: 8pt"><xsl:value-of select="$i" /></td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OnsiteUICDisposalQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OnsiteOtherDisposalQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OffsiteUICDisposalQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OffsiteOtherDisposalQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OnsiteEnergyRecoveryQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OffsiteEnergyRecoveryQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OnsiteRecycledQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OffsiteRecycledQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OnsiteTreatedQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:OffsiteTreatedQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
          <td align="center" class="teqAnswer">
            <xsl:value-of select="TRI:SourceReductionQuantity/TRI:ToxicEquivalencyIdentification/*[name() = $teqNodeName]" /><br />
          </td>
        </tr>

        <xsl:call-template name="ScheduleOnePageFourRow">
          <xsl:with-param name="i"><xsl:value-of select="$i + 1" /></xsl:with-param>
        </xsl:call-template>

      </xsl:if>
    </xsl:template>

    <xsl:template name="FormAPage2Links">
      <xsl:param name="lastPosition">0</xsl:param>
      <xsl:param name="currentPosition">0</xsl:param>
      <xsl:param name="reportID">0</xsl:param>
      <xsl:if test="$currentPosition &lt;= $lastPosition and $lastPosition &gt; 4">
          <xsl:call-template name="FormAPage2Links">
            <xsl:with-param name="lastPosition"><xsl:value-of select="$lastPosition"/></xsl:with-param>
            <xsl:with-param name="currentPosition"><xsl:value-of select="$currentPosition + 1"/></xsl:with-param>
            <xsl:with-param name="reportID"><xsl:value-of select="$reportID"/></xsl:with-param>
          </xsl:call-template>
      </xsl:if>
    </xsl:template>
</xsl:stylesheet>
