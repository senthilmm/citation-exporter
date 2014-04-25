<?xml version="1.0"?> 
<xsl:stylesheet
        version="2.0" 
        xmlns:xsl     ="http://www.w3.org/1999/XSL/Transform"
        xmlns:xlink   ="http://www.w3.org/1999/xlink" 
        xmlns:XSL     ="http://www.pubmedcentral.gov/XSL"
        xmlns:redirect="http://www.pubmedcentral.gov/redirect"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:pmc="http://www.pubmedcentral.gov/functions"
        exclude-result-prefixes="pmc XSL xlink redirect xs">


   <!-- ==================================================================== -->
   <!-- common-modules/dates.xsl:

        Templates to help out with date processing.

           (Checking)
		   (extract dates from raw data):
   		      get-date-from-string: convert various formats to NLM.
              get-date-from-atts
		      get-year-from-string
		   (convert month names/numbers)
		   (date arithmetic)

        WARNINGS
           There are dependencies on English.
           There may be dependencies on US date formats.
     -->
   <!-- ==================================================================== -->


<!-- Some long strings we use a lot. -->
<xsl:param name="E-thinsp" select="'_ENTITYSTART_#8201_ENTITYEND_'"/>
<xsl:param name="E-endash" select="'_ENTITYSTART_#8211_ENTITYEND_'"/>


<!-- ********************* DATE CHECKING TEMPLATES **************************-->

	<xsl:template name="checkday">
		<xsl:param name="checkdate_day"/>
		<xsl:if test="$checkdate_day!=''">
			<xsl:choose>
				<xsl:when test="number(1 &lt;= $checkdate_day) 
				            and number(31 >= $checkdate_day)">
					<day>
						<xsl:value-of select="number($checkdate_day)"/>
					</day>
					</xsl:when>
				<xsl:otherwise>
					<xsl:comment>Day provided is not valid</xsl:comment><xsl:text>&#xA;</xsl:text>
					<xsl:message>ERROR   :[<xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>]: Day provided is not valid---<xsl:value-of select="$checkdate_day"/></xsl:message>
					<error>Day provided is not valid---<xsl:value-of select="$checkdate_day"/></error>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:template>
		
		
	<xsl:template name="checkmonth">
		<xsl:param name="checkdate_month"/>
		<xsl:if test="$checkdate_month!=''">
			<xsl:choose>
				<xsl:when test="string(number($checkdate_month))='NaN'">
					<xsl:call-template name="find-month-season">
						<xsl:with-param name="month" select="$checkdate_month"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="number(1 &lt;= $checkdate_month)
				            and number(12 >= $checkdate_month)">
					<month>
						<xsl:value-of select="number($checkdate_month)"/>
					</month>
					</xsl:when>
				<xsl:otherwise>
					<xsl:comment>Month provided is not valid</xsl:comment><xsl:text>&#xA;</xsl:text>
					<xsl:message>ERROR   :[<xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>]: Month provided is not valid---<xsl:value-of select="$checkdate_month"/></xsl:message>
					<error>Month provided is not valid---<xsl:value-of select="$checkdate_month"/></error>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	
	<xsl:template name="checkday-string">
		<xsl:param name="checkdate_day"/>
		<xsl:choose>
			<xsl:when test="number(1 &lt;= $checkdate_day) and number(31 >= $checkdate_day)">
					<xsl:value-of select="$checkdate_day"/>
				</xsl:when>
			</xsl:choose>
		</xsl:template>
		
		
	<xsl:template name="checkmonth-string">
		<xsl:param name="checkdate_month"/>
		<xsl:choose>
			<xsl:when test="number(1 &lt;= $checkdate_month) and number(12 >= $checkdate_month)">
					<xsl:value-of select="$checkdate_month"/>
				</xsl:when>
			<xsl:when test="string(number($checkdate_month))='NaN'">
					<xsl:value-of select="$checkdate_month"/>
				</xsl:when>
			</xsl:choose>
		</xsl:template>
	
	
	<xsl:template name="checkyear-string">
		<xsl:param name="checkdate_year"/>
		<xsl:choose>
			<xsl:when test="number(1800 &lt;= $checkdate_year) and number(2100 >= $checkdate_year)">
				<xsl:value-of select="$checkdate_year"/>
				</xsl:when>
			</xsl:choose>
		</xsl:template>
	
	
	<xsl:template name="checkyear">
		<xsl:param name="checkdate_year"/>
		<xsl:variable name="year" select="number($checkdate_year)"/>
		<xsl:choose>
	<!--		<xsl:when test="number(1800 &lt;= $checkdate_year) and number(2100 >= $checkdate_year)"> -->
			<xsl:when test="1800 &lt; $year and $year &lt; 2100">
				<year>
					<xsl:value-of select="$checkdate_year"/>
				</year>
				</xsl:when>
			<xsl:otherwise>
				<xsl:comment>Year provided is not valid</xsl:comment><xsl:text>&#xA;</xsl:text>
				<xsl:message>ERROR   :[<xsl:value-of select="format-dateTime(current-dateTime(), '[Y0001]-[M01]-[D01] [H01]:[m01]:[s01]')"/>]: Year provided is not valid---<xsl:value-of select="$checkdate_year"/></xsl:message>
				<error>Year provided is not valid---<xsl:value-of select="$checkdate_year"/></error>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:template>



   <!-- ==================================================================== -->
   <!-- ******************** END DATE CHECKING TEMPLATES ********************-->
   <!-- ==================================================================== -->


   <!-- ==================================================================== -->
   <!-- TEMPLATE: get-date-from-string
		Parses written-out date in various forms to NLM  format.
        For use in <date> or <pub-date>.
						 
  	    Parameters:  date  - a string that has been run through normalize-space()
	                         and had all punctuation removed
		  			 order - (optional) defines the order of the parts sent:
									dmy = day month year
									mdy = month day year
									ymd
					 nsd   - if the form is not recognized, we put the
                        original date in <string-date> unless this is non-null.
						This is used when creating conf-date. Cf Rt bug 11224.

		The template will work for these types of dates
		   (DD is day, MM is month, YYYY is year):
		
			Month DD, YYYY
			DD Month YYYY
			YYYY Month DD
			Season YYYY
			YYYY Season
			Month YYYY
			YYYY Month
			Month-Month YYYY
         Month/Month YYYY
			Season-Season YYYY
			DD-MM-YYYY (with order parameter)						
			DD/MM/YYYY (with order parameter)						
			DD.MM.YYYY (with order parameter)						
			MM-DD-YYYY (with order parameter)						
			MM/DD/YYYY (with order parameter)						
			MM.DD.YYYY (with order parameter)							  

     To do:
	    Seems like we should normalize heavily before doing anything else.
		normalize-space; ditch all kinds of weird space/dash chars; case.
      --> 
    <!-- ==================================================================== -->
	<xsl:template name="get-date-from-string">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
		<xsl:param name="nsd"/>
      <xsl:param name="force-parsedate"/>
		
		<!-- xsl:message fails if there's an en-dash -->
		<!--<xsl:message>get-date-from-string, nsd is '<xsl:value-of select="$nsd"/>', date is '<xsl:value-of select="translate(.,'&#8211;','-')"/>'. </xsl:message> -->

		<!-- Remove all thin-space entities from date -->
		<xsl:variable name="no-thin-space"> 
			<xsl:call-template name="replace-thin-space-entity">
			   <xsl:with-param name="str" select="$date"/>
			</xsl:call-template>
		</xsl:variable>

        <!-- Remove commas and spaces from date -->
	    <xsl:variable name="cleandate"> 
		   <xsl:call-template name="nocomma-nodot">
		      <xsl:with-param name="str" select="normalize-space($no-thin-space)"/>
	           </xsl:call-template>
		</xsl:variable>

		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="$cleandate"/>
				</xsl:call-template>
		</xsl:variable>

