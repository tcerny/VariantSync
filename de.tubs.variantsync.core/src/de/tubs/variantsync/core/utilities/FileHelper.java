package de.tubs.variantsync.core.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class FileHelper {

	public static IFileState getLatestHistory(IFile res) {
		IFileState[] states = null;
		try {
			states = res.getHistory(null);
		} catch (CoreException e) {
			LogOperations.logError("File states could not be retrieved.", e);
		}
		if (states.length > 0) {
			return states[0];
		}
		return null;
	}

	public static List<String> getFileLines(IFile res) {
		List<String> currentFilelines = new ArrayList<>();
		try {
			currentFilelines = readFile(res.getContents(), res.getCharset());
		} catch (IOException | NullPointerException | CoreException e) {
			LogOperations.logError("File could not be read.", e);
		}
		return currentFilelines;
	}

	public static List<String> getFileLines(IFileState state) {
		List<String> currentFilelines = new ArrayList<>();
		try {
			currentFilelines = readFile(state.getContents(), state.getCharset());
		} catch (IOException | NullPointerException | CoreException e) {
			LogOperations.logError("File states could not be read.", e);
		}
		return currentFilelines;
	}

	public static void setFileLines(IFile res, List<String> lines) {
		try {
			writeFile(res, lines);
		} catch (IOException | CoreException e) {
			LogOperations.logError("File could not be written.", e);
		}
	}

	/**
	 * Reads content from file using buffered reader. Adds each line in file to
	 * List<String>.
	 *
	 * @param in
	 *            buffered Reader for file
	 * @param charset
	 * @return list with file content
	 * @throws IOException
	 */
	private static List<String> readFile(InputStream in, String charset) throws IOException {
		List<String> fileContent = new LinkedList<String>();
		String line = "";
		BufferedReader reader = null;
		if (charset == null)
			charset = (String) "UTF-8";
		try {
			reader = new BufferedReader(new InputStreamReader(in, charset));
			while ((line = reader.readLine()) != null) {
				fileContent.add(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				reader.close();
			} catch (NullPointerException | IOException e) {
				LogOperations.logError("BufferedReader could not be closed.", e);
			}
		}
		return fileContent;
	}

	public static void writeFile(IFile res, List<String> lines) throws IOException, CoreException {
		File file = new File(res.getRawLocationURI());
		System.out.println(file.getAbsolutePath());
		if (file.exists()) {
			file.delete();
		}
		File parentDir = file.getParentFile();
		if (!parentDir.exists())
			parentDir.mkdirs();
		PrintWriter out = null;
		try {
			file.createNewFile();
			out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			for (String line : lines) {
				out.println(line);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
			try {
				ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				LogOperations.logInfo("Refresh could not be made because the workspace is locked up");
			}
		}
	}

}
