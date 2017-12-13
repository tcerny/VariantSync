package de.tubs.variantsync.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.BundleContext;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.EclipseExtensionLoader;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.tubs.variantsync.core.data.Context;
import de.tubs.variantsync.core.data.FeatureExpression;
import de.tubs.variantsync.core.data.SourceFile;
import de.tubs.variantsync.core.exceptions.ProjectNotFoundException;
import de.tubs.variantsync.core.jobs.MarkerUpdateJob;
import de.tubs.variantsync.core.monitor.ResourceChangeHandler;
import de.tubs.variantsync.core.nature.Variant;
import de.tubs.variantsync.core.patch.DeltaFactoryManager;
import de.tubs.variantsync.core.patch.interfaces.IDeltaFactory;
import de.tubs.variantsync.core.patch.interfaces.IPatch;
import de.tubs.variantsync.core.persistence.Persistence;
import de.tubs.variantsync.core.utilities.LogOperations;
import de.tubs.variantsync.core.utilities.MarkerUtils;
import de.tubs.variantsync.core.utilities.event.IEventListener;
import de.tubs.variantsync.core.utilities.event.VariantSyncEvent;
import de.tubs.variantsync.core.utilities.event.VariantSyncEvent.EventType;
import de.tubs.variantsync.core.view.editor.PartAdapter;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author Christopher Sontag (c.sontag@tu-bs.de)
 * @version 1.0
 * @since 1.0.0.0
 */
