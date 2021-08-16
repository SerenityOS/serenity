<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
   version="1.1"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:fo="http://www.w3.org/1999/XSL/Format"
>
<xsl:param name="language">en</xsl:param>
<xsl:param name="country">GB</xsl:param>
<xsl:param name="variant">
</xsl:param>
<xsl:variable name="schema">null</xsl:variable>
<xsl:param name="imageroot">file:.</xsl:param>
<xsl:param name="designmode">true</xsl:param>
<xsl:variable name="history">null</xsl:variable>
<xsl:variable name="new_line">
<xsl:text></xsl:text>
</xsl:variable>
<xsl:template match="tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST" xmlns:tns="urn:myworld-com:customer_order_deliv_note_rep">

   
   <xsl:variable name="pageheight">
<xsl:choose>
<xsl:when test="tns:PROCESSING_INFO/tns:FORMATTING_OPTIONS/tns:PAPER_FORMAT = 'LETTER'">27.94cm</xsl:when>
<xsl:otherwise>29.7cm</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<xsl:variable name="pagewidth">
<xsl:choose>
<xsl:when test="tns:PROCESSING_INFO/tns:FORMATTING_OPTIONS/tns:PAPER_FORMAT = 'LETTER'">21.59cm</xsl:when>
<xsl:otherwise>21cm</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<fo:root font-family="Arial" font-size="12pt" font-style="normal" font-weight="normal" text-decoration="none" xmlns:fo="http://www.w3.org/1999/XSL/Format" element-id="17142">
      <fo:layout-master-set element-id="17143">
         <fo:page-sequence-master master-name="vldt-sequence" element-id="17144">
            <fo:repeatable-page-master-reference master-reference="repeat-page" element-id="17145">
            </fo:repeatable-page-master-reference>
         </fo:page-sequence-master>
         
         <xsl:variable name="pagemargin">
<xsl:choose>
<xsl:when test="tns:PROCESSING_INFO/tns:FORMATTING_OPTIONS/tns:PAPER_FORMAT = 'LETTER'">2.3cm</xsl:when>
<xsl:otherwise>2.0cm</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<xsl:variable name="basemargin">2.0cm</xsl:variable>
<fo:simple-page-master margin-bottom="2cm" margin-left="{$pagemargin}" margin-right="2cm" margin-top="2cm" master-name="repeat-page" page-height="{$pageheight}" page-width="{$pagewidth}" element-id="17146">
            
            <fo:region-body margin-bottom="1cm" margin-top="3cm" region-name="xsl-region-body" vldt-object="FoRegionBody" element-id="17147">
            </fo:region-body>
            <fo:region-before extent="3cm" region-name="repeat-page-head" element-id="17148">
            </fo:region-before>
            <fo:region-after extent="1cm" region-name="repeat-page-foot" element-id="17149">
            </fo:region-after>
         </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence force-page-count="no-force" initial-page-number="1" master-reference="vldt-sequence" element-id="17150">
         <fo:static-content flow-name="repeat-page-head" element-id="17151">
         
         </fo:static-content>
         <fo:static-content flow-name="repeat-page-foot" element-id="17152">
         
         </fo:static-content>
         <fo:flow flow-name="xsl-region-body" element-id="17153">
         
            
            <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:ORDER_LINES/tns:ORDER_LINE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
               <fo:table-column column-width="0.566667cm" element-id="17154">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17155">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17156">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17157">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17158">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17159">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17160">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17161">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17162">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17163">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17164">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17165">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17166">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17167">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17168">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17169">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17170">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17171">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17172">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17173">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17174">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17175">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17176">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17177">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17178">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17179">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17180">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17181">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17182">
               </fo:table-column>
               <fo:table-column column-width="0.566667cm" element-id="17183">
               </fo:table-column>
               <xsl:if test="count(/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:ORDER_LINES/tns:ORDER_LINE) &gt; 0">
