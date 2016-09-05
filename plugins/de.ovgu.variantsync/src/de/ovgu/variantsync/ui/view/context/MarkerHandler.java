package de.ovgu.variantsync.ui.view.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import de.ovgu.variantsync.VariantSyncPlugin;
import de.ovgu.variantsync.applicationlayer.ModuleFactory;
import de.ovgu.variantsync.applicationlayer.context.ContextOperations;
import de.ovgu.variantsync.applicationlayer.datamodel.context.Class;
import de.ovgu.variantsync.applicationlayer.datamodel.context.CodeLine;
import de.ovgu.variantsync.applicationlayer.datamodel.context.Context;
import de.ovgu.variantsync.applicationlayer.datamodel.context.Element;
import de.ovgu.variantsync.applicationlayer.datamodel.context.Variant;
import de.ovgu.variantsync.ui.view.codemapping.CodeMarkerFactory;
import de.ovgu.variantsync.ui.view.codemapping.MarkerInformation;
import de.ovgu.variantsync.ui.view.codemapping.RemoveMarkerJob;
import de.ovgu.variantsync.utilities.Util;

/**
 * 
 *
 * @author Tristan Pfofe (tristan.pfofe@ckc.de)
 * @version 1.0
 * @since 14.09.2015
 */
public class MarkerHandler {

	private Map<Long, MarkerInformation> markerMap;
	private ContextOperations contextOp = ModuleFactory.getContextOperations();
	private IFile activeFile;

	private static MarkerHandler instance;

	private MarkerHandler() {
		markerMap = new HashMap<Long, MarkerInformation>();
	}

	public static MarkerHandler getInstance() {
		if (instance == null) {
			instance = new MarkerHandler();
		}
		return instance;
	}

	public void addMarker(MarkerInformation mi, IMarker marker, IResource file, String feature) {
		CodeMarkerFactory.addAnnotation(marker, VariantSyncPlugin.getEditor(),
				CodeMarkerFactory.getFeatureColor(feature), mi.getOffset(), mi.getLength());
		mi.setMarkerId(marker.getId());
		markerMap.put(mi.getMarkerId(), mi);
	}

