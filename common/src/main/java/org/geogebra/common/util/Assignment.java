package org.geogebra.common.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Macro;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoMacro;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.Test;
import org.geogebra.common.kernel.prover.AlgoAreEqual;
import org.geogebra.common.main.App;

/**
 * @author Christoph
 * 
 */
public class Assignment {

	/**
	 * The Result of the Assignment
	 */
	public static enum Result {
		/**
		 * The assignment is CORRECT
		 */
		CORRECT,
		/**
		 * The assignment is WRONG and we can't tell why
		 */
		WRONG,
		/**
		 * There are not enough input geos, so we cannot check
		 */
		NOT_ENOUGH_INPUTS,
		/**
		 * We have enough input geos, but one or more are of the wrong type
		 */
		WRONG_INPUT_TYPES,
		/**
		 * There is no output geo matching our macro
		 */
		WRONG_OUTPUT_TYPE,
		/**
		 * The assignment was correct in the first place but wrong after
		 * randomization
		 */
		WRONG_AFTER_RANDOMIZE,
		/**
		 * The assignment could not be checked
		 */
		UNKNOWN,
	}

	/**
	 * Possible values for fractions (sorted ascending!)
	 */
	public final static float[] FRACTIONS = { 0f, 0.1f, 0.125f, 0.2f, 0.25f,
			0.3333f, 0.5f, 1f };

	private Macro macro;

	private HashMap<Result, Float> fractionForResult;
	/* The hints displayed to the Student */
	private HashMap<Result, String> hintForResult;

	private GeoElement[] solutionObjects;

	private Result res;

	private int callsToAlgoEqual = 0;

	/**
	 * @param macro
	 *            the macro (user defined tool) corresponding to the assignment
	 */
	public Assignment(Macro macro) {
		this.macro = macro;

		fractionForResult = new HashMap<Result, Float>();

		hintForResult = new HashMap<Result, String>();

		res = Result.UNKNOWN;
	}

	/**
	 * Exhaustive Testing of the Assignment
	 * 
	 * @param cons
	 *            the construction object of the kernel
	 * 
	 * @return {@link Result} of the check
	 */
	public Result checkAssignment(Construction cons) {
		res = Result.UNKNOWN;
		callsToAlgoEqual = 0;
		TreeSet<GeoElement> possibleOutputGeos = new TreeSet<GeoElement>();

		// find all possible inputgeos and all outputgeos that match the type of
		// the macro
		TreeSet<GeoElement> sortedSet = cons.getGeoSetNameDescriptionOrder();
		Iterator<GeoElement> it = sortedSet.iterator();
		while (it.hasNext()) {
			GeoElement geo = it.next();
			for (GeoElement macroOut : macro.getMacroOutput()) {
				if (macroOut.getClass().equals(geo.getClass())) {
					TreeSet<GeoElement> allPredecessors = geo
							.getAllPredecessors();
					if (!allPredecessors.isEmpty()) {
						possibleOutputGeos.add(geo);
					}
				}
			}
		}
		if (macro.getMacroOutput().length > possibleOutputGeos.size()) {
			res = Result.WRONG_OUTPUT_TYPE;
		} else {
			checkCorrectness(possibleOutputGeos, cons);
		}
		App.debug("Checking on " + macro.getToolName()
				+ " completed. Comparisons of Objects: " + callsToAlgoEqual);

		return res;
	}

	private void checkCorrectness(TreeSet<GeoElement> possibleOutputGeos,
			Construction cons) {

		PermutationOfGeOElementsUtil outputPermutationUtil = new PermutationOfGeOElementsUtil(
				possibleOutputGeos.toArray(new GeoElement[0]),
				macro.getMacroOutput().length);
		GeoElement[] possibleOutputPermutation = outputPermutationUtil.next();

		TreeSet<Result> partRes = new TreeSet<Result>();

		while (possibleOutputPermutation != null && res != Result.CORRECT) {
			TreeSet<GeoElement> possibleInputGeos = getAllPredecessors(possibleOutputPermutation);
			if (possibleInputGeos.size() < macro.getInputTypes().length) {
				res = Result.NOT_ENOUGH_INPUTS;
			} else {
				checkPermutationsOfInputs(cons, possibleOutputPermutation,
						partRes, possibleInputGeos);
			}
			possibleOutputPermutation = outputPermutationUtil.next();
		}
	}