<!-- [[<xsl:value-of select="$cleandate"/>]]  -->

		<xsl:choose>
			<xsl:when test="$space-count=0">
				<xsl:call-template name="date-nospace">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
                        <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

			<xsl:when test="contains($date,'-')
				               or contains($date,'ENTITYSTART_hyphen')
				               or contains($date,'ENTITYSTART_ndash')
                           or contains($date, '&#x2013;')
									or contains($date, '&#x00026;#x02013;')"> <!-- need to add charent testing -->
					<xsl:call-template name="date-range">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
                  <xsl:with-param name="nsd" select="$nsd"/>
						<xsl:with-param name="force-parsedate" select="$force-parsedate"/>
						</xsl:call-template>
			</xsl:when>  

			<xsl:when test="$space-count=1">
					<xsl:call-template name="date-1space">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
                        <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

			<xsl:when test="$space-count=2">
					<xsl:call-template name="date-2space">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
                        <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

			<!-- what if *3* spaces? -->

			<xsl:when test="$space-count > 3 and $order='citation'">
				<xsl:value-of select="$date"/>
			</xsl:when>

			<xsl:when test="$space-count > 3">
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$date"/>
			      <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
			</xsl:when>

			<xsl:when test="$order='citation'">
					<xsl:value-of select="$date"/>
			</xsl:when>

			<!-- Form not recognized, so do something simple -->
            <xsl:otherwise>
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$date"/>
			      <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
            </xsl:otherwise>
		</xsl:choose>				
	</xsl:template> <!-- get-date-from-string -->



   <!-- ==================================================================== -->
   <!-- TEMPLATE: replace-thin-space-entity
         Replaces _ENTITYSTART_#8201_ENTITYEND_ (param defined at top)
		 with a simple space: 
	     entity for thin space is sometimes used to separate parts of date
         (probably a lot of funky space characters are:
x0020 d0032 space
x00a0 d0160 nbsp
x1361 d4961 ethiopic wordspace
x1680 d5760 egham space mark
x2002 d8194 en space
x2003 d8195 em space
x2004 d8196 thre per em space
x2005 d8197 four per em space
x2006 d8198 six per em space
x2007 d9200 figure space
x2008 d8200 punctuation space
x2009 d8201 thin space
x200a d8202 hair space
x200b d8203 zero width space
x202F d8239 narrow no-break space
x205F d8287 medium mathematical space
x2420 d9248 symbol for space (??)
x3000 d12288 ideographic space
x303f d12351 ideographic half fill space
xfeff d65279 zero width no-break space
     -->
   <!-- ==================================================================== -->
   <xsl:template name="replace-thin-space-entity">
	   <xsl:param name="str"/>
	   
	   <xsl:choose>
	      <!-- Base Case: no more entities to remove -->
              <xsl:when test="not(contains($str, $E-thinsp))">
	         <xsl:value-of select="$str"/>
	      </xsl:when>
	       
	      <!-- Case 1: Entity is in the string -->
	      <xsl:otherwise>
	         <xsl:value-of select="substring-before($str, $E-thinsp)"/>
		 <xsl:text> </xsl:text>
		 <xsl:call-template name="replace-thin-space-entity">
		    <xsl:with-param name="str" select="substring-after($str, $E-thinsp)"/>
		 </xsl:call-template>
	      </xsl:otherwise>
	   </xsl:choose>
	</xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE: count-space
           (should be dropped in favor of a generic count-chars in strings)
     -->
   <!-- ==================================================================== -->
	<xsl:template name="count-space">
		<xsl:param name="date"/>
			<xsl:choose>
				<xsl:when test="contains($date,' ')">
					<xsl:choose>
						<xsl:when test="contains(substring-after($date,' '),' ')">
							<xsl:choose>
								<xsl:when test="contains(substring-after(substring-after($date,' '),' '),' ')">
									<xsl:choose>
										<xsl:when test="contains(substring-after(substring-after(substring-after($date,' '),' '),' '),' ')">4</xsl:when>
										<xsl:otherwise>3</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
								<xsl:otherwise>2</xsl:otherwise>
									</xsl:choose>
							</xsl:when>
						<xsl:when test="contains($date,' ')">1</xsl:when>
						</xsl:choose>
					</xsl:when>
				<xsl:otherwise>0</xsl:otherwise>
				</xsl:choose>
		</xsl:template>

    
   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-range
        NOTES:    Called when we've got a hyphen *and* spaces.
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-range">
		<xsl:param name="date"/>
		<xsl:param name="nsd"/>
		<xsl:param name="order"/>
		<xsl:param name="force-parsedate"/>

		<xsl:variable name="stringdate-mod">
			<xsl:call-template name="fix-range">
				<xsl:with-param name="date" select="$date"/>
				</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="$stringdate-mod"/>
				</xsl:call-template>
		</xsl:variable>


		<xsl:choose>
			<xsl:when test="$force-parsedate='yes'">
				<xsl:call-template name="get-date-from-string">
					<xsl:with-param name="date" select="substring-before($stringdate-mod,'_ENTITYSTART')"/>
					</xsl:call-template>
				</xsl:when>
			<xsl:when test="$space-count=1">
				<xsl:call-template name="date-1space">
				   <xsl:with-param name="date" select="$stringdate-mod"/>
			       <xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="$space-count=2">
				<xsl:call-template name="date-2space">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="'range'"/>
			        <xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="$space-count > 2">
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$stringdate-mod"/>
			      <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
			   <xsl:message>date-range: Can't handle date format in '<xsl:value-of select="translate(.,'&#8211;','-')"/>''.</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template> <!-- date-range -->


   <!-- ==================================================================== -->
   <!-- TEMPLATE: fix-range
        NOTES:    Change ' - ' or '-' or endash char, all to endash.
		          This should switch to just a translate and then a space-drop.
				  And should handle other space-types.
				  Update 5/13/11: PMC-11550, normalize en-dashes to hyphens
     -->
   <!-- ==================================================================== -->
	<xsl:template name="fix-range">
		<xsl:param name="date"/>
		<xsl:choose>
			<xsl:when test="contains($date, ' - ')">
				<xsl:value-of select="concat(
				   substring-before($date,' -'),'-',substring-after($date,'- '))"/>
			</xsl:when>
			<xsl:when test="contains($date, '-')">
				<xsl:value-of select="$date"/>
			</xsl:when>
			<xsl:when test="contains($date, '&#8211;')">
				<xsl:value-of select="normalize-space(concat(
				   substring-before($date,'&#8211;'),'-',substring-after($date,'&#8211;')))"/>
			</xsl:when>
			<xsl:when test="contains($date, '&#x00026;#x02013;')">
				<xsl:value-of select="normalize-space(concat(
				   substring-before($date,'&#x00026;#x02013;'),'-',substring-after($date,'&#x00026;#x02013;')))"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-nospace
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-nospace">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
        <xsl:param name="nsd"/>

		<xsl:variable name="stringdate-mod" select="normalize-space(translate($date,'-./','   '))"/>
		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="normalize-space($stringdate-mod)"/>
			</xsl:call-template>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$space-count=0">
				<!-- just a year -->
				<year><xsl:value-of select="$stringdate-mod"/></year>
			</xsl:when>
			<xsl:when test="$space-count=1">
				<xsl:call-template name="date-1space">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="$order"/>
					</xsl:call-template>
			</xsl:when>
			<xsl:when test="$space-count=2">
				<xsl:call-template name="date-2space">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="$order"/>
					</xsl:call-template>
			</xsl:when>
			<xsl:when test="$order='citation'">
				<xsl:value-of select="$date"/>
			</xsl:when>
			<xsl:when test="$space-count > 2">
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$stringdate-mod"/>
			      <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
			</xsl:when>
            <xsl:otherwise>
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$date"/>
			      <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
               </xsl:otherwise>
		</xsl:choose>
	</xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-1space
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-1space">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
        <xsl:param name="nsd"/>

		<xsl:variable name="x1" select="substring-before($date,' ')"/>
		<xsl:variable name="x2" select="substring-after($date,' ')"/>
        <xsl:variable name="yeardiff" select="string-length($x1) - string-length($x2)"/>
		<xsl:choose>
          <!--  <xsl:when test="$x1 > 1000">
                <xsl:choose>
                    <xsl:when test="$yeardiff > 0">
                        <year><xsl:value-of select="$x1"/>&#x2013;<xsl:value-of select="substring($x1,1,$yeardiff)"/><xsl:value-of select="$x2"/></year>
                        </xsl:when>
                    <xsl:otherwise>
                        <year><xsl:value-of select="$x1"/>&#x2013;<xsl:value-of select="$x2"/></year>
                        </xsl:otherwise>
                    </xsl:choose>
            </xsl:when>  -->

       		<xsl:when test="number($x1) > 1000">
		    	<xsl:call-template name="find-month-season">
			       	<xsl:with-param name="month" select="$x2"/>
       			</xsl:call-template>
       			<year><xsl:value-of select="$x1"/></year>
       		</xsl:when>

       		<xsl:when test="number($x2) > 1000">
		    	<xsl:call-template name="find-month-season">
			       	<xsl:with-param name="month" select="$x1"/>
       			</xsl:call-template>
       			<year><xsl:value-of select="$x2"/></year>
       		</xsl:when>

			<!-- when one is a number, the other is probably a month,
			     since neither is a year -->	
			<xsl:when test="string(number($x1)) != 'NaN' 
			             or string(number($x2)) != 'NaN'">
				<xsl:value-of select="$date"/>
			</xsl:when>

       		<xsl:when test="$order='citation'">
       			<xsl:value-of select="$date"/>
       		</xsl:when>

       		<xsl:otherwise>
			   <xsl:call-template name="maybe-string-date">
			      <xsl:with-param name="date" select="$date"/>
				  <xsl:with-param name="nsd" select="$nsd"/>
			   </xsl:call-template>
            </xsl:otherwise>
   		</xsl:choose>
	</xsl:template> <!-- date-1space -->


   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-2space
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-2space">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
        <xsl:param name="nsd"/>

		<xsl:variable name="x1" select="substring-before($date,' ')"/>
		<xsl:variable name="x2" select="substring-before(substring-after($date,' '),' ')"/>
		<xsl:variable name="x3" select="substring-after(substring-after($date,' '),' ')"/>
		<xsl:variable name="quartercheck" select="upper-case($date)"/>

		<!-- Added in case need to check if part 2 of date is a month value -->
		<xsl:variable name="x2-is-valid-month">
			<xsl:call-template name="valid-month">
				<xsl:with-param name="value" select="$x2"/>
			</xsl:call-template>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="contains($quartercheck,'QUARTER')
			             or contains($quartercheck,'QTR')
						 or contains($quartercheck,'Q')">
                <xsl:choose>
                	<xsl:when test="number($x1) > 1000">
						<season><xsl:value-of select=
						   "concat($x2,' ',$x3)"/></season>
						<year><xsl:value-of select="$x1"/></year>
					</xsl:when>
					<xsl:when test="number($x3) > 1000">
						<season><xsl:value-of select=
						   "concat($x1,' ',$x2)"/></season>
  					    <year><xsl:value-of select="$x3"/></year>
					</xsl:when>
				</xsl:choose>
			</xsl:when>
			
			<xsl:when test="$order='range'">
				<xsl:choose>
					<!-- when "year month day-day" don't process as a range -->
					<xsl:when test="number($x1) > 1000
					  and $x2-is-valid-month='true' 
					  and (contains($x3,'-') or contains($x3,'_ENTITYSTART'))">
						<day>
							<!-- for a day range, take the first day -->
							<xsl:choose>
								<xsl:when test="contains($x3,'-')">
									<xsl:value-of select="substring-before($x3,'-')"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="substring-before($x3,'_ENTITYSTART')"/>
								</xsl:otherwise>
							</xsl:choose>
						</day>
						<xsl:call-template name="find-month-season">
							<xsl:with-param name="month" select="$x2"/>
						</xsl:call-template>
						<year><xsl:value-of select="$x1"/></year>
					</xsl:when>
					<xsl:otherwise>
 			           <xsl:call-template name="maybe-string-date">
			              <xsl:with-param name="date" select="$date"/>
			              <xsl:with-param name="nsd" select="$nsd"/>
			           </xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<!-- YYYY Month DD -->
			<xsl:when test="number($x1) > 1000 or $order='ymd'">
				<xsl:choose>
					<xsl:when test="number($x3) >= 0">
						<day><xsl:value-of select="number($x3)"/></day>
					</xsl:when>
					<xsl:otherwise>
						<day><xsl:value-of select="$x3"/></day>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:call-template name="find-month-season">
					<xsl:with-param name="month" select="$x2"/>
				</xsl:call-template>
				<year><xsl:value-of select="$x1"/></year>
			</xsl:when>

			<!-- Month DD YYYY  -->
			<xsl:when test="$order='mdy'"> 
				<day>
					<xsl:call-template name="remlett">
						<xsl:with-param name="str" select="$x2"/>
					</xsl:call-template>	
				</day>
				<xsl:call-template name="find-month-season">
					<xsl:with-param name="month" select="$x1"/>
				</xsl:call-template>
				<year><xsl:value-of select="$x3"/></year>
			</xsl:when>	

	        <!-- Two explicit tests for DD-MM-YYYY to handle cases where 
			     $order is not set. Note that if $order is not set, 
				 date will be treated as DD-MM-YYYY if $x2 is a number
				 between 1 and 12, inclusive.
			  -->
			<!-- DD Month YYYY -->
			<xsl:when test="$order='dmy'">
				<day>
					<xsl:call-template name="remlett">
						<xsl:with-param name="str" select="$x1"/>
					</xsl:call-template>	
				</day>
				<xsl:call-template name="find-month-season">
					<xsl:with-param name="month" select="$x2"/>
				</xsl:call-template>
				<year><xsl:value-of select="$x3"/></year>
			</xsl:when>

			<!-- DD Month YYYY -->
			<xsl:when test="number($x1) > 0 and $x2-is-valid-month='true'"> 
			   <day>
					<xsl:call-template name="remlett">
						<xsl:with-param name="str" select="$x1"/>
					</xsl:call-template>	
				</day>
				<xsl:call-template name="find-month-season">
					<xsl:with-param name="month" select="$x2"/>
				</xsl:call-template>
				<year><xsl:value-of select="$x3"/></year>		   
			</xsl:when>
             
			<!-- Month DD YYYY  -->
			<xsl:otherwise> 
				<day>
                    <xsl:choose>
                        <xsl:when test="number($x2)">
                             <xsl:value-of select="number($x2)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$x2"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </day>
				<xsl:call-template name="find-month-season">
					<xsl:with-param name="month" select="$x1"/>
				</xsl:call-template>
				<year><xsl:value-of select="$x3"/></year>
			</xsl:otherwise>	
		</xsl:choose>
	</xsl:template> <!-- date-2space -->



   <!-- ==================================================================== -->
   <!-- TEMPLATE: maybe-string-date
        NOTES:    Issue the string passed, within a string-date unless
		          the 'nsd' param is set, in which case issue
				  it bare. This is here to centralize this check, and to
				  keep conditions in several places simpler.
     -->
   <!-- ==================================================================== -->
   <xsl:template name="maybe-string-date">
		<xsl:param name="date"/>
        <xsl:param name="nsd"/>

		<xsl:choose>
			<xsl:when test="$nsd!=''">
				<xsl:value-of select="$date"/>
			</xsl:when>
            <xsl:otherwise>
			   <string-date>
  				  <xsl:value-of select="$date"/>
			   </string-date>
            </xsl:otherwise>
		</xsl:choose>				
   </xsl:template>


                    <!-- END DATE FROM STRING TEMPLATES -->



   <!-- ==================================================================== -->
   <!-- 
                             Date from Attributes
     -->
   <!-- ==================================================================== -->

	<xsl:template name="get-date-from-atts">
		<xsl:param name="datetype"/>
		<xsl:param name="element"/>
		<xsl:param name="date"/>
		<?nodepath att="day"?>
		<?nodepath att="mo"?>
		<?nodepath att="yr"?>
		<xsl:choose>
			<xsl:when test="$element='pub-date' or $element='pubdate'">
				<pub-date pub-type="{$datetype}">
					<xsl:call-template name="atts-date-guts">
						<xsl:with-param name="date" select="$date"/>
						</xsl:call-template>
				</pub-date>
				</xsl:when>
			<xsl:otherwise>
				<date date-type="{$datetype}">
					<xsl:call-template name="atts-date-guts">
						<xsl:with-param name="date" select="$date"/>
						</xsl:call-template>
				</date>
				</xsl:otherwise>
			</xsl:choose>
	</xsl:template>


	<xsl:template name="atts-date-guts">
		<xsl:param name="date"/>
		<xsl:call-template name="checkday">
			<xsl:with-param name="checkdate_day" select="$date/@day"/>
			</xsl:call-template>
		<xsl:call-template name="checkmonth">
			<xsl:with-param name="checkdate_month" select="$date/@mo"/>
			</xsl:call-template>
		<xsl:call-template name="checkyear">
			<xsl:with-param name="checkdate_year" select="$date/@yr"/>
			</xsl:call-template>
		</xsl:template>


	<xsl:template name="no-day">
		<xsl:param name="date"/>
		<month><xsl:value-of select="number(substring-before($date,'-'))"/></month>
		<year><xsl:value-of select="substring-after(substring-after($date,'-'),'-')"/></year>
		</xsl:template>



   <!-- ==================================================================== -->
   <!-- TEMPLATE: get-year-from-string
        Parses written-out date in the following forms and outputs
           <year> only. Modified copy of get-date-from-string template
						 
	    Parameters:  
	        date - a string that has been run through normalize-space() 
	                         and had all punctuation removed
		  	order - (optional) a string that defines the order of the parts sent
									dmy = day month year
									mdy = month day year
									ymd

		The template will work for these types of dates
		    (DD is day, YYYY is year):
		
			Month DD, YYYY
			DD Month YYYY
			YYYY Month DD
			Season YYYY
			YYYY Season
			Month YYYY
			YYYY Month
			Month-Month YYYY
			Season-Season YYYY
			DD-MM-YYYY (with order parameter)						
			DD/MM/YYYY (with order parameter)						
			DD.MM.YYYY (with order parameter)						
			MM-DD-YYYY (with order parameter)						
			MM/DD/YYYY (with order parameter)						
			MM.DD.YYYY (with order parameter)								  
     -->
   <!-- ==================================================================== -->
	<xsl:template name="get-year-from-string">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
		<xsl:param name="nsd"/>
		
		<xsl:variable name="cleandate"> <!-- remove commas and periods from string -->
		 	<xsl:call-template name="nocomma-nodot">
				<xsl:with-param name="str" select="$date"/>
			</xsl:call-template>
		</xsl:variable>

		
		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="$cleandate"/>
				</xsl:call-template>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$space-count=0">
					<xsl:call-template name="date-nospace-year">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
					    <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

            <!-- need to add charent testing -->
			<xsl:when test="contains($date,'-')
				               or contains($date,'ENTITYSTART_hyphen')
				               or contains($date,'ENTITYSTART_ndash')"> 
					<xsl:call-template name="date-range-year">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
					    <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>  

			<xsl:when test="$space-count=1">
					<xsl:call-template name="date-1space-year">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
					    <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

			<xsl:when test="$space-count=2">
					<xsl:call-template name="date-2space-year">
						<xsl:with-param name="date" select="$cleandate"/>
						<xsl:with-param name="order" select="$order"/>
					    <xsl:with-param name="nsd" select="$nsd"/>
						</xsl:call-template>
			</xsl:when>

			<!-- <xsl:when test="$space-count > 3">
				<xsl:choose>
						<xsl:when test="$order='citation'">
							<xsl:value-of select="$date"/>
						</xsl:when>
						<xsl:otherwise>
						   <xsl:call-template name="maybe-string-date">
						      <xsl:with-param name="date" select="$date"/>
							  <xsl:with-param name="nsd" select="$nsd"/>
						   </xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when> -->
		</xsl:choose>
	</xsl:template> <!-- get-year-from-string -->

    

   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-range-year
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-range-year">
		<xsl:param name="date"/>
	    <xsl:param name="nsd"/>
		<xsl:param name="order"/>

		<xsl:variable name="stringdate-mod">
			<xsl:call-template name="fix-range">
				<xsl:with-param name="date" select="$date"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="$stringdate-mod"/>
			</xsl:call-template>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$space-count=1">
				<xsl:call-template name="date-1space-year">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="$space-count=2">
				<xsl:call-template name="date-2space-year">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="'range'"/>
				    <xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<!-- <xsl:when test="$space-count > 2">
				<string-date><xsl:value-of select="$stringdate-mod"/></string-date>
			</xsl:when> -->
		</xsl:choose>
	</xsl:template>



   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-nospace-year
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-nospace-year">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
	    <xsl:param name="nsd"/>

		<xsl:variable name="stringdate-mod"
		   select="translate($date,'-./','   ')"/>
		<xsl:variable name="space-count">
			<xsl:call-template name="count-space">
				<xsl:with-param name="date" select="$stringdate-mod"/>
			</xsl:call-template>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$space-count=0">
				<!-- just a year -->
				<year><xsl:value-of select="$stringdate-mod"/></year>
			</xsl:when>
			<xsl:when test="$space-count=1">
				<xsl:call-template name="date-1space-year">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="$order"/>
					  <xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="$space-count=2">
				<xsl:call-template name="date-2space-year">
					<xsl:with-param name="date" select="$stringdate-mod"/>
					<xsl:with-param name="order" select="$order"/>
					  <xsl:with-param name="nsd" select="$nsd"/>
				</xsl:call-template>
			</xsl:when>
			<!-- <xsl:when test="$space-count > 2">
				<string-date><xsl:value-of select="$stringdate-mod"/></string-date>
			</xsl:when> -->
		</xsl:choose>
	</xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-1space-year
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-1space-year">
		<xsl:param name="date"/>
		<xsl:param name="order"/>
	    <xsl:param name="nsd"/>

		<xsl:variable name="x1" select="substring-before($date,' ')"/>
		<xsl:variable name="x2" select="substring-after($date,' ')"/>
        <xsl:variable name="yeardiff" 
		  select="string-length($x1) - string-length($x2)"/>

		<xsl:choose>
          <!--  <xsl:when test="$x1 > 1000">
                <xsl:choose>
                    <xsl:when test="$yeardiff > 0">
                        <year><xsl:value-of select="$x1"/>&#x2013;<xsl:value-of
						   select="substring($x1,1,$yeardiff)"/><xsl:value-of
						    select="$x2"/></year>
                        </xsl:when>
                    <xsl:otherwise>
                        <year><xsl:value-of select="$x1"/>&#x2013;<xsl:value-of
						   select="$x2"/></year>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>  -->

   			<xsl:when test="number($x1) > 1000">
       				<year><xsl:value-of select="$x1"/></year>
			</xsl:when>
   			<xsl:when test="number($x2) > 1000">
       				<year><xsl:value-of select="$x2"/></year>
			</xsl:when>
		</xsl:choose>
   </xsl:template>

   <!-- ==================================================================== -->
   <!-- TEMPLATE: date-2space-year
     -->
   <!-- ==================================================================== -->
	<xsl:template name="date-2space-year">
		<xsl:param name="date"/>
		<xsl:param name="nsd"/>
		<xsl:param name="order"/>

		<xsl:variable name="x1" select="substring-before($date,' ')"/>
		<xsl:variable name="x2" select="substring-before(substring-after($date,' '),' ')"/>
		<xsl:variable name="x3" select="substring-after(substring-after($date,' '),' ')"/>
		<xsl:variable name="quartercheck" select="upper-case($date)"/>
		<!-- Added in case second part of date is a month value -->
		<xsl:variable name="x2-is-valid-month"> 
           <xsl:call-template name="valid-month">
		      <xsl:with-param name="value" select="$x2"/>
		   </xsl:call-template>
		</xsl:variable>
		
		<xsl:choose>
			<xsl:when test="contains($quartercheck,'QUARTER') or
			                contains($quartercheck,'QTR') or 
							contains($quartercheck,'Q')">
                <xsl:choose>
                	<xsl:when test="number($x1) > 1000">
						<year><xsl:value-of select="$x1"/></year>
					</xsl:when>
					<xsl:when test="number($x3) > 1000">
						<year><xsl:value-of select="$x3"/></year>
					</xsl:when>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$order='range'"/>
			<!-- YYYY Month DD -->
			<xsl:when test="number($x1) > 1000 or $order='ymd'"> 
				<year><xsl:value-of select="$x1"/></year>
			</xsl:when>
			<!-- Month DD YYYY  -->
			<xsl:when test="$order='mdy'"> 
				<year><xsl:value-of select="$x3"/></year>
			</xsl:when>	
		        <!-- Two explicit tests for DD-MM-YYYY to handle cases
				     where $order is not set. Note that if $order is not set,
					 date will be treated as DD-MM-YYYY if $x2 is a number
					 between 1 and 12, inclusive.
			      -->
			<!-- DD Month YYYY -->
			<xsl:when test="$order='dmy'"> 
				<year><xsl:value-of select="$x3"/></year>
			</xsl:when>
			<!-- DD Month YYYY -->
			<xsl:when test="number($x1) > 0 and $x2-is-valid-month='true'">
				<year><xsl:value-of select="$x3"/></year>		   
			</xsl:when>
			<!-- Month DD YYYY  -->
			<xsl:otherwise> 
				<year><xsl:value-of select="$x3"/></year>
			</xsl:otherwise>	
		</xsl:choose>
	</xsl:template>


                     <!-- END YEAR FROM STRING TEMPLATES -->




   <!-- ==================================================================== -->
   <!-- 
                      MONTH AND SEASON NUMBERS and NAMES
     -->
   <!-- ==================================================================== -->


   <!-- ==================================================================== -->
   <!-- TEMPLATE: find-month-season
     -->
   <!-- ==================================================================== -->
   <xsl:template name="find-month-season">
      <xsl:param name="month"/>
   	<xsl:variable name="lc-month" select="lower-case($month)"/>

      <xsl:choose>
      	<xsl:when test="$lc-month='january' or $lc-month='jan'">
            <month>1</month></xsl:when>
      	<xsl:when test="$lc-month='february' or $lc-month='feb' or $lc-month='febraury'">
            <month>2</month></xsl:when>
      	<xsl:when test="$lc-month='march' or $lc-month='mar'">
            <month>3</month></xsl:when>
      	<xsl:when test="$lc-month='april' or $lc-month='apr'">
            <month>4</month></xsl:when>
      	<xsl:when test="$lc-month='may'">
            <month>5</month></xsl:when>
      	<xsl:when test="$lc-month='june' or $lc-month='jun'">
            <month>6</month></xsl:when>
      	<xsl:when test="$lc-month='july' or $lc-month='jul'">
            <month>7</month></xsl:when>
      	<xsl:when test="$lc-month='august' or $lc-month='aug'">
            <month>8</month></xsl:when>
      	<xsl:when test="$lc-month='september' or $lc-month='sep' or $lc-month='sept'">
            <month>9</month></xsl:when>
      	<xsl:when test="$lc-month='october' or $lc-month='oct'">
            <month>10</month></xsl:when>
      	<xsl:when test="$lc-month='november' or $lc-month='nov'">
            <month>11</month></xsl:when>
      	<xsl:when test="$lc-month='december' or $lc-month='dec'">
            <month>12</month></xsl:when> 
		<xsl:when test="1 &lt;= number($month) and number($month)&lt;= 12">
            <month><xsl:value-of select="number($month)"/></month></xsl:when>  

        <xsl:otherwise>
		   <season>
              <xsl:call-template name="clean-season">
                <xsl:with-param name="season" select="$month"/>
              </xsl:call-template>
              <!-- [[$month=<xsl:value-of select="$month"/>]]  --> 
           </season>
		</xsl:otherwise>
      </xsl:choose>
   </xsl:template> <!-- find-month-season -->


   <!-- ==================================================================== -->
   <!-- TEMPLATE: clean-season
        Cleans month and season ranges, feeds values to define-season 
		to build pmc-style-compliant season
        TODO:     Refactor: set delim char in tests, then do code once with
                  delim char filled in.
         Update 5/13/11: PMC-11550 use hypens instead of en-dashes
     -->
   <!-- ==================================================================== -->
    <xsl:template name="clean-season">
        <xsl:param name="season"/>
        <xsl:choose>
            <xsl:when test="contains($season,'/')">
                <xsl:variable name="x1">
                    <xsl:call-template name="find-season">
                    	<xsl:with-param name="str" select="upper-case(substring-before($season,'/'))"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="x2">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str" select="upper-case(substring-after($season,'/'))"/>
                        </xsl:call-template>
                    </xsl:variable>
                <xsl:value-of select="$x1"/>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$x2"/>
            </xsl:when>

            <xsl:when test="contains($season,'&#x2013;')">
                <xsl:variable name="x1">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str" select="upper-case(substring-before($season,'&#x2013;'))"/>
                       </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="x2">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str" select="upper-case(substring-after($season,'&#x2013;'))"/>
                        </xsl:call-template>
                </xsl:variable>
							<xsl:value-of select="$x1"/>
							<xsl:text>-</xsl:text>
							<xsl:value-of select="$x2"/>
            </xsl:when>

            <xsl:when test="contains($season,'-')">
                <xsl:variable name="x1">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str" select="upper-case(substring-before($season,'-'))"/>
                        </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="x2">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str" select="upper-case(substring-after($season,'-'))"/>
                        </xsl:call-template>
                </xsl:variable>
					 <xsl:choose>
					 	<xsl:when test="number($x1) and number($x2)">
							<xsl:call-template name="get-month">
								<xsl:with-param name="month" select="$x1"/>
								</xsl:call-template>
							<xsl:text>-</xsl:text>
							<xsl:call-template name="get-month">
								<xsl:with-param name="month" select="$x2"/>
								</xsl:call-template>
							</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$x1"/>
							<xsl:text>-</xsl:text>
							<xsl:value-of select="$x2"/>
							</xsl:otherwise>
					 	</xsl:choose>
            </xsl:when>

            <xsl:when test="contains($season,'_ENTITYSTART_#8211_ENTITYEND_')">
                <xsl:variable name="x1">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str"
                        	select="upper-case(substring-before($season,'_ENTITYSTART_#8211_ENTITYEND_'))"/>
                        </xsl:call-template>
                    </xsl:variable>
                <xsl:variable name="x2">
                    <xsl:call-template name="find-season">
                        <xsl:with-param name="str"
                        	select="upper-case(substring-after($season,'_ENTITYSTART_#8211_ENTITYEND_'))"/>
                        </xsl:call-template>
                    </xsl:variable>
                <xsl:value-of select="$x1"/>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$x2"/>
            </xsl:when>

            <xsl:otherwise>
                <xsl:value-of select="$season"/>
            </xsl:otherwise>
        </xsl:choose>
   </xsl:template>
    

   <!-- ==================================================================== -->
   <!-- TEMPLATE: find-season
        Identifies string and writes out PMC style abbreviation.
        If no match, write out value in title case.
     -->
   <!-- ==================================================================== -->
    <xsl:template name="find-season">
        <xsl:param name="str"/>
        <xsl:choose>
            <xsl:when test="contains($str,'JAN')">Jan</xsl:when>
            <xsl:when test="contains($str,'FEB')">Feb</xsl:when>
            <xsl:when test="contains($str,'MAR')">Mar</xsl:when>
            <xsl:when test="contains($str,'APR')">Apr</xsl:when>
            <xsl:when test="contains($str,'MAY')">May</xsl:when>
            <xsl:when test="contains($str,'JUN')">Jun</xsl:when>
            <xsl:when test="contains($str,'JUL')">Jul</xsl:when>
            <xsl:when test="contains($str,'AUG')">Aug</xsl:when>
            <xsl:when test="contains($str,'SEP')">Sep</xsl:when>
            <xsl:when test="contains($str,'OCT')">Oct</xsl:when>
            <xsl:when test="contains($str,'NOV')">Nov</xsl:when>
            <xsl:when test="contains($str,'DEC')">Dec</xsl:when>
            <xsl:when test="$str='1' or $str='01'">Jan</xsl:when>
            <xsl:when test="$str='2' or $str='02'">Feb</xsl:when>
            <xsl:when test="$str='3' or $str='03'">Mar</xsl:when>
            <xsl:when test="$str='4' or $str='04'">Apr</xsl:when>
            <xsl:when test="$str='5' or $str='05'">May</xsl:when>
            <xsl:when test="$str='6' or $str='06'">Jun</xsl:when>
            <xsl:when test="$str='7' or $str='07'">Jul</xsl:when>
            <xsl:when test="$str='8' or $str='08'">Aug</xsl:when>
            <xsl:when test="$str='9' or $str='09'">Sep</xsl:when>
            <xsl:when test="$str='10'">Oct</xsl:when>
            <xsl:when test="$str='11'">Nov</xsl:when>
            <xsl:when test="$str='12'">Dec</xsl:when>
            <xsl:when test="contains($str,'WIN')">Winter</xsl:when>
            <xsl:when test="contains($str,'SPR')">Spring</xsl:when>
            <xsl:when test="contains($str,'SUM')">Summer</xsl:when>
            <xsl:when test="contains($str,'FAL')">Fall</xsl:when>
            <xsl:when test="contains($str,'AUT')">Autumn</xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="title-case-string">
                    <xsl:with-param name="str" select="$str"/>
                    </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
   
   <!-- ==================================================================== -->
   <!-- TEMPLATE: valid-month
        Returns 'true' when value passed in is a valid name for a month
		or numeric value from 1 to 12.

		(Should delete and just test month-name-to-number for non-zero.
         That way they'll both behave exactly the same. Less maintenance.
     -->
   <!-- ==================================================================== -->
   <xsl:template name="valid-month">
      <xsl:param name="value"/>
      
      <xsl:choose>
         <xsl:when test="$value='January' or $value='Jan'">true</xsl:when>
	 <xsl:when test="$value='February'    or $value='Feb'">true</xsl:when>
	 <xsl:when test="$value='March'       or $value='Mar'">true</xsl:when>
	 <xsl:when test="$value='April'       or $value='Apr'">true</xsl:when>
	 <xsl:when test="$value='May'">true</xsl:when>
	 <xsl:when test="$value='June'        or $value='Jun'">true</xsl:when>
	 <xsl:when test="$value='July'        or $value='Jul'">true</xsl:when>
	 <xsl:when test="$value='August'      or $value='Aug'">true</xsl:when>
	 <xsl:when test="$value='September'   or $value='Sep' or $value='Sept'">true</xsl:when>
	 <xsl:when test="$value='October'     or $value='Oct'">true</xsl:when>
	 <xsl:when test="$value='November'    or $value='Nov'">true</xsl:when>
	 <xsl:when test="$value='December'    or $value='december' or
	                 $value='Dec'">true</xsl:when> 
	 <xsl:when test="1 &lt;= number($value) and
                     number($value)&lt;= 12">true</xsl:when>
	 <xsl:otherwise>false</xsl:otherwise>
      </xsl:choose>
   </xsl:template> <!-- valid-month -->


   <xsl:template name="get-month">
		<xsl:param name="month"/>
		<xsl:choose>
			<xsl:when test="number($month)=1">January</xsl:when>
			<xsl:when test="number($month)=2">February</xsl:when>
			<xsl:when test="number($month)=3">March</xsl:when>
			<xsl:when test="number($month)=4">April</xsl:when>
			<xsl:when test="number($month)=5">May</xsl:when>
			<xsl:when test="number($month)=6">June</xsl:when>
			<xsl:when test="number($month)=7">July</xsl:when>
			<xsl:when test="number($month)=8">August</xsl:when>
			<xsl:when test="number($month)=9">September</xsl:when>
			<xsl:when test="number($month)=10">October</xsl:when>
			<xsl:when test="number($month)=11">November</xsl:when>
			<xsl:when test="number($month)=12">December</xsl:when>
			</xsl:choose>
		</xsl:template>

	<xsl:template name="get-month-abb">
		<xsl:param name="month"/>
		<xsl:choose>
			<xsl:when test="number($month)=1">Jan</xsl:when>
			<xsl:when test="number($month)=2">Feb</xsl:when>
			<xsl:when test="number($month)=3">Mar</xsl:when>
			<xsl:when test="number($month)=4">Apr</xsl:when>
			<xsl:when test="number($month)=5">May</xsl:when>
			<xsl:when test="number($month)=6">Jun</xsl:when>
			<xsl:when test="number($month)=7">Jul</xsl:when>
			<xsl:when test="number($month)=8">Aug</xsl:when>
			<xsl:when test="number($month)=9">Sep</xsl:when>
			<xsl:when test="number($month)=10">Oct</xsl:when>
			<xsl:when test="number($month)=11">Nov</xsl:when>
			<xsl:when test="number($month)=12">Dec</xsl:when>
			</xsl:choose>
		</xsl:template>

	<xsl:template name="get-month-no">
		<xsl:param name="month"/>
		<xsl:choose>
			<xsl:when test="number($month)">
				<xsl:value-of select="number($month)"/>
				</xsl:when>
			<xsl:when test="$month='January'">1</xsl:when>
			<xsl:when test="$month='February'">2</xsl:when>
			<xsl:when test="$month='March'">3</xsl:when>
			<xsl:when test="$month='April'">4</xsl:when>
			<xsl:when test="$month='May'">5</xsl:when>
			<xsl:when test="$month='June'">6</xsl:when>
			<xsl:when test="$month='July'">7</xsl:when>
			<xsl:when test="$month='August'">8</xsl:when>
			<xsl:when test="$month='September'">9</xsl:when>
			<xsl:when test="$month='October'">10</xsl:when>
			<xsl:when test="$month='November'">11</xsl:when>
			<xsl:when test="$month='December'">12</xsl:when>
			</xsl:choose>
		</xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE:  month-name-to-number
        NOTES:     Only test first 3 characters of name. That way we catch
                   a variety of abbreviations and typos. We will also match
                   some nonsense on occasion.
                   Additional cases can be added for other languages.
        PARAMS:    name     the month name to decode
                   nums     if true, numbers 1-12 are ok also.
        RETURN:    A number 1-12, or nil on failure.
     -->
   <!-- ==================================================================== -->
   <xsl:template name="month-name-to-number">
      <xsl:param name="name"/>
      <xsl:param name="nums" select="'1'"/>

      <xsl:variable name="UCname" select="upper-case(normalize-space(
			   translate($name,'.',' ')))"/>

   	<xsl:variable name="f3" select="if (substring($UCname,1,3) castable as xs:integer) 
   		then (number(substring($UCname,1,3)))
   		else (substring($UCname,1,3))"/>

      <xsl:choose>
	     <xsl:when test="$nums and number($f3)>=1 and number($f3)&lt;=12
		                 and number($f3)=floor($f3)">
		    <xsl:value-of select="number($f3)"/>
		 </xsl:when>
	     <xsl:when test="$f3 = 'JAN'">1</xsl:when>
	     <xsl:when test="$f3 = 'FEB'">2</xsl:when>
	     <xsl:when test="$f3 = 'MAR'">3</xsl:when>
	     <xsl:when test="$f3 = 'APR'">4</xsl:when>
	     <xsl:when test="$f3 = 'MAY'">5</xsl:when>
	     <xsl:when test="$f3 = 'JUN'">6</xsl:when>
	     <xsl:when test="$f3 = 'JUL'">7</xsl:when>
	     <xsl:when test="$f3 = 'AUG'">8</xsl:when>
	     <xsl:when test="$f3 = 'SEP'">9</xsl:when>
	     <xsl:when test="$f3 = 'OCT'">10</xsl:when>
	     <xsl:when test="$f3 = 'NOV'">11</xsl:when>
	     <xsl:when test="$f3 = 'DEC'">12</xsl:when>
         <xsl:otherwise/>
      </xsl:choose>
   </xsl:template>


   <!-- ==================================================================== -->
   <!-- TEMPLATE:  month-number-to-name
        NOTES:     
        PARAMS:    num      month number, 1-12
		           lang     what language to generate name in (later)
                   abbrev   If true, chop to first 3 letters
        RETURNS:   name of month, or '' on failure.
     -->
   <!-- ==================================================================== -->
   <xsl:template name="month-number-to-name">
      <xsl:param name="num"/>
      <xsl:param name="lang" select="EN"/>
      <xsl:param name="abbrev" select="1"/>

	  <xsl:variable name="monthName">
        <xsl:choose>
	     <xsl:when test="number($num) = 1">January</xsl:when>
	     <xsl:when test="number($num) = 2">February</xsl:when>
	     <xsl:when test="number($num) = 3">March</xsl:when>
	     <xsl:when test="number($num) = 4">April</xsl:when>
	     <xsl:when test="number($num) = 5">May</xsl:when>
	     <xsl:when test="number($num) = 6">June</xsl:when>
	     <xsl:when test="number($num) = 7">July</xsl:when>
	     <xsl:when test="number($num) = 8">August</xsl:when>
	     <xsl:when test="number($num) = 9">September</xsl:when>
	     <xsl:when test="number($num) =10">October</xsl:when>
	     <xsl:when test="number($num) =11">November</xsl:when>
	     <xsl:when test="number($num) =12">December</xsl:when>
         <xsl:otherwise/>
        </xsl:choose>
	  </xsl:variable>

      <xsl:choose>
	     <xsl:when test="$abbrev != 0 and $monthName != ''">
		    <xsl:value-of select="substring($monthName,1,3)"/>
		 </xsl:when>
		 <xsl:otherwise>
		    <xsl:value-of select="$monthName"/>
		 </xsl:otherwise>
      </xsl:choose>
   </xsl:template>




   <!-- ==================================================================== -->
   <!--                     Date Arithmetic Templates 

        PARAMS:  day/month/year to start at
                 change: number of days to add or subtract
        TODO:    Since change is signed, modify-date is redundant
     -->
   <!-- ==================================================================== -->
	<xsl:template name="get-release-delay">
		<xsl:param name="nodes"/>		
		<xsl:variable name="releasedelay">
			<xsl:call-template name="get-shortest-release-delay">
				<xsl:with-param name="nodes" select="$nodes"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$releasedelay='BAD-RELEASE-DELAY'">
		<xsl:message terminate="yes">
			BAD RELEASE DELAY value <xsl:value-of select="$nodes"/>
		</xsl:message>
		</xsl:if>
		<xsl:variable name="months">
			<xsl:choose>
				<xsl:when test="normalize-space(substring-after($releasedelay,'|'))='0'">					
					<xsl:value-of select="normalize-space(substring-before($releasedelay,'|')) cast as xs:integer"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="(substring-before($releasedelay,'|') cast as xs:integer) + 1"/>
					</xsl:otherwise>
				</xsl:choose>
		</xsl:variable>		
		
				<xsl:value-of select="$releasedelay"/>
		</xsl:template>



	<xsl:template name="get-shortest-release-delay">
		<xsl:param name="nodes"/>
		<xsl:param name="winner"/>
		<xsl:variable name="months" select="substring-before($nodes[1],'|')"/>
		<xsl:variable name="days" select="if (ends-with($nodes[1],'?')) then (substring-after(substring-before($nodes[1],'?'),'|'))
			else (substring-after($nodes[1],'|'))"/>
		<xsl:variable name="win-months" select="substring-before($winner,'|')"/>
		<xsl:variable name="win-days" select="if (ends-with($winner,'?')) then (substring-after(substring-before($winner,'?'),'|'))
			else (substring-after($winner,'|'))"/>
		<xsl:choose>
			<xsl:when test="$nodes">
				<xsl:choose>
					<xsl:when test="xs:integer($days) &gt; 31">
						<xsl:value-of select="'BAD-RELEASE-DELAY'"/>
						</xsl:when>
					<xsl:when test="$winner=''">
						<xsl:call-template name="get-shortest-release-delay">
							<xsl:with-param name="winner" select="$nodes[1]"/>
							<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
							</xsl:call-template>
						</xsl:when>
					<xsl:when test="$nodes[1] = $winner">
						<xsl:call-template name="get-shortest-release-delay">
							<xsl:with-param name="winner" select="$winner"/>
							<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
							</xsl:call-template>
						</xsl:when>
					<xsl:when test="xs:integer($months) &lt; xs:integer($win-months)">
						<xsl:call-template name="get-shortest-release-delay">
							<xsl:with-param name="winner" select="$nodes[1]"/>
							<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
							</xsl:call-template>
						</xsl:when>
					<xsl:when test="xs:integer($months) &gt; xs:integer($win-months)">
						<xsl:call-template name="get-shortest-release-delay">
							<xsl:with-param name="winner" select="$winner"/>
							<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
							</xsl:call-template>
						</xsl:when>
					<xsl:when test="xs:integer($months) = xs:integer($win-months)">
						<xsl:choose>
							<xsl:when test="xs:integer($days) &lt; xs:integer($win-days)">
								<xsl:call-template name="get-shortest-release-delay">
									<xsl:with-param name="winner" select="$nodes[1]"/>
									<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
									</xsl:call-template>
								</xsl:when>
							<xsl:when test="xs:integer($days) &gt; xs:integer($win-days)">
								<xsl:call-template name="get-shortest-release-delay">
									<xsl:with-param name="winner" select="$winner"/>
									<xsl:with-param name="nodes" select="$nodes[position()!=1]"/>
									</xsl:call-template>
								</xsl:when>
							</xsl:choose>
						</xsl:when>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="if (ends-with($winner,'?')) then (substring-before($winner,'?'))
						else ($winner)"/>
					</xsl:otherwise>
				</xsl:choose>
		</xsl:template>


	<xsl:template name="add-days-months">
		<xsl:param name="day" />
		<xsl:param name="month"/>
		<xsl:param name="year"/>
		<xsl:param name="plusmonths"/>
		<xsl:param name="plusdays"/>
		
		<xsl:variable name="month-days">
			<xsl:call-template name="get-month-days">
				<xsl:with-param name="month" select="$month cast as xs:string"/>
				<xsl:with-param name="year" select="$year"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="next-month-days">
			<xsl:call-template name="get-month-days">
				<xsl:with-param name="month" select="($month cast as xs:integer) + 1"/>
				<xsl:with-param name="year" select="$year"/>
			</xsl:call-template>
		</xsl:variable>
		
	<!--	<xsl:comment>month=<xsl:value-of select="$month"/> | day=<xsl:value-of select="$day"/> | month-days=<xsl:value-of select="$month-days"/> | $plusmonths=<xsl:value-of select="$plusmonths"/> | $plusdays=<xsl:value-of select="$plusdays"/></xsl:comment>
	-->
		<!-- http://jira/browse/PEZ-1423 AB: If month contains dash, stylechecker should flag it, it is a datatype mismatch here -->
		<!-- loc-month, if $month is not numeric, selects the first letters or numbers in the string provided. -->
		<xsl:variable name="loc-month">
			<xsl:choose>
				<xsl:when test="$month castable as xs:integer">
					<xsl:value-of select="$month"/>
				</xsl:when>
				<xsl:when test="$month castable as xs:string
					and matches(xs:string($month),'^([a-zA-Z]+).*')">
					<xsl:analyze-string select="xs:string($month)" regex="^([a-zA-Z]+).*">
						<xsl:matching-substring>
							<xsl:call-template name="month-name-to-number">
								<xsl:with-param name="name" select="regex-group(1)"/>
							</xsl:call-template>
						</xsl:matching-substring>
					</xsl:analyze-string>
				</xsl:when>
				<xsl:when test="$month castable as xs:string
					and matches(xs:string($month),'^([0-9]+).*')">
					<xsl:analyze-string select="xs:string($month)" regex="^([0-9]+).*">
						<xsl:matching-substring>
							<xsl:value-of select="regex-group(1)"/>
						</xsl:matching-substring>
					</xsl:analyze-string>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$month"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$loc-month &gt; 12">
						<xsl:call-template name="add-days-months">
							<xsl:with-param name="day" select="$day"/>
							<xsl:with-param name="month" select="$loc-month - 12"/>
							<xsl:with-param name="year" select="$year + 1"/>
							<xsl:with-param name="plusmonths" select="$plusmonths - 12"/>
							<xsl:with-param name="plusdays" select="$plusdays"/>
							</xsl:call-template>
				</xsl:when>
			<xsl:when test="$plusmonths &gt; 0">
						<xsl:call-template name="add-days-months">
							<xsl:with-param name="day" select="$day"/>
							<xsl:with-param name="month" select="$loc-month + $plusmonths"/>
							<xsl:with-param name="year" select="$year"/>
							<xsl:with-param name="plusmonths" select="0"/>
							<xsl:with-param name="plusdays" select="$plusdays"/>
							</xsl:call-template>
				</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="$plusdays &gt; 0 or $day &gt; $month-days">
								<xsl:choose>
									<xsl:when test="$day + $plusdays &gt; $month-days">
										<xsl:call-template name="add-days-months">
											<xsl:with-param name="day" select="$day + $plusdays - $month-days"/>
											<xsl:with-param name="month" select="$loc-month"/>
											<xsl:with-param name="year" select="$year"/>
											<xsl:with-param name="plusmonths" select="1"/>
											<xsl:with-param name="plusdays" select="0"/>
											</xsl:call-template>
										</xsl:when>
									<xsl:when test="$day + $plusdays &lt;= $month-days">
										<xsl:call-template name="add-days-months">
											<xsl:with-param name="day" select="$day + $plusdays"/>
											<xsl:with-param name="month" select="$loc-month"/>
											<xsl:with-param name="year" select="$year"/>
											<xsl:with-param name="plusmonths" select="0"/>
											<xsl:with-param name="plusdays" select="0"/>
											</xsl:call-template>
										</xsl:when>
									</xsl:choose>
								</xsl:when>
							<xsl:otherwise>
								<day><xsl:value-of select="$day"/></day>
								<month><xsl:value-of select="$loc-month"/></month>
								<year><xsl:value-of select="$year"/></year>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
			
			</xsl:choose>
	</xsl:template>
		
		
		
		
		
	<xsl:template name="modify-date">
		<xsl:param name="day"/>
		<xsl:param name="month"/>
		<xsl:param name="year"/>
		<xsl:param name="change"/>

		<xsl:choose>
			<xsl:when test="$change > 0">
				<xsl:call-template name="add-date">
					<xsl:with-param name="day" select="$day"/>
					<xsl:with-param name="month" select="$month"/>
					<xsl:with-param name="year" select="$year"/>
					<xsl:with-param name="change" select="$change"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="subtract-date">
					<xsl:with-param name="day" select="$day"/>
					<xsl:with-param name="month" select="$month"/>
					<xsl:with-param name="year" select="$year"/>
					<xsl:with-param name="change" select="0 - $change"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>


	<xsl:template name="add-date">
		<xsl:param name="day"/>
		<xsl:param name="month"/>
		<xsl:param name="year"/>
		<xsl:param name="change"/>

		<xsl:variable name="month-days">
			<xsl:call-template name="get-month-days">
				<xsl:with-param name="month" select="$month"/>
				<xsl:with-param name="year" select="$year"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="next-month-days">
			<xsl:call-template name="get-month-days">
				<xsl:with-param name="month" select="$month + 1"/>
				<xsl:with-param name="year" select="$year"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$day + $change &lt;= $month-days">
				<day><xsl:value-of select="$day + $change"/></day>
				<month><xsl:value-of select="$month"/></month>
				<year><xsl:value-of select="$year"/></year>
			</xsl:when>
			<xsl:when test="$day + $change - $month-days &lt;= $next-month-days">
				<day><xsl:value-of select="$day + $change - $month-days"/></day>
				<xsl:choose>
					<xsl:when test="$month + 1 &lt;= 12">
						<month>
							<xsl:value-of select="$month + 1"/>
						</month>
						<year><xsl:value-of select="$year"/></year>
					</xsl:when>
					<xsl:otherwise>
						<month>
							<xsl:value-of select="'1'"/>
						</month>
						<year><xsl:value-of select="$year + 1"/></year>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<!-- so far only works for dates rolling into the next month -->	
		</xsl:choose>
	</xsl:template>


	<xsl:template name="subtract-date">
	<!-- subtract is not working yet -->
		<xsl:param name="day"/>
		<xsl:param name="month"/>
		<xsl:param name="year"/>
		<xsl:param name="change"/>
		<xsl:variable name="month-days">
			<xsl:call-template name="get-month-days">
				<xsl:with-param name="month" select="$month -1"/>
				<xsl:with-param name="year" select="$year"/>
			</xsl:call-template>
		</xsl:variable>
		[[<xsl:value-of select="$month-days"/>]]
	</xsl:template>

	<xsl:template name="get-month-days">
		<xsl:param name="month"/>
		<xsl:param name="year"/>

		<xsl:choose>	
			<xsl:when test="$month='4' or $month='6' or
			                $month='9' or $month='11'">30</xsl:when>
			<xsl:when test="$month='2'">
				<xsl:choose>
					<xsl:when test="$year mod    4 = 0 and
					                $year mod 100 != 0">29</xsl:when>
					<xsl:otherwise>28</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>31</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- ==================================================================== -->
	<!-- TEMPLATE:  season-to-first-month-number
		NOTES:     
		PARAMS:    season
		RETURNS:   number of first month in season returned.
		USED IN: <xsl:template name="release-date">
	-->
	<!-- ==================================================================== -->
	<xsl:template name="season-to-first-month-number">
		<xsl:param name="season"/>
		<xsl:variable name="clean-season">
			<xsl:call-template name="clean-season">
				<xsl:with-param name="season" select="$season"/>
			</xsl:call-template>
		</xsl:variable>
		
		<xsl:variable name="cap-clean-season" select="upper-case($clean-season)"/>
		
		<!-- http://jira/browse/PMC-6278 Exceptions based on DateTranslator table in PMC3 -->
		<xsl:choose>
			<xsl:when test="starts-with($cap-clean-season,'WIN')">12</xsl:when>
			<xsl:when test="starts-with($cap-clean-season,'SPR')">3</xsl:when>
			<xsl:when test="starts-with($cap-clean-season,'SUM')">6</xsl:when>
			<xsl:when test="starts-with($cap-clean-season,'FAL')">9</xsl:when>
			<xsl:when test="starts-with($cap-clean-season,'AUT')">9</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="month-name-to-number">
					<xsl:with-param name="name" select="$season"/>
					<xsl:with-param name="nums" select="''"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	