<fo:table-header border-style="none">
                  <fo:table-row font-weight="bold" element-id="17184">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17185">
                        <fo:block white-space-collapse="false" element-id="17186">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:OWNERSHIP"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17187">
                        <fo:block white-space-collapse="false" element-id="17188">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:OWNER_NAME"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17189">
                        <fo:block white-space-collapse="false" element-id="17190">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CONFIG_ID"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17191">
                        <fo:block white-space-collapse="false" element-id="17192">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CONFIG_SPEC_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17193">
                        <fo:block white-space-collapse="false" element-id="17194">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:REAL_SHIP_DATE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17195">
                        <fo:block white-space-collapse="false" element-id="17196">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:LINE_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17197">
                        <fo:block white-space-collapse="false" element-id="17198">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:REL_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17199">
                        <fo:block white-space-collapse="false" element-id="17200">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:LINE_ITEM_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17201">
                        <fo:block white-space-collapse="false" element-id="17202">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CATALOG_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17203">
                        <fo:block white-space-collapse="false" element-id="17204">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CATALOG_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17205">
                        <fo:block white-space-collapse="false" element-id="17206">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:BUY_QTY_DUE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17207">
                        <fo:block white-space-collapse="false" element-id="17208">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:QTY_REMAINING"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17209">
                        <fo:block white-space-collapse="false" element-id="17210">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17211">
                        <fo:block white-space-collapse="false" element-id="17212">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:FINAL_DELIVERY"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17213">
                        <fo:block white-space-collapse="false" element-id="17214">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:TOTAL_QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17215">
                        <fo:block white-space-collapse="false" element-id="17216">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:SALES_UNIT_MEAS"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17217">
                        <fo:block white-space-collapse="false" element-id="17218">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:REF_ID"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17219">
                        <fo:block white-space-collapse="false" element-id="17220">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:LOCATION_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17221">
                        <fo:block white-space-collapse="false" element-id="17222">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CONDITION_CODE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17223">
                        <fo:block white-space-collapse="false" element-id="17224">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CONDITION_CODE_DESCRIPTION"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17225">
                        <fo:block white-space-collapse="false" element-id="17226">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CONTACT"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17227">
                        <fo:block white-space-collapse="false" element-id="17228">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CUSTOMER_PART_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17229">
                        <fo:block white-space-collapse="false" element-id="17230">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CUSTOMER_PART_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17231">
                        <fo:block white-space-collapse="false" element-id="17232">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CATCH_QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17233">
                        <fo:block white-space-collapse="false" element-id="17234">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CATCH_UOM"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17235">
                        <fo:block white-space-collapse="false" element-id="17236">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:MANUFACTURING_DEPARTMENT"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17237">
                        <fo:block white-space-collapse="false" element-id="17238">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:DELIVERY_SEQUENCE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17239">
                        <fo:block white-space-collapse="false" element-id="17240">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PO_REF"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17241">
                        <fo:block white-space-collapse="false" element-id="17242">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:GTIN_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17243">
                        <fo:block white-space-collapse="false" element-id="17244">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:GTIN14"/>
                        </fo:block>
                     </fo:table-cell>
                  </fo:table-row>
               </fo:table-header>
