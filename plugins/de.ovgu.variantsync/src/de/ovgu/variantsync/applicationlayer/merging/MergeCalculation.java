package de.ovgu.variantsync.applicationlayer.merging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.ovgu.variantsync.applicationlayer.ModuleFactory;
import de.ovgu.variantsync.applicationlayer.datamodel.exception.PatchException;
import de.ovgu.variantsync.applicationlayer.deltacalculation.DeltaOperations;
import de.ovgu.variantsync.utilities.LogOperations;
import difflib.Delta;
import difflib.Patch;

/**
 * Provides functions to merge changes.
 *
 * @author Tristan Pfofe (tristan.pfofe@ckc.de)
 * @version 1.0
 * @since 15.05.2015
 */
class MergeCalculation {

	private DeltaOperations deltaOperations = ModuleFactory.getDeltaOperations();

	/**
	 * Performs three way merge. Joins three development histories together.
	 * 
	 * @param fOrigin
	 *            origin development history
	 * @param fList1
	 *            development history one
	 * @param fList2
	 *            development history two
	 * @return merged development branch
	 */
	public Collection<String> performThreeWayMerge(Collection<String> fOrigin, Collection<String> fList1,
			Collection<String> fList2) {
		Collection<String> result = null;
		Patch patchOriginWithOne = deltaOperations.computeDifference(fOrigin, fList1);
		Patch patchOriginWithTwo = deltaOperations.computeDifference(fOrigin, fList2);
		List<Delta> deltasO1 = patchOriginWithOne.getDeltas();
		List<Delta> deltasO2 = patchOriginWithTwo.getDeltas();
		if (!checkConflict(deltasO1, deltasO2)) {

			Patch patchTemp = new Patch();
			Set<Delta> tempDeltas = new HashSet<Delta>();
			tempDeltas.addAll(deltasO1);
			tempDeltas.addAll(deltasO2);
			for (Delta d : tempDeltas) {
				patchTemp.addDelta(d);
			}
			try {
				result = deltaOperations.computePatch(fOrigin, patchTemp);
			} catch (PatchException e) {
				LogOperations.logError("Patch could not be computed.", e);
			}
			return result;
		} else {
			return fList2;
		}
	}

	public boolean checkConflict(List<String> fOrigin, List<String> fList1, List<String> fList2) {
		Patch patchOriginWithOne = deltaOperations.computeDifference(fOrigin, fList1);
		Patch patchOriginWithTwo = deltaOperations.computeDifference(fOrigin, fList2);
		List<Delta> deltasO1 = patchOriginWithOne.getDeltas();
		List<Delta> deltasO2 = patchOriginWithTwo.getDeltas();
		return checkConflict(deltasO1, deltasO2);
	}

	/**
	 * Checks conflicts between two deltas. A conflict is detected if one of the
	 * following criteria is satisfied:<br>
	 * <ul>
	 * <li>changed delta contains original delta</li>
	 * <li>end position of change of delta is before starting position of change
	 * of following delta
	 * </ul>
	 * 
	 * @param deltas12
	 *            original delta
	 * @param deltas13
	 *            changed delta
	 * @return true if conflict is detected; otherwise false
	 */
	public boolean checkConflict(List<Delta> d12, List<Delta> d13) {
		List<Delta> deltas12 = new ArrayList<Delta>();
		for (Delta dw : d12) {
			deltas12.add(dw);
		}
		List<Delta> deltas13 = new ArrayList<Delta>();
		for (Delta dw : d13) {
			deltas13.add(dw);
		}
		if (deltas13.containsAll(deltas12)) {
			return true;
		}
		Set<Delta> tempDeltas = new HashSet<Delta>();
		tempDeltas.addAll(deltas12);
		tempDeltas.addAll(deltas13);
		Patch patchTemp = new Patch();
		for (Delta d : tempDeltas) {
			patchTemp.addDelta(d);
		}
		List<Delta> deltas = patchTemp.getDeltas();
		for (int i = 0; i < deltas.size(); i++) {
			if (i + 1 < deltas.size()) {
				Delta actualDelta = deltas.get(i + 1);
				int nextStartPosition = actualDelta.getOriginal().getPosition();
				Delta followingDelta = deltas.get(i);
				int curEndPosition = followingDelta.getOriginal().last();
				if (nextStartPosition - curEndPosition > 1) {
					continue;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	public Collection<Delta> getConflictingDeltas(ArrayList<String> arrayList, ArrayList<String> arrayList2,
			ArrayList<String> arrayList3) {
		Patch patchOriginWithOne = deltaOperations.computeDifference(arrayList, arrayList2);
		Patch patchOriginWithTwo = deltaOperations.computeDifference(arrayList, arrayList3);
		List<Delta> deltasO1 = patchOriginWithOne.getDeltas();
		List<Delta> deltasO2 = patchOriginWithTwo.getDeltas();

		List<Delta> deltas12 = new ArrayList<Delta>();
		for (Delta dw : deltasO1) {
			deltas12.add(dw);
		}
		List<Delta> deltas13 = new ArrayList<Delta>();
		for (Delta dw : deltasO2) {
			deltas13.add(dw);
		}
		if (deltas13.containsAll(deltas12)) {
			return Collections.emptyList();
		}
		Set<Delta> tempDeltas = new HashSet<Delta>();
		tempDeltas.addAll(deltas12);
		tempDeltas.addAll(deltas13);
		Patch patchTemp = new Patch();
		for (Delta d : tempDeltas) {
			patchTemp.addDelta(d);
		}
		List<Delta> deltas = patchTemp.getDeltas();
		for (int i = 0; i < deltas.size(); i++) {
			if (i + 1 < deltas.size()) {
				Delta actualDelta = deltas.get(i + 1);
				int nextStartPosition = actualDelta.getOriginal().getPosition();
				Delta followingDelta = deltas.get(i);
				int curEndPosition = followingDelta.getOriginal().last();
				if (nextStartPosition - curEndPosition > 1) {
					continue;
				} else {
					Collection<Delta> result = new ArrayList<Delta>();
					result.add(actualDelta);
					result.add(followingDelta);
					return result;
				}
			}
		}
		return Collections.emptyList();
	}
}