public class VariantSyncPlugin extends AbstractUIPlugin implements IEventListener, de.ovgu.featureide.fm.core.base.event.IEventListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tubs.variantsync.core"; //$NON-NLS-1$

	// The shared instance
	private static VariantSyncPlugin plugin;
	private static ResourceChangeHandler listener = new ResourceChangeHandler();
	private static HashMap<IFeatureProject, Context> INSTANCES = new HashMap<>();
	private static Context lastRequestedContext = null;
	private boolean isActive;
	private List<IEventListener> listeners = new ArrayList<>();

	/**
	 * The constructor
	 */
	public VariantSyncPlugin() {}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework. BundleContext)
	 */
	public void start(BundleContext ctxt) throws Exception {
		super.start(ctxt);
		plugin = this;
		addListener(this);

		DeltaFactoryManager
				.setExtensionLoader(new EclipseExtensionLoader<>(PLUGIN_ID, IDeltaFactory.extensionPointID, IDeltaFactory.extensionID, IDeltaFactory.class));

		init();

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				listenForActiveClass();
			}
		});
		addResourceChangeListener();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework. BundleContext)
	 */
	public void stop(BundleContext ctxt) throws Exception {
		plugin = null;

		if (lastRequestedContext != null && lastRequestedContext.getConfigurationProject() != null)
			this.getPreferenceStore().setValue("lastRequestedContext", lastRequestedContext.getConfigurationProject().getProjectName());

		for (Context context : INSTANCES.values()) {
			if (context != null && context.getConfigurationProject().getProject().exists()) {
				Persistence.writeContext(context);
				Persistence.writeFeatureExpressions(context);

				HashMap<IProject, List<SourceFile>> codeMappings = context.getCodeMappings();
				for (IProject project : codeMappings.keySet()) {
					Persistence.writeCodeMapping(project, codeMappings.get(project));
				}
				Persistence.writePatches(context.getConfigurationProject(), context.getPatches());
			}
		}
		super.stop(ctxt);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static VariantSyncPlugin getDefault() {
		return plugin;
	}

	public static IWorkspaceRoot getWorkspace() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	public static Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path.
	 * 
	 * @param path the path
	 * @return the image descriptor
	 */
	public ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Always good to have this static method as when dealing with IResources having a interface to get the editor is very handy
	 *
	 * @return
	 */
	public static ITextEditor getEditor() {
		if (getActiveWorkbenchWindow().getActivePage().getActiveEditor() instanceof ITextEditor) {
			return (ITextEditor) getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		} else {
			return null;
		}
	}

	public static IFile getEditorInput() {
		if (VariantSyncPlugin.getActiveWorkbenchWindow() == null) return null;
		IEditorPart editorPart = VariantSyncPlugin.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editorPart == null || !(editorPart instanceof IFileEditorInput)) return null;
		return ((IFileEditorInput) editorPart.getEditorInput()).getFile();
	}

	public Context getActiveEditorContext() {
		if (getEditorInput() == null) return lastRequestedContext;
		return getContext(getEditorInput().getProject());
	}

	public Context getContext(IFeatureProject project) {
		if (project != null) {
			for (IFeatureProject featureProject : INSTANCES.keySet()) {
				if (featureProject.getProjectName().equals(project.getProjectName())) {
					Context context = INSTANCES.get(featureProject);
					if (lastRequestedContext != context) {
						lastRequestedContext = context;
						fireEvent(new VariantSyncEvent(this, EventType.CONFIGURATIONPROJECT_CHANGED, null, lastRequestedContext));
					}
					return context;
				}
			}
			Context context = Persistence.loadContext(project);
			context.setConfigurationProject(project);
			addListener(context);
			INSTANCES.put(project, context);
			if (lastRequestedContext != context) {
				lastRequestedContext = context;
				fireEvent(new VariantSyncEvent(this, EventType.CONFIGURATIONPROJECT_CHANGED, null, lastRequestedContext));
			}
			return context;
		}
		return null;
	}

	public IFeatureProject getFeatureProject(IProject project) {
		for (IFeatureProject featureProject : INSTANCES.keySet()) {
			if (project.getName().equals(featureProject.getProjectName())) return featureProject;
			if (featureProject.getProject().exists() && INSTANCES.get(featureProject).getProjects().contains(project)) return featureProject;
		}
		return null;
	}

	public Context getContext(IProject project) {
		if (project != null) {
			IFeatureProject featureProject = getFeatureProject(project);
			if (featureProject != null) {
				return getContext(featureProject);
			}
		}
		return null;
	}

	/**
	 * Loads all contexts which are saved in a XML-file.
	 */
	private List<IFeatureProject> getConfigurationProjects() {
		List<IFeatureProject> projects = new ArrayList<>();
		for (IFeatureProject project : CorePlugin.getFeatureProjects()) {
			if (project.getComposerID().equals("de.tubs.variantsync.core.composer")) {
				LogOperations.logInfo("Found configuration project with name: " + project.getProjectName());
				projects.add(project);
			}
		}
		return projects;
	}

	public void loadVariants() {
		for (Context context : INSTANCES.values()) {
			for (IFile file : context.getConfigurationProject().getAllConfigurations()) {
				System.out.println("Variant:" + file.getName());
				String projectName = file.getName().substring(0, file.getName().lastIndexOf("."));
				IProject project = getWorkspace().getProject(projectName);
				if (project.exists()) {
					context.addProject(project);
					context.addCodeMapping(project, Persistence.loadCodeMapping(project));
				} else {
					try {
						IMarker m = file.createMarker("de.tubs.variantsync.marker.error");
						m.setAttribute(IMarker.MESSAGE, "Project " + projectName + " is missing in the workspace");
						m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
						m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						m.setAttribute(IMarker.LINE_NUMBER, 0);
					} catch (CoreException e) {
						LogOperations.logError("Marker cannot be created!", e);
					}
				}
			}
		}
	}

	public void loadFeatureExpressions() {
		for (Context context : INSTANCES.values()) {
			if (context.getConfigurationProject() != null) {
				List<FeatureExpression> expressions = Persistence.loadFeatureExpressions(context.getConfigurationProject());
				if (expressions.isEmpty()) {
					try {
						context.importFeaturesFromModel();
						LogOperations.logInfo("Loaded feature expressions from feature model");
					} catch (ProjectNotFoundException e) {
						System.out.println(e.toString());
					}
				} else {
					context.setFeatureExpressions(expressions);
				}
			}
		}
	}

	public void loadPatches() {
		for (Context context : INSTANCES.values()) {
			if (context.getConfigurationProject() != null) {
				List<IPatch<?>> patches = Persistence.loadPatches(context.getConfigurationProject());
				if (!patches.isEmpty()) {
					context.setPatches(patches);
				}
			}
		}
	}

	public void loadCodeMappings() {
		for (Context context : INSTANCES.values()) {
			for (IProject project : context.getProjects()) {
				List<SourceFile> sourceFiles = Persistence.loadCodeMapping(project);
				if (!sourceFiles.isEmpty()) {
					context.addCodeMapping(project, sourceFiles);
				}
			}
		}
	}

	public static void addResourceChangeListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
		try {
			listener.registerSaveParticipant();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public static void removeResourceChangeListener() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
	}

	public void init() {
		Context lastContext = null;
		for (IFeatureProject project : getConfigurationProjects()) {
			try {
				MarkerUtils.cleanProject(project.getProject());
			} catch (CoreException e) {
				LogOperations.logError("A marker could not be deleted", e);
			}
			if (project.getProjectName().equals(this.getPreferenceStore().getString("lastRequestedContext"))) {
				lastContext = getContext(project);
			} else {
				getContext(project);
			}
		}
		if (lastContext != null && lastRequestedContext != lastContext) {
			lastRequestedContext = lastContext;
			fireEvent(new VariantSyncEvent(this, EventType.CONFIGURATIONPROJECT_CHANGED, null, lastRequestedContext));
		}
		loadVariants();
		loadFeatureExpressions();
		loadPatches();
		loadCodeMappings();
		fireEvent(new VariantSyncEvent(this, EventType.INITALIZED, null, null));
	}

	public void reinit() {
		for (Context context : INSTANCES.values()) {
			if (context != null) {
				Persistence.writeContext(context);
				Persistence.writeFeatureExpressions(context);

				HashMap<IProject, List<SourceFile>> codeMappings = context.getCodeMappings();
				for (IProject project : codeMappings.keySet()) {
					Persistence.writeCodeMapping(project, codeMappings.get(project));
				}
				Persistence.writePatches(context.getConfigurationProject(), context.getPatches());
				context.reset();
			}
		}
		init();
	}

	/**
	 * Listen whether the active file in the java editor changes.
	 */
	public void listenForActiveClass() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow ww = wb.getActiveWorkbenchWindow();
