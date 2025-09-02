import java.util.*;

/**
 * Main class to test the Quine-McCluskey implementation.
 */
public class Main {
    public static void main(String[] args) {
        // --- 1. Define the Input Problem ---
        int noVars = 5;
        List<Integer> onSet = Arrays.asList(0,2,4,6,9,13,21,23,25,29,31);
        List<Integer> dontCares = Arrays.asList();

        System.out.println("Minimizing for function: f(A,B,C,D) = Σm(" + onSet + ") + d(" + dontCares + ")\n");

        // --- 2. Create Initial Minterms for Prime Implicant Generation ---
        // Both the on-set and don't-care set are used in this phase.
        List<Minterm> initialMinterms = new ArrayList<>();
        onSet.forEach(m -> initialMinterms.add(new Minterm(m, noVars)));
        dontCares.forEach(d -> initialMinterms.add(new Minterm(d, noVars)));

        // --- 3. Generate All Prime Implicants ---
        List<Minterm> primeImplicants = QM_Minimization.getPrimeImplicants(initialMinterms);

        System.out.println("--- Step 1: All Prime Implicants Found ---");
        for (int i = 0; i < primeImplicants.size(); i++) {
            Minterm pi = primeImplicants.get(i);
            // Sorting the minterm numbers for consistent display
            List<Integer> sortedMinterms = new ArrayList<>(pi.mintermNos);
            Collections.sort(sortedMinterms);
            System.out.println("  Index " + i + ": " + pi.s + "  (Covers original minterms: " + sortedMinterms + ")");
        }
        System.out.println();

        // --- 4. Find the Minimal Set of Prime Implicants ---
        // This function builds the chart, selects essentials, and uses backtracking.
        Set<Integer> minimalPIIndices = QM_Minimization.getMinExpression(primeImplicants, onSet, noVars);

        // --- 5. Display the Final Result ---
        System.out.println("--- Step 2: Final Minimized Solution ---");
        System.out.println("The minimal solution requires the following prime implicants (by index): " + minimalPIIndices + "\n");
        System.out.println("Corresponding Boolean Expression Terms:");
        for (int index : minimalPIIndices) {
            System.out.println("  " + primeImplicants.get(index).s);
        }
    }
}