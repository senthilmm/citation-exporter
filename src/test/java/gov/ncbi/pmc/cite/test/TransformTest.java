package gov.ncbi.pmc.cite.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.helger.commons.xml.serialize.read.DOMInputStreamProvider;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.ItemSource;
import gov.ncbi.pmc.cite.TransformEngine;
import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;

public class TransformTest {
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();

    /**
     * Test simply retrieving an NXML.
     */
    @Test
    public void testGetNxml() throws Exception
    {
        log = TestSetup.setup(name);
        ItemSource itemSource = App.getItemSource();

        Document nxml_31 = null;
        RequestId requestId = new RequestId("31", new Identifier("aiid", "31"));
        nxml_31 = itemSource.retrieveItemNxml(requestId);
        assertEquals(nxml_31.getDocumentElement().getTagName(), "article");
    }

    /**
     * Test the identity transform.
     */
    @Test
    public void testIdentityTransform() throws Exception
    {
        log = TestSetup.setup(name);
        Document in, out;
        TransformEngine engine = App.getTransformEngine();

        in = xmlFromString("<foo/>");
        out = (Document) engine.doTransform(in, "identity");
        log.trace("Output XML:\n" + serializeXml(out));
        assertEquals("foo", out.getDocumentElement().getTagName());

        in = xmlFromString(
            "<splits>\n" +
            "  <fleegle plays='guitar'/>\n" +
            "  <bingo plays='drums'/>\n" +
            "</splits>\n");
        out = (Document) engine.doTransform(in, "identity");
        log.trace("Output XML:\n" + serializeXml(out));
        Element outRoot = out.getDocumentElement();
        assertEquals("splits", outRoot.getTagName());
        // child nodes including whitespace text nodes
        assertEquals(5, outRoot.getChildNodes().getLength());
    }

    /**
     * Test the string-value transform.
     */
    @Test
    public void testStringValueTransform() throws Exception
    {
        log = TestSetup.setup(name);
        Document in;
        String out;
        TransformEngine engine = App.getTransformEngine();

        in = xmlFromString("<foo>bar</foo>");
        out = (String) engine.doTransform(in, "string-value");
        log.trace("Output String:\n" + out);
        assertThat(out, containsString("bar"));

        in = xmlFromString(
            "<splits>\n" +
            "  <split><name>fleegle</name>: <plays>guitar</plays></split>\n" +
            "  <split><name>bingo</name>: <plays>drums</plays></split>\n" +
            "</splits>\n");
        out = (String) engine.doTransform(in, "string-value");
        assertThat(out, containsString("fleegle: guitar"));
        log.trace("Output String:\n" + out);
    }

    /**
     * Test transform with parameters.
     */
    @Test
    public void testParams() throws Exception
    {
        log = TestSetup.setup(name);
        Document in;
        Map<String, String> params = new HashMap<String, String>();
        String out;
        String expect;
        TransformEngine engine = App.getTransformEngine();

        in = xmlFromString("<foo/>");

        // With no params, we should see the default value
        out = (String) engine.doTransform(in, "test-params");
        log.trace("Output String:\n" + out);
        expect = "default param1 value";
        assertThat(out, containsString(expect));

        // Empty params map should work the same way
        out = (String) engine.doTransform(in, "test-params", params);
        log.trace("Output String:\n" + out);
        assertThat(out, containsString(expect));

        // Now, pass in a real param
        expect = "Zombo and Wumbus rock!";
        params.put("param1", expect);
        out = (String) engine.doTransform(in, "test-params", params);
        log.trace("Output String:\n" + out);
        assertThat(out, containsString(expect));
    }

    /**
     * Test that transforms with our custom URI resolver work.
     */
    @Test
    public void testTransformWithResolver() throws Exception
    {
        log = TestSetup.setup(name);
        Document in, out;
        TransformEngine engine = App.getTransformEngine();

        in = xmlFromString("<foo/>");
        out = (Document) engine.doTransform(in, "test-resolver");
        log.trace("Output XML:\n" + serializeXml(out));
        assertEquals("foo", out.getDocumentElement().getTagName());
    }


    /**
     * Run all the test cases
    @Test
    public void testCases() throws Exception
    {
        log = TestSetup.setup(name);

        // Read the test-cases json file
        ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        URL testCasesUrl = getClass().getClassLoader()
            .getResource("test-cases.json");
        List<TestCaseDescriptor> testCaseList =
            mapper.readValue(testCasesUrl.openStream(),
                new TypeReference<List<TestCaseDescriptor>>() {});

        ItemSource itemSource = App.getItemSource();
        TransformEngine engine = App.getTransformEngine();
        Document srcDoc = null;


        log.info("Number of test cases: " + testCaseList.size());

        Iterator<TestCaseDescriptor> i = testCaseList.iterator();
        while (i.hasNext()) {
            TestCaseDescriptor testCase = i.next();
            String description = testCase.description;
            log.info("Running transform test '" + description + "'");

            String id = testCase.id;
            String inReport = testCase.inreport;
            String transform = testCase.transform;

            // Get the input
            RequestId rid = new RequestId(id);
            if (inReport.equals("nxml")) {
                srcDoc = itemSource.retrieveItemNxml(rid);
            }
            log.trace("Source document:");
            log.trace(serializeXml(srcDoc));


            // Do the transformation
            Object result = engine.doTransform(srcDoc, transform);
            log.trace("Transform result:");
            log.trace(serializeXml((Document) result));
            System.out.println("======================= wee");

            String s =
                    "<sch:schema xmlns:sch='http://purl.oclc.org/dsdl/schematron' xml:lang='de'>\n" +
                            "  <sch:title>Example of Multi-Lingual Schema</sch:title>\n" +
                            "  <sch:pattern>\n" +
                            "    <sch:rule context='dog'>\n" +
                            "      <sch:assert test='bone' diagnostics='d1 d2'> A dog should have a bone.</sch:assert>\n" +
                            "    </sch:rule>\n" +
                            "  </sch:pattern>\n" +
                            "  <sch:diagnostics>\n" +
                            "    <sch:diagnostic id='d1' xml:lang='en'> A dog should have a bone.</sch:diagnostic>\n" +
                            "    <sch:diagnostic id='d2' xml:lang='de'> Das  Hund muss ein Bein haben.</sch:diagnostic>\n" +
                            "  </sch:diagnostics>\n" +
                            "</sch:schema>\n";
            validateXml(s, (Document) result);
        }
    }

     */


    /**
     * Test whether an XML file conforms to a given Schematron
     * @param sstr
     * @param xml
     * @return
     * @throws IllegalArgumentException
     */
    public static boolean validateXml(
            @Nonnull final String sstr,
            @Nonnull final Document xml)
        throws Exception
    {
        System.out.println("======================= woo");
        final ISchematronResource schematron =
            SchematronResourcePure.fromString(sstr,
                Charset.forName("utf-8"));
        if (!schematron.isValidSchematron())
            throw new IllegalArgumentException("Invalid Schematron!");
        return schematron.getSchematronValidity(
            new DOMInputStreamProvider(xml)).isValid();
    }

    /**
     * Helper function to serialize XML for logging results.
     * @param doc
     * @return
     */
    public static String serializeXml(Document doc)    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           writer.flush();
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }

    /**
     * Helper function to create a JAXP Document object from a string of xml.
     * @param s
     * @return JAXP Document object
     * @throws Exception
     */
    public static Document xmlFromString(String s) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(s));
        return db.parse(is);
    }


}