//		IWorkbenchPage page = ww.getActivePage();

		PartAdapter adapter = new PartAdapter();
		ww.getPartService().addPartListener(adapter);
//		page.addPartListener(adapter);
//		if (page == null) return;
//		page.addPartListener(adapter);
	}

	public static void addNature(IProject project) {
		VariantSyncProgressMonitor progressMonitor = new VariantSyncProgressMonitor("Adding VariantSync nature to " + project.getName());
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 1, natures.length);

			newNatures[0] = Variant.NATURE_ID;

			description.setNatureIds(newNatures);
			progressMonitor.setSubTaskName("Add nature");
			project.setDescription(description, progressMonitor);
			progressMonitor.setSubTaskName("Refresh resources");
			project.refreshLocal(IResource.DEPTH_INFINITE, progressMonitor);
		} catch (CoreException e) {
			LogOperations.logError("", e);
		}
	}

	@Override
	public void propertyChange(FeatureIDEEvent event) {
		switch (event.getEventType()) {
		case FEATURE_ADD:
			LogOperations.logInfo("Feature added: " + event);
		case MODEL_DATA_SAVED:
			LogOperations.logInfo("Model Event" + event);
			if (event.getSource() instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) event.getSource();
				List<String> featureExpressions = getActiveEditorContext().getFeatureExpressionsAsStrings();
				for (IFeature feature : model.getFeatures()) {
					if (!featureExpressions.contains(feature.getName())) {
						getActiveEditorContext().addFeatureExpression(feature.getName());
					}
				}
			}
			// context.importFeaturesFromModel();
			break;
		default:
			break;

		}
	}

	@Override
	public void propertyChange(VariantSyncEvent event) {
		switch (event.getEventType()) {
		case CONFIGURATIONPROJECT_SET:
			if (event.getSource() instanceof Context) {
				Context context = (Context) event.getSource();
				if (context.getConfigurationProject() != null) {
					context.getConfigurationProject().getFeatureModelManager().addListener(this);
					context.getConfigurationProject().getFeatureModel().addListener(this);
				}
			}
			break;
		case PATCH_CHANGED:
			if (event.getSource() instanceof IFile) {
				MarkerUpdateJob job = new MarkerUpdateJob((IFile) event.getSource());
				job.schedule();
			}
		default:
			break;
		}
	}

	public boolean isActive() {
		return this.isActive;
	}

	public void setActive(boolean status) {
		this.isActive = status;
		if (isActive) {
			fireEvent(new VariantSyncEvent(this, EventType.CONTEXT_RECORDING_START, null, getActiveEditorContext()));
		} else {
			fireEvent(new VariantSyncEvent(this, EventType.CONTEXT_RECORDING_STOP, getActiveEditorContext(), null));
		}
	}

	public void addListener(IEventListener listener) {
		this.listeners.add(listener);

	}

	public void fireEvent(VariantSyncEvent event) {
		System.out.println(event);
		for (IEventListener listener : listeners) {
			listener.propertyChange(event);
		}
	}

	public void removeListener(IEventListener listener) {
		this.listeners.remove(listener);
	}
}