</xsl:if>
               <fo:table-body border-style="none" element-id="17245">
               <xsl:for-each select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:ORDER_LINES/tns:ORDER_LINE">
                  <fo:table-row element-id="17246">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17247">
                        <fo:block white-space-collapse="false" element-id="17248">
                        <xsl:value-of select="tns:OWNERSHIP"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17249">
                        <fo:block white-space-collapse="false" element-id="17250">
                        <xsl:value-of select="tns:OWNER_NAME"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17251">
                        <fo:block white-space-collapse="false" element-id="17252">
                        <xsl:value-of select="tns:CONFIG_ID"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17253">
                        <fo:block white-space-collapse="false" element-id="17254">
                        <xsl:value-of select="tns:CONFIG_SPEC_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17255">
                        <fo:block white-space-collapse="false" element-id="17256">
                        <xsl:value-of select="tns:REAL_SHIP_DATE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17257">
                        <fo:block white-space-collapse="false" element-id="17258">
                        <xsl:value-of select="tns:LINE_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17259">
                        <fo:block white-space-collapse="false" element-id="17260">
                        <xsl:value-of select="tns:REL_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17261">
                        <fo:block white-space-collapse="false" element-id="17262">
                        <xsl:value-of select="tns:LINE_ITEM_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17263">
                        <fo:block white-space-collapse="false" element-id="17264">
                        <xsl:value-of select="tns:CATALOG_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17265">
                        <fo:block white-space-collapse="false" element-id="17266">
                        <xsl:value-of select="tns:CATALOG_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17267">
                        <fo:block white-space-collapse="false" element-id="17268">
                        <xsl:value-of select="tns:BUY_QTY_DUE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17269">
                        <fo:block white-space-collapse="false" element-id="17270">
                        <xsl:value-of select="tns:QTY_REMAINING"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17271">
                        <fo:block white-space-collapse="false" element-id="17272">
                        <xsl:value-of select="tns:QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17273">
                        <fo:block white-space-collapse="false" element-id="17274">
                        <xsl:value-of select="tns:FINAL_DELIVERY"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17275">
                        <fo:block white-space-collapse="false" element-id="17276">
                        <xsl:value-of select="tns:TOTAL_QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17277">
                        <fo:block white-space-collapse="false" element-id="17278">
                        <xsl:value-of select="tns:SALES_UNIT_MEAS"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17279">
                        <fo:block white-space-collapse="false" element-id="17280">
                        <xsl:value-of select="tns:REF_ID"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17281">
                        <fo:block white-space-collapse="false" element-id="17282">
                        <xsl:value-of select="tns:LOCATION_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17283">
                        <fo:block white-space-collapse="false" element-id="17284">
                        <xsl:value-of select="tns:CONDITION_CODE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17285">
                        <fo:block white-space-collapse="false" element-id="17286">
                        <xsl:value-of select="tns:CONDITION_CODE_DESCRIPTION"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17287">
                        <fo:block white-space-collapse="false" element-id="17288">
                        <xsl:value-of select="tns:CONTACT"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17289">
                        <fo:block white-space-collapse="false" element-id="17290">
                        <xsl:value-of select="tns:CUSTOMER_PART_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17291">
                        <fo:block white-space-collapse="false" element-id="17292">
                        <xsl:value-of select="tns:CUSTOMER_PART_DESC"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17293">
                        <fo:block white-space-collapse="false" element-id="17294">
                        <xsl:value-of select="tns:CATCH_QTY_DELIVERED"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17295">
                        <fo:block white-space-collapse="false" element-id="17296">
                        <xsl:value-of select="tns:CATCH_UOM"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17297">
                        <fo:block white-space-collapse="false" element-id="17298">
                        <xsl:value-of select="tns:MANUFACTURING_DEPARTMENT"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17299">
                        <fo:block white-space-collapse="false" element-id="17300">
                        <xsl:value-of select="tns:DELIVERY_SEQUENCE"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17301">
                        <fo:block white-space-collapse="false" element-id="17302">
                        <xsl:value-of select="tns:PO_REF"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17303">
                        <fo:block white-space-collapse="false" element-id="17304">
                        <xsl:value-of select="tns:GTIN_NO"/>
                        </fo:block>
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17305">
                        <fo:block white-space-collapse="false" element-id="17306">
                        <xsl:value-of select="tns:GTIN14"/>
                        </fo:block>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17307">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17308">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17309">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17310">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17311">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17312">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17313">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17314">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17315">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17316">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17317">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17318">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17319">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17320">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17321">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17322">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17323">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17324">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17325">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17326">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17327">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17328">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17329">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17330">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17331">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17332">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17333">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17334">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17335">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17336">
                     </fo:table-cell>
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17337">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17338">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17339">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:CATALOG_DOC_TEXTS/tns:CATALOG_DOC_TEXT) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17340">
                           </fo:table-column>
                           <xsl:if test="count(tns:CATALOG_DOC_TEXTS/tns:CATALOG_DOC_TEXT) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17341">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17342">
                                    <fo:block white-space-collapse="false" element-id="17343">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:CATALOG_DOC_TEXTS/tns:CATALOG_DOC_TEXT/tns:CATALOG_NO_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17344">
                           <xsl:for-each select="tns:CATALOG_DOC_TEXTS/tns:CATALOG_DOC_TEXT">
                              <fo:table-row element-id="17345">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17346">
                                    <fo:block white-space-collapse="false" element-id="17347">
                                    <xsl:value-of select="tns:CATALOG_NO_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17348">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17349">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17350">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17351">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:INPUT_VALUES/tns:INPUT_VALUE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="5.666667cm" element-id="17352">
                           </fo:table-column>
                           <fo:table-column column-width="5.666667cm" element-id="17353">
                           </fo:table-column>
                           <fo:table-column column-width="5.666667cm" element-id="17354">
                           </fo:table-column>
                           <xsl:if test="count(tns:INPUT_VALUES/tns:INPUT_VALUE) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17355">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17356">
                                    <fo:block white-space-collapse="false" element-id="17357">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:INPUT_VALUES/tns:INPUT_VALUE/tns:INPUT_QTY"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17358">
                                    <fo:block white-space-collapse="false" element-id="17359">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:INPUT_VALUES/tns:INPUT_VALUE/tns:INPUT_UNIT_MEAS"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17360">
                                    <fo:block white-space-collapse="false" element-id="17361">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:INPUT_VALUES/tns:INPUT_VALUE/tns:INPUT_VARIABLE_VALUES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17362">
                           <xsl:for-each select="tns:INPUT_VALUES/tns:INPUT_VALUE">
                              <fo:table-row element-id="17363">
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17364">
                                    <fo:block white-space-collapse="false" element-id="17365">
                                    <xsl:value-of select="tns:INPUT_QTY"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17366">
                                    <fo:block white-space-collapse="false" element-id="17367">
                                    <xsl:value-of select="tns:INPUT_UNIT_MEAS"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17368">
                                    <fo:block white-space-collapse="false" element-id="17369">
                                    <xsl:value-of select="tns:INPUT_VARIABLE_VALUES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17370">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17371">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17372">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17373">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17374">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17375">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:INVENTORY_PART_NOTES/tns:INVENTORY_PART_NOTE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17376">
                           </fo:table-column>
                           <xsl:if test="count(tns:INVENTORY_PART_NOTES/tns:INVENTORY_PART_NOTE) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17377">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17378">
                                    <fo:block white-space-collapse="false" element-id="17379">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:INVENTORY_PART_NOTES/tns:INVENTORY_PART_NOTE/tns:INVENTORY_PART_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17380">
                           <xsl:for-each select="tns:INVENTORY_PART_NOTES/tns:INVENTORY_PART_NOTE">
                              <fo:table-row element-id="17381">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17382">
                                    <fo:block white-space-collapse="false" element-id="17383">
                                    <xsl:value-of select="tns:INVENTORY_PART_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17384">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17385">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17386">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17387">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17388">
                           </fo:table-column>
                           <xsl:if test="count(tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17389">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17390">
                                    <fo:block white-space-collapse="false" element-id="17391">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT/tns:ORDER_LINE_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17392">
                           <xsl:for-each select="tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT">
                              <fo:table-row element-id="17393">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17394">
                                    <fo:block white-space-collapse="false" element-id="17395">
                                    <xsl:value-of select="tns:ORDER_LINE_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17396">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17397">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17398">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17399">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17400">
                           </fo:table-column>
                           <xsl:if test="count(tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17401">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17402">
                                    <fo:block white-space-collapse="false" element-id="17403">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART/tns:LOT_BATCH_NOS"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17404">
                           <xsl:for-each select="tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART">
                              <fo:table-row element-id="17405">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17406">
                                    <fo:block white-space-collapse="false" element-id="17407">
                                    <xsl:value-of select="tns:LOT_BATCH_NOS"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17408">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17409">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17410">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17411">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="2.125cm" element-id="17412">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17413">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17414">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17415">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17416">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17417">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17418">
                           </fo:table-column>
                           <fo:table-column column-width="2.125cm" element-id="17419">
                           </fo:table-column>
                           <xsl:if test="count(tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17420">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17421">
                                    <fo:block white-space-collapse="false" element-id="17422">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_ID"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17423">
                                    <fo:block white-space-collapse="false" element-id="17424">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_VALUE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17425">
                                    <fo:block white-space-collapse="false" element-id="17426">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_UOM"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17427">
                                    <fo:block white-space-collapse="false" element-id="17428">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_QTY"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17429">
                                    <fo:block white-space-collapse="false" element-id="17430">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_FIRST"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17431">
                                    <fo:block white-space-collapse="false" element-id="17432">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_LAST"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17433">
                                    <fo:block white-space-collapse="false" element-id="17434">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_PRICE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17435">
                                    <fo:block white-space-collapse="false" element-id="17436">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_PFLAG"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17437">
                           <xsl:for-each select="tns:ORDER_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC">
                              <fo:table-row element-id="17438">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17439">
                                    <fo:block white-space-collapse="false" element-id="17440">
                                    <xsl:value-of select="tns:CHARACTERISTIC_ID"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17441">
                                    <fo:block white-space-collapse="false" element-id="17442">
                                    <xsl:value-of select="tns:CHARACTERISTIC_VALUE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17443">
                                    <fo:block white-space-collapse="false" element-id="17444">
                                    <xsl:value-of select="tns:CHARACTERISTIC_UOM"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17445">
                                    <fo:block white-space-collapse="false" element-id="17446">
                                    <xsl:value-of select="tns:CHARACTERISTIC_QTY"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17447">
                                    <fo:block white-space-collapse="false" element-id="17448">
                                    <xsl:value-of select="tns:CHARACTERISTIC_FIRST"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17449">
                                    <fo:block white-space-collapse="false" element-id="17450">
                                    <xsl:value-of select="tns:CHARACTERISTIC_LAST"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17451">
                                    <fo:block white-space-collapse="false" element-id="17452">
                                    <xsl:value-of select="tns:CHARACTERISTIC_PRICE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17453">
                                    <fo:block white-space-collapse="false" element-id="17454">
                                    <xsl:value-of select="tns:CHARACTERISTIC_PFLAG"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17455">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17456">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17457">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17458">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17459">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17460">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17461">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17462">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17463">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17464">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17465">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:PACKAGE_LINES/tns:PACKAGE_LINE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="1.545455cm" element-id="17466">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17467">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17468">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17469">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17470">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17471">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17472">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17473">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17474">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17475">
                           </fo:table-column>
                           <fo:table-column column-width="1.545455cm" element-id="17476">
                           </fo:table-column>
                           <xsl:if test="count(tns:PACKAGE_LINES/tns:PACKAGE_LINE) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17477">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17478">
                                    <fo:block white-space-collapse="false" element-id="17479">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CATALOG_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17480">
                                    <fo:block white-space-collapse="false" element-id="17481">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CATALOG_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17482">
                                    <fo:block white-space-collapse="false" element-id="17483">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:QTY_DELIVERED"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17484">
                                    <fo:block white-space-collapse="false" element-id="17485">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:SALES_UNIT_MEAS"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17486">
                                    <fo:block white-space-collapse="false" element-id="17487">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CONFIG_SPEC_ID"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17488">
                                    <fo:block white-space-collapse="false" element-id="17489">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CONFIG_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17490">
                                    <fo:block white-space-collapse="false" element-id="17491">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:REAL_SHIP_DATE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17492">
                                    <fo:block white-space-collapse="false" element-id="17493">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CUSTOMER_PART_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17494">
                                    <fo:block white-space-collapse="false" element-id="17495">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CUSTOMER_PART_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17496">
                                    <fo:block white-space-collapse="false" element-id="17497">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CATCH_QTY_DELIVERED"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17498">
                                    <fo:block white-space-collapse="false" element-id="17499">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:CATCH_UOM"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17500">
                           <xsl:for-each select="tns:PACKAGE_LINES/tns:PACKAGE_LINE">
                              <fo:table-row element-id="17501">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17502">
                                    <fo:block white-space-collapse="false" element-id="17503">
                                    <xsl:value-of select="tns:CATALOG_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17504">
                                    <fo:block white-space-collapse="false" element-id="17505">
                                    <xsl:value-of select="tns:CATALOG_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17506">
                                    <fo:block white-space-collapse="false" element-id="17507">
                                    <xsl:value-of select="tns:QTY_DELIVERED"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17508">
                                    <fo:block white-space-collapse="false" element-id="17509">
                                    <xsl:value-of select="tns:SALES_UNIT_MEAS"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17510">
                                    <fo:block white-space-collapse="false" element-id="17511">
                                    <xsl:value-of select="tns:CONFIG_SPEC_ID"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17512">
                                    <fo:block white-space-collapse="false" element-id="17513">
                                    <xsl:value-of select="tns:CONFIG_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17514">
                                    <fo:block white-space-collapse="false" element-id="17515">
                                    <xsl:value-of select="tns:REAL_SHIP_DATE"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17516">
                                    <fo:block white-space-collapse="false" element-id="17517">
                                    <xsl:value-of select="tns:CUSTOMER_PART_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17518">
                                    <fo:block white-space-collapse="false" element-id="17519">
                                    <xsl:value-of select="tns:CUSTOMER_PART_DESC"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17520">
                                    <fo:block white-space-collapse="false" element-id="17521">
                                    <xsl:value-of select="tns:CATCH_QTY_DELIVERED"/>
                                    </fo:block>
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17522">
                                    <fo:block white-space-collapse="false" element-id="17523">
                                    <xsl:value-of select="tns:CATCH_UOM"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17524">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17525">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17526">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17527">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17528">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17529">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17530">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17531">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17532">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17533">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17534">
                                 </fo:table-cell>
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17535">
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17536">
                                 <fo:table-cell border-style="none" number-columns-spanned="11" white-space-collapse="false" element-id="17537">
                                    
                                    <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                                       <fo:table-column column-width="17cm" element-id="17538">
                                       </fo:table-column>
                                       <xsl:if test="count(tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT) &gt; 0">
