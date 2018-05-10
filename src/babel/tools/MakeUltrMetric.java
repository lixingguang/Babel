package babel.tools;

import java.io.PrintStream;

import beast.app.treeannotator.TreeAnnotator;
import beast.app.treeannotator.TreeAnnotator.FastTreeSet;
import beast.app.util.Application;
import beast.app.util.OutFile;
import beast.app.util.TreeFile;
import beast.core.Description;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Converts a rooted tree (set) to an ultrametric tree (set), "
		+ "i.e. make all leafs have same distance to root by extending leaf "
		+ "branches so the height of all leaf nodes is zero.")
public class MakeUltrMetric extends Runnable {
	final public Input<TreeFile> treesInput = new Input<>("trees","NEXUS file containing a tree set", Validate.REQUIRED);
	final public Input<OutFile> outputInput = new Input<>("out","output file. Print to stdout if not specified");

	@Override
	public void initAndValidate() {

	}

	@Override
	public void run() throws Exception {
		// open file for writing
        PrintStream out = System.out;
        if (outputInput.get() != null) {
        	out = new PrintStream(outputInput.get());
			Log.warning("Writing to file " + outputInput.get().getPath());
        }

        
        FastTreeSet trees = new TreeAnnotator().new FastTreeSet(treesInput.get().getAbsolutePath(), 0);
        trees.reset();
        Tree tree = trees.next();
        tree.init(out);
        out.println();

        trees.reset();
        int i = 0;
        while (trees.hasNext()) {
        	tree = trees.next();
            makeUltrametric(tree.getRoot());
            out.println();
            out.print("tree STATE_" + i + " = ");
            final String newick = tree.getRoot().toSortedNewick(new int[1], true);
            out.print(newick);
            out.print(";");
        	i++;
        }
        out.println();
        out.println("end;");
        Log.warning("Done");
	}

	private void makeUltrametric(Node node) {
		if (node.isLeaf()) {
			node.setHeight(0);
		} else {
			for (Node child : node.getChildren()) {
				makeUltrametric(child);
			}
		}
	}


	public static void main(String[] args) throws Exception{
		new Application(new MakeUltrMetric(), "Make Ultrametric", args);

	}

}