<!-- STRINGS -->
	
	<!-- ==================================================================== -->
	<!--
                             GLOBAL LITERAL STRINGS
     -->
	<!-- ==================================================================== -->
	
	<!-- Character classes for basic ASCII -->
	<xsl:param name="lower" select="'abcdefghijklmnopqrstuvwxyz'"/>
	<xsl:param name="upper" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
	<xsl:param name="alpha" select="concat($lower,$upper)"/>
	<xsl:param name="digit" select="'0123456789'"/>
	<xsl:param name="alnum" select="concat($lower,$upper,$digit)"/>
	<xsl:param name="space" select="' &#9;&#10;&#13;'"/> <!-- sp tab cr lf -->
	
	<!-- Punctuation other than XML name characters. Namely:
             x21:    !&quo;#$%&' (pus apostrophe, #x27/d39 via variable)
             x28:    ()*+,/
             x3B:    ;&lt;=&gt;?
             x40:    @
             x5B:    [\]^
             x60:    `
             x7B:    {|}~            -->
	<xsl:param name="apostrophe">'</xsl:param>
	<xsl:param name="nonnamepunct">
		<xsl:value-of select="concat($apostrophe,
			'&#x0021;&#x0022;&#x0023;&#x0024;&#x0025;&#x0026;',
			'&#x0028;&#x0029;&#x002A;&#x002B;&#x002C;&#x002F;',
			'&#x003B;&#x003C;&#x003D;&#x003E;&#x003F;',
			'&#x0040;',
			'&#x005B;&#x005C;&#x005D;&#x005E;',
			'&#x0060;',
			'&#x007B;&#x007C;&#x007D;&#x007E;'
			)">
		</xsl:value-of>
	</xsl:param>
	
	<!-- Punctuation in general (including name chars '.-_:') -->
	<xsl:param name="punct">
		<xsl:value-of select="concat($nonnamepunct, '.-_:')"/>
	</xsl:param>
	
	<!-- Nonname: characters not allowed in XML names -->
	<xsl:param name="nonname">
		<xsl:value-of select="concat($nonnamepunct, $space)"/>
	</xsl:param>
	
	
	
	<!-- ==================================================================== -->
	<!--
           Character classes for Latin-1 (slightly better than just ASCII) 
           
           Also included because they were in capitalize and knockdown, below:
              x150/x151 capital/small o with double acute
              x10C/x10D capital/small c with caron
     -->
	<!-- ==================================================================== -->
	<xsl:param name="lowerLat1" select="concat($lower,
		'&#x00E0;&#x00E1;&#x00E2;&#x00E3;&#x00E4;&#x00E5;&#x00E6;&#x00E7;',
		'&#x00E8;&#x00E9;&#x00EA;&#x00EB;&#x00EC;&#x00ED;&#x00EE;&#x00EF;',
		'&#x00F0;&#x00F1;&#x00F2;&#x00F3;&#x00F4;&#x00F5;&#x00F6;',
		'&#x00F8;&#x00F9;&#x00FA;&#x00FB;&#x00FC;&#x00FD;&#x00FE;',
		'&#x0151;&#x010D;')"/>
	<xsl:param name="upperLat1" select="concat($upper,
		'&#x00C0;&#x00C1;&#x00C2;&#x00C3;&#x00C4;&#x00C5;&#x00C6;&#x00C7;',
		'&#x00C8;&#x00C9;&#x00CA;&#x00CB;&#x00CC;&#x00CD;&#x00CE;&#x00CF;',
		'&#x00D0;&#x00D1;&#x00D2;&#x00D3;&#x00D4;&#x00D5;&#x00D6;',
		'&#x00D8;&#x00D9;&#x00DA;&#x00DB;&#x00DC;&#x00DD;&#x00DE;',
		'&#x0150;&#x010C;')"/>
	<xsl:param name="uncasedLat1" select="'&#x00DF;&#x00FF;'"/>
	<!-- Uncased: sharp s xDF, y with diaeresis xFF -->
	
	<xsl:param name="alphaLat1"
		select="concat($lowerLat1,$upperLat1,$uncasedLat1)"/>
	<xsl:param name="digitLat1" select="$digit"/>
	<xsl:param name="alnumLat1" select="concat($alphaLat1,$digitLat1)"/>
	<xsl:param name="spaceLat1" select="concat($space,'&#160;')"/> <!--nbsp-->
	
	<!-- Latin 1 adds symbols from xA1 to BF, D7, F7. Not xA0=nbsp -->
	<xsl:param name="nonnamepunctLat1" select="concat($nonnamepunct,
		'&#xA1;&#xA2;&#xA3;&#xA4;&#xA5;&#xA6;&#xA7;',
		'&#xA8;&#xA9;&#xAA;&#xAB;&#xAC;&#xAD;&#xAE;&#xAF;',
		'&#xB0;&#xB1;&#xB2;&#xB3;&#xB4;&#xB5;&#xB6;&#xB7;',
		'&#xB8;&#xB9;&#xBA;&#xBB;&#xBC;&#xBD;&#xBE;&#xBF;',
		'&#xD7;&#xF7;')"/>         
	
	<xsl:param name="punctLat1" select="concat($nonnamepunctLat1, '.-_:')"/>
	
	<xsl:param name="nonnameLat1"
		select="concat($nonnamepunctLat1,$spaceLat1)"/>
	
	
	<!-- ==================================================================== -->
	<!--
                       Character classes for Latin-Extended
     -->
	<!-- ==================================================================== -->
	
	<!-- Latin Extended-A: 0x0100-0x017F (alpha only) -->
	<xsl:param name="lowerExtendedA" select="concat(
		'&#x0101;&#x0103;&#x0105;&#x0107;&#x0109;&#x010B;&#x010D;&#x010F;',
		'&#x0111;&#x0113;&#x0115;&#x0117;&#x0119;&#x011B;&#x011D;&#x011F;',
		'&#x0121;&#x0123;&#x0125;&#x0127;&#x0129;&#x012B;&#x012D;&#x012F;',
		'&#x0131;&#x0133;&#x0135;&#x0137;',
		'&#x013A;&#x013C;&#x013E;&#x0140;&#x0142;&#x0144;&#x0146;&#x0148;',
		'&#x014B;&#x014D;&#x014F;&#x0151;&#x0153;&#x0155;&#x0157;&#x0159;',
		'&#x015B;&#x015D;&#x015F;&#x0161;&#x0163;&#x0165;&#x0167;&#x0169;',
		'&#x016B;&#x016D;&#x016F;&#x0171;&#x0173;&#x0175;&#x0177;',
		'&#x00FF;',
		'&#x017A;&#x017C;&#x017E;'
		)"/>
	<xsl:param name="upperExtendedA" select="concat(
		'&#x0100;&#x0102;&#x0104;&#x0106;&#x0108;&#x010A;&#x010C;&#x010E;',
		'&#x0110;&#x0112;&#x0114;&#x0116;&#x0118;&#x011A;&#x011C;&#x011E;',
		'&#x0120;&#x0122;&#x0124;&#x0126;&#x0128;&#x012A;&#x012C;&#x012E;',
		'&#x0130;&#x0132;&#x0134;&#x0136;',
		'&#x0139;&#x013B;&#x013D;&#x013F;&#x0141;&#x0143;&#x0145;&#x0147;',
		'&#x014A;&#x014C;&#x014E;&#x0150;&#x0152;&#x0154;&#x0156;&#x0158;',
		'&#x015A;&#x015C;&#x015E;&#x0160;&#x0162;&#x0164;&#x0166;&#x0168;',
		'&#x016A;&#x016C;&#x016E;&#x0170;&#x0172;&#x0174;&#x0176;',
		'&#x0178;',
		'&#x0179;&#x017B;&#x017D;'
		)"/>
	<xsl:param name="uncasedExtendedA" select="'&#x138;&#x149;&#x17F;'"/>
	<!-- x138 Latin Small Letter Kra (greenlandic)
           x149 Latin Small Letter n preceded by apostrophe
           X17F Latin Small Letter Long S (cf 0xDF??)
        -->
	
	
	<!-- Latin Extended-B: 0x0180-0x024F (alpha only, a few look like punct) -->
	<!-- Problems: Some chars only in one case, some in undefined case.
        Some in 3 cases (e.g. LJ/Lj/lj). Most case pairs are adjacent,
        but not all (18f/1dd, 1f6/195, 220/19e, 1F7/1bf).
        x241 capital glottal stop maps to x294, in a later block.
     -->
	<!-- These 3 sets are for title-cased letters in Unicode. -->
	<!-- KNOWN BUG: Mixed-case characters don't get case-translation. -->
	<xsl:param name="titleLower" select="'&#x01C6;&#x01C9;&#x01CC;&#x01F3;'"/>
	<xsl:param name="titleMixed" select="'&#x01C5;&#x01C8;&#x01CB;&#x01F2;'"/>
	<xsl:param name="titleUpper" select="'&#x01C4;&#x01C7;&#x01CA;&#x01F1;'"/>
	
	<xsl:param name="lowerExtendedB" select="concat($titleLower,
		'&#x0183;&#x0185;&#x0188;&#x018C;&#x0192;&#x0199;&#x01A1;&#x01A3;',
		'&#x01A5;&#x01A8;&#x01AD;&#x01B0;&#x01B4;&#x01B6;&#x01B9;&#x01BD;',
		'&#x01CE;&#x01D0;&#x01D2;&#x01D4;&#x01D6;&#x01D8;&#x01DA;&#x01DC;',
		'&#x01DF;&#x01E1;&#x01E3;&#x01E5;&#x01E7;&#x01E9;&#x01EB;&#x01ED;&#x01EF;',
		'&#x01F5;&#x01F9;&#x01FB;&#x01FD;&#x01FF;',
		'&#x0201;&#x0203;&#x0205;&#x0207;&#x0209;&#x020B;&#x020D;&#x020F;',
		'&#x0211;&#x0213;&#x0215;&#x0217;&#x0219;&#x021B;&#x021D;&#x021F;',
		'&#x0223;&#x0225;&#x0227;&#x0229;&#x022B;&#x022D;&#x022F;&#x0231;&#x0233;',
		'&#x23C;',
		'&#x1dd;&#x195;&#x19e;&#x1bf;',
		'&#x0294;'
		)"/>
	<xsl:param name="upperExtendedB" select="concat($titleUpper,
		'&#x0182;&#x0184;&#x0187;&#x018B;&#x0191;&#x0198;&#x01A0;&#x01A2;',
		'&#x01A4;&#x01A7;&#x01AC;&#x01AF;&#x01B3;&#x01B5;&#x01B8;&#x01BC;',
		'&#x01CD;&#x01CF;&#x01D1;&#x01D3;&#x01D5;&#x01D7;&#x01D9;&#x01DB;',
		'&#x01DE;&#x01E0;&#x01E2;&#x01E4;&#x01E6;&#x01E8;&#x01EA;&#x01EC;&#x01EE;',
		'&#x01F4;&#x01F8;&#x01FA;&#x01FC;&#x01FE;',
		'&#x0200;&#x0202;&#x0204;&#x0206;&#x0208;&#x020A;&#x020C;&#x020E;',
		'&#x0210;&#x0212;&#x0214;&#x0216;&#x0218;&#x021A;&#x021C;&#x021E;',
		'&#x0222;&#x0224;&#x0226;&#x0228;&#x022A;&#x022C;&#x022E;&#x0230;&#x0232;',
		'&#x23B;',
		'&#x18f;&#x1f6;&#x220;&#x1F7;',
		'&#x241;'
		)"/>
	<xsl:param name="uncasedExtendedB" select="concat(
		'&#x180;&#x181;&#x186;&#x189;&#x18A;&#x18D;&#x18E;',
		'&#x190;&#x193;&#x194;&#x196;&#x197;',
		'&#x19A;&#x19B;&#x19C;&#x19D;&#x19F;',
		'&#x1A6;&#x1A9;&#x1AA;&#x1AB;&#x1AE;',
		'&#x1B1;&#x1B2;&#x1B7;&#x1BA;&#x1BB;&#x1BE;',
		'&#x1C0;&#x1C1;&#x1C2;&#x1C3;',
		'&#x1F0;',
		'&#x221;',
		'&#x234;&#x235;&#x236;&#x237;',
		'&#x238;&#x239;&#x23A;&#x23D;&#x23E;&#x23F;',
		'&#x240;'
		)"/>
	
	
	<!-- Latin Extended Additional:  0x1E00-0x1EFF (alpha only) -->
	<xsl:param name="lowerExtendedAdditional" select="concat(
		'&#x1E01;&#x1E03;&#x1E05;&#x1E07;&#x1E09;&#x1E0B;&#x1E0D;&#x1E0F;',
		'&#x1E11;&#x1E13;&#x1E15;&#x1E17;&#x1E19;&#x1E1B;&#x1E1D;&#x1E1F;',
		'&#x1E21;&#x1E23;&#x1E25;&#x1E27;&#x1E29;&#x1E2B;&#x1E2D;&#x1E2F;',
		'&#x1E31;&#x1E33;&#x1E35;&#x1E37;&#x1E39;&#x1E3B;&#x1E3D;&#x1E3F;',
		'&#x1E41;&#x1E43;&#x1E45;&#x1E47;&#x1E49;&#x1E4B;&#x1E4D;&#x1E4F;',
		'&#x1E51;&#x1E53;&#x1E55;&#x1E57;&#x1E59;&#x1E5B;&#x1E5D;&#x1E5F;',
		'&#x1E61;&#x1E63;&#x1E65;&#x1E67;&#x1E69;&#x1E6B;&#x1E6D;&#x1E6F;',
		'&#x1E71;&#x1E73;&#x1E75;&#x1E77;&#x1E79;&#x1E7B;&#x1E7D;&#x1E7F;',
		'&#x1E81;&#x1E83;&#x1E85;&#x1E87;&#x1E89;&#x1E8B;&#x1E8D;&#x1E8F;',
		'&#x1E91;&#x1E93;&#x1E95;',
		'&#x1EA1;&#x1EA3;&#x1EA5;&#x1EA7;&#x1EA9;&#x1EAB;&#x1EAD;&#x1EAF;',
		'&#x1EB1;&#x1EB3;&#x1EB5;&#x1EB7;&#x1EB9;&#x1EBB;&#x1EBD;&#x1EBF;',
		'&#x1EC1;&#x1EC3;&#x1EC5;&#x1EC7;&#x1EC9;&#x1ECB;&#x1ECD;&#x1ECF;',
		'&#x1ED1;&#x1ED3;&#x1ED5;&#x1ED7;&#x1ED9;&#x1EDB;&#x1EDD;&#x1EDF;',
		'&#x1EE1;&#x1EE3;&#x1EE5;&#x1EE7;&#x1EE9;&#x1EEB;&#x1EED;&#x1EEF;',
		'&#x1EF1;&#x1EF3;&#x1EF5;&#x1EF7;&#x1EF9;'
		)"/>
	<xsl:param name="upperExtendedAdditional" select="concat(
		'&#x1E00;&#x1E02;&#x1E04;&#x1E06;&#x1E08;&#x1E0A;&#x1E0C;&#x1E0E;',
		'&#x1E10;&#x1E12;&#x1E14;&#x1E16;&#x1E18;&#x1E1A;&#x1E1C;&#x1E1E;',
		'&#x1E20;&#x1E22;&#x1E24;&#x1E26;&#x1E28;&#x1E2A;&#x1E2C;&#x1E2E;',
		'&#x1E30;&#x1E32;&#x1E34;&#x1E36;&#x1E38;&#x1E3A;&#x1E3C;&#x1E3E;',
		'&#x1E40;&#x1E42;&#x1E44;&#x1E46;&#x1E48;&#x1E4A;&#x1E4C;&#x1E4E;',
		'&#x1E50;&#x1E52;&#x1E54;&#x1E56;&#x1E58;&#x1E5A;&#x1E5C;&#x1E5E;',
		'&#x1E60;&#x1E62;&#x1E64;&#x1E66;&#x1E68;&#x1E6A;&#x1E6C;&#x1E6E;',
		'&#x1E70;&#x1E72;&#x1E74;&#x1E76;&#x1E78;&#x1E7A;&#x1E7C;&#x1E7E;',
		'&#x1E80;&#x1E82;&#x1E84;&#x1E86;&#x1E88;&#x1E8A;&#x1E8C;&#x1E8E;',
		'&#x1E90;&#x1E92;&#x1E94;',
		'&#x1EA0;&#x1EA2;&#x1EA4;&#x1EA6;&#x1EA8;&#x1EAA;&#x1EAC;&#x1EAE;',
		'&#x1EB0;&#x1EB2;&#x1EB4;&#x1EB6;&#x1EB8;&#x1EBA;&#x1EBC;&#x1EBE;',
		'&#x1EC0;&#x1EC2;&#x1EC4;&#x1EC6;&#x1EC8;&#x1ECA;&#x1ECC;&#x1ECE;',
		'&#x1ED0;&#x1ED2;&#x1ED4;&#x1ED6;&#x1ED8;&#x1EDA;&#x1EDC;&#x1EDE;',
		'&#x1EE0;&#x1EE2;&#x1EE4;&#x1EE6;&#x1EE8;&#x1EEA;&#x1EEC;&#x1EEE;',
		'&#x1EF0;&#x1EF2;&#x1EF4;&#x1EF6;&#x1EF8;'
		)"/>
	<xsl:param name="uncasedExtendedAdditional" select="
		'&#x1E97;&#x1E99;&#x1E9B;&#x1E96;&#x1E98;&#x1E9A;'"/>
	
	
	<!-- Latin Extended: Assemble everything we have so far -->
	<xsl:param name="lowerLatin" select="concat(
		$lowerLat1,$lowerExtendedA,$lowerExtendedB,$lowerExtendedAdditional,
		$titleLower)"/>
	<xsl:param name="upperLatin" select="concat(
		$upperLat1,$upperExtendedA,$upperExtendedB,$upperExtendedAdditional,
		$titleUpper)"/>
	<xsl:param name="digitLatin"        select="$digitLat1"/>
	<xsl:param name="alphaLatin" select="concat($lowerLatin,$upperLatin,
		$uncasedLat1,$uncasedExtendedA,$uncasedExtendedB,
		$uncasedExtendedAdditional,$titleMixed)"/>
	<xsl:param name="alnumLatin" select="concat($alphaLatin,$digitLatin)"/>
	
	
	<!-- this should be the complete latin set with teh ascii values removed -->
	<xsl:param name="highLatin" select="translate($alnumLatin,$alnum,'')"/>
	
	
	<xsl:param name="spaceLatin"        select="$spaceLat1"/>
	<xsl:param name="nonnamepunctLatin" select="$nonnamepunctLat1"/>
	<xsl:param name="punctLatin"        select="$punctLat1"/>
	<xsl:param name="nonnameLatin"
		select="concat($nonnamepunctLatin, $spaceLatin)"/>
	
	<!-- ==================================================================== -->
	
	<!--Removes letters from a string -->
	<xsl:template name="remlett">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str, $alphaLatin, '')"/>
	</xsl:template>    
	
	<!--Removes letters and punctuation from a string -->
	<xsl:template name="just-digits">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str, concat($alphaLatin,$punctLatin,' '), '')"/>
	</xsl:template>    
	
	<!--Removes numbers from a string -->
	<xsl:template name="remnum">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str, $digitLatin, '')"/>
	</xsl:template>    
	
	<!--Removes periods from a string -->
	<xsl:template name="nodot">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,'.','')"/>
	</xsl:template>    
	
	<!--Removes periods and spaces from a string -->
	<xsl:template name="nodot-nospace">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,'. ','')"/>
	</xsl:template>    
	
	<!--Removes commas from a string -->
	<xsl:template name="nocomma">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,',','')"/>
	</xsl:template>    
	
	<!--Removes periods and commas from a string (and normalize) -->
	<xsl:template name="nocomma-nodot">
		<xsl:param name="str"/>
		<xsl:value-of select="normalize-space(translate($str,',.','  '))"/>
	</xsl:template>    
	
	<!-- Removes basic punctuation from a string, except quotes. -->
	<xsl:template name="nopunct">
		<xsl:param name="str"/>
		<!--<xsl:value-of select="translate($str,'-,.()[]{};:','')"/> -->
		<xsl:value-of select="translate($str,$punctLatin,'')"/>
	</xsl:template>   
	
	<!--Removes punctuation from a string, but keeps "-" -->
	<xsl:function name="pmc:voliss-nopunct">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,',.()[]{};:','')"/>
	</xsl:function> 
	
	<!--Removes | from a string -->
	<xsl:template name="strip-pipe">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,'|','')"/>
	</xsl:template>    
	
	<!--Removes accented chars from a string -->
	<xsl:template name="remaccent">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str,$highLatin,'')"/>
	</xsl:template>
	
   <!-- ==================================================================== -->
    <!-- TEMPLATE:    title-case-string
        NOTES:       Converts a phrase to title case. 
        CALLING SEQ: Called from title-case when we're down to a text-node
                     (thus with no child nodes to worry about).
        ALGORITHM:
           when str contains mdash as x2014 unescaped, or d8212 escaped
              title-case-string before
              output the mdash as x2014 unescaped
              title-case-string after
           when str contains ndash as x2013 unescaped (only)
              title-case-string before
              output the ndash as x2013 unescaped
              title-case-string after
       when str contains / or x2f
            same deal, split it there
           when str contains colon space
              same deal, split it there
           when str contains space
              title-case-word before
              generate space
              title-case-string after
           when str contains underscore
              generate string unchanged
              (since space was checked first, this only does single 'words')
           otherwise
              title-case-word the whole thing

        IMPROVEMENT:
           Add a template splitAt(delim, case-before, case-after),
              where params 2 and 3 choose uc, lc, or tc.
     -->
    <!-- ==================================================================== -->
    <xsl:template name="title-case-string">
        <xsl:param name="str"/>              <!-- string to operate on -->
        <xsl:param name="mode"/>             <!-- only passed down -->
        <xsl:param name="first" select="1"/> <!-- only passed down -->
		  <xsl:variable name="modstring">
		  	<xsl:call-template name="repl-lb">
				<xsl:with-param name="str" select="$str"/>
				</xsl:call-template>
        	</xsl:variable>
			
		<xsl:variable name="Amodifiers" select="'|HEPATITIS|INFLUENZA|TYPE|VITAMIN|POLYMER|POLYMERS|'"/>	
		
        <xsl:if test="$modstring">
            <xsl:choose>
                <!-- Case 1: String has an  emdash character -->
                <xsl:when test="contains($modstring, '&#x02014;')">
                    <!-- Title case the string before the emdash -->
                    <xsl:call-template name='title-case-string'>
                        <xsl:with-param name="str" select="normalize-space(
                            substring-before($modstring, '&#x02014;'))"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    
                    <!-- Output the emdash -->
                    <xsl:text>&#x02014;</xsl:text>
                    
                    <!-- Recurse on the remainder of string after endash -->
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                            substring-after($modstring,'&#x02014;'))"/>
                        <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                            remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>
                
                <xsl:when test="contains($modstring, '&#x2014;')">
                    <!-- Title case the string before the emdash -->
                    <xsl:call-template name='title-case-string'>
                        <xsl:with-param name="str" select="normalize-space(
                            substring-before($modstring, '&#x2014;'))"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    
                    <!-- Output the emdash -->
                    <xsl:text>&#x02014;</xsl:text>
                    
                    <!-- Recurse on the remainder of string after endash -->
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                            substring-after($modstring,'&#x2014;'))"/>
                        <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                            remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>

                <!-- Case 1.5: String has an escaped emdash -->
                <xsl:when test="contains($modstring, '_ENTITYSTART_#8212_ENTITYEND_')">
                   <!-- Title case the string before the emdash -->
                   <xsl:call-template name='title-case-string'>
                      <xsl:with-param name="str" select="normalize-space(
                      substring-before($modstring, '_ENTITYSTART_#8212_ENTITYEND_'))"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                   </xsl:call-template>
                   
                   <!-- Output the emdash -->
                   <xsl:text>&#x02014;</xsl:text>
                   
                   <!-- Recurse on the remainder of string after endash -->
                   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                        substring-after($modstring,'_ENTITYSTART_#8212_ENTITYEND_'))"/>
                      <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                        remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>

              <!-- Case 1.6: String has a hyphen character -->
                <xsl:when test="contains($modstring, '_ENTITYSTART_#8208_ENTITYEND_')">
                   <!-- Title case the string before the hyphen -->
                   <xsl:call-template name='title-case-string'>
                      <xsl:with-param name="str" select="normalize-space(
                      substring-before($modstring, '_ENTITYSTART_#8208_ENTITYEND_'))"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                   </xsl:call-template>
                   
                   <!-- Output hyphen -->
                   <xsl:text>-</xsl:text>
                   
                   <!-- Recurse on the remainder of string after hyphen -->
                   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                        substring-after($modstring,'_ENTITYSTART_#8208_ENTITYEND_'))"/>
                      <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                        remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Case 2: String has an endash character -->
                <xsl:when test="contains($modstring, '&#x02013;')">
                   <!-- Title case the string before the endash -->
                   <xsl:call-template name='title-case-string'>
                      <xsl:with-param name="str" select="normalize-space(
                      substring-before($modstring, '&#x02013;'))"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                   </xsl:call-template>
                   
                   <!-- Output the endash -->
                   <xsl:text>&#x02013;</xsl:text>
                   
                   <!-- Recurse on the remainder of string after endash -->
                   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                        substring-after($modstring,'&#x02013;'))"/>
                      <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                        remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>

                <!-- Case 2.5: Has escaped emdash -->
                <xsl:when test="contains($modstring, '_ENTITYSTART_#8211_ENTITYEND_')">
                    <!-- Title case the string before the emdash -->
                    <xsl:call-template name='title-case-string'>
                        <xsl:with-param name="str" select="normalize-space(
                        substring-before($modstring, '_ENTITYSTART_#8211_ENTITYEND_'))"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    
                    <!-- Output the emdash -->
                    <xsl:text>&#x02013;</xsl:text>
                    
                    <!-- Recurse on the remainder of string after endash -->
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                        substring-after($modstring,'_ENTITYSTART_#8211_ENTITYEND_'))"/>
                        <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                             remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Case 2.75: Has escaped solidus -->
                <xsl:when test="contains($modstring, '_ENTITYSTART_#47_ENTITYEND_')">
                    <!-- Title case the string before the emdash -->
                    <xsl:call-template name='title-case-string'>
                        <xsl:with-param name="str" select="normalize-space(
                            substring-before($modstring, '_ENTITYSTART_#47_ENTITYEND_'))"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    
                    <!-- Output the emdash -->
                    <xsl:text>&#x02f;</xsl:text>
                    
                    <!-- Recurse on the remainder of string after endash -->
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="normalize-space(
                            substring-after($modstring,'_ENTITYSTART_#47_ENTITYEND_'))"/>
                        <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                            remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>
                
                
                <!-- Case 3: String contains hypen with number on each side -->
                <!-- (so change hyphen to endash. -->
                <xsl:when test="contains($modstring, '-') and
                                number(substring-before($modstring,'-')) and
                                number(substring-after($modstring,'-'))">
                    <xsl:value-of select="substring-before($modstring,'-')"/>
                    <xsl:text>&#x2013;</xsl:text>
                    <xsl:value-of select="substring-after($modstring,'-')"/>
                </xsl:when>
                
                
                <!-- Case 4: String has parens -->
                <xsl:when test="contains($modstring, '(')">
                   <!-- Title case the string before the parenthesis -->
                   <xsl:call-template name='title-case-string'>
                      <xsl:with-param name="str"
                          select="substring-before($modstring, '(')"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                   </xsl:call-template>
                   
                   <!-- Output the open parenthesis -->
                   <xsl:text>(</xsl:text>
                   
                   <!-- Recurse on the remainder of string after parenthesis -->
                   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,'(')"/>
                      <xsl:with-param name="mode" select="$mode"/>
                        <!-- Note: no value passed for 'first' parameter; treat
                         remainder of string as if it were a new title -->
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Case 5: String has a colon -->
                <xsl:when test="contains($modstring, ': ')">
                   <!-- Title case the word before the colon -->
                   <xsl:call-template name='title-case-string'>
                      <xsl:with-param name="str" select="substring-before($modstring, ': ')"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                   </xsl:call-template>
                   
                   <!-- Output the colon space -->
                   <xsl:text>: </xsl:text>
                   
                   <!-- Recurse on the remainder of string after colon -->
                   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,': ')"/>
                      <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Case 6: String starts with double quote. Title case after quote. -->
                <xsl:when test="starts-with($modstring,'&#x0201C;')">
                    <xsl:text>&#x201c;</xsl:text>
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,'&#x201c;')"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Escaped double quote -->
                <xsl:when test="starts-with($modstring,'_ENTITYSTART_#8220')">
                    <xsl:text>_ENTITYSTART_#8220_ENTITYEND_</xsl:text>
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,'_ENTITYSTART_#8220_ENTITYEND_')"/>
                        <xsl:with-param name="first" select="$first"/>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:when>
                
                <!-- Case 6.5: String has " A " -->
                <xsl:when test="substring-before(substring-after($modstring,' '),' ')='A' or substring-after($modstring,' ')='A'">
					 	<xsl:choose>
							<xsl:when test="contains($Amodifiers,substring-before($modstring,' '))">
							<xsl:call-template name="title-case-word">
                        <xsl:with-param name="str" select="substring-before($modstring,' ')"/>
                        <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    	</xsl:call-template>
                    <xsl:text> A </xsl:text>
						   <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,' A ')"/>
                        <xsl:with-param name="first" select="0"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
								</xsl:when>
							<xsl:otherwise>
                   <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str" select="substring-before($modstring,' ')"/>
                        <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    <xsl:text> </xsl:text>
                     <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,' ')"/>
                        <xsl:with-param name="first" select="0"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
							
								</xsl:otherwise>
							</xsl:choose>
					 
                 </xsl:when>
					 
					 
               <!-- Case 7: String has space(s) -->
                <xsl:when test="contains($modstring,' ')">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str" select="substring-before($modstring,' ')"/>
                        <xsl:with-param name="first" select="$first"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    <xsl:text> </xsl:text>
                    
                    <xsl:call-template name="title-case-string">
                        <xsl:with-param name="str" select="substring-after($modstring,' ')"/>
                        <xsl:with-param name="first" select="0"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:when>
            
                <!-- Case 8: No spaces, mdashes or colons, but has underscore -->
                <!-- Treat this as an unchangeable string and end recursion. -->
                <xsl:when test="contains($modstring, '_')">
                    <xsl:value-of select="$modstring"/>
                </xsl:when>
                
                <!-- Base case: no more spaces or emdashes (is a single word) -->
                <xsl:otherwise>
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str" select="$modstring"/>
                      <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
            </xsl:if>
        </xsl:template> <!-- title-case-string -->


    <!-- ==================================================================== -->
    <!-- TEMPLATE: title-case-word
         NOTES:    Converts a word to title case.
         PARAMS:   str:  word to work on, all in caps with "|" on each end.
                   mode:
                      'jtitle'
                      'name'
                   first:
                      1
                      otherwise
         ALGORITHM:
           when 1st char is punct
              return 1st char
              recurse on rest of string
           when word has hyphen or slash or colon
              title-case-word string preceding the char
              re-generate the char
              title-case-word string after the char
           when word has apostrophe
              title-case-word string preceding the apostrophe
              if length after apostrophe <= 1 then
                 lowercase string after apostrophe
              else
                 title-case-word string after apostrophe
              endif
           when word is in keepasis list
              generate word as is
           when word is in noknockdown list
              generate word in all caps 
           when word is in nocap list AND mode is 'name'
              generate word in all lowercase
           otherwise
              generate word via title-case-word-guts (initial cap, rest lower)
     -->
    <!-- ==================================================================== -->
    <xsl:template name="title-case-word">
        <xsl:param name="str"/>    <!-- word to title-case -->
        <xsl:param name="mode"/>   <!-- values: jtitle, name -->
        <xsl:param name="first"/>  <!-- values: 1 vs. anything else -->

        <!-- Capitalize str and add | on each end. BUT this is only needed in a couple places.
             Could skip this entirely except when need || for lookup in $keepasis-words early,
             $noknockdown-words and $nocap-words near end. 
        -->     
        <xsl:variable name="STR">
            <xsl:text>|</xsl:text>
        	<xsl:value-of select="upper-case($str)"/>
            <xsl:text>|</xsl:text>
        </xsl:variable>

        <xsl:variable name="punctuation">
            <xsl:text>({['&#x22;/:</xsl:text>
        </xsl:variable>

        <xsl:variable name="apos">
            <xsl:text>'</xsl:text>
            </xsl:variable>

        <xsl:variable name="nocap-words">
<!-- 6/14/06: Laura K removed BEYOND| because the only occurrence thus far has been in "BPH and Beyond" where it needs to be capped 
    3/1/07: Laura K removed ANTI| "anti- and Pro-Nociceptive Mechanisms" and others-->
            <xsl:text>|A|AN|THE|AND|BUT|OR|NOR|FOR|ABOARD|ABOUT|ABOVE|ACROSS|AFTER|AGAINST|ALONG|AMID|AMONG|AROUND|AS|AT|BEFORE|BEHIND|BELOW|BENEATH|BESIDE|BESIDES|BETWEEN|BMJUPDATES+|BOMBASTUS|BUT|BY|CONCERNING|CONSIDERING|DESPITE|DOWN|DU|DURING|ET|EXCEPT|EXCEPTING|EXCLUDING|FOLLOWING|FOR|FROM|IN|INTO|LIKE|MDX|MINUS|NEAR|OF|OFF|ON|ONTO|OPPOSITE|OUTSIDE|OVER|PER|PLUS|REGARDING|SAVE|SINCE|SO|THAN|THEOPHRASTUS|THROUGH|TO|TOWARD|TOWARDS|UNDER|UNDERNEATH|UNLIKE|UNTIL|UP|UPON|VERSUS|VIA|WITH|WITHIN|WITHOUT|A.M.|P.M.|bmj.com|</xsl:text>
            </xsl:variable>
        
        <xsl:variable name="cap-words">
        	<xsl:text>|I|I.|II|II.|III|III.|IV|IV.|V|V.|VI|VI.|VII|VII.|VIII|VIII.|IX|IX.|X|X.|XI|XI.|XII|XII.|XIII|XIII.|XIV|XIV.|XV|XV.|XVI|XVI.|XVII|XVII.|XVIII|XVIII.|XIV|XIV.|XX|XX.|AACP|ABC|ABN|ABRF|ACC|ACMG|ACMI|ACPE|ACTH|AD|ADA|AGM|A.G.M|AHCPR|AHR|AHRQ|AIACEMPV|AIDS|AJHG|AJP|AMACR|AMD|AMIA|AMMI|ANCA|APC|APE|APHA|APOE|APP|ASA|ASB|ASCO|ASH|ASHG|ASIP|AUA|AUQ|B.A.C.R.|B.J.|BACE|BAEM|BASEM|BASHH|BASL|BCS|BIOP|BJ|BLF|BMA|BMC|BMJ|BMJPG|BMT|BJ|BJO|BMP|BNPA|BOA|BPH|BTS|BSE|BSG|CAM|CAMM|CAMSI|CAVEPM|CCAR|CCDR|CCN|CCNP|CCPA|CD|CD-ROM|CEHJ|CF|CIDS|CIHR|CJD|CJCM|C.M.A.|CMA|CMAJ|CMJ|C.M.E.|CME|CMV|CNS|CORR|COPD|COX|CPC|CPOE|CRH|CRSN|CSLSR|CUA|CVMA|D.L.N.|DLN|DNA|DVD|EBM|EBN|EBV|ECG|EEG|ELC|EMBO|EM|EMT|ENS|ENSEMBL|ENT|EP|EPP|EQA|ER|ERK|ERS|EULAR|FAMRI|FAS|FH|FJ|FL|GCTAB|GI|GIS|GMA|GMJ|GORD|GP|GRH|GSA|GSH|HCV|HD|HGF|HIV|HMORN|HNSCC|HPB|HPV|HRSA|HSR|HSS|HSV|HUPO|IAIMS|IBD|IBS|IDE|IEFS|IFDAS|IFN|IJD|IL|IL12|ILC|INSERM|IQ|IQSY|IRB|ISA|ISCAIP|ISCB|ISEV|ISES|ISVH|IUNS|IUPHAR|JAACT|JABA|JACCT|JBO|JCB|JCCA|JCO|JCP|JDSA|JEAB|JECH|JCV|JMG|JRSM|KIT|LBCL|LCDC|LCM|LDLLHRH|LUTS|MAPK|MAPKK|M.D.|MIRP|MITF|MJM|MLA|MM|MMC|MP|MRMPI|MSSVD|MTAS|MTC|N.A.S.|NAC|NAS|NC|NCD|NCHS|NCI|NCT|NEIHS|NETT|NHS|NICB|NICE|NIDRR|NIEHS|NIH|NLM|NPC|NS|NTD|OPG|OTC|OVA|O.V.C.|PAI|PARC|PBRNS|PDAPP|PDGF|PDZ|PEDNET|PHS|PKB|PNAS|PNG|POMC|PPAR|PS|PTC|PTH|PTSD|QUA|QUERI|R.A.F.|RAC|RCPCH|RFLP|RGTA|RK|RNA|ROM|ROP|RPE|RS|RSM|SAEM|SARS|SCC|SCI|SGIM|SHIV|SICS|SIDS|SIV|SNS|SOCS|SUI|SSE|SSO|STD|TF|TGF|THI|TLC|TLR9|TM|TNAP|TNF|TRH|TRP|TRS|TSH|TTR|TV|TWIB|UCH|U.K.|U.S.|UK|UPR|US|UV|V.D.|VA|VADD|VCD|VEGF|VSV|W.D.M.|WCHD|WGA|WHA|WHO|WNT|WPA|YNT|</xsl:text>
            <!-- LK 2/22/06: POEM removed from cap-words list;
            LK 9/15/06: AN removed from cap-words list -->
            </xsl:variable>
        <xsl:variable name="jtitle-cap-words">
            <xsl:text>ACS|TAG.|D.C.|JPEN|SAR|QSAR|</xsl:text>
            </xsl:variable>

        <xsl:variable name="noknockdown-words">
            <xsl:choose>
                <xsl:when test="$mode='jtitle'">
                    <xsl:value-of select="concat($cap-words, $jtitle-cap-words)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$cap-words"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="keepasis-word">
            <xsl:call-template name="keepasis-words">
                <xsl:with-param name="str" select="$STR"/>
            </xsl:call-template>
        </xsl:variable>
            
			<xsl:if test="$str">
				<xsl:choose>

					<xsl:when test="contains($STR,')')">
						<xsl:call-template name="title-case-word">
							<xsl:with-param name="str">
								<xsl:call-template name="strip-pipe">
									<xsl:with-param name="str" select="translate(substring-after($STR,substring($STR,1,1)), ')', '')"/>
								</xsl:call-template>
							</xsl:with-param>
							<xsl:with-param name="mode" select="$mode"/>
						</xsl:call-template>
						<xsl:text>)</xsl:text>
					</xsl:when>

					<xsl:when test="contains($STR,'}')">
						<xsl:call-template name="title-case-word">
							<xsl:with-param name="str">
								<xsl:call-template name="strip-pipe">
									<xsl:with-param name="str" select="translate(substring-after($STR,substring($STR,1,1)), '}', '')"/>
								</xsl:call-template>
							</xsl:with-param>
							<xsl:with-param name="mode" select="$mode"/>
						</xsl:call-template>
						<xsl:text>}</xsl:text>
					</xsl:when>

					<xsl:when test="contains($STR,']')">
						<xsl:call-template name="title-case-word">
							<xsl:with-param name="str">
								<xsl:call-template name="strip-pipe">
									<xsl:with-param name="str" select="translate(substring-after($STR,substring($STR,1,1)), ']', '')"/>
								</xsl:call-template>
							</xsl:with-param>
							<xsl:with-param name="mode" select="$mode"/>
						</xsl:call-template>
						<xsl:text>]</xsl:text>
					</xsl:when>

                <xsl:when test="contains($punctuation,substring($STR,2,1))">
                    <xsl:value-of select="substring($STR,2,1)"/>
                   <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select=
                                "substring-after($STR,substring($STR,2,1))"/>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                </xsl:when>

                <xsl:when test="contains($STR,'-')">
                    <xsl:choose>
                        <xsl:when test="$keepasis-word!=''">
                            <xsl:value-of select="$keepasis-word"/>
                        </xsl:when>
                        <xsl:otherwise>
                        <xsl:call-template name="title-case-word">
                            <xsl:with-param name="str">
                                <xsl:call-template name="strip-pipe">
                                    <xsl:with-param name="str" select="substring-before($STR,'-')"/>
                                    </xsl:call-template>
                                </xsl:with-param>
                            <xsl:with-param name="mode" select="$mode"/>
                            </xsl:call-template>
                        <xsl:text>-</xsl:text>
                        <xsl:call-template name="title-case-word">
                            <xsl:with-param name="str">
                                <xsl:call-template name="strip-pipe">
                                    <xsl:with-param name="str" select="substring-after($STR,'-')"/>
                                    </xsl:call-template>
                                </xsl:with-param>
                            <xsl:with-param name="mode" select="$mode"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="contains($STR,'/')">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-before($STR,'/')"/>
                                </xsl:call-template>
                            </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                        </xsl:call-template>
                    <xsl:text>/</xsl:text>
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-after($STR,'/')"/>
                                </xsl:call-template>
                            </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                        </xsl:call-template>
                </xsl:when>

                <xsl:when test="contains($STR,':')">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-before($STR,':')"/>
                                </xsl:call-template>
                            </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                        </xsl:call-template>
                    <xsl:text>:</xsl:text>
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-after($STR,':')"/>
                                </xsl:call-template>
                            </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                        </xsl:call-template>
                </xsl:when>

                <xsl:when test="contains($STR,$apos)">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-before($STR,$apos)"/>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    <xsl:text>'</xsl:text>
                    <xsl:choose>
                        <xsl:when test="string-length(substring-before(substring-after($STR,$apos),'|')) &lt;=1">
                        	<xsl:value-of select="lower-case(substring-before(substring-after($STR,$apos),'|'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="title-case-word">
                                <xsl:with-param name="str">
                                    <xsl:call-template name="strip-pipe">
                                        <xsl:with-param name="str" select="substring-after($STR,$apos)"/>
                                    </xsl:call-template>
                                </xsl:with-param>
                                <xsl:with-param name="mode" select="$mode"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="contains($STR,'&#x02019;')">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-before($STR,'&#x02019;')"/>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    <xsl:text>'</xsl:text>
                    <xsl:choose>
                        <xsl:when test="string-length(substring-before(substring-after($STR,'&#x02019;'),'|')) &lt;=1">
                            <xsl:value-of select="lower-case(substring-before(substring-after($STR,'&#x02019;'),'|'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="title-case-word">
                                <xsl:with-param name="str">
                                    <xsl:call-template name="strip-pipe">
                                        <xsl:with-param name="str" select="substring-after($STR,'&#x02019;')"/>
                                    </xsl:call-template>
                                </xsl:with-param>
                                <xsl:with-param name="mode" select="$mode"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="contains($STR,'_ENTITYSTART_#8217_ENTITYEND_')">
                    <xsl:call-template name="title-case-word">
                        <xsl:with-param name="str">
                            <xsl:call-template name="strip-pipe">
                                <xsl:with-param name="str" select="substring-before($STR,'_ENTITYSTART_#8217_ENTITYEND_')"/>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="mode" select="$mode"/>
                    </xsl:call-template>
                    <xsl:text>'</xsl:text>
                    <xsl:choose>
                        <xsl:when test="string-length(substring-before(substring-after($STR,'_ENTITYSTART_#8217_ENTITYEND_'),'|')) &lt;=1">
                            <xsl:value-of select="lower-case(substring-before(substring-after($STR,'_ENTITYSTART_#8217_ENTITYEND_'),'|'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="title-case-word">
                                <xsl:with-param name="str">
                                    <xsl:call-template name="strip-pipe">
                                        <xsl:with-param name="str" select="substring-after($STR,'_ENTITYSTART_#8217_ENTITYEND_')"/>
                                    </xsl:call-template>
                                </xsl:with-param>
                                <xsl:with-param name="mode" select="$mode"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="$keepasis-word!=''">
                    <xsl:value-of select="$keepasis-word"/>
                </xsl:when>             

                <xsl:when test="contains($noknockdown-words,$STR)">
                    <xsl:call-template name="strip-pipe">
                        <xsl:with-param name="str" select="$STR"/>
                    </xsl:call-template>
                </xsl:when>             

					<xsl:when test="matches($str,'H[0-9]N[0-9]')">
						<xsl:value-of select="$str"/>
						</xsl:when> 						
						
               <xsl:when test="string($first)='1'">
                    <xsl:call-template name="title-case-word-guts">
                        <xsl:with-param name="str" select="$str"/>
                        </xsl:call-template>
                </xsl:when>

                <xsl:when test="contains($nocap-words,$STR) and $mode != 'name'">
                		<xsl:value-of select="lower-case($str)"/>
                </xsl:when>             
					
                <xsl:otherwise>
                    <xsl:call-template name="title-case-word-guts">
                        <xsl:with-param name="str" select="$str"/>
                        </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
     </xsl:template> <!-- title-case-word -->



    <!-- ==================================================================== -->
    <!-- TEMPLATE: keepasis-words
        NOTES:    Check whether a single word has special case rules.
        PARAMS:   str:  The word, all in caps and with "|" or each end.
        RETURNS:  The correct case form if special, otherwise nil.
     -->
    <!-- ==================================================================== -->
    <xsl:template name="keepasis-words">
    	 <xsl:param name="str"/>
        <xsl:param name="jidab"/>
    	<xsl:choose>
            <xsl:when test="$str='|_ENTITYSTART_#945_ENTITYEND_B|'">_ENTITYSTART_#945_ENTITYEND_B</xsl:when>
            <xsl:when test="$str='|A|' and $jidab='amjphnation'">A</xsl:when>
            <xsl:when test="$str='|ACADEMYHEALTH|'">AcademyHealth</xsl:when>
            <xsl:when test="$str='|ANXA1|'">ANXA1</xsl:when>
            <xsl:when test="$str='|APOE-KO|'">ApoE-KO</xsl:when>
            <xsl:when test="$str='|ARK5|'">ARK5</xsl:when>
            <xsl:when test="$str='|AROM_ENTITYSTART_#43_ENTITYEND_|'">AROM_ENTITYSTART_#43_ENTITYEND_</xsl:when>
            <xsl:when test="$str='|ATPASES|'">ATPases</xsl:when>
            <xsl:when test="$str='|B.A.S.M|'">B.A.S.M.</xsl:when>
            <xsl:when test="$str='|BACKCHAT|'">BackChat</xsl:when>
            <xsl:when test="$str='|B_ENTITYSTART_#91_ENTITYEND_A_ENTITYSTART_#93_ENTITYEND_P|'">B_ENTITYSTART_#91_ENTITYEND_a_ENTITYSTART_#93_ENTITYEND_P</xsl:when>
            <xsl:when test="$str='|BM-DERIVED|'">BM-Derived</xsl:when>
            <xsl:when test="$str='|BPS|'">BPs</xsl:when>
            <xsl:when test="$str='|CAP|'">CaP</xsl:when>
            <xsl:when test="$str='|CATCH-IT|'">CATCH-IT</xsl:when>
            <xsl:when test="$str='|CCL28|'">CCL28</xsl:when>
            <xsl:when test="$str='|CD3|'">CD3</xsl:when>
            <xsl:when test="$str='|CD1D|'">CD1d</xsl:when>
            <xsl:when test="$str='|CD59A|'">CD59a</xsl:when>
            <xsl:when test="$str='|CDNA|'">cDNA</xsl:when>
            <xsl:when test="$str='|CXCR3|'">CXCR3</xsl:when>
            <xsl:when test="$str='|CYP2C23|'">CYP2C23</xsl:when>
            <xsl:when test="$str='|DC-LAMP|'">DC-LAMP</xsl:when>
            <xsl:when test="$str='|DCS|'">DCs</xsl:when>
            <xsl:when test="$str='|DNMT1|'">DNMT1</xsl:when>
            <xsl:when test="$str='|ECGS|'">ECGs</xsl:when>
            <xsl:when test="$str='|ECM,|'">ECM,</xsl:when>
            <xsl:when test="$str='|EHPNET|'">EHPnet</xsl:when>
            <xsl:when test="$str='|EJIAS|'">eJIAS</xsl:when>
            <xsl:when test="$str='|EJOURNAL|'">eJournal</xsl:when>
            <xsl:when test="$str='|ELETTER|'">eLetter</xsl:when>
            <xsl:when test="$str='|ERBB2|'">ErbB2</xsl:when>
            <xsl:when test="$str='|ESYMPOSIUM|'">eSymposium</xsl:when>
            <xsl:when test="$str='|FIZZ1|'">FIZZ1</xsl:when>
    		 <xsl:when test="$str='|FMRI|'">fMRI</xsl:when>
            <xsl:when test="$str='|GIMEDIA|'">GIMedia</xsl:when>
            <xsl:when test="$str='|GPS|' and $jidab='bmj'">GPs</xsl:when>
            <xsl:when test="$str='|HBA|'">HbA</xsl:when>
            <xsl:when test="$str='|HER2|'">HER2</xsl:when>
            <xsl:when test="$str='|I-A|'">I-A</xsl:when>
            <xsl:when test="$str='|ICAM-1|'">ICAM-1</xsl:when>
            <xsl:when test="$str='|IGE|'">IgE</xsl:when>
            <xsl:when test="$str='|IPMNS|'">IPMNs</xsl:when>
            <xsl:when test="$str='|IRBS|'">IRBs</xsl:when>
            <xsl:when test="$str='|JOURNALSCAN|'">JournalScan</xsl:when>
            <xsl:when test="$str='|K-RAS-AKT|'">K-ras-Akt</xsl:when>
            <xsl:when test="$str='|LFA-1|'">LFA-1</xsl:when>
            <xsl:when test="$str='|MACLEAN|'">MacLean</xsl:when>
            <xsl:when test="$str='|MEDGENMED|'">MedGenMed</xsl:when>
            <xsl:when test="$str='|MED.PIX|'">Med.Pix</xsl:when>
            <xsl:when test="$str='|MMPS|'">MMPs</xsl:when>
            <xsl:when test="$str='|MRNA|'">mRNA</xsl:when>
            <xsl:when test="$str='|NSAIDS|'">NSAIDs</xsl:when>
            <xsl:when test="$str='|NS2|'">NS2</xsl:when>
            <xsl:when test="$str='|NS3|'">NS3</xsl:when>
            <xsl:when test="$str='|NS5B|'">NS5B</xsl:when>
            <xsl:when test="$str='|NS4A|'">NS4A</xsl:when>
            <xsl:when test="$str='|NS5A|'">NS5A</xsl:when>
            <xsl:when test="$str='|NS4B|'">NS4B</xsl:when>
            <xsl:when test="$str='|NTDS|'">NTDs</xsl:when>
            <xsl:when test="$str='|OPG,|'">OPG,</xsl:when>
            <xsl:when test="$str='|ON TRACK|'">On TRACK</xsl:when>
            <xsl:when test="$str='|OXLDL|'">OxLDL</xsl:when>
            <xsl:when test="$str='|P16|'">p16</xsl:when>
            <xsl:when test="$str='|P38|'">p38</xsl:when>
            <xsl:when test="$str='|PCG|'">PcG</xsl:when>
            <xsl:when test="$str='|PLOS|'">PLoS</xsl:when>
            <xsl:when test="$str='|PP32|'">pp32</xsl:when>
            <xsl:when test="$str='|PRETX|'">PRETx</xsl:when>
            <xsl:when test="$str='|PTH,|'">PTH,</xsl:when>
            <xsl:when test="$str='|QNAS|'">QnAs</xsl:when>
            <xsl:when test="$str='|RAP-ARRAY|'">RAP-Array</xsl:when>
            <xsl:when test="$str='|RT-PCR|'">RT-PCR</xsl:when>
            <xsl:when test="$str='|RXR_ENTITYSTART_#945_ENTITYEND_|'">RXR_ENTITYSTART_#945_ENTITYEND_</xsl:when>
            <xsl:when test="$str='|S-IBM|'">s-IBM</xsl:when>
            <xsl:when test="$str='|S100A4|'">S100A4</xsl:when>
            <xsl:when test="$str='|SAS|'">SAs</xsl:when>
            <xsl:when test="$str='|SC.D.|'">Sc.D.</xsl:when>
            <xsl:when test="$str='|SR-BI|'">SR-BI</xsl:when>
            <xsl:when test="$str='|SSRIS|'">SSRIs</xsl:when>
            <xsl:when test="$str='|STDS|'">STDs</xsl:when>
            <xsl:when test="$str='|TLR2|'">TLR2</xsl:when>
    		<xsl:when test="$str='|TRACK|' and $jidab='annfammed'">TRACK</xsl:when>
            <xsl:when test="$str='|UP-REGULATES|'">Up-Regulates</xsl:when>
            <xsl:when test="$str='|UP-TO-DATE|'">Up-to-Date</xsl:when>
            <xsl:when test="$str='|VAL30MET|'">Val30Met</xsl:when>
            <xsl:when test="$str='|VCJD|'">vCJD</xsl:when>
            <xsl:otherwise/>
            </xsl:choose>
        </xsl:template>


    <!-- ==================================================================== -->
    <!-- TEMPLATE: title-case-word-guts
        NOTES:    Turn a single word to initial cap and rest lower case.
     -->
    <!-- ==================================================================== -->
    <xsl:template name="title-case-word-guts">
        <xsl:param name="str"/>
    	   <xsl:value-of select="upper-case(substring($str,1,1))"/>
    	   <xsl:value-of select="lower-case(substring($str,2))"/>
     </xsl:template>


    
    <!-- ==================================================================== -->
    <!-- TEMPLATE: mixed-case-text
        NOTES:    Tests string, minus first letter, for both uppercase and lowercase letters. If both
        exist in string, returns 'yes' [mixed-case=yes]. If either uppercase OR lowercase letters are 
        missing from string, returns value 'no'.
        
        Uses string param "txt". Calls find-uc and find-lc templates.
    -->
    <!-- ==================================================================== -->   
    
    
    <xsl:template name="mixed-case-test">
        <xsl:param name="txt"/>
        <xsl:variable name="has-uc">
            <xsl:call-template name="find-uc">
                <xsl:with-param name="txt" select="substring($txt,2)"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="has-lc">
            <xsl:call-template name="find-lc">
                <xsl:with-param name="txt" select="substring($txt,2)"/>
            </xsl:call-template>
        </xsl:variable>
                
        <xsl:choose>
            <xsl:when test="$has-uc='yes' and $has-lc='yes'">
                <xsl:text>yes</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>no</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="find-uc">
        <xsl:param name="txt"/>
        <xsl:variable name="uc" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
        <xsl:if test="$txt">
            <xsl:choose>
                <xsl:when test="contains($uc,substring($txt,1,1))">
                    <xsl:text>yes</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="find-uc">
                        <xsl:with-param name="txt" select="substring($txt,2)"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="find-lc">
        <xsl:param name="txt"/>
        <xsl:variable name="lc" select="'abcdefghijklmnopqrstuvwxyz'"/>
        <xsl:if test="$txt">
            <xsl:choose>
                <xsl:when test="contains($lc,substring($txt,1,1))">
                    <xsl:text>yes</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="find-lc">
                        <xsl:with-param name="txt" select="substring($txt,2)"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
    	
	<!-- Replaces line breaks in a string with spaces-->
	<xsl:template name="repl-lb">
		<xsl:param name="str"/>
		<xsl:value-of select="translate($str, 
			'&#x9;&#xA;&#xD;','        ')"/>
	</xsl:template>        
	

</xsl:stylesheet>   