<fo:table-header border-style="none">
                                          <fo:table-row font-weight="bold" element-id="17539">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17540">
                                                <fo:block white-space-collapse="false" element-id="17541">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT/tns:ORDER_LINE_NOTES"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </fo:table-header>
</xsl:if>
                                       <fo:table-body border-style="none" element-id="17542">
                                       <xsl:for-each select="tns:LINE_DOC_TEXTS/tns:LINE_DOC_TEXT">
                                          <fo:table-row element-id="17543">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17544">
                                                <fo:block white-space-collapse="false" element-id="17545">
                                                <xsl:value-of select="tns:ORDER_LINE_NOTES"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                          <fo:table-row element-id="17546">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17547">
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </xsl:for-each>
                                       </fo:table-body>
                                    </fo:table>
</xsl:if>
                                    </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17548">
                                 <fo:table-cell border-style="none" number-columns-spanned="11" white-space-collapse="false" element-id="17549">
                                    
                                    <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                                       <fo:table-column column-width="17cm" element-id="17550">
                                       </fo:table-column>
                                       <xsl:if test="count(tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART) &gt; 0">
<fo:table-header border-style="none">
                                          <fo:table-row font-weight="bold" element-id="17551">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17552">
                                                <fo:block white-space-collapse="false" element-id="17553">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART/tns:LOT_BATCH_NOS"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </fo:table-header>
