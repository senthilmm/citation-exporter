<schema xmlns='http://purl.oclc.org/dsdl/schematron'>
  <pattern>
    <rule context='pub-one-record'>
      <assert test='source-meta'/>
    </rule>
    <rule context='source-meta'>
      <assert test='object-id[@pub-id-type="nlm-ta"] = "PLoS Med"'/>
    </rule>
  </pattern>
</schema>
