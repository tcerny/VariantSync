package de.tubs.variantsync.core.persistence;

import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.ovgu.featureide.fm.core.io.APersistentFormat;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.AXMLFormat;
import de.tubs.variantsync.core.data.Context;

public class ContextFormat extends AXMLFormat<Context> {

	private static final String ID = "Context";
	private static final String CONTEXT = "context";
	private static final String CONTEXT_ACTIVE = "active";
	private static final Pattern CONTENT_REGEX = Pattern.compile("\\A\\s*(<[?]xml\\s.*[?]>\\s*)?<" + CONTEXT + "[\\s>]");

	public static final String FILENAME = ".context.xml";

	@Override
	public APersistentFormat<Context> getInstance() {
		return new ContextFormat();
	}

	@Override
	public boolean supportsRead() {
		return true;
	}

	@Override
	public boolean supportsWrite() {
		return true;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	protected void readDocument(Document doc, List<Problem> warnings) throws UnsupportedModelException {
		for (final Element e : getElements(doc.getDocumentElement().getChildNodes())) {
			object.setActualContext(e.getAttribute("context"));
		}

	}

	@Override
	protected void writeDocument(Document doc) {
		final Element root = doc.createElement(CONTEXT);

		Element e = doc.createElement(CONTEXT_ACTIVE);
		e.setAttribute("context", object.getActualContext());
		root.appendChild(e);

		doc.appendChild(root);
	}

	@Override
	public boolean supportsContent(CharSequence content) {
		return supportsRead() && CONTENT_REGEX.matcher(content).find();
	}

	@Override
	public String getName() {
		return "Context";
	}

}