</xsl:if>
                                       <fo:table-body border-style="none" element-id="17554">
                                       <xsl:for-each select="tns:LOT_BATCH_PARTS/tns:LOT_BATCH_PART">
                                          <fo:table-row element-id="17555">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17556">
                                                <fo:block white-space-collapse="false" element-id="17557">
                                                <xsl:value-of select="tns:LOT_BATCH_NOS"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                          <fo:table-row element-id="17558">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17559">
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </xsl:for-each>
                                       </fo:table-body>
                                    </fo:table>
</xsl:if>
                                    </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17560">
                                 <fo:table-cell border-style="none" number-columns-spanned="11" white-space-collapse="false" element-id="17561">
                                    
                                    <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                                       <fo:table-column column-width="2.125cm" element-id="17562">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17563">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17564">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17565">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17566">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17567">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17568">
                                       </fo:table-column>
                                       <fo:table-column column-width="2.125cm" element-id="17569">
                                       </fo:table-column>
                                       <xsl:if test="count(tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC) &gt; 0">
<fo:table-header border-style="none">
                                          <fo:table-row font-weight="bold" element-id="17570">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17571">
                                                <fo:block white-space-collapse="false" element-id="17572">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_ID"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17573">
                                                <fo:block white-space-collapse="false" element-id="17574">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_VALUE"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17575">
                                                <fo:block white-space-collapse="false" element-id="17576">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_UOM"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17577">
                                                <fo:block white-space-collapse="false" element-id="17578">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_QTY"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17579">
                                                <fo:block white-space-collapse="false" element-id="17580">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_FIRST"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17581">
                                                <fo:block white-space-collapse="false" element-id="17582">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_LAST"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17583">
                                                <fo:block white-space-collapse="false" element-id="17584">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_PRICE"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17585">
                                                <fo:block white-space-collapse="false" element-id="17586">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC/tns:CHARACTERISTIC_PFLAG"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </fo:table-header>