	private void checkPermutationsOfInputs(Construction cons,
			GeoElement[] possibleOutputPermutation, TreeSet<Result> partRes,
			TreeSet<GeoElement> possibleInputGeos) {
		GeoElement[] input;
		PermutationOfGeOElementsUtil inputPermutationUtil = new PermutationOfGeOElementsUtil(
				possibleInputGeos.toArray(new GeoElement[0]),
				macro.getInputTypes().length);
		input = inputPermutationUtil.next();
		boolean solutionFound = false;
		while (input != null && !solutionFound) {
			partRes.clear();
			if (areTypesOK(input)) {
				AlgoMacro algoMacro = new AlgoMacro(cons, null, macro,
						input);
				GeoElement[] macroOutput = algoMacro.getOutput();
				for (int i = 0; i < possibleOutputPermutation.length
						&& (!partRes.contains(Result.WRONG)); i++) {
					checkEqualityOfGeos(cons, input, macroOutput[i],
							possibleOutputPermutation[i], partRes);
				}
				algoMacro.remove();
				solutionFound = !partRes.contains(Result.WRONG)
						&& !partRes
								.contains(Result.WRONG_AFTER_RANDOMIZE)
						&& partRes.contains(Result.CORRECT);
			} else if (res != Result.WRONG_AFTER_RANDOMIZE
					&& res != Result.WRONG) {
				res = Result.WRONG_INPUT_TYPES;
			}
			if (partRes.contains(Result.WRONG)
					&& res != Result.WRONG_AFTER_RANDOMIZE) {
				res = Result.WRONG;
			} else if (partRes.contains(Result.WRONG_AFTER_RANDOMIZE)) {
				res = Result.WRONG_AFTER_RANDOMIZE;
				App.debug("Objects wrong after Randomize: "
						+ toString(possibleOutputPermutation));
				App.debug("Objects used as inputs: " + toString(input));
			} else if (partRes.contains(Result.CORRECT)) {
				res = Result.CORRECT;
				solutionObjects = possibleOutputPermutation;
				App.debug("Objects found to be the Solution: "
						+ toString(solutionObjects));
				App.debug("Objects used as inputs: " + toString(input));
			}
			input = inputPermutationUtil.next();

		}
	}

	private void checkEqualityOfGeos(Construction cons, GeoElement[] input,
			GeoElement macroOutput, GeoElement possibleOutput,
			TreeSet<Result> partRes) {
		GeoElement saveInput;
		AlgoAreEqual algoEqual = new AlgoAreEqual(cons, "", macroOutput,
				possibleOutput);
		partRes.add(algoEqual.getResult().getBoolean() ? Result.CORRECT
				: Result.WRONG);
		callsToAlgoEqual++;
		int j = 0;
		while (j < input.length && !partRes.contains(Result.WRONG)) {
			if (input[j].isRandomizable()) {
				saveInput = input[j].copy();
				input[j].randomizeForProbabilisticChecking();
				input[j].updateCascade();
				partRes.add(algoEqual.getResult().getBoolean() ? Result.CORRECT
						: Result.WRONG_AFTER_RANDOMIZE);
				callsToAlgoEqual++;
				input[j].set(saveInput);
				input[j].updateCascade();
			}
			j++;
		}
	}

	private static TreeSet<GeoElement> getAllPredecessors(
			GeoElement[] possibleOutputPermutation) {
		TreeSet<GeoElement> possibleInputGeos = new TreeSet<GeoElement>();
		for (int i = 0; i < possibleOutputPermutation.length; i++) {
			possibleOutputPermutation[i].addPredecessorsToSet(
					possibleInputGeos, true);
		}
		return possibleInputGeos;
	}

	private boolean areTypesOK(GeoElement[] input) {
		boolean typesOK = true; // we assume that types are OK

		Test[] inputTypes = macro.getInputTypes();

		int k = 0;
		// we assumed types are OK in the beginning
		typesOK = true;
		while (k < input.length && typesOK) {
			if (inputTypes[k].check(input[k])) {
				typesOK = true;
			} else {
				typesOK = false;
			}
			k++;
		}
		return typesOK;
	}

