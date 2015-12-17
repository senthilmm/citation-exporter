package gov.ncbi.pmc.cite.test;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * Test the transformations
     */
    @Test
    public void testTransforms() throws Exception
    {
        log = TestSetup.setup(name);
        ItemSource itemSource = App.getItemSource();

        Document nxml_31 = null;
        RequestId requestId = new RequestId("31", new Identifier("aiid", "31"));
        nxml_31 = itemSource.retrieveItemNxml(requestId);
        assertEquals(nxml_31.getDocumentElement().getTagName(), "article");
    }

    /**
     * Run all the test cases
     */
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

    public String serializeXml(Document doc)    {
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
}