</xsl:if>
                                       <fo:table-body border-style="none" element-id="17587">
                                       <xsl:for-each select="tns:PACKAGE_LINE_CHARACTERISTICS/tns:LINE_CHARACTERISTIC">
                                          <fo:table-row element-id="17588">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17589">
                                                <fo:block white-space-collapse="false" element-id="17590">
                                                <xsl:value-of select="tns:CHARACTERISTIC_ID"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17591">
                                                <fo:block white-space-collapse="false" element-id="17592">
                                                <xsl:value-of select="tns:CHARACTERISTIC_VALUE"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17593">
                                                <fo:block white-space-collapse="false" element-id="17594">
                                                <xsl:value-of select="tns:CHARACTERISTIC_UOM"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17595">
                                                <fo:block white-space-collapse="false" element-id="17596">
                                                <xsl:value-of select="tns:CHARACTERISTIC_QTY"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17597">
                                                <fo:block white-space-collapse="false" element-id="17598">
                                                <xsl:value-of select="tns:CHARACTERISTIC_FIRST"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17599">
                                                <fo:block white-space-collapse="false" element-id="17600">
                                                <xsl:value-of select="tns:CHARACTERISTIC_LAST"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17601">
                                                <fo:block white-space-collapse="false" element-id="17602">
                                                <xsl:value-of select="tns:CHARACTERISTIC_PRICE"/>
                                                </fo:block>
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" overflow="error-if-overflow" wrap-option="no-wrap" element-id="17603">
                                                <fo:block white-space-collapse="false" element-id="17604">
                                                <xsl:value-of select="tns:CHARACTERISTIC_PFLAG"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                          <fo:table-row element-id="17605">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17606">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17607">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17608">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17609">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17610">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17611">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17612">
                                             </fo:table-cell>
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17613">
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </xsl:for-each>
                                       </fo:table-body>
                                    </fo:table>
