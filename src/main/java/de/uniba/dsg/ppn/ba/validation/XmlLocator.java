/**
 *
 * BPMN Validation Project to validate special BPMN Constraints, see \README.md
 *
 * Copyright (C) 2014 Philipp Neugebauer
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package de.uniba.dsg.ppn.ba.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedElement;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniba.dsg.ppn.ba.helper.ConstantHelper;

/**
 *
 * Locates the lines of the validation errors
 *
 * @author Philipp Neugebauer
 * @version 1.0
 *
 */
public class XmlLocator {

    private final SAXBuilder saxBuilder;
    private final XPathFactory xPathFactory;
    private static final Logger LOGGER;

    static {
        LOGGER = LoggerFactory.getLogger(XmlLocator.class.getSimpleName());
    }

    public XmlLocator() {
        saxBuilder = new SAXBuilder();
        saxBuilder.setJDOMFactory(new LocatedJDOMFactory());
        xPathFactory = XPathFactory.instance();
    }

    /**
     * Searches the line of the given xpath expression in the given file and
     * returns either the line or -1. -1 means, that with the xpath expression
     * couldn't be determined a bpmn element.
     *
     * @param xmlFile
     *            the xml file where the error has to be found
     * @param xpathExpression
     *            the xpath expression to find the error in the file
     * @return line or -1
     */
    public int findLine(File xmlFile, String xpathExpression) {
        try {
            Document doc = saxBuilder.build(xmlFile);
            int bracketPosition = xpathExpression.lastIndexOf('[');
            int elementPosition = 0;
            try {
                elementPosition = Integer.parseInt(xpathExpression.substring(
                        bracketPosition + 1, xpathExpression.lastIndexOf(']')));
                xpathExpression = xpathExpression.substring(0, bracketPosition);
            } catch (NumberFormatException e) {
                // ignore, because then there's no position number in the xpath
                // expression and the expression needn't to be rewritten
            }
            XPathExpression<Element> xpath = xPathFactory
                    .compile(xpathExpression, Filters.element(), null,
                            Namespace.getNamespace("bpmn",
                                    ConstantHelper.BPMNNAMESPACE));

            List<Element> foundElements = xpath.evaluate(doc);

            if (foundElements.size() > elementPosition) {
                return ((LocatedElement) foundElements.get(elementPosition))
                        .getLine();
            }
        } catch (IOException | JDOMException e) {
            LOGGER.debug(ConstantHelper.FILENOTFOUNDMESSAGEWITHCAUSE,
                    xmlFile.getName(), e);
        }
        return -1;
    }
}