	public void removeMarker(IResource res, int start, int end) {
		List<IMarker> markers = CodeMarkerFactory.findMarkers(res);
		for (IMarker marker : markers) {
			MarkerInformation mi = markerMap.get(marker.getId());
			if (mi != null && start == mi.getStart() && end == mi.getEnd()) {
				try {
					markerMap.remove(marker.getId());
					marker.delete();
					break;
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void clearAllMarker(IResource res) {
		Job job = new RemoveMarkerJob(res);
		job.setPriority(Job.SHORT);
		job.schedule();
	}

	private void setMarker(IFile file, List<MarkerInformation> markers) {
		IDocument document = null;
		try {
			document = (IDocument) VariantSyncPlugin.getEditor().getDocumentProvider()
					.getDocument(VariantSyncPlugin.getEditor().getEditorInput());
		} catch (NullPointerException e) {
			return;
		}
		int i = 0;
		for (MarkerInformation mi : markers) {
			try {
				IRegion regionStart = document.getLineInformation(mi.getStart() - 1);
				IRegion regionEnd = document.getLineInformation(mi.getEnd() - 1);
				try {
					int start = regionStart.getOffset();
					int end = regionEnd.getOffset() + 2;
					if (regionStart.getLength() == regionEnd.getLength()
							&& regionStart.getOffset() == regionEnd.getOffset()) {
						end = regionStart.getOffset() + regionEnd.getLength();
					}
					CodeMarkerFactory.createMarker(String.valueOf(i), file, start, end, mi.getFeature(), mi.getColor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
			i++;
		}
	}

	public void updateMarker(String projectName, String packageName, String className, Context activeContext) {
		if (activeContext.getFeatureExpression().equals("Default_Context"))
			return;
		String file = "/src/" + packageName + "/" + className;
		file = file.replace(".", "/");
		file = file.replace("/java", ".java");
		IPath path = new Path(file);
		IProject iProject = null;
		if (VariantSyncPlugin.getDefault() != null) {
			List<IProject> projects = VariantSyncPlugin.getDefault().getSupportProjectList();
			for (IProject p : projects) {
				if (p.getName().equals(projectName)) {
					iProject = p;
					break;
				}
			}
		}
		if (iProject != null) {
			IFile iFile = iProject.getFile(path);
			MarkerHandler.getInstance().clearAllMarker(iFile);
			List<MarkerInformation> markers = initMarker(activeContext, projectName,
					iFile.toString().substring(iFile.toString().lastIndexOf("/") + 1));

			setMarker(iFile, markers);
		}
	}

	private List<MarkerInformation> initMarker(Context context, String projectName, String className) {
		if (context.getFeatureExpression().equals("Default_Context"))
			return new ArrayList<MarkerInformation>();
		Set<MarkerInformation> markers = new HashSet<MarkerInformation>();
		Map<String, List<Class>> classes = ModuleFactory.getContextOperations().findJavaClass(projectName, className);
		Set<Entry<String, List<Class>>> set = classes.entrySet();
		Iterator<Entry<String, List<Class>>> it = set.iterator();
		while (it.hasNext()) {
			Entry<String, List<Class>> entry = it.next();
			List<Class> listClasses = entry.getValue();
			for (Class c : listClasses) {
				List<CodeLine> cls = c.getCodeLines();
				int i = 0;
				List<CodeLine> tmp = new ArrayList<CodeLine>();
				for (CodeLine cl : cls) {
					tmp.add(cl);
					if (cls.size() > i + 1 && cls.get(i + 1).getLine() == cl.getLine() + 1) {
						tmp.add(cls.get(i + 1));
						System.out.println("Tagged: " + cls.get(i + 1));
					} else {
						MarkerInformation mi = new MarkerInformation(0, tmp.get(0).getLine(),
								tmp.get(tmp.size() - 1).getLine(), 0, 0);
						System.out.println("Tagged: " + tmp.get(0).getLine() + " - " + tmp.get(tmp.size() - 1).getLine()
								+ " => " + tmp.get(0).getCode() + ", " + tmp.get(tmp.size() - 1).getCode());
						mi.setFeature(entry.getKey());
						mi.setColor(ModuleFactory.getContextOperations().findColor(entry.getKey()));
						markers.add(mi);
						tmp.clear();
					}
					i++;
				}
			}
		}
		Variant jp = context.getJavaProject(projectName);
		List<Element> elements = jp.getChildren();
		List<Class> cc = new ArrayList<Class>();
		Util.getClassesByClassName(elements, cc, className);
		for (Class c : cc) {
			List<CodeLine> cls = c.getCodeLines();
			int i = 0;
			List<CodeLine> tmp = new ArrayList<CodeLine>();
			for (CodeLine cl : cls) {
				tmp.add(cl);
				if (cls.size() > i + 1 && cls.get(i + 1).getLine() == cl.getLine() + 1) {
					tmp.add(cls.get(i + 1));
					System.out.println("Tagged: " + cls.get(i + 1));
				} else {
					MarkerInformation mi = new MarkerInformation(0, tmp.get(0).getLine(),
							tmp.get(tmp.size() - 1).getLine(), 0, 0);
					System.out.println("Tagged: " + tmp.get(0).getLine() + " - " + tmp.get(tmp.size() - 1).getLine()
							+ " => " + tmp.get(0).getCode() + ", " + tmp.get(tmp.size() - 1).getCode());
					mi.setFeature(context.getFeatureExpression());
					mi.setColor(context.getColor());
					markers.add(mi);
					tmp.clear();
				}
				i++;
			}
		}
		// markers.add(new MarkerInformation(0, 2, 5, 0, 0));
		return new ArrayList<MarkerInformation>(markers);
	}

	public void refreshMarker(IFile file) {

		// file is null, if marker of active file have to be refreshed
		if (file == null) {
			file = activeFile;

			// if a new file is active then the active file will be stored
		} else {
			activeFile = file;
		}
		if (file != null && file.getProject() != null) {
			String projectName = file.getProject().getName();
			String fileName = file.getName();
			Map<String, List<Class>> classes = contextOp.findJavaClass(projectName, fileName);

			MarkerHandler.getInstance().clearAllMarker(file);
			List<MarkerInformation> markers = new ArrayList<MarkerInformation>();
			Set<Entry<String, List<Class>>> set = classes.entrySet();
			Iterator<Entry<String, List<Class>>> it = set.iterator();
			while (it.hasNext()) {
				Entry<String, List<Class>> entry = it.next();
				List<Class> listClasses = entry.getValue();
				for (Class c : listClasses) {
					List<CodeLine> cls = c.getCodeLines();
					int i = 0;
					List<CodeLine> tmp = new ArrayList<CodeLine>();
					for (CodeLine cl : cls) {
						tmp.add(cl);
						if (cls.size() > i + 1 && cls.get(i + 1).getLine() == cl.getLine() + 1) {
							tmp.add(cls.get(i + 1));
						} else {
							MarkerInformation mi = new MarkerInformation(0, tmp.get(0).getLine(),
									tmp.get(tmp.size() - 1).getLine(), 0, 0);
							mi.setFeature(entry.getKey());
							mi.setColor(contextOp.findColor(entry.getKey()));
							if (mi.getFeature() != null && !"Default_Context".equals(mi.getFeature())) {
								markers.add(mi);
							}
							tmp.clear();
						}
						i++;
					}
				}
			}
			try {
				setMarker(file, markers);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}