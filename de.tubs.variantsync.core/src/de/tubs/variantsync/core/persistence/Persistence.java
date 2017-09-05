package de.tubs.variantsync.core.persistence;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.tubs.variantsync.core.VariantSyncPlugin;
import de.tubs.variantsync.core.data.Context;
import de.tubs.variantsync.core.data.FeatureExpression;
import de.tubs.variantsync.core.data.SourceFile;


/**
 * Loads and saves objects
 * @author Christopher Sontag (c.sontag@tu-bs.de)
 * @since 1.0.0.0
 */
public class Persistence {
	
	/**
	 * Loads all feature expressions
	 * @param filename
	 * @return
	 */
	public static List<FeatureExpression> loadFeatureExpressions() {
		List<FeatureExpression> featureExpressions = new ArrayList<>();
		FileHandler.load(Paths.get(VariantSyncPlugin.getContext().getConfigurationProject().getProject().getFile(FeatureExpressionFormat.FILENAME).getLocationURI()), featureExpressions, new FeatureExpressionFormat());
		return featureExpressions;
	}

	/**
	 * Saves all feature expressions
	 * @param context
	 * @param filename
	 */
	public static void writeFeatureExpressions(List<FeatureExpression> list) {
		FileHandler.save(Paths.get(VariantSyncPlugin.getContext().getConfigurationProject().getProject().getFile(FeatureExpressionFormat.FILENAME).getLocationURI()), list, new FeatureExpressionFormat());
	}

	/**
	 * Loads all feature expressions
	 * @param iFeatureProject 
	 * @return
	 */
	public static Context loadContext(IFeatureProject iFeatureProject) {
		Context context = new Context();
		if (iFeatureProject != null) {
			context.setConfigurationProject(iFeatureProject);
			FileHandler.load(Paths.get(iFeatureProject.getProject().getFile(ContextFormat.FILENAME).getLocationURI()), context, new ContextFormat());
		}
		return context;
	}

	/**
	 * Saves all feature expressions
	 * @param context
	 * @param filename
	 */
	public static void writeContext(Context context) {
		FileHandler.save(Paths.get(VariantSyncPlugin.getContext().getConfigurationProject().getProject().getFile(ContextFormat.FILENAME).getLocationURI()), context, new ContextFormat());
	}
	
	/**
	 * 
	 * @param project
	 * @return
	 */
	public static List<SourceFile> loadCodeMapping(IProject project) {
		List<SourceFile> sourceFiles = new ArrayList<>();
		if (project != null) {
			FileHandler.load(Paths.get(project.getFile(CodeMappingFormat.FILENAME).getLocationURI()), sourceFiles, new CodeMappingFormat());
		}
		return sourceFiles;
	}

	/**
	 * Saves all feature expressions
	 * @param context
	 * @param filename
	 */
	public static void writeCodeMapping(IProject project, List<SourceFile> sourceFiles) {
		FileHandler.save(Paths.get(project.getFile(CodeMappingFormat.FILENAME).getLocationURI()), sourceFiles, new CodeMappingFormat());
	}
	
}
