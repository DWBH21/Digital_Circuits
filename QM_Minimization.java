import java.util.*;

import raw.Minterm;

public class QM_Minimization {
    
    private static class BestCover 
    {
        Set<Integer> solution = null;
    }
    public static List<Minterm> getPrimeImplicants(List<Minterm> minterms) {
        if (minterms.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<Minterm> primeImplicants = new HashSet<>();
        Set<Minterm> mergedMinterms = new HashSet<>();
        boolean[] merged = new boolean[minterms.size()];
        boolean hadMerge = false;

        for (int i = 0; i < minterms.size(); i++) {
            for (int j = i + 1; j < minterms.size(); j++) {
                Minterm m1 = minterms.get(i);
                Minterm m2 = minterms.get(j);
                
                if (m1.canMergeWith(m2)) {
                    Minterm mergedMinterm = m1.merge(m2);
                    mergedMinterms.add(mergedMinterm);
                    merged[i] = true;
                    merged[j] = true;
                    hadMerge = true;
                }
            } 
        }
        
        for (int i = 0; i < minterms.size(); i++) {
            if (!merged[i]) {
                primeImplicants.add(minterms.get(i));
            }
        }
        
        if (!hadMerge)
        {
            return new ArrayList<>(primeImplicants);
        } 
        else
        {
            List<Minterm> nextMintermList = new ArrayList<>(mergedMinterms);
            primeImplicants.addAll(getPrimeImplicants(nextMintermList));
            return new ArrayList<>(primeImplicants);
        }   
    }
    
    static Map<Integer, List<Integer>> buildChart(List<Minterm> primeImplicants, List<Integer> ones, int noVars) 
    {
        Map<Integer, List<Integer>> chart = new LinkedHashMap<>();
        for(int i=0; i<primeImplicants.size(); i++) 
        {
            Minterm m = primeImplicants.get(i);
            List<Integer> covered = new ArrayList<>();
            for(int no : ones)
            {
                if(m.covers(no, noVars))
                    covered.add(no);
            }
            if(!covered.isEmpty())
                chart.put(i, covered);
        }
        return chart;
    }

    static Set<Integer> getMinExpression(List<Minterm> primeImplicants, List<Integer> ones, int noVars) 
    {
        if(primeImplicants.isEmpty())
            return new HashSet<>();

        // Building the prime implicant chart
        Map<Integer, List<Integer>> chart = buildChart(primeImplicants, ones, noVars);
        Set<Integer> remMinterms = new HashSet<>(ones);

        // Selecting esssential prime implicants
        Set<Integer> chosenPIs = reduceByEssentials(chart, remMinterms);

        // If some minterms still remain uncovered, using some trial and error
        if(!remMinterms.isEmpty())
        {
            Set<Integer> cover = findMinCover(chart, remMinterms);
            chosenPIs.addAll(cover);
        }        
        
        return chosenPIs;
    }

    // uses backtracking and pruning to find best expression 
    private static Set<Integer> findMinCover(Map<Integer, List<Integer>> chart, Set<Integer> remMinterms) {
        
        BestCover best = new BestCover();
        backtrack(new HashSet<>(), new HashSet<>(remMinterms), chart, best);

        return best.solution != null ? best.solution : new HashSet<>();
    }

    private static void backtrack(Set<Integer> currCover, Set<Integer> remMinterms, Map<Integer, List<Integer>> chart, BestCover best) {

        if(remMinterms.isEmpty())
        {
            // check if it is optimal 
            if(best.solution == null || currCover.size() < best.solution.size())
            {
                best.solution = new HashSet<>(currCover);
            }
            return;
        }

        // Prune 
        if(best.solution != null && currCover.size() >= best.solution.size())
            return;

        int firstMinterm = remMinterms.iterator().next();
        List<Integer> candidatePIs = new ArrayList<>();
        for(Map.Entry<Integer, List<Integer>> entry : chart.entrySet())
        {
            if(entry.getValue().contains(firstMinterm))
                candidatePIs.add(entry.getKey());
        }

        for(int pi : candidatePIs)
        {
            Set<Integer> nextCover = new HashSet<>(currCover);
            nextCover.add(pi);

            Set<Integer> nextRem = new HashSet<>(remMinterms);
            nextRem.removeAll(chart.get(pi));

            backtrack(nextCover, nextRem, chart, best);
        }
    }   

    static Set<Integer> reduceByEssentials(Map<Integer, List<Integer>> chart, Set<Integer> remMinterms)
    {
        Set<Integer> chosenImplicants = new HashSet<>();
        boolean progress = true;
        while(progress)   // while there is progress
        {
            progress = false;
            
            Map<Integer, List<Integer>> reverseMap = new HashMap<>();   // constructs the map from remTerms to the List of prime implicants it is a part of 
            for(int minterm : remMinterms) 
            {
                reverseMap.put(minterm, new ArrayList<>());
            }
            for(Map.Entry<Integer, List<Integer>> entry : chart.entrySet()) 
            {
                for(int minterm : entry.getValue()) 
                {
                    if(remMinterms.contains(minterm))
                        reverseMap.get(minterm).add(entry.getKey());
                }
            }

            for(Map.Entry<Integer, List<Integer>> entry : reverseMap.entrySet())
            {
                if(entry.getValue().size() == 1) // if the minterm is covered by only one prime implicant
                {
                    int essential = entry.getValue().get(0);
                    if(chosenImplicants.add(essential))
                    {
                        progress = true;
                        remMinterms.removeAll(chart.get(essential));
                    }
                }
            }
        }
        return chosenImplicants;
    }
}