package gov.ncbi.pmc.cite.test;

import static gov.ncbi.pmc.cite.test.Utils.serializeXml;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.helger.commons.xml.serialize.read.DOMInputStreamProvider;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.cite.ItemSource;
import gov.ncbi.pmc.cite.TransformEngine;
import gov.ncbi.pmc.ids.RequestId;

/**
 * Data-driven schematron and regular-expression matching tests of the
 * XSLT transforms. The data for these is read from transform-tests.json
 * into a List of TransformTestCase objects.
 *
 * You can use the `test_cases` system property to select which tests to run:
 * - If omitted, or empty, all tests are run
 * - Otherwise, it's matched against the description, as a regular expression
 *
 * So, to test all the cases that have "PubOne" in the description, set:
 * -Dtest_cases=PubOne
 */
@RunWith(value = Parameterized.class)
public class TestTransforms {
    protected App app;
    @SuppressWarnings("unused")
    private Logger log;
    private TransformTestCase testCase;
    private static String selected_test_cases =
            System.getProperty("test_cases");

    @Rule
    public TestName name = new TestName();

    @Before
    public void setup()  throws Exception
    {
        log = Utils.setup(name);
    }

    // Parameter passed in via this constructor
    public TestTransforms(TransformTestCase _testCase) {
        testCase = _testCase;
    }

    // This generates the parameters; reading them from
    // the JSON file
    @Parameters(name= "{index}: TestRequest: {0}")
    public static Collection<TransformTestCase> cases()
        throws Exception
    {
        // Read the transform-tests.json file
        ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        URL testCasesUrl = TestTransforms.class.getClassLoader()
            .getResource("transform-tests.json");
        List<TransformTestCase> testCaseList =
            mapper.readValue(testCasesUrl.openStream(),
                new TypeReference<List<TransformTestCase>>() {});

        // Filter the test based on the test_case system property
        return testCaseList.stream()
            .filter(p ->
                selected_test_cases == null ||
                selected_test_cases.equals("") ||
                p.description.matches("(.*)" + selected_test_cases + "(.*)")
            ).collect(Collectors.toList());
    }

    /**
     * Test one test case from the JSON file
     */
    @Test
    public void testCases() throws Exception
    {
        ItemSource itemSource = App.getItemSource();
        TransformEngine engine = App.getTransformEngine();
        Document srcDoc = null;

        String description = testCase.description;
        log.info("Running transform test '" + description + "'");

        // Get the input
        String id = testCase.id;
        RequestId rid = new RequestId(id);
        String informat = testCase.inFormat;
        if (informat.equals("nxml")) {
            srcDoc = itemSource.retrieveItemNxml(rid);
        }
        else if (informat.equals("pub1")) {
            srcDoc = itemSource.retrieveItemPubOne(rid);
        }
        else {
            throw new BadParamException("Bad value in test-cases for 'informat': " + informat);
        }
        log.trace("Source document:");
        log.trace(serializeXml(srcDoc));

        // Do the transformation
        Object result = engine.doTransform(srcDoc, testCase.transform);
        log.trace("Transform result:");

        // Validate the output
        String outformat = testCase.outFormat;
        if (outformat.equals("xml")) {
            Document resultDocument = (Document) result;
            log.trace(serializeXml(resultDocument));
            validateXmlTestCase(testCase, resultDocument);
        }
        else if (outformat.equals("json")) {
            String resultString = (String) result;
            log.trace(resultString);

            // use Jackson to convert it to an XML Document
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode resultJson = jsonMapper.readTree(resultString);
            XmlMapper xmlMapper = new XmlMapper();
            String xmlString = xmlMapper.writeValueAsString(resultJson);
            log.trace("json converted to xml:\n" + xmlString);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document resultDocument = builder.parse(is);
            validateXmlTestCase(testCase, resultDocument);
        }

        else if (outformat.equals("text")) {
            String resultString = (String) result;
            log.trace(resultString);
            testCase.expressions.forEach((String exp) -> {
                Pattern p = Pattern.compile("^.*" + exp + ".*$",
                        Pattern.DOTALL);
                assertThat(resultString, matchesPattern(p));
            });
        }
    }

    // Use schematron to validate xml and json
    private void validateXmlTestCase(TransformTestCase testCase,
            Document resultDocument)
        throws Exception
    {
        URL schematronUrl = getClass().getClassLoader()
            .getResource(testCase.validator + ".sch");
        assertTrue("Failed schematron validation",
            validateXml(schematronUrl, resultDocument));
    }

    /**
     * Test whether an XML file conforms to a given Schematron
     */
    static boolean validateXml(
            @Nonnull final URL schematronUrl,
            @Nonnull final Document xml)
        throws Exception
    {
        final ISchematronResource s =
            SchematronResourcePure.fromURL(schematronUrl);
        if (!s.isValidSchematron())
            throw new IllegalArgumentException("Invalid Schematron!");
        return s.getSchematronValidity(
            new DOMInputStreamProvider(xml)).isValid();
    }
}