</xsl:if>
                                    </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17614">
                                 <fo:table-cell border-style="none" number-columns-spanned="11" white-space-collapse="false" element-id="17615">
                                    
                                    <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:SERIAL_PARTS/tns:SERIAL_PART) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                                       <fo:table-column column-width="17cm" element-id="17616">
                                       </fo:table-column>
                                       <xsl:if test="count(tns:SERIAL_PARTS/tns:SERIAL_PART) &gt; 0">
<fo:table-header border-style="none">
                                          <fo:table-row font-weight="bold" element-id="17617">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17618">
                                                <fo:block white-space-collapse="false" element-id="17619">
                                                <xsl:value-of select="../../../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PACKAGE_LINES/tns:PACKAGE_LINE/tns:SERIAL_PARTS/tns:SERIAL_PART/tns:SERIAL_NO"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </fo:table-header>
</xsl:if>
                                       <fo:table-body border-style="none" element-id="17620">
                                       <xsl:for-each select="tns:SERIAL_PARTS/tns:SERIAL_PART">
                                          <fo:table-row element-id="17621">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17622">
                                                <fo:block white-space-collapse="false" element-id="17623">
                                                <xsl:value-of select="tns:SERIAL_NO"/>
                                                </fo:block>
                                             </fo:table-cell>
                                          </fo:table-row>
                                          <fo:table-row element-id="17624">
                                             <fo:table-cell border-style="none" white-space-collapse="false" element-id="17625">
                                             </fo:table-cell>
                                          </fo:table-row>
                                       </xsl:for-each>
                                       </fo:table-body>
                                    </fo:table>
</xsl:if>
                                    </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17626">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17627">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:PART_CATALOG_NOTES/tns:PART_CATALOG_NOTE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17628">
                           </fo:table-column>
                           <xsl:if test="count(tns:PART_CATALOG_NOTES/tns:PART_CATALOG_NOTE) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17629">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17630">
                                    <fo:block white-space-collapse="false" element-id="17631">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:PART_CATALOG_NOTES/tns:PART_CATALOG_NOTE/tns:PART_CATALOG_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17632">
                           <xsl:for-each select="tns:PART_CATALOG_NOTES/tns:PART_CATALOG_NOTE">
                              <fo:table-row element-id="17633">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17634">
                                    <fo:block white-space-collapse="false" element-id="17635">
                                    <xsl:value-of select="tns:PART_CATALOG_NOTES"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17636">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17637">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17638">
                     <fo:table-cell border-style="none" number-columns-spanned="30" white-space-collapse="false" element-id="17639">
                        
                        <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(tns:SERIAL_PARTS/tns:SERIAL_PART) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
                           <fo:table-column column-width="17cm" element-id="17640">
                           </fo:table-column>
                           <xsl:if test="count(tns:SERIAL_PARTS/tns:SERIAL_PART) &gt; 0">
<fo:table-header border-style="none">
                              <fo:table-row font-weight="bold" element-id="17641">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17642">
                                    <fo:block white-space-collapse="false" element-id="17643">
                                    <xsl:value-of select="../../../tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:ORDER_LINES/tns:ORDER_LINE/tns:SERIAL_PARTS/tns:SERIAL_PART/tns:SERIAL_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                           </fo:table-header>
</xsl:if>
                           <fo:table-body border-style="none" element-id="17644">
                           <xsl:for-each select="tns:SERIAL_PARTS/tns:SERIAL_PART">
                              <fo:table-row element-id="17645">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17646">
                                    <fo:block white-space-collapse="false" element-id="17647">
                                    <xsl:value-of select="tns:SERIAL_NO"/>
                                    </fo:block>
                                 </fo:table-cell>
                              </fo:table-row>
                              <fo:table-row element-id="17648">
                                 <fo:table-cell border-style="none" white-space-collapse="false" element-id="17649">
                                 </fo:table-cell>
                              </fo:table-row>
                           </xsl:for-each>
                           </fo:table-body>
                        </fo:table>
</xsl:if>
                        </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
                     </fo:table-cell>
                  </fo:table-row>
               </xsl:for-each>
               </fo:table-body>
            </fo:table>
</xsl:if>
            </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
            
            <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<xsl:if test="count(/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:CUSTOMER_NOTES/tns:CUSTOMER_NOTE) &gt; 0">
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm">
               <fo:table-column column-width="17cm" element-id="17650">
               </fo:table-column>
               <xsl:if test="count(/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:CUSTOMER_NOTES/tns:CUSTOMER_NOTE) &gt; 0">
<fo:table-header border-style="none">
                  <fo:table-row font-weight="bold" element-id="17651">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17652">
                        <fo:block white-space-collapse="false" element-id="17653">
                        <xsl:value-of select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_TRANSLATIONS/tns:ATTRIBUTE_DISPLAY_TEXTS/tns:CUSTOMER_NOTES/tns:CUSTOMER_NOTE/tns:CUSTOMER_NOTES"/>
                        </fo:block>
                     </fo:table-cell>
                  </fo:table-row>
               </fo:table-header>
