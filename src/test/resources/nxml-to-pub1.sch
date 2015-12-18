<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron'
            xmlns:sqf='http://www.schematron-quickfix.com/validator/process'>
  <sch:pattern>
    <sch:rule context='front'>
      <sch:assert test='journal-meta'>front must have journal-meta</sch:assert>
    </sch:rule>
  </sch:pattern>
</sch:schema>
