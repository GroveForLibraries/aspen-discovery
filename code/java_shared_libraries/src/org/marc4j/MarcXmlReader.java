/**
 * Copyright (C) 2004 Bas Peters
 *
 * This file is part of MARC4J
 *
 * MARC4J is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * MARC4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with MARC4J; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.marc4j;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.marc4j.marc.Record;
import org.xml.sax.InputSource;

/**
 * An iterator over a collection of MARC records in MARCXML format.
 * <p>
 * Basic usage:
 * </p>
 *
 * <pre>
 * InputStream input = new FileInputStream(&quot;file.xml&quot;);
 * MarcReader reader = new MarcXmlReader(input);
 * while (reader.hasNext()) {
 *   Record record = reader.next();
 *   // Process record
 * }
 * </pre>
 * <p>
 * Check the {@link org.marc4j.marc}&nbsp;package for examples about the use of the {@link Record}
 * &nbsp;object model.
 * </p>
 * <p>
 * You can also pre-process the source to create MARC XML from a different format using an XSLT stylesheet. The
 * following example creates an iterator over a collection of MARC records in MARC XML format from a MODS source and
 * outputs MARC records in MARC21 format:
 * </p>
 *
 * <pre>
 * InputStream in = new FileInputStream(&quot;modsfile.xml&quot;);
 *
 * MarcStreamWriter writer = new MarcStreamWriter(System.out, Constants.MARC8);
 * MarcXmlReader reader =
 *   new MarcXmlReader(in, &quot;<a href="http://www.loc.gov/standards/marcxml/xslt/MODS2MARC21slim.xsl&quot;">...</a>);
 * while (reader.hasNext()) {
 *   Record record = reader.next();
 *   writer.write(record);
 * }
 * writer.close();
 * </pre>
 *
 * @author Bas Peters
 */
@SuppressWarnings("SpellCheckingInspection")
public class MarcXmlReader implements MarcReader {

    private final RecordStack queue;

    /**
     * Constructs an instance with the specified input stream.
     *
     * @param input the input stream
     */
    public MarcXmlReader(final InputStream input) {
        this(new InputSource(input));
    }

    /**
     * Constructs an instance with the specified input source.
     *
     * @param input the input source
     */
    public MarcXmlReader(final InputSource input) {
        this.queue = new RecordStack();
        final MarcXmlParserThread producer = new MarcXmlParserThread(queue, input);
        producer.start();
    }

    /**
     * Constructs an instance with the specified input stream and stylesheet
     * location.
     * 
     * The stylesheet is used to transform the source file and should produce
     * valid MARC XML records. The result is then used to create
     * <code>Record</code> objects.
     *
     * @param input the input stream
     * @param stylesheetUrl the stylesheet location
     */
    public MarcXmlReader(final InputStream input, final String stylesheetUrl) {
        this(new InputSource(input), new StreamSource(stylesheetUrl));
    }

    /**
     * Constructs an instance with the specified input stream and stylesheet
     * source.
     * 
     * The stylesheet is used to transform the source file and should produce
     * valid MARCXML records. The result is then used to create
     * <code>Record</code> objects.
     * 
     * @param input the input stream
     * @param stylesheet the stylesheet source
     */
    public MarcXmlReader(final InputStream input, final Source stylesheet) {
        this(new InputSource(input), stylesheet);
    }

    /**
     * Constructs an instance with the specified input source and stylesheet
     * source.
     * 
     * The stylesheet is used to transform the source file and should produce
     * valid MARCXML records. The result is then used to create
     * <code>Record</code> objects.
     * 
     * @param input the input source
     * @param stylesheet the stylesheet source
     */
    public MarcXmlReader(final InputSource input, final Source stylesheet) {
        this.queue = new RecordStack();
        final MarcXmlParserThread producer = new MarcXmlParserThread(queue, input);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final SAXTransformerFactory stf = (SAXTransformerFactory) factory;
        TransformerHandler th;
        try {
            th = stf.newTransformerHandler(stylesheet);
        } catch (final TransformerConfigurationException e) {
            throw new MarcException("Error creating TransformerHandler", e);
        }
        producer.setTransformerHandler(th);
        producer.start();
    }

    /**
     * Constructs an instance with the specified input stream and transformer
     * handler.
     * 
     * The {@link TransformerHandler}&nbsp;is used to
     * transform the source file and should produce valid MARCXML records. The
     * result is then used to create <code>Record</code> objects. A
     * <code>TransformerHandler</code> can be obtained from a
     * <code>SAXTransformerFactory</code> with either a
     * {@link Source}&nbsp;or
     * {@link javax.xml.transform.Templates}&nbsp;object.
     * 
     * @param input the input stream
     * @param th the transformation content handler
     */
    public MarcXmlReader(final InputStream input, final TransformerHandler th) {
        this(new InputSource(input), th);
    }

    /**
     * Constructs an instance with the specified input source and transformer
     * handler.
     * 
     * The {@link TransformerHandler}&nbsp;is used to
     * transform the source file and should produce valid MARCXML records. The
     * result is then used to create <code>Record</code> objects. A
     * <code>TransformerHandler</code> can be obtained from a
     * <code>SAXTransformerFactory</code> with either a
     * {@link Source}&nbsp;or
     * {@link javax.xml.transform.Templates}&nbsp;object.
     * 
     * @param input the input source
     * @param th the transformation content handler
     */
    public MarcXmlReader(final InputSource input, final TransformerHandler th) {
        this.queue = new RecordStack();
        final MarcXmlParserThread producer = new MarcXmlParserThread(queue, input);
        producer.setTransformerHandler(th);
        producer.start();
    }

    /**
     * Returns true if the iteration has more records, false otherwise.
     *
     * @return boolean - true if the iteration has more records, false otherwise
     */
    @Override
    public boolean hasNext() {
        return queue.hasNext();
    }

    /**
     * Returns the next record in the iteration.
     *
     * @return Record - the record object
     */
    @Override
    public Record next() {
        return queue.pop();
    }

}
