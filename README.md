This is a citation exporting web service, based on the following excellent open-source
tools:

* [citeproc-java](https://github.com/michel-kraemer/citeproc-java).
* [citeproc-js](http://gsl-nagoya-u.net/http/pub/citeproc-doc.html)


## Quick start

Clone this repository:

```
git clone https://github.com/ncbi/citation-exporter.git
```

Then you'll need to download and install some dependencies. First, a
forked version of citeproc-java:

```
git clone https://github.com/Klortho/citeproc-java.git
cd citeproc-java
git checkout pmc-22661-ahead-of-print
./gradlew install
cd ..
```

Next, [kitty-cache](https://code.google.com/p/kitty-cache/),
that is not in Maven central, so you'll need to build and install that first.

```
svn checkout http://kitty-cache.googlecode.com/svn/trunk/ kitty-cache-read-only
cd kitty-cache-read-only
mvn install
cd ..
```

Next, clone a special branch of the citation-style-language/styles repo. This
must be put under the citation-exporter working directory:

```
cd citation-exporter
git clone https://github.com/Klortho/styles.git
cd styles
git checkout pmc-22661-olf
cd ../..
```

The service also depends on XSLT files that are, unfortunately, at the time 
of this writing, internal to NCBI.  Please write to maloneyc@ncbi.nlm.nih.gov 
to get a copy of these "pub-one" XSLT files.  These should then be copied 
into the citation-exporter/src/main/resources/xslt directory.

Finally, you need to download the JATS DTDs:

```
cd citation-exporter/jats
./get-dtds.sh
```

Then build and run this web service:

```
mvn jetty:run
```

Point your browser to [http://localhost:11999/samples](http://localhost:11999/samples).


### Modified instructions for ahead-of-print branch


The above forks includes changes to support:

* Multiple dates: I added the `epub-date` field
* Ahead-of-print

Then, you should be able to build and run the citation-exporter, with `mvn jetty:run`.





## Testing

Run unit tests as follows:

```
mvn test
```

### Test samples

A good set of samples is listed in the application's [samples 
page](http://www.ncbi.nlm.nih.gov/pmc/utils/ctxp/samples).


### Performance tests

Use the test script src/test/resources/performance-test.pl to test performance and reliability under
load. Use `-?` to get usage help.

On a development deployment (one that uses item_source="test") then you need 
to make sure that you only use a fixed set of IDs, that correspond to the
available test files in src/main/resources/samples.

For example, to run it against a local installation, that was started with the command
line

```
mvn jetty:run
```

You could use the following:

```
cd src/test/resources
./performance-test.pl --id 3352855
```

Because there exists the PubOne-format file src/main/resources/samples/aiid/3352855.pub1,
the service will be able to generate all of the responses in the requested formats. 


## Running as executable jar with embedded Jetty

```
mvn package
mkdir jetty-temp-dir
java -Djava.io.tmpdir=./jetty-temp-dir \
  -jar target/pmc-citation-exporter-0.1-SNAPSHOT.jar
```


## Configuration

Configuration is controlled with system properties.
Set these on the run command line, for example:

```
mvn jetty:run -Djetty.port=9876 -Dcache_ids=true -Did_cache_ttl=8
```

Here are the parameters that are defined:

* `jetty.port`
* `item_source` - string specifying which ItemSource to use.  The default is "test",
  which indicates to use the TestItemSource, which loads data items from files in the class path.
  If not "test", then the value should be the fully qualified name of the class.
  Possible values are:
    * gov.ncbi.pmc.cite.StcachePubOneItemSource - requires item_source_loc to also be set
    * gov.ncbi.pmc.cite.StcacheNxmlItemSource - requires item_source_loc to also be set
    * gov.ncbi.pmc.cite.ConvAppNxmlItemSource - Get NXML from an HTTP web service. Requires 
      item_source_loc to also be set
* `item_source_loc` - When item_source is one of the Stcache options, this needs to be the full
  pathname of the stcache image file.  When item_source is ConvAppNxmlItemSource, then this should
  be the URL of the converter app service.
* `id_converter_url` - URL of the PMC ID converter API.  Default is
  "http://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/".
* `id_converter_params` - Query string parameters to send to the the PMC ID
  converter API.  Default is "showaiid=yes&format=json&tool=ctxp&email=pubmedcentral@ncbi.nlm.nih.gov".
* `cache_ids` - either "true" or "false".  Default is "false".
* `id_cache_ttl` - time-to-live for each of the IDs in the ID cache, in seconds.
  Default is 86400.
* `xml.catalog.files` - used by the Apache commons CatalogResolver; this is the pathname
  of the OASIS catalog file to use when parsing XML files.  See below for more info.
  Default value is "catalog.xml"
* `log` - location of the log files.  Defaults to the *log* subdirectory of the directory
  from which the app is run.

### DTDs and XML catalog files

The repository comes with an OASIS catalog file, *catalog.xml* that is used, 
by default, to find DTDs. This contains:

```xml
<nextCatalog catalog="catalog-local.xml"/>
<nextCatalog catalog="jats/catalog.xml"/>
```

This causes the resolver to try to resolve IDs from:

* catalog-local.xml, if it exists. If you create this file, then you can override
  any cross-references from other catalogs.
* jats/catalog.xml, if it exists. This file is included in the repository, and 
  you can use the jats/get-dtds.sh script to download the corresponding DTDs from
  the JATS site.

If the JATS (and other) DTDs are located somewhere else on your system, then 
there are two ways to override the default behavior.

1.  Set the `xml.catalog.files` system property, to point to some other master catalog file.
    For example:

        mvn test -Dxml.catalog.files=/pmc/load/catalog/linux-oxygen-pmc3-catalog.xml

2.  Create a *catalog-local.xml* file in the root directory of the repo, and 
    override specific DTDs there.


## API

### Special URLs

The following two URLs are special:

* /samples - provides a list of links to sample outputs of various document, in various formats
* /errortest - strictly for testing, this causes the application to generate an error page

### Parameters:

* **id** or **ids** - List of IDs, comma-delimited. The types and expected 
  patterns of the values given here are the same as for
  the [PMC ID converter API](https://www.ncbi.nlm.nih.gov/pmc/tools/id-converter-api/).
  The type can either be specified explicitly with the idtype parameter, or can be inferred.
  IDs are always resolved to one of `aiid` or `pmid`.
* **idtype** - Specifies the type of the IDs given in the ids parameter.
  Any of these types is allowed:
    * pmcid - includes versioned ids
    * pmid
    * mid
    * doi
* **report** - Specifies the embedded format of the data.  Defaults to "html".
  See the table below for allowed values.
* **format** - corresponds to the returned "content-type". Can be used as 
  substitute for content negotiation.  Default depends on report; see the 
  table below.
* **style** or **styles** - CSL style name, or a list of stylenames.  If 
  just one ID is given, this can include multiple names, comma-delimited.
  If multiple IDs are given, then this must be only one style name. In 
  other words, you can have multiple IDs or multiple styles, but not both.  
  Defaults to "modern-language-association".

Value combinations of report and format are listed in the following table.

```
report    format  Comments
------    ------  --------
html      html    Styled citations in raw HTML format. Same as citeproc-node.
ris       ris     Machine-readable citation, in RIS format. Media type is application/x-research-info-systems.
nbib      nbib    Machine-readable citation, in NBIB (MEDLINE) format. Media type is application/nbib.
citeproc  json    Machine-readable citation, in citeproc-json format.
pub-one   xml     New unified literature format.
```

### Error responses

If everything goes well, the service will return a status of 200, of course. If there is a problem, one of
the following codes will be returned:

* 400 Bad request - if the request parameters can't be deciphered, or similar problems
* 404 Not found - for IDs that are of the correct form, but can't be found in the data
* 500 Internal server error - if there is problem with an upstream service, like the ID
  converter, or a runtime exception in this service's software.

### Styled citation responses

Each styled citation will be in it's own `<div>` element, which will have some special attributes to
identify it:

* **data-id** - the ID used in the request for the resource.  This will have a type prefix, followed
  by a colon, and then the ID value.  E.g. pmdi
* **data-style** - the name of the citation style
* **data-resolved-id** - optional, if the requested ID doesn't match the resolved ID.

The individual record `<div>`s will be wrapped in an outer `<div>` element.  If there were requested IDs that
couldn't be resolved, then they will be listed in the **data-not-found** attribute on this outer `<div>`.

For example, a single record styled in a single citation style (request "?ids=PMC3000436"):

```html
<div>
  <div class="csl-entry" data-id="aiid:3000436" data-style="modern-language-association">Barash,
    Uri et al. “Proteoglycans in Health and Disease: New Concepts for Heparanase Function in Tumor
    Progression and Metastasis.” <i>FEBS J</i> 277.19 (2010): n. pag.</div>
</div>
```

A response for a request for multiple IDs, one of which cannot be resolved (request
"?ids=PMC3000436,PMC99999999,PMC3155436"):

```html
<div data-not-found="pmcid:PMC99999999">
  <div class="csl-entry" data-id="pmcid:PMC3000436" data-resolved-id="aiid:3000436"
    data-style="modern-language-association">Barash, Uri et al. “Proteoglycans in Health
    and Disease: New Concepts for Heparanase Function in Tumor Progression and Metastasis.”
    <i>FEBS J</i> 277.19 (2010): n. pag.</div>
  <div class="csl-entry" data-id="pmcid:PMC3155436" data-resolved-id="aiid:3155436"
    data-style="modern-language-association">“Correction.” <i>Can Fam Physician</i> 57.8
    (2011): 879–879. Print.</div>
</div>
```


### PubOne format responses

The response for a single record in PubOne format will look something like the following
(request "?ids=PMC3000436&report=pub-one"):

```xml
<pub-one-record xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:xlink="http://www.w3.org/1999/xlink"
  record-type="article" xml:lang="en">
  <source-meta>
    <object-id pub-id-type="nlm-journal-id">101229646</object-id>
    ...
  </source-meta>
  <document-meta>
    <object-id pub-id-type="doi">10.1111/j.1742-4658.2010.07799.x</object-id>
    <object-id pub-id-type="manuscript">nihpa226684</object-id>
    <object-id pub-id-type="pmcid">3000436</object-id>
    <object-id pub-id-type="pmid">20840586</object-id>
    ...
  </document-meta>
</pub-one-record>
```

If more than one record is returned, they will be wrapped in an outer `<pub-one-records>` element,
and each identified with attributes in the namespace "http://www.ncbi.nlm.nih.gov/ns/search".
For example, a response with for two good IDs and one bad one
(request "?ids=PMC3000436,PMC99999999,PMC3155436&report=pub-one"):

```xml
<pub-one-records xmlns:s="http://www.ncbi.nlm.nih.gov/ns/search"
                 s:not-found="pmcid:PMC99999999">
  <pub-one-record s:id="pmcid:PMC3000436"
                  s:resolved-id="aiid:3000436" ...>...</pub-one-record>
  <pub-one-record s:id="pmcid:PMC3155436"
                  s:resolved-id="aiid:3155436" ...>...</pub-one-record>
</pub-one-records>
```

### citeproc-json format responses

The response for a single record in this format will be a JSON object.  For example
(request "?ids=PMC3000436&report=citeproc"):

```json
{
  "id": "aiid:3000436",
  "title": "Proteoglycans in health and disease: New concepts for heparanase function in tumor
    progression and metastasis",
  "author": [ ... ],
  ...
  "issue": "19",
  "PMID": "20840586",
  "PMCID": "3000436",
  "DOI": "10.1111/j.1742-4658.2010.07799.x",
}
```

Note that the value used for the `id` field is the resolved, canonical identifier, and that
the other forms of identifier appear in citeproc-json specific fields `PMID`, `PMCID`, etc.

If there are multiple records, they will be wrapped in a JSON array.  For example, the
response with two good records and one bad one will look like this (request
""):

```json
[
  {
    "not-found": "pmcid:PMC99999999"
  },
  {
    "id": "aiid:3000436",
    "title": "Proteoglycans in health and disease: New concepts for heparanase function in
      tumor progression and metastasis",
    ...
    "PMID": "20840586",
    "PMCID": "3000436",
    "DOI": "10.1111/j.1742-4658.2010.07799.x",
  },
  {
    "id": "aiid:3155436",
    "title": "Correction",
    ...
    "PMCID": "3155436",
  }
]
```

## Logging

The location of log files is controlled by the system parameter `log`, which is usually set to
the value "log" using `-Dlog=log` command-line switch.

Logging is controlled by properties set in the *src/main/resources/log4j.properties*
file. The log level is controlled by the `log4j.rootLogger` property, and can be set to
one of TRACE, DEBUG, INFO, WARN, ERROR or FATAL.




## Development

### Build environments

This repository has been configured such that, *by default*, it can run stand-alone, without any
dependencies on NCBI-internal libraries or services.

In the production environment, however, we require access to a Java library which has not been released
openly (groupId=gov.ncbi.pmc, artifactId=pmc-lib). References to that library exist in two class files,
StcachePubOneItemSource and
StcacheNxmlItemSource; so, *by default*, those are excluded from compilation.  This is because the default
build profile is "test", which explicitly excludes those two class files, and doesn't include the dependency
on the pmc-lib library.

Building for production is done with the use of the "prod" Maven profile (`mvn -Pprod`).
That profile doesn't exclude those class files for compilation, and does declares the dependency on the
pmc-lib library.


### Test item provider

When the value of *item_source" is "test", the citation data is mock data from the
*src/main/webapp/test* directory.

### Eclipse / Tomcat configuration

To run the application from Eclipse, right-click on the project, and select
*Run As* -> *Run on server*.  Depending on your workspace server configuration
*server.xml*, you should then be able to point your browser to
http://locahost:12006/pmc-citation-service/.




### Jetty configuration

There are two ways to run the server under Jetty.  Running with `mvn jetty:run` is used during development, and is quicker.
For production, however, the application should be packaged as an "uber-jar", using the
[Apache Maven Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/), and then run as an executable jar file (see
the next section for more about that).

Jetty is configured by the following code in the *pom.xml* file:

```xml
...
<properties>
  ...
  <jettyVersion>9.2.1.v20140609</jettyVersion>
</properties>
...
<dependencies>
  ...
  <dependency>
    <groupId>org.eclipse.jetty.orbit</groupId>
    <artifactId>javax.servlet</artifactId>
    <version>3.0.0.v201112011016</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
...
<build>
  <plugins>
    ...
    <plugin>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-maven-plugin</artifactId>
      <version>${jettyVersion}</version>
    </plugin>
  </plugins>
</build>
```

### Jetty shaded uber-jar

This is used when you run the `mvn package` command, and causes the creation of an executable jar
file in the target subdirectory (currently target/pmc-citation-exporter-0.1-SNAPSHOT.jar), that includes
all of the dependencies, including Jetty itself.

This is controlled by the [Apache Maven Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/),
which is configured by a \<plugin> section of the pom.xml.

In addition, another plugin section, build-helper-maven-plugin, is required to specify additional directories to copy
into the target jar file.

To run the server from this executable jar, execute something like this

    java -Djava.io.tmpdir=./jetty-temp-dir -jar target/pmc-citation-exporter-0.1-SNAPSHOT.jar

Note that system properties must be set on the command line *before* the `-jar` option.

When running this way, Jetty is configured by the src/main/webapp/jetty.xml file. (Note that this is
*not used* when running with `mvn jetty:run`).

### citeproc-java

* [Javadocs](http://michel-kraemer.github.io/citeproc-java/api/latest/)
* [GitHub/michel-kraemer/citeproc-java]()

To use the latest development version of this library, rather than the release package, first clone the GitHub
repository to any local directory.  Then, see the [build
instructions](http://michel-kraemer.github.io/citeproc-java/using/building/).  Do the following to create
the jar file, and then install that in your local Maven repository:

```
./gradlew install
```

Note that this installs the library as version "0.7-SNAPSHOT", meaning that it is later than 0.6, but not a
stable version.

Next, change the pom.xml file in *this* repository to require that latest version:

```xml
<dependency>
  <groupId>de.undercouch</groupId>
  <artifactId>citeproc-java</artifactId>
  <version>0.7-SNAPSHOT</version>
</dependency>
```

FIXME:  Is it possible to add the citeproc-java target/classes directories to the classpath, using
\<resource> elements (see how I did that for styles and locales, below)?  That would mean that
the dev version of citeproc-java could be used without having to repackage it every time you make
a change.


### Citation style language (CSL) library

By default, this runs with the styles and locales packages that are uploaded to the Sonatype
repository daily.  When packaged as an uber-jar, that version of those repos will be packaged
as well.  So at the point it is packaged and deployed, the available CSL files is frozen.

You can develop and test with a development version of these, simply by cloning the GitHub
repositories [citation-style-language/styles](https://github.com/citation-style-language/styles) and/or
[citation-style-language/locales](https://github.com/citation-style-language/locales) underneath
this repo's root directory.  The pom file adds the `styles` and `locales` directories as resources
to the classpath, so if they are present, those development versions will be used instead of the
packaged ones.


### Dependencies

Dependencies are declared in the *pom.xml* file, and most are resolved automatically by Maven.

Below is a list of some of the notable dependencies, along with links to documentation,
useful when doing development.


**Java**

Requires Java version 8.

* Platform / library [Javadocs](http://docs.oracle.com/javase/8/docs/api/)

**Saxon**

It uses Saxon for XSLT tranformations.

* [Javadocs](http://www.saxonica.com/documentation/Javadoc/index.html)

**citeproc-java**

* [Javadocs](http://michel-kraemer.github.io/citeproc-java/api/latest/)

See [above](#citeproc-java) for some information about how to build and 
link the development version of this library, rather
than using the released package.

**citeproc-js**

* [Manual](http://gsl-nagoya-u.net/http/pub/citeproc-doc.html)

This is included by reference, from citeproc-java.

**PMC ID Converter API**

* [Documentation](https://www.ncbi.nlm.nih.gov/pmc/tools/id-converter-api/)

**Jackson**

We're using the Jackson library to read JSON objects:

* [Home page](http://wiki.fasterxml.com/JacksonHome)
* [Data binding](https://github.com/FasterXML/jackson-databind) - includes tutorial
  on how to use it.
* [Javadocs](http://fasterxml.github.io/jackson-databind/javadoc/2.3.0/)
* [Jackson annotations](https://github.com/FasterXML/jackson-annotations) - how to
  annotate the classes that map to JSON objects

**kitty-cache**

This library, [kitty-cache](https://code.google.com/p/kitty-cache/), is not in
Maven Central. It is declared in the *pom.xml*, but needs to be built and installed to your
local maven repository.  For example:

```
svn checkout http://kitty-cache.googlecode.com/svn/trunk/ kitty-cache-read-only
cd kitty-cache-read-only
mvn install
```

**DtdAnalyzer**

Some of the XSLT conversions of XML to JSON import a library XSLT from the
[DtdAnalyzer](https://github.com/ncbi/DtdAnalyzer).  The library XSLT is
[xml2json-2.0.xsl](https://github.com/ncbi/DtdAnalyzer/blob/master/xslt/xml2json-2.0.xsl).

**Jetty**

* [Documentation](http://www.eclipse.org/jetty/documentation/9.2.1.v20140609/)
* [Javadocs](http://download.eclipse.org/jetty/stable-9/apidocs/)


### XSLT conversions

You can try out any given XSLT from the command line, using Saxon Home Edition.

For example, nxml2json.xsl converts PMC NXML into citeproc-json format.  To try it out,
first set an alias to point to your Saxon jar file, and the catalog
file to use to resolve the DTDs.  For example:

```
alias saxon95='java -cp /path/to/saxon9.5.1.4/saxon9he.jar:/pmc/JAVA/saxon9.5.1.4/xml-commons-resolver-1.2/resolver.jar \
  net.sf.saxon.Transform -catalog:/path/to/catalog.xml '
```

Then, run the transformations:

```
cd test
saxon95 -s:aiid/3362639.nxml -xsl:../xslt/nxml2json.xsl pmcid=PMC3362639
saxon95 -s:aiid/3352855.nxml -xsl:../xslt/nxml2json.xsl pmid=22615544 pmcid=PMC3352855
```

### Exception handling

Exception classes used when handling a request:

- CiteException
    - BadParamException - e.g. an id value that doesn't match one of the known patterns
    - NotFoundException - when an id is of the correct form, but not resolved, or the pub-one
      not found in the stcache
    - ServiceException - some problem with upstream service; results in 500
    - StyleNotFoundException - results in 404
- java.io.IOException - from, for example, java xml processing, error creating CSL object, etc.
  Results in 500

When handling a request, the PrintWriter for the page is instantiated last.  If there is an
exception when that is done, then the code just writes to the error log and returns.

This is implemented with the pattern:

```java
resp.setContentType("text/html;charset=UTF-8");
resp.setCharacterEncoding("UTF-8");
resp.setStatus(HttpServletResponse.SC_OK);
initPage();
if (page == null) return;
```

So, this means that the whole page is prepared as a String first, and then written out after the
page is initialized.


# Public Domain notice

National Center for Biotechnology Information.

This software is a "United States Government Work" under the terms of the
United States Copyright Act.  It was written as part of the authors'
official duties as United States Government employees and thus cannot
be copyrighted.  This software is freely available to the public for
use. The National Library of Medicine and the U.S. Government have not
placed any restriction on its use or reproduction.

Although all reasonable efforts have been taken to ensure the accuracy
and reliability of the software and data, the NLM and the U.S.
Government do not and cannot warrant the performance or results that
may be obtained by using this software or data. The NLM and the U.S.
Government disclaim all warranties, express or implied, including
warranties of performance, merchantability or fitness for any
particular purpose.

Please cite NCBI in any work or product based on this material.