	private static String toString(GeoElement[] elements) {
		String solObj = "";
		for (GeoElement g : elements) {
			if (!solObj.isEmpty()) {
				solObj += ", ";
			}
			solObj += g.toString(StringTemplate.defaultTemplate);
	
		}
		return solObj;
	}

	/**
	 * @return the fraction for the Result if set, else 1.0 for correct Result
	 *         and 0 else.
	 */
	public float getFraction() {
		float fraction = 0f;
		if (fractionForResult.containsKey(res)) {
			fraction = fractionForResult.get(res);
		} else if (res == Result.CORRECT) {
			fraction = 1.0f;
		}
		return fraction;
	}

	public void setFractionForResult(Result res, float f) {
		fractionForResult.put(res, f);
	}

	public float getFractionForResult(Result res) {
		float frac = 0f;
		if (fractionForResult.containsKey(res)) {
			frac = fractionForResult.get(res);
		} else if (res == Result.CORRECT) {
			frac = 1.0f;
		}
		return frac;
	}

	public String getIconFileName() {
		return macro.getIconFileName();
	}

	/**
	 * @return the Name of the Tool corresponding to this Assignment
	 */
	public String getToolName() {
		return macro.getToolName();
	}

	/**
	 * Gives the hint for the actual state for this {@link Assignment}
	 * 
	 * @return the hint for current {@link Result}
	 */
	public String getHint() {
		return hintForResult.get(res);
	}

	/**
	 * Sets the Hint for a particular Result.
	 * 
	 * @param res
	 *            the {@link Result}
	 * @param hint
	 *            the hint which should be shown to the student in case of the
	 *            {@link Result} res
	 */
	public void setHintForResult(Result res, String hint) {
		this.hintForResult.put(res, hint);
	}

	/**
	 * @return the actual state for this {@link Assignment} as {@link Result}
	 */
	public Result getResult() {
		return res;
	}

	/**
	 * @param result
	 *            the Result for which the hint should be returned
	 * @return hint corresponding to result
	 */
	public String getHintForResult(Result result) {
		String hint = "";
		if (hintForResult.containsKey(result)) {
			hint = hintForResult.get(result);
		}
		return hint;
	}
}

// Eyal Schneider
// http://stackoverflow.com/a/2799190
/**
 * Utility Class to permute the array of GeoElements
 * 
 * @author Eyal Schneider, http://stackoverflow.com/a/2799190
 * @author Adaption: Christoph Reinisch
 */
class PermutationOfGeOElementsUtil {
	private GeoElement[] arr;
	private int[] permSwappings;

	/**
	 * @param arr
	 *            the Array with the Elements to be permuted
	 */
	public PermutationOfGeOElementsUtil(GeoElement[] arr) {
		this(arr, arr.length);
	}

	/**
	 * @param arr
	 *            the Array with the Elements to be permuted
	 * @param permSize
	 *            the Elements k < arr.length of the array you need to permute
	 */
	public PermutationOfGeOElementsUtil(GeoElement[] arr, int permSize) {

		// this.arr = arr.clone();
		this.arr = new GeoElement[arr.length];
		System.arraycopy(arr, 0, this.arr, 0, arr.length);
		this.permSwappings = new int[permSize];
		for (int i = 0; i < permSwappings.length; i++) {
			permSwappings[i] = i;
		}
	}

	/**
	 * @return the next permutation of the array if exists, null otherwise
	 */
	public GeoElement[] next() {
		if (arr == null) {
			return null;
		}

		GeoElement[] res = new GeoElement[permSwappings.length];
		System.arraycopy(arr, 0, res, 0, permSwappings.length);
		// GeoElement[] res = Arrays.copyOf(arr, permSwappings.length);

		// Prepare next
		int i = permSwappings.length - 1;
		while (i >= 0 && permSwappings[i] == arr.length - 1) {
			swap(i, permSwappings[i]); // Undo the swap represented by
										// permSwappings[i]
			permSwappings[i] = i;
			i--;
		}

		if (i < 0) {
			arr = null;
		} else {
			int prev = permSwappings[i];
			swap(i, prev);
			int next = prev + 1;
			permSwappings[i] = next;
			swap(i, next);
		}

		return res;
	}

	private void swap(int i, int j) {
		GeoElement tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

}