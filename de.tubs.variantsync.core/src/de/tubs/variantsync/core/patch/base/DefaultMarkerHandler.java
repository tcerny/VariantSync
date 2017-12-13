package de.tubs.variantsync.core.patch.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.github.difflib.patch.Chunk;

import de.tubs.variantsync.core.patch.AMarkerHandler;
import de.tubs.variantsync.core.patch.AMarkerInformation;
import de.tubs.variantsync.core.patch.interfaces.IDelta;
import de.tubs.variantsync.core.patch.interfaces.IMarkerInformation;

public class DefaultMarkerHandler extends AMarkerHandler<Chunk<String>> {

	@SuppressWarnings("rawtypes")
	@Override
	public List<IMarkerInformation> getMarkersForDeltas(IFile file, List<IDelta<Chunk<String>>> deltas) {
		List<IMarkerInformation> markerInformations = new ArrayList<>();
		for (IDelta<Chunk<String>> delta : deltas) {
			Chunk revised = delta.getRevised();
			IMarkerInformation markerInformation = new AMarkerInformation(revised.getPosition() - 1, revised.getLines().size() - 1, true);
			markerInformation.setFeatureExpression(delta.getFeature());
			markerInformations.add(markerInformation);
		}
		return markerInformations;
	}

	@Override
	public IMarkerInformation updateMarkerForDelta(IMarkerInformation markerInformation, IDelta<Chunk<String>> delta) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IMarkerInformation> getMarkers(IFile file, int offset, int length) {
		return Arrays.asList(new AMarkerInformation(offset, length, false));
	}

}
