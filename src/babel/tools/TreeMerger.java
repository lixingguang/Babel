package babel.tools;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import beast.app.beauti.BeautiDoc;
import beast.app.treeannotator.TreeAnnotator;
import beast.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beast.app.util.Application;
import beast.core.Description;
import beast.core.Input;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Merge sub-trees into skeleton tree")
public class TreeMerger extends TreeCombiner {
	final public Input<File> cgfFileInput = new Input<>("cfg", "tab separated configuration file containing info for one tree set per line. "
			+ "Firts column is name of tree file, second column a comma separated list of taxa to be transfered to source tree.");

	int subTreeCount;
	Set<String> [] subTaxonSets;
	MemoryFriendlyTreeSet [] subTreeSet;
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		MemoryFriendlyTreeSet srcTreeSet = new TreeAnnotator().new MemoryFriendlyTreeSet(srcInput.get().getPath(), 0);
		srcTreeSet.reset();
		Tree tree = srcTreeSet.next();
		
		processCfgFile();
		
		PrintStream out = System.out;
		if (outputInput.get() != null && !outputInput.get().getName().equals("[[none]]")) {
			Log.warning("Writing to file " + outputInput.get().getName());
			out = new PrintStream(outputInput.get());
		}

		srcTreeSet.reset();
		while (srcTreeSet.hasNext()) {
			tree = srcTreeSet.next();
			for (int i = 0; i < subTreeCount; i++) {
				Node src = getMRCA(tree, subTaxonSets[i]);
				Node parent = src.getParent();
				if (subTreeSet[i].hasNext()) {
					Tree subTree = subTreeSet[i].next();
					
					Node replacement = getMRCA(subTree, subTaxonSets[i]);
					boolean replaced = false;
					for (int j = 0; j < parent.getChildCount(); j++) {
						if (parent.getChild(j) == src) {
							src.getParent().setChild(j, replacement);
							replacement.setParent(parent);
							replaced = true;
						}
					}
					if (!replaced) {
						throw new RuntimeException("Something went wrong replacing node");
					}
				} else {
					throw new IllegalArgumentException("Tree sets are of different sizes: treeset " + i + " is smaler than source set");
				}
			}
			out.print(tree.getRoot().toNewick());
		}
		 
		Log.err("Done!");
		out.close();
	}
	
	private void processCfgFile() throws IOException {
		String cfg = BeautiDoc.load(cgfFileInput.get());
		String [] strs = cfg.split("\n");
		subTreeCount = 0;
		for (String str : strs) {
			if (!str.matches("^\\s*$")) {
				subTreeCount++;
			}
		}
		subTreeSet = new MemoryFriendlyTreeSet[subTreeCount];
		subTaxonSets = new Set[subTreeCount];
		int i = 0;
		for (String str : strs) {
			if (!str.matches("^\\s*$")) {
				String [] strs2 = str.split("\t");
				subTreeSet[i] = new TreeAnnotator().new MemoryFriendlyTreeSet(strs2[0], 0);
				subTaxonSets[i] = new HashSet<>();
				for (String taxon : strs2[1].split(",")) {
					subTaxonSets[i].add(taxon);					
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new Application(new TreeMerger(), "Tree Merger", args);
	}

}