</xsl:if>
               <fo:table-body border-style="none" element-id="17654">
               <xsl:for-each select="/tns:CUSTOMER_ORDER_DELIV_NOTE_REP_REQUEST/tns:CUSTOMER_ORDER_DELIV_NOTE_REP/tns:CUSTOMER_NOTES/tns:CUSTOMER_NOTE">
                  <fo:table-row element-id="17655">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17656">
                        <fo:block white-space-collapse="false" element-id="17657">
                        <xsl:value-of select="tns:CUSTOMER_NOTES"/>
                        </fo:block>
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17658">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17659">
                     </fo:table-cell>
                  </fo:table-row>
               </xsl:for-each>
               </fo:table-body>
            </fo:table>
</xsl:if>
            </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
            
            <fo:table>
<fo:table-column column-width="0.0cm" />
<fo:table-column />
<fo:table-body>
<fo:table-row height="0cm">
<fo:table-cell />
<fo:table-cell />
</fo:table-row>
<fo:table-row>
<fo:table-cell />
<fo:table-cell>
<fo:table border-style="none" left="0cm" table-layout="fixed" top="0cm" element-id="17660">
               <fo:table-column column-width="17cm" element-id="17661">
               </fo:table-column>
               <fo:table-body border-style="none" element-id="17662">
                  <fo:table-row element-id="17663">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17664">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17665">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17666">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17667">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17668">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17669">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17670">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17671">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17672">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17673">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17674">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17675">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17676">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17677">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17678">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17679">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17680">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17681">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17682">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17683">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17684">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17685">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17686">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17687">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17688">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17689">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17690">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17691">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17692">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17693">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17694">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17695">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17696">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17697">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17698">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17699">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17700">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17701">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17702">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17703">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17704">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17705">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17706">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17707">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17708">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17709">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17710">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17711">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17712">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17713">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17714">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17715">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17716">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17717">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17718">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17719">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17720">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17721">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17722">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17723">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17724">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17725">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17726">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17727">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17728">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17729">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17730">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17731">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17732">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17733">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17734">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17735">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17736">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17737">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17738">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17739">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17740">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17741">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17742">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17743">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17744">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17745">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17746">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17747">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17748">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17749">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17750">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17751">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17752">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17753">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17754">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17755">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17756">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17757">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17758">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17759">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17760">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17761">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17762">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17763">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17764">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17765">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17766">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17767">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17768">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17769">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17770">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17771">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17772">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17773">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17774">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17775">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17776">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17777">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17778">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17779">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17780">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17781">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17782">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17783">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17784">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17785">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17786">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17787">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17788">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17789">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17790">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17791">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17792">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17793">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17794">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17795">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17796">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17797">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17798">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17799">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17800">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17801">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17802">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17803">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17804">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17804">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17805">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17806">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17807">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17808">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17809">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17810">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17811">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17812">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17813">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17814">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17815">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17816">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17817">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17818">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17819">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17820">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17821">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17822">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17823">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17824">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17825">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17826">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17827">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17828">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17829">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17830">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17831">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17832">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17833">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17833">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17834">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17835">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17836">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17837">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17838">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17839">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17840">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17841">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17842">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17843">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17844">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17845">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17846">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17847">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17848">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17849">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17850">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17851">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17852">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17853">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17854">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17855">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17856">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17857">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17858">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17859">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17860">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17861">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17862">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17863">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17864">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17865">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17866">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17867">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17868">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17869">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17870">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17871">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17872">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17873">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17874">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17875">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17876">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17877">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17878">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17879">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17880">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17881">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17882">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17883">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17884">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17885">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17886">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17887">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17888">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17889">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17890">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17891">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17892">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17893">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17894">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17895">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17896">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17897">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17898">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17899">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17900">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17901">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17902">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17903">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17904">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17905">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17906">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17907">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17908">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17909">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17910">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17911">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17912">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17913">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17914">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17915">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17916">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17917">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17918">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17919">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17920">
                     </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row element-id="17921">
                     <fo:table-cell border-style="none" white-space-collapse="false" element-id="17922">
                     </fo:table-cell>
                  </fo:table-row>
               </fo:table-body>
            </fo:table>
            </fo:table-cell>
</fo:table-row>
</fo:table-body>
</fo:table>
         <fo:block id="lastPage"/>
         </fo:flow>
      </fo:page-sequence>
   </fo:root>
</xsl:template>
</xsl:stylesheet>

