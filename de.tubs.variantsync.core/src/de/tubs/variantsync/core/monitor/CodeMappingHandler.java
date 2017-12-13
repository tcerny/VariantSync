package de.tubs.variantsync.core.monitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.tubs.variantsync.core.VariantSyncPlugin;
import de.tubs.variantsync.core.data.CodeMapping;
import de.tubs.variantsync.core.data.Context;
import de.tubs.variantsync.core.data.SourceFile;
import de.tubs.variantsync.core.patch.DeltaFactoryManager;
import de.tubs.variantsync.core.patch.interfaces.IDelta;
import de.tubs.variantsync.core.patch.interfaces.IDeltaFactory;
import de.tubs.variantsync.core.patch.interfaces.IMarkerInformation;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CodeMappingHandler {

	public static void addCodeMappingsForDeltas(List<IDelta<?>> deltas) {
		for (IDelta delta : deltas) {
			try {
				IDeltaFactory<?> deltaFactory = DeltaFactoryManager.getFactoryById(delta.getFactoryId());
				List<IMarkerInformation> markerInformations = deltaFactory.getMarkerHandler().getMarkersForDelta(delta.getResource(), delta);

				Context context = VariantSyncPlugin.getDefault().getActiveEditorContext();
				if (context != null) {
					SourceFile sourceFile = context.getMapping(delta.getResource());
					if (sourceFile == null) {
						sourceFile = new SourceFile(delta.getResource());
					}
					for (IMarkerInformation markerInformation : markerInformations) {
						markerInformation.setFeatureExpression(delta.getFeature());
						sourceFile.addMapping(new CodeMapping(delta.getRevisedAsString(), markerInformation));
					}
					context.addCodeMapping(delta.getResource(), sourceFile);
				}
			} catch (NoSuchExtensionException e) {
				e.printStackTrace();
			}
		}
	}

	public static void addCodeMappings(IFile file, String feature, int offset, int length, String content) {
		try {
			IDeltaFactory<?> deltaFactory = DeltaFactoryManager.getInstance().getFactoryByFile(file);
			List<IMarkerInformation> markerInformations = deltaFactory.getMarkerHandler().getMarkers(file, offset, length);

			Context context = VariantSyncPlugin.getDefault().getActiveEditorContext();
			if (context != null) {
				SourceFile sourceFile = context.getMapping(file);
				if (sourceFile == null) {
					sourceFile = new SourceFile(file);
				}
				for (IMarkerInformation markerInformation : markerInformations) {
					markerInformation.setFeatureExpression(feature);
					sourceFile.addMapping(new CodeMapping(content, markerInformation));
				}
				context.addCodeMapping(file, sourceFile);
			}
		} catch (NoSuchExtensionException e) {
			e.printStackTrace();
		}
	}

	public static CodeMapping getCodeMapping(SourceFile sourceFile, IMarkerInformation markerInformation) {
		for (CodeMapping mapping : sourceFile.getMappings()) {
			if (!mapping.getMarkerInformation().equals(markerInformation)) {
				return mapping;
			}
		}
		return null;
	}

	public static boolean contains(SourceFile sourceFile, int line) {
		for (CodeMapping mapping : sourceFile.getMappings()) {
			IMarkerInformation markerInformation = mapping.getMarkerInformation();
			if (markerInformation.isLine() && markerInformation.getOffset() == line) {
				return true;
			}
		}
		return false;
	}

	public static boolean remove(SourceFile sourceFile, IMarkerInformation markerInformation) {
		List<CodeMapping> mappings = new ArrayList<>();
		List<CodeMapping> oldMappings = sourceFile.getMappings();
		for (CodeMapping mapping : oldMappings) {
			if (!mapping.getMarkerInformation().equals(markerInformation)) {
				mappings.add(mapping);
			}
		}
		sourceFile.setMapping(mappings);
		return oldMappings.size() == mappings.size();
	}

	public static void updateCodeMappingsForDelta(SourceFile sourceFile, IDelta delta) {
		if (sourceFile.getFile().getFullPath().equals(delta.getResource().getFullPath())) {
			try {
				IDeltaFactory<?> deltaFactory = DeltaFactoryManager.getFactoryById(delta.getFactoryId());
				List<IMarkerInformation> markerInformations = deltaFactory.getMarkerHandler().getMarkersForDelta(delta.getResource(), delta);

				switch (delta.getType()) {
				case ADDED:
					updateCodeMappingsForAddedDelta(sourceFile, delta, markerInformations);
					break;
				case CHANGED:
					updateCodeMappingsForChangedDelta(sourceFile, delta, markerInformations);
					break;
				case REMOVED:
					updateCodeMappingsForRemovedDelta(sourceFile, delta, markerInformations);
					break;
				default:
					break;
				}
			} catch (NoSuchExtensionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void updateCodeMappingsForAddedDelta(SourceFile sourceFile, IDelta delta, List<IMarkerInformation> markerInformations) {
		for (CodeMapping codeMapping : sourceFile.getMappings()) {
			IMarkerInformation cmMarkerInformation = codeMapping.getMarkerInformation();
			if (cmMarkerInformation.isLine()) {

			}
		}
	}

	private static void updateCodeMappingsForChangedDelta(SourceFile sourceFile, IDelta delta, List<IMarkerInformation> markerInformations) {
		// TODO Auto-generated method stub

	}

	private static void updateCodeMappingsForRemovedDelta(SourceFile sourceFile, IDelta delta, List<IMarkerInformation> markerInformations) {
		// TODO Auto-generated method stub

	}

}
