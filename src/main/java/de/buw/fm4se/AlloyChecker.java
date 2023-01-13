package de.buw.fm4se;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.CommandScope;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class AlloyChecker {

	public static List<String> findDeadSignatures(String fileName, A4Options options, A4Reporter rep) {
		List<String> deadSignatures = new ArrayList<>();
		Module world = CompUtil.parseEverything_fromFile(rep, null, fileName);
		options.solver = A4Options.SatSolver.SAT4J;

		// create a map where I'll keep the sum of the all signatures in all instances
		// if the sum in all instances is zero that means signature is dead
		Map<String, Integer> mapSumSignaturePerInstance = new HashMap<>();

		// execute command run or check depending the als file
		for (Command command : world.getAllCommands()) {
			A4Solution instance = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command,
					options);
			// get all signatures per model in a conslist
			ConstList<Sig> sigUser = world.getAllReachableUserDefinedSigs();

			// initialization of the signature in the list
			for (int i = 0; i < sigUser.size(); i++) {
				mapSumSignaturePerInstance.put(sigUser.get(i).label, 0);
			}

			// I scroll through all the instances to add up the number of atoms per
			// signature
			while (instance.satisfiable()) {
				int sumAtoms = 0;
				for (int i = 0; i < sigUser.size(); i++) {
					if (instance.eval(sigUser.get(i)).size() > 0) {
						sumAtoms += instance.eval(sigUser.get(i)).size();
					}
					mapSumSignaturePerInstance.put(sigUser.get(i).label,
							mapSumSignaturePerInstance.get(sigUser.get(i).label) + sumAtoms);
				}
				instance = instance.next();
			}
		}
		// after scroll through all instances check the instances in zero and add to the
		// deadsignatures list
		for (Map.Entry<String, Integer> entry : mapSumSignaturePerInstance.entrySet()) {
			String nameSignature = entry.getKey();
			Integer sumSignature = entry.getValue();

			if (sumSignature == 0) {
				deadSignatures.add(nameSignature);
			}
		}
		return deadSignatures;
	}

	public static List<String> findCoreSignatures(String fileName, A4Options options, A4Reporter rep) {
		List<String> coreSignatures = new ArrayList<>();
		Module world = CompUtil.parseEverything_fromFile(rep, null, fileName);

		for (Command command : world.getAllCommands()) {
			A4Solution instance = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command,
					options);

			ConstList<Sig> sigUser = world.getAllReachableUserDefinedSigs();

			// I assumed at the beginning that all signatures are core
			for (int i = 0; i < sigUser.size(); i++) {
				coreSignatures.add(sigUser.get(i).label);
			}

			// I scroll through all the instances to see if in any instance the signature
			// doesn't have atoms that means the signature is not core signature
			while (instance.satisfiable()) {
				for (int i = 0; i < sigUser.size(); i++) {
					Integer numAtomsSignature = instance.eval(sigUser.get(i)).size();
					// Here I check if the number of atoms in the signature in this specific
					// instance is zero and the signature was already removed from the list
					// I can remove
					if ((numAtomsSignature == null || numAtomsSignature.intValue() == 0)
							&& (coreSignatures.contains(sigUser.get(i).label))) {
						coreSignatures.remove(sigUser.get(i).label);
					}
				}
				instance = instance.next();
			}
		}
		return coreSignatures;
	}

	/**
	 * Computes for each user-defines signature a minimal scope for which the model
	 * is still satisfiable. Note that the scopes will be independent, i.e., minimum
	 * 0 for sig A and 0 for sig B does not mean that both can be 0 together.
	 * 
	 * @param fileName
	 * @param options
	 * @param rep
	 * @return map from signature names to minimum scopes
	 */
	public static Map<String, Integer> findMinScope(String fileName, A4Options options, A4Reporter rep) {
		// TODO Task 3
		return null;

	}

	/**
	 * Computes the maximum scope for a signature in a command. This is either the
	 * default of 4, the overall scope, or the specific scope for the signature in
	 * the command.
	 * 
	 * @param sig
	 * @param cmd
	 * @return
	 */
	public static int getMaxScope(Sig sig, Command cmd) {
		int scope = 4; // Alloy's default
		if (cmd.overall != -1) {
			scope = cmd.overall;
		}
		CommandScope cmdScope = cmd.getScope(sig);
		if (cmdScope != null) {
			scope = cmdScope.endingScope;
		}
		return scope;
	}
}