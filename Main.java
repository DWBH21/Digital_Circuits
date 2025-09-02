import java.util.*;

public class Main {
    public static void main(String[] args) {
        // variables: A,B,C,D → 4 variables
        int noVars = 4;

        // on-set minterms
        int[] ones = {4, 8, 10, 11, 12, 15};

        // don't-cares (included for merging but excluded from final coverage columns)
        int[] dontCares = {9, 14};

        // build initial list: include on-set and don't-cares for the merging phase
        List<Minterm> initial = new ArrayList<>();
        for (int m : ones) initial.add(new Minterm(m, noVars));
        for (int d : dontCares) initial.add(new Minterm(d, noVars));

        // compute prime implicants
        List<Minterm> primeImplicants = QM_Minimization.getPrimeImplicants(initial);

        // print prime implicants (pattern and covered minterms)
        System.out.println("Prime Implicants:");
        for (Minterm pi : primeImplicants) {
            System.out.println("  " + pi.s + "  covers " + pi.mintermNos);
        }

        // If desired, filter coverage display to only the on-set (exclude don't-cares)
        Set<Integer> onSet = new HashSet<>();
        for (int m : ones) onSet.add(m);

        System.out.println("\nPrime Implicants (coverage restricted to on-set):");
        for (Minterm pi : primeImplicants) {
            List<Integer> coverOnSet = new ArrayList<>();
            for (int x : pi.mintermNos) if (onSet.contains(x)) coverOnSet.add(x);
            if (!coverOnSet.isEmpty()) {
                System.out.println("  " + pi.s + "  covers " + coverOnSet);
            }
        }

        // Note: Next step would be to build the prime implicant chart and select essentials.
        // The sample here only demonstrates invoking getPrimeImplicants on the given example.
    }